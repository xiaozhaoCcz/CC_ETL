package com.cc.job.executor.compose.handler;

import cn.hutool.json.JSONUtil;
import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.engine.Async;
import com.cc.job.executor.compose.engine.callback.ICallback;
import com.cc.job.executor.compose.engine.callback.IWorker;
import com.cc.job.executor.compose.engine.constant.JobConstant;
import com.cc.job.executor.compose.engine.worker.WorkResult;
import com.cc.job.executor.compose.engine.wrapper.WorkerWrapper;
import com.cc.job.executor.compose.service.JobExecutionMonitor;
import com.cc.job.executor.compose.service.JobTriggerService;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 任务组编排执行器（完整版）
 * 
 * <p>职责：
 * <ul>
 *   <li>1. 接收任务组执行请求</li>
 *   <li>2. 从 Admin 获取任务组信息（节点和边）</li>
 *   <li>3. 构建任务执行图</li>
 *   <li>4. 执行拓扑排序编排</li>
 *   <li>5. 监控执行状态并上报</li>
 *   <li>6. 处理暂停、恢复、重试等高级特性</li>
 * </ul>
 * 
 * @author xiaozhao
 */
@Component("jobGroupExecutorComplete")
@RequiredArgsConstructor
public class JobGroupExecutorComplete {
    
    private static final Logger logger = LoggerFactory.getLogger(JobGroupExecutorComplete.class);
    
    private final AdminApiClient adminApiClient;
    private final JobTriggerService jobTriggerService;
    
    /**
     * 存储正在执行的任务组
     * Key: jobId:randomId
     */
    private static final Map<String, List<WorkerWrapper<Long, String>>> RUNNING_JOBS = new ConcurrentHashMap<>();
    
    /**
     * 存储任务执行结果
     * Key: jobId:randomId, Value: 是否成功
     */
    private static final Map<String, Boolean> JOB_RESULTS = new ConcurrentHashMap<>();
    
    /**
     * 线程本地变量，用于存储 XxlJobContext
     */
    private static final InheritableThreadLocal<XxlJobContext> CONTEXT_HOLDER = new InheritableThreadLocal<>();
    
    /**
     * 任务组执行入口
     */
    @XxlJob("runJobGroupXxlJob")
    public void execute() {
        long jobId = XxlJobHelper.getJobId();
        String executeParam = XxlJobHelper.getJobParam();
        
        logger.info("[JobGroupExecutor] ========== 开始执行任务组 ==========");
        logger.info("[JobGroupExecutor] 任务ID: {}, 执行参数: {}", jobId, executeParam);
        
        // 1. 验证参数并生成批次ID
        if (StringUtils.isBlank(executeParam)) {
            logger.error("[JobGroupExecutor] 执行参数为空");
            XxlJobHelper.handleFail("执行参数为空");
            return;
        }
        
        String randomId = String.valueOf(jobId).equals(executeParam) 
                ? UUID.randomUUID().toString() 
                : executeParam;
        
        logger.info("[JobGroupExecutor] 批次ID: {}", randomId);
        
        try {
            // 2. 获取任务组信息
            JobInfo jobInfo = adminApiClient.getJobInfo(jobId);
            if (jobInfo == null) {
                logger.error("[JobGroupExecutor] 获取任务信息失败 - jobId: {}", jobId);
                XxlJobHelper.handleFail("获取任务信息失败");
                return;
            }
            
            logger.info("[JobGroupExecutor] 任务信息 - 名称: {}, 类型: {}, 超时: {}秒", 
                    jobInfo.getJobDesc(), jobInfo.getJobType(), jobInfo.getExecutorTimeout());
            
            // 3. 获取节点和边
            List<JobNode> nodes = adminApiClient.getJobNodes(jobId);
            List<JobEdge> edges = adminApiClient.getJobEdges(jobId);
            
            if (nodes.isEmpty()) {
                logger.warn("[JobGroupExecutor] 任务组没有节点 - jobId: {}", jobId);
                XxlJobHelper.handleSuccess("任务组没有节点，跳过执行");
                return;
            }
            
            logger.info("[JobGroupExecutor] 获取到 {} 个节点，{} 条边", nodes.size(), edges.size());
            
            // 4. 构建任务图（计算入度和出度）
            buildGraph(jobId, nodes, edges);
            
            // 5. 保存 XxlJobContext 到线程本地变量
            CONTEXT_HOLDER.set(XxlJobContext.getXxlJobContext());
            
            // 6. 构建 WorkerWrapper 列表
            List<WorkerWrapper<Long, String>> workerWrappers = buildWorkerWrappers(
                    nodes, edges, jobId, randomId, jobInfo);
            
            if (workerWrappers.isEmpty()) {
                logger.warn("[JobGroupExecutor] 没有可执行的任务");
                XxlJobHelper.handleSuccess("没有可执行的任务");
                return;
            }
            
            // 7. 保存到运行中的任务映射
            String executeKey = buildExecuteKey(jobId, randomId);
            RUNNING_JOBS.put(executeKey, workerWrappers);
            
            logger.info("[JobGroupExecutor] 开始执行任务组 - 任务数: {}, 超时: {}秒", 
                    workerWrappers.size(), jobInfo.getExecutorTimeout());
            
            // 8. 调用 Async 引擎执行
            Async.beginWork(jobInfo.getExecutorTimeout(), (List<WorkerWrapper>) (List<?>) workerWrappers);
            
            logger.info("[JobGroupExecutor] ========== 任务组执行完成 ==========");
            XxlJobHelper.handleSuccess("任务组执行完成");
            
        } catch (ExecutionException e) {
            logger.error("[JobGroupExecutor] 任务组执行异常 - jobId: {}, randomId: {}", jobId, randomId, e);
            XxlJobHelper.handleFail("任务组执行异常: " + e.getMessage());
        } catch (Exception e) {
            logger.error("[JobGroupExecutor] 任务组执行失败 - jobId: {}, randomId: {}", jobId, randomId, e);
            XxlJobHelper.handleFail("任务组执行失败: " + e.getMessage());
        } finally {
            // 9. 清理资源
            cleanup(jobId, randomId);
        }
    }
    
    /**
     * 构建 WorkerWrapper 列表
     */
    private List<WorkerWrapper<Long, String>> buildWorkerWrappers(
            List<JobNode> nodes, List<JobEdge> edges, Long parentJobId, String randomId, JobInfo parentJobInfo) {
        
        logger.debug("[JobGroupExecutor] 开始构建 WorkerWrapper");
        
        List<WorkerWrapper<Long, String>> result = new ArrayList<>();
        XxlJobContext xxlJobContext = CONTEXT_HOLDER.get();
        
        // 获取所有节点对应的任务信息
        Map<Long, JobInfo> jobInfoMap = getJobInfoMap(nodes);
        
        // 为每个节点创建 WorkerWrapper
        for (JobNode node : nodes) {
            JobInfo jobInfo = jobInfoMap.get(node.getJobId());
            if (jobInfo == null) {
                logger.warn("[JobGroupExecutor] 节点对应的任务不存在 - nodeId: {}, jobId: {}", 
                        node.getId(), node.getJobId());
                continue;
            }
            
            WorkerWrapper<Long, String> worker = new WorkerWrapper<Long, String>()
                    .id(String.valueOf(node.getId()))
                    .param(node.getJobId())
                    .timeout(jobInfo.getExecutorTimeout())
                    .retryCount(jobInfo.getExecutorFailRetryCount())
                    .worker(new IWorker<Long, String>() {
                        @Override
                        public String action(Long jobId, Map<String, WorkerWrapper> allWrappers) {
                            // 获取当前执行次数
                            WorkerWrapper currentWrapper = allWrappers.get(String.valueOf(node.getId()));
                            int count = currentWrapper != null ? currentWrapper.getCount() : 0;
                            
                            // 执行任务（完整实现）
                            return executeTaskComplete(xxlJobContext, node, jobInfo, parentJobId, randomId, count);
                        }
                        
                        @Override
                        public String defaultValue() {
                            // 默认失败（传递 parentJobId）
                            reportStatus(parentJobId, jobInfo.getId(), randomId, 0, "任务执行失败");
                            return JobConstant.FAIL_COMPLETE;
                        }
                    })
                    .callback(new ICallback<Long, String>() {
                        private long startTime;
                        
                        @Override
                        public void begin(Long jobId) {
                            startTime = System.currentTimeMillis();
                            logger.info("[JobGroupExecutor] ========== 任务开始执行 ==========");
                            logger.info("[JobGroupExecutor] 任务ID: {}, 节点ID: {}, 父任务ID: {}", 
                                    jobId, node.getId(), node.getJobParentId());
                            XxlJobHelper.log(xxlJobContext, "========== 任务开始执行 ==========");
                            XxlJobHelper.log(xxlJobContext, "任务ID: {}, 节点ID: {}", jobId, node.getId());
                            
                            // 上报开始状态（传递 parentJobId）
                            reportStatus(parentJobId, jobId, randomId, 2, "任务开始执行");
                        }
                        
                        @Override
                        public void result(boolean success, Long jobId, WorkResult<String> workResult) {
                            long duration = System.currentTimeMillis() - startTime;
                            logger.info("[JobGroupExecutor] ========== 任务执行完成 ==========");
                            logger.info("[JobGroupExecutor] 任务ID: {}, 成功: {}, 耗时: {}ms, 结果: {}", 
                                    jobId, success, duration, workResult.getResult());
                            XxlJobHelper.log(xxlJobContext, "========== 任务执行完成 ==========");
                            XxlJobHelper.log(xxlJobContext, "任务ID: {}, 成功: {}, 耗时: {}ms", 
                                    jobId, success, duration);
                            
                            // 上报结果状态（传递 parentJobId）
                            int status = success ? 1 : 0;
                            reportStatus(parentJobId, jobId, randomId, status, 
                                    success ? "任务执行成功" : "任务执行失败");
                        }
                    });
            
            result.add(worker);
        }
        
        // 构建节点间的依赖关系
        buildDependencies(result, nodes, edges);
        
        logger.debug("[JobGroupExecutor] WorkerWrapper构建完成 - 数量: {}", result.size());
        return result;
    }
    
    /**
     * 执行任务（完整实现）
     * 
     * <p>包括：
     * <ul>
     *   <li>1. 检查暂停状态</li>
     *   <li>2. 触发任务执行</li>
     *   <li>3. 监听任务执行状态</li>
     *   <li>4. 处理超时和重试</li>
     * </ul>
     */
    private String executeTaskComplete(XxlJobContext xxlJobContext, JobNode node, JobInfo jobInfo, 
                                      Long parentJobId, String randomId, int retryCount) {
        logger.info("[JobGroupExecutor] 开始执行任务 - jobId: {}, nodeId: {}, 重试次数: {}, 任务名称: {}",
                jobInfo.getId(), node.getId(), retryCount, jobInfo.getJobDesc());
        
        try {
            // 1. 处理暂停逻辑
            handlePause(jobInfo);
            
            // 2. 触发任务执行
            boolean triggerSuccess = jobTriggerService.triggerJob(xxlJobContext, jobInfo, randomId);
            if (!triggerSuccess) {
                logger.error("[JobGroupExecutor] 任务触发失败 - jobId: {}", jobInfo.getId());
                
                // 判断是否需要抛出异常
                if (!JobConstant.DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
                    return JobConstant.FAIL_RETRY;
                } else {
                    logger.warn("[JobGroupExecutor] 任务触发失败但忽略继续执行 - jobId: {}", jobInfo.getId());
                    return JobConstant.SUCCESS;
                }
            }
            
            // 3. 监听任务执行状态
            return monitorTaskExecution(node, jobInfo, parentJobId, randomId, retryCount);
            
        } catch (Exception e) {
            logger.error("[JobGroupExecutor] 任务执行异常 - jobId: {}, nodeId: {}", 
                    jobInfo.getId(), node.getId(), e);
            
            // 上报失败状态（传递 parentJobId）
            reportStatus(parentJobId, jobInfo.getId(), randomId, 0, "任务执行异常: " + e.getMessage());
            
            // 判断是否需要抛出异常
            if (!JobConstant.DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
                throw new RuntimeException(e);
            } else {
                logger.warn("[JobGroupExecutor] 任务执行异常但忽略继续执行 - jobId: {}", jobInfo.getId());
                return JobConstant.SUCCESS;
            }
        }
    }
    
    /**
     * 处理任务暂停
     */
    private void handlePause(JobInfo jobInfo) {
        // 重新获取 jobInfo，检查是否处于暂停状态
        JobInfo latestJobInfo = adminApiClient.getJobInfo(jobInfo.getId());
        if (latestJobInfo == null) {
            logger.warn("[JobGroupExecutor] 无法获取最新任务信息 - jobId: {}", jobInfo.getId());
            return;
        }
        
        boolean isPause = latestJobInfo.getIsPause() != null && latestJobInfo.getIsPause() == 1;
        
        if (isPause) {
            logger.info("[JobGroupExecutor] 任务处于暂停状态，等待恢复 - jobId: {}, 任务名称: {}",
                    jobInfo.getId(), jobInfo.getJobDesc());
        }
        
        // 暂停任务，默认最多等待5分钟或任务超时时间
        long timeout = jobInfo.getExecutorTimeout() > 0 
                ? jobInfo.getExecutorTimeout() * 1000 
                : 5 * 60 * 1000;
        long startTime = System.currentTimeMillis();
        
        while (isPause) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= timeout) {
                logger.warn("[JobGroupExecutor] 任务暂停等待超时 - jobId: {}, 等待时长: {}ms", 
                        jobInfo.getId(), elapsed);
                break;
            }
            
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.error("[JobGroupExecutor] 任务暂停等待被中断 - jobId: {}", jobInfo.getId(), e);
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            
            // 重新检查暂停状态
            JobInfo checkJobInfo = adminApiClient.getJobInfo(jobInfo.getId());
            if (checkJobInfo != null) {
                isPause = checkJobInfo.getIsPause() != null && checkJobInfo.getIsPause() == 1;
            }
        }
        
        if (!isPause) {
            logger.info("[JobGroupExecutor] 任务恢复执行 - jobId: {}, 任务名称: {}", 
                    jobInfo.getId(), jobInfo.getJobDesc());
        }
    }
    
    /**
     * 监听任务执行状态
     */
    private String monitorTaskExecution(JobNode node, JobInfo jobInfo, Long parentJobId, String randomId, int retryCount) {
        logger.debug("[JobGroupExecutor] 开始监听任务执行状态 - jobId: {}, nodeId: {}, 超时: {}秒",
                jobInfo.getId(), node.getId(), jobInfo.getExecutorTimeout());
        
        String result = null;
        Thread monitorThread = null;
        JobExecutionMonitor monitor = null;
        
        try {
            monitor = new JobExecutionMonitor(jobInfo, node, randomId, JOB_RESULTS, retryCount);
            FutureTask<String> futureTask = new FutureTask<>(monitor);
            monitorThread = new Thread(futureTask);
            monitorThread.start();
            
            // 等待任务完成，支持超时
            if (jobInfo.getExecutorTimeout() > 0) {
                result = futureTask.get(jobInfo.getExecutorTimeout(), TimeUnit.SECONDS);
            } else {
                result = futureTask.get();
            }
            
            logger.info("[JobGroupExecutor] 任务监听完成 - jobId: {}, nodeId: {}, 结果: {}",
                    jobInfo.getId(), node.getId(), result);
                    
        } catch (TimeoutException e) {
            logger.error("[JobGroupExecutor] 任务执行超时 - jobId: {}, nodeId: {}, 超时时间: {}秒",
                    jobInfo.getId(), node.getId(), jobInfo.getExecutorTimeout());
            
            // 上报超时状态（传递 parentJobId）
            reportStatus(node.getJobParentId(), jobInfo.getId(), randomId, 0, "任务执行超时");
            
            // 判断是否需要抛出异常
            if (!JobConstant.DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
                throw new RuntimeException("任务执行超时");
            } else {
                logger.warn("[JobGroupExecutor] 任务执行超时但忽略继续执行 - jobId: {}", jobInfo.getId());
                return JobConstant.SUCCESS;
            }
            
        } catch (Exception e) {
            logger.error("[JobGroupExecutor] 任务监听异常 - jobId: {}, nodeId: {}",
                    jobInfo.getId(), node.getId(), e);
            
            // 上报失败状态（传递 parentJobId）
            reportStatus(parentJobId, jobInfo.getId(), randomId, 0, "任务监听异常");
            
            // 判断是否需要抛出异常
            if (!JobConstant.DO_NOTHING.equalsIgnoreCase(jobInfo.getExecutorBlockStrategy())) {
                throw new RuntimeException(e);
            } else {
                logger.warn("[JobGroupExecutor] 任务监听异常但忽略继续执行 - jobId: {}", jobInfo.getId());
                return JobConstant.SUCCESS;
            }
            
        } finally {
            if (monitor != null) {
                monitor.stopMonitoring();
            }
            if (monitorThread != null) {
                monitorThread.interrupt();
            }
        }
        
        return result != null ? result : JobConstant.SUCCESS;
    }
    
    /**
     * 上报任务执行状态（带 parentJobId）
     * 
     * @param parentJobId 父任务ID（任务组ID）
     * @param jobId 任务ID（子任务ID）
     * @param randomId 批次ID
     * @param status 状态（0=失败，1=成功，2=执行中，5=完成）
     * @param message 状态消息
     */
    private void reportStatus(Long parentJobId, Long jobId, String randomId, Integer status, String message) {
        try {
            adminApiClient.reportStatus(parentJobId, jobId, randomId, status, message);
        } catch (Exception e) {
            logger.error("[JobGroupExecutor] 上报状态失败 - parentJobId: {}, jobId: {}, status: {}", 
                    parentJobId, jobId, status, e);
        }
    }
    
    /**
     * 获取任务信息映射
     */
    private Map<Long, JobInfo> getJobInfoMap(List<JobNode> nodes) {
        Map<Long, JobInfo> jobInfoMap = new HashMap<>();
        List<Long> jobIds = nodes.stream().map(JobNode::getJobId).distinct().toList();
        
        for (Long jobId : jobIds) {
            JobInfo jobInfo = adminApiClient.getJobInfo(jobId);
            if (jobInfo != null) {
                jobInfoMap.put(jobId, jobInfo);
            }
        }
        
        return jobInfoMap;
    }
    
    /**
     * 构建任务图（计算入度和出度）
     */
    private void buildGraph(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
        logger.debug("[JobGroupExecutor] 开始构建任务图 - jobId: {}", jobId);
        
        Map<Long, List<JobEdge>> edgesByFrom = new HashMap<>();
        Map<Long, List<JobEdge>> edgesByTo = new HashMap<>();
        
        for (JobEdge edge : edges) {
            edgesByFrom.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>()).add(edge);
            edgesByTo.computeIfAbsent(edge.getEndNodeId(), k -> new ArrayList<>()).add(edge);
        }
        
        for (JobNode node : nodes) {
            long inDegree = edgesByTo.getOrDefault(node.getId(), Collections.emptyList()).size();
            long outDegree = edgesByFrom.getOrDefault(node.getId(), Collections.emptyList()).size();
            node.setNodeInDegree(inDegree);
            node.setNodeOutDegree(outDegree);
            node.setJobParentId(jobId);
        }
        
        logger.debug("[JobGroupExecutor] 任务图构建完成 - 节点数: {}, 边数: {}", nodes.size(), edges.size());
    }
    
    /**
     * 构建依赖关系
     */
    private void buildDependencies(List<WorkerWrapper<Long, String>> workerWrappers, 
                                  List<JobNode> nodes, List<JobEdge> edges) {
        logger.info("[JobGroupExecutor] ========== 开始构建依赖关系 ==========");
        logger.info("[JobGroupExecutor] 节点数: {}, 边数: {}", nodes.size(), edges.size());
        
        // 打印所有边
        for (JobEdge edge : edges) {
            logger.info("[JobGroupExecutor] 边: {} -> {}", edge.getFromNodeId(), edge.getEndNodeId());
        }
        
        Map<Long, List<Long>> nextMap = new HashMap<>();
        
        // 构建 nextMap：节点ID -> 后续节点ID列表
        for (JobNode node : nodes) {
            List<Long> nextNodeIds = edges.stream()
                    .filter(e -> e.getFromNodeId().equals(node.getId()))
                    .map(JobEdge::getEndNodeId)
                    .toList();
            nextMap.put(node.getId(), nextNodeIds);
            
            if (!nextNodeIds.isEmpty()) {
                logger.info("[JobGroupExecutor] 节点: {} 的后续节点: {}", node.getId(), nextNodeIds);
            }
        }
        
        // 为每个 WorkerWrapper 设置后续任务
        for (WorkerWrapper<Long, String> wrapper : workerWrappers) {
            Long nodeId = Long.valueOf(wrapper.getId());
            List<Long> nextNodeIds = nextMap.get(nodeId);
            
            logger.debug("[JobGroupExecutor] 处理WorkerWrapper: {}, 后续节点IDs: {}", 
                    nodeId, nextNodeIds);
            
            if (nextNodeIds != null && !nextNodeIds.isEmpty()) {
                List<WorkerWrapper<Long, String>> nextWorkers = workerWrappers.stream()
                        .filter(w -> nextNodeIds.contains(Long.valueOf(w.getId())))
                        .toList();
                
                logger.info("[JobGroupExecutor] 节点: {} 找到 {} 个后续WorkerWrapper", 
                        nodeId, nextWorkers.size());
                
                if (!nextWorkers.isEmpty()) {
                    wrapper.next(nextWorkers.toArray(new WorkerWrapper[0]));
                    
                    // 打印设置的依赖关系
                    for (WorkerWrapper<Long, String> nextWorker : nextWorkers) {
                        logger.info("[JobGroupExecutor] 设置依赖: {} -> {}", nodeId, nextWorker.getId());
                    }
                }
            } else {
                logger.info("[JobGroupExecutor] 节点: {} 没有后续节点（可能是终点）", nodeId);
            }
        }
        
        // 验证依赖关系
        logger.info("[JobGroupExecutor] ========== 依赖关系验证 ==========");
        for (WorkerWrapper<Long, String> wrapper : workerWrappers) {
            int dependCount = wrapper.getDependWrappers() != null ? wrapper.getDependWrappers().size() : 0;
            int nextCount = wrapper.getNextWrappers() != null ? wrapper.getNextWrappers().size() : 0;
            logger.info("[JobGroupExecutor] WorkerWrapper: {}, 依赖数: {}, 后续数: {}", 
                    wrapper.getId(), dependCount, nextCount);
        }
        
        logger.info("[JobGroupExecutor] ========== 依赖关系构建完成 ==========");
    }
    
    /**
     * 清理资源
     */
    private void cleanup(long jobId, String randomId) {
        logger.info("[JobGroupExecutor] ========== 开始清理资源 ==========");
        logger.info("[JobGroupExecutor] 任务ID: {}, 随机ID: {}", jobId, randomId);
        
        String executeKey = buildExecuteKey(jobId, randomId);
        
        // 移除运行中的任务
        RUNNING_JOBS.remove(executeKey);
        
        // 清理结果映射
        JOB_RESULTS.entrySet().removeIf(entry -> entry.getKey().startsWith(jobId + ":"));
        
        // 上报完成状态（任务组本身，parentJobId = jobId）
        reportStatus(jobId, jobId, randomId, 5, "任务组执行完成");
        
        // 清理线程本地变量
        CONTEXT_HOLDER.remove();
        
        logger.info("[JobGroupExecutor] 资源清理完成 - jobId: {}, 剩余任务组数量: {}", 
                jobId, RUNNING_JOBS.size());
    }
    
    /**
     * 停止任务组执行
     */
    public void stopJobGroup(Long jobId, String randomId) {
        String executeKey = buildExecuteKey(jobId, randomId);
        List<WorkerWrapper<Long, String>> workerWrappers = RUNNING_JOBS.get(executeKey);
        
        if (workerWrappers != null) {
            logger.info("[JobGroupExecutor] 停止任务组 - jobId: {}, randomId: {}", jobId, randomId);
            Async.stopWork((List<WorkerWrapper>) (List<?>) workerWrappers);
            RUNNING_JOBS.remove(executeKey);
            
            // 上报停止状态（任务组本身，parentJobId = jobId）
            reportStatus(jobId, jobId, randomId, 0, "任务组已被停止");
        } else {
            logger.warn("[JobGroupExecutor] 任务组不存在或已完成 - jobId: {}, randomId: {}", jobId, randomId);
        }
    }
    
    /**
     * 查询任务组是否正在运行
     * 
     * @param jobId 任务组ID
     * @param randomId 批次ID
     * @return 是否正在运行
     */
    public boolean isJobGroupRunning(Long jobId, String randomId) {
        String executeKey = buildExecuteKey(jobId, randomId);
        return RUNNING_JOBS.containsKey(executeKey);
    }
    
    /**
     * 获取所有运行中的任务组
     * 
     * @return 运行中的任务组映射 (executeKey -> isRunning)
     */
    public Map<String, Boolean> getAllRunningJobGroups() {
        Map<String, Boolean> result = new HashMap<>();
        for (String executeKey : RUNNING_JOBS.keySet()) {
            result.put(executeKey, true);
        }
        logger.debug("[JobGroupExecutor] 当前运行中的任务组数量: {}", result.size());
        return result;
    }
    
    /**
     * 记录任务执行结果
     */
    public static void addJobResult(String executeKey, Boolean success) {
        JOB_RESULTS.put(executeKey, success);
        logger.debug("[JobGroupExecutor] 记录任务结果 - key: {}, 成功: {}", executeKey, success);
    }
    
    /**
     * 移除任务执行结果
     */
    public static void removeJobResult(String executeKey) {
        JOB_RESULTS.remove(executeKey);
        logger.debug("[JobGroupExecutor] 移除任务结果 - key: {}", executeKey);
    }
    
    /**
     * 构建执行键
     */
    private static String buildExecuteKey(Long jobId, String randomId) {
        return jobId + ":" + randomId;
    }
}

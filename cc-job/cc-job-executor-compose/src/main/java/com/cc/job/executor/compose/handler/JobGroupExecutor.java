package com.cc.job.executor.compose.handler;

import cn.hutool.json.JSONUtil;
import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.engine.Async;
import com.cc.job.executor.compose.engine.callback.ICallback;
import com.cc.job.executor.compose.engine.callback.IWorker;
import com.cc.job.executor.compose.engine.constant.JobConstant;
import com.cc.job.executor.compose.engine.worker.WorkResult;
import com.cc.job.executor.compose.engine.wrapper.WorkerWrapper;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 任务组编排执行器
 * 
 * <p>职责：
 * <ul>
 *   <li>1. 接收任务组执行请求</li>
 *   <li>2. 从 Admin 获取任务组信息（节点和边）</li>
 *   <li>3. 构建任务执行图</li>
 *   <li>4. 执行拓扑排序编排</li>
 *   <li>5. 监控执行状态并上报</li>
 * </ul>
 * 
 * @author xiaozhao
 */
@Component
@RequiredArgsConstructor
public class JobGroupExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(JobGroupExecutor.class);
    
    private final AdminApiClient adminApiClient;
    
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
     * 任务组执行入口
     * 
     * <p>执行流程：
     * <ol>
     *   <li>1. 解析执行参数，生成批次ID</li>
     *   <li>2. 从 Admin 获取任务组信息</li>
     *   <li>3. 构建任务执行图（节点 + 边）</li>
     *   <li>4. 创建 WorkerWrapper 列表</li>
     *   <li>5. 调用 Async 引擎执行拓扑排序</li>
     *   <li>6. 等待执行完成，清理资源</li>
     * </ol>
     */
    @XxlJob("runJobGroupHandler")
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
            
            // 5. 构建 WorkerWrapper 列表
            List<WorkerWrapper<Long, String>> workerWrappers = buildWorkerWrappers(
                    nodes, edges, jobId, randomId, jobInfo);
            
            if (workerWrappers.isEmpty()) {
                logger.warn("[JobGroupExecutor] 没有可执行的任务");
                XxlJobHelper.handleSuccess("没有可执行的任务");
                return;
            }
            
            // 6. 保存到运行中的任务映射
            String executeKey = buildExecuteKey(jobId, randomId);
            RUNNING_JOBS.put(executeKey, workerWrappers);
            
            logger.info("[JobGroupExecutor] 开始执行任务组 - 任务数: {}, 超时: {}秒", 
                    workerWrappers.size(), jobInfo.getExecutorTimeout());
            
            // 7. 调用 Async 引擎执行
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
            // 8. 清理资源
            String executeKey = buildExecuteKey(jobId, randomId);
            RUNNING_JOBS.remove(executeKey);
            JOB_RESULTS.remove(executeKey);
            
            // 上报完成状态
            adminApiClient.reportStatus(jobId, randomId, 5, "任务组执行完成");
            
            logger.info("[JobGroupExecutor] 资源清理完成 - jobId: {}, randomId: {}", jobId, randomId);
        }
    }
    
    /**
     * 构建任务图（计算入度和出度）
     */
    private void buildGraph(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
        logger.debug("[JobGroupExecutor] 开始构建任务图 - jobId: {}", jobId);
        
        // 构建边的映射
        Map<Long, List<JobEdge>> edgesByFrom = new HashMap<>();
        Map<Long, List<JobEdge>> edgesByTo = new HashMap<>();
        
        for (JobEdge edge : edges) {
            edgesByFrom.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>()).add(edge);
            edgesByTo.computeIfAbsent(edge.getEndNodeId(), k -> new ArrayList<>()).add(edge);
        }
        
        // 计算每个节点的入度和出度
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
     * 构建 WorkerWrapper 列表
     */
    private List<WorkerWrapper<Long, String>> buildWorkerWrappers(
            List<JobNode> nodes, List<JobEdge> edges, Long parentJobId, String randomId, JobInfo parentJobInfo) {
        
        logger.debug("[JobGroupExecutor] 开始构建 WorkerWrapper");
        
        List<WorkerWrapper<Long, String>> result = new ArrayList<>();
        
        // 获取所有节点对应的任务信息
        List<Long> jobIds = nodes.stream().map(JobNode::getJobId).toList();
        Map<Long, JobInfo> jobInfoMap = new HashMap<>();
        for (Long jobId : jobIds) {
            JobInfo jobInfo = adminApiClient.getJobInfo(jobId);
            if (jobInfo != null) {
                jobInfoMap.put(jobId, jobInfo);
            }
        }
        
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
                            // 执行单个任务
                            return executeTask(jobId, node, randomId);
                        }
                        
                        @Override
                        public String defaultValue() {
                            return JobConstant.FAIL_COMPLETE;
                        }
                    })
                    .callback(new ICallback<Long, String>() {
                        @Override
                        public void begin(Long jobId) {
                            logger.info("[JobGroupExecutor] 任务开始 - jobId: {}, nodeId: {}", jobId, node.getId());
                            // 上报开始状态
                            adminApiClient.reportStatus(jobId, randomId, 2, "任务开始执行");
                        }
                        
                        @Override
                        public void result(boolean success, Long jobId, WorkResult<String> workResult) {
                            logger.info("[JobGroupExecutor] 任务完成 - jobId: {}, nodeId: {}, 成功: {}", 
                                    jobId, node.getId(), success);
                            // 上报结果状态
                            int status = success ? 1 : 0;
                            adminApiClient.reportStatus(jobId, randomId, status, 
                                    success ? "任务执行成功" : "任务执行失败");
                        }
                    });
            
            result.add(worker);
        }
        
        // 构建节点间的依赖关系
        Map<Long, List<JobNode>> nextMap = buildNextMap(nodes, edges);
        for (WorkerWrapper<Long, String> wrapper : result) {
            Long nodeId = Long.valueOf(wrapper.getId());
            List<JobNode> nextNodes = nextMap.get(nodeId);
            if (nextNodes != null && !nextNodes.isEmpty()) {
                List<Long> nextNodeIds = nextNodes.stream().map(JobNode::getId).toList();
                List<WorkerWrapper<Long, String>> nextWorkers = result.stream()
                        .filter(w -> nextNodeIds.contains(Long.valueOf(w.getId())))
                        .toList();
                if (!nextWorkers.isEmpty()) {
                    wrapper.next(nextWorkers.toArray(new WorkerWrapper[0]));
                }
            }
        }
        
        logger.debug("[JobGroupExecutor] WorkerWrapper构建完成 - 数量: {}", result.size());
        return result;
    }
    
    /**
     * 构建节点的后继节点映射
     */
    private Map<Long, List<JobNode>> buildNextMap(List<JobNode> nodes, List<JobEdge> edges) {
        Map<Long, List<JobNode>> result = new HashMap<>();
        
        for (JobNode node : nodes) {
            List<Long> nextNodeIds = edges.stream()
                    .filter(e -> e.getFromNodeId().equals(node.getId()))
                    .map(JobEdge::getEndNodeId)
                    .toList();
            
            List<JobNode> nextNodes = nodes.stream()
                    .filter(n -> nextNodeIds.contains(n.getId()))
                    .toList();
            
            result.put(node.getId(), nextNodes);
        }
        
        return result;
    }
    
    /**
     * 执行单个任务
     * 
     * <p>这里是简化实现，实际应该：
     * <ul>
     *   <li>1. 根据任务类型调用不同的执行器（Bean、Shell、HTTP、DataX等）</li>
     *   <li>2. 处理暂停、恢复逻辑</li>
     *   <li>3. 实时监控任务状态</li>
     * </ul>
     * 
     * <p>TODO: 完整实现需要集成 XXL-Job 的任务触发器
     */
    private String executeTask(Long jobId, JobNode node, String randomId) {
        logger.info("[JobGroupExecutor] 执行任务 - jobId: {}, nodeId: {}", jobId, node.getId());
        
        try {
            // TODO: 这里应该调用实际的任务执行逻辑
            // 暂时返回成功，实际应该：
            // 1. 调用 Admin 的任务触发接口
            // 2. 等待任务执行完成
            // 3. 获取执行结果
            
            // 模拟任务执行
            Thread.sleep(100);
            
            // 记录任务结果
            String executeKey = buildExecuteKey(jobId, randomId);
            JOB_RESULTS.put(executeKey, true);
            
            logger.info("[JobGroupExecutor] 任务执行成功 - jobId: {}", jobId);
            return JobConstant.SUCCESS;
            
        } catch (Exception e) {
            logger.error("[JobGroupExecutor] 任务执行失败 - jobId: {}, nodeId: {}", jobId, node.getId(), e);
            
            // 记录任务失败
            String executeKey = buildExecuteKey(jobId, randomId);
            JOB_RESULTS.put(executeKey, false);
            
            return JobConstant.FAIL_RETRY;
        }
    }
    
    /**
     * 构建执行键
     */
    private static String buildExecuteKey(Long jobId, String randomId) {
        return jobId + ":" + randomId;
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
        } else {
            logger.warn("[JobGroupExecutor] 任务组不存在或已完成 - jobId: {}, randomId: {}", jobId, randomId);
        }
    }
}

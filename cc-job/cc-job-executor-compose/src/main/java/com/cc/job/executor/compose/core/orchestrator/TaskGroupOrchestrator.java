package com.cc.job.executor.compose.core.orchestrator;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.context.DataContext;
import com.cc.job.executor.compose.core.context.DataContextManager;
import com.cc.job.executor.compose.core.model.ExecutionContext;
import com.cc.job.executor.compose.core.evaluator.ConditionEvaluator;
import com.cc.job.executor.compose.core.service.HistoricalDataLoader;
import com.cc.job.executor.compose.core.service.ResultStorageService;
import com.cc.job.executor.compose.core.service.TaskDependencyBuilder;
import com.cc.job.executor.compose.core.service.TaskGraphBuilder;
import com.cc.job.executor.compose.core.service.TaskWrapperFactory;
import com.cc.job.executor.compose.engine.Async;
import com.cc.job.executor.compose.engine.wrapper.WorkerWrapper;
import com.cc.job.executor.compose.engine.worker.ResultState;
import com.cc.job.executor.compose.handler.JobGroupUtils;
import com.cc.job.xo.common.BaseEntity;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.ArrayDeque;

/**
 * 任务组编排器
 *
 * <p>负责任务组的编排和执行
 *
 * @author cc-job-team
 */
@Component
public class TaskGroupOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(TaskGroupOrchestrator.class);

    private static final String CONDITION_NODE = "ConditionNode";

    private final AdminApiClient adminApiClient;
    private final TaskGraphBuilder graphBuilder;
    private final TaskWrapperFactory wrapperFactory;
    private final TaskDependencyBuilder dependencyBuilder;
    private final JobGroupUtils jobGroupUtils;
    private final DataContextManager dataContextManager;
    private final ResultStorageService resultStorageService;
    private final HistoricalDataLoader historicalDataLoader;
    private final ConditionEvaluator conditionEvaluator;

    public TaskGroupOrchestrator(AdminApiClient adminApiClient, TaskGraphBuilder graphBuilder, TaskWrapperFactory wrapperFactory, TaskDependencyBuilder dependencyBuilder, JobGroupUtils jobGroupUtils, DataContextManager dataContextManager, ResultStorageService resultStorageService, HistoricalDataLoader historicalDataLoader, ConditionEvaluator conditionEvaluator) {
        this.adminApiClient = adminApiClient;
        this.graphBuilder = graphBuilder;
        this.wrapperFactory = wrapperFactory;
        this.dependencyBuilder = dependencyBuilder;
        this.jobGroupUtils = jobGroupUtils;
        this.dataContextManager = dataContextManager;
        this.resultStorageService = resultStorageService;
        this.historicalDataLoader = historicalDataLoader;
        this.conditionEvaluator = conditionEvaluator;
    }

    /**
     * 存储正在执行的任务组
     */
    private static final Map<String, List<WorkerWrapper<Long, String>>> RUNNING_JOBS = new ConcurrentHashMap<>();

    /**
     * 存储执行上下文，用于在任务组完成时收集节点状态
     */
    private static final Map<String, ExecutionContext> CONTEXT_MAP = new ConcurrentHashMap<>();

    /**
     * 线程本地变量，用于存储 XxlJobContext
     */
    private static final InheritableThreadLocal<XxlJobContext> CONTEXT_HOLDER = new InheritableThreadLocal<>();

    @Value("${cc-job.job.executor.ip}")
    private String ip;

    @Value("${cc-job.job.executor.port}")
    private int executorPort;

    @Value("${server.port:8500}")
    private int httpPort;

    @Value("${cc-job.job.executor.appname}")
    private String appname;

    @Value("${cc-job.job.admin.addresses}")
    private String adminAddresses;

    public boolean updateRegistryWithHttpPort() {
        try {
            // 构建执行器地址（Netty端口）
            String executorIp = (ip != null && !ip.trim().isEmpty()) ? ip : IpUtil.getIp();
            String executorAddress = "http://" + executorIp + ":" + executorPort + "/";

            String executorServerAddress = "http://" + executorIp + ":" + httpPort + "/";

            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("appName", appname);
            params.put("executorAddress", executorAddress);
            params.put("executorServerAddress", executorServerAddress);

            // 调用admin接口更新注册信息
            // adminAddresses格式可能是 "http://127.0.0.1:8989/xxl-job-admin,http://127.0.0.1:8990/xxl-job-admin"
            String[] adminAddressArray = adminAddresses.split(",");
            for (String adminAddress : adminAddressArray) {
                try {
                    // 确保adminAddress以/结尾
                    String baseUrl = adminAddress.trim();
                    if (!baseUrl.endsWith("/")) {
                        baseUrl += "/";
                    }
                    String url = baseUrl + "api/updateRegistryValue";

                    HttpResponse response = HttpRequest.post(url)
                            .header("Content-Type", "application/json")
                            .body(JSONUtil.toJsonStr(params))
                            .timeout(5000)
                            .execute();

                    if (response.isOk()) {
                        logger.info("成功更新注册信息 - executorAddress: {}, httpPort: {}", executorAddress, httpPort);
                        return true; // 成功则返回true
                    } else {
                        logger.debug("更新注册信息失败 - adminAddress: {}, status: {}, body: {}",
                                adminAddress, response.getStatus(), response.body());
                    }
                } catch (Exception e) {
                    logger.debug("调用admin接口更新注册信息失败 - adminAddress: {}, error: {}", adminAddress, e.getMessage());
                }
            }
            return false; // 所有admin地址都失败
        } catch (Exception e) {
            logger.error("更新注册信息异常", e);
            return false;
        }
    }

    /*
     * 执行任务组（带执行参数）
     *
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID
     * @param executeParam 执行参数（可能包含batchId参数）
     */
    public void execute(Long taskGroupId, String executionBatchId, List<Integer> jobFlowPositionIds, String executeParam) {
        logger.info("[Orchestrator] ========== 开始执行任务组 ==========");
        logger.info("[Orchestrator] 任务组ID: {}, 批次ID: {}, 执行参数: {}", taskGroupId, executionBatchId, executeParam);

        try {
            // 1. 准备执行上下文
            ExecutionContext context = prepareExecution(taskGroupId, executionBatchId, jobFlowPositionIds, executeParam);

            // 2. 构建执行计划
            List<WorkerWrapper<Long, String>> workerWrappers = buildExecutionPlan(context);

            // 3. 执行任务组
            executeTaskGroup(context, workerWrappers);

            logger.info("[Orchestrator] ========== 任务组执行完成 ==========");
            XxlJobHelper.handleSuccess("任务组执行完成");

        } catch (ExecutionException e) {
            handleExecutionException(taskGroupId, executionBatchId, e);
        } catch (Exception e) {
            handleGeneralException(taskGroupId, executionBatchId, e);
        } finally {
            cleanup(taskGroupId, executionBatchId);
        }
    }

    /**
     * 准备执行上下文
     */
    private ExecutionContext prepareExecution(Long taskGroupId, String executionBatchId, List<Integer> jobFlowPositionIds, String executeParam) {
        logger.debug("[Orchestrator] 准备执行上下文 - taskGroupId: {}", taskGroupId);

        // 获取任务组信息
        JobInfo taskGroupInfo = adminApiClient.getJobInfo(taskGroupId);
        if (taskGroupInfo == null) {
            throw new RuntimeException("获取任务组信息失败");
        }

        // 获取节点和边
        List<JobNode> nodes = adminApiClient.getJobNodes(taskGroupId);
        List<JobEdge> edges = adminApiClient.getJobEdges(taskGroupId);

        if (nodes.isEmpty()) {
            logger.warn("[Orchestrator] 任务组没有节点 - taskGroupId: {}", taskGroupId);
            XxlJobHelper.handleSuccess("任务组没有节点，跳过执行");
            throw new RuntimeException("任务组没有节点");
        }

        // 保存全量节点和边（用于部分跑时计算上游与补全缺失）
        List<JobNode> allNodes = new ArrayList<>(nodes);
        List<JobEdge> allEdges = new ArrayList<>(edges);
        Map<String, Long> fullJobNameMap = buildJobNameMap(nodes);

        // 过滤节点和边
        if (jobFlowPositionIds != null && !jobFlowPositionIds.isEmpty()) {
            nodes = nodes.stream().filter(n -> jobFlowPositionIds.contains(n.getJobId().intValue())).toList();
            if (!nodes.isEmpty()) {
                //过滤边
                List<Long> nodeIds = nodes.stream().map(BaseEntity::getId).toList();
                edges = edges.stream().filter(e -> nodeIds.contains(e.getFromNodeId()) && nodeIds.contains(e.getEndNodeId())).toList();
            }
        }

        logger.info("[Orchestrator] 获取到 {} 个节点，{} 条边", nodes.size(), edges.size());

        // ⭐ 新增：过滤条件不满足的条件节点内的任务
        // 需要在构建任务图之前进行，因为条件节点内的任务不应该参与图构建
        DataContext dataContext = dataContextManager.getContext(executionBatchId);
        Map<String, Long> jobNameMap = buildJobNameMap(nodes);
        nodes = filterNodesByCondition(nodes, dataContext, jobNameMap, taskGroupId);
        // 重新过滤边，只保留过滤后节点之间的边
        List<Long> filteredNodeIds = nodes.stream().map(BaseEntity::getId).collect(Collectors.toList());
        edges = edges.stream()
                .filter(e -> filteredNodeIds.contains(e.getFromNodeId()) && filteredNodeIds.contains(e.getEndNodeId()))
                .collect(Collectors.toList());

        logger.info("[Orchestrator] 条件过滤后剩余 {} 个节点，{} 条边", nodes.size(), edges.size());

        // 构建任务图
        graphBuilder.buildGraph(taskGroupId, nodes, edges);

        // 创建或获取数据上下文（如果之前没有创建）
        if (dataContext == null) {
            dataContext = dataContextManager.getContext(executionBatchId);
        }
        logger.debug("[Orchestrator] 获取数据上下文 - batchId: {}", executionBatchId);

        // 构建 jobName 映射（jobDesc -> jobId）（如果之前没有构建）
        if (jobNameMap.isEmpty()) {
            jobNameMap = buildJobNameMap(nodes);
            logger.debug("[Orchestrator] 构建 jobName 映射 - 数量: {}", jobNameMap.size());
        }

        // 解析执行参数，加载历史数据（如果指定了batchId）
        String historicalBatchId = parseBatchIdFromParam(executeParam);
        ExecutionContext tempContext = new ExecutionContext();
        tempContext.setTaskGroupId(taskGroupId);
        tempContext.setExecutionBatchId(executionBatchId);
        tempContext.setDataContext(dataContext);
        tempContext.setJobNameMap(jobNameMap);
        if (historicalBatchId != null && !historicalBatchId.isEmpty()) {
            logger.info("[Orchestrator] 检测到历史批次ID参数，开始加载历史数据 - batchId: {}", historicalBatchId);
            int loadedCount = historicalDataLoader.loadFromDatabase(tempContext, taskGroupId, historicalBatchId);
            logger.info("[Orchestrator] 历史数据加载完成 - batchId: {}, 加载数量: {}", historicalBatchId, loadedCount);
        } else {
            // 部分跑时优先用最近一次全量跑批次，保证上游 #参数 有数据（方案一）
            boolean isPartialRun = jobFlowPositionIds != null && !jobFlowPositionIds.isEmpty();
            if (isPartialRun) {
                String fullRunBatchId = adminApiClient.getLatestFullRunBatchId(taskGroupId);
                if (fullRunBatchId != null && !fullRunBatchId.isEmpty()) {
                    logger.info("[Orchestrator] 部分跑检测到全量跑批次，加载其历史数据 - batchId: {}", fullRunBatchId);
                    int loadedCount = historicalDataLoader.loadFromDatabase(tempContext, taskGroupId, fullRunBatchId);
                    logger.info("[Orchestrator] 全量跑批次历史数据加载完成 - 加载数量: {}", loadedCount);
                } else {
                    logger.debug("[Orchestrator] 无全量跑批次，回退到最近一次执行数据");
                    int loadedCount = historicalDataLoader.loadLatestBatch(tempContext, taskGroupId);
                    if (loadedCount > 0) {
                        logger.info("[Orchestrator] 最近一次执行数据加载完成 - 加载数量: {}", loadedCount);
                    }
                }
            } else {
                logger.debug("[Orchestrator] 未指定历史批次ID，尝试加载最近一次执行的数据");
                int loadedCount = historicalDataLoader.loadLatestBatch(tempContext, taskGroupId);
                if (loadedCount > 0) {
                    logger.info("[Orchestrator] 最近一次执行数据加载完成 - 加载数量: {}", loadedCount);
                }
            }
        }
        // 部分跑时按节点补全缺失上游结果（方案二）
        if (jobFlowPositionIds != null && !jobFlowPositionIds.isEmpty() && fullJobNameMap != null && !fullJobNameMap.isEmpty()) {
            Set<Long> currentRunNodeIds = nodes.stream().map(BaseEntity::getId).collect(Collectors.toSet());
            Set<Long> ancestorNodeIds = computeAncestorNodeIds(allEdges, currentRunNodeIds);
            Map<Long, String> ancestorJobIdToJobName = new HashMap<>();
            for (JobNode an : allNodes) {
                if (!ancestorNodeIds.contains(an.getId())) {
                    continue;
                }
                Long jid = an.getJobId();
                for (Map.Entry<String, Long> e : fullJobNameMap.entrySet()) {
                    if (e.getValue().equals(jid)) {
                        ancestorJobIdToJobName.put(jid, e.getKey());
                        break;
                    }
                }
                if (!ancestorJobIdToJobName.containsKey(jid)) {
                    ancestorJobIdToJobName.put(jid, null);
                }
            }
            if (!ancestorJobIdToJobName.isEmpty()) {
                int filled = historicalDataLoader.fillMissingAncestors(tempContext, taskGroupId, ancestorJobIdToJobName);
                if (filled > 0) {
                    logger.info("[Orchestrator] 补全缺失上游节点结果 - 数量: {}", filled);
                }
            }
        }

        // 保存上下文
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        CONTEXT_HOLDER.set(xxlJobContext);

        return ExecutionContext.builder()
                .taskGroupId(taskGroupId)
                .executionBatchId(executionBatchId)
                .taskGroupInfo(taskGroupInfo)
                .nodes(nodes)
                .edges(edges)
                .xxlJobContext(xxlJobContext)
                .executeKey(buildExecuteKey(taskGroupId, executionBatchId))
                .dataContext(dataContext)
                .jobNameMap(jobNameMap)
                .build();
    }

    /**
     * 构建 jobName 映射
     *
     * <p>从节点列表中获取所有任务的 jobDesc，构建 jobDesc -> jobId 的映射
     *
     * @param nodes 节点列表
     * @return jobName 映射
     */
    private Map<String, Long> buildJobNameMap(List<JobNode> nodes) {
        Map<String, Long> jobNameMap = new HashMap<>();

        // 获取所有节点的 jobId
        List<Long> jobIds = nodes.stream()
                .map(JobNode::getJobId)
                .distinct()
                .collect(Collectors.toList());

        if (jobIds.isEmpty()) {
            return jobNameMap;
        }

        // 批量获取任务信息
        List<JobInfo> jobInfos = adminApiClient.getJobInfos(jobIds);

        // 构建映射：jobDesc -> jobId
        for (JobInfo jobInfo : jobInfos) {
            if (jobInfo != null && jobInfo.getJobDesc() != null) {
                String jobDesc = jobInfo.getJobDesc().trim();
                if (!jobDesc.isEmpty()) {
                    // 规范化 jobName（与 ParameterResolver 保持一致）
                    String normalizedJobName = normalizeJobName(jobDesc);
                    jobNameMap.put(normalizedJobName, jobInfo.getId());
                    // 同时保存原始 jobDesc（以防用户使用原始名称）
                    jobNameMap.put(jobDesc, jobInfo.getId());
                }
            }
        }

        logger.debug("[Orchestrator] jobName 映射构建完成 - 数量: {}", jobNameMap.size());
        return jobNameMap;
    }

    /**
     * 规范化任务名称
     *
     * <p>与 ParameterResolver 中的规范化逻辑保持一致
     *
     * @param jobDesc 任务描述
     * @return 规范化后的任务名称
     */
    private String normalizeJobName(String jobDesc) {
        if (jobDesc == null || jobDesc.isEmpty()) {
            return "";
        }

        // 去除前后空格
        String normalized = jobDesc.trim();

        // 将特殊字符替换为下划线（保留中文字符、字母、数字）
        normalized = normalized.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");

        // 去除连续的下划线
        normalized = normalized.replaceAll("_{2,}", "_");

        // 去除开头和结尾的下划线
        normalized = normalized.replaceAll("^_+|_+$", "");

        return normalized;
    }

    /**
     * 计算当前运行节点的上游节点 ID 集合（在完整图中从当前节点沿反向边 BFS）
     *
     * @param allEdges 完整边列表（fromNodeId -> endNodeId）
     * @param currentRunNodeIds 当前运行节点 ID 集合
     * @return 上游节点 ID 集合（不包含 currentRunNodeIds）
     */
    private Set<Long> computeAncestorNodeIds(List<JobEdge> allEdges, Set<Long> currentRunNodeIds) {
        if (allEdges == null || currentRunNodeIds == null || currentRunNodeIds.isEmpty()) {
            return Collections.emptySet();
        }
        Map<Long, List<Long>> reverseAdj = new HashMap<>();
        for (JobEdge e : allEdges) {
            Long from = e.getFromNodeId();
            Long end = e.getEndNodeId();
            if (from == null || end == null) {
                continue;
            }
            reverseAdj.computeIfAbsent(end, k -> new ArrayList<>()).add(from);
        }
        Set<Long> reached = new HashSet<>(currentRunNodeIds);
        ArrayDeque<Long> queue = new ArrayDeque<>(currentRunNodeIds);
        while (!queue.isEmpty()) {
            Long nodeId = queue.poll();
            for (Long pred : reverseAdj.getOrDefault(nodeId, Collections.emptyList())) {
                if (reached.add(pred)) {
                    queue.add(pred);
                }
            }
        }
        reached.removeAll(currentRunNodeIds);
        return reached;
    }

    /**
     * 构建执行计划
     */
    private List<WorkerWrapper<Long, String>> buildExecutionPlan(ExecutionContext context) {
        logger.debug("[Orchestrator] 构建执行计划");

        // 创建 WorkerWrapper
        List<WorkerWrapper<Long, String>> workerWrappers =
                wrapperFactory.createWorkerWrappers(context);

        if (workerWrappers.isEmpty()) {
            throw new RuntimeException("没有可执行的任务");
        }

        // 构建依赖关系
        dependencyBuilder.buildDependencies(workerWrappers, context.getNodes(), context.getEdges());

        // 保存到运行中的任务映射
        RUNNING_JOBS.put(context.getExecuteKey(), workerWrappers);

        // 保存执行上下文，用于在任务组完成时收集节点状态
        CONTEXT_MAP.put(context.getExecuteKey(), context);

        return workerWrappers;
    }

    /**
     * 执行任务组
     */
    private void executeTaskGroup(ExecutionContext context,
                                  List<WorkerWrapper<Long, String>> workerWrappers)
            throws ExecutionException, IOException {
        logger.info("[Orchestrator] 开始执行任务组 - 任务数: {}, 超时: {}秒",
                workerWrappers.size(), context.getTaskGroupInfo().getExecutorTimeout());

        // 预测运行时间
        predictExecutionTime(context, workerWrappers);

        // 调用编排引擎执行
        Async.beginWork(context.getTaskGroupInfo().getExecutorTimeout(),
                (List<WorkerWrapper>) (List<?>) workerWrappers);
    }

    /**
     * 预测执行时间
     */
    private void predictExecutionTime(ExecutionContext context,
                                      List<WorkerWrapper<Long, String>> workerWrappers)
            throws IOException {
        logger.debug("[Orchestrator] 开始预测执行时间");

        List<Long> jobIds = context.getNodes().stream()
                .map(JobNode::getJobId)
                .toList();
        List<JobInfo> jobInfos = adminApiClient.getJobInfos(jobIds);

        Map<Long, JobInfo> jobInfoMap = jobInfos.stream()
                .collect(Collectors.toMap(JobInfo::getId, t -> t));

        Map<Long, JobInfo> nodeJobInfoMap = new HashMap<>();
        for (JobNode node : context.getNodes()) {
            nodeJobInfoMap.put(node.getId(), jobInfoMap.get(node.getJobId()));
        }

        List<Long> startNodeIds = context.getNodes().stream()
                .filter(v -> v.getNodeInDegree().equals(0L))
                .map(JobNode::getId)
                .toList();

        String[][] nextRunTime = jobGroupUtils.getNextRunTime(
                workerWrappers, nodeJobInfoMap,
                context.getTaskGroupInfo().getExecutorTimeout(),
                startNodeIds, context.getTaskGroupId());

        adminApiClient.reportStatus(context.getTaskGroupId(), context.getTaskGroupId(),
                context.getExecutionBatchId(), 9, JSONUtil.toJsonStr(nextRunTime));
    }

    /**
     * 处理执行异常
     */
    private void handleExecutionException(Long taskGroupId, String executionBatchId, ExecutionException e) {
        logger.error("[Orchestrator] 任务组执行异常 - taskGroupId: {}", taskGroupId, e);
        XxlJobHelper.handleFail("任务组执行异常: " + e.getMessage());
        updateStatusOnError(taskGroupId);
    }

    /**
     * 处理通用异常
     */
    private void handleGeneralException(Long taskGroupId, String executionBatchId, Exception e) {
        logger.error("[Orchestrator] 任务组执行失败 - taskGroupId: {}", taskGroupId, e);
        XxlJobHelper.handleFail("任务组执行失败: " + e.getMessage());
        updateStatusOnError(taskGroupId);
    }

    /**
     * 错误时更新状态
     */
    private void updateStatusOnError(Long taskGroupId) {
        try {
            adminApiClient.updateRankTriggerStatus(taskGroupId, 0);
        } catch (Exception ex) {
            logger.error("[Orchestrator] 异常情况下更新状态失败 - taskGroupId: {}", taskGroupId, ex);
        }
    }

    /**
     * 清理资源
     */
    private void cleanup(Long taskGroupId, String executionBatchId) {
        logger.info("[Orchestrator] 开始清理资源 - taskGroupId: {}", taskGroupId);

        String executeKey = buildExecuteKey(taskGroupId, executionBatchId);

        // ⚠️ 重要：在移除 RUNNING_JOBS 之前，先获取 WorkerWrapper 列表
        // 因为需要从 WorkerWrapper 中获取节点的执行结果
        List<WorkerWrapper<Long, String>> workerWrappers = RUNNING_JOBS.get(executeKey);

        // 移除运行中的任务
        RUNNING_JOBS.remove(executeKey);

        // 获取执行上下文（用于收集节点状态）
        ExecutionContext context = CONTEXT_MAP.remove(executeKey);

        // ⚠️ 重要：等待一段时间，确保所有子任务的日志都已经写入完成
        // 子任务执行完成后，日志通过异步回调写入，需要给日志写入留出时间
        // 否则前端收到完成状态后会停止日志轮询，导致日志未完全显示
        waitForLogsToFlush();

        // 更新任务组运行状态（包含收集和保存节点状态）
        updateTaskGroupStatus(taskGroupId, executionBatchId, context, workerWrappers);

        // 持久化节点执行结果到数据库
        if (context != null && resultStorageService != null) {
            try {
                int persistedCount = resultStorageService.persistResultToDatabase(context, taskGroupId, executionBatchId);
                logger.info("[Orchestrator] 节点执行结果持久化完成 - taskGroupId: {}, batchId: {}, 数量: {}",
                        taskGroupId, executionBatchId, persistedCount);
            } catch (Exception e) {
                logger.error("[Orchestrator] 持久化节点执行结果失败 - taskGroupId: {}, batchId: {}",
                        taskGroupId, executionBatchId, e);
                // 不抛出异常，避免影响清理流程
            }
        }

        // 清理结果映射（在保存节点状态之后）
        Map<String, Boolean> jobResults = TaskWrapperFactory.getJobResults();
        jobResults.entrySet().removeIf(entry -> entry.getKey().startsWith(taskGroupId + ":"));

        // 销毁数据上下文
        dataContextManager.destroyContext(executionBatchId);
        logger.debug("[Orchestrator] 数据上下文已销毁 - batchId: {}", executionBatchId);

        // 清理线程本地变量
        CONTEXT_HOLDER.remove();

        logger.info("[Orchestrator] 资源清理完成 - 剩余任务组数量: {}", RUNNING_JOBS.size());
    }

    /**
     * 等待日志刷新完成
     *
     * <p>给子任务的异步日志写入留出时间，确保所有日志都已经写入到文件
     * 这样可以避免前端收到完成状态后停止日志轮询，但日志还未完全写入的问题
     *
     * <p>优化：减少延迟时间，因为前端在收到完成状态后会继续轮询一段时间
     */
    private void waitForLogsToFlush() {
        try {
            // 等待1秒，确保大部分子任务的日志回调都已经完成并写入文件
            // 前端在收到完成状态后会继续轮询，所以这里只需要短暂延迟即可
            long waitTime = 1000L; // 1秒（从3秒减少到1秒，减少用户等待时间）
            logger.debug("[Orchestrator] 等待日志刷新完成，延迟 {}ms", waitTime);
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("[Orchestrator] 等待日志刷新被中断", e);
        }
    }

    /**
     * 更新任务组状态
     */
    private void updateTaskGroupStatus(Long taskGroupId, String executionBatchId) {
        updateTaskGroupStatus(taskGroupId, executionBatchId, null, null);
    }

    /**
     * 更新任务组状态（带节点状态收集）
     */
    private void updateTaskGroupStatus(Long taskGroupId, String executionBatchId,
                                       ExecutionContext context, List<WorkerWrapper<Long, String>> workerWrappers) {
        try {
            boolean success = adminApiClient.updateRankTriggerStatus(taskGroupId, 0);
            if (success) {
                logger.info("[Orchestrator] 任务组运行状态已更新 - taskGroupId: {}", taskGroupId);
            } else {
                logger.error("[Orchestrator] 任务组运行状态更新失败 - taskGroupId: {}", taskGroupId);
            }
            // 收集并保存节点执行状态
            if (context != null && workerWrappers != null) {
                collectAndSaveNodeStatus(taskGroupId, executionBatchId, context, workerWrappers);
            }

            // 上报完成状态
            adminApiClient.reportStatus(taskGroupId, taskGroupId, executionBatchId, 5, "任务组执行完成");
        } catch (Exception e) {
            logger.error("[Orchestrator] 更新任务组状态异常 - taskGroupId: {}", taskGroupId, e);
        }
    }

    /**
     * 收集并保存节点执行状态
     */
    private void collectAndSaveNodeStatus(Long taskGroupId, String executionBatchId,
                                          ExecutionContext context, List<WorkerWrapper<Long, String>> workerWrappers) {
        try {
            logger.info("[Orchestrator] 开始收集节点执行状态 - taskGroupId: {}, batchId: {}",
                    taskGroupId, executionBatchId);

            // 构建节点状态JSON
            Map<String, Object> nodeStatusMap = new HashMap<>();

            // 获取任务信息映射（用于获取节点名称）
            Map<Long, JobInfo> jobInfoMap = new HashMap<>();
            if (context.getNodes() != null && !context.getNodes().isEmpty()) {
                List<Long> jobIds = context.getNodes().stream()
                        .map(JobNode::getJobId)
                        .distinct()
                        .collect(Collectors.toList());
                try {
                    List<JobInfo> jobInfos = adminApiClient.getJobInfos(jobIds);
                    jobInfoMap = jobInfos.stream()
                            .collect(Collectors.toMap(JobInfo::getId, jobInfo -> jobInfo));
                } catch (Exception e) {
                    logger.warn("[Orchestrator] 获取任务信息失败，将使用默认值 - taskGroupId: {}", taskGroupId, e);
                }
            }

            // 构建 WorkerWrapper 映射，以节点ID为key，方便查找
            Map<String, WorkerWrapper<Long, String>> wrapperMap = new HashMap<>();
            if (workerWrappers != null) {
                for (WorkerWrapper<Long, String> wrapper : workerWrappers) {
                    if (wrapper.getId() != null) {
                        wrapperMap.put(wrapper.getId(), wrapper);
                    }
                }
            }

            // 遍历所有节点，收集执行状态
            for (JobNode node : context.getNodes()) {
                Long nodeJobId = node.getJobId();
                String nodeId = node.getId() != null ? String.valueOf(node.getId()) : String.valueOf(nodeJobId);

                // 从 WorkerWrapper 中获取执行状态
                WorkerWrapper<Long, String> wrapper = wrapperMap.get(String.valueOf(node.getId()));
                Integer status = -1; // 未执行

                if (wrapper != null && wrapper.getWorkResult() != null) {
                    ResultState resultState = wrapper.getWorkResult().getResultState();
                    if (resultState == ResultState.SUCCESS) {
                        status = 1; // 成功
                    } else if (resultState == ResultState.EXCEPTION || resultState == ResultState.TIMEOUT) {
                        status = 0; // 失败
                    } else if (resultState == ResultState.DEFAULT) {
                        // ⭐ 修复：如果是默认状态（未执行），保持为 null，不保存
                        status = -1;
                    } else {
                        status = 2; // 运行中
                    }
                    logger.debug("[Orchestrator] 节点执行状态 - nodeId: {}, jobId: {}, resultState: {}, status: {}",
                            node.getId(), nodeJobId, resultState, status);
                } else {
                    logger.warn("[Orchestrator] 未找到节点的 WorkerWrapper 或执行结果（节点未执行）- nodeId: {}, jobId: {}",
                            node.getId(), nodeJobId);
                }

                // ⭐ 修复：只保存真正执行过的节点（status 不为 null）
                if (status == -1) {
                    logger.info("[Orchestrator] 跳过未执行的节点 - nodeId: {}, jobId: {}",
                            node.getId(), nodeJobId);
                    continue; // 跳过，不添加到 nodeStatusMap
                }

                // 构建节点状态信息
                Map<String, Object> nodeStatus = new HashMap<>();
                nodeStatus.put("jobId", nodeJobId);

                // 获取节点名称
                JobInfo jobInfo = jobInfoMap.get(nodeJobId);
                if (jobInfo != null && jobInfo.getJobDesc() != null) {
                    nodeStatus.put("jobDesc", jobInfo.getJobDesc());
                } else {
                    nodeStatus.put("jobDesc", "节点 " + nodeJobId);
                }

                // 设置状态：0=失败, 1=成功, 2=运行中
                nodeStatus.put("status", status);

                // 使用节点ID作为key（如果节点ID为空，使用jobId）
                String nodeKey = node.getId() != null ? String.valueOf(node.getId()) : String.valueOf(nodeJobId);
                nodeStatusMap.put(nodeKey, nodeStatus);
            }

            // 序列化为JSON
            String nodeStatusJson = JSONUtil.toJsonStr(nodeStatusMap);
            logger.debug("[Orchestrator] 节点状态JSON: {}", nodeStatusJson);

            // 调用admin接口保存节点状态
            adminApiClient.saveNodeStatus(taskGroupId, executionBatchId, nodeStatusJson);

            logger.info("[Orchestrator] 节点执行状态已保存 - taskGroupId: {}, 节点数: {}",
                    taskGroupId, nodeStatusMap.size());

        } catch (Exception e) {
            logger.error("[Orchestrator] 收集或保存节点状态失败 - taskGroupId: {}", taskGroupId, e);
            // 不抛出异常，避免影响任务组完成流程
        }
    }

    /**
     * 从执行参数中解析批次ID
     *
     * <p>支持两种格式：
     * <ul>
     *   <li>JSON格式：{"batchId": "xxx"}</li>
     *   <li>键值对格式：batchId=xxx</li>
     * </ul>
     *
     * @param executeParam 执行参数
     * @return 批次ID，如果未找到则返回null
     */
    private String parseBatchIdFromParam(String executeParam) {
        if (executeParam == null || executeParam.trim().isEmpty()) {
            return null;
        }

        try {
            String trimmed = executeParam.trim();

            // 尝试解析为JSON格式
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                Map<String, Object> paramMap = JSONUtil.toBean(trimmed, Map.class);
                Object batchId = paramMap.get("batchId");
                if (batchId != null) {
                    return batchId.toString();
                }
            }

            // 尝试解析为键值对格式：batchId=xxx
            if (trimmed.contains("batchId=")) {
                String[] parts = trimmed.split("batchId=");
                if (parts.length > 1) {
                    String batchIdPart = parts[1].trim();
                    // 提取batchId（可能后面还有其他参数，用&或空格分隔）
                    int endIndex = batchIdPart.indexOf('&');
                    if (endIndex < 0) {
                        endIndex = batchIdPart.indexOf(' ');
                    }
                    if (endIndex > 0) {
                        return batchIdPart.substring(0, endIndex).trim();
                    } else {
                        return batchIdPart.trim();
                    }
                }
            }

            return null;
        } catch (Exception e) {
            logger.warn("[Orchestrator] 解析执行参数中的batchId失败 - executeParam: {}", executeParam, e);
            return null;
        }
    }

    /**
     * 停止任务组
     */
    public void stopTaskGroup(Long taskGroupId, String executionBatchId) {
        String executeKey = buildExecuteKey(taskGroupId, executionBatchId);
        List<WorkerWrapper<Long, String>> workerWrappers = RUNNING_JOBS.get(executeKey);

        if (workerWrappers != null) {
            logger.info("[Orchestrator] 停止任务组 - taskGroupId: {}", taskGroupId);
            Async.stopWork((List<WorkerWrapper>) (List<?>) workerWrappers);
            RUNNING_JOBS.remove(executeKey);
            XxlJobExecutor.removeJobThread(taskGroupId.intValue(), "");
            updateTaskGroupStatus(taskGroupId, executionBatchId);
        } else {
            logger.warn("[Orchestrator] 任务组不存在或已完成 - taskGroupId: {}", taskGroupId);
        }
    }

    /**
     * 检查任务组是否正在运行
     */
    public boolean isTaskGroupRunning(Long taskGroupId, String executionBatchId) {
        String executeKey = buildExecuteKey(taskGroupId, executionBatchId);
        return RUNNING_JOBS.containsKey(executeKey);
    }

    /**
     * 获取所有运行中的任务组
     */
    public Map<String, Boolean> getAllRunningTaskGroups() {
        Map<String, Boolean> result = new HashMap<>();
        for (String executeKey : RUNNING_JOBS.keySet()) {
            result.put(executeKey, true);
        }
        return result;
    }

    /**
     * 根据条件表达式过滤节点
     *
     * <p>过滤掉条件不满足的条件节点内的所有任务
     *
     * @param nodes       原始节点列表
     * @param dataContext 数据上下文
     * @param jobNameMap  jobName 映射
     * @param taskGroupId 任务组ID
     * @return 过滤后的节点列表
     */
    private List<JobNode> filterNodesByCondition(List<JobNode> nodes, DataContext dataContext,
                                                 Map<String, Long> jobNameMap, Long taskGroupId) {
        if (nodes == null || nodes.isEmpty()) {
            return nodes;
        }

        // 构建父子关系：parentJobId -> List<JobNode>
        Map<Long, List<JobNode>> nodesByParent = new HashMap<>();
        for (JobNode node : nodes) {
            Long parentId = node.getJobParentId();
            if (parentId != null) {
                nodesByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
            }
        }

        // 收集需要过滤的节点ID（条件不满足的条件节点内的所有任务）
        Set<Long> nodesToFilter = new HashSet<>();

        // 找到所有条件节点
        List<JobNode> conditionNodes = nodes.stream()
                .filter(n -> CONDITION_NODE.equalsIgnoreCase(n.getNodeType()))
                .collect(Collectors.toList());

        // 评估每个条件节点
        for (JobNode conditionNode : conditionNodes) {
            String conditionExpression = conditionNode.getConditionExpression();
            String expressionType = conditionNode.getExpressionType();

            if (conditionExpression == null || conditionExpression.trim().isEmpty()) {
                logger.warn("[Orchestrator] 条件节点 {} 的条件表达式为空，默认跳过", conditionNode.getId());
                // 条件表达式为空，跳过条件节点内的所有任务
                collectChildNodeIds(conditionNode.getJobId(), nodesByParent, nodesToFilter);
                continue;
            }

            // 评估条件表达式
            boolean conditionMet = conditionEvaluator.evaluate(
                    conditionExpression,
                    expressionType != null ? expressionType : "SIMPLE",
                    dataContext,
                    jobNameMap
            );

            logger.info("[Orchestrator] 条件节点 {} 评估结果: {} (表达式: {})",
                    conditionNode.getId(), conditionMet, conditionExpression);

            if (!conditionMet) {
                // 条件不满足，收集条件节点内的所有任务ID
                collectChildNodeIds(conditionNode.getJobId(), nodesByParent, nodesToFilter);
                logger.info("[Orchestrator] 条件节点 {} 条件不满足，将跳过 {} 个任务",
                        conditionNode.getId(), nodesToFilter.size());
            }
        }

        // 过滤节点：排除条件不满足的条件节点内的任务，但保留条件节点本身（作为容器）
        List<JobNode> filteredNodes = nodes.stream()
                .filter(n -> !nodesToFilter.contains(n.getId()))
                .collect(Collectors.toList());

        return filteredNodes;
    }

    /**
     * 递归收集条件节点内的所有子节点ID
     *
     * @param parentJobId   父任务ID（条件节点的jobId）
     * @param nodesByParent 按父ID分组的节点映射
     * @param result        输出参数：收集到的节点ID集合
     */
    private void collectChildNodeIds(Long parentJobId, Map<Long, List<JobNode>> nodesByParent,
                                     Set<Long> result) {
        List<JobNode> childNodes = nodesByParent.getOrDefault(parentJobId, Collections.emptyList());
        for (JobNode childNode : childNodes) {
            result.add(childNode.getId());
            // 如果子节点也是条件节点或任务组节点，递归收集其子节点
            String nodeType = childNode.getNodeType();
            if (CONDITION_NODE.equalsIgnoreCase(nodeType) ||
                    "CustomGroup".equalsIgnoreCase(nodeType) ||
                    "DYNAMIC_GROUP".equalsIgnoreCase(nodeType)) {
                collectChildNodeIds(childNode.getJobId(), nodesByParent, result);
            }
        }
    }

    /**
     * 构建执行键
     */
    private static String buildExecuteKey(Long taskGroupId, String executionBatchId) {
        return taskGroupId + ":" + executionBatchId;
    }

    /**
     * 获取 XxlJobContext
     */
    public static XxlJobContext getContext() {
        return CONTEXT_HOLDER.get();
    }
}


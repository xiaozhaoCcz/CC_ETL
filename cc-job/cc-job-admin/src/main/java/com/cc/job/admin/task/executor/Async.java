package com.cc.job.admin.task.executor;

import com.cc.job.admin.task.executor.callback.ICallback;
import com.cc.job.admin.task.executor.callback.IWorker;
import com.cc.job.admin.task.executor.timer.SystemClock;
import com.cc.job.admin.task.executor.worker.DependWrapper;
import com.cc.job.admin.task.executor.worker.ResultState;
import com.cc.job.admin.task.executor.worker.WorkResult;
import com.cc.job.admin.task.executor.wrapper.WorkerWrapper;
import com.cc.job.admin.task.handler.JobConstant;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Async 异步任务调度执行器，支持任务依赖拓扑排序执行。
 * 支持任务超时、失败重试、依赖关系处理等。
 */
public class Async {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(Async.class);

    // 建立线程池
    private static final ThreadPoolExecutor COMMON_POOL = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            100,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());

    private static ExecutorService executorService;

    /**
     * 启动任务组执行，使用默认线程池
     *
     * @param timeout        超时时间（毫秒）
     * @param workerWrappers 任务包装器列表
     * @return 是否成功启动
     * @throws ExecutionException 执行异常
     */
    public static boolean beginWork(long timeout, List<WorkerWrapper> workerWrappers) throws ExecutionException {
        return beginWork(timeout, COMMON_POOL, workerWrappers);
    }

    /**
     * 启动任务组执行，支持自定义线程池
     *
     * @param timeout         超时时间（毫秒）
     * @param executorService 线程池
     * @param workerWrappers  任务包装器列表
     * @return 是否成功启动
     * @throws ExecutionException 执行异常
     */
    public static boolean beginWork(long timeout, ExecutorService executorService,
                                    List<WorkerWrapper> workerWrappers) throws ExecutionException {
        if (workerWrappers == null || workerWrappers.isEmpty()) {
            logger.warn("workerWrappers 为空，未执行任何任务");
            return false;
        }

        Async.executorService = executorService;

        try {
            // 1. 构建任务依赖图
            Map<String, WorkerWrapper> wrapperMap = new ConcurrentHashMap<>();
            Map<String, Integer> inDegree = new ConcurrentHashMap<>();
            Set<String> submitted = ConcurrentHashMap.newKeySet();

            // 初始化映射
            for (WorkerWrapper wrapper : workerWrappers) {
                String id = wrapper.getId();
                wrapperMap.put(id, wrapper);
                inDegree.put(id, 0);
            }

            // 构建依赖关系
            for (WorkerWrapper wrapper : workerWrappers) {
                String id = wrapper.getId();
                List<DependWrapper> dependWrappers = wrapper.getDependWrappers();
                if (dependWrappers != null) {
                    inDegree.put(id, dependWrappers.size());
                }
            }

            logger.info("任务依赖图构建完成，任务数: {}", workerWrappers.size());
            executorWorkerWrapper(timeout, wrapperMap, inDegree, submitted);
            return true;
        } catch (Exception e) {
            logger.error("[TopologicalAsync] 拓扑排序执行异常: {}", e.getMessage(), e);
            throw new ExecutionException(e);
        }
    }

    /**
     * 执行任务调度，拓扑排序依赖调度
     *
     * @param timeout    超时时间
     * @param wrapperMap 任务映射
     * @param inDegree   入度表
     * @param submitted  已提交任务集合
     */
    private static void executorWorkerWrapper(long timeout, Map<String, WorkerWrapper> wrapperMap,
                                              Map<String, Integer> inDegree, Set<String> submitted) {
        // 当前剩余时间
        AtomicLong time = new AtomicLong(timeout * 1000);

        Thread thread = null;
        try {
            FutureTask<Boolean> futureTask = new FutureTask<>(() -> {
                doWorkWrappers(wrapperMap, inDegree, submitted, time);
                return true;
            });
            thread = new Thread(futureTask);
            thread.start();
            futureTask.get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    private static void doWorkWrappers(Map<String, WorkerWrapper> wrapperMap, Map<String, Integer> inDegree, Set<String> submitted, AtomicLong time) {
        while (!inDegree.isEmpty()) {
            List<String> inDegreeZero = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() == 0) {
                    inDegreeZero.add(entry.getKey());
                }
            }

            for (String id : inDegreeZero) {
                if (!submitted.add(id)) {
                    continue;
                }

                logger.info("提交任务: {} 到线程池执行", id);
                executorService.submit(() -> {
                    WorkerWrapper workerWrapper = null;
                    try {
                        // 当前任务
                        long beginTime = SystemClock.now();
                        workerWrapper = wrapperMap.get(id);
                        logger.debug("开始执行任务: {}", id);
                        doJob(workerWrapper, wrapperMap);

                        synchronized (inDegree) {
                            inDegree.remove(id);
                            List<WorkerWrapper> nextWrappers = workerWrapper.getNextWrappers();
                            if (nextWrappers != null && !nextWrappers.isEmpty()) {
                                List<String> nextIds = nextWrappers.stream().map(WorkerWrapper::getId).toList();
                                for (String nextId : nextIds) {
                                    // -1;
                                    inDegree.compute(nextId, (k, i) -> i - 1);
                                }
                            }
                        }
                        // 计算剩余时间
                        long costTime = time.get() - (SystemClock.now() - beginTime);
                        if (costTime > 0) {
                            time.set(costTime);
                            logger.info("任务组剩余时间{}", costTime);
                        } else {
                            logger.error("任务组运行超时异常，任务: {}", id);
                            throw new RuntimeException("任务组运行超时异常");
                        }
                        logger.info("任务: {} 执行完成", id);
                    } catch (Exception e) {
                        // 项目运行失败，抛出异常
                        logger.error("任务: {} 执行失败: {}", id, e.getMessage(), e);
                        synchronized (inDegree) {
                            inDegree.clear();
                        }
                        throw new RuntimeException(e);
                    }
                });
            }

        }
    }

    /**
     * 执行单个任务，包含重试逻辑
     *
     * @param workerWrapper 任务包装器
     * @param wrapperMap    任务映射
     */
    private static void doJob(WorkerWrapper workerWrapper, Map<String, WorkerWrapper> wrapperMap) {
        ICallback callback = workerWrapper.getCallback();
        long timeout = workerWrapper.getTimeout();
        Object param = workerWrapper.getParam();
        IWorker worker = workerWrapper.getWorker();
        Integer retryCount = workerWrapper.getRetryCount();

        if (workerWrapper.getState() == 3) {
            logger.error("任务运行失败，状态为3: {}", workerWrapper.getId());
            throw new RuntimeException("任务运行失败");
        }

        callback.begin(param);
        int count = 0;

        Object resultValue = null;
        boolean success = true;
        try {
            resultValue = getResultValue(timeout, worker, param, wrapperMap);

            while (JobConstant.FAIL_RETRY.equals(String.valueOf(resultValue)) && retryCount != null
                    && count++ <= retryCount) {
                // 睡眠5秒重试任务
                logger.warn("任务: {} 执行失败，进行第{}次重试", workerWrapper.getId(), count);
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    logger.error("重试等待被中断: {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
                resultValue = getResultValue(timeout, worker, param, wrapperMap);
                workerWrapper.setCount(count);
            }
            workerWrapper.setWorkResult(new WorkResult(resultValue, ResultState.SUCCESS));

        } catch (Exception e) {
            logger.error("任务: {} 执行异常: {}", workerWrapper.getId(), e.getMessage(), e);
            success = false;
            workerWrapper.setWorkResult(new WorkResult(e.getMessage(), ResultState.EXCEPTION));
            throw new RuntimeException(e);
        } finally {
            callback.result(success, param, workerWrapper.getWorkResult());
        }
    }

    /**
     * 获取任务执行结果，支持超时控制
     *
     * @param timeout    超时时间
     * @param worker     任务工作者
     * @param param      参数
     * @param wrapperMap 任务映射
     * @return 结果
     */
    private static Object getResultValue(long timeout, IWorker worker, Object param,
                                         Map<String, WorkerWrapper> wrapperMap) {

        Object resultValue = null;
        if (timeout > 0) {
            Thread thread = null;
            try {
                FutureTask<Object> futureTask = new FutureTask<>(() -> worker.action(param, wrapperMap));
                thread = new Thread(futureTask);
                thread.start();
                resultValue = futureTask.get(timeout, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error("任务执行超时或异常: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            } finally {
                if (thread != null) {
                    thread.interrupt();
                }
            }
        } else {
            resultValue = worker.action(param, wrapperMap);
        }
        return resultValue;
    }

    /**
     * 停止任务组执行，将所有任务状态置为3（失败）
     *
     * @param workerWrappers 任务包装器列表
     */
    public static void stopWork(List<WorkerWrapper> workerWrappers) {
        for (WorkerWrapper workerWrapper : workerWrappers) {
            logger.info("停止任务: {}，设置状态为3（失败）", workerWrapper.getId());
            workerWrapper.setState(new AtomicInteger(3));
        }
    }
}

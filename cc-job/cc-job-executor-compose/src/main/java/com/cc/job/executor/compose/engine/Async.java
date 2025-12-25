package com.cc.job.executor.compose.engine;

import com.cc.job.executor.compose.engine.callback.ICallback;
import com.cc.job.executor.compose.engine.callback.IWorker;
import com.cc.job.executor.compose.infrastructure.constant.ExecutorConstants;
import com.cc.job.executor.compose.engine.timer.SystemClock;
import com.cc.job.executor.compose.engine.worker.DependWrapper;
import com.cc.job.executor.compose.engine.worker.ResultState;
import com.cc.job.executor.compose.engine.worker.WorkResult;
import com.cc.job.executor.compose.engine.wrapper.WorkerWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Async 异步任务调度执行器，支持任务依赖拓扑排序执行。
 * 支持任务超时、失败重试、依赖关系处理等。
 * 
 * @author wuweifeng
 * @author xiaozhao (migrated to compose executor)
 */
public class Async {

    private static final Logger logger = LoggerFactory.getLogger(Async.class);

    // 默认线程池上限配置，可按需覆盖
    private static final int DEFAULT_MAX_POOL_SIZE = 100;
    private static final int MAX_QUEUE_CAPACITY = 10_000;

    // 建立线程池
    private static volatile ThreadPoolExecutor COMMON_POOL = buildDefaultExecutor(DEFAULT_MAX_POOL_SIZE);

    private static final ConcurrentHashMap<String, ExecutorService> EXECUTOR_CACHE = new ConcurrentHashMap<>();

    private static ExecutorService executorService;

    /**
     * 启动任务组执行，使用默认线程池
     *
     * @param timeout        超时时间（秒）
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
     * @param timeout         超时时间（秒）
     * @param executorService 线程池
     * @param workerWrappers  任务包装器列表
     * @return 是否成功启动
     * @throws ExecutionException 执行异常
     */
    public static boolean beginWork(long timeout, ExecutorService executorService,
                                    List<WorkerWrapper> workerWrappers) throws ExecutionException {
        if (workerWrappers == null || workerWrappers.isEmpty()) {
            logger.warn("[Async] workerWrappers 为空，未执行任何任务");
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
            
            // 第二遍遍历：确保所有nextWrappers中的任务ID都在inDegree中存在
            for (WorkerWrapper wrapper : workerWrappers) {
                List<WorkerWrapper> nextWrappers = wrapper.getNextWrappers();
                if (nextWrappers != null && !nextWrappers.isEmpty()) {
                    for (WorkerWrapper nextWrapper : nextWrappers) {
                        String nextId = nextWrapper.getId();
                        if (!inDegree.containsKey(nextId)) {
                            logger.debug("[Async] 发现nextWrappers中的任务ID: {}，将其初始化为0", nextId);
                            inDegree.put(nextId, 0);
                        }
                    }
                }
            }

            logger.info("[Async] 任务依赖图构建完成，任务数: {}", workerWrappers.size());
            executorWorkerWrapper(timeout, wrapperMap, inDegree, submitted);
            return true;
        } catch (Exception e) {
            logger.error("[Async] 拓扑排序执行异常: {}", e.getMessage(), e);
            throw new ExecutionException(e);
        }
    }

    /**
     * 根据任务组特性获取或创建线程池
     *
     * @param resourceKey    任务资源键
     * @param coreSize       最小线程数
     * @param maxSize        最大线程数
     * @param keepAlive      空闲保活时间
     * @param queueCapacity  队列容量
     * @return ExecutorService
     */
    public static ExecutorService getOrCreateExecutor(String resourceKey, int coreSize, int maxSize, long keepAlive,
                                                      int queueCapacity) {
        return EXECUTOR_CACHE.computeIfAbsent(resourceKey,
                key -> buildExecutor(coreSize, maxSize, keepAlive, queueCapacity));
    }

    /**
     * 重设默认公共线程池的最大线程数，可在运维层面动态调整
     *
     * @param maxPoolSize 最大线程数
     */
    public static synchronized void resizeCommonPool(int maxPoolSize) {
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("maxPoolSize must be positive");
        }
        ThreadPoolExecutor oldPool = COMMON_POOL;
        ThreadPoolExecutor newPool = buildDefaultExecutor(maxPoolSize);
        COMMON_POOL = newPool;
        logger.info("[Async] 重建默认线程池，旧最大线程数: {} -> 新最大线程数: {}", oldPool.getMaximumPoolSize(),
                newPool.getMaximumPoolSize());
        oldPool.shutdown();
    }

    private static ThreadPoolExecutor buildDefaultExecutor(int maxPoolSize) {
        int coreSize = Math.min(Runtime.getRuntime().availableProcessors(), maxPoolSize);
        return buildExecutor(coreSize, maxPoolSize, 60L, MAX_QUEUE_CAPACITY);
    }

    private static ThreadPoolExecutor buildExecutor(int coreSize, int maxSize, long keepAliveSeconds, int queueCapacity) {
        if (coreSize <= 0 || maxSize <= 0 || queueCapacity <= 0) {
            throw new IllegalArgumentException("coreSize, maxSize, queueCapacity must be > 0");
        }
        if (coreSize > maxSize) {
            coreSize = maxSize;
        }
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                coreSize,
                maxSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadFactory() {
                    private final ThreadFactory delegate = Executors.defaultThreadFactory();
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = delegate.newThread(r);
                        t.setName("cc-async-exec-" + t.getId());
                        t.setDaemon(true);
                        return t;
                    }
                },
                // 使用 CallerRunsPolicy 以提供背压，避免直接拒绝导致上游失败
                new ThreadPoolExecutor.CallerRunsPolicy());
        // 允许核心线程超时以在低峰时回收资源
        executor.allowCoreThreadTimeOut(true);
        return executor;
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
        boolean interrupted = false;
        try {
            FutureTask<Boolean> futureTask = new FutureTask<>(() -> {
                doWorkWrappers(wrapperMap, inDegree, submitted, time);
                return true;
            });
            thread = new Thread(futureTask);
            thread.start();
            futureTask.get(timeout, TimeUnit.SECONDS);
            logger.info("[Async] 任务组调度正常完成");
        } catch (TimeoutException e) {
            logger.error("[Async] 任务组调度超时");
            interrupted = true;
            throw new RuntimeException("任务组调度超时", e);
        } catch (Exception e) {
            logger.error("[Async] 任务组调度异常: {}", e.getMessage(), e);
            interrupted = true;
            throw new RuntimeException(e);
        } finally {
            // 只有在超时或异常时才中断线程
            if (thread != null && interrupted) {
                logger.warn("[Async] 中断调度线程");
                thread.interrupt();
            }
        }
    }

    private static void doWorkWrappers(Map<String, WorkerWrapper> wrapperMap, Map<String, Integer> inDegree,
                                       Set<String> submitted, AtomicLong time) {
        BlockingQueue<String> zeroQueue = new LinkedBlockingQueue<>();
        AtomicInteger remaining = new AtomicInteger(inDegree.size());
        String END_MARKER = "";

        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                zeroQueue.offer(entry.getKey());
                logger.info("[Async] 初始入度为0的任务: {}", entry.getKey());
            }
        }

        logger.info("[Async] 开始任务调度，总任务数: {}, 初始可执行任务数: {}", 
                remaining.get(), zeroQueue.size());

        // 图算法：拓扑排序算法
        while (true) {
            String id;
            try {
                id = zeroQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("[Async] 任务调度线程被中断，剩余任务数: {}", remaining.get());
                break;
            }

            if (END_MARKER.equals(id)) {
                logger.info("[Async] 收到结束标记，所有任务已完成");
                break;
            }
            
            if (!submitted.add(id)) {
                logger.warn("[Async] 任务: {} 已经提交过，跳过", id);
                continue;
            }

            logger.info("[Async] 提交任务: {} 到线程池执行，剩余任务数: {}", id, remaining.get());
            executorService.submit(() -> {
                WorkerWrapper workerWrapper = null;
                try {
                    long beginTime = SystemClock.now();
                    workerWrapper = wrapperMap.get(id);
                    logger.info("[Async] ========== 开始执行任务: {} ==========", id);
                    doJob(workerWrapper, wrapperMap);
                    logger.info("[Async] ========== 任务: {} 执行完成 ==========", id);

                    synchronized (inDegree) {
                        inDegree.remove(id);
                        int remainingCount = remaining.decrementAndGet();
                        logger.info("[Async] 任务: {} 完成，剩余任务数: {}", id, remainingCount);
                        
                        if (remainingCount == 0) {
                            zeroQueue.offer(END_MARKER);
                            logger.info("[Async] 所有任务已完成，发送结束标记");
                        } else {
                            List<WorkerWrapper> nextWrappers = workerWrapper.getNextWrappers();
                            if (nextWrappers != null && !nextWrappers.isEmpty()) {
                                logger.info("[Async] 任务: {} 有 {} 个后续任务", id, nextWrappers.size());
                                List<String> nextIds = nextWrappers.stream().map(WorkerWrapper::getId).toList();
                                
                                for (String nextId : nextIds) {
                                    if (nextId == null || !inDegree.containsKey(nextId)) {
                                        logger.warn("[Async] 后续任务: {} 不在inDegree中，跳过入度减少", nextId);
                                        continue;
                                    }
                                    
                                    Integer oldInDegree = inDegree.get(nextId);
                                    logger.info("[Async] 准备更新后续任务: {} 的入度，当前入度: {}", nextId, oldInDegree);
                                    
                                    inDegree.compute(nextId, (k, i) -> {
                                        if (i == null) {
                                            logger.warn("[Async] 后续任务: {} 入度为null，重置为0", nextId);
                                            return 0;
                                        }
                                        if (i <= 0) {
                                            logger.warn("[Async] 后续任务: {} 入度已经为0或负数: {}，不再减少", nextId, i);
                                            return i;
                                        }
                                        int updated = i - 1;
                                        logger.info("[Async] 后续任务: {} 入度从 {} 减少到 {}", nextId, i, updated);
                                        
                                        if (updated == 0) {
                                            boolean offered = zeroQueue.offer(nextId);
                                            logger.info("[Async] 后续任务: {} 入度变为0，加入执行队列，结果: {}", 
                                                    nextId, offered ? "成功" : "失败");
                                            return 0;
                                        }
                                        return updated;
                                    });
                                }
                            } else {
                                logger.info("[Async] 任务: {} 没有后续任务", id);
                            }
                        }
                    }

                    long costTime = time.get() - (SystemClock.now() - beginTime);
                    if (costTime > 0) {
                        time.set(costTime);
                        logger.info("[Async] 任务组剩余时间 {} ms", costTime);
                    } else {
                        logger.error("[Async] 任务组运行超时异常，任务: {}", id);
                        throw new RuntimeException("任务组运行超时异常");
                    }
                } catch (Exception e) {
                    logger.error("[Async] 任务: {} 执行失败: {}", id, e.getMessage(), e);
                    synchronized (inDegree) {
                        inDegree.clear();
                        remaining.set(0);
                    }
                    zeroQueue.clear();
                    zeroQueue.offer(END_MARKER);
                    throw new RuntimeException(e);
                }
            });
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
            logger.error("[Async] 任务运行失败，状态为3: {}", workerWrapper.getId());
            throw new RuntimeException("任务运行失败");
        }

        callback.begin(param);
        int count = 0;

        Object resultValue = null;
        boolean success = true;
        try {
            resultValue = getResultValue(timeout, worker, param, wrapperMap);

            while (retryCount != null && count < retryCount) {
                count++;
                // 指数退避 + 抖动，避免重试风暴；基于秒为单位
                long baseDelayMillis = 300L;
                long backoffMillis = Math.min(10_000L, baseDelayMillis * (1L << Math.min(count, 5))); // 封顶10s
                long jitter = ThreadLocalRandom.current().nextLong(100L, 400L);
                long sleepMillis = backoffMillis + jitter;
                logger.warn("[Async] 任务: {} 执行失败，准备第{}次重试，等待 {} ms", workerWrapper.getId(), count, sleepMillis);
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("[Async] 重试等待被中断: {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
                resultValue = getResultValue(timeout, worker, param, wrapperMap);
                workerWrapper.setCount(count);
            }
            
            // 检查重试后是否仍然失败
            if (ExecutorConstants.ExecutionResult.FAIL_RETRY.equals(String.valueOf(resultValue)) || 
                ExecutorConstants.ExecutionResult.FAIL_COMPLETE.equals(String.valueOf(resultValue))||
                    ExecutorConstants.ExecutionResult.DO_NOTHING.equals(String.valueOf(resultValue))) {
                success = false;
                workerWrapper.setWorkResult(new WorkResult(resultValue, ResultState.EXCEPTION));
                logger.error("[Async] 任务: {} 执行失败，重试后仍然失败，结果: {}", workerWrapper.getId(), resultValue);
                if(!ExecutorConstants.ExecutionResult.DO_NOTHING.equals(String.valueOf(resultValue))){
                    throw new RuntimeException();
                }
            } else {
                workerWrapper.setWorkResult(new WorkResult(resultValue, ResultState.SUCCESS));
            }

        } catch (Exception e) {
            logger.error("[Async] 任务: {} 执行异常: {}", workerWrapper.getId(), e.getMessage(), e);
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
        return worker.action(param, wrapperMap);
    }

    /**
     * 停止任务组执行，将所有任务状态置为3（失败）
     *
     * @param workerWrappers 任务包装器列表
     */
    public static void stopWork(List<WorkerWrapper> workerWrappers) {
        for (WorkerWrapper workerWrapper : workerWrappers) {
            logger.info("[Async] 停止任务: {}，设置状态为3（失败）", workerWrapper.getId());
            workerWrapper.setState(new AtomicInteger(3));
        }
    }
}

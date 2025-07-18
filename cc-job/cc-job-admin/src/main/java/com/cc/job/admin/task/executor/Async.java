package com.cc.job.admin.task.executor;

import com.cc.job.admin.task.handler.JobConstant;
import com.cc.tasktool.callback.ICallback;
import com.cc.tasktool.callback.IWorker;
import com.cc.tasktool.executor.timer.SystemClock;
import com.cc.tasktool.worker.DependWrapper;
import com.cc.tasktool.wrapper.WorkerWrapper;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Async {

    //建立线程池
    private static final ThreadPoolExecutor COMMON_POOL = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            100,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
    );

    private static ExecutorService executorService;

    public static boolean beginWork(long timeout,List<WorkerWrapper> workerWrappers) throws ExecutionException, InterruptedException {
        return beginWork(timeout,COMMON_POOL,workerWrappers);
    }


    public static boolean beginWork(long timeout, ExecutorService executorService,
                                               List<WorkerWrapper> workerWrappers) throws ExecutionException, InterruptedException {
        if (workerWrappers == null || workerWrappers.isEmpty()) {
            return false;
        }

        Async.executorService = executorService;

        try {
            // 1. 构建任务依赖图
            Map<String, WorkerWrapper> wrapperMap = new ConcurrentHashMap<>();
            Map<String, Integer> inDegree = new ConcurrentHashMap<>();
            Map<String, Integer> inDegreeCountMap = new HashMap<>();

            // 初始化映射
            for (WorkerWrapper wrapper : workerWrappers) {
                String id = wrapper.getId();
                wrapperMap.put(id, wrapper);
                inDegreeCountMap.put(id,0);
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

            executorWorkerWrapper(timeout,wrapperMap, inDegree,inDegreeCountMap);
            return true;
        } catch (Exception e) {
            System.err.println("[TopologicalAsync] 拓扑排序执行异常: " + e.getMessage());
            throw new ExecutionException(e);
        }
    }



    private static void executorWorkerWrapper(long timeout,Map<String, WorkerWrapper> wrapperMap, Map<String, Integer> inDegree, Map<String, Integer> inDegreeCountMap) {
        //当前时间
        AtomicLong time = new AtomicLong(timeout);
        while (!inDegree.isEmpty()) {
            List<String> inDegreeZero = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() == 0) {
                    inDegreeZero.add(entry.getKey());
                }
            }

            if (!inDegreeZero.isEmpty()) {
                for (String id : inDegreeZero) {
                    if (inDegreeCountMap.get(id) > 0) {
                        continue;
                    }
                    inDegreeCountMap.put(id, inDegreeCountMap.get(id) + 1);

                    executorService.submit(()->{
                        WorkerWrapper workerWrapper = null;
                        try {
                            // 当前任务
                            long beginTime = SystemClock.now();
                            workerWrapper = wrapperMap.get(id);
                            doJob(workerWrapper,wrapperMap);
                            long costTime = time.get()-(SystemClock.now() - beginTime);
                            if(costTime > 0){
                                time.set(costTime);
                            }else{
                                throw new RuntimeException("任务组运行超时异常");
                            }

                            synchronized (inDegree){
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
                        } catch (Exception e) {
                            //项目运行失败，抛出异常
                            synchronized (inDegree){
                                inDegree.clear();
                            }
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }

    private static void doJob(WorkerWrapper workerWrapper,Map<String, WorkerWrapper> wrapperMap) {
        ICallback callback = workerWrapper.getCallback();
        long timeout = workerWrapper.getTimeout();
        Object param = workerWrapper.getParam();
        IWorker worker = workerWrapper.getWorker();
        Integer retryCount = workerWrapper.getRetryCount();

        if(workerWrapper.getState()==3){
            throw new RuntimeException("任务运行失败");
        }

        callback.begin(param);
        int count = 0;


       Object resultValue = null;
        try {
            resultValue = getResultValue(timeout, worker, param,wrapperMap);

            while (JobConstant.FAIL_RETRY.equals(String.valueOf(resultValue)) && retryCount != null && count++ <= retryCount) {
                // 睡眠5秒重试任务
                try {
                    TimeUnit.MILLISECONDS.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                resultValue = getResultValue(timeout,worker,param,wrapperMap);
                workerWrapper.setCount(count);
            }

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private static Object getResultValue(long timeout, IWorker worker, Object param,Map<String, WorkerWrapper> wrapperMap) {

        Object resultValue = null;
        if (timeout > 0) {
            Thread thread = null;
            try {
                FutureTask<Object> futureTask = new FutureTask<>(() -> worker.action(param,wrapperMap));
                thread = new Thread(futureTask);
                thread.start();
                resultValue=  futureTask.get(timeout, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
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


    public static void stopWork(List<WorkerWrapper> workerWrappers){
        for (WorkerWrapper workerWrapper : workerWrappers) {
            workerWrapper.setState(new AtomicInteger(3));
        }
    }
}

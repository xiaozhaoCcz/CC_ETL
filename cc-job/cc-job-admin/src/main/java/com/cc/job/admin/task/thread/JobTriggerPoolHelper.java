package com.cc.job.admin.task.thread;

import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.task.enums.TriggerTypeEnum;
import com.cc.job.admin.task.trigger.XxlJobTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * job trigger thread pool helper
 *
 * @author xuxueli 2018-07-03 21:08:07
 */
public class JobTriggerPoolHelper {
    private static Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class);


    // ---------------------- trigger pool ----------------------

    // fast/slow thread pool
    private ThreadPoolExecutor fastTriggerPool = null;
    private ThreadPoolExecutor slowTriggerPool = null;

    public void start(){
        int fastMax = XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax();
        int fastCore = Math.min(Math.max(4, Runtime.getRuntime().availableProcessors() * 2), fastMax);
        fastTriggerPool = new ThreadPoolExecutor(
                fastCore,
                fastMax,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2048),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "xxl-job, admin JobTriggerPoolHelper-fast-" + r.hashCode());
                        t.setDaemon(true);
                        return t;
                    }
                },
                // 背压优先，避免拒绝直接失败；触发线程偶尔同步执行可平滑峰值
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        fastTriggerPool.allowCoreThreadTimeOut(true);

        int slowMax = XxlJobAdminConfig.getAdminConfig().getTriggerPoolSlowMax();
        int slowCore = Math.min(Math.max(2, Runtime.getRuntime().availableProcessors()), slowMax);
        slowTriggerPool = new ThreadPoolExecutor(
                slowCore,
                slowMax,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(4096),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "xxl-job, admin JobTriggerPoolHelper-slow-" + r.hashCode());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        slowTriggerPool.allowCoreThreadTimeOut(true);
    }


    public void stop() {
        //triggerPool.shutdown();
        fastTriggerPool.shutdownNow();
        slowTriggerPool.shutdownNow();
        logger.info(">>>>>>>>> xxl-job trigger thread pool shutdown success.");
    }


    // job timeout count
    private volatile long minTim = System.currentTimeMillis()/60000;     // ms > min
    private volatile ConcurrentMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();


    /**
     * add trigger
     */
    public void addTrigger(final int jobId,
                           final TriggerTypeEnum triggerType,
                           final int failRetryCount,
                           final String executorShardingParam,
                           final String executorParam,
                           final String addressList,
                           final long logId
    ) {

        // choose thread pool
        ThreadPoolExecutor triggerPool_ = fastTriggerPool;
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        if (jobTimeoutCount!=null && jobTimeoutCount.get() > 10) {      // job-timeout 10 times in 1 min
            triggerPool_ = slowTriggerPool;
        }

        // trigger
        triggerPool_.execute(new Runnable() {
            @Override
            public void run() {

                long start = System.currentTimeMillis();

                try {
                    // do trigger
                    XxlJobTrigger.trigger((long) jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList,logId);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                } finally {

                    // check timeout-count-map
                    long minTim_now = System.currentTimeMillis()/60000;
                    if (minTim != minTim_now) {
                        minTim = minTim_now;
                        jobTimeoutCountMap.clear();
                    }

                    // incr timeout-count-map
                    long cost = System.currentTimeMillis()-start;
                    if (cost > 500) {       // ob-timeout threshold 500ms
                        AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                        if (timeoutCount != null) {
                            timeoutCount.incrementAndGet();
                        }
                    }

                }

            }
        });
    }



    // ---------------------- helper ----------------------

    private static JobTriggerPoolHelper helper = new JobTriggerPoolHelper();

    public static void toStart() {
        helper.start();
    }
    public static void toStop() {
        helper.stop();
    }

    /**
     * @param jobId
     * @param triggerType
     * @param failRetryCount
     * 			>=0: use this param
     * 			<0: use param from job info config
     * @param executorShardingParam
     * @param executorParam
     *          null: use job param
     *          not null: cover job param
     */
    public static void trigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam, String executorParam, String addressList, long logId) {
        helper.addTrigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList,logId);
    }

}

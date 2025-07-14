package com.cc.job.admin.task.thread;

import cn.hutool.core.lang.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

/**
 * @author xiaozhao
 */
public class JobLogHelper {

    private static Logger logger = LoggerFactory.getLogger(JobLogHelper.class);

    private final static Map<String,String> JOB_LOG_MAP = new ConcurrentHashMap<>();

    private final static Map<String,Thread> THREAD_MAP = new ConcurrentHashMap<>();

    public static void addJobLog(Pair<String, String> jobLog) {
        JOB_LOG_MAP.put(jobLog.getKey(),jobLog.getValue());
    }

    public static Map<String,String> getJobLogMap(){
        return JOB_LOG_MAP;
    }

    public static void addJobLogThread(String key,Thread thread){
        THREAD_MAP.put(key, thread);
    }

    public static void removeJobLogThread(String key){
        THREAD_MAP.remove(key);
        Thread thread = THREAD_MAP.get(key);
        if(thread != null){
           thread.interrupt();
        }
    }

    public static void stop(){
        JOB_LOG_MAP.clear();
        THREAD_MAP.values().forEach(Thread::interrupt);
        logger.info(">>>>>>>>>>>关闭所有日志id读取任务");
    }
}

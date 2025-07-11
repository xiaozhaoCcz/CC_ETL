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

public class JobLogHelper {

    private static Logger logger = LoggerFactory.getLogger(JobLogHelper.class);

    private final static List<Pair<String,String>> JOB_LOG_LIST = Collections.synchronizedList(new ArrayList<>());

    private final static Map<String,Thread> threadMap = new ConcurrentHashMap<>();

    public static void addJobLog(Pair<String, String> jobLog) {
        JOB_LOG_LIST.add(jobLog);
    }

    public static List<Pair<String,String>> getJobLogList(){
        return JOB_LOG_LIST;
    }

    public static void addJobLogThread(String key,Thread thread){
         threadMap.put(key, thread);
    }

    public static void removeJobLogThread(String key){
        JOB_LOG_LIST.removeIf(pair -> pair.getKey().equals(key));
        Thread thread = threadMap.get(key);
        if(thread != null){
           thread.interrupt();
        }
    }

    public static void stop(){
        JOB_LOG_LIST.clear();
        threadMap.values().forEach(Thread::interrupt);
        logger.info(">>>>>>>>>>>关闭所有日志id读取任务");
    }
}

package com.cc.job.admin.task.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * @author xiaozhao
 */
public class JobLogThreadListener implements Callable<String> {
    private static Logger logger = LoggerFactory.getLogger(JobLogThreadListener.class);


    private final String key;

    public JobLogThreadListener(String key) {
        this.key = key;
    }

    private volatile boolean stop = false;

    @Override
    public String call() {
        while (!stop) {
            // 从任务集合中遍历
            if (JobLogHelper.getJobLogMap().containsKey(key)) {
                try {
                    return JobLogHelper.getJobLogMap().get(key);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }
        return "";
    }

    public void toStop() {
        this.stop = true;
    }
}

package com.cc.job.admin.task.thread;

import cn.hutool.core.lang.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.Callable;

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
            for (Pair<String, String> pair : new ArrayList<>(JobLogHelper.getJobLogList())) {
                if (pair.getKey().equals(key)){
                    try {
                        return pair.getValue();
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return "";
    }

    public void toStop() {
        this.stop = true;
    }
}

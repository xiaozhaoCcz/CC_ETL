package com.cc.job.task.jobhandler;

import cn.hutool.http.HttpUtil;
import com.cc.job.task.thread.JobTriggerPoolHelper;
import com.xxl.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ApiJobHandler extends IJobHandler {
    private static Logger logger = LoggerFactory.getLogger(ApiJobHandler.class);
    private final String api;

    public ApiJobHandler(String api) {
        this.api = api;
    }
    @Override
    public void execute() throws Exception {
        System.out.println("ApiJobHandler 任务运行"+api);
        String s = HttpUtil.get(api);
        System.out.println("返回结果"+s);
    }

    @Override
    public void init() throws Exception {
       logger.info("Starting apiJobHandler......");
    }

    @Override
    public void destroy() throws Exception {
       logger.info("destroy apiJobHandler......");
    }
}

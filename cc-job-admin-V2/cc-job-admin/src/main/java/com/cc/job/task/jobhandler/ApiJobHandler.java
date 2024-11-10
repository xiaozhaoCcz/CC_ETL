package com.cc.job.task.jobhandler;


import com.xxl.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class ApiJobHandler extends IJobHandler {
    private static Logger logger = LoggerFactory.getLogger(ApiJobHandler.class);
    private final String api;

    public ApiJobHandler(String api) {
        this.api = api;
    }
    @Override
    public void execute() throws Exception {
        System.out.println("ApiJobHandler 任务运行"+api);
        System.out.println("返回结果1111");
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

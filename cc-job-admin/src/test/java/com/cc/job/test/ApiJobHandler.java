package com.cc.job.test;

import com.xxl.job.core.handler.IJobHandler;

public class ApiJobHandler extends IJobHandler {

    private final String api;

    public ApiJobHandler(String api) {
        this.api = api;
    }

    @Override
    public void execute() throws Exception {
        System.out.println("当前方法"+api);
    }


    @Override
    public void init() throws Exception {
        System.out.println("ApiJobHandler init");
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("ApiJobHandler destroy");
    }
}

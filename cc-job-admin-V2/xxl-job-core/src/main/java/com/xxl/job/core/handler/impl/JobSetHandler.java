package com.xxl.job.core.handler.impl;

import cn.hutool.http.HttpUtil;
import com.xxl.job.core.handler.IJobHandler;

public class JobSetHandler extends IJobHandler {

    private final long jobId;

    public JobSetHandler(long jobId) {
        this.jobId = jobId;
    }

    @Override
    public void execute() throws Exception {
        System.out.println(">>>>>>>>>>> taskSet start"+jobId);
        HttpUtil.get("http://localhost:8989/api/v1/taskInfos/runTaskSet/"+jobId);
    }
}

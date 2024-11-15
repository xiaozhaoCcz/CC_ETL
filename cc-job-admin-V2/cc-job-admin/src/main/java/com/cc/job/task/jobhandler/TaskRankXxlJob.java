package com.cc.job.task.jobhandler;

import com.cc.job.task.service.TaskInfoService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class TaskRankXxlJob {

    @Resource
    TaskInfoService taskInfoService;

    @XxlJob("runTaskRankXxlJob")
    public void runTaskRankXxlJob(){
        String jobId = XxlJobHelper.getJobParam();
        System.out.println(">>>>>>>runTaskRankXxlJob"+jobId);
        assert jobId != null;
        taskInfoService.runTaskSet(Long.valueOf(jobId));
    }
}

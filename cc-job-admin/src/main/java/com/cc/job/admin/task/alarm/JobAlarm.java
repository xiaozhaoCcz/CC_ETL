package com.cc.job.admin.task.alarm;


import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobLog;

/**
 * @author xuxueli 2020-01-19
 */
public interface JobAlarm {

    /**
     * job alarm
     *
     * @param info
     * @param jobLog
     * @return
     */
    public boolean doAlarm(JobInfo info, JobLog jobLog);

}

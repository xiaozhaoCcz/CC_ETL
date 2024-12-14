package com.cc.job.task.alarm;


import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.entity.TaskLog;

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
    public boolean doAlarm(TaskInfo info, TaskLog jobLog);

}

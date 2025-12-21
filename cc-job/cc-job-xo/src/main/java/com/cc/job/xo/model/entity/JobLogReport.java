package com.cc.job.xo.model.entity;

import com.cc.job.xo.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * task_log_report实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@TableName("job_log_report")
public class JobLogReport extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 调度-时间
     */
    private LocalDateTime triggerDay;
    /**
     * 运行中-日志数量
     */
    private Integer runningCount;
    /**
     * 执行成功-日志数量
     */
    private Integer sucCount;
    /**
     * 执行失败-日志数量
     */
    private Integer failCount;

    public LocalDateTime getTriggerDay() {
        return triggerDay;
    }

    public void setTriggerDay(LocalDateTime triggerDay) {
        this.triggerDay = triggerDay;
    }

    public Integer getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(Integer runningCount) {
        this.runningCount = runningCount;
    }

    public Integer getSucCount() {
        return sucCount;
    }

    public void setSucCount(Integer sucCount) {
        this.sucCount = sucCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }
}

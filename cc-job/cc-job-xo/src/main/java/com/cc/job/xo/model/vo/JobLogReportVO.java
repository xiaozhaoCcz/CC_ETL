package com.cc.job.xo.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * task_log_report视图对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Schema( description = "task_log_report视图对象")
public class JobLogReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;
    @Schema(description = "调度-时间")
    private LocalDateTime triggerDay;
    @Schema(description = "运行中-日志数量")
    private Integer runningCount;
    @Schema(description = "执行成功-日志数量")
    private Integer sucCount;
    @Schema(description = "执行失败-日志数量")
    private Integer failCount;
    private LocalDateTime updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}

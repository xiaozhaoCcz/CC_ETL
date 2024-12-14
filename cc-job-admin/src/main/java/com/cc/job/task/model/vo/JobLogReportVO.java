package com.cc.job.task.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * task_log_report视图对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Getter
@Setter
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
}

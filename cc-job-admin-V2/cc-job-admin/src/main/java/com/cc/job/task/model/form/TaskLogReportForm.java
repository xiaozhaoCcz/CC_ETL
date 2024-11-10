package com.cc.job.task.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

/**
 * task_log_report表单对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Getter
@Setter
@Schema(description = "task_log_report表单对象")
public class TaskLogReportForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;

    @Schema(description = "调度-时间")
    @NotNull(message = "调度-时间不能为空")
    private LocalDateTime triggerDay;

    @Schema(description = "运行中-日志数量")
    private Integer runningCount;

    @Schema(description = "执行成功-日志数量")
    private Integer sucCount;

    @Schema(description = "执行失败-日志数量")
    private Integer failCount;

    @NotNull(message = "不能为空")
    private LocalDateTime updateTime;


}

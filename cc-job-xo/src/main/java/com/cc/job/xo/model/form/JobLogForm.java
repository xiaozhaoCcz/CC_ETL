package com.cc.job.xo.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

/**
 * task_log表单对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Getter
@Setter
@Schema(description = "task_log表单对象")
public class JobLogForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @Schema(description = "执行器主键ID")
    private Integer jobGroup;

    @Schema(description = "任务，主键ID")
    private Integer jobId;

    @Schema(description = "执行器地址，本次执行的地址")
    @NotBlank(message = "执行器地址，本次执行的地址不能为空")
    @Size(max=255, message="执行器地址，本次执行的地址长度不能超过255个字符")
    private String executorAddress;

    @Schema(description = "执行器任务handler")
    @NotBlank(message = "执行器任务handler不能为空")
    @Size(max=255, message="执行器任务handler长度不能超过255个字符")
    private String executorHandler;

    @Schema(description = "执行器任务参数")
    @NotBlank(message = "执行器任务参数不能为空")
    @Size(max=512, message="执行器任务参数长度不能超过512个字符")
    private String executorParam;

    @Schema(description = "执行器任务分片参数，格式如 1/2")
    @NotBlank(message = "执行器任务分片参数，格式如 1/2不能为空")
    @Size(max=20, message="执行器任务分片参数，格式如 1/2长度不能超过20个字符")
    private String executorShardingParam;

    @Schema(description = "失败重试次数")
    private Integer executorFailRetryCount;

    @Schema(description = "调度-时间")
    @NotNull(message = "调度-时间不能为空")
    private LocalDateTime triggerTime;

    @Schema(description = "调度-结果")
    private Integer triggerCode;

    @Schema(description = "调度-日志")
    @NotBlank(message = "调度-日志不能为空")
    @Size(max=65535, message="调度-日志长度不能超过65535个字符")
    private String triggerMsg;

    @Schema(description = "执行-时间")
    @NotNull(message = "执行-时间不能为空")
    private LocalDateTime handleTime;

    @Schema(description = "执行-状态")
    private Integer handleCode;

    @Schema(description = "执行-日志")
    @NotBlank(message = "执行-日志不能为空")
    @Size(max=65535, message="执行-日志长度不能超过65535个字符")
    private String handleMsg;

    @Schema(description = "告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败")
    private Integer alarmStatus;


}

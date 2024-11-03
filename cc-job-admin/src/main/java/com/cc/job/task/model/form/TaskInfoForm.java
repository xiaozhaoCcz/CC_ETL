package com.cc.job.task.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

/**
 * task_info表单对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Getter
@Setter
@Schema(description = "task_info表单对象")
public class TaskInfoForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;

    @Schema(description = "执行器主键ID")
    private Integer jobGroup;

    @Size(max=255, message="长度不能超过255个字符")
    private String jobDesc;

    @NotNull(message = "不能为空")
    private LocalDateTime addTime;

    @NotNull(message = "不能为空")
    private LocalDateTime updateTime;

    @Schema(description = "作者")
    @NotBlank(message = "作者不能为空")
    @Size(max=64, message="作者长度不能超过64个字符")
    private String author;

    @Schema(description = "报警邮件")
    @NotBlank(message = "报警邮件不能为空")
    @Size(max=255, message="报警邮件长度不能超过255个字符")
    private String alarmEmail;

    @Schema(description = "调度类型")
    @Size(max=50, message="调度类型长度不能超过50个字符")
    private String scheduleType;

    @Schema(description = "调度配置，值含义取决于调度类型")
    @NotBlank(message = "调度配置，值含义取决于调度类型不能为空")
    @Size(max=128, message="调度配置，值含义取决于调度类型长度不能超过128个字符")
    private String scheduleConf;

    @Schema(description = "调度过期策略")
    @Size(max=50, message="调度过期策略长度不能超过50个字符")
    private String misfireStrategy;

    @Schema(description = "执行器路由策略")
    @NotBlank(message = "执行器路由策略不能为空")
    @Size(max=50, message="执行器路由策略长度不能超过50个字符")
    private String executorRouteStrategy;

    @Schema(description = "执行器任务handler")
    @NotBlank(message = "执行器任务handler不能为空")
    @Size(max=255, message="执行器任务handler长度不能超过255个字符")
    private String executorHandler;

    @Schema(description = "执行器任务参数")
    @NotBlank(message = "执行器任务参数不能为空")
    @Size(max=512, message="执行器任务参数长度不能超过512个字符")
    private String executorParam;

    @Schema(description = "阻塞处理策略")
    @NotBlank(message = "阻塞处理策略不能为空")
    @Size(max=50, message="阻塞处理策略长度不能超过50个字符")
    private String executorBlockStrategy;

    @Schema(description = "任务执行超时时间，单位秒")
    private Integer executorTimeout;

    @Schema(description = "失败重试次数")
    private Integer executorFailRetryCount;

    @Schema(description = "GLUE类型")
    @Size(max=50, message="GLUE类型长度不能超过50个字符")
    private String glueType;

    @Schema(description = "GLUE源代码")
    @NotNull(message = "GLUE源代码不能为空")
    @Size(max=16777215, message="GLUE源代码长度不能超过16777215个字符")
    private String glueSource;

    @Schema(description = "GLUE备注")
    @NotBlank(message = "GLUE备注不能为空")
    @Size(max=128, message="GLUE备注长度不能超过128个字符")
    private String glueRemark;

    @Schema(description = "GLUE更新时间")
    @NotNull(message = "GLUE更新时间不能为空")
    private LocalDateTime glueUpdatetime;

    @Schema(description = "子任务ID，多个逗号分隔")
    @NotBlank(message = "子任务ID，多个逗号分隔不能为空")
    @Size(max=255, message="子任务ID，多个逗号分隔长度不能超过255个字符")
    private String childJobid;

    @Schema(description = "调度状态：0-停止，1-运行")
    private Integer triggerStatus;

    @Schema(description = "上次调度时间")
    private Long triggerLastTime;

    @Schema(description = "下次调度时间")
    private Long triggerNextTime;


}

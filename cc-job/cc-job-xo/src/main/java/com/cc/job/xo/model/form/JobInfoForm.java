package com.cc.job.xo.model.form;

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
public class JobInfoForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 画布节点ID（前端需要显示的节点唯一标识）
     */
    private String nodeId;

    private Long id;

    @Schema(description = "执行器主键ID")
    @NotNull(message = "执行器不能为空")
    private Long jobGroup;

    @Size(max=255, message="长度不能超过255个字符")
    @NotBlank(message = "任务描述不能为空")
    private String jobDesc;

    @Schema(description = "作者")
    @NotBlank(message = "作者不能为空")
    @Size(max=64, message="作者长度不能超过64个字符")
    private String author;

    @Schema(description = "报警邮件")
    private String alarmEmail;

    @Schema(description = "调度类型")
    @NotBlank(message = "调度类型不能为空")
    @Size(max=50, message="调度类型长度不能超过50个字符")
    private String scheduleType;

    @Schema(description = "调度配置，值含义取决于调度类型")
    private String scheduleConf;

    @Schema(description = "调度过期策略")
    @NotBlank(message = "调度过期策略不能为空")
    private String misfireStrategy;

    @Schema(description = "执行器路由策略")
    @NotBlank(message = "执行器路由策略不能为空")
    private String executorRouteStrategy;

    @Schema(description = "失败策略")
    @NotBlank(message = "失败策略不能为空")
    private String failStrategy;

    @Schema(description = "执行器任务handler")
    private String executorHandler;

    @Schema(description = "执行器任务参数")
    private String executorParam;

    @Schema(description = "阻塞处理策略")
    @NotBlank(message = "阻塞处理策略不能为空")
    private String executorBlockStrategy;

    @Schema(description = "任务执行超时时间，单位秒")
    private Integer executorTimeout;

    @Schema(description = "失败重试次数")
    private Integer executorFailRetryCount;

    @Schema(description = "GLUE类型")
    @NotNull(message = "GLUE类型代码不能为空")
    private String glueType;

    @Schema(description = "GLUE源代码")
    private String glueSource;

    @Schema(description = "GLUE备注")
    private String glueRemark;

    @Schema(description = "GLUE更新时间")
    private LocalDateTime glueUpdateTime;

    @Schema(description = "子任务ID，多个逗号分隔")
    private String childJobId;

    private Integer jobType;

    private Long parentId;

    private String reqType;

    private String reqHeader;

    private String reqBody;

    private String reqUrl;

    private String nodes;

    private String edges;

    private Long jdbcDatasourceId;

    private Integer incrementType;

    private String incrementContent;

    private Integer pauseStatus;

    private Integer jobPartId;

    private Double nodePositionX;

    private Double nodePositionY;

    /**
     * 最近一次运行耗时（毫秒），来自 job_info.run_time
     */
    private Long runTime;
}

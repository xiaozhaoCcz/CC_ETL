package com.cc.job.task.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * task_info视图对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Getter
@Setter
@Schema( description = "task_info视图对象")
public class TaskInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "执行器主键ID")
    private Long jobGroup;
    private String jobDesc;
    private LocalDateTime addTime;
    private LocalDateTime updateTime;
    @Schema(description = "作者")
    private String author;
    @Schema(description = "报警邮件")
    private String alarmEmail;
    @Schema(description = "调度类型")
    private String scheduleType;
    @Schema(description = "调度配置，值含义取决于调度类型")
    private String scheduleConf;
    @Schema(description = "调度过期策略")
    private String misfireStrategy;
    @Schema(description = "执行器路由策略")
    private String executorRouteStrategy;
    @Schema(description = "执行器任务handler")
    private String executorHandler;
    @Schema(description = "执行器任务参数")
    private String executorParam;
    @Schema(description = "阻塞处理策略")
    private String executorBlockStrategy;
    @Schema(description = "任务执行超时时间，单位秒")
    private Integer executorTimeout;
    @Schema(description = "失败重试次数")
    private Integer executorFailRetryCount;
    @Schema(description = "GLUE类型")
    private String glueType;
    @Schema(description = "GLUE源代码")
    private String glueSource;
    @Schema(description = "GLUE备注")
    private String glueRemark;
    @Schema(description = "GLUE更新时间")
    private LocalDateTime glueUpdatetime;
    @Schema(description = "子任务ID，多个逗号分隔")
    private String childJobid;
    @Schema(description = "调度状态：0-停止，1-运行")
    private Integer triggerStatus;
    @Schema(description = "上次调度时间")
    private Long triggerLastTime;
    @Schema(description = "下次调度时间")
    private Long triggerNextTime;
}

package com.cc.job.xo.model.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * task_log视图对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Getter
@Setter
@Schema( description = "task_log视图对象")
public class JobLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "执行器主键ID")
    private Integer jobGroup;
    @Schema(description = "任务，主键ID")
    private Integer jobId;
    @Schema(description = "任务名称")
    private String jobDesc;
    @Schema(description = "执行器地址，本次执行的地址")
    private String executorAddress;
    @Schema(description = "执行器任务handler")
    private String executorHandler;
    @Schema(description = "执行器任务参数")
    private String executorParam;
    @Schema(description = "执行器任务分片参数，格式如 1/2")
    private String executorShardingParam;
    @Schema(description = "失败重试次数")
    private Integer executorFailRetryCount;
    @Schema(description = "调度-时间")
    private String triggerTime;
    @Schema(description = "调度-结果")
    private Integer triggerCode;
    @Schema(description = "调度-日志")
    private String triggerMsg;
    @Schema(description = "执行-时间")
    private String handleTime;
    @Schema(description = "执行-状态")
    private Integer handleCode;
    @Schema(description = "执行-日志")
    private String handleMsg;
    @Schema(description = "告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败")
    private Integer alarmStatus;
    @Schema(description = "任务类型")
    private Integer jobType;
    @Schema(description = "节点执行状态（JSON格式）")
    private String nodeStatus;
}

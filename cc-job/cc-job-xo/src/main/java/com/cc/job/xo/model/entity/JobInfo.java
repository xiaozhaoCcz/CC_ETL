package com.cc.job.xo.model.entity;

import com.cc.job.xo.common.BaseEntity;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * task_info实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Data
@TableName("job_info")
public class JobInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 执行器主键ID
     */
    private Long jobGroup;

    private String jobDesc;
    /**
     * 作者
     */
    private String author;
    /**
     * 报警邮件
     */
    private String alarmEmail;
    /**
     * 调度类型
     */
    private String scheduleType;
    /**
     * 调度配置，值含义取决于调度类型
     */
    private String scheduleConf;
    /**
     * 调度过期策略
     */
    private String misfireStrategy;
    /**
     * 执行器路由策略
     */
    private String executorRouteStrategy;
    /**
     * 失败策略
     */
    private String failStrategy;
    /**
     * 执行器任务handler
     */
    private String executorHandler;
    /**
     * 执行器任务参数
     */
    private String executorParam;
    /**
     * 阻塞处理策略
     */
    private String executorBlockStrategy;
    /**
     * 任务执行超时时间，单位秒
     */
    private Integer executorTimeout;
    /**
     * 失败重试次数
     */
    private Integer executorFailRetryCount;
    /**
     * GLUE类型
     */
    private String glueType;
    /**
     * GLUE源代码
     */
    private String glueSource;
    /**
     * GLUE备注
     */
    private String glueRemark;
    /**
     * GLUE更新时间
     */
    private LocalDateTime glueUpdateTime;
    /**
     * 子任务ID，多个逗号分隔
     */
    private String childJobId;
    /**
     * 调度状态：0-停止，1-运行
     */
    private Integer triggerStatus;
    /**
     * 一次调度状态：0-停止，1-运行
     */
    private Integer triggerOneStatus;
    /**
     * 上次调度时间
     */
    private Long triggerLastTime;
    /**
     * 下次调度时间
     */
    private Long triggerNextTime;
    /**
     * 任务类型：0-普通任务，2-任务组
     */
    private Integer jobType;
    /**
     * 父任务ID
     */
    private Long parentId;
    /**
     * 请求类型（API任务专用）
     */
    private String reqType;
    /**
     * 请求头（API任务专用）
     */
    private String reqHeader;
    /**
     * 请求体（API任务专用）
     */
    private String reqBody;
    /**
     * 请求URL（API任务专用）
     */
    private String reqUrl;
    /**
     * 节点标识：Y-是节点，N-不是节点
     */
    private String nodeFlag;
    /**
     * JDBC数据源ID
     */
    private Long jdbcDatasourceId;
    /**
     * 增量类型：0-全量，1-增量
     */
    private Integer incrementType;
    /**
     * 增量字段配置（JSON格式）
     */
    private String incrementContent;
    /**
     * 最近一次运行耗时（毫秒）
     */
    private Long runTime;
    /**
     * 暂停状态：0-运行，1-暂停
     */
    private Integer pauseStatus;
    /**
     * 任务分区ID
     */
    private Integer jobPartId;
    /**
     * 触发用户ID
     */
    private Integer triggerUserId;
}

package com.cc.job.xo.model.entity;

import com.cc.job.xo.common.BaseEntity;
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
@Getter
@Setter
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
    private LocalDateTime glueUpdatetime;
    /**
     * 子任务ID，多个逗号分隔
     */
    private String childJobid;
    /**
     * 调度状态：0-停止，1-运行
     */
    private Integer triggerStatus;
    /**
     * 上次调度时间
     */
    private Long triggerLastTime;
    /**
     * 下次调度时间
     */
    private Long triggerNextTime;

    private Integer jobType;

    private Long parentId;

    private String reqType;

    private String reqHeader;

    private String reqBody;

    private String reqUrl;

    private String isNode;

    private Integer rankTriggerStatus;

    private Long jdbcDatasourceId;

    private Integer incrType;

    private String incrContent;

    private Long runTime;
}

package com.cc.job.task.model.entity;

import com.cc.job.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * task_log_report实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Getter
@Setter
@TableName("task_log_report")
public class TaskLogReport extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 调度-时间
     */
    private LocalDateTime triggerDay;
    /**
     * 运行中-日志数量
     */
    private Integer runningCount;
    /**
     * 执行成功-日志数量
     */
    private Integer sucCount;
    /**
     * 执行失败-日志数量
     */
    private Integer failCount;
}

package com.cc.job.task.model.entity;

import com.cc.job.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * task_lock实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Getter
@Setter
@TableName("task_lock")
public class TaskLock {

    private static final long serialVersionUID = 1L;

    /**
     * 锁名称
     */
    private String lockName;
}

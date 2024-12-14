package com.cc.job.task.model.entity;

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
@TableName("job_lock")
public class JobLock {

    private static final long serialVersionUID = 1L;

    /**
     * 锁名称
     */
    private String lockName;
}

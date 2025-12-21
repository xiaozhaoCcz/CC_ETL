package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * task_lock实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */

@TableName("job_lock")
public class JobLock {

    private static final long serialVersionUID = 1L;

    /**
     * 锁名称
     */
    private String lockName;

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }
}

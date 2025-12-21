package com.cc.job.xo.model.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * task_lock视图对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Schema( description = "task_lock视图对象")
public class JobLockVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "锁名称")
    private String lockName;

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }
}

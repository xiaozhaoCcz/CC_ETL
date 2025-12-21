package com.cc.job.xo.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * task_lock表单对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Schema(description = "task_lock表单对象")
public class JobLockForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "锁名称")
    @Size(max=50, message="锁名称长度不能超过50个字符")
    private String lockName;

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }
}

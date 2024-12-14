package com.cc.job.task.model.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * task_lock视图对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Getter
@Setter
@Schema( description = "task_lock视图对象")
public class JobLockVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "锁名称")
    private String lockName;
}

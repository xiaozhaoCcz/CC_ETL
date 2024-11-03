package com.cc.job.task.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * task_lock表单对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Getter
@Setter
@Schema(description = "task_lock表单对象")
public class TaskLockForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "锁名称")
    @Size(max=50, message="锁名称长度不能超过50个字符")
    private String lockName;


}

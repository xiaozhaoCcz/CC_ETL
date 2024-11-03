package com.cc.job.task.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

/**
 * 执行器表单对象
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Getter
@Setter
@Schema(description = "执行器表单对象")
public class TaskRegistryForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;

    @Size(max=50, message="长度不能超过50个字符")
    private String registryGroup;

    @Size(max=255, message="长度不能超过255个字符")
    private String registryKey;

    @Size(max=255, message="长度不能超过255个字符")
    private String registryValue;

    @NotNull(message = "不能为空")
    private LocalDateTime updateTime;


}

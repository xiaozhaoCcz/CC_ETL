package com.cc.job.xo.model.form;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * task_group表单对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Getter
@Setter
@Schema(description = "task_group表单对象")
public class JobGroupForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @Schema(description = "执行器AppName")
    @Size(max=40, message="执行器AppName长度不能超过40个字符")
    private String appName;

    @Schema(description = "执行器名称")
    @Size(max=40, message="执行器名称长度不能超过40个字符")
    private String title;

    @Schema(description = "执行器地址类型：0=自动注册、1=手动录入")
    private Integer addressType;

    @Schema(description = "执行器地址列表，多地址逗号分隔")
    @NotBlank(message = "执行器地址列表，多地址逗号分隔不能为空")
    @Size(max=65535, message="执行器地址列表，多地址逗号分隔长度不能超过65535个字符")
    private String addressList;
}

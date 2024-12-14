package com.cc.job.task.model.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * task_group视图对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Getter
@Setter
@Schema( description = "task_group视图对象")
public class JobGroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @Schema(description = "执行器AppName")
    private String appName;
    @Schema(description = "执行器名称")
    private String title;
    @Schema(description = "执行器地址类型：0=自动注册、1=手动录入")
    private Integer addressType;
    @Schema(description = "执行器地址列表，多地址逗号分隔")
    private String addressList;

    private String createTime;

    private String updateTime;
}

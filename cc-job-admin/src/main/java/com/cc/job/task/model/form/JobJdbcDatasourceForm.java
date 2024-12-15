package com.cc.job.task.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

/**
 * jdbc数据源配置表单对象
 *
 * @author ccjob
 * @since 2024-12-14 17:12
 */
@Getter
@Setter
@Schema(description = "jdbc数据源配置表单对象")
public class JobJdbcDatasourceForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "数据源名称")
    @Size(max=200, message="数据源名称长度不能超过200个字符")
    private String datasourceName;

    @Schema(description = "数据源")
    @Size(max=45, message="数据源长度不能超过45个字符")
    private String datasource;

    @Schema(description = "数据源分组")
    private String datasourceGroup;

    @Schema(description = "数据库名")
    @NotBlank(message = "数据库名不能为空")
    @Size(max=45, message="数据库名长度不能超过45个字符")
    private String databaseName;

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(max=100, message="用户名长度不能超过100个字符")
    private String jdbcUsername;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    @Size(max=200, message="密码长度不能超过200个字符")
    private String jdbcPassword;

    @Schema(description = "jdbc url")
    @Size(max=500, message="jdbc url长度不能超过500个字符")
    private String jdbcUrl;

    @Schema(description = "jdbc驱动类")
    @NotBlank(message = "jdbc驱动类不能为空")
    @Size(max=200, message="jdbc驱动类长度不能超过200个字符")
    private String jdbcDriverClass;

    @Schema(description = "备注")
    @NotBlank(message = "备注不能为空")
    @Size(max=1000, message="备注长度不能超过1000个字符")
    private String comments;


}

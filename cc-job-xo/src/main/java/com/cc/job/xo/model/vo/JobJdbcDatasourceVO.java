package com.cc.job.xo.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * jdbc数据源配置视图对象
 *
 * @author ccjob
 * @since 2024-12-14 17:12
 */
@Getter
@Setter
@Schema( description = "jdbc数据源配置视图对象")
public class JobJdbcDatasourceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "自增主键")
    private Long id;
    @Schema(description = "数据源名称")
    private String datasourceName;
    @Schema(description = "数据源")
    private String datasource;
    @Schema(description = "数据源分组")
    private String datasourceGroup;
    @Schema(description = "数据库名")
    private String databaseName;
    @Schema(description = "用户名")
    private String jdbcUsername;
    @Schema(description = "密码")
    private String jdbcPassword;
    @Schema(description = "jdbc url")
    private String jdbcUrl;
    @Schema(description = "jdbc驱动类")
    private String jdbcDriverClass;
    @Schema(description = "状态：0删除 1启用 2禁用")
    private Integer status;
    @Schema(description = "创建人")
    private String createBy;
    @Schema(description = "创建时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private LocalDateTime createTime;
    @Schema(description = "更新人")
    private String updateBy;
    @Schema(description = "更新时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private LocalDateTime updateTime;
    @Schema(description = "备注")
    private String comments;
}

package com.cc.job.xo.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * jdbc数据源配置视图对象
 *
 * @author ccjob
 * @since 2024-12-14 17:12
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getDatasourceGroup() {
        return datasourceGroup;
    }

    public void setDatasourceGroup(String datasourceGroup) {
        this.datasourceGroup = datasourceGroup;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcDriverClass() {
        return jdbcDriverClass;
    }

    public void setJdbcDriverClass(String jdbcDriverClass) {
        this.jdbcDriverClass = jdbcDriverClass;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}

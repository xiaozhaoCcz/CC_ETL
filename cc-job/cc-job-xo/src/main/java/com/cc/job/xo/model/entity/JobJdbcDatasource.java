package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;



@TableName("job_jdbc_datasource")
public class JobJdbcDatasource extends BaseEntity {

   private String datasourceName;

   private String datasource;

   private String datasourceGroup;

   private String databaseName;

   private String jdbcUsername;

   private String jdbcPassword;

   private String jdbcUrl;

   private String jdbcDriverClass;

   private Integer status;

   private String comments;

   private String schemaName;

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

   public String getComments() {
      return comments;
   }

   public void setComments(String comments) {
      this.comments = comments;
   }

   public String getSchemaName() {
      return schemaName;
   }

   public void setSchemaName(String schemaName) {
      this.schemaName = schemaName;
   }
}

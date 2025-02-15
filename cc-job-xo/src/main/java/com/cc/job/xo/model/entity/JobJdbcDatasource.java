package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
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
}

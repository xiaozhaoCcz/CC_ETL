package com.cc.job.xo.model.query;

import com.cc.job.xo.common.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description ="jdbc数据源配置查询对象")
public class JobJdbcDatasourceQuery extends BasePageQuery {

    private String datasourceName;

    private String datasource;

    private String databaseName;

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

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
}

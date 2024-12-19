package com.cc.job.xo.model.query;

import com.cc.job.xo.common.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description ="jdbc数据源配置查询对象")
@Getter
@Setter
public class JobJdbcDatasourceQuery extends BasePageQuery {

    private String datasourceName;

    private String datasource;

    private String databaseName;
}

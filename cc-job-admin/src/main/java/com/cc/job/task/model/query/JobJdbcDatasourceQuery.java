package com.cc.job.task.model.query;

import com.cc.job.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description ="jdbc数据源配置查询对象")
@Getter
@Setter
public class JobJdbcDatasourceQuery extends BasePageQuery {

}

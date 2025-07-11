package com.cc.job.xo.model.query;

import com.cc.job.xo.common.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 执行器分页查询对象
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Schema(description ="执行器查询对象")
@Getter
@Setter
public class JobRegistryQuery extends BasePageQuery {

}

package com.cc.job.task.model.query;

import com.cc.job.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * task_lock分页查询对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Schema(description ="task_lock查询对象")
@Getter
@Setter
public class JobLockQuery extends BasePageQuery {

}

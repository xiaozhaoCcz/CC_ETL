package com.cc.job.task.model.query;

import com.cc.job.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * task_log分页查询对象
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Schema(description ="task_log查询对象")
@Getter
@Setter
public class TaskLogQuery extends BasePageQuery {

}

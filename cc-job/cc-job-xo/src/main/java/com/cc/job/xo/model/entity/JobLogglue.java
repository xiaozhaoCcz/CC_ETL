package com.cc.job.xo.model.entity;

import com.cc.job.xo.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * task_logglue实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Getter
@Setter
@TableName("job_logglue")
public class JobLogglue extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 任务，主键ID
     */
    private Long jobId;
    /**
     * GLUE类型
     */
    private String glueType;
    /**
     * GLUE源代码
     */
    private String glueSource;
    /**
     * GLUE备注
     */
    private String glueRemark;

}

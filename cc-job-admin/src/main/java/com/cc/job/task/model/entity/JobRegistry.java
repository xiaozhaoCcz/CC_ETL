package com.cc.job.task.model.entity;

import com.cc.job.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 执行器实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Getter
@Setter
@TableName("job_registry")
public class JobRegistry extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String registryGroup;
    private String registryKey;
    private String registryValue;
}

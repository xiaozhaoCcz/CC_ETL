package com.cc.job.task.model.entity;

import com.cc.job.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * task_group实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Getter
@Setter
@TableName("task_group")
public class TaskGroup extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 执行器AppName
     */
    private String appName;
    /**
     * 执行器名称
     */
    private String title;
    /**
     * 执行器地址类型：0=自动注册、1=手动录入
     */
    private Integer addressType;
    /**
     * 执行器地址列表，多地址逗号分隔
     */
    private String addressList;
}

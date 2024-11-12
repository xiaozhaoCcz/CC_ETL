package com.cc.job.task.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("task_node")
@AllArgsConstructor
public class TaskNode extends BaseEntity {

    private Long taskId;

    private Double nodePositionX;

    private Double nodePositionY;

    private Integer nodeInDegree;

    private Integer nodeOutDegree;

    private Integer sort;
}

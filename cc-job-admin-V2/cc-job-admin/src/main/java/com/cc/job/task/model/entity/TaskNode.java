package com.cc.job.task.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@TableName("task_node")
public class TaskNode extends BaseEntity {

    private Long taskId;

    private Long taskParentId;

    private Double nodePositionX;

    private Double nodePositionY;

    private Long nodeInDegree;

    private Long nodeOutDegree;

    private Integer sort;
}

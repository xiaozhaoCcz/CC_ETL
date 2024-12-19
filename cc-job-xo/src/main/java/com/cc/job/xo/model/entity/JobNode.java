package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@TableName("job_node")
@ToString
public class JobNode extends BaseEntity {

    private Long taskId;

    private Long taskParentId;

    private Double nodePositionX;

    private Double nodePositionY;

    private Long nodeInDegree;

    private Long nodeOutDegree;

    private Integer sort;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JobNode taskNode = (JobNode) o;
        return Objects.equals(taskId, taskNode.taskId) && Objects.equals(taskParentId, taskNode.taskParentId) && Objects.equals(nodePositionX, taskNode.nodePositionX) && Objects.equals(nodePositionY, taskNode.nodePositionY) && Objects.equals(nodeInDegree, taskNode.nodeInDegree) && Objects.equals(nodeOutDegree, taskNode.nodeOutDegree) && Objects.equals(sort, taskNode.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), taskId, taskParentId, nodePositionX, nodePositionY, nodeInDegree, nodeOutDegree, sort);
    }
}

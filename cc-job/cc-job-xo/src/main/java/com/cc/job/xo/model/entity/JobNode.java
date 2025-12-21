package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;

import java.util.Objects;

@TableName("job_node")
public class JobNode extends BaseEntity {

    private Long jobId;

    private Long jobParentId;

    private Double nodePositionX;

    private Double nodePositionY;

    private Long nodeInDegree;

    private Long nodeOutDegree;

    private Integer sort;

    private String children;

    private String properties;

    private String nodeType;

    /**
     * 节点运行状态：-1=未运行, 0=失败, 1=成功, 2=运行中
     */
    private Integer triggerStatus;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JobNode taskNode = (JobNode) o;
        return Objects.equals(jobId, taskNode.jobId) && Objects.equals(jobParentId, taskNode.jobParentId) && Objects.equals(nodePositionX, taskNode.nodePositionX) && Objects.equals(nodePositionY, taskNode.nodePositionY) && Objects.equals(nodeInDegree, taskNode.nodeInDegree) && Objects.equals(nodeOutDegree, taskNode.nodeOutDegree) && Objects.equals(sort, taskNode.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), jobId, jobParentId, nodePositionX, nodePositionY, nodeInDegree, nodeOutDegree, sort);
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getJobParentId() {
        return jobParentId;
    }

    public void setJobParentId(Long jobParentId) {
        this.jobParentId = jobParentId;
    }

    public Double getNodePositionX() {
        return nodePositionX;
    }

    public void setNodePositionX(Double nodePositionX) {
        this.nodePositionX = nodePositionX;
    }

    public Double getNodePositionY() {
        return nodePositionY;
    }

    public void setNodePositionY(Double nodePositionY) {
        this.nodePositionY = nodePositionY;
    }

    public Long getNodeInDegree() {
        return nodeInDegree;
    }

    public void setNodeInDegree(Long nodeInDegree) {
        this.nodeInDegree = nodeInDegree;
    }

    public Long getNodeOutDegree() {
        return nodeOutDegree;
    }

    public void setNodeOutDegree(Long nodeOutDegree) {
        this.nodeOutDegree = nodeOutDegree;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getChildren() {
        return children;
    }

    public void setChildren(String children) {
        this.children = children;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public Integer getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(Integer triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    @Override
    public String toString() {
        return "JobNode{" +
                "jobId=" + jobId +
                ", jobParentId=" + jobParentId +
                ", nodePositionX=" + nodePositionX +
                ", nodePositionY=" + nodePositionY +
                ", nodeInDegree=" + nodeInDegree +
                ", nodeOutDegree=" + nodeOutDegree +
                ", sort=" + sort +
                ", children='" + children + '\'' +
                ", properties='" + properties + '\'' +
                ", nodeType='" + nodeType + '\'' +
                ", triggerStatus=" + triggerStatus +
                '}';
    }
}

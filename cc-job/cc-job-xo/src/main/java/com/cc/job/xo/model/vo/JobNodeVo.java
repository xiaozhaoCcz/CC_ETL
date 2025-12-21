package com.cc.job.xo.model.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JobNodeVo implements Serializable {

    private String id;

    private Long jobId;

    private String jobName;

    private Long jobParentId;

    private Double nodePositionX;

    private Double nodePositionY;

    private Long nodeInDegree;

    private Long nodeOutDegree;

    private Integer sort;

    private String nodePatentId;

    private String properties;

    private String children;

    private String nodeType;

    private Integer pauseStatus;

    private List<JobNodeVo> childrenNodes = new ArrayList<>();

    /**
     * 节点运行状态：0=失败, 1=成功, 2=运行中
     */
    private Integer triggerStatus;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
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

    public String getNodePatentId() {
        return nodePatentId;
    }

    public void setNodePatentId(String nodePatentId) {
        this.nodePatentId = nodePatentId;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public String getChildren() {
        return children;
    }

    public void setChildren(String children) {
        this.children = children;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public Integer getPauseStatus() {
        return pauseStatus;
    }

    public void setPauseStatus(Integer pauseStatus) {
        this.pauseStatus = pauseStatus;
    }

    public List<JobNodeVo> getChildrenNodes() {
        return childrenNodes;
    }

    public void setChildrenNodes(List<JobNodeVo> childrenNodes) {
        this.childrenNodes = childrenNodes;
    }

    public Integer getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(Integer triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobNodeVo jobNodeVo = (JobNodeVo) o;
        return Objects.equals(id, jobNodeVo.id) &&
                Objects.equals(jobId, jobNodeVo.jobId) &&
                Objects.equals(jobName, jobNodeVo.jobName) &&
                Objects.equals(jobParentId, jobNodeVo.jobParentId) &&
                Objects.equals(nodePositionX, jobNodeVo.nodePositionX) &&
                Objects.equals(nodePositionY, jobNodeVo.nodePositionY) &&
                Objects.equals(nodeInDegree, jobNodeVo.nodeInDegree) &&
                Objects.equals(nodeOutDegree, jobNodeVo.nodeOutDegree) &&
                Objects.equals(sort, jobNodeVo.sort) &&
                Objects.equals(nodePatentId, jobNodeVo.nodePatentId) &&
                Objects.equals(properties, jobNodeVo.properties) &&
                Objects.equals(children, jobNodeVo.children) &&
                Objects.equals(nodeType, jobNodeVo.nodeType) &&
                Objects.equals(pauseStatus, jobNodeVo.pauseStatus) &&
                Objects.equals(childrenNodes, jobNodeVo.childrenNodes) &&
                Objects.equals(triggerStatus, jobNodeVo.triggerStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, jobId, jobName, jobParentId, nodePositionX, nodePositionY, nodeInDegree, nodeOutDegree, sort, nodePatentId, properties, children, nodeType, pauseStatus, childrenNodes, triggerStatus);
    }

    @Override
    public String toString() {
        return "JobNodeVo{" +
                "id='" + id + '\'' +
                ", jobId=" + jobId +
                ", jobName='" + jobName + '\'' +
                ", jobParentId=" + jobParentId +
                ", nodePositionX=" + nodePositionX +
                ", nodePositionY=" + nodePositionY +
                ", nodeInDegree=" + nodeInDegree +
                ", nodeOutDegree=" + nodeOutDegree +
                ", sort=" + sort +
                ", nodePatentId='" + nodePatentId + '\'' +
                ", properties='" + properties + '\'' +
                ", children='" + children + '\'' +
                ", nodeType='" + nodeType + '\'' +
                ", pauseStatus=" + pauseStatus +
                ", childrenNodes=" + childrenNodes +
                ", triggerStatus=" + triggerStatus +
                '}';
    }
}

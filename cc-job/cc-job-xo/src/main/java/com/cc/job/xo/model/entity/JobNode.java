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

    private String nodeParentId;

    /**
     * 条件表达式（条件节点专用）
     */
    private String conditionExpression;

    /**
     * 表达式类型：SIMPLE-简单表达式，SCRIPT-脚本表达式
     */
    private String expressionType;

    /**
     * 节点运行状态：-1=未运行, 0=失败, 1=成功, 2=运行中
     */
    private Integer triggerStatus;

    /**
     * 节点备注
     */
    private String remark;

    /**
     * 节点标签（JSON格式存储标签列表）
     */
    private String tags;


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

    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    public String getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(String expressionType) {
        this.expressionType = expressionType;
    }

    public Integer getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(Integer triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getNodeParentId() {
        return nodeParentId;
    }

    public void setNodeParentId(String nodeParentId) {
        this.nodeParentId = nodeParentId;
    }

}

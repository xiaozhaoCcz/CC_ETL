package com.cc.job.xo.model.dto;


import java.io.Serializable;


public class JobNodeDto implements Serializable {

    private String id;

    private Long jobId;

    private Long jobParentId;

    private Double nodePositionX;

    private Double nodePositionY;

    private Long nodeInDegree;

    private Long nodeOutDegree;

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
}

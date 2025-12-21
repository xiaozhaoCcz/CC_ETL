package com.cc.job.xo.model.vo;

import java.io.Serializable;
import java.util.Objects;

public class JobEdgeVo implements Serializable {

    private String id;

    private Long jobParentId;

    private String fromNodeId;

    private String endNodeId;
    
    private String startPoint;  // 起始锚点
    
    private String endPoint;    // 结束锚点
    
    private String properties;  // 边属性

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getJobParentId() {
        return jobParentId;
    }

    public void setJobParentId(Long jobParentId) {
        this.jobParentId = jobParentId;
    }

    public String getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(String fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public String getEndNodeId() {
        return endNodeId;
    }

    public void setEndNodeId(String endNodeId) {
        this.endNodeId = endNodeId;
    }

    public String getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(String startPoint) {
        this.startPoint = startPoint;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobEdgeVo jobEdgeVo = (JobEdgeVo) o;
        return Objects.equals(id, jobEdgeVo.id) &&
                Objects.equals(jobParentId, jobEdgeVo.jobParentId) &&
                Objects.equals(fromNodeId, jobEdgeVo.fromNodeId) &&
                Objects.equals(endNodeId, jobEdgeVo.endNodeId) &&
                Objects.equals(startPoint, jobEdgeVo.startPoint) &&
                Objects.equals(endPoint, jobEdgeVo.endPoint) &&
                Objects.equals(properties, jobEdgeVo.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, jobParentId, fromNodeId, endNodeId, startPoint, endPoint, properties);
    }

    @Override
    public String toString() {
        return "JobEdgeVo{" +
                "id='" + id + '\'' +
                ", jobParentId=" + jobParentId +
                ", fromNodeId='" + fromNodeId + '\'' +
                ", endNodeId='" + endNodeId + '\'' +
                ", startPoint='" + startPoint + '\'' +
                ", endPoint='" + endPoint + '\'' +
                ", properties='" + properties + '\'' +
                '}';
    }
}

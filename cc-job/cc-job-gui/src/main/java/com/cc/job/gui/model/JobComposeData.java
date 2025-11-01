package com.cc.job.gui.model;

import java.util.List;
import java.util.Map;

/**
 * 任务组合数据模型
 */
public class JobComposeData {
    
    private List<NodeData> nodes;
    private List<EdgeData> edges;
    
    public JobComposeData() {
    }
    
    public List<NodeData> getNodes() {
        return nodes;
    }
    
    public void setNodes(List<NodeData> nodes) {
        this.nodes = nodes;
    }
    
    public List<EdgeData> getEdges() {
        return edges;
    }
    
    public void setEdges(List<EdgeData> edges) {
        this.edges = edges;
    }
    
    /**
     * 节点数据
     */
    public static class NodeData {
        private String id;
        private Long jobId;      // 任务ID，用于编辑节点
        private String type;
        private String jobName;  // 节点显示名称
        private Double x;
        private Double y;
        private Integer triggerStatus; // 节点运行状态：0=失败, 1=成功, 2=运行中
        private Map<String, Object> properties;
        
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
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getJobName() {
            return jobName;
        }
        
        public void setJobName(String jobName) {
            this.jobName = jobName;
        }
        
        public Double getX() {
            return x;
        }
        
        public void setX(Double x) {
            this.x = x;
        }
        
        public Double getY() {
            return y;
        }
        
        public void setY(Double y) {
            this.y = y;
        }
        
        public Integer getTriggerStatus() {
            return triggerStatus;
        }
        
        public void setTriggerStatus(Integer triggerStatus) {
            this.triggerStatus = triggerStatus;
        }
        
        public Map<String, Object> getProperties() {
            return properties;
        }
        
        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
        
        @Override
        public String toString() {
            return "NodeData{" +
                    "id='" + id + '\'' +
                    ", jobId=" + jobId +
                    ", type='" + type + '\'' +
                    ", jobName='" + jobName + '\'' +
                    ", x=" + x +
                    ", y=" + y +
                    ", properties=" + properties +
                    '}';
        }
    }
    
    /**
     * 边数据
     */
    public static class EdgeData {
        private String id;
        private String sourceNodeId;
        private String targetNodeId;
        private String sourceAnchor;
        private String targetAnchor;
        private String type;
        private Map<String, Object> properties;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getSourceNodeId() {
            return sourceNodeId;
        }
        
        public void setSourceNodeId(String sourceNodeId) {
            this.sourceNodeId = sourceNodeId;
        }
        
        public String getTargetNodeId() {
            return targetNodeId;
        }
        
        public void setTargetNodeId(String targetNodeId) {
            this.targetNodeId = targetNodeId;
        }
        
        public String getSourceAnchor() {
            return sourceAnchor;
        }
        
        public void setSourceAnchor(String sourceAnchor) {
            this.sourceAnchor = sourceAnchor;
        }
        
        public String getTargetAnchor() {
            return targetAnchor;
        }
        
        public void setTargetAnchor(String targetAnchor) {
            this.targetAnchor = targetAnchor;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Map<String, Object> getProperties() {
            return properties;
        }
        
        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
        
        @Override
        public String toString() {
            return "EdgeData{" +
                    "id='" + id + '\'' +
                    ", sourceNodeId='" + sourceNodeId + '\'' +
                    ", targetNodeId='" + targetNodeId + '\'' +
                    ", sourceAnchor='" + sourceAnchor + '\'' +
                    ", targetAnchor='" + targetAnchor + '\'' +
                    ", type='" + type + '\'' +
                    ", properties=" + properties +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "JobComposeData{" +
                "nodes=" + (nodes != null ? nodes.size() : 0) +
                ", edges=" + (edges != null ? edges.size() : 0) +
                '}';
    }
}


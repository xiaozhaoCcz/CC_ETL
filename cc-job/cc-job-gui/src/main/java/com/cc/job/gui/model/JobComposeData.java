package com.cc.job.gui.model;

import java.util.List;
import java.util.Map;

/**
 * 任务组合数据模型
 */
public class JobComposeData {
    
    private List<NodeData> nodes;
    private List<EdgeData> edges;
    // 后端返回的根任务组“聚合节点”，包含 children 列表与总体尺寸/位置等
    private NodeData jobNode;
    
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
    
    public NodeData getJobNode() {
        return jobNode;
    }
    
    public void setJobNode(NodeData jobNode) {
        this.jobNode = jobNode;
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
        private List<NodeData> childrenNodes;  // ⭐ 新增：子节点列表（用于任务组节点的嵌套展示）
        private Long jobParentId;  // ⭐ 新增：父任务组ID
        
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
        
        public List<NodeData> getChildrenNodes() {
            return childrenNodes;
        }
        
        public void setChildrenNodes(List<NodeData> childrenNodes) {
            this.childrenNodes = childrenNodes;
        }
        
        public Long getJobParentId() {
            return jobParentId;
        }
        
        public void setJobParentId(Long jobParentId) {
            this.jobParentId = jobParentId;
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
                    ", childrenNodesCount=" + (childrenNodes != null ? childrenNodes.size() : 0) +
                    ", jobParentId=" + jobParentId +
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


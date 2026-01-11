package com.cc.job.gui.manager;

import com.cc.job.gui.model.JobComposeData;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.util.NodeStatusSyncManager;

import java.util.*;
import java.util.function.Consumer;

/**
 * 画布数据加载器 - 负责从 JobComposeData 加载数据到画布
 */
public class CanvasDataLoader {
    
    private final Consumer<String> logger;
    
    public CanvasDataLoader(Consumer<String> logger) {
        this.logger = logger;
    }
    
    /**
     * 加载节点数据
     */
    public Map<String, ProcessNode> loadNodes(List<JobComposeData.NodeData> nodeDataList, Consumer<ProcessNode> addNodeCallback) {
        Map<String, ProcessNode> nodeMap = new HashMap<>();
        if (nodeDataList == null || nodeDataList.isEmpty()) return nodeMap;
        
        for (JobComposeData.NodeData nodeData : nodeDataList) {
            if (isGroupNode(nodeData)) continue;
            
            ProcessNode node = createNode(nodeData);
            addNodeCallback.accept(node);
            nodeMap.put(nodeData.getId(), node);
        }
        
        logger.accept("✓ 加载了 " + nodeMap.size() + " 个节点");
        return nodeMap;
    }
    
    private ProcessNode createNode(JobComposeData.NodeData nodeData) {
        return createNodeFromData(nodeData);
    }
    
    /**
     * ⭐ 新增：从NodeData创建ProcessNode（公共方法，供外部调用）
     */
    public ProcessNode createNodeFromData(JobComposeData.NodeData nodeData) {
        String text = nodeData.getJobName() != null ? nodeData.getJobName() : "Node";
        ProcessNode node = new ProcessNode(nodeData.getId(), text);
        
        if (nodeData.getJobId() != null) {
            node.setJobId(nodeData.getJobId());
            NodeStatusSyncManager.getInstance().rememberStatus(nodeData.getJobId(), nodeData.getTriggerStatus());
        }
        
        Integer triggerStatus = resolveNodeStatus(nodeData);
        if (triggerStatus != null) {
            node.updateStatusByCode(triggerStatus);
            if (nodeData.getJobId() != null) {
                NodeStatusSyncManager.getInstance().rememberStatus(nodeData.getJobId(), triggerStatus);
            }
        }
        
        if (hasValidCoordinates(nodeData.getX(), nodeData.getY())) {
            node.setLayoutX(nodeData.getX());
            node.setLayoutY(nodeData.getY());
        }
        
        node.setType(mapNodeType(nodeData.getType(), nodeData.getProperties()));
        
        // 从properties读取颜色并应用
        if (nodeData.getProperties() != null) {
            Object colorObj = nodeData.getProperties().get("color");
            if (colorObj != null) {
                String color = colorObj.toString();
                if (color != null && !color.isEmpty()) {
                    node.setNodeColor(color);
                }
            }
            
            // 从properties读取标签
            Object tagsObj = nodeData.getProperties().get("tags");
            if (tagsObj != null) {
                java.util.List<String> tags = new java.util.ArrayList<>();
                if (tagsObj instanceof java.util.List) {
                    for (Object tag : (java.util.List<?>) tagsObj) {
                        if (tag != null) {
                            tags.add(tag.toString());
                        }
                    }
                } else if (tagsObj instanceof String) {
                    // 尝试解析JSON数组字符串
                    try {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        java.util.List<?> tagList = gson.fromJson((String) tagsObj, java.util.List.class);
                        for (Object tag : tagList) {
                            if (tag != null) {
                                tags.add(tag.toString());
                            }
                        }
                    } catch (Exception e) {
                        // 解析失败，忽略
                    }
                }
                node.setTags(tags);
            }
            
            // 从properties读取备注
            Object remarkObj = nodeData.getProperties().get("remark");
            if (remarkObj != null) {
                String remark = remarkObj.toString();
                if (remark != null && !remark.trim().isEmpty()) {
                    node.setRemark(remark);
                }
            }
        }
        
        return node;
    }
    
    private boolean isGroupNode(JobComposeData.NodeData nodeData) {
        String nodeType = nodeData.getType();
        if (nodeType != null && !nodeType.trim().isEmpty() && !"null".equals(nodeType)) {
            String normalized = nodeType.trim();
            if (normalized.equals("CustomGroup") || normalized.equalsIgnoreCase("custom-group")) {
                return true;
            }
        }
        if (nodeData.getProperties() != null && nodeData.getProperties().containsKey("children")) {
            return true;
        }
        return false;
    }
    
    private Integer resolveNodeStatus(JobComposeData.NodeData nodeData) {
        Long nodeJobId = nodeData.getJobId();
        Integer backendStatus = nodeData.getTriggerStatus();
        Integer cachedStatus = nodeJobId != null ? 
            NodeStatusSyncManager.getInstance().getCachedStatus(nodeJobId) : null;
        
        if (backendStatus != null && cachedStatus != null) {
            if (backendStatus == 2 && (cachedStatus == 0 || cachedStatus == 1)) {
                return cachedStatus;
            } else {
                return backendStatus;
            }
        } else if (cachedStatus != null) {
            return cachedStatus;
        } else if (backendStatus != null) {
            return backendStatus;
        }
        return null;
    }
    
    private boolean hasValidCoordinates(Double x, Double y) {
        if (x == null || y == null) return false;
        if (x.isNaN() || x.isInfinite() || y.isNaN() || y.isInfinite()) return false;
        return Math.abs(x) + Math.abs(y) > 1e-3;
    }
    
    private String mapNodeType(String rawType, Map<String, Object> properties) {
        String candidate = rawType;
        if ((candidate == null || candidate.isBlank()) && properties != null) {
            Object glueType = properties.get("glueType");
            if (glueType instanceof String) {
                candidate = (String) glueType;
            }
        }
        if (candidate == null || candidate.isBlank()) return "Bean";
        
        String normalized = candidate.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "bean", "custom-bean" -> "Bean";
            case "api", "custom-api" -> "API";
            case "sql", "custom-sql" -> "SQL";
            case "java", "glue(java)", "custom-java" -> "Java";
            case "shell", "glue(shell)", "custom-shell" -> "Shell";
            case "python", "glue(python)", "custom-python" -> "Python";
            case "php", "glue(php)", "custom-php" -> "PHP";
            case "node", "nodejs", "glue(nodejs)", "custom-nodejs" -> "Node";
            case "powershell", "ps", "glue(powershell)", "custom-powershell" -> "PS";
            case "csharp", "c#", "glue(csharp)", "custom-csharp" -> "C#";
            default -> "Bean";
        };
    }
}


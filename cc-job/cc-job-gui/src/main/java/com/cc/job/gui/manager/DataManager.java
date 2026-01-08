package com.cc.job.gui.manager;

import com.cc.job.gui.model.*;
import com.cc.job.gui.service.*;
import com.cc.job.gui.util.ApiUtil;
import com.cc.job.gui.util.NodeStatusSyncManager;
import com.cc.job.gui.util.NotificationToast;
import com.cc.job.gui.view.*;
import com.cc.job.xo.model.form.JobInfoForm;
import javafx.application.Platform;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 数据管理器 - 负责数据的加载和保存
 */
public class DataManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);
    
    private final NodeCanvas canvas;
    private final LogPanel logPanel;
    private final TaskTreeView treeView;
    
    private final JobPartService jobPartService;
    private final JobInfoService jobInfoService;
    private final ApiUtil apiUtil;
    
    private Runnable onDataLoadedCallback;
    
    public DataManager(NodeCanvas canvas, LogPanel logPanel, TaskTreeView treeView) {
        this.canvas = canvas;
        this.logPanel = logPanel;
        this.treeView = treeView;
        this.jobPartService = new JobPartService();
        this.jobInfoService = new JobInfoService();
        this.apiUtil = ApiUtil.getInstance();
    }

    
    /**
     * 设置数据加载完成后的回调
     */
    public void setOnDataLoaded(Runnable callback) {
        this.onDataLoadedCallback = callback;
    }
    
    /**
     * 加载任务组数据
     */
    public void loadTaskGroupData(Long taskId, String taskName) {
        logPanel.info("════════════════════════════════");
        logPanel.info("开始加载任务组: " + taskName);
        
        // 在加载前禁用自动保存，防止加载过程中误触发保存
        canvas.disableAutoSave();
        
        new Thread(() -> {
            try {
                canvas.syncPendingNodeStatusBlocking();
                Platform.runLater(() -> logPanel.info("节点状态同步完成"));
                
                JobComposeData composeData = jobPartService.getJobCompose(taskId);
                
                Platform.runLater(() -> {
                    try {
                        NodeStatusSyncManager.getInstance().clearCacheForTaskGroupSwitch();
                        
                        if (composeData != null) {
                            canvas.loadFromComposeData(composeData);
                            setupEditCallbacks(composeData);
                            
                            int nodeCount = composeData.getNodes() != null ? composeData.getNodes().size() : 0;
                            int edgeCount = composeData.getEdges() != null ? composeData.getEdges().size() : 0;
                            
                            logPanel.success("✓ 任务组加载成功！");
                            logPanel.info("节点数: " + nodeCount + ", 连接数: " + edgeCount);
                            
                            // 清除未保存标记（刚加载的数据是已保存状态）
                            canvas.markAsSaved();
                            
                            // 调用数据加载完成回调
                            if (onDataLoadedCallback != null) {
                                onDataLoadedCallback.run();
                            }
                        } else {
                            logPanel.warn("⚠ 任务组数据为空");
                            canvas.clear();
                            canvas.markAsSaved();
                        }
                    } finally {
                        // 确保无论如何都重新启用自动保存
                        canvas.enableAutoSave();
                        logPanel.info("════════════════════════════════");
                    }
                });
            } catch (Exception e) {
                logger.error("加载任务组数据失败", e);
                Platform.runLater(() -> {
                    try {
                        logPanel.error("✗ 加载失败: " + e.getMessage());
                    } finally {
                        // 确保即使加载失败也要重新启用自动保存
                        canvas.enableAutoSave();
                    }
                });
            }
        }).start();
    }
    
    /**
     * 保存或更新任务组（异步执行，不阻塞调用线程）
     */
    public void saveOrUpdateJob(Long currentTaskGroupId) {
        saveOrUpdateJobInternal(currentTaskGroupId, true);
    }
    
    /**
     * 保存或更新任务组（同步执行，会阻塞调用线程）
     * ⚠️ 此方法会阻塞，必须在后台线程中调用，不要在JavaFX主线程中调用
     */
    public void saveOrUpdateJobSync(Long currentTaskGroupId) {
        saveOrUpdateJobInternal(currentTaskGroupId, false);
    }
    
    /**
     * 保存或更新任务组（内部方法）
     * @param currentTaskGroupId 任务组ID
     * @param async 是否异步执行
     */
    private void saveOrUpdateJobInternal(Long currentTaskGroupId, boolean async) {
        logPanel.info("════════════════════════════════");
        logPanel.info("💾 开始保存任务组数据... (" + (async ? "异步" : "同步") + ")");
        
        if (currentTaskGroupId == null || currentTaskGroupId == 0) {
            NotificationToast.showWarning("请先选择一个任务组后再进行保存操作。");
            return;
        }
        
        List<ProcessNode> nodes = canvas.getNodes();
        List<NodeConnection> connections = canvas.getConnections();
        List<GroupContainer> groups = canvas.getGroupContainers();
        List<com.cc.job.gui.model.ConditionNode> conditionNodes = canvas.getConditionNodes();
        
        logPanel.info(String.format("节点: %d, 连接: %d, 任务组: %d, 条件节点: %d", 
            nodes.size(), connections.size(), groups.size(), conditionNodes.size()));
        
        try {
            // 构建节点数据
            Set<String> addedNodeIds = new HashSet<>();
            List<Map<String, Object>> nodesData = new ArrayList<>();
            
            for (ProcessNode node : nodes) {
                if (node.getNodeId() == null || addedNodeIds.contains(node.getNodeId())) continue;
                
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("id", node.getNodeId());
                nodeData.put("type", node.getType() != null ? node.getType() : "rect");
                nodeData.put("x", node.getX());
                nodeData.put("y", node.getY());
                
                Map<String, Object> propertiesMap = new HashMap<>();
                if (node.getJobId() != null) {
                    propertiesMap.put("jobId", node.getJobId());
                }
                nodeData.put("properties", apiUtil.getGson().toJson(propertiesMap));
                
                nodesData.add(nodeData);
                addedNodeIds.add(node.getNodeId());
            }
            
            // 添加任务组节点
            for (GroupContainer group : groups) {
                if (group.getNodeId() == null) continue;
                
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("id", group.getNodeId());
                nodeData.put("type", "CustomGroup");
                nodeData.put("x", group.getLayoutX());
                nodeData.put("y", group.getLayoutY());
                
                Map<String, Object> propertiesMap = new HashMap<>();
                if (group.getGroupId() != null) {
                    propertiesMap.put("jobId", group.getGroupId());
                }
                
                List<String> childNodeIds = new ArrayList<>();
                if (group.getManagedCanvasNodes() != null) {
                    for (ProcessNode child : group.getManagedCanvasNodes()) {
                        if (child.getNodeId() != null) {
                            childNodeIds.add(child.getNodeId());
                        }
                    }
                }
                propertiesMap.put("children", childNodeIds);
                nodeData.put("properties", apiUtil.getGson().toJson(propertiesMap));
                
                nodesData.add(nodeData);
                addedNodeIds.add(group.getNodeId());
            }
            
            // 添加条件节点
            for (com.cc.job.gui.model.ConditionNode conditionNode : conditionNodes) {
                if (conditionNode.getNodeId() == null || addedNodeIds.contains(conditionNode.getNodeId())) continue;
                
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("id", conditionNode.getNodeId());
                nodeData.put("type", "ConditionNode");
                nodeData.put("x", conditionNode.getLayoutX());
                nodeData.put("y", conditionNode.getLayoutY());
                
                Map<String, Object> propertiesMap = new HashMap<>();
                if (conditionNode.getConditionId() != null && conditionNode.getConditionId() > 0) {
                    propertiesMap.put("jobId", conditionNode.getConditionId());
                }
                
                // 保存条件节点属性
                if (conditionNode.getConditionExpression() != null) {
                    propertiesMap.put("conditionExpression", conditionNode.getConditionExpression());
                }
                if (conditionNode.getExpressionType() != null) {
                    propertiesMap.put("expressionType", conditionNode.getExpressionType().toString());
                }
                // ⭐ 新增：保存conditionType
                if (conditionNode.getConditionType() != null) {
                    propertiesMap.put("conditionType", conditionNode.getConditionType().toString());
                }
                // ⭐ 新增：保存容器大小
                propertiesMap.put("width", conditionNode.getContainerWidth());
                propertiesMap.put("height", conditionNode.getContainerHeight());
                
                // 保存子节点ID列表
                List<String> childNodeIds = new ArrayList<>();
                if (conditionNode.getManagedCanvasNodes() != null) {
                    for (ProcessNode child : conditionNode.getManagedCanvasNodes()) {
                        if (child.getNodeId() != null) {
                            childNodeIds.add(child.getNodeId());
                        }
                    }
                }
                // 也包含嵌套的条件节点
                if (conditionNode.getManagedConditionNodes() != null) {
                    for (com.cc.job.gui.model.ConditionNode childCondition : conditionNode.getManagedConditionNodes()) {
                        if (childCondition.getNodeId() != null) {
                            childNodeIds.add(childCondition.getNodeId());
                        }
                    }
                }
                propertiesMap.put("children", childNodeIds);
                nodeData.put("properties", apiUtil.getGson().toJson(propertiesMap));
                
                nodesData.add(nodeData);
                addedNodeIds.add(conditionNode.getNodeId());
            }
            
            // 构建连线数据
            List<Map<String, Object>> edgesData = new ArrayList<>();
            Set<String> addedEdgeKeys = new HashSet<>();
            
            for (NodeConnection conn : connections) {
                String sourceId = getNodeId(conn.getSourceOwner());
                String targetId = getNodeId(conn.getTargetOwner());
                
                if (sourceId == null || targetId == null) continue;
                
                String edgeKey = sourceId + "->" + targetId;
                if (addedEdgeKeys.contains(edgeKey)) continue;
                addedEdgeKeys.add(edgeKey);
                
                Map<String, Object> edgeData = new HashMap<>();
                edgeData.put("sourceNodeId", sourceId);
                edgeData.put("targetNodeId", targetId);
                edgeData.put("startPoint", "right");
                edgeData.put("endPoint", "left");
                edgesData.add(edgeData);
            }
            
            String nodesJson = apiUtil.getGson().toJson(nodesData);
            String edgesJson = apiUtil.getGson().toJson(edgesData);
            
            // 保存逻辑封装为 Runnable
            Runnable saveTask = () -> {
                try {
                    JobInfoForm formData = jobInfoService.getFormData(currentTaskGroupId);
                    if (formData == null) {
                        Platform.runLater(() -> logPanel.error("✗ 获取任务组数据失败"));
                        return;
                    }
                    
                    formData.setNodes(nodesJson);
                    formData.setEdges(edgesJson);
                    formData.setGlueType("BEAN");
                    formData.setExecutorHandler("runJobGroupXxlJob");
                    formData.setGlueUpdateTime(null);
                    
                    boolean success = jobInfoService.updateJobCompose(currentTaskGroupId, formData);
                    
                    Platform.runLater(() -> {
                        if (success) {
                            logPanel.success("✓ 保存成功！");
                            refreshTreeView();
                        } else {
                            logPanel.error("✗ 保存失败");
                        }
                        logPanel.info("════════════════════════════════");
                    });
                } catch (Exception e) {
                    logger.error("保存任务组失败", e);
                    Platform.runLater(() -> {
                        logPanel.error("✗ 保存失败: " + e.getMessage());
                        logPanel.info("════════════════════════════════");
                    });
                }
            };
            
            // 根据参数决定同步或异步执行
            if (async) {
                new Thread(saveTask).start();
            } else {
                saveTask.run();
            }
            
        } catch (Exception e) {
            logger.error("转换数据失败", e);
            logPanel.error("✗ 数据转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 刷新树形视图
     */
    public void refreshTreeView() {
        if (treeView != null) {
            treeView.refreshTreeData();
            logPanel.success("✓ 任务树刷新成功");
        }
    }
    
    private String getNodeId(Node owner) {
        if (owner instanceof ProcessNode) {
            return ((ProcessNode) owner).getNodeId();
        } else if (owner instanceof GroupContainer) {
            return ((GroupContainer) owner).getNodeId();
        } else if (owner instanceof com.cc.job.gui.model.ConditionNode) {
            return ((com.cc.job.gui.model.ConditionNode) owner).getNodeId();
        }
        return null;
    }
    
    private void setupEditCallbacks(JobComposeData composeData) {
        if (composeData == null || composeData.getNodes() == null) return;
        
        List<ProcessNode> loadedNodes = canvas.getNodes();
        if (loadedNodes == null || loadedNodes.isEmpty()) return;
        
        for (JobComposeData.NodeData nodeData : composeData.getNodes()) {
            String nodeId = nodeData.getId();
            Long jobId = nodeData.getJobId();
            
            if (nodeId == null || jobId == null) continue;
            
            loadedNodes.stream()
                .filter(node -> node.getNodeId() != null && node.getNodeId().equals(nodeId))
                .findFirst()
                .ifPresent(node -> node.setJobId(jobId));
        }
    }
}


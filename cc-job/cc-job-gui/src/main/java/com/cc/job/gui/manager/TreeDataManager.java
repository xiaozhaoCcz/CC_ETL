package com.cc.job.gui.manager;

import com.cc.job.gui.model.TreeNodeData;
import com.cc.job.xo.model.vo.JobPartVo;
import com.cc.job.gui.service.JobPartService;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;

import java.io.IOException;
import java.util.*;

/**
 * 树形数据管理器 - 负责树数据的加载和构建
 */
public class TreeDataManager {
    
    private final JobPartService jobPartService;
    private final TreeItem<TreeNodeData> rootItem;
    private final Map<Long, Boolean> expandedState;
    private volatile Long pendingSelectId;
    
    public TreeDataManager(TreeItem<TreeNodeData> rootItem, Map<Long, Boolean> expandedState) {
        this.jobPartService = new JobPartService();
        this.rootItem = rootItem;
        this.expandedState = expandedState;
    }
    
    /**
     * 加载树形数据
     */
    public void loadTreeData(Runnable onSuccess, Runnable onFallback) {
        new Thread(() -> {
            try {
                List<JobPartVo> treeData = jobPartService.getTree();
                Platform.runLater(() -> {
                    buildTreeFromData(treeData);
                    if (onSuccess != null) onSuccess.run();
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    if (onFallback != null) onFallback.run();
                });
            }
        }).start();
    }
    
    /**
     * 构建树形结构
     */
    public void buildTreeFromData(List<JobPartVo> data) {
        if (data == null || data.isEmpty()) return;
        
        Map<Long, Boolean> previousExpanded = new HashMap<>();
        captureExpandedState(rootItem, previousExpanded);
        expandedState.clear();
        expandedState.putAll(previousExpanded);
        
        rootItem.getChildren().clear();
        
        for (JobPartVo partVo : data) {
            // 显示所有分区，即使没有任务组（新建的分区可能还没有任务组）
            TreeItem<TreeNodeData> partitionItem = createPartitionItem(partVo);
            rootItem.getChildren().add(partitionItem);
        }
        
        handlePendingSelection();
        refreshExpandedStateCache();
    }
    
    private TreeItem<TreeNodeData> createPartitionItem(JobPartVo partVo) {
        TreeNodeData partitionData = new TreeNodeData(partVo.getId(), partVo.getLabel(), partVo.getType(), partVo.getExt1());
        TreeItem<TreeNodeData> partitionItem = new TreeItem<>(partitionData);
        applyExpandedState(partitionItem);
        
        if (partVo.getChildren() != null) {
            for (JobPartVo child : partVo.getChildren()) {
                if (child.getType() != null && child.getType() == 1) {
                    TreeItem<TreeNodeData> taskGroupItem = createTreeItemRecursive(child);
                    partitionItem.getChildren().add(taskGroupItem);
                }
            }
        }
        
        return partitionItem;
    }
    
    private TreeItem<TreeNodeData> createTreeItemRecursive(JobPartVo vo) {
        TreeNodeData nodeData = new TreeNodeData(vo.getId(), vo.getLabel(), vo.getType(), vo.getExt1());
        TreeItem<TreeNodeData> item = new TreeItem<>(nodeData);
        applyExpandedState(item);
        
        if (vo.getChildren() != null) {
            for (JobPartVo child : vo.getChildren()) {
                TreeItem<TreeNodeData> childItem = createTreeItemRecursive(child);
                item.getChildren().add(childItem);
            }
        }
        
        return item;
    }
    
    private void handlePendingSelection() {
        if (pendingSelectId != null) {
            TreeItem<TreeNodeData> target = findTreeItemById(rootItem, pendingSelectId);
            pendingSelectId = null;
            if (target != null) {
                // 选择由外部处理
            }
        }
    }
    
    public void setPendingSelectId(Long id) {
        this.pendingSelectId = id;
    }
    
    public Long getPendingSelectId() {
        return pendingSelectId;
    }
    
    /**
     * 查找指定ID的树节点
     */
    public TreeItem<TreeNodeData> findTreeItemById(TreeItem<TreeNodeData> item, Long targetId) {
        if (item == null || item.getValue() == null || targetId == null) return null;
        
        if (targetId.equals(item.getValue().getId())) {
            return item;
        }
        
        for (TreeItem<TreeNodeData> child : item.getChildren()) {
            TreeItem<TreeNodeData> found = findTreeItemById(child, targetId);
            if (found != null) return found;
        }
        
        return null;
    }
    
    /**
     * 根据jobId查找树节点
     */
    public TreeItem<TreeNodeData> findTreeItemByJobId(TreeItem<TreeNodeData> current, Long jobId) {
        if (current == null || jobId == null) return null;
        
        TreeNodeData value = current.getValue();
        if (value != null && value.getExt1() != null) {
            try {
                long stored = Long.parseLong(value.getExt1().trim());
                if (stored == jobId) return current;
            } catch (NumberFormatException ignored) {}
        }
        
        for (TreeItem<TreeNodeData> child : current.getChildren()) {
            TreeItem<TreeNodeData> found = findTreeItemByJobId(child, jobId);
            if (found != null) return found;
        }
        
        return null;
    }
    
    private void captureExpandedState(TreeItem<TreeNodeData> item, Map<Long, Boolean> snapshot) {
        if (item == null || snapshot == null) return;
        
        TreeNodeData data = item.getValue();
        if (data != null && data.getId() != null) {
            snapshot.put(data.getId(), item.isExpanded());
        }
        
        for (TreeItem<TreeNodeData> child : item.getChildren()) {
            captureExpandedState(child, snapshot);
        }
    }
    
    private void applyExpandedState(TreeItem<TreeNodeData> item) {
        if (item == null) return;
        
        TreeNodeData data = item.getValue();
        if (data == null) {
            item.setExpanded(false);
            return;
        }
        
        Long id = data.getId();
        Boolean stored = id != null ? expandedState.get(id) : null;
        
        if (stored != null) {
            item.setExpanded(stored);
        } else if (data.getType() != null && (data.getType() == 0 || data.getType() == 1)) {
            item.setExpanded(true);
        } else {
            item.setExpanded(false);
        }
    }
    
    private void refreshExpandedStateCache() {
        expandedState.clear();
        captureExpandedState(rootItem, expandedState);
    }
    
    /**
     * 执行删除操作
     */
    public void performDelete(Long nodeId, Integer nodeType, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        new Thread(() -> {
            boolean success = false;
            String errorMessage = null;
            
            try {
                if (nodeType != null && nodeType == 0) {
                    success = jobPartService.deleteJobPart(nodeId);
                } else if (nodeType != null && nodeType == 1) {
                    success = jobPartService.deleteJobInfo(nodeId);
                } else if (nodeType != null && nodeType == 4) {
                    success = jobPartService.deleteJobNode(nodeId);
                } else {
                    errorMessage = "暂不支持删除该类型的节点。";
                }
            } catch (IOException ex) {
                errorMessage = ex.getMessage();
            }
            
            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;
            
            Platform.runLater(() -> {
                if (finalSuccess) {
                    if (onSuccess != null) onSuccess.run();
                } else {
                    if (onError != null) {
                        onError.accept(finalErrorMessage != null ? finalErrorMessage : "删除失败");
                    }
                }
            });
        }).start();
    }
}


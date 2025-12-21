package com.cc.job.gui.manager;

import com.cc.job.gui.model.TreeNodeData;
import javafx.scene.control.TreeItem;

import java.util.*;

/**
 * 树搜索管理器 - 负责树的搜索和过滤功能
 */
public class TreeSearchManager {
    
    private final TreeItem<TreeNodeData> rootItem;
    private final Map<TreeItem<TreeNodeData>, List<TreeItem<TreeNodeData>>> originalChildren = new HashMap<>();
    
    public TreeSearchManager(TreeItem<TreeNodeData> rootItem) {
        this.rootItem = rootItem;
    }
    
    /**
     * 搜索过滤
     */
    public void filterTree(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            restoreAllNodes();
            return;
        }
        
        String lowerSearchText = searchText.toLowerCase().trim();
        
        if (originalChildren.isEmpty()) {
            saveOriginalStructure(rootItem);
        }
        
        filterNodeRecursive(rootItem, lowerSearchText);
    }
    
    /**
     * 保存原始树结构
     */
    private void saveOriginalStructure(TreeItem<TreeNodeData> node) {
        if (node == null) return;
        
        originalChildren.put(node, new ArrayList<>(node.getChildren()));
        
        for (TreeItem<TreeNodeData> child : node.getChildren()) {
            saveOriginalStructure(child);
        }
    }
    
    /**
     * 递归过滤节点
     */
    private boolean filterNodeRecursive(TreeItem<TreeNodeData> node, String searchText) {
        if (node == null) return false;
        
        if (node == rootItem) {
            List<TreeItem<TreeNodeData>> originalPartitions = originalChildren.get(rootItem);
            if (originalPartitions != null) {
                rootItem.getChildren().clear();
                
                for (TreeItem<TreeNodeData> partition : originalPartitions) {
                    if (filterNodeRecursive(partition, searchText)) {
                        if (!rootItem.getChildren().contains(partition)) {
                            rootItem.getChildren().add(partition);
                        }
                    }
                }
            }
            return true;
        }
        
        String nodeName = node.getValue().getLabel().toLowerCase();
        boolean nameMatches = nodeName.contains(searchText);
        
        boolean hasMatchingChild = false;
        List<TreeItem<TreeNodeData>> originalChildList = originalChildren.get(node);
        
        if (originalChildList != null && !originalChildList.isEmpty()) {
            node.getChildren().clear();
            
            for (TreeItem<TreeNodeData> child : originalChildList) {
                String childName = child.getValue().getLabel().toLowerCase();
                boolean childMatches = childName.contains(searchText);
                
                if (childMatches) {
                    if (!node.getChildren().contains(child)) {
                        node.getChildren().add(child);
                    }
                    hasMatchingChild = true;
                }
            }
        }
        
        boolean shouldShow = nameMatches || hasMatchingChild;
        
        if (shouldShow && hasMatchingChild) {
            node.setExpanded(true);
        }
        
        return shouldShow;
    }
    
    /**
     * 恢复所有节点
     */
    private void restoreAllNodes() {
        if (originalChildren.isEmpty()) return;
        
        restoreNode(rootItem);
        originalChildren.clear();
        
        for (TreeItem<TreeNodeData> partition : rootItem.getChildren()) {
            if (partition.getValue() != null && partition.getValue().getType() == 0) {
                partition.setExpanded(rootItem.getChildren().indexOf(partition) == 0);
            }
        }
    }
    
    /**
     * 递归恢复节点
     */
    private void restoreNode(TreeItem<TreeNodeData> node) {
        if (node == null) return;
        
        List<TreeItem<TreeNodeData>> originalChildList = originalChildren.get(node);
        if (originalChildList != null) {
            node.getChildren().clear();
            node.getChildren().addAll(originalChildList);
            
            for (TreeItem<TreeNodeData> child : originalChildList) {
                restoreNode(child);
            }
        }
    }
    
    public void clearOriginalChildren() {
        originalChildren.clear();
    }
}


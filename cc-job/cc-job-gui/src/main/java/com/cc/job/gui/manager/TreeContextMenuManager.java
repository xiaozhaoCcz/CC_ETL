package com.cc.job.gui.manager;

import com.cc.job.gui.model.TreeNodeData;
import com.cc.job.gui.view.TaskTreeView;
import javafx.scene.control.*;
import java.util.Optional;

/**
 * 树右键菜单管理器 - 负责创建和处理树节点的右键菜单
 */
public class TreeContextMenuManager {
    
    private final TaskTreeView.TaskSelectionCallback selectionCallback;
    
    public TreeContextMenuManager(TaskTreeView.TaskSelectionCallback selectionCallback) {
        this.selectionCallback = selectionCallback;
    }
    
    /**
     * 创建树节点的右键菜单
     */
    public ContextMenu createContextMenu(TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem,
                                        Runnable onRefresh, Runnable onDelete) {
        ContextMenu menu = new ContextMenu();
        Integer nodeType = nodeData.getType();
        
        if (nodeType == null) {
            return createDefaultMenu(onRefresh);
        }
        
        switch (nodeType) {
            case 0 -> createPartitionMenu(menu, nodeData, onRefresh, onDelete);
            case 1 -> createTaskGroupMenu(menu, nodeData, onRefresh, onDelete);
            case 4 -> createJobNodeMenu(menu, nodeData, treeItem, onRefresh, onDelete);
            case 5 -> createEdgeMenu(menu, nodeData, treeItem, onDelete);
            default -> createDefaultMenu(menu, onRefresh);
        }
        
        return menu;
    }
    
    private void createPartitionMenu(ContextMenu menu, TreeNodeData nodeData, Runnable onRefresh, Runnable onDelete) {
        MenuItem newTaskItem = new MenuItem("新建任务组");
        newTaskItem.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2563EB;");
        newTaskItem.setOnAction(e -> {
            if (selectionCallback != null) {
                selectionCallback.onNewJobGroup(nodeData.getId(), nodeData.getLabel());
            }
        });
        MenuItem importTaskGroupItem = new MenuItem("导入任务组");
        importTaskGroupItem.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2563EB;");
        importTaskGroupItem.setOnAction(e -> {
            if (selectionCallback != null) {
                selectionCallback.onPartitionAction(nodeData.getId(), nodeData.getLabel(),
                    TaskTreeView.TaskSelectionCallback.PartitionAction.IMPORT);
            }
        });
        
        MenuItem refreshItem = createMenuItem("刷新", onRefresh);
        MenuItem editItem = createMenuItem("编辑", () -> {
            if (selectionCallback != null) {
                selectionCallback.onPartitionAction(nodeData.getId(), nodeData.getLabel(), 
                    TaskTreeView.TaskSelectionCallback.PartitionAction.EDIT);
            }
        });
        MenuItem exportItem = createMenuItem("导出", () -> {
            if (selectionCallback != null) {
                selectionCallback.onPartitionAction(nodeData.getId(), nodeData.getLabel(), 
                    TaskTreeView.TaskSelectionCallback.PartitionAction.EXPORT);
            }
        });
        
        menu.getItems().addAll(newTaskItem, importTaskGroupItem, refreshItem, editItem, exportItem);
        
        if (supportsDeletion(nodeData)) {
            menu.getItems().addAll(new SeparatorMenuItem(), createDeleteMenuItem(onDelete));
        }
    }
    
    private void createTaskGroupMenu(ContextMenu menu, TreeNodeData nodeData, Runnable onRefresh, Runnable onDelete) {
        MenuItem addNodeItem = new MenuItem("新增节点");
        addNodeItem.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #10B981;");
        addNodeItem.setOnAction(e -> {
            if (selectionCallback != null) {
                selectionCallback.onNewJobNode(nodeData.getId(), nodeData.getLabel());
            }
        });
        
        MenuItem refreshItem = createMenuItem("刷新", onRefresh);
        MenuItem editItem = createMenuItem("编辑", () -> {
            if (selectionCallback != null) {
                selectionCallback.onJobGroupEdit(nodeData.getId(), nodeData.getLabel());
            }
        });
        MenuItem exportItem = createMenuItem("导出", () -> {
            if (selectionCallback != null) {
                selectionCallback.onExportTaskGroup(nodeData.getId(), nodeData.getLabel());
            }
        });
        
        menu.getItems().addAll(addNodeItem, refreshItem, editItem, exportItem);
        
        if (supportsDeletion(nodeData)) {
            menu.getItems().addAll(new SeparatorMenuItem(), createDeleteMenuItem(onDelete));
        }
    }
    
    private void createJobNodeMenu(ContextMenu menu, TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem,
                                  Runnable onRefresh, Runnable onDelete) {
        MenuItem refreshItem = createMenuItem("刷新", onRefresh);
        
        MenuItem propertiesItem = createMenuItem("属性", () -> {
            if (selectionCallback != null) {
                Long taskGroupId = findTaskGroupIdFromTreeItem(treeItem);
                selectionCallback.onJobNodeAction(nodeData.getId(), parseJobId(nodeData), 
                    nodeData.getLabel(), taskGroupId, TaskTreeView.TaskSelectionCallback.JobNodeAction.PROPERTIES);
            }
        });
        
        MenuItem editItem = createMenuItem("编辑", () -> {
            if (selectionCallback != null) {
                Long taskGroupId = findTaskGroupIdFromTreeItem(treeItem);
                selectionCallback.onJobNodeAction(nodeData.getId(), parseJobId(nodeData), 
                    nodeData.getLabel(), taskGroupId, TaskTreeView.TaskSelectionCallback.JobNodeAction.EDIT);
            }
        });
        
        MenuItem locateItem = createMenuItem("定位", () -> {
            if (selectionCallback != null) {
                Long taskGroupId = findTaskGroupIdFromTreeItem(treeItem);
                selectionCallback.onJobNodeAction(nodeData.getId(), parseJobId(nodeData), 
                    nodeData.getLabel(), taskGroupId, TaskTreeView.TaskSelectionCallback.JobNodeAction.LOCATE);
            }
        });
        
        menu.getItems().addAll(refreshItem, new SeparatorMenuItem(), propertiesItem, editItem, locateItem, 
            new SeparatorMenuItem(), createDeleteMenuItem(onDelete));
    }
    
    private void createEdgeMenu(ContextMenu menu, TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem, Runnable onDelete) {
        MenuItem locateEdgeItem = createMenuItem("定位连接", () -> {
            if (selectionCallback != null) {
                Long taskGroupId = findTaskGroupIdFromTreeItem(treeItem);
                selectionCallback.onEdgeAction(nodeData.getId(), taskGroupId, TaskTreeView.TaskSelectionCallback.EdgeAction.LOCATE);
            }
        });
        
        MenuItem deleteEdgeItem = new MenuItem("删除连接");
        deleteEdgeItem.setStyle("-fx-text-fill: #EF4444;");
        deleteEdgeItem.setOnAction(e -> {
            if (onDelete != null) onDelete.run();
        });
        
        menu.getItems().addAll(locateEdgeItem, new SeparatorMenuItem(), deleteEdgeItem);
    }
    
    private void createDefaultMenu(ContextMenu menu, Runnable onRefresh) {
        menu.getItems().add(createMenuItem("刷新", onRefresh));
    }
    
    private ContextMenu createDefaultMenu(Runnable onRefresh) {
        ContextMenu menu = new ContextMenu();
        menu.getItems().add(createMenuItem("刷新", onRefresh));
        return menu;
    }
    
    private MenuItem createMenuItem(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setStyle("-fx-text-fill: #000000;");
        item.setOnAction(e -> { if (action != null) action.run(); });
        return item;
    }
    
    private MenuItem createDeleteMenuItem(Runnable onDelete) {
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setStyle("-fx-text-fill: #EF4444;");
        deleteItem.setOnAction(e -> { if (onDelete != null) onDelete.run(); });
        return deleteItem;
    }
    
    private boolean supportsDeletion(TreeNodeData nodeData) {
        if (nodeData == null || nodeData.getType() == null) return false;
        return nodeData.getType() == 0 || nodeData.getType() == 1 || nodeData.getType() == 4;
    }
    
    private Long parseJobId(TreeNodeData nodeData) {
        if (nodeData == null || nodeData.getExt1() == null || nodeData.getExt1().isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(nodeData.getExt1().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    private Long findTaskGroupIdFromTreeItem(TreeItem<TreeNodeData> treeItem) {
        if (treeItem == null) return null;
        
        TreeItem<TreeNodeData> current = treeItem;
        while (current != null) {
            TreeNodeData nodeData = current.getValue();
            if (nodeData != null && nodeData.getType() != null && nodeData.getType() == 1) {
                return nodeData.getId();
            }
            current = current.getParent();
        }
        
        return null;
    }
    
    /**
     * 显示删除确认对话框
     */
    public boolean confirmDelete(String nodeName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除节点");
        alert.setHeaderText("确认删除 \"" + nodeName + "\" ?");
        alert.setContentText("删除后，该节点及其所有子节点都将被移除，且不可恢复。");
        
        ButtonType confirmButton = new ButtonType("确认删除", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmButton, cancelButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirmButton;
    }
    
    /**
     * 显示删除边确认对话框
     */
    public boolean confirmDeleteEdge(String edgeName, Runnable onConfirm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除连接");
        alert.setHeaderText("删除连接: " + edgeName);
        alert.setContentText("确定要删除该连接吗？删除后需重新保存任务组以生效。");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (onConfirm != null) onConfirm.run();
            return true;
        }
        return false;
    }
}


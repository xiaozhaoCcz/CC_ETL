package com.cc.job.gui.view;

import com.cc.job.gui.manager.*;
import com.cc.job.gui.model.TreeNodeData;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.StyleUtil;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.*;

/**
 * 任务组树形视图组件（简化版）
 */
public class TaskTreeView extends VBox {
    
    private TextField searchField;
    private TreeView<TreeNodeData> treeView;
    private TreeItem<TreeNodeData> rootItem;
    
    private TaskSelectionCallback selectionCallback;
    private Runnable onClose;
    private Runnable onDetach;
    private Button detachBtn;
    private Button closeBtn;
    
    private TreeDataManager dataManager;
    private TreeSearchManager searchManager;
    private TreeContextMenuManager contextMenuManager;
    
    private Map<Long, Boolean> runningTaskGroups = new HashMap<>();
    private final Map<Long, Boolean> expandedState = new HashMap<>();
    
    public interface TaskSelectionCallback {
        void onTaskSelected(String taskName);
        void onTaskSelected(Long taskId, String taskName, Integer type);
        void onNewJobGroup(Long partitionId, String partitionName);
        void onNewJobNode(Long taskGroupId, String taskGroupName);
        
        default void onSuppressTreeSelectionOnce() {}
        default void onJobNodeAction(Long jobNodeId, Long jobId, String nodeName, JobNodeAction action) {}
        default void onJobNodeAction(Long jobNodeId, Long jobId, String nodeName, Long taskGroupId, JobNodeAction action) {
            onJobNodeAction(jobNodeId, jobId, nodeName, action);
        }
        default void onEdgeAction(Long edgeId, EdgeAction action) {}
        default void onEdgeAction(Long edgeId, Long taskGroupId, EdgeAction action) {
            onEdgeAction(edgeId, action);
        }
        default void onPartitionAction(Long partitionId, String partitionName, PartitionAction action) {}
        default void onJobGroupEdit(Long taskGroupId, String taskGroupName) {}
        default void onExportTaskGroup(Long taskGroupId, String taskGroupName) {}
        
        enum JobNodeAction { EDIT, LOCATE, PROPERTIES }
        enum EdgeAction { LOCATE, DELETE }
        enum PartitionAction { EDIT, EXPORT, IMPORT }
    }
    
    public TaskTreeView() {
        initializeUI();
        initializeManagers();
        loadTreeData();
    }
    
    private void initializeUI() {
        getStyleClass().add("tree-view-panel");
        setStyle("-fx-padding: 0 0 0 8;");
        setMinWidth(240);
        setSpacing(10);
        
        HBox titleBar = createTitleBar();
        HBox searchBar = createSearchBar();
        treeView = createTreeView();
        VBox.setVgrow(treeView, Priority.ALWAYS);
        
        getChildren().addAll(titleBar, searchBar, treeView);
    }
    
    private void initializeManagers() {
        dataManager = new TreeDataManager(rootItem, expandedState);
        searchManager = new TreeSearchManager(rootItem);
        contextMenuManager = new TreeContextMenuManager(selectionCallback);
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox(8);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("任务组");
        titleLabel.setStyle(StyleUtil.subtitle());
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        detachBtn = new Button("", IconUtil.windowIcon());
        StyleUtil.applyIconButtonHover(detachBtn);
        detachBtn.setTooltip(new Tooltip("弹出为独立窗口"));
        detachBtn.setOnAction(e -> { if (onDetach != null) onDetach.run(); });
        
        closeBtn = new Button("", IconUtil.closeIcon());
        StyleUtil.applyIconButtonHover(closeBtn);
        closeBtn.setTooltip(new Tooltip("关闭面板"));
        closeBtn.setOnAction(e -> { if (onClose != null) onClose.run(); });
        
        titleBar.getChildren().addAll(titleLabel, detachBtn, closeBtn);
        return titleBar;
    }
    
    private HBox createSearchBar() {
        HBox searchWrapper = new HBox();
        searchWrapper.setAlignment(Pos.CENTER_LEFT);
        searchWrapper.setPadding(new Insets(0, 12, 0, 0));
        
        searchField = new TextField();
        searchField.setPromptText("搜索节点...");
        searchField.setPrefHeight(28);
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.getStyleClass().add("tree-search-field");
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> searchManager.filterTree(newVal));
        
        searchWrapper.getChildren().add(searchField);
        return searchWrapper;
    }
    
    private TreeView<TreeNodeData> createTreeView() {
        rootItem = new TreeItem<>(new TreeNodeData(0L, "所有任务组", -1));
        rootItem.setExpanded(true);
        
        TreeView<TreeNodeData> tree = new TreeView<>(rootItem);
        tree.setShowRoot(false);
        tree.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        tree.setCellFactory(tv -> new CustomTreeCell());
        
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                handleNodeSelection(newVal);
            }
        });
        
        tree.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.DELETE || 
                event.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                handleDeleteKey(tree.getSelectionModel().getSelectedItem());
                event.consume();
            }
        });
        
        return tree;
    }
    
    private void handleNodeSelection(TreeItem<TreeNodeData> newVal) {
        TreeNodeData nodeData = newVal.getValue();
        if (selectionCallback == null) return;
        
        Integer nodeType = nodeData.getType();
        if (nodeType != null && (nodeType == 2 || nodeType == 3)) {
            TreeItem<TreeNodeData> current = newVal;
            while (current != null && current.getValue() != null &&
                   (current.getValue().getType() == null || current.getValue().getType() != 1)) {
                current = current.getParent();
            }
            if (current != null && current.getValue() != null && current.getValue().getType() == 1) {
                TreeNodeData groupData = current.getValue();
                try {
                    selectionCallback.onSuppressTreeSelectionOnce();
                } catch (Throwable ignore) {}
                selectionCallback.onTaskSelected(groupData.getId(), groupData.getLabel(), 1);
                return;
            }
        }
        
        selectionCallback.onTaskSelected(nodeData.getId(), nodeData.getLabel(), nodeData.getType());
    }
    
    private void handleDeleteKey(TreeItem<TreeNodeData> selected) {
        if (selected == null || selected.getValue() == null) return;
        
        Integer type = selected.getValue().getType();
        if (type != null && type == 5) {
            confirmDeleteEdge(selected.getValue(), selected);
        } else if (type != null && type == 4) {
            confirmDeleteNode(selected.getValue(), selected);
        }
    }
    
    /**
     * 自定义树单元格
     */
    private class CustomTreeCell extends TreeCell<TreeNodeData> {
        private HBox contentBox;
        private StackPane iconContainer;
        private Label textLabel;
        private Circle runningIndicator;
        private FadeTransition blinkAnimation;
        
        @Override
        protected void updateItem(TreeNodeData item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
                setContextMenu(null);
                //stopBlinking();
            } else {
                if (contentBox == null) {
                    contentBox = new HBox(8);
                    contentBox.setAlignment(Pos.CENTER_LEFT);
                    
                    iconContainer = new StackPane();
                    iconContainer.setMinSize(16, 16);
                    iconContainer.setMaxSize(16, 16);
                    
                    textLabel = new Label();
                    
//                    runningIndicator = new Circle(4);
//                    runningIndicator.setFill(Color.web("#10B981"));
//                    runningIndicator.setVisible(false);
//                    runningIndicator.setManaged(false);  // 初始状态不占用布局空间
                    
                    contentBox.getChildren().addAll(iconContainer, textLabel);
                }
                
                textLabel.setText(item.getLabel());
                iconContainer.getChildren().clear();
                iconContainer.getChildren().add(IconUtil.getIconByType(item.getType()));
                
                // TODO 显示只有任务组类型（type == 1）且正在运行时才显示绿色圆点
//                boolean isRunning = item.getType() != null && item.getType() == 1 &&
//                    runningTaskGroups.containsKey(item.getId()) && runningTaskGroups.get(item.getId());
//
//                runningIndicator.setVisible(isRunning);
//                runningIndicator.setManaged(isRunning);
//                if (isRunning) {
//                    startBlinking();
//                } else {
//                    stopBlinking();
//                }
                
                setText(null);
                setGraphic(contentBox);
                
                setStyle(StyleUtil.body() + "-fx-padding: 8 12; -fx-background-radius: " + StyleUtil.RADIUS_MD + ";");
                
                if (isSelected()) {
                    setStyle("-fx-background-color: " + StyleUtil.PRIMARY + "20; -fx-text-fill: " + StyleUtil.PRIMARY + "; " +
                        "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; -fx-background-radius: " + StyleUtil.RADIUS_MD + ";");
                    textLabel.setStyle("-fx-text-fill: " + StyleUtil.PRIMARY + ";");
                } else {
                    textLabel.setStyle("-fx-text-fill: #374151;");
                }
                
                setContextMenu(contextMenuManager.createContextMenu(item, getTreeItem(),
                    () -> handleRefresh(item, getTreeItem()),
                    () -> confirmDeleteNode(item, getTreeItem())));
            }
        }
        
        private void startBlinking() {
            if (runningIndicator == null || !runningIndicator.isVisible()) return;
            
            if (blinkAnimation == null) {
                blinkAnimation = new FadeTransition(Duration.millis(800), runningIndicator);
                blinkAnimation.setFromValue(1.0);
                blinkAnimation.setToValue(0.2);
                blinkAnimation.setCycleCount(FadeTransition.INDEFINITE);
                blinkAnimation.setAutoReverse(true);
            }
            
            if (blinkAnimation.getStatus() != FadeTransition.Status.RUNNING) {
                blinkAnimation.play();
            }
        }
        
        private void stopBlinking() {
            if (blinkAnimation != null && blinkAnimation.getStatus() == FadeTransition.Status.RUNNING) {
                blinkAnimation.stop();
                if (runningIndicator != null) {
                    runningIndicator.setOpacity(1.0);
                }
            }
        }
    }
    
    // ==================== 数据加载 ====================
    
    private void loadTreeData() {
        dataManager.loadTreeData(null, this::loadSampleData);
    }
    
    public void refreshTreeData() {
        searchManager.clearOriginalChildren();
        loadTreeData();
    }
    
    @SuppressWarnings("unchecked")
    private void loadSampleData() {
        rootItem.getChildren().clear();
        
        TreeItem<TreeNodeData> partition1 = new TreeItem<>(new TreeNodeData(1L, "分区1", 0));
        partition1.setExpanded(true);
        partition1.getChildren().addAll(
            new TreeItem<>(new TreeNodeData(101L, "测试任务组1", 1)),
            new TreeItem<>(new TreeNodeData(102L, "测试任务组2", 1)),
            new TreeItem<>(new TreeNodeData(103L, "数据处理任务", 1))
        );
        
        TreeItem<TreeNodeData> partition2 = new TreeItem<>(new TreeNodeData(2L, "分区2", 0));
        partition2.setExpanded(false);
        partition2.getChildren().addAll(
            new TreeItem<>(new TreeNodeData(201L, "定时任务组", 1)),
            new TreeItem<>(new TreeNodeData(202L, "批量任务组", 1))
        );
        
        rootItem.getChildren().addAll(partition1, partition2);
        treeView.getSelectionModel().select(partition1.getChildren().get(0));
    }
    
    // ==================== 节点操作 ====================
    
    private void handleRefresh(TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem) {
        if (nodeData == null) {
            dataManager.setPendingSelectId(null);
            refreshTreeData();
            return;
        }
        
        if (!supportsDeletion(nodeData)) {
            dataManager.setPendingSelectId(nodeData.getId());
            refreshTreeData();
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("刷新节点");
        alert.setHeaderText("刷新 \"" + nodeData.getLabel() + "\"");
        alert.setContentText("是否同时删除当前节点？");
        
        ButtonType deleteButton = new ButtonType("删除节点", ButtonBar.ButtonData.OK_DONE);
        ButtonType refreshOnlyButton = new ButtonType("仅刷新", ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(deleteButton, refreshOnlyButton, cancelButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancelButton) return;
        
        if (result.get() == deleteButton) {
            performDelete(nodeData, treeItem);
        } else {
            dataManager.setPendingSelectId(nodeData.getId());
            refreshTreeData();
        }
    }
    
    private void confirmDeleteNode(TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem) {
        if (!supportsDeletion(nodeData)) {
            showAlert(Alert.AlertType.INFORMATION, "删除节点", "暂不支持删除此类型的节点。");
            return;
        }
        
        if (contextMenuManager.confirmDelete(nodeData.getLabel())) {
            performDelete(nodeData, treeItem);
        }
    }
    
    private void performDelete(TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem) {
        if (nodeData == null || nodeData.getId() == null) return;
        
        dataManager.performDelete(nodeData.getId(), nodeData.getType(),
            () -> {
                Long focusId = null;
                if (treeItem != null && treeItem.getParent() != null && treeItem.getParent().getValue() != null) {
                    focusId = treeItem.getParent().getValue().getId();
                }
                dataManager.setPendingSelectId(focusId);
                refreshTreeData();
                showAlert(Alert.AlertType.INFORMATION, "删除成功", "节点 \"" + nodeData.getLabel() + "\" 已删除。");
            },
            errorMsg -> showAlert(Alert.AlertType.ERROR, "删除失败", errorMsg)
        );
    }
    
    private void confirmDeleteEdge(TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem) {
        contextMenuManager.confirmDeleteEdge(nodeData.getLabel(), () -> {
            if (selectionCallback != null) {
                selectionCallback.onEdgeAction(nodeData.getId(), TaskSelectionCallback.EdgeAction.DELETE);
            }
            if (treeItem.getParent() != null) {
                treeItem.getParent().getChildren().remove(treeItem);
            }
        });
    }
    
    private boolean supportsDeletion(TreeNodeData nodeData) {
        if (nodeData == null || nodeData.getType() == null) return false;
        return nodeData.getType() == 0 || nodeData.getType() == 1 || nodeData.getType() == 4;
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // ==================== 选择和定位 ====================
    
    public void setSelectionCallback(TaskSelectionCallback callback) {
        this.selectionCallback = callback;
        if (contextMenuManager != null) {
            contextMenuManager = new TreeContextMenuManager(callback);
        }
    }
    
    public void selectTaskGroupById(Long taskGroupId) {
        if (taskGroupId == null) return;
        
        TreeItem<TreeNodeData> foundItem = dataManager.findTreeItemById(rootItem, taskGroupId);
        if (foundItem != null) {
            treeView.getSelectionModel().select(foundItem);
            int row = treeView.getRow(foundItem);
            if (row >= 0) treeView.scrollTo(row);
        }
    }
    
    public void selectTaskGroupByName(String taskGroupName) {
        if (taskGroupName == null || taskGroupName.isEmpty()) return;
        
        Platform.runLater(() -> {
            TreeItem<TreeNodeData> foundItem = findTreeItemByName(rootItem, taskGroupName);
            if (foundItem != null) {
                treeView.getSelectionModel().select(foundItem);
                TreeItem<TreeNodeData> parent = foundItem.getParent();
                while (parent != null && parent != rootItem) {
                    if (!parent.isExpanded()) parent.setExpanded(true);
                    parent = parent.getParent();
                }
                int row = treeView.getRow(foundItem);
                if (row >= 0) treeView.scrollTo(row);
            }
        });
    }
    
    public void clearSelection() {
        Platform.runLater(() -> treeView.getSelectionModel().clearSelection());
    }
    
    public Long findTaskGroupIdByName(String taskGroupName) {
        if (taskGroupName == null || taskGroupName.isEmpty() || rootItem == null) return null;
        return findTaskGroupIdRecursive(rootItem, taskGroupName);
    }
    
    private Long findTaskGroupIdRecursive(TreeItem<TreeNodeData> item, String taskGroupName) {
        if (item == null || item.getValue() == null) return null;
        
        TreeNodeData nodeData = item.getValue();
        if (nodeData.getType() != null && nodeData.getType() == 1 && 
            taskGroupName.equals(nodeData.getLabel())) {
            return nodeData.getId();
        }
        
        for (TreeItem<TreeNodeData> child : item.getChildren()) {
            Long foundId = findTaskGroupIdRecursive(child, taskGroupName);
            if (foundId != null) return foundId;
        }
        
        return null;
    }
    
    private TreeItem<TreeNodeData> findTreeItemByName(TreeItem<TreeNodeData> item, String taskGroupName) {
        if (item == null || item.getValue() == null) return null;
        
        TreeNodeData nodeData = item.getValue();
        if (nodeData.getType() != null && nodeData.getType() == 1 && 
            taskGroupName.equals(nodeData.getLabel())) {
            return item;
        }
        
        for (TreeItem<TreeNodeData> child : item.getChildren()) {
            TreeItem<TreeNodeData> found = findTreeItemByName(child, taskGroupName);
            if (found != null) return found;
        }
        
        return null;
    }
    
    public TreeItem<TreeNodeData> findTreeItemById(Long targetId) {
        if (targetId == null || rootItem == null) return null;
        return dataManager.findTreeItemById(rootItem, targetId);
    }
    
    // ==================== 运行状态管理 ====================
    
    public void updateTaskGroupRunningStatus(Long taskGroupId, boolean isRunning) {
        Platform.runLater(() -> {
            if (isRunning) {
                runningTaskGroups.put(taskGroupId, true);
            } else {
                runningTaskGroups.remove(taskGroupId);
            }
            treeView.refresh();
        });
    }
    
    public void updateRunningTaskGroups(Map<Long, Boolean> runningMap) {
        Platform.runLater(() -> {
            this.runningTaskGroups.clear();
            if (runningMap != null) {
                this.runningTaskGroups.putAll(runningMap);
            }
            treeView.refresh();
        });
    }
    
    public void clearAllRunningStatus() {
        Platform.runLater(() -> {
            runningTaskGroups.clear();
            treeView.refresh();
        });
    }
    
    // ==================== 节点更新 ====================
    
    public void updateJobNode(Long jobId, String newLabel) {
        if (jobId == null) return;
        
        TreeItem<TreeNodeData> target = dataManager.findTreeItemByJobId(rootItem, jobId);
        if (target != null) {
            TreeNodeData data = target.getValue();
            if (data != null) {
                if (newLabel != null && !newLabel.isBlank()) {
                    data.setLabel(newLabel);
                }
                data.setExt1(String.valueOf(jobId));
                treeView.refresh();
            }
        }
    }
    
    // ==================== Setters ====================
    
    public void setOnClose(Runnable callback) {
        this.onClose = callback;
    }
    
    public void setOnDetach(Runnable callback) {
        this.onDetach = callback;
    }
    
    /**
     * 设置弹出和关闭按钮的可见性（用于面板弹出为独立窗口时隐藏按钮）
     */
    public void setDetachButtonsVisible(boolean visible) {
        if (detachBtn != null) {
            detachBtn.setVisible(visible);
            detachBtn.setManaged(visible);
        }
        if (closeBtn != null) {
            closeBtn.setVisible(visible);
            closeBtn.setManaged(visible);
        }
    }
    
    public String getSelectedTask() {
        TreeItem<TreeNodeData> selected = treeView.getSelectionModel().getSelectedItem();
        return selected != null && selected.getValue() != null ? selected.getValue().getLabel() : null;
    }

    /**
     * 获取当前树选中的节点数据（分区 type=0，任务组 type=1 等）
     */
    public TreeNodeData getSelectedTreeNodeData() {
        TreeItem<TreeNodeData> selected = treeView.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getValue() : null;
    }
}

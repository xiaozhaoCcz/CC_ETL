package com.example.nodefx.view;

import com.cc.job.xo.model.vo.JobPartVo;
import com.example.nodefx.model.TreeNodeData;
import com.example.nodefx.service.JobPartService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

/**
 * 任务组树形视图组件
 */
public class TaskTreeView extends VBox {
    
    private TextField searchField;
    private TreeView<TreeNodeData> treeView;
    private TreeItem<TreeNodeData> rootItem;
    
    // 选择回调
    private TaskSelectionCallback selectionCallback;
    
    // 关闭回调
    private Runnable onClose;
    
    // 弹出回调
    private Runnable onDetach;
    
    // API 服务
    private final JobPartService jobPartService;
    
    public interface TaskSelectionCallback {
        void onTaskSelected(String taskName);
        void onTaskSelected(Long taskId, String taskName, Integer type);
    }
    
    public TaskTreeView() {
        this.jobPartService = new JobPartService();
        initializeUI();
        loadTreeData();
    }
    
    private void initializeUI() {
        // 设置面板样式
        setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-width: 0 1 0 0;");
        setMinWidth(200);  // 最小宽度200px
        setPadding(new Insets(10));
        setSpacing(10);
        
        // 标题栏
        HBox titleBar = createTitleBar();
        
        // 搜索框
        HBox searchBar = createSearchBar();
        
        // 树形视图
        treeView = createTreeView();
        VBox.setVgrow(treeView, Priority.ALWAYS);
        
        // 添加所有组件
        getChildren().addAll(titleBar, searchBar, treeView);
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox(10);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("📁 任务组");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        // 弹出按钮
        Button detachBtn = new Button("🪟");
        detachBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #6B7280; " +
            "-fx-font-size: 14; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 2 6 2 6;"
        );
        detachBtn.setTooltip(new Tooltip("弹出为独立窗口"));
        detachBtn.setOnMouseEntered(e -> detachBtn.setStyle(
            "-fx-background-color: #EEF2FF; " +
            "-fx-text-fill: #8B5CF6; " +
            "-fx-font-size: 14; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 3;"
        ));
        detachBtn.setOnMouseExited(e -> detachBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #6B7280; " +
            "-fx-font-size: 14; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 2 6 2 6;"
        ));
        detachBtn.setOnAction(e -> {
            if (onDetach != null) {
                onDetach.run();
            }
        });
        
        // 关闭按钮
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #6B7280; " +
            "-fx-font-size: 14; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 2 6 2 6;"
        );
        closeBtn.setTooltip(new Tooltip("关闭面板"));
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
            "-fx-background-color: #F3F4F6; " +
            "-fx-text-fill: #1F2937; " +
            "-fx-font-size: 14; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 3;"
        ));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #6B7280; " +
            "-fx-font-size: 14; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 2 6 2 6;"
        ));
        closeBtn.setOnAction(e -> {
            if (onClose != null) {
                onClose.run();
            }
        });
        
        titleBar.getChildren().addAll(titleLabel, detachBtn, closeBtn);
        
        return titleBar;
    }
    
    private HBox createSearchBar() {
        HBox searchBar = new HBox(5);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setStyle(
            "-fx-background-color: #F9FAFB; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8;"
        );
        
        searchField = new TextField();
        searchField.setPromptText("🔍 搜索节点");
        searchField.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-color: transparent; " +
            "-fx-text-fill: #1F2937; " +
            "-fx-prompt-text-fill: #9CA3AF;"
        );
        searchField.setPrefWidth(240);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        // 搜索功能
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterTree(newVal);
        });
        
        searchBar.getChildren().add(searchField);
        
        return searchBar;
    }
    
    private TreeView<TreeNodeData> createTreeView() {
        // 创建根节点
        rootItem = new TreeItem<>(new TreeNodeData(0L, "所有任务组", -1));
        rootItem.setExpanded(true);
        
        // 创建树形视图
        TreeView<TreeNodeData> tree = new TreeView<>(rootItem);
        tree.setShowRoot(false);
        tree.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: transparent;"
        );
        
        // 自定义单元格渲染
        tree.setCellFactory(tv -> new TreeCell<TreeNodeData>() {
            
            private ContextMenu contextMenu;
            
            @Override
            protected void updateItem(TreeNodeData item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    setContextMenu(null);
                } else {
                    setText(item.getLabel());
                    
                    // 根据节点类型设置图标
                    String icon = item.getTypeIcon();
                    String color = item.getTypeColor();
                    setGraphic(createIcon(icon, color));
                    
                    // 样式
                    setStyle(
                        "-fx-text-fill: #1F2937; " +
                        "-fx-font-size: 13; " +
                        "-fx-padding: 6 8 6 8;"
                    );
                    
                    // 选中样式
                    if (isSelected()) {
                        setStyle(
                            "-fx-background-color: #EDE9FE; " +
                            "-fx-text-fill: #7C3AED; " +
                            "-fx-font-size: 13; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 8 6 8; " +
                            "-fx-background-radius: 6;"
                        );
                    }
                    
                    // 设置右键菜单
                    TreeItem<TreeNodeData> treeItem = getTreeItem();
                    boolean isLeaf = treeItem != null && treeItem.getChildren().isEmpty();
                    contextMenu = createTreeContextMenu(item.getLabel(), isLeaf);
                    setContextMenu(contextMenu);
                }
            }
            
            private Label createIcon(String emoji, String color) {
                Label icon = new Label(emoji);
                icon.setStyle("-fx-font-size: 14;");
                return icon;
            }
            
            /**
             * 创建树节点的右键菜单
             */
            private ContextMenu createTreeContextMenu(String nodeName, boolean isLeaf) {
                ContextMenu menu = new ContextMenu();
                
                if (isLeaf) {
                    // 任务组的右键菜单
                    MenuItem openItem = new MenuItem("📂 打开任务组");
                    openItem.setOnAction(e -> {
                        System.out.println("📂 打开任务组: " + nodeName);
                        if (selectionCallback != null) {
                            selectionCallback.onTaskSelected(nodeName);
                        }
                    });
                    
                    MenuItem renameItem = new MenuItem("✏️ 重命名");
                    renameItem.setOnAction(e -> {
                        System.out.println("✏️ 重命名任务组: " + nodeName);
                        // TODO: 显示重命名对话框
                    });
                    
                    MenuItem copyItem = new MenuItem("📋 复制");
                    copyItem.setOnAction(e -> {
                        System.out.println("📋 复制任务组: " + nodeName);
                        // TODO: 复制任务组
                    });
                    
                    MenuItem exportItem = new MenuItem("💾 导出");
                    exportItem.setOnAction(e -> {
                        System.out.println("💾 导出任务组: " + nodeName);
                        // TODO: 导出任务组
                    });
                    
                    SeparatorMenuItem separator1 = new SeparatorMenuItem();
                    
                    MenuItem propertiesItem = new MenuItem("⚙️ 属性");
                    propertiesItem.setOnAction(e -> {
                        System.out.println("⚙️ 查看属性: " + nodeName);
                        // TODO: 显示属性对话框
                    });
                    
                    SeparatorMenuItem separator2 = new SeparatorMenuItem();
                    
                    MenuItem deleteItem = new MenuItem("🗑️ 删除");
                    deleteItem.setStyle("-fx-text-fill: #EF4444;");
                    deleteItem.setOnAction(e -> {
                        System.out.println("🗑️ 删除任务组: " + nodeName);
                        // TODO: 确认并删除任务组
                    });
                    
                    menu.getItems().addAll(
                        openItem,
                        renameItem,
                        copyItem,
                        exportItem,
                        separator1,
                        propertiesItem,
                        separator2,
                        deleteItem
                    );
                } else {
                    // 分区的右键菜单
                    MenuItem newTaskItem = new MenuItem("➕ 新建任务组");
                    newTaskItem.setOnAction(e -> {
                        System.out.println("➕ 在分区 " + nodeName + " 中新建任务组");
                        // TODO: 显示新建任务组对话框
                    });
                    
                    MenuItem expandAllItem = new MenuItem("📂 展开全部");
                    expandAllItem.setOnAction(e -> {
                        TreeItem<TreeNodeData> currentItem = getTreeItem();
                        if (currentItem != null) {
                            expandAllChildren(currentItem);
                        }
                    });
                    
                    MenuItem collapseAllItem = new MenuItem("📁 折叠全部");
                    collapseAllItem.setOnAction(e -> {
                        TreeItem<TreeNodeData> currentItem = getTreeItem();
                        if (currentItem != null) {
                            collapseAllChildren(currentItem);
                        }
                    });
                    
                    MenuItem renameItem = new MenuItem("✏️ 重命名");
                    renameItem.setOnAction(e -> {
                        System.out.println("✏️ 重命名分区: " + nodeName);
                        // TODO: 显示重命名对话框
                    });
                    
                    SeparatorMenuItem separator1 = new SeparatorMenuItem();
                    
                    MenuItem deleteItem = new MenuItem("🗑️ 删除分区");
                    deleteItem.setStyle("-fx-text-fill: #EF4444;");
                    deleteItem.setOnAction(e -> {
                        System.out.println("🗑️ 删除分区: " + nodeName);
                        // TODO: 确认并删除分区
                    });
                    
                    menu.getItems().addAll(
                        newTaskItem,
                        expandAllItem,
                        collapseAllItem,
                        renameItem,
                        separator1,
                        deleteItem
                    );
                }
                
                return menu;
            }
            
            /**
             * 展开所有子节点
             */
            private void expandAllChildren(TreeItem<TreeNodeData> item) {
                if (item == null) return;
                item.setExpanded(true);
                for (TreeItem<TreeNodeData> child : item.getChildren()) {
                    expandAllChildren(child);
                }
            }
            
            /**
             * 折叠所有子节点
             */
            private void collapseAllChildren(TreeItem<TreeNodeData> item) {
                if (item == null) return;
                item.setExpanded(false);
                for (TreeItem<TreeNodeData> child : item.getChildren()) {
                    collapseAllChildren(child);
                }
            }
        });
        
        // 选择监听
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                TreeNodeData nodeData = newVal.getValue();
                System.out.println("✓ 选择节点: " + nodeData.getLabel() + " [" + nodeData.getTypeName() + "] ID: " + nodeData.getId());
                if (selectionCallback != null) {
                    // 调用新的回调方法，传递完整信息
                    selectionCallback.onTaskSelected(nodeData.getId(), nodeData.getLabel(), nodeData.getType());
                }
            }
        });
        
        return tree;
    }
    
    /**
     * 加载树形数据（从后端API）
     */
    private void loadTreeData() {
        // 在后台线程中加载数据
        new Thread(() -> {
            try {
                System.out.println("开始加载树形数据...");
                List<JobPartVo> treeData = jobPartService.getTree();
                System.out.println("成功获取树形数据，数量: " + (treeData != null ? treeData.size() : 0));
                
                // 在 JavaFX 应用线程中更新 UI
                Platform.runLater(() -> {
                    buildTreeFromData(treeData);
                    System.out.println("✓ 树形数据加载完成");
                });
                
            } catch (IOException e) {
                System.err.println("加载树形数据失败: " + e.getMessage());
                e.printStackTrace();
                
                // 如果后端服务不可用，加载示例数据
                Platform.runLater(() -> {
                    System.out.println("⚠ 后端服务不可用，加载示例数据");
                    loadSampleData();
                });
            }
        }).start();
    }
    
    /**
     * 根据后端数据构建树形结构
     */
    private void buildTreeFromData(List<JobPartVo> data) {
        if (data == null || data.isEmpty()) {
            System.out.println("⚠ 没有数据，加载示例数据");
            loadSampleData();
            return;
        }
        
        // 清空现有数据
        rootItem.getChildren().clear();
        
        // 构建树形结构
        for (JobPartVo partVo : data) {
            TreeItem<TreeNodeData> partitionItem = createTreeItem(partVo);
            rootItem.getChildren().add(partitionItem);
        }
        
        // 默认展开第一个分区并选中第一个任务
        if (!rootItem.getChildren().isEmpty()) {
            TreeItem<TreeNodeData> firstPartition = rootItem.getChildren().get(0);
            firstPartition.setExpanded(true);
            
            if (!firstPartition.getChildren().isEmpty()) {
                treeView.getSelectionModel().select(firstPartition.getChildren().get(0));
            }
        }
    }
    
    /**
     * 递归创建树节点
     */
    private TreeItem<TreeNodeData> createTreeItem(JobPartVo vo) {
        TreeNodeData nodeData = new TreeNodeData(vo.getId(), vo.getLabel(), vo.getType(), vo.getExt1());
        TreeItem<TreeNodeData> item = new TreeItem<>(nodeData);
        
        // 如果是分区（type=0），默认展开第一个
        if (vo.getType() != null && vo.getType() == 0) {
            item.setExpanded(rootItem.getChildren().isEmpty()); // 只展开第一个分区
        }
        
        // 递归添加子节点
        if (vo.getChildren() != null && !vo.getChildren().isEmpty()) {
            for (JobPartVo child : vo.getChildren()) {
                TreeItem<TreeNodeData> childItem = createTreeItem(child);
                item.getChildren().add(childItem);
            }
        }
        
        return item;
    }
    
    /**
     * 加载示例数据（备用方案）
     */
    private void loadSampleData() {
        // 清空现有数据
        rootItem.getChildren().clear();
        
        // 分区1
        TreeItem<TreeNodeData> partition1 = new TreeItem<>(new TreeNodeData(1L, "分区1", 0));
        partition1.setExpanded(true);
        
        TreeItem<TreeNodeData> task1 = new TreeItem<>(new TreeNodeData(101L, "测试任务组1", 1));
        TreeItem<TreeNodeData> task2 = new TreeItem<>(new TreeNodeData(102L, "测试任务组2", 1));
        TreeItem<TreeNodeData> task3 = new TreeItem<>(new TreeNodeData(103L, "数据处理任务", 1));
        
        partition1.getChildren().addAll(task1, task2, task3);
        
        // 分区2
        TreeItem<TreeNodeData> partition2 = new TreeItem<>(new TreeNodeData(2L, "分区2", 0));
        partition2.setExpanded(false);
        
        TreeItem<TreeNodeData> task4 = new TreeItem<>(new TreeNodeData(201L, "定时任务组", 1));
        TreeItem<TreeNodeData> task5 = new TreeItem<>(new TreeNodeData(202L, "批量任务组", 1));
        
        partition2.getChildren().addAll(task4, task5);
        
        // 分区3
        TreeItem<TreeNodeData> partition3 = new TreeItem<>(new TreeNodeData(3L, "分区3", 0));
        partition3.setExpanded(false);
        
        TreeItem<TreeNodeData> task6 = new TreeItem<>(new TreeNodeData(301L, "监控任务组", 1));
        TreeItem<TreeNodeData> task7 = new TreeItem<>(new TreeNodeData(302L, "报表任务组", 1));
        TreeItem<TreeNodeData> task8 = new TreeItem<>(new TreeNodeData(303L, "清理任务组", 1));
        
        partition3.getChildren().addAll(task6, task7, task8);
        
        // 添加到根节点
        rootItem.getChildren().addAll(partition1, partition2, partition3);
        
        // 默认选中第一个任务
        treeView.getSelectionModel().select(task1);
    }
    
    /**
     * 刷新树形数据
     */
    public void refreshTreeData() {
        loadTreeData();
    }
    
    // 保存原始的树结构，用于搜索过滤
    private java.util.Map<TreeItem<TreeNodeData>, java.util.List<TreeItem<TreeNodeData>>> originalChildren = new java.util.HashMap<>();
    
    /**
     * 搜索过滤功能
     */
    private void filterTree(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            // 清空搜索，恢复所有节点
            restoreAllNodes();
            System.out.println("✓ 搜索清空，显示所有节点");
            return;
        }
        
        String lowerSearchText = searchText.toLowerCase().trim();
        System.out.println("✓ 搜索: " + searchText);
        
        // 保存原始结构（如果还没保存）
        if (originalChildren.isEmpty()) {
            saveOriginalStructure(rootItem);
        }
        
        // 过滤节点
        filterNodeRecursive(rootItem, lowerSearchText);
    }
    
    /**
     * 保存原始树结构
     */
    private void saveOriginalStructure(TreeItem<TreeNodeData> node) {
        if (node == null) return;
        
        // 保存当前节点的子节点
        originalChildren.put(node, new java.util.ArrayList<>(node.getChildren()));
        
        // 递归保存子节点
        for (TreeItem<TreeNodeData> child : node.getChildren()) {
            saveOriginalStructure(child);
        }
    }
    
    /**
     * 递归过滤节点
     */
    private boolean filterNodeRecursive(TreeItem<TreeNodeData> node, String searchText) {
        if (node == null) return false;
        
        // 根节点特殊处理
        if (node == rootItem) {
            java.util.List<TreeItem<TreeNodeData>> originalPartitions = originalChildren.get(rootItem);
            if (originalPartitions != null) {
                // 清空根节点的子节点
                rootItem.getChildren().clear();
                
                // 递归检查每个分区
                for (TreeItem<TreeNodeData> partition : originalPartitions) {
                    if (filterNodeRecursive(partition, searchText)) {
                        // 如果分区或其子节点匹配，添加回来
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
        
        // 检查子节点是否匹配
        boolean hasMatchingChild = false;
        java.util.List<TreeItem<TreeNodeData>> originalChildList = originalChildren.get(node);
        
        if (originalChildList != null && !originalChildList.isEmpty()) {
            // 清空当前子节点
            node.getChildren().clear();
            
            // 递归检查每个子节点
            for (TreeItem<TreeNodeData> child : originalChildList) {
                String childName = child.getValue().getLabel().toLowerCase();
                boolean childMatches = childName.contains(searchText);
                
                if (childMatches) {
                    // 如果子节点匹配，添加回来
                    if (!node.getChildren().contains(child)) {
                        node.getChildren().add(child);
                    }
                    hasMatchingChild = true;
                }
            }
        }
        
        // 节点本身匹配或有匹配的子节点
        boolean shouldShow = nameMatches || hasMatchingChild;
        
        if (shouldShow && hasMatchingChild) {
            node.setExpanded(true);
        }
        
        System.out.println("   过滤节点: " + nodeName + " -> " + (shouldShow ? "显示" : "隐藏"));
        
        return shouldShow;
    }
    
    /**
     * 恢复所有节点
     */
    private void restoreAllNodes() {
        if (originalChildren.isEmpty()) return;
        
        // 恢复所有节点
        restoreNode(rootItem);
        
        // 清空保存的结构
        originalChildren.clear();
        
        // 恢复默认展开状态
        for (TreeItem<TreeNodeData> partition : rootItem.getChildren()) {
            if (partition.getValue() != null && partition.getValue().getType() == 0) {
                // 只展开第一个分区
                partition.setExpanded(rootItem.getChildren().indexOf(partition) == 0);
            }
        }
    }
    
    /**
     * 递归恢复节点
     */
    private void restoreNode(TreeItem<TreeNodeData> node) {
        if (node == null) return;
        
        java.util.List<TreeItem<TreeNodeData>> originalChildList = originalChildren.get(node);
        if (originalChildList != null) {
            // 恢复子节点
            node.getChildren().clear();
            node.getChildren().addAll(originalChildList);
            
            // 递归恢复
            for (TreeItem<TreeNodeData> child : originalChildList) {
                restoreNode(child);
            }
        }
    }
    
    /**
     * 设置选择回调
     */
    public void setSelectionCallback(TaskSelectionCallback callback) {
        this.selectionCallback = callback;
    }
    
    /**
     * 添加任务组
     */
    public void addTask(String partition, String taskName) {
        // 查找或创建分区
        TreeItem<TreeNodeData> partitionItem = findOrCreatePartition(partition);
        
        // 添加任务
        Long newId = System.currentTimeMillis();
        TreeItem<TreeNodeData> taskItem = new TreeItem<>(new TreeNodeData(newId, taskName, 1));
        partitionItem.getChildren().add(taskItem);
        partitionItem.setExpanded(true);
        
        System.out.println("✓ 添加任务: " + partition + "/" + taskName);
    }
    
    /**
     * 查找或创建分区
     */
    private TreeItem<TreeNodeData> findOrCreatePartition(String partitionName) {
        for (TreeItem<TreeNodeData> child : rootItem.getChildren()) {
            if (child.getValue().getLabel().equals(partitionName)) {
                return child;
            }
        }
        
        // 创建新分区
        Long newId = System.currentTimeMillis();
        TreeItem<TreeNodeData> newPartition = new TreeItem<>(new TreeNodeData(newId, partitionName, 0));
        newPartition.setExpanded(true);
        rootItem.getChildren().add(newPartition);
        
        return newPartition;
    }
    
    /**
     * 获取选中的任务
     */
    public String getSelectedTask() {
        TreeItem<TreeNodeData> selected = treeView.getSelectionModel().getSelectedItem();
        return selected != null && selected.getValue() != null ? selected.getValue().getLabel() : null;
    }
    
    /**
     * 设置关闭回调
     */
    public void setOnClose(Runnable callback) {
        this.onClose = callback;
    }
    
    /**
     * 设置弹出回调
     */
    public void setOnDetach(Runnable callback) {
        this.onDetach = callback;
    }
}


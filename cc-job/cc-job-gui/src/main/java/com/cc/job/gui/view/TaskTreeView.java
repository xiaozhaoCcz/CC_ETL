package com.cc.job.gui.view;

import com.cc.job.xo.model.vo.JobPartVo;
import com.cc.job.gui.model.TreeNodeData;
import com.cc.job.gui.service.JobPartService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * 任务组树形视图组件
 */
public class TaskTreeView extends VBox {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskTreeView.class);
    
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
    
    // 运行中的任务组ID集合
    private Map<Long, Boolean> runningTaskGroups = new HashMap<>();

    // 记录各节点的展开状态
    private final Map<Long, Boolean> expandedState = new HashMap<>();

    // 刷新后需要重新选中的节点ID
    private volatile Long pendingSelectId;
    
    public interface TaskSelectionCallback {
        void onTaskSelected(String taskName);
        void onTaskSelected(Long taskId, String taskName, Integer type);
        void onNewJobGroup(Long partitionId, String partitionName);
        void onNewJobNode(Long taskGroupId, String taskGroupName);
 
        default void onJobNodeAction(Long jobNodeId, Long jobId, String nodeName, JobNodeAction action) {
        }

        default void onEdgeAction(Long edgeId, EdgeAction action) {
        }
        
        default void onPartitionAction(Long partitionId, String partitionName, PartitionAction action) {
        }
        
        default void onJobGroupEdit(Long taskGroupId, String taskGroupName) {
        }

        enum JobNodeAction {
            EDIT,
            LOCATE
        }

        enum EdgeAction {
            LOCATE,
            DELETE
        }
        
        enum PartitionAction {
            EDIT,
            EXPORT
        }
    }
    
    public TaskTreeView() {
        this.jobPartService = new JobPartService();
        initializeUI();
        loadTreeData();
    }
    
    private void initializeUI() {
        // 设置面板样式
        setStyle("-fx-background-color: transparent;"+"-fx-padding: 0 0 0 8;");
        setMinWidth(240);  // 最小宽度240px
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
        HBox titleBar = new HBox(8);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        
        // 标题
        Label titleLabel = new Label("任务组");
        titleLabel.setStyle(StyleUtil.subtitle());
        
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        // 弹出按钮
        Button detachBtn = new Button("", IconUtil.windowIcon());
        StyleUtil.applyIconButtonHover(detachBtn);
        detachBtn.setTooltip(new Tooltip("弹出为独立窗口"));
        detachBtn.setOnAction(e -> {
            if (onDetach != null) {
                onDetach.run();
            }
        });
        
        // 关闭按钮
        Button closeBtn = new Button("", IconUtil.closeIcon());
        StyleUtil.applyIconButtonHover(closeBtn);
        closeBtn.setTooltip(new Tooltip("关闭面板"));
        closeBtn.setOnAction(e -> {
            if (onClose != null) {
                onClose.run();
            }
        });
        
        titleBar.getChildren().addAll(titleLabel, detachBtn, closeBtn);
        
        return titleBar;
    }
    
    private HBox createSearchBar() {
        HBox searchWrapper = new HBox();
        searchWrapper.setAlignment(Pos.CENTER_LEFT);
        searchWrapper.setSpacing(0);
        searchWrapper.setPadding(new Insets(0, 12, 0, 0));
        
        searchField = new TextField();
        searchField.setPromptText("搜索节点...");
        searchField.setPrefHeight(28);
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        // 使用内联样式覆盖 CSS，减小 padding 和字体大小
        String normalStyle = 
            "-fx-background-color: #FFFFFF; " +
            "-fx-text-fill: #111827; " +
            "-fx-font-size: 12px; " +
            "-fx-padding: 4 8; " +
            "-fx-border-color: #D1D5DB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-prompt-text-fill: #9CA3AF;";
        
        String focusedStyle = 
            "-fx-background-color: #FFFFFF; " +
            "-fx-text-fill: #111827; " +
            "-fx-font-size: 12px; " +
            "-fx-padding: 4 8; " +
            "-fx-border-color: #6366F1; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-prompt-text-fill: #9CA3AF; " +
            "-fx-effect: dropshadow(gaussian, rgba(99, 102, 241, 0.2), 3, 0, 0, 0);";
        
        searchField.setStyle(normalStyle);
        
        // 处理聚焦状态
        searchField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                searchField.setStyle(focusedStyle);
            } else {
                searchField.setStyle(normalStyle);
            }
        });
        
        searchWrapper.getChildren().add(searchField);
        
        // 搜索功能
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterTree(newVal);
        });
        
        return searchWrapper;
    }
    
    private TreeView<TreeNodeData> createTreeView() {
        // 创建根节点
        rootItem = new TreeItem<>(new TreeNodeData(0L, "所有任务组", -1));
        rootItem.setExpanded(true);
        
        // 创建树形视图
        TreeView<TreeNodeData> tree = new TreeView<>(rootItem);
        tree.setShowRoot(false);
        tree.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-color: transparent;"
        );
        
        // 自定义单元格渲染
        tree.setCellFactory(tv -> new TreeCell<TreeNodeData>() {
            
            private ContextMenu contextMenu;
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
                    stopBlinking();
                } else {
                    // 创建内容容器
                    if (contentBox == null) {
                        contentBox = new HBox(8);
                        contentBox.setAlignment(Pos.CENTER_LEFT);
                        
                        iconContainer = new StackPane();
                        iconContainer.setMinSize(16, 16);
                        iconContainer.setMaxSize(16, 16);
                        
                        textLabel = new Label();
                        
                        runningIndicator = new Circle(4);
                        runningIndicator.setFill(Color.web("#10B981"));
                        runningIndicator.setVisible(false);
                        
                        contentBox.getChildren().addAll(iconContainer, textLabel, runningIndicator);
                    }
                    
                    // 设置文本
                    textLabel.setText(item.getLabel());
                    
                    // 设置图标
                    iconContainer.getChildren().clear();
                    iconContainer.getChildren().add(IconUtil.getIconByType(item.getType()));
                    
                    // 检查是否为任务组且正在运行
                    boolean isTaskGroup = item.getType() != null && item.getType() == 1;
                    boolean isRunning = isTaskGroup && runningTaskGroups.containsKey(item.getId()) 
                        && runningTaskGroups.get(item.getId());
                    
                    // 显示/隐藏运行指示器
                    if (isRunning) {
                        runningIndicator.setVisible(true);
                        startBlinking();
                    } else {
                        runningIndicator.setVisible(false);
                        stopBlinking();
                    }
                    
                    // 设置graphic而不是text
                    setText(null);
                    setGraphic(contentBox);
                    
                    // 样式
                    setStyle(
                        StyleUtil.body() +
                        "-fx-padding: 8 12; " +
                        "-fx-background-radius: " + StyleUtil.RADIUS_MD + ";"
                    );
                    
                    // 选中样式
                    if (isSelected()) {
                        setStyle(
                            "-fx-background-color: " + StyleUtil.PRIMARY + "20; " +
                            "-fx-text-fill: " + StyleUtil.PRIMARY + "; " +
                            "-fx-font-size: 13px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-padding: 8 12; " +
                            "-fx-background-radius: " + StyleUtil.RADIUS_MD + ";"
                        );
                        textLabel.setStyle("-fx-text-fill: " + StyleUtil.PRIMARY + ";");
                    } else {
                        textLabel.setStyle("-fx-text-fill: #374151;");
                    }
                    
                    // 设置右键菜单
                    TreeItem<TreeNodeData> treeItem = getTreeItem();
                    contextMenu = createTreeContextMenu(item, treeItem);
                    setContextMenu(contextMenu);
                }
            }
            
            /**
             * 开始闪烁动画
             */
            private void startBlinking() {
                if (runningIndicator != null && !runningIndicator.isVisible()) {
                    return;
                }
                
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
            
            /**
             * 停止闪烁动画
             */
            private void stopBlinking() {
                if (blinkAnimation != null && blinkAnimation.getStatus() == FadeTransition.Status.RUNNING) {
                    blinkAnimation.stop();
                    if (runningIndicator != null) {
                        runningIndicator.setOpacity(1.0);
                    }
                }
            }
            
            /**
             * 创建树节点的右键菜单
             */
            private ContextMenu createTreeContextMenu(TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem) {
                ContextMenu menu = new ContextMenu();
                String nodeName = nodeData.getLabel();
                Integer nodeType = nodeData.getType();
                
                // type: 0=分区, 1=任务组, 2=任务节点 - 扁平化设计
                if (nodeType == 0) {
                    // 一级节点（分区）- 新建任务组 + 刷新/编辑/导出/删除
                    MenuItem newTaskItem = new MenuItem("新建任务组");
                    newTaskItem.setStyle(
                        "-fx-font-size: 13; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #2563EB;"
                    );
                    newTaskItem.setOnAction(e -> {
                        logger.debug("➕ 在分区 {} 中新建任务组, ID: {}", nodeName, nodeData.getId());
                        if (selectionCallback != null) {
                            selectionCallback.onNewJobGroup(nodeData.getId(), nodeData.getLabel());
                        }
                    });

                    MenuItem refreshItem = new MenuItem("刷新");
                    refreshItem.setStyle("-fx-text-fill: #000000;"); // 黑色字体
                    refreshItem.setOnAction(e -> handleRefresh(nodeData, treeItem));
                    
                    MenuItem editItem = new MenuItem("编辑");
                    editItem.setStyle("-fx-text-fill: #000000;"); // 黑色字体
                    editItem.setOnAction(e -> {
                        logger.debug("✏️ 编辑分区: {}, ID: {}", nodeName, nodeData.getId());
                        if (selectionCallback != null) {
                            selectionCallback.onPartitionAction(nodeData.getId(), nodeData.getLabel(), 
                                TaskSelectionCallback.PartitionAction.EDIT);
                        }
                    });
                    
                    MenuItem exportItem = new MenuItem("导出");
                    exportItem.setStyle("-fx-text-fill: #000000;"); // 黑色字体
                    exportItem.setOnAction(e -> {
                        logger.debug("📤 导出分区: {}, ID: {}", nodeName, nodeData.getId());
                        if (selectionCallback != null) {
                            selectionCallback.onPartitionAction(nodeData.getId(), nodeData.getLabel(), 
                                TaskSelectionCallback.PartitionAction.EXPORT);
                        }
                    });

                    menu.getItems().add(newTaskItem);
                    menu.getItems().add(refreshItem);
                    menu.getItems().add(editItem);
                    menu.getItems().add(exportItem);
                    if (supportsDeletion(nodeData)) {
                        menu.getItems().add(new SeparatorMenuItem());
                        menu.getItems().add(createDeleteMenuItem(nodeData, treeItem));
                    }
                    
                } else if (nodeType == 1) {
                    // 二级节点（任务组）- 新增节点、刷新、编辑功能
                    MenuItem addNodeItem = new MenuItem("新增节点");
                    addNodeItem.setStyle(
                        "-fx-font-size: 13; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #10B981;"
                    );
                    addNodeItem.setOnAction(e -> {
                        logger.debug("➕ 在任务组 {} 中新增节点, ID: {}", nodeName, nodeData.getId());
                        if (selectionCallback != null) {
                            selectionCallback.onNewJobNode(nodeData.getId(), nodeData.getLabel());
                        }
                    });
                    
                    MenuItem refreshItem = new MenuItem("刷新");
                    refreshItem.setStyle("-fx-text-fill: #000000;"); // 黑色字体
                    refreshItem.setOnAction(e -> handleRefresh(nodeData, treeItem));
                    
                    MenuItem editItem = new MenuItem("编辑");
                    editItem.setStyle("-fx-text-fill: #000000;"); // 黑色字体
                    editItem.setOnAction(e -> {
                        logger.debug("✏️ 编辑任务组: {}, ID: {}", nodeName, nodeData.getId());
                        if (selectionCallback != null) {
                            selectionCallback.onJobGroupEdit(nodeData.getId(), nodeData.getLabel());
                        }
                    });

                    menu.getItems().add(addNodeItem);
                    menu.getItems().add(refreshItem);
                    menu.getItems().add(editItem);
                    if (supportsDeletion(nodeData)) {
                        menu.getItems().add(new SeparatorMenuItem());
                        menu.getItems().add(createDeleteMenuItem(nodeData, treeItem));
                    }
                    
                } else if (nodeType != null && nodeType == 4) {
                    // 任务节点（type=4）
                    MenuItem refreshItem = new MenuItem("刷新");
                    refreshItem.setStyle("-fx-text-fill: #000000;"); // 黑色字体
                    refreshItem.setOnAction(e -> handleRefresh(nodeData, treeItem));

                    MenuItem openItem = new MenuItem("打开");
                    openItem.setOnAction(e -> {
                        logger.debug("📂 打开节点: {}", nodeName);
                        if (selectionCallback != null) {
                            selectionCallback.onTaskSelected(nodeName);
                        }
                    });

                    MenuItem editItem = new MenuItem("编辑");
                    editItem.setOnAction(e -> {
                        logger.debug("✏️ 编辑节点: {}", nodeName);
                        if (selectionCallback != null) {
                            selectionCallback.onJobNodeAction(nodeData.getId(), parseJobId(nodeData), nodeName, TaskSelectionCallback.JobNodeAction.EDIT);
                        }
                    });

                    MenuItem locateItem = new MenuItem("定位");
                    locateItem.setOnAction(e -> {
                        logger.debug("📍 定位节点: {}", nodeName);
                        if (selectionCallback != null) {
                            selectionCallback.onJobNodeAction(nodeData.getId(), parseJobId(nodeData), nodeName, TaskSelectionCallback.JobNodeAction.LOCATE);
                        }
                    });

                    MenuItem deleteItem = new MenuItem("删除");
                    deleteItem.setStyle("-fx-text-fill: #EF4444;");
                    deleteItem.setOnAction(e -> {
                        logger.debug("🗑️ 删除节点: {}", nodeName);
                        confirmDeleteNode(nodeData, treeItem);
                    });

                    menu.getItems().add(refreshItem);
                    menu.getItems().add(new SeparatorMenuItem());
                    menu.getItems().add(openItem);
                    menu.getItems().add(editItem);
                    menu.getItems().add(locateItem);
                    menu.getItems().add(new SeparatorMenuItem());
                    menu.getItems().add(deleteItem);
                } else if (nodeType != null && nodeType == 5) {
                    // 关系边
                    MenuItem locateEdgeItem = new MenuItem("定位连接");
                    locateEdgeItem.setOnAction(e -> handleLocateEdge(nodeData));

                    MenuItem deleteEdgeItem = new MenuItem("删除连接");
                    deleteEdgeItem.setStyle("-fx-text-fill: #EF4444;");
                    deleteEdgeItem.setOnAction(e -> confirmDeleteEdge(nodeData, treeItem));

                    menu.getItems().add(locateEdgeItem);
                    menu.getItems().add(new SeparatorMenuItem());
                    menu.getItems().add(deleteEdgeItem);
                } else {
                    MenuItem refreshItem = new MenuItem("刷新");
                    refreshItem.setStyle("-fx-text-fill: #000000;"); // 黑色字体
                    refreshItem.setOnAction(e -> handleRefresh(nodeData, treeItem));
                    menu.getItems().add(refreshItem);
                }
                
                return menu;
            }
        });
        
        // 选择监听
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                TreeNodeData nodeData = newVal.getValue();
                logger.debug("✓ 选择节点: {} [{}] ID: {}", nodeData.getLabel(), nodeData.getTypeName(), nodeData.getId());
                if (selectionCallback != null) {
                    // 调用新的回调方法，传递完整信息
                    selectionCallback.onTaskSelected(nodeData.getId(), nodeData.getLabel(), nodeData.getType());
                }
            }
        });

        tree.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.DELETE || event.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                TreeItem<TreeNodeData> selected = tree.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() != null) {
                    Integer type = selected.getValue().getType();
                    if (type != null && type == 5) {
                        confirmDeleteEdge(selected.getValue(), selected);
                        event.consume();
                    } else if (type != null && type == 4) {
                        confirmDeleteNode(selected.getValue(), selected);
                        event.consume();
                    }
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
                logger.debug("开始加载树形数据...");
                List<JobPartVo> treeData = jobPartService.getTree();
                logger.debug("成功获取树形数据，数量: {}", treeData != null ? treeData.size() : 0);
                
                // 在 JavaFX 应用线程中更新 UI
                Platform.runLater(() -> {
                    buildTreeFromData(treeData);
                    logger.debug("✓ 树形数据加载完成");
                });
                
            } catch (IOException e) {
                logger.error("加载树形数据失败: {}", e.getMessage(), e);
                
                // 如果后端服务不可用，加载示例数据
                Platform.runLater(() -> {
                    logger.warn("⚠ 后端服务不可用，加载示例数据");
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
            logger.warn("⚠ 没有数据，加载示例数据");
            loadSampleData();
            return;
        }

        Map<Long, Boolean> previousExpanded = new HashMap<>();
        captureExpandedState(rootItem, previousExpanded);
        expandedState.clear();
        expandedState.putAll(previousExpanded);

        // 清空搜索缓存
        originalChildren.clear();
        
        // 清空现有数据
        rootItem.getChildren().clear();
        
        // 构建树形结构：显示完整的层级结构（分区 -> 任务组）
        for (JobPartVo partVo : data) {
            // partVo 是1级节点（分区，type=0）
            // 只添加有子节点（任务组）的分区
            if (partVo.getChildren() != null && !partVo.getChildren().isEmpty()) {
                // 检查是否有任务组（type=1）
                boolean hasTaskGroup = false;
                for (JobPartVo child : partVo.getChildren()) {
                    if (child.getType() != null && child.getType() == 1) {
                        hasTaskGroup = true;
                        break;
                    }
                }
                
                // 如果有任务组，添加分区节点
                if (hasTaskGroup) {
                    // 使用createPartitionItem创建分区节点（只包含任务组，过滤掉"任务"和"关系"节点）
                    TreeItem<TreeNodeData> partitionItem = createPartitionItem(partVo);
                    rootItem.getChildren().add(partitionItem);
                }
            }
        }
        
        // 默认展开第一个分区并选中第一个任务组
        if (pendingSelectId != null) {
            Long targetId = pendingSelectId;
            TreeItem<TreeNodeData> target = findTreeItemById(rootItem, targetId);
            pendingSelectId = null;
            if (target != null) {
                treeView.getSelectionModel().select(target);
                int row = treeView.getRow(target);
                if (row >= 0) {
                    treeView.scrollTo(row);
                }
                logger.debug("✓ 定位到刷新节点: {}", targetId);
                refreshExpandedStateCache();
                return;
            } else {
                logger.warn("⚠ 未找到ID为 {} 的节点，使用默认选择", targetId);
            }
        }

        if (!rootItem.getChildren().isEmpty()) {
            TreeItem<TreeNodeData> firstPartition = rootItem.getChildren().get(0);
            TreeNodeData firstData = firstPartition.getValue();
            Long firstId = firstData != null ? firstData.getId() : null;
            if (firstId == null || !expandedState.containsKey(firstId)) {
                firstPartition.setExpanded(true);
            }
            
            if (!firstPartition.getChildren().isEmpty()) {
                TreeItem<TreeNodeData> firstChild = firstPartition.getChildren().get(0);
                TreeNodeData childData = firstChild.getValue();
                Long childId = childData != null ? childData.getId() : null;
                if (childId == null || !expandedState.containsKey(childId)) {
                    treeView.getSelectionModel().select(firstChild);
                }
            }
        }

        refreshExpandedStateCache();
    }
    
    /**
     * 创建分区节点（包含完整的层级结构：分区 -> 任务组 -> 任务/关系 -> 任务节点/关系边）
     */
    private TreeItem<TreeNodeData> createPartitionItem(JobPartVo partVo) {
        TreeNodeData partitionData = new TreeNodeData(partVo.getId(), partVo.getLabel(), partVo.getType(), partVo.getExt1());
        TreeItem<TreeNodeData> partitionItem = new TreeItem<>(partitionData);
        applyExpandedState(partitionItem);
        
        // 添加所有子节点（包括任务组及其子节点）
        if (partVo.getChildren() != null && !partVo.getChildren().isEmpty()) {
            for (JobPartVo child : partVo.getChildren()) {
                if (child.getType() != null && child.getType() == 1) {
                    // 创建任务组节点，递归添加子节点
                    TreeItem<TreeNodeData> taskGroupItem = createTreeItemRecursive(child);
                    partitionItem.getChildren().add(taskGroupItem);
                }
            }
        }
        
        return partitionItem;
    }
    
    /**
     * 递归创建树节点（包含所有子节点）
     */
    private TreeItem<TreeNodeData> createTreeItemRecursive(JobPartVo vo) {
        TreeNodeData nodeData = new TreeNodeData(vo.getId(), vo.getLabel(), vo.getType(), vo.getExt1());
        TreeItem<TreeNodeData> item = new TreeItem<>(nodeData);
        applyExpandedState(item);
        
        // 递归添加所有子节点
        if (vo.getChildren() != null && !vo.getChildren().isEmpty()) {
            for (JobPartVo child : vo.getChildren()) {
                TreeItem<TreeNodeData> childItem = createTreeItemRecursive(child);
                item.getChildren().add(childItem);
            }
        }
        
        return item;
    }
    
    /**
     * 递归创建树节点（保留用于其他场景）
     */
    @SuppressWarnings("unused")
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
        
        partition1.getChildren().add(task1);
        partition1.getChildren().add(task2);
        partition1.getChildren().add(task3);
        
        // 分区2
        TreeItem<TreeNodeData> partition2 = new TreeItem<>(new TreeNodeData(2L, "分区2", 0));
        partition2.setExpanded(false);
        
        TreeItem<TreeNodeData> task4 = new TreeItem<>(new TreeNodeData(201L, "定时任务组", 1));
        TreeItem<TreeNodeData> task5 = new TreeItem<>(new TreeNodeData(202L, "批量任务组", 1));
        
        partition2.getChildren().add(task4);
        partition2.getChildren().add(task5);
        
        // 分区3
        TreeItem<TreeNodeData> partition3 = new TreeItem<>(new TreeNodeData(3L, "分区3", 0));
        partition3.setExpanded(false);
        
        TreeItem<TreeNodeData> task6 = new TreeItem<>(new TreeNodeData(301L, "监控任务组", 1));
        TreeItem<TreeNodeData> task7 = new TreeItem<>(new TreeNodeData(302L, "报表任务组", 1));
        TreeItem<TreeNodeData> task8 = new TreeItem<>(new TreeNodeData(303L, "清理任务组", 1));
        
        partition3.getChildren().add(task6);
        partition3.getChildren().add(task7);
        partition3.getChildren().add(task8);
        
        // 添加到根节点
        rootItem.getChildren().add(partition1);
        rootItem.getChildren().add(partition2);
        rootItem.getChildren().add(partition3);
        
        // 默认选中第一个任务
        treeView.getSelectionModel().select(task1);

        refreshExpandedStateCache();
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
            logger.debug("✓ 搜索清空，显示所有节点");
            return;
        }
        
        String lowerSearchText = searchText.toLowerCase().trim();
        logger.debug("✓ 搜索: {}", searchText);
        
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
        
        logger.debug("   过滤节点: {} -> {}", nodeName, shouldShow ? "显示" : "隐藏");
        
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
        
        logger.debug("✓ 添加任务: {}/{}", partition, taskName);
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
    
    /**
     * 更新运行中的任务组状态
     * @param taskGroupId 任务组ID
     * @param isRunning 是否正在运行
     */
    public void updateTaskGroupRunningStatus(Long taskGroupId, boolean isRunning) {
        Platform.runLater(() -> {
            if (isRunning) {
                runningTaskGroups.put(taskGroupId, true);
            } else {
                runningTaskGroups.remove(taskGroupId);
            }
            
            // 刷新树形视图
            treeView.refresh();
        });
    }
    
    /**
     * 批量更新运行中的任务组
     * @param runningMap 运行状态Map
     */
    public void updateRunningTaskGroups(Map<Long, Boolean> runningMap) {
        Platform.runLater(() -> {
            this.runningTaskGroups.clear();
            if (runningMap != null) {
                this.runningTaskGroups.putAll(runningMap);
            }
            
            // 刷新树形视图
            treeView.refresh();
        });
    }
    
    /**
     * 清除所有运行状态
     */
    public void clearAllRunningStatus() {
        Platform.runLater(() -> {
            runningTaskGroups.clear();
            treeView.refresh();
        });
    }
    
    /**
     * 根据任务组名称查找任务组ID
     */
    public Long findTaskGroupIdByName(String taskGroupName) {
        if (taskGroupName == null || taskGroupName.isEmpty() || rootItem == null) {
            return null;
        }
        return findTaskGroupIdRecursive(rootItem, taskGroupName);
    }
    
    /**
     * 递归查找任务组ID
     */
    private Long findTaskGroupIdRecursive(TreeItem<TreeNodeData> item, String taskGroupName) {
        if (item == null || item.getValue() == null) {
            return null;
        }
        
        TreeNodeData nodeData = item.getValue();
        // 检查是否是任务组且名称匹配
        if (nodeData.getType() != null && nodeData.getType() == 1 && 
            taskGroupName.equals(nodeData.getLabel())) {
            return nodeData.getId();
        }
        
        // 递归查找子节点
        for (TreeItem<TreeNodeData> child : item.getChildren()) {
            Long foundId = findTaskGroupIdRecursive(child, taskGroupName);
            if (foundId != null) {
                return foundId;
            }
        }
        
        return null;
    }
    
    /**
     * 根据任务组ID选中对应的树节点
     * @param taskGroupId 任务组ID
     */
    public void selectTaskGroupById(Long taskGroupId) {
        if (taskGroupId == null) {
            return;
        }
        
        TreeItem<TreeNodeData> foundItem = findTreeItemById(rootItem, taskGroupId);
        if (foundItem != null) {
            // 选中找到的节点
            treeView.getSelectionModel().select(foundItem);
            // 确保节点可见
            int row = treeView.getRow(foundItem);
            if (row >= 0) {
                treeView.scrollTo(row);
            }
        }
    }
    
    /**
     * 递归查找指定ID的树节点
     */
    private TreeItem<TreeNodeData> findTreeItemById(TreeItem<TreeNodeData> item, Long targetId) {
        if (item == null || item.getValue() == null) {
            return null;
        }
        
        TreeNodeData nodeData = item.getValue();
        // 检查当前节点是否匹配
        if (targetId.equals(nodeData.getId())) {
            return item;
        }
        
        // 递归查找子节点
        for (TreeItem<TreeNodeData> child : item.getChildren()) {
            TreeItem<TreeNodeData> found = findTreeItemById(child, targetId);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }

    private void captureExpandedState(TreeItem<TreeNodeData> item, Map<Long, Boolean> snapshot) {
        if (item == null || snapshot == null) {
            return;
        }
        TreeNodeData data = item.getValue();
        if (data != null && data.getId() != null) {
            snapshot.put(data.getId(), item.isExpanded());
        }
        for (TreeItem<TreeNodeData> child : item.getChildren()) {
            captureExpandedState(child, snapshot);
        }
    }

    private void applyExpandedState(TreeItem<TreeNodeData> item) {
        if (item == null) {
            return;
        }
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
     * 处理刷新逻辑（可选删除）
     */
    private void handleRefresh(TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem) {
        if (nodeData == null) {
            logger.debug("🔄 刷新全部树形数据");
            reloadTreeWithFocus(null);
            return;
        }

        if (!supportsDeletion(nodeData)) {
            logger.debug("🔄 刷新节点: {}", nodeData.getLabel());
            reloadTreeWithFocus(nodeData.getId());
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("刷新节点");
        alert.setHeaderText("刷新 \"" + nodeData.getLabel() + "\"");
        alert.setContentText("是否同时删除当前节点？删除后将一并移除所有子节点。");

        ButtonType deleteButton = new ButtonType("删除节点", ButtonBar.ButtonData.OK_DONE);
        ButtonType refreshOnlyButton = new ButtonType("仅刷新", ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(deleteButton, refreshOnlyButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancelButton) {
            return;
        }

        if (result.get() == deleteButton) {
            performDelete(nodeData, treeItem);
        } else if (result.get() == refreshOnlyButton) {
            reloadTreeWithFocus(nodeData.getId());
        }
    }

    /**
     * 创建删除菜单项
     */
    private MenuItem createDeleteMenuItem(TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem) {
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setStyle("-fx-text-fill: #EF4444;");
        deleteItem.setOnAction(e -> confirmDeleteNode(nodeData, treeItem));
        return deleteItem;
    }

    /**
     * 删除确认
     */
    private void confirmDeleteNode(TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem) {
        if (!supportsDeletion(nodeData)) {
            showAlert(Alert.AlertType.INFORMATION, "删除节点", "暂不支持删除此类型的节点。");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除节点");
        alert.setHeaderText("确认删除 \"" + nodeData.getLabel() + "\" ?");
        alert.setContentText("删除后，该节点及其所有子节点都将被移除，且不可恢复。");

        ButtonType confirmButton = new ButtonType("确认删除", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == confirmButton) {
            performDelete(nodeData, treeItem);
        }
    }

    /**
     * 执行删除动作
     */
    private void performDelete(TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem) {
        if (nodeData == null || nodeData.getId() == null) {
            return;
        }

        Long nodeId = nodeData.getId();
        Integer nodeType = nodeData.getType();
        logger.debug("🗑️ 准备删除节点: {} (ID={}, type={})", nodeData.getLabel(), nodeId, nodeType);

        new Thread(() -> {
            boolean success = false;
            String errorMessage = null;

            try {
                if (nodeType != null && nodeType == 0) {
                    success = jobPartService.deleteJobPart(nodeId);
                } else if (nodeType != null && nodeType == 1) {
                    success = jobPartService.deleteJobInfo(nodeId);
                } else {
                    errorMessage = "暂不支持删除该类型的节点。";
                }
            } catch (IOException ex) {
                errorMessage = ex.getMessage();
                logger.error("删除节点发生异常: {}", ex.getMessage(), ex);
            }

            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;

            Platform.runLater(() -> {
                if (finalSuccess) {
                    Long focusId = null;
                    if (treeItem != null) {
                        TreeItem<TreeNodeData> parentItem = treeItem.getParent();
                        if (parentItem != null && parentItem.getValue() != null) {
                            Integer parentType = parentItem.getValue().getType();
                            if (parentType != null && parentType >= 0) {
                                focusId = parentItem.getValue().getId();
                            }
                        }
                    }

                    reloadTreeWithFocus(focusId);
                    showAlert(Alert.AlertType.INFORMATION, "删除成功", "节点 \"" + nodeData.getLabel() + "\" 已删除。");
                } else {
                    String message = finalErrorMessage != null ? finalErrorMessage : "删除失败，请稍后重试。";
                    showAlert(Alert.AlertType.ERROR, "删除失败", message);
                }
            });
        }, "task-tree-delete-" + nodeId).start();
    }

    /**
     * 判断节点是否支持删除
     */
    private boolean supportsDeletion(TreeNodeData nodeData) {
        if (nodeData == null || nodeData.getType() == null) {
            return false;
        }
        return nodeData.getType() == 0 || nodeData.getType() == 1;
    }

    /**
     * 刷新树并尝试保留选中节点
     */
    private void reloadTreeWithFocus(Long focusNodeId) {
        pendingSelectId = focusNodeId;
        loadTreeData();
    }

    public void updateJobNode(Long jobId, String newLabel) {
        if (jobId == null) {
            return;
        }
        TreeItem<TreeNodeData> target = findTreeItemByJobId(rootItem, jobId);
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

    private TreeItem<TreeNodeData> findTreeItemByJobId(TreeItem<TreeNodeData> current, Long jobId) {
        if (current == null) {
            return null;
        }
        TreeNodeData value = current.getValue();
        if (value != null) {
            String ext1 = value.getExt1();
            if (ext1 != null) {
                try {
                    long stored = Long.parseLong(ext1.trim());
                    if (stored == jobId) {
                        return current;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        for (TreeItem<TreeNodeData> child : current.getChildren()) {
            TreeItem<TreeNodeData> found = findTreeItemByJobId(child, jobId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
 
    /**
     * 统一弹窗提示
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Long parseJobId(TreeNodeData nodeData) {
        if (nodeData == null) {
            return null;
        }
        String ext1 = nodeData.getExt1();
        if (ext1 != null && !ext1.isBlank()) {
            try {
                return Long.parseLong(ext1.trim());
            } catch (NumberFormatException ex) {
                logger.error("解析任务节点 jobId 失败: {}, 错误: {}", ext1, ex.getMessage());
            }
        }
        return null;
    }

    private void handleLocateEdge(TreeNodeData nodeData) {
        if (selectionCallback != null) {
            selectionCallback.onEdgeAction(nodeData.getId(), TaskSelectionCallback.EdgeAction.LOCATE);
        }
    }

    private void confirmDeleteEdge(TreeNodeData nodeData, TreeItem<TreeNodeData> treeItem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除连接");
        alert.setHeaderText("删除连接: " + nodeData.getLabel());
        alert.setContentText("确定要删除该连接吗？删除后需重新保存任务组以生效。");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (selectionCallback != null) {
                selectionCallback.onEdgeAction(nodeData.getId(), TaskSelectionCallback.EdgeAction.DELETE);
            }
            TreeItem<TreeNodeData> parent = treeItem.getParent();
            if (parent != null) {
                parent.getChildren().remove(treeItem);
            }
        }
    }
}


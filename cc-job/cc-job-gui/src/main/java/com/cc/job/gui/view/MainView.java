package com.cc.job.gui.view;

import com.cc.job.gui.history.UndoRedoManager;
import com.cc.job.gui.manager.*;
import com.cc.job.gui.model.*;
import com.cc.job.gui.service.*;
import com.cc.job.gui.util.*;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.vo.JobPartVo;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.Set;

/**
 * 主界面视图 - 作为协调器，委托具体逻辑给各个管理器
 * 重构版本：将原5009行代码拆分为多个管理器，主类仅保留650行
 * @author xiaozhao
 */
public class MainView extends BorderPane {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    // UI组件
    private NodeCanvas canvas;
    private TaskTreeView treeView;
    private TopToolBar toolBar;
    private LogPanel logPanel;
    private MiniMapView miniMap;
    private CollapsedSidebar collapsedSidebar;
    private TaskNavigationBar navigationBar;
    private ScrollPane scrollPane;
    private StatusBar statusBar;
    
    // 布局容器
    private SplitPane leftArea;
    private SplitPane horizontalSplit;
    private SplitPane verticalSplit;
    
    // 管理器
    private TaskExecutionManager taskExecutionManager;
    private NodeOperationManager nodeOperationManager;
    private DialogManager dialogManager;
    private DataManager dataManager;
    private NodeCallbackConfigurator nodeCallbackConfigurator;
    
    // 状态管理
    private UndoRedoManager undoRedoManager;
    private boolean treeViewVisible = true;
    private boolean miniMapVisible = true;
    private boolean logPanelVisible = true;
    private double currentZoom = 1.0;
    
    // 服务
    private final JobGroupService jobGroupService;
    
    // 页面存储
    private final PageStoreHelper pageStoreHelper = new PageStoreHelper();
    
    // 任务组名称到ID的映射
    private final Map<String, Long> taskGroupNameToIdMap = new HashMap<>();
    
    // 任务组ID -> 滚动位置映射（用于保存和恢复每个任务组的画布位置）
    private final Map<Long, ScrollPosition> taskGroupScrollPositions = new HashMap<>();

    // 任务组ID -> 画布尺寸映射（每个任务组单独画布尺寸）
    private final Map<Long, CanvasSize> taskGroupCanvasSizes = new HashMap<>();

    public MainView() {
        this.jobGroupService = new JobGroupService();
        initializeUI();
        // 延迟初始化管理器和回调，等待 Scene 就绪
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && taskExecutionManager == null) {
                initializeManagers();
                setupCallbacks();
                setupKeyboardShortcuts();
            }
        });
    }

    private void initializeUI() {
        // 背景色由当前主题 CSS (.root) 控制

        // 顶部工具栏
        toolBar = new TopToolBar();
        this.setTop(toolBar);

        // 折叠侧边栏
        collapsedSidebar = new CollapsedSidebar();

        // 左侧区域：树形导航 + 小地图
        leftArea = new SplitPane();
        leftArea.setOrientation(Orientation.VERTICAL);
        leftArea.setStyle("-fx-background-color: transparent;");
        
        treeView = new TaskTreeView();
        miniMap = new MiniMapView();
        leftArea.getItems().addAll(treeView, miniMap);
        leftArea.setDividerPositions(0.7); // 树形菜单占70%，小地图占30%

        // 左侧容器（企业级深色边栏）
        HBox leftContainer = new HBox(0);
        leftContainer.getStyleClass().add("sidebar-left");
        leftContainer.getChildren().addAll(collapsedSidebar, leftArea);
        HBox.setHgrow(leftArea, Priority.ALWAYS);

        // 任务组导航栏
        navigationBar = new TaskNavigationBar();

        // 画布
        canvas = new NodeCanvas();
        undoRedoManager = new UndoRedoManager();
        undoRedoManager.setOnChange(this::updateUndoRedoButtons);
        canvas.setUndoRedoManager(undoRedoManager);
        
        scrollPane = new ScrollPane(canvas);
        canvas.setScrollPane(scrollPane);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);

        // 画布区域容器（中央白底）
        VBox canvasArea = new VBox(0);
        canvasArea.getStyleClass().add("content-area");
        canvasArea.getChildren().addAll(navigationBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 日志面板
        logPanel = new LogPanel();

        // 垂直分割面板
        verticalSplit = new SplitPane();
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getStyleClass().add("content-area");
        verticalSplit.getItems().addAll(canvasArea, logPanel);
        verticalSplit.setDividerPositions(0.7);
        verticalSplit.setPadding(Insets.EMPTY);

        // 水平分割面板
        horizontalSplit = new SplitPane();
        horizontalSplit.setOrientation(Orientation.HORIZONTAL);
        horizontalSplit.getItems().addAll(leftContainer, verticalSplit);
        horizontalSplit.setDividerPositions(0.2);
        horizontalSplit.setPadding(Insets.EMPTY);

        miniMap.bindTo(canvas, scrollPane);
        this.setCenter(horizontalSplit);

        // 底部状态栏（企业级 IDE 风格）
        statusBar = new StatusBar();
        this.setBottom(statusBar);
    }
    
    private void initializeManagers() {
        Stage ownerStage = (Stage) this.getScene().getWindow();

        dialogManager = new DialogManager(ownerStage, logPanel);
        taskExecutionManager = new TaskExecutionManager(canvas, logPanel, navigationBar, toolBar);
        nodeOperationManager = new NodeOperationManager(canvas, logPanel, treeView, ownerStage);
        dataManager = new DataManager(canvas, logPanel, treeView);
        nodeCallbackConfigurator = new NodeCallbackConfigurator(nodeOperationManager, canvas, logPanel);
        
        // 重要：设置对话框管理器的任务执行管理器（用于获取预测时间）
        dialogManager.setTaskExecutionManager(taskExecutionManager);
        // 重要：设置节点操作管理器（用于从任务列表添加节点时本地入画布并支持撤销）
        dialogManager.setNodeOperationManager(nodeOperationManager);
        
        // 重要：设置节点操作管理器的回调配置器（用于新增节点时自动配置回调）
        nodeOperationManager.setNodeCallbackConfigurator(nodeCallbackConfigurator);
        
        // 重要：设置对话框管理器（用于显示节点详情对话框）
        nodeCallbackConfigurator.setDialogManager(dialogManager);
        // 重要：设置主窗口Stage（用于显示依赖关系面板）
        nodeCallbackConfigurator.setOwnerStage(ownerStage);
        
        // 设置canvas的ownerStage（用于连线样式对话框等）
        canvas.setOwnerStage(ownerStage);
        
        // 设置颜色变更回调(用于保存颜色到数据库)
        nodeCallbackConfigurator.setOnColorChangedCallback(() -> {
            Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            if (currentTaskGroupId != null && currentTaskGroupId != 0) {
                // 异步保存,不阻塞UI
                dataManager.saveOrUpdateJob(currentTaskGroupId);
            }
        });
        
        // 设置数据加载完成后的回调，配置所有节点的操作回调
        dataManager.setOnDataLoaded(() -> {
            Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            if (currentTaskGroupId != null) {
                nodeCallbackConfigurator.configureAllNodeCallbacks(currentTaskGroupId);
                
                // ⭐ 修复：切换任务组标签后，如果任务组正在运行，恢复边的运行状态
                if (taskExecutionManager != null && taskExecutionManager.isTaskGroupRunning(currentTaskGroupId)) {
                    canvas.setAllConnectionsRunning(true);
                }

                // 首次进入该任务组：根据内容扩展画布并将内容居中；否则恢复滚动位置
                // 统一使用 50ms 延迟，在回调中先执行「画布过大则收缩」，再扩展或恢复滚动，避免右侧/下方大片空白
                Platform.runLater(() -> {
                    javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(50));
                    delay.setOnFinished(e -> {
                        shrinkCanvasToFitContentIfNeeded(currentTaskGroupId);
                        if (currentTaskGroupId == 0) {
                            restoreScrollPosition(currentTaskGroupId);
                        } else if (taskGroupScrollPositions.get(currentTaskGroupId) == null) {
                            expandCanvasToFitContentAndCenter(currentTaskGroupId);
                        } else {
                            restoreScrollPosition(currentTaskGroupId);
                        }
                    });
                    delay.play();
                });
            }
        });
        
        // 重要：重新设置撤销重做管理器，确保管理器初始化后画布仍能正常工作
        canvas.setUndoRedoManager(undoRedoManager);
    }

    private void setupCallbacks() {
        // 工具栏回调
        toolBar.setCallback(new TopToolBar.ToolBarCallback() {
            @Override
            public void onNew() {
                // 导航栏"新增任务"使用新的 NewJobDialog 页面（含调度配置）
                dialogManager.showJobDialog(null, () -> {
                    dataManager.refreshTreeView();
                });
            }

            @Override
            public void onNewPart() {
                dialogManager.showNewPartitionDialog(() -> dataManager.refreshTreeView());
            }

            @Override
            public void onOpen() {
                // 导入分区文件
                importPartitionFile();
            }

            @Override
            public void onSave() {
                // 异步保存，不阻塞主线程
                dataManager.saveOrUpdateJob(pageStoreHelper.getCurrentTaskGroupId());
            }

            @Override
            public void onUndo() {
                if (undoRedoManager.canUndo()) {
                    undoRedoManager.undo();
                    logger.info("↩ 已撤销");
                }
            }

            @Override
            public void onRedo() {
                if (undoRedoManager.canRedo()) {
                    undoRedoManager.redo();
                    logger.info("↪ 已重做");
                }
            }

            @Override
            public void onZoomIn() {
                zoomCanvas(currentZoom + 0.1);
            }

            @Override
            public void onZoomOut() {
                zoomCanvas(currentZoom - 0.1);
            }

            @Override
            public void onZoomFit() {
                zoomCanvas(1.0);
            }

            @Override
            public void onExportCanvas() {
                Stage ownerStage = (Stage) MainView.this.getScene().getWindow();
                CanvasExportDialog exportDialog = new CanvasExportDialog(ownerStage);
                
                exportDialog.showAndWait().ifPresent(file -> {
                    if (file != null) {
                        com.cc.job.gui.manager.CanvasExportManager exportManager = 
                            new com.cc.job.gui.manager.CanvasExportManager(logger::info);
                        com.cc.job.gui.manager.CanvasExportManager.ExportConfig config = exportDialog.getConfig();
                        boolean success = exportManager.exportCanvas(canvas, file, config);
                        if (success) {
                            logger.info("✓ 画布已导出到: " + file.getAbsolutePath());
                        } else {
                            NotificationToast.showError("✗ 导出失败");
                            logger.error("✗ 导出失败");
                        }
                    }
                });
            }
            
            @Override
            public void onNodeHistory() {
                Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                if (currentTaskGroupId == null) {
                    NotificationToast.showWarning("⚠ 请先选择一个任务组");
                    logger.warn("⚠ 请先选择一个任务组");
                    return;
                }
                String taskGroupName = getJobNameById(currentTaskGroupId);
                showNodeHistoryDialog(currentTaskGroupId, taskGroupName != null ? taskGroupName : "任务组" + currentTaskGroupId);
            }

            @Override
            public void onRun() {
                Long jobId = pageStoreHelper.getCurrentTaskGroupId();
                String jobName = getJobNameById(jobId);
                
                // ⭐ 运行前检测循环依赖
                boolean hasCycle = canvas.detectAndHighlightCycles();
                if (hasCycle) {
                    // 获取详细的循环信息
                    com.cc.job.gui.manager.CycleDetectionManager.CycleDetectionResult result = 
                        canvas.getCycleDetectionResult();
                    
                    // 构建循环路径信息
                    StringBuilder message = new StringBuilder();
                    message.append("检测到循环依赖，无法运行任务！\n\n");
                    message.append("参与循环的连接线已标记为红色并加粗显示。\n\n");
                    
                    if (!result.getCyclePaths().isEmpty()) {
                        message.append("循环路径：\n");
                        for (int i = 0; i < result.getCyclePaths().size(); i++) {
                            List<String> path = result.getCyclePaths().get(i);
                            if (path.size() > 1) {
                                message.append("循环 ").append(i + 1).append(": ");
                                message.append(String.join(" → ", path));
                                message.append("\n");
                            }
                        }
                    }
                    message.append("\n请修复循环依赖后再运行任务。");
                    
                    // 显示错误对话框阻止运行
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("循环依赖错误");
                    alert.setHeaderText("存在循环依赖，无法运行");
                    alert.setContentText(message.toString());
                    alert.showAndWait();
                    
                    logPanel.error("✗ 存在循环依赖，运行已取消");
                    return;
                }
                
                // 清除循环标记（如果没有循环）
                canvas.clearCycleHighlight();
                
                // ⭐ 清除节点状态缓存（避免旧状态影响）
                NodeStatusSyncManager.getInstance().clearCacheForTaskGroupSwitch();
                
                // ⭐ 立即重置所有节点状态为空闲（让用户立即看到变化）
                for (ProcessNode node : canvas.getNodes()) {
                    node.updateStatus(ProcessNode.NodeStatus.IDLE);
                }
                
                // 立即显示加载状态（转圈圈）
                toolBar.setRunButtonLoading(true);
                
                // 如果有未保存的更改，在后台同步保存（等待完成后再运行）
                if (canvas.hasUnsavedChanges()) {
                    logPanel.info("💾 运行前保存数据...");
                    
                    // ⭐ 关键修复：在新线程中同步保存数据，确保保存完成后再触发运行
                    // 避免保存和运行的时序竞态问题
                    new Thread(() -> {
                        try {
                            // 同步保存（会阻塞当前线程，直到保存完成）
                            dataManager.saveOrUpdateJobSync(jobId);
                            
                            Platform.runLater(() -> {
                                canvas.markAsSaved();
                                logPanel.info("✓ 保存完成，开始运行任务");
                                // 保存完成后开始运行
                                taskExecutionManager.triggerJobExecution(jobId, jobName != null ? jobName : "任务组" + jobId);
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                toolBar.setRunButtonLoading(false);
                                logPanel.error("保存失败: " + e.getMessage());
                                
                                // ⭐ 保存失败时，从服务器同步节点状态（恢复为实际状态）
                                canvas.syncPendingNodeStatus();
                            });
                        }
                    }).start();
                } else {
                    // 没有未保存更改，直接运行
                    taskExecutionManager.triggerJobExecution(jobId, jobName != null ? jobName : "任务组" + jobId);
                }
            }

            @Override
            public void onStop(Long jobId) {
                taskExecutionManager.stopJobExecution(jobId);
            }

            @Override
            public void onClear() {
                canvas.disableAutoSave();
                canvas.clear();
                canvas.enableAutoSave();
                NotificationToast.showWarning("画布已清空");
                logger.warn("画布已清空");
            }

            @Override
            public void onSettings() {
                Stage ownerStage = (Stage) MainView.this.getScene().getWindow();
                SettingsDialog settingsDialog = new SettingsDialog(ownerStage);
                settingsDialog.showAndWait();
            }

            @Override
            public void onSelect() {
                boolean currentMode = canvas.isSelectionMode();
                canvas.setSelectionMode(!currentMode);
                logger.info(currentMode ? "✓ 框选模式已禁用" : "✓ 框选模式已启用");
            }

            @Override
            public void onLayoutHorizontal() {
                canvas.alignHorizontal();
            }

            @Override
            public void onLayoutVertical() {
                canvas.alignVertical();
            }
            
            @Override
            public void onDistributeHorizontally() {
                canvas.distributeNodesHorizontally();
                logger.info("✓ 水平等距分布完成");
            }
            
            @Override
            public void onDistributeVertically() {
                canvas.distributeNodesVertically();
                logger.info("✓ 垂直等距分布完成");
            }
            
            @Override
            public void onAlignToCenter() {
                canvas.alignToCanvasCenter();
                logger.info("✓ 已对齐到画布中心");
            }
            
            @Override
            public void onAlignLeft() {
                canvas.alignLeft();
            }
            
            @Override
            public void onAlignRight() {
                canvas.alignRight();
            }
            
            @Override
            public void onAlignHorizontalCenter() {
                canvas.alignHorizontalCenter();
            }
            
            @Override
            public void onAlignTop() {
                canvas.alignTop();
            }
            
            @Override
            public void onAlignBottom() {
                canvas.alignBottom();
            }
            
            @Override
            public void onAlignVerticalCenter() {
                canvas.alignVerticalCenter();
            }
            
            @Override
            public void onAlignToGroupLeft() {
                canvas.alignToGroupLeft();
            }
            
            @Override
            public void onAlignToGroupRight() {
                canvas.alignToGroupRight();
            }
            
            @Override
            public void onAlignToGroupHorizontalCenter() {
                canvas.alignToGroupHorizontalCenter();
            }
            
            @Override
            public void onAlignToGroupTop() {
                canvas.alignToGroupTop();
            }
            
            @Override
            public void onAlignToGroupBottom() {
                canvas.alignToGroupBottom();
            }
            
            @Override
            public void onAlignToGroupVerticalCenter() {
                canvas.alignToGroupVerticalCenter();
            }
            
            @Override
            public void onToggleSnapToGrid() {
                boolean currentState = canvas.isSnapToGridEnabled();
                canvas.setSnapToGridEnabled(!currentState);
                logger.info("✓ 网格吸附已" + (!currentState ? "启用" : "禁用"));
                // 按钮状态会通过监听器自动更新
            }
            
            @Override
            public void onDetectCycles() {
                // 执行循环依赖检测
                boolean hasCycle = canvas.detectAndHighlightCycles();
                
                if (hasCycle) {
                    // 获取详细的循环信息
                    com.cc.job.gui.manager.CycleDetectionManager.CycleDetectionResult result = 
                        canvas.getCycleDetectionResult();
                    
                    // 构建循环路径信息
                    StringBuilder message = new StringBuilder();
                    message.append("检测到循环依赖！\n\n");
                    message.append("参与循环的连接线已标记为红色并加粗显示。\n\n");
                    
                    if (!result.getCyclePaths().isEmpty()) {
                        message.append("循环路径：\n");
                        for (int i = 0; i < result.getCyclePaths().size(); i++) {
                            List<String> path = result.getCyclePaths().get(i);
                            if (path.size() > 1) {
                                message.append("循环 ").append(i + 1).append(": ");
                                message.append(String.join(" → ", path));
                                message.append("\n");
                            }
                        }
                    }
                    
                    // 显示警告对话框
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("循环依赖检测");
                    alert.setHeaderText("检测到循环依赖");
                    alert.setContentText(message.toString());
                    alert.showAndWait();
                    
                    NotificationToast.showWarning("⚠ 检测到循环依赖，请修复后再运行任务");
                    logger.warn("⚠ 检测到循环依赖，请修复后再运行任务");
                } else {
                    // 清除之前的标记
                    canvas.clearCycleHighlight();
                    
                    // 显示成功提示
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("循环依赖检测");
                    alert.setHeaderText("检测完成");
                    alert.setContentText("未检测到循环依赖，画布结构正常。");
                    alert.showAndWait();
                    
                    logger.info("✓ 未检测到循环依赖");
                }
            }

            @Override
            public void onJobList() {
                dialogManager.showJobListDialog();
            }

            @Override
            public void onJobGroupList() {
                dialogManager.showJobGroupListDialog();
            }

            @Override
            public void onJobLogList() {
                dialogManager.showJobLogListDialog();
            }

            @Override
            public void onTaskReport() {
                dialogManager.showTaskReportDialog();
            }

            @Override
            public void onDatasourceList() {
                dialogManager.showDatasourceListDialog();
            }

            @Override
            public void onDataxSync() {
                dialogManager.showDataxSyncDialog();
            }

            @Override
            public void onDataxGroupSync() {
                dialogManager.showDataxGroupSyncDialog();
            }

            @Override
            public List<JobGroup> onRequestTaskList() {
                try {
                    return jobGroupService.getAllJobGroupList();
                } catch (Exception e) {
                    logger.error("加载任务列表失败", e);
                    return Collections.emptyList();
                }
            }

            @Override
            public void onTaskSelected(Long taskGroupId, String taskGroupName) {
                if (taskGroupId == null) {
                    NotificationToast.showWarning("⚠ 任务ID无效");
                    logger.warn("⚠ 任务ID无效");
                    return;
                }
                
                // 切换任务组前，先保存当前任务组的数据和滚动位置
                Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                if (currentTaskGroupId != null && currentTaskGroupId != 0 && !currentTaskGroupId.equals(taskGroupId)) {
                    if (canvas.hasUnsavedChanges()) {
                        dataManager.saveOrUpdateJob(currentTaskGroupId);
                        canvas.markAsSaved();
                    }
                    // 保存当前任务组的滚动位置
                    saveCurrentCanvasSize();
                    saveCurrentScrollPosition();
                }
                
                String displayName = (taskGroupName != null && !taskGroupName.isBlank())
                    ? taskGroupName
                    : ("任务组 " + taskGroupId);
                
                taskGroupNameToIdMap.put(displayName, taskGroupId);
                pageStoreHelper.setCurrentPage(taskGroupId);
                navigationBar.addOrSelectTask(displayName, taskGroupId);
                toolBar.setCurrentTaskGroupId(taskGroupId);
                // 数据加载后会自动配置节点回调，并恢复滚动位置
                restoreCanvasSize(taskGroupId);
                dataManager.loadTaskGroupData(taskGroupId, displayName);
                
                // 添加到最近打开的文件列表
                com.cc.job.gui.util.RecentFilesManager.getInstance().addRecentFile(taskGroupId, displayName);
                toolBar.refreshRecentFilesMenu();
            }
            
            // 文件菜单扩展
            @Override
            public void onSaveAs() {
                Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                if (currentTaskGroupId == null) {
                    NotificationToast.showWarning("⚠ 请先选择一个任务组");
                    logger.warn("⚠ 请先选择一个任务组");
                    return;
                }
                exportTaskGroup(currentTaskGroupId);
            }

            @Override
            public void onExportPartition() {
                TreeNodeData selected = treeView.getSelectedTreeNodeData();
                if (selected != null && selected.getType() != null && selected.getType() == 0) {
                    exportPartitionData(selected.getId(), selected.getLabel());
                } else {
                    NotificationToast.showWarning("⚠ 请先在左侧树中选择一个分区");
                    logger.warn("⚠ 请先在左侧树中选择一个分区");
                }
            }

            @Override
            public void onImportTaskGroup() {
                Long partitionId = null;
                TreeNodeData selected = treeView.getSelectedTreeNodeData();
                if (selected != null && selected.getType() != null && selected.getType() == 0) {
                    partitionId = selected.getId();
                }
                if (partitionId == null) {
                    Optional<Long> chosen = showSelectPartitionDialog();
                    if (chosen.isEmpty()) {
                        return;
                    }
                    partitionId = chosen.get();
                }
                importTaskGroupFile(partitionId);
            }
            
            @Override
            public void onExit() {
                Stage stage = (Stage) MainView.this.getScene().getWindow();
                stage.fireEvent(new javafx.stage.WindowEvent(stage, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
            }
            
            @Override
            public void onRecentFile(Long taskGroupId, String taskGroupName) {
                onTaskSelected(taskGroupId, taskGroupName);
            }
            
            @Override
            public List<TopToolBar.RecentFile> getRecentFiles() {
                return com.cc.job.gui.util.RecentFilesManager.getInstance().getRecentFiles();
            }
            
            // 编辑菜单扩展
            @Override
            public void onCut() {
                handleCopyShortcut();
                Set<ProcessNode> selectedNodes = canvas.getSelectedNodes();
                if (!selectedNodes.isEmpty()) {
                    canvas.removeNodesAsBatch(selectedNodes);
                    logger.info("✓ 已剪切节点");
                }
            }
            
            @Override
            public void onCopy() {
                handleCopyShortcut();
            }
            
            @Override
            public void onPaste() {
                handlePasteShortcut();
            }
            
            @Override
            public void onDelete() {
                Set<ProcessNode> selectedNodes = canvas.getSelectedNodes();
                if (selectedNodes.isEmpty()) {
                    NotificationToast.showWarning("⚠ 请先选中要删除的节点");
                    logger.warn("⚠ 请先选中要删除的节点");
                    return;
                }
                canvas.removeNodesAsBatch(selectedNodes);
                logger.info("✓ 已删除节点");
            }
            
            @Override
            public void onSelectAll() {
                canvas.selectAllNodes();
                logger.info("✓ 已全选所有节点");
            }
            
            @Override
            public void onBatchEdit() {
                Set<ProcessNode> selectedNodes = canvas.getSelectedNodes();
                if (selectedNodes.isEmpty()) {
                    NotificationToast.showWarning("⚠ 请先选中要编辑的节点");
                    logger.warn("⚠ 请先选中要编辑的节点");
                    return;
                }
                
                Stage ownerStage = (Stage) MainView.this.getScene().getWindow();
                com.cc.job.gui.view.BatchEditDialog batchDialog = new com.cc.job.gui.view.BatchEditDialog(
                    ownerStage,
                    selectedNodes
                );
                
                batchDialog.showAndWait().ifPresent(result -> {
                    if (result != null) {
                        // 如果是删除操作，需要确认
                        if (result.getOperation() == com.cc.job.gui.view.BatchEditDialog.BatchEditOperation.DELETE) {
                            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                            confirm.setTitle("确认删除");
                            confirm.setHeaderText("确定要删除选中的 " + selectedNodes.size() + " 个节点吗？");
                            confirm.setContentText("此操作不可撤销！");
                            confirm.initOwner(ownerStage);
                            String themeCss = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
                            if (themeCss != null && !themeCss.isEmpty()) {
                                confirm.getDialogPane().getStylesheets().add(themeCss);
                            }
                            confirm.showAndWait().ifPresent(buttonType -> {
                                if (buttonType == ButtonType.OK) {
                                    nodeOperationManager.batchEditNodes(selectedNodes, result);
                                }
                            });
                        } else {
                            nodeOperationManager.batchEditNodes(selectedNodes, result);
                        }
                    }
                });
            }
            
            @Override
            public void onFindNode() {
                // 显示高级搜索对话框
                Stage ownerStage = (Stage) MainView.this.getScene().getWindow();
                NodeSearchDialog searchDialog = new NodeSearchDialog(
                    ownerStage,
                    canvas.getNodes(),
                    node -> {
                        // 定位到节点
                        canvas.locateNode(node);
                        logger.info("✓ 已定位到节点: " + node.getJobHandlerName());
                    }
                );
                searchDialog.showAndWait();
            }
            
            @Override
            public void onFindNext() {
                // TODO: 实现查找下一个
                logger.info("查找下一个功能开发中...");
            }
            
            @Override
            public void onFindPrevious() {
                // TODO: 实现查找上一个
                logger.info("查找上一个功能开发中...");
            }
            
            @Override
            public void onAutoLayout() {
                canvas.autoLayout();
                logger.info("✓ 已自动布局（网格布局）");
            }
            
            @Override
            public void onAutoLayout(com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm algorithm) {
                canvas.autoLayout(algorithm);
                String algorithmName = algorithm == com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.GRID ? "网格布局" :
                                      algorithm == com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.HIERARCHICAL ? "层次化布局" :
                                      algorithm == com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.FORCE_DIRECTED ? "力导向布局" :
                                      algorithm == com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.TREE ? "树形布局" : "未知布局";
                logger.info("✓ 已自动布局（" + algorithmName + "）");
            }
            
            // 选择菜单
            @Override
            public void onInvertSelection() {
                canvas.invertSelection();
                logger.info("✓ 已反选");
            }
            
            @Override
            public void onSelectByType(String type) {
                canvas.selectByType(type);
                logger.info("✓ 已按类型选择: " + type);
            }
            
            @Override
            public void onClearSelection() {
                canvas.clearSelection();
            }
            
            @Override
            public void onSelectUpstream() {
                canvas.selectUpstreamNodes();
                logger.info("✓ 已选择上游节点");
            }
            
            @Override
            public void onSelectDownstream() {
                canvas.selectDownstreamNodes();
                logger.info("✓ 已选择下游节点");
            }
            
            // 查看菜单
            @Override
            public void onZoomActualSize() {
                zoomCanvas(1.0);
            }
            
            @Override
            public void onToggleTreeView() {
                treeViewVisible = !treeViewVisible;
                updateLeftSidebar();
            }
            
            @Override
            public void onToggleMiniMap() {
                miniMapVisible = !miniMapVisible;
                updateLeftSidebar();
            }
            
            @Override
            public void onToggleLogPanel() {
                logPanelVisible = !logPanelVisible;
                updateLeftSidebar();
            }
            
            @Override
            public void onResetLayout() {
                treeViewVisible = true;
                miniMapVisible = true;
                logPanelVisible = true;
                updateLeftSidebar();
                logger.info("✓ 已重置布局");
            }

            @Override
            public void onAddBookmark() {
                Long taskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                if (taskGroupId == null || taskGroupId == 0) {
                    NotificationToast.showWarning("请先选择任务组");
                    return;
                }
                double hvalue = scrollPane != null ? scrollPane.getHvalue() : 0.5;
                double vvalue = scrollPane != null ? scrollPane.getVvalue() : 0.5;
                Long selectedNodeJobIdFinal = null;
                Set<ProcessNode> selected = canvas.getSelectedNodes();
                if (selected != null && selected.size() == 1) {
                    ProcessNode node = selected.iterator().next();
                    if (node != null && node.getJobId() != null) {
                        selectedNodeJobIdFinal = node.getJobId();
                    }
                }
                final Long selectedNodeJobId = selectedNodeJobIdFinal;
                Stage stage = (Stage) MainView.this.getScene().getWindow();
                AddBookmarkDialog dialog = new AddBookmarkDialog(stage, selectedNodeJobId != null);
                dialog.showAndWait().ifPresent(result -> {
                    com.cc.job.xo.model.entity.JobCanvasBookmark bookmark = new com.cc.job.xo.model.entity.JobCanvasBookmark();
                    bookmark.setTaskGroupId(taskGroupId);
                    bookmark.setBookmarkName(result.getBookmarkName());
                    bookmark.setDescription(result.getDescription());
                    bookmark.setBookmarkType(result.getBookmarkType());
                    bookmark.setZoomLevel(1.0);
                    if ("position".equals(result.getBookmarkType())) {
                        bookmark.setCanvasPositionX(hvalue);
                        bookmark.setCanvasPositionY(vvalue);
                    } else {
                        bookmark.setTargetNodeId(selectedNodeJobId);
                    }
                    new Thread(() -> {
                        try {
                            com.cc.job.gui.service.JobCanvasBookmarkApiService api = new com.cc.job.gui.service.JobCanvasBookmarkApiService();
                            Long id = api.save(bookmark);
                            Platform.runLater(() -> {
                                if (id != null) {
                                    NotificationToast.showSuccess("✓ 已添加书签: " + result.getBookmarkName());
                                } else {
                                    NotificationToast.showError("✗ 添加书签失败");
                                }
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> NotificationToast.showError("✗ 添加书签失败: " + e.getMessage()));
                            logger.error("添加书签失败", e);
                        }
                    }).start();
                });
            }

            @Override
            public void onBookmarkList() {
                Long taskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                if (taskGroupId == null || taskGroupId == 0) {
                    NotificationToast.showWarning("请先选择任务组");
                    return;
                }
                Stage stage = (Stage) MainView.this.getScene().getWindow();
                BookmarkListDialog listDialog = new BookmarkListDialog(
                    stage,
                    taskGroupId,
                    pos -> {
                        if (scrollPane != null && pos != null && pos.length >= 2) {
                            scrollPane.setHvalue(pos[0]);
                            scrollPane.setVvalue(pos[1]);
                        }
                    },
                    nodeJobId -> {
                        if (nodeJobId != null) {
                            ProcessNode node = canvas.getNodeByJobId(nodeJobId);
                            if (node != null) {
                                canvas.locateNode(node);
                            } else {
                                NotificationToast.showWarning("未找到对应节点");
                            }
                        }
                    }
                );
                listDialog.showAndWait();
            }
            
            @Override
            public void onToggleGrid() {
                canvas.toggleGrid();
                logger.info("✓ 已切换网格显示");
            }
            
            @Override
            public void onToggleRuler() {
                canvas.toggleRuler();
                logger.info("✓ 已切换标尺显示");
            }
            
            @Override
            public void onToggleNodeLabels() {
                canvas.toggleNodeLabels();
                logger.info("✓ 已切换节点标签显示");
            }
            
            @Override
            public void onToggleEdgeLabels() {
                canvas.toggleEdgeLabels();
                logger.info("✓ 已切换连线标签显示");
            }
            
            @Override
            public void onSetTheme(String theme) {
                ThemeManager.getInstance().setTheme(theme);
                logger.info("已切换主题: " + theme);
            }
            
            @Override
            public void onToggleFullScreen() {
                Stage stage = (Stage) MainView.this.getScene().getWindow();
                stage.setFullScreen(!stage.isFullScreen());
            }
            
            // 转到菜单
            @Override
            public void onGoToNode() {
                showGoToNodeDialog();
            }
            
            @Override
            public void onGoToTaskGroup() {
                showGoToTaskGroupDialog();
            }
            
            @Override
            public void onGoToPartition() {
                showGoToPartitionDialog();
            }
            
            @Override
            public void onPreviousNode() {
                canvas.navigateToPreviousNode();
            }
            
            @Override
            public void onNextNode() {
                canvas.navigateToNextNode();
            }
            
            @Override
            public void onLocateSelectedNode() {
                Set<ProcessNode> selectedNodes = canvas.getSelectedNodes();
                if (!selectedNodes.isEmpty()) {
                    ProcessNode node = selectedNodes.iterator().next();
                    canvas.locateNode(node);
                    logger.info("✓ 已定位到选中节点");
                } else {
                    NotificationToast.showWarning("⚠ 请先选中一个节点");
                    logger.warn("⚠ 请先选中一个节点");
                }
            }
            
            @Override
            public void onLocateRunningNode() {
                canvas.locateRunningNode();
                logger.info("✓ 已定位到运行中的节点");
            }
            
            // 运行菜单扩展
            @Override
            public void onRerun() {
                Long jobId = pageStoreHelper.getCurrentTaskGroupId();
                if (jobId != null) {
                    onStop(jobId);
                    // 等待停止后重新运行
                    Platform.runLater(() -> {
                        try {
                            Thread.sleep(500);
                            onRun();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
            
            @Override
            public void onRunSelectedNodes() {
                // TODO: 实现运行选中的节点
                logger.info("运行选中节点功能开发中...");
            }
            
            @Override
            public void onRunToHere() {
                // TODO: 实现运行到此处
                logger.info("运行到此处功能开发中...");
            }
            
            // 任务菜单扩展
            @Override
            public void onNewJobGroup() {
                dialogManager.showJobGroupDialog(null, null, null, () -> {
                    dataManager.refreshTreeView();
                });
            }
            
            @Override
            public void onEditJobGroup() {
                Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                if (currentTaskGroupId == null) {
                    NotificationToast.showWarning("⚠ 请先选择一个任务组");
                    logger.warn("⚠ 请先选择一个任务组");
                    return;
                }
                String taskGroupName = getJobNameById(currentTaskGroupId);
                new Thread(() -> {
                    try {
                        JobInfoForm formData = new JobInfoService().getJobNodeFormData(currentTaskGroupId);
                        Platform.runLater(() -> {
                            Long partitionId = formData.getJobPartId() != null ? formData.getJobPartId().longValue() : null;
                            dialogManager.showJobGroupDialog(partitionId, taskGroupName, formData, () -> {
                                dataManager.refreshTreeView();
                            });
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            NotificationToast.showError("✗ 加载任务组数据失败: " + e.getMessage());
                        });
                        logger.error("✗ 加载任务组数据失败: {}", e.getMessage());
                    }
                }).start();
            }
            
            @Override
            public void onDeleteJobGroup() {
                Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                if (currentTaskGroupId == null) {
                    NotificationToast.showWarning("⚠ 请先选择一个任务组");
                    logger.warn("⚠ 请先选择一个任务组");
                    return;
                }
                // 显示确认对话框
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("确认删除");
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText("确定要删除当前任务组吗？此操作不可撤销。");
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            new JobGroupService().deleteJobGroups(String.valueOf(currentTaskGroupId));
                            logger.info("✓ 任务组已删除");
                            dataManager.refreshTreeView();
                            canvas.clear();
                            pageStoreHelper.setCurrentPage(null);
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                NotificationToast.showError("✗ 删除任务组失败: " + e.getMessage());
                            });
                            logger.error("✗ 删除任务组失败: {}", e.getMessage());
                        }
                    }
                });
            }
            
            @Override
            public void onCopyJobGroup() {
                // TODO: 实现复制任务组
                logger.info("复制任务组功能开发中...");
            }
            
            // 窗口菜单
            @Override
            public void onMinimize() {
                Stage stage = (Stage) MainView.this.getScene().getWindow();
                stage.setIconified(true);
            }
            
            @Override
            public void onZoomWindow() {
                Stage stage = (Stage) MainView.this.getScene().getWindow();
                if (stage.isMaximized()) {
                    stage.setMaximized(false);
                } else {
                    stage.setMaximized(true);
                }
            }
            
            @Override
            public void onDetachTreeView() {
                detachPanel("任务组", treeView, () -> {
                    treeViewVisible = true;
                    updateLeftSidebar();
                });
            }
            
            @Override
            public void onDetachMiniMap() {
                detachPanel("小地图", miniMap, () -> {
                    miniMapVisible = true;
                    updateLeftSidebar();
                });
            }
            
            @Override
            public void onDetachLogPanel() {
                detachLogPanel();
            }
            
            @Override
            public void onRestoreAllPanels() {
                treeViewVisible = true;
                miniMapVisible = true;
                logPanelVisible = true;
                updateLeftSidebar();
                logger.info("✓ 已恢复所有面板");
            }
            
            @Override
            public void onSaveLayout() {
                // TODO: 实现保存布局
                logger.info("保存布局功能开发中...");
            }
            
            @Override
            public void onRestoreDefaultLayout() {
                onResetLayout();
            }
            
            // 帮助菜单
            @Override
            public void onUserManual() {
                // 打开用户手册（可以是本地文件或在线链接）
                logger.info("用户手册功能开发中...");
            }
            
            @Override
            public void onShortcutsList() {
                showShortcutsDialog();
            }
            
            @Override
            public void onApiDocumentation() {
                // 打开API文档
                logger.info("API文档功能开发中...");
            }
            
            @Override
            public void onChangelog() {
                // 显示更新日志
                showChangelogDialog();
            }
            
            @Override
            public void onAbout() {
                showAboutDialog();
            }
            
            @Override
            public void onCheckUpdate() {
                logger.info("检查更新功能开发中...");
            }
            
            @Override
            public void onReportIssue() {
                // 打开报告问题的链接或对话框
                logger.info("报告问题功能开发中...");
            }
            
            @Override
            public void onFeedback() {
                // 打开反馈建议的链接或对话框
                logger.info("反馈建议功能开发中...");
            }
            
            @Override
            public void onOnlineHelp() {
                // 打开在线帮助
                logger.info("在线帮助功能开发中...");
            }
        });

        // 主题变更监听：统一更新主 Scene 样式表并刷新小地图（菜单、系统设置等入口切换主题时都会触发）
        ThemeManager.getInstance().addOnThemeChanged(() -> {
            Scene scene = MainView.this.getScene();
            if (scene != null) {
                scene.getStylesheets().clear();
                String url = ThemeManager.getInstance().getStylesheetUrl();
                if (url != null && !url.isEmpty()) {
                    scene.getStylesheets().add(url);
                }
            }
            if (miniMap != null) miniMap.refresh();
        });

        // 导航栏回调
        navigationBar.setOnTaskSwitch((taskGroupName, taskGroupId) -> {
            // 切换任务组前，先保存当前任务组的数据（只在有未保存更改时）和滚动位置
            Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            Long resolvedTaskId = taskGroupId != null ? taskGroupId : findTaskGroupIdByName(taskGroupName);
            
            // 只有在真正切换到不同任务组时才保存当前任务组的滚动位置
            if (currentTaskGroupId != null && currentTaskGroupId != 0 
                    && resolvedTaskId != null && !currentTaskGroupId.equals(resolvedTaskId)) {
                if (canvas.hasUnsavedChanges()) {
                    dataManager.saveOrUpdateJob(currentTaskGroupId);
                }
                canvas.markAsSaved();
                // 保存当前任务组的滚动位置
                saveCurrentCanvasSize();
                saveCurrentScrollPosition();
            }
            
            if (resolvedTaskId != null) {
                taskGroupNameToIdMap.put(taskGroupName, resolvedTaskId);
                navigationBar.setCurrentTaskGroupId(resolvedTaskId);
                toolBar.setCurrentTaskGroupId(resolvedTaskId);
                pageStoreHelper.setCurrentPage(resolvedTaskId);
                // 数据加载后会自动恢复滚动位置
                restoreCanvasSize(resolvedTaskId);
                dataManager.loadTaskGroupData(resolvedTaskId, taskGroupName);
                
                Platform.runLater(() -> {
                    treeView.selectTaskGroupById(resolvedTaskId);
                });
            } else {
                NotificationToast.showWarning("⚠ 未找到任务组ID: " + taskGroupName);
                logger.warn("⚠ 未找到任务组ID: " + taskGroupName);
            }
        });

        navigationBar.setOnTaskClose((taskGroupId, taskGroupName) -> {
            Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            
            // 如果关闭的是当前显示的任务组，先保存数据再清空画布
            if (taskGroupId != null && taskGroupId.equals(currentTaskGroupId)) {
                // 保存当前任务组的数据（只在有未保存更改时）
                if (canvas.hasUnsavedChanges()) {
                    dataManager.saveOrUpdateJob(currentTaskGroupId);
                }
                canvas.markAsSaved();
                
                treeView.clearSelection();
                // 清空画布
                canvas.disableAutoSave();
                canvas.clear();
                canvas.enableAutoSave();
                // 清空当前页面状态
                pageStoreHelper.setCurrentPage(null);
                toolBar.setCurrentTaskGroupId(null);
                logger.info("📋 已关闭任务组: " + taskGroupName);
            }
            
            // 检查是否所有标签页都已关闭
            Long remainingTaskGroupId = navigationBar.getCurrentTaskGroupId();
            if (remainingTaskGroupId == null) {
                // 所有标签页都已关闭，确保画布是空的
                canvas.disableAutoSave();
                canvas.clear();
                canvas.enableAutoSave();
                pageStoreHelper.setCurrentPage(null);
                toolBar.setCurrentTaskGroupId(null);
                logger.info("📋 所有任务组已关闭");
            }
            
            taskGroupNameToIdMap.remove(taskGroupName);
        });

        // 画布回调
        canvas.setOnLog(logPanel::info);
        canvas.setOnNodeMoved(() -> {
            if (miniMap != null) miniMap.refresh();
        });
        
        // 设置框选模式改变回调
        canvas.setOnSelectionModeChanged(isActive -> {
            if (toolBar != null) {
                toolBar.updateSelectionButtonState(isActive);
            }
        });
        
        // 设置网格吸附状态改变回调
        canvas.setOnSnapToGridChanged(isActive -> {
            if (toolBar != null) {
                toolBar.updateSnapToGridButtonState(isActive);
            }
        });
        
        // 设置自动保存回调（鼠标移开画布时自动保存）
        canvas.setOnRequestSave(() -> {
            Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            if (currentTaskGroupId != null && currentTaskGroupId != 0) {
                // 异步保存，不阻塞主线程
                dataManager.saveOrUpdateJob(currentTaskGroupId);
                canvas.markAsSaved();
            }
        });
        
        canvas.setOnRequestAddNode(() -> {
            Long taskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            if (taskGroupId == null || taskGroupId == 0) {
                NotificationToast.showWarning("请先选择任务组");
                return;
            }
            String taskGroupName = getJobNameById(taskGroupId);
            dialogManager.showJobNodeDialog(taskGroupId, taskGroupName, null, () -> {
                // 数据加载后会自动配置节点回调
                restoreCanvasSize(taskGroupId);
                dataManager.loadTaskGroupData(taskGroupId, taskGroupName);
            });
        });

        canvas.setOnRequestCreateFromTemplate(() -> {
            Long taskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            if (taskGroupId == null || taskGroupId == 0) {
                NotificationToast.showWarning("请先选择任务组");
                return;
            }
            String taskGroupName = getJobNameById(taskGroupId);
            double[] pos = calculateNewNodePosition();
            dialogManager.showCreateNodeFromTemplateDialog(
                taskGroupId, taskGroupName, pos[0], pos[1],
                () -> {
                    restoreCanvasSize(taskGroupId);
                    dataManager.loadTaskGroupData(taskGroupId, taskGroupName);
                });
        });
        
        canvas.setOnRequestAddJobFromList(() -> {
            Long taskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            if (taskGroupId == null || taskGroupId == 0) {
                NotificationToast.showWarning("请先选择任务组");
                return;
            }
            String taskGroupName = getJobNameById(taskGroupId);
            double[] pos = calculateNewNodePosition();
            dialogManager.showAddJobFromListDialog(
                taskGroupId, taskGroupName, pos[0], pos[1],
                () -> restoreCanvasSize(taskGroupId));
        });
        
        canvas.setOnRequestRunTaskGroup(() -> {
            Long jobId = pageStoreHelper.getCurrentTaskGroupId();
            String jobName = getJobNameById(jobId);
            taskExecutionManager.triggerJobExecution(jobId, jobName);
        });
        
        // 设置创建条件节点回调
        canvas.setOnRequestAddConditionNode(() -> {
            Long taskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            if (taskGroupId == null || taskGroupId == 0) {
                NotificationToast.showWarning("请先选择任务组");
                return;
            }
            
            Stage ownerStage = (Stage) canvas.getScene().getWindow();
            ConditionNodeDialog dialog = new ConditionNodeDialog(ownerStage, null);
            Optional<ConditionNodeDialog.ConditionData> result = dialog.showAndWait();
            
            result.ifPresent(data -> {
                // 计算位置（在画布中心附近）
                double[] position = calculateNewConditionNodePosition();
                double x = position[0];
                double y = position[1];
                
                // 在后台线程中调用API创建条件节点
                new Thread(() -> {
                    try {
                        JobInfoService jobInfoService = new JobInfoService();
                        Map<String, Object> createResult = jobInfoService.createConditionNode(
                            taskGroupId,
                            data.getConditionName(),
                            data.getConditionExpression(),
                            data.getExpressionType() != null ? data.getExpressionType().toString() : null,
                            data.getConditionType() != null ? data.getConditionType().toString() : "IF",
                            x,
                            y
                        );
                        
                        // 在主线程中更新UI
                        Platform.runLater(() -> {
                            Long nodeId = parseToLong(createResult.get("nodeId"));
                            ConditionNode conditionNode = new ConditionNode(String.valueOf(nodeId), data.getConditionName(), data.getConditionType());
                            conditionNode.setConditionExpression(data.getConditionExpression());
                            conditionNode.setExpressionType(data.getExpressionType());
                            
                            // 设置大小改变回调，标记需要保存
                            conditionNode.setOnSizeChanged(() -> canvas.markAsUnsaved());
                            
                            // 设置位置
                            conditionNode.setLayoutX(x);
                            conditionNode.setLayoutY(y);
                            
                            // 添加到画布
                            canvas.addConditionNode(conditionNode);
                            canvas.markAsUnsaved();
                            logger.info("✓ 条件节点创建成功");
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            NotificationToast.showError("✗ 创建条件节点失败: " + e.getMessage());
                        });
                        logger.error("✗ 创建条件节点失败: {}", e.getMessage());
                    }
                }).start();
            });
        });
        
        // 设置条件节点编辑回调
        canvas.setOnEditConditionNode(conditionNode -> {
            Stage ownerStage = (Stage) canvas.getScene().getWindow();
                ConditionNodeDialog.ConditionData editData = new ConditionNodeDialog.ConditionData();
                editData.setConditionName(conditionNode.getConditionName());
                editData.setConditionType(conditionNode.getConditionType());
                editData.setConditionExpression(conditionNode.getConditionExpression());
                editData.setExpressionType(conditionNode.getExpressionType());
            
            ConditionNodeDialog dialog = new ConditionNodeDialog(ownerStage, editData);
            Optional<ConditionNodeDialog.ConditionData> result = dialog.showAndWait();
            
                result.ifPresent(data -> {
                    conditionNode.setConditionName(data.getConditionName());
                    conditionNode.setConditionType(data.getConditionType());
                    conditionNode.setConditionExpression(data.getConditionExpression());
                    conditionNode.setExpressionType(data.getExpressionType());
                    canvas.markAsUnsaved();
                    logger.info("✓ 条件节点已更新");
                });
        });
        
        // 设置条件节点删除回调
        canvas.setOnDeleteConditionNode(conditionNode -> {
            canvas.removeConditionNode(conditionNode);
            canvas.markAsUnsaved();
            logger.info("✓ 条件节点已删除");
        });

        // 树形视图回调
        setupTreeViewCallback();
        
        // 侧边栏恢复回调
        collapsedSidebar.setOnTreeViewRestore(() -> {
            treeViewVisible = true;
            updateLeftSidebar();
        });
        collapsedSidebar.setOnMiniMapRestore(() -> {
            miniMapVisible = true;
            updateLeftSidebar();
        });
        collapsedSidebar.setOnLogPanelRestore(() -> {
            logPanelVisible = true;
            updateLeftSidebar();
        });
        
        // 任务组面板关闭和弹出回调
        treeView.setOnClose(() -> {
            treeViewVisible = false;
            updateLeftSidebar();
        });
        treeView.setOnDetach(() -> {
            detachPanel("任务组", treeView, () -> {
                treeViewVisible = true;
                updateLeftSidebar();
            });
        });
        
        // 小地图面板关闭和弹出回调
        miniMap.setOnClose(() -> {
            miniMapVisible = false;
            updateLeftSidebar();
        });
        miniMap.setOnDetach(() -> {
            detachPanel("小地图", miniMap, () -> {
                miniMapVisible = true;
                updateLeftSidebar();
            });
        });
        
        // 日志面板关闭和弹出回调（使用专用方法）
        logPanel.setOnClose(() -> {
            logPanelVisible = false;
            updateLeftSidebar();
        });

        logPanel.setOnDetach(() -> {
            detachLogPanel();
        });
    }

    private void setupTreeViewCallback() {
        treeView.setSelectionCallback(new TaskTreeView.TaskSelectionCallback() {
            @Override
            public void onSuppressTreeSelectionOnce() {}

            @Override
            public void onTaskSelected(String taskName) {}

            @Override
            public void onTaskSelected(Long taskId, String taskName, Integer type) {
                if (type != null && type == 1 && taskId != null) {
                    // 切换任务组前，先保存当前任务组的数据和滚动位置
                    Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                    if (currentTaskGroupId != null && currentTaskGroupId != 0 && !currentTaskGroupId.equals(taskId)) {
                        if (canvas.hasUnsavedChanges()) {
                            dataManager.saveOrUpdateJob(currentTaskGroupId);
                            canvas.markAsSaved();
                        }
                        // 保存当前任务组的滚动位置
                        saveCurrentCanvasSize();
                        saveCurrentScrollPosition();
                    }
                    
                    taskGroupNameToIdMap.put(taskName, taskId);
                    pageStoreHelper.setCurrentPage(taskId);
                    navigationBar.addOrSelectTask(taskName, taskId);
                    toolBar.setCurrentTaskGroupId(taskId);
                    // 数据加载后会自动恢复滚动位置
                    restoreCanvasSize(taskId);
                    dataManager.loadTaskGroupData(taskId, taskName);
                    // 添加到最近打开的文件列表
                    com.cc.job.gui.util.RecentFilesManager.getInstance().addRecentFile(taskId, taskName);
                    toolBar.refreshRecentFilesMenu();
                }
            }

            @Override
            public void onNewJobGroup(Long partitionId, String partitionName) {
                dialogManager.showJobGroupDialog(partitionId, partitionName, null, () -> {
                    dataManager.refreshTreeView();
                });
            }

            @Override
            public void onNewJobNode(Long taskGroupId, String taskGroupName) {
                dialogManager.showJobNodeDialog(taskGroupId, taskGroupName, null, () -> {
                    // 数据加载后会自动配置节点回调
                    restoreCanvasSize(taskGroupId);
                    dataManager.loadTaskGroupData(taskGroupId, taskGroupName);
                });
            }

            @Override
            public void onJobNodeAction(Long jobNodeId, Long jobId, String nodeName, TaskTreeView.TaskSelectionCallback.JobNodeAction action) {
                onJobNodeAction(jobNodeId, jobId, nodeName, null, action);
            }

            @Override
            public void onJobNodeAction(Long jobNodeId, Long jobId, String nodeName, Long taskGroupId, TaskTreeView.TaskSelectionCallback.JobNodeAction action) {
                if (action == TaskTreeView.TaskSelectionCallback.JobNodeAction.EDIT) {
                    ProcessNode targetNode = canvas.getNodeByJobId(jobId);
                    nodeOperationManager.editNode(jobId, targetNode, taskGroupId);
                } else if (action == TaskTreeView.TaskSelectionCallback.JobNodeAction.LOCATE) {
                    // 定位节点：先检查是否需要切换任务组
                    locateNodeWithTaskGroupSwitch(jobId, nodeName, taskGroupId);
                } else if (action == TaskTreeView.TaskSelectionCallback.JobNodeAction.PROPERTIES) {
                    // 属性功能：显示节点详情对话框，与右键菜单的查看详情功能一致
                    ProcessNode targetNode = canvas.getNodeByJobId(jobId);
                    String nodeId = targetNode != null ? targetNode.getNodeId() : null;
                    if (dialogManager != null) {
                        Platform.runLater(() -> {
                            dialogManager.showNodeDetailsDialog(jobId, taskGroupId, nodeName, nodeId);
                        });
                    } else {
                        Platform.runLater(() -> {
                            NotificationToast.showWarning("⚠ 对话框管理器未设置，无法显示详情");
                        });
                        logger.warn("⚠ 对话框管理器未设置，无法显示详情");
                    }
                }
            }
            
            /**
             * 定位节点，如果需要则先切换到对应的任务组
             */
            private void locateNodeWithTaskGroupSwitch(Long jobId, String nodeName, Long targetTaskGroupId) {
                Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                
                // 如果目标任务组ID为空，尝试从当前画布查找节点
                if (targetTaskGroupId == null) {
                    ProcessNode targetNode = canvas.getNodeByJobId(jobId);
                    if (targetNode != null) {
                        // 节点在当前画布中，直接定位
                        canvas.locateNode(targetNode);
                        logger.info("📍 已定位到节点: " + nodeName);
                        return;
                    } else {
                        // 节点不在当前画布中，无法定位
                        NotificationToast.showWarning("⚠ 未找到节点: " + nodeName + "，请先打开对应的任务组");
                        logger.warn("⚠ 未找到节点: " + nodeName + "，请先打开对应的任务组");
                        return;
                    }
                }
                
                // 检查是否需要切换任务组
                if (currentTaskGroupId == null || !currentTaskGroupId.equals(targetTaskGroupId)) {
                    // 需要切换任务组
                    logger.info("🔄 切换到任务组: " + targetTaskGroupId);
                    
                    // 保存当前任务组的滚动位置
                    if (currentTaskGroupId != null && currentTaskGroupId != 0) {
                        saveCurrentCanvasSize();
                        saveCurrentScrollPosition();
                    }
                    
                    // 获取任务组名称
                    String taskGroupName = getJobNameById(targetTaskGroupId);
                    if (taskGroupName == null) {
                        taskGroupName = "任务组 " + targetTaskGroupId;
                    }
                    
                    // 切换到目标任务组
                    taskGroupNameToIdMap.put(taskGroupName, targetTaskGroupId);
                    pageStoreHelper.setCurrentPage(targetTaskGroupId);
                    navigationBar.addOrSelectTask(taskGroupName, targetTaskGroupId);
                    toolBar.setCurrentTaskGroupId(targetTaskGroupId);
                    restoreCanvasSize(targetTaskGroupId);
                    // 加载任务组数据，加载完成后定位节点（滚动位置会在数据加载完成后自动恢复）
                    dataManager.loadTaskGroupData(targetTaskGroupId, taskGroupName);
                    
                    // 延迟定位节点（等待数据加载完成）
                    Platform.runLater(() -> {
                        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                        delay.setOnFinished(e -> {
                            ProcessNode targetNode = canvas.getNodeByJobId(jobId);
                            if (targetNode != null) {
                                canvas.locateNode(targetNode);
                                logger.info("📍 已定位到节点: " + nodeName);
                            } else {
                                NotificationToast.showWarning("⚠ 节点加载后仍未找到: " + nodeName);
                                logger.warn("⚠ 节点加载后仍未找到: " + nodeName);
                            }
                        });
                        delay.play();
                    });
                } else {
                    // 当前任务组已匹配，直接定位
                    ProcessNode targetNode = canvas.getNodeByJobId(jobId);
                    if (targetNode != null) {
                        canvas.locateNode(targetNode);
                        logger.info("📍 已定位到节点: " + nodeName);
                    } else {
                        NotificationToast.showWarning("⚠ 未找到节点: " + nodeName);
                        logger.warn("⚠ 未找到节点: " + nodeName);
                    }
                }
            }

            @Override
            public void onEdgeAction(Long edgeId, TaskTreeView.TaskSelectionCallback.EdgeAction action) {
                if (action == TaskTreeView.TaskSelectionCallback.EdgeAction.LOCATE) {
                    // 定位连接线
                    if (canvas.locateConnectionByEdgeId(edgeId != null ? edgeId.toString() : null)) {
                        logger.info("📍 已定位到连接线");
                    } else {
                        NotificationToast.showWarning("⚠ 未找到连接线");
                        logger.warn("⚠ 未找到连接线");
                    }
                }
            }

            @Override
            public void onEdgeAction(Long edgeId, Long taskGroupId, TaskTreeView.TaskSelectionCallback.EdgeAction action) {
                if (action == TaskTreeView.TaskSelectionCallback.EdgeAction.LOCATE) {
                    // 定位连接线：先检查是否需要切换任务组
                    locateEdgeWithTaskGroupSwitch(edgeId, taskGroupId);
                }
            }
            
            /**
             * 定位连接线，如果需要则先切换到对应的任务组
             */
            private void locateEdgeWithTaskGroupSwitch(Long edgeId, Long targetTaskGroupId) {
                Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                
                // 如果目标任务组ID为空，尝试在当前画布中查找连接线
                if (targetTaskGroupId == null) {
                    if (canvas.locateConnectionByEdgeId(edgeId != null ? edgeId.toString() : null)) {
                        logger.info("📍 已定位到连接线");
                        return;
                    } else {
                        NotificationToast.showWarning("⚠ 未找到连接线，请先打开对应的任务组");
                        logger.warn("⚠ 未找到连接线，请先打开对应的任务组");
                        return;
                    }
                }
                
                // 检查是否需要切换任务组
                if (currentTaskGroupId == null || !currentTaskGroupId.equals(targetTaskGroupId)) {
                    // 需要切换任务组
                    logger.info("🔄 切换到任务组: " + targetTaskGroupId);
                    
                    // 保存当前任务组的滚动位置
                    if (currentTaskGroupId != null && currentTaskGroupId != 0) {
                        saveCurrentCanvasSize();
                        saveCurrentScrollPosition();
                    }
                    
                    // 获取任务组名称
                    String taskGroupName = getJobNameById(targetTaskGroupId);
                    if (taskGroupName == null) {
                        taskGroupName = "任务组 " + targetTaskGroupId;
                    }
                    
                    // 切换到目标任务组
                    taskGroupNameToIdMap.put(taskGroupName, targetTaskGroupId);
                    pageStoreHelper.setCurrentPage(targetTaskGroupId);
                    navigationBar.addOrSelectTask(taskGroupName, targetTaskGroupId);
                    toolBar.setCurrentTaskGroupId(targetTaskGroupId);
                    restoreCanvasSize(targetTaskGroupId);
                    // 加载任务组数据，加载完成后定位连接线（滚动位置会在数据加载完成后自动恢复）
                    dataManager.loadTaskGroupData(targetTaskGroupId, taskGroupName);
                    
                    // 延迟定位连接线（等待数据加载完成）
                    Platform.runLater(() -> {
                        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                        delay.setOnFinished(e -> {
                            if (canvas.locateConnectionByEdgeId(edgeId != null ? edgeId.toString() : null)) {
                                logger.info("📍 已定位到连接线");
                            } else {
                                NotificationToast.showWarning("⚠ 连接线加载后仍未找到");
                                logger.warn("⚠ 连接线加载后仍未找到");
                            }
                        });
                        delay.play();
                    });
                } else {
                    // 当前任务组已匹配，直接定位
                    if (canvas.locateConnectionByEdgeId(edgeId != null ? edgeId.toString() : null)) {
                        logger.info("📍 已定位到连接线");
                    } else {
                        NotificationToast.showWarning("⚠ 未找到连接线");
                        logger.warn("⚠ 未找到连接线");
                    }
                }
            }

            @Override
            public void onPartitionAction(Long partitionId, String partitionName, TaskTreeView.TaskSelectionCallback.PartitionAction action) {
                if (action == TaskTreeView.TaskSelectionCallback.PartitionAction.EDIT) {
                    // 编辑分区：打开分区编辑对话框
                    dialogManager.showEditPartitionDialog(partitionId, partitionName, () -> {
                        // 编辑成功后刷新树视图
                        dataManager.refreshTreeView();
                    });
                } else if (action == TaskTreeView.TaskSelectionCallback.PartitionAction.EXPORT) {
                    // 导出分区数据
                    exportPartitionData(partitionId, partitionName);
                } else if (action == TaskTreeView.TaskSelectionCallback.PartitionAction.IMPORT) {
                    // 导入任务组到该分区（与顶部导入任务组一致）
                    importTaskGroupFile(partitionId);
                }
            }

            @Override
            public void onExportTaskGroup(Long taskGroupId, String taskGroupName) {
                exportTaskGroup(taskGroupId);
            }

            @Override
            public void onJobGroupEdit(Long taskGroupId, String taskGroupName) {
                // 编辑任务组：加载任务组数据并弹出编辑对话框
                if (taskGroupId != null) {
                    new Thread(() -> {
                        try {
                            // 使用getJobNodeFormData获取任务组的表单数据
                            JobInfoForm formData = new JobInfoService().getJobNodeFormData(taskGroupId);
                            Platform.runLater(() -> {
                                // 获取任务组所属的分区ID
                                Long partitionId = formData.getJobPartId() != null ? formData.getJobPartId().longValue() : null;
                                // 弹出编辑对话框
                                dialogManager.showJobGroupDialog(partitionId, taskGroupName, formData, () -> {
                                    // 编辑成功后刷新树视图
                                    dataManager.refreshTreeView();
                                });
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                NotificationToast.showError("✗ 加载任务组数据失败: " + e.getMessage());
                            });
                            logger.error("✗ 加载任务组数据失败: {}", e.getMessage());
                        }
                    }).start();
                }
            }
        });
    }

    private void setupKeyboardShortcuts() {
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.C) {
                        handleCopyShortcut();
                        event.consume();
                    } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.V) {
                        handlePasteShortcut();
                        event.consume();
                    } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.F) {
                        // Ctrl+F: 查找节点
                        Stage ownerStage = (Stage) MainView.this.getScene().getWindow();
                        NodeSearchDialog searchDialog = new NodeSearchDialog(
                            ownerStage,
                            canvas.getNodes(),
                            node -> {
                                canvas.locateNode(node);
                                logger.info("✓ 已定位到节点: " + node.getJobHandlerName());
                            }
                        );
                        searchDialog.showAndWait();
                        event.consume();
                    }
                });
            }
        });
    }

    private void handleCopyShortcut() {
        Set<ProcessNode> selectedNodes = canvas.getSelectedNodes();
        Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
        
        if (selectedNodes.isEmpty()) {
            NotificationToast.showWarning("⚠ 请先选中要复制的节点");
            logger.warn("⚠ 请先选中要复制的节点");
            return;
        }
        
        if (selectedNodes.size() == 1) {
            nodeOperationManager.copyNodeToClipboard(selectedNodes.iterator().next(), currentTaskGroupId);
        } else {
            nodeOperationManager.copyNodesToClipboard(selectedNodes, currentTaskGroupId);
        }
    }

    private void handlePasteShortcut() {
        Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
        if (currentTaskGroupId == null) {
            NotificationToast.showWarning("请先选择任务组");
            return;
        }
        nodeOperationManager.pasteNodes(currentTaskGroupId);
    }

    private void zoomCanvas(double newZoom) {
        newZoom = Math.max(0.25, Math.min(3.0, newZoom));
        if (newZoom == currentZoom) return;
        
        Scale scale = new Scale(newZoom, newZoom, 0, 0);
        canvas.getTransforms().clear();
        canvas.getTransforms().add(scale);
        
        currentZoom = newZoom;
        toolBar.updateZoomLevel(newZoom);
    }

    private void updateUndoRedoButtons() {
        boolean canUndo = undoRedoManager != null && undoRedoManager.canUndo();
        boolean canRedo = undoRedoManager != null && undoRedoManager.canRedo();
        Platform.runLater(() -> {
            if (toolBar != null) {
                toolBar.updateUndoRedoState(canUndo, canRedo);
            }
        });
    }

    private void updateLeftSidebar() {
        treeView.setVisible(treeViewVisible);
        treeView.setManaged(treeViewVisible);
        miniMap.setVisible(miniMapVisible);
        miniMap.setManaged(miniMapVisible);
        logPanel.setVisible(logPanelVisible);
        logPanel.setManaged(logPanelVisible);
        
        boolean anyVisible = treeViewVisible || miniMapVisible;
        leftArea.setVisible(anyVisible);
        leftArea.setManaged(anyVisible);
        
        collapsedSidebar.showTreeViewButton(!treeViewVisible);
        collapsedSidebar.showMiniMapButton(!miniMapVisible);
        collapsedSidebar.showLogPanelButton(!logPanelVisible);
        
        Platform.runLater(() -> {
            if (!anyVisible) {
                horizontalSplit.setDividerPositions(0);
            } else {
                double[] currentPositions = horizontalSplit.getDividerPositions();
                if (currentPositions.length > 0 && currentPositions[0] == 0.0) {
                    horizontalSplit.setDividerPositions(0.2);
                }
            }
            
            // 处理左侧区域的分隔线位置
            if (treeViewVisible && miniMapVisible) {
                // 两个都可见，恢复默认位置
                double[] currentPositions = leftArea.getDividerPositions();
                if (currentPositions.length == 0 || currentPositions[0] <= 0.0 || currentPositions[0] >= 1.0) {
                    leftArea.setDividerPositions(0.7);
                }
            } else if (!treeViewVisible && miniMapVisible) {
                // 只有小地图可见，分隔线移到顶部（小地图占满）
                leftArea.setDividerPositions(0.0);
            } else if (treeViewVisible && !miniMapVisible) {
                // 只有树形菜单可见，分隔线移到底部（树形菜单占满）
                leftArea.setDividerPositions(1.0);
            }
            
            if (!logPanelVisible) {
                verticalSplit.setDividerPositions(1.0);
            } else {
                double[] currentPositions = verticalSplit.getDividerPositions();
                if (currentPositions.length > 0 && currentPositions[0] == 1.0) {
                    verticalSplit.setDividerPositions(0.7);
                }
            }
        });
    }

    /**
     * 弹出面板为独立窗口
     * @param title 窗口标题
     * @param panel 要弹出的面板
     * @param onWindowClosed 窗口关闭时的回调
     */
    private void detachPanel(String title, Node panel, Runnable onWindowClosed) {
        // 注意：LogPanel 使用专用的 detachLogPanel 方法
        if (panel instanceof LogPanel) {
            detachLogPanel();
            return;
        }
        
        // 先更新状态标志（在移除面板之前）
        if (panel instanceof TaskTreeView) {
            treeViewVisible = false;
        } else if (panel instanceof MiniMapView) {
            miniMapVisible = false;
        }
        
        // 从原容器中移除面板
        Parent parent = panel.getParent();
        if (parent instanceof Pane) {
            ((Pane) parent).getChildren().remove(panel);
        } else if (parent instanceof SplitPane) {
            ((SplitPane) parent).getItems().remove(panel);
        }
        
        // 更新侧边栏显示
        updateDetachedSidebarState();
        
        // 确保面板在弹出窗口中可见
        panel.setVisible(true);
        panel.setManaged(true);
        
        // 隐藏面板内部的弹出和关闭按钮
        if (panel instanceof TaskTreeView) {
            ((TaskTreeView) panel).setDetachButtonsVisible(false);
        } else if (panel instanceof MiniMapView) {
            ((MiniMapView) panel).setDetachButtonsVisible(false);
        }
        
        // 创建新窗口
        Stage detachedStage = new Stage();
        detachedStage.setTitle(title);
        detachedStage.initOwner(this.getScene().getWindow());
        detachedStage.initModality(Modality.NONE);
        
        // 设置窗口内容
        VBox container = new VBox();
        container.getStyleClass().add("detached-panel-root");
        container.getChildren().add(panel);
        VBox.setVgrow(panel, Priority.ALWAYS);
        
        // 设置窗口大小
        Scene scene = new Scene(container, 400, 500);
        scene.getStylesheets().add(com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl());
        detachedStage.setScene(scene);
        
        // 窗口关闭时的处理
        detachedStage.setOnHidden(event -> {
            // 从弹出窗口中移除面板
            if (container.getChildren().contains(panel)) {
                container.getChildren().remove(panel);
            }
            VBox.clearConstraints(panel);
            
            // 确保面板可见
            panel.setVisible(true);
            panel.setManaged(true);
            
            // 恢复面板内部的弹出和关闭按钮显示
            if (panel instanceof TaskTreeView) {
                ((TaskTreeView) panel).setDetachButtonsVisible(true);
            } else if (panel instanceof MiniMapView) {
                ((MiniMapView) panel).setDetachButtonsVisible(true);
            }
            
            // 将面板添加回原容器
            // 注意：即使面板已经在 leftArea 中，我们也需要先移除再添加
            // 因为面板的父节点可能还是弹出窗口的容器，而不是 leftArea
            boolean wasInContainer = leftArea.getItems().contains(panel);
            if (wasInContainer) {
                leftArea.getItems().remove(panel);
            }
            
            // 先恢复状态标志，避免面板的 onClose 回调干扰
            if (panel instanceof TaskTreeView) {
                treeViewVisible = true;
                leftArea.getItems().add(0, treeView);
            } else if (panel instanceof MiniMapView) {
                miniMapVisible = true;
                leftArea.getItems().add(miniMap);
            }
            
            // 立即更新侧边栏，确保状态正确恢复
            updateLeftSidebar();
            
            // 执行回调（回调中也会调用 updateLeftSidebar，但这是安全的，因为状态已经正确）
            if (onWindowClosed != null) {
                onWindowClosed.run();
            }
        });
        
        detachedStage.show();
    }
    
    /**
     * 专用方法：弹出日志面板为独立窗口
     * 与其他面板不同，LogPanel 本身不移动，只移动内部的所有子节点
     */
    private void detachLogPanel() {
        
        // 隐藏弹出和关闭按钮
        logPanel.setDetachButtonsVisible(false);
        
        // 创建新窗口
        Stage detachedStage = new Stage();
        detachedStage.setTitle("监控");
        detachedStage.initOwner(this.getScene().getWindow());
        detachedStage.initModality(Modality.NONE);
        
        // 创建容器（不需要 padding，因为子节点已经有自己的样式）
        VBox container = new VBox();
        container.getStyleClass().add("detached-panel-root");
        
        // 将日志面板的所有子节点移动到弹出窗口
        logPanel.detachContent(container);
        
        // 隐藏主界面的日志面板（但不移除）
        logPanelVisible = false;
        updateLeftSidebar();
        
        Scene logScene = new Scene(container, 900, 500);
        logScene.getStylesheets().add(com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl());
        detachedStage.setScene(logScene);
        
        // 窗口关闭时的处理
        detachedStage.setOnHidden(event -> {
            
            // 将内容移回主面板
            logPanel.restoreContentFromDetach(container);
            
            // 恢复按钮显示
            logPanel.setDetachButtonsVisible(true);
            
            // 显示日志面板
            logPanelVisible = true;
            updateLeftSidebar();
        });
        
        detachedStage.show();
    }
    
    /**
     * 递归设置节点及其所有子节点的可见性和管理状态
     */
    private void setNodeVisibleAndManaged(javafx.scene.Node node, boolean visible) {
        if (node == null) return;
        
        // 检查 visible 属性是否是绑定的，如果是绑定的则不能直接设置
        try {
            if (!node.visibleProperty().isBound()) {
                node.setVisible(visible);
            }
        } catch (Exception e) {
            // 如果设置失败，忽略（可能是某些特殊节点）
        }
        
        // 检查 managed 属性是否是绑定的
        try {
            if (!node.managedProperty().isBound()) {
                node.setManaged(visible);
            }
        } catch (Exception e) {
            // 如果设置失败，忽略
        }
        
        if (node instanceof Parent) {
            Parent parent = (Parent) node;
            for (Node child : parent.getChildrenUnmodifiable()) {
                setNodeVisibleAndManaged(child, visible);
            }
        }
    }
    
    /**
     * 更新弹出面板后的侧边栏状态（不影响已弹出的面板）
     */
    private void updateDetachedSidebarState() {
        // 更新折叠侧边栏按钮显示
        collapsedSidebar.showTreeViewButton(!treeViewVisible);
        collapsedSidebar.showMiniMapButton(!miniMapVisible);
        collapsedSidebar.showLogPanelButton(!logPanelVisible);
        
        // 更新左侧区域可见性
        boolean anyLeftVisible = treeViewVisible || miniMapVisible;
        leftArea.setVisible(anyLeftVisible);
        leftArea.setManaged(anyLeftVisible);
        
        Platform.runLater(() -> {
            // 调整水平分割面板
            if (!anyLeftVisible) {
                horizontalSplit.setDividerPositions(0);
            } else {
                double[] currentPositions = horizontalSplit.getDividerPositions();
                if (currentPositions.length > 0 && currentPositions[0] == 0.0) {
                    horizontalSplit.setDividerPositions(0.2);
                }
            }
            
            // 调整左侧区域的分隔线位置，使剩余面板占据全部空间
            if (treeViewVisible && miniMapVisible) {
                // 两个都可见，保持当前分隔线位置（不强制改变）
                // 如果分隔线位置异常，才恢复默认位置
                double[] currentPositions = leftArea.getDividerPositions();
                if (currentPositions.length == 0 || currentPositions[0] <= 0.0 || currentPositions[0] >= 1.0) {
                    leftArea.setDividerPositions(0.7);
                }
            } else if (!treeViewVisible && miniMapVisible) {
                // 只有小地图可见，分隔线移到顶部（小地图占满）
                leftArea.setDividerPositions(0.0);
            } else if (treeViewVisible && !miniMapVisible) {
                // 只有树形菜单可见，分隔线移到底部（树形菜单占满）
                leftArea.setDividerPositions(1.0);
            }
            
            // 调整垂直分割面板
            if (!logPanelVisible) {
                verticalSplit.setDividerPositions(1.0);
            } else {
                double[] currentPositions = verticalSplit.getDividerPositions();
                if (currentPositions.length > 0 && currentPositions[0] == 1.0) {
                    verticalSplit.setDividerPositions(0.7);
                }
            }
        });
    }

    private Long findTaskGroupIdByName(String taskGroupName) {
        if (taskGroupName == null || taskGroupName.isEmpty()) return null;
        return treeView.findTaskGroupIdByName(taskGroupName);
    }

    private String getJobNameById(Long jobId) {
        String name = navigationBar.getTaskNameById(jobId);
        if (name != null) return name;
        
        for (Map.Entry<String, Long> entry : taskGroupNameToIdMap.entrySet()) {
            if (entry.getValue().equals(jobId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public NodeCanvas getCanvas() {
        return canvas;
    }

    public TaskTreeView getTreeView() {
        return treeView;
    }

    public LogPanel getLogPanel() {
        return logPanel;
    }

    public void cleanup() {
        taskExecutionManager.cleanup();
    }
    
    /**
     * 导出分区数据
     * @param partitionId 分区ID
     * @param partitionName 分区名称
     */
    private void exportPartitionData(Long partitionId, String partitionName) {
        if (partitionId == null) {
            NotificationToast.showWarning("⚠ 分区ID无效");
            logger.warn("⚠ 分区ID无效");
            return;
        }
        
        Stage ownerStage = (Stage) this.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出分区数据");
        fileChooser.setInitialFileName(partitionName != null ? partitionName: "partition_" + partitionId);
        
        // 设置文件过滤器
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CE文件 (*.ce)", "*.ce");
        fileChooser.getExtensionFilters().add(extFilter);
        
        File file = fileChooser.showSaveDialog(ownerStage);
        if (file == null) {
            return; // 用户取消了保存
        }
        
        logger.info("📤 开始导出分区数据: " + partitionName);
        
        new Thread(() -> {
            try {
                JobPartService jobPartService = new JobPartService();
                byte[] data = jobPartService.exportData(partitionId);
                
                // 保存文件
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(data);
                    fos.flush();
                }
                
                Platform.runLater(() -> {
                    NotificationToast.showSuccess("分区数据已导出到: " + file.getAbsolutePath());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationToast.showError("导出分区数据时发生错误: " + e.getMessage());
                });
            }
        }, "export-partition").start();
    }
    
    /**
     * 导入分区文件
     */
    private void importPartitionFile() {
        Stage ownerStage = (Stage) this.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导入分区数据");
        
        // 设置文件过滤器
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CE文件 (*.ce)", "*.ce");
        fileChooser.getExtensionFilters().add(extFilter);
        
        File file = fileChooser.showOpenDialog(ownerStage);
        if (file == null) {
            return; // 用户取消了选择
        }
        
        if (!file.exists() || !file.isFile()) {
            NotificationToast.showError("选择的文件不存在或无效");
            return;
        }
        
        logger.info("📥 开始导入分区数据: " + file.getName());
        
        new Thread(() -> {
            try {
                JobPartService jobPartService = new JobPartService();
                boolean success = jobPartService.importData(file);
                
                Platform.runLater(() -> {
                    if (success) {
                        NotificationToast.showSuccess("分区数据已成功导入");
                        // 刷新树形视图
                        dataManager.refreshTreeView();
                    } else {
                        NotificationToast.showError("导入分区数据失败，请检查文件格式是否正确");
                    }
                });
            } catch (Exception e) {
                logger.error("导入分区数据失败", e);
                Platform.runLater(() -> {
                    NotificationToast.showError("导入分区数据时发生错误: " + e.getMessage());
                });
            }
        }, "import-partition").start();
    }

    /**
     * 显示选择分区对话框（用于导入任务组时未选中分区）
     */
    private Optional<Long> showSelectPartitionDialog() {
        Stage ownerStage = (Stage) getScene().getWindow();
        Dialog<Long> dialog = new Dialog<>();
        dialog.initOwner(ownerStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("选择分区");
        dialog.setHeaderText("请选择要导入任务组的目标分区");

        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, okButtonType);

        ComboBox<com.cc.job.xo.model.vo.JobPartVo> partCombo = new ComboBox<>();
        partCombo.setPromptText("请选择分区");
        partCombo.setPrefWidth(320);
        partCombo.setCellFactory(cb -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(com.cc.job.xo.model.vo.JobPartVo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });
        partCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(com.cc.job.xo.model.vo.JobPartVo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(new Label("分区"), partCombo);
        dialog.getDialogPane().setContent(content);
        String themeCss = ThemeManager.getInstance().getStylesheetUrl();
        if (themeCss != null && !themeCss.isEmpty()) {
            dialog.getDialogPane().getStylesheets().add(themeCss);
        }
        try {
            List<com.cc.job.xo.model.vo.JobPartVo> tree = new JobPartService().getTree();
            partCombo.getItems().setAll(tree != null ? tree : Collections.emptyList());
            if (partCombo.getItems().isEmpty()) {
                partCombo.setDisable(true);
            } else {
                partCombo.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            logger.error("加载分区列表失败", e);
            NotificationToast.showError("加载分区列表失败: " + e.getMessage());
            return Optional.empty();
        }

        dialog.setResultConverter(bt -> {
            if (bt == okButtonType) {
                com.cc.job.xo.model.vo.JobPartVo selected = partCombo.getValue();
                return selected != null ? selected.getId() : null;
            }
            return null;
        });
        return dialog.showAndWait().filter(Objects::nonNull);
    }

    /**
     * 导入任务组文件到指定分区
     */
    private void importTaskGroupFile(Long partitionId) {
        Stage ownerStage = (Stage) getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导入任务组");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CEL文件 (*.cel)", "*.cel");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showOpenDialog(ownerStage);
        if (file == null) {
            return;
        }
        if (!file.exists() || !file.isFile()) {
            NotificationToast.showError("选择的文件不存在或无效");
            return;
        }

        logger.info("📥 开始导入任务组到分区: " + partitionId);
        new Thread(() -> {
            try {
                JobPartService jobPartService = new JobPartService();
                boolean success = jobPartService.importTaskGroup(partitionId, file);
                Platform.runLater(() -> {
                    if (success) {
                        NotificationToast.showSuccess("任务组已成功导入");
                        dataManager.refreshTreeView();
                    } else {
                        NotificationToast.showError("导入任务组失败，请检查文件格式是否正确");
                    }
                });
            } catch (Exception e) {
                logger.error("导入任务组失败", e);
                Platform.runLater(() ->
                    NotificationToast.showError("导入任务组时发生错误: " + e.getMessage()));
            }
        }, "import-taskgroup").start();
    }
    
    /**
     * 计算新条件节点的位置
     */
    private double[] calculateNewConditionNodePosition() {
        List<ConditionNode> existingConditionNodes = canvas.getConditionNodes();
        if (existingConditionNodes == null || existingConditionNodes.isEmpty()) {
            return new double[]{200, 200};
        }
        
        int nodeCount = existingConditionNodes.size();
        int row = nodeCount / 3;
        int col = nodeCount % 3;
        
        double x = 200 + col * 360;
        double y = 200 + row * 240;
        
        return new double[]{x, y};
    }

    /**
     * 计算新节点放置位置（用于从模板创建节点等）
     */
    private double[] calculateNewNodePosition() {
        return new double[]{200, 200};
    }
    
    /**
     * 安全地将对象转换为 Long 类型
     * 处理可能包含小数点的数字字符串（如 "25850.0"）
     */
    private Long parseToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String str = String.valueOf(value);
        // 如果包含小数点，先转换为 Double 再转换为 Long
        if (str.contains(".")) {
            return (long) Double.parseDouble(str);
        }
        return Long.parseLong(str);
    }
    
    /**
     * 显示节点历史对话框
     */
    private void showNodeHistoryDialog(Long taskGroupId, String taskGroupName) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("节点执行历史 - " + taskGroupName);
        
        NodeHistoryView historyView = new NodeHistoryView(taskGroupId, taskGroupName);
        historyView.getStyleClass().add("popup-window-root");
        
        Scene scene = new Scene(historyView, 1000, 600);
        scene.getStylesheets().add(com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl());
        dialog.setScene(scene);
        dialog.show();
        
        logger.info("✓ 打开节点历史页面: " + taskGroupName);
    }

    // 页面状态管理辅助类
    private static class PageStoreHelper {
        private Long currentPage = 0L;

        public void setCurrentPage(Long pageId) {
            this.currentPage = pageId;
        }

        public Long getCurrentPage() {
            return this.currentPage;
        }

        public Long getCurrentTaskGroupId() {
            return this.currentPage;
        }
    }
    
    // 滚动位置存储类
    private static class ScrollPosition {
        double hvalue;
        double vvalue;
        
        ScrollPosition(double hvalue, double vvalue) {
            this.hvalue = hvalue;
            this.vvalue = vvalue;
        }
    }

    private static class CanvasSize {
        final double width;
        final double height;

        CanvasSize(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * 保存当前任务组的画布尺寸
     */
    private void saveCurrentCanvasSize() {
        Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
        if (currentTaskGroupId != null && currentTaskGroupId != 0 && canvas != null) {
            double w = canvas.getPrefWidth();
            double h = canvas.getPrefHeight();
            if (w > 0 && h > 0) {
                taskGroupCanvasSizes.put(currentTaskGroupId, new CanvasSize(w, h));
                logger.debug("保存任务组 {} 的画布尺寸: {} x {}", currentTaskGroupId, w, h);
            }
        }
    }

    /**
     * 恢复指定任务组的画布尺寸；若无保存的尺寸则不做（首次进入由 onDataLoaded 中扩展）。
     * 恢复时不低于当前视口，避免放大窗口后再切换时画布小于视口导致右侧和下方空白。
     */
    private void restoreCanvasSize(Long taskGroupId) {
        if (canvas == null || taskGroupId == null || taskGroupId == 0) return;
        CanvasSize size = taskGroupCanvasSizes.get(taskGroupId);
        if (size != null && size.width > 0 && size.height > 0) {
            double w = size.width;
            double h = size.height;
            if (scrollPane != null) {
                double viewportW = scrollPane.getViewportBounds().getWidth();
                double viewportH = scrollPane.getViewportBounds().getHeight();
                if (viewportW > 0 && viewportH > 0) {
                    w = Math.max(w, viewportW);
                    h = Math.max(h, viewportH);
                }
            }
            canvas.setPrefSize(w, h);
            canvas.setMinSize(w, h);
            logger.debug("恢复任务组 {} 的画布尺寸: {} x {}", taskGroupId, w, h);
        }
    }

    private static final double FIRST_ENTER_CANVAS_MARGIN = 150.0;

    /** 画布面积超过「内容+边距」面积的该倍数时才收缩，避免频繁微调 */
    private static final double SHRINK_CANVAS_AREA_RATIO_THRESHOLD = 1.2;

    /**
     * 若当前画布尺寸明显大于当前任务组内容的包围盒，则收缩到「内容+边距」与视口取大，并保存尺寸与滚动位置，
     * 避免恢复出过大的画布导致右侧和下方出现大片空白。
     */
    private void shrinkCanvasToFitContentIfNeeded(Long taskGroupId) {
        if (scrollPane == null || canvas == null || taskGroupId == null || taskGroupId == 0) return;
        Bounds bounds = canvas.getContentBounds();
        if (bounds == null) return;
        double contentRight = bounds.getMaxX() + FIRST_ENTER_CANVAS_MARGIN;
        double contentBottom = bounds.getMaxY() + FIRST_ENTER_CANVAS_MARGIN;
        double viewportW = scrollPane.getViewportBounds().getWidth();
        double viewportH = scrollPane.getViewportBounds().getHeight();
        double targetW = Math.max(contentRight, viewportW);
        double targetH = Math.max(contentBottom, viewportH);
        double currentW = canvas.getPrefWidth();
        double currentH = canvas.getPrefHeight();
        double targetArea = targetW * targetH;
        double currentArea = currentW * currentH;
        if (targetArea <= 0 || currentArea < targetArea * SHRINK_CANVAS_AREA_RATIO_THRESHOLD) return;
        canvas.setPrefSize(targetW, targetH);
        canvas.setMinSize(targetW, targetH);
        saveCurrentScrollPosition();
        saveCurrentCanvasSize();
        logger.debug("任务组 {} 画布已收缩至贴合内容: {}x{}", taskGroupId, targetW, targetH);
    }

    /**
     * 首次进入任务组时：根据内容扩展画布并将内容中心对准视口中心，然后保存状态。
     */
    private void expandCanvasToFitContentAndCenter(Long taskGroupId) {
        if (scrollPane == null || canvas == null || taskGroupId == null || taskGroupId == 0) return;
        Bounds bounds = canvas.getContentBounds();
        if (bounds == null) {
            scrollPane.setHvalue(0.5);
            scrollPane.setVvalue(0.5);
            saveCurrentScrollPosition();
            saveCurrentCanvasSize();
            logger.debug("任务组 {} 首次加载无内容，设置滚动居中并保存画布尺寸", taskGroupId);
            return;
        }
        double viewportW = scrollPane.getViewportBounds().getWidth();
        double viewportH = scrollPane.getViewportBounds().getHeight();
        // 按内容在画布上的实际右、下边界扩展，保证可滚动看到所有节点
        double newWidth = Math.max(bounds.getMaxX() + FIRST_ENTER_CANVAS_MARGIN, viewportW);
        double newHeight = Math.max(bounds.getMaxY() + FIRST_ENTER_CANVAS_MARGIN, viewportH);
        canvas.setPrefSize(newWidth, newHeight);
        canvas.setMinSize(newWidth, newHeight);
        double contentCenterX = bounds.getMinX() + bounds.getWidth() / 2;
        double contentCenterY = bounds.getMinY() + bounds.getHeight() / 2;
        double canvasW = canvas.getPrefWidth();
        double canvasH = canvas.getPrefHeight();
        double scrollableW = canvasW - viewportW;
        double scrollableH = canvasH - viewportH;
        double hvalue = scrollableW > 0 ? Math.max(0, Math.min(1, (contentCenterX - viewportW / 2) / scrollableW)) : 0.5;
        double vvalue = scrollableH > 0 ? Math.max(0, Math.min(1, (contentCenterY - viewportH / 2) / scrollableH)) : 0.5;
        scrollPane.setHvalue(hvalue);
        scrollPane.setVvalue(vvalue);
        saveCurrentScrollPosition();
        saveCurrentCanvasSize();
        logger.debug("任务组 {} 首次加载已扩展画布并居中: 画布 {}x{}, 滚动 h={}, v={}", taskGroupId, canvasW, canvasH, hvalue, vvalue);
    }
    
    /**
     * 保存当前任务组的滚动位置
     */
    private void saveCurrentScrollPosition() {
        if (scrollPane == null) return;
        
        Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
        if (currentTaskGroupId != null && currentTaskGroupId != 0) {
            double hvalue = scrollPane.getHvalue();
            double vvalue = scrollPane.getVvalue();
            double canvasWidth = canvas.getPrefWidth();
            double canvasHeight = canvas.getPrefHeight();
            double viewportWidth = scrollPane.getViewportBounds().getWidth();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();
            
            taskGroupScrollPositions.put(currentTaskGroupId, new ScrollPosition(hvalue, vvalue));
            logger.debug("保存任务组 {} 的滚动位置: hvalue={}, vvalue={}", currentTaskGroupId, hvalue, vvalue);
        }
    }
    
    /**
     * 恢复指定任务组的滚动位置
     * 如果任务组没有保存的位置，则设置为居中（0.5, 0.5）
     */
    private void restoreScrollPosition(Long taskGroupId) {
        if (scrollPane == null || taskGroupId == null || taskGroupId == 0) return;
        
        ScrollPosition savedPosition = taskGroupScrollPositions.get(taskGroupId);
        
        // 立即设置滚动位置（如果已保存），避免延迟导致的视觉跳转
        if (savedPosition != null) {
            double targetH = savedPosition.hvalue;
            double targetV = savedPosition.vvalue;
            scrollPane.setHvalue(targetH);
            scrollPane.setVvalue(targetV);
            logger.debug("立即恢复任务组 {} 的滚动位置: hvalue={}, vvalue={}", taskGroupId, targetH, targetV);
        } else {
            // 首次加载，立即设置为居中
            scrollPane.setHvalue(0.5);
            scrollPane.setVvalue(0.5);
            taskGroupScrollPositions.put(taskGroupId, new ScrollPosition(0.5, 0.5));
            logger.debug("任务组 {} 首次加载，立即设置滚动位置为居中", taskGroupId);
        }
        
        // 使用Platform.runLater确保在UI更新后验证和修正滚动位置
        Platform.runLater(() -> {
            // 使用较短的延迟验证滚动位置（减少到50ms，减少视觉延迟）
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(50));
            delay.setOnFinished(e -> {
                ScrollPosition position = taskGroupScrollPositions.get(taskGroupId);
                
                double canvasWidth = canvas.getPrefWidth();
                double canvasHeight = canvas.getPrefHeight();
                double viewportWidth = scrollPane.getViewportBounds().getWidth();
                double viewportHeight = scrollPane.getViewportBounds().getHeight();
                
                if (position != null) {
                    double targetH = position.hvalue;
                    double targetV = position.vvalue;
                    double actualH = scrollPane.getHvalue();
                    double actualV = scrollPane.getVvalue();
                    
                    // 如果滚动位置被改变了，再次恢复（只在差异较大时修正，避免微小抖动）
                    if (Math.abs(targetH - actualH) > 0.01 || Math.abs(targetV - actualV) > 0.01) {
                        scrollPane.setHvalue(targetH);
                        scrollPane.setVvalue(targetV);
                        logger.debug("检测到滚动位置被改变，重新恢复任务组 {} 的滚动位置: hvalue={}, vvalue={}", taskGroupId, targetH, targetV);
                    }
                } else {
                    // 首次加载，设置为居中
                    scrollPane.setHvalue(0.5);
                    scrollPane.setVvalue(0.5);
                    taskGroupScrollPositions.put(taskGroupId, new ScrollPosition(0.5, 0.5));
                    logger.debug("任务组 {} 首次加载，设置滚动位置为居中", taskGroupId);
                }
            });
            delay.play();
        });
    }
    
    /**
     * 导出任务组
     */
    private void exportTaskGroup(Long taskGroupId) {
        if (taskGroupId == null) {
            NotificationToast.showWarning("⚠ 任务组ID无效");
            logger.warn("⚠ 任务组ID无效");
            return;
        }

        Stage ownerStage = (Stage) this.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出任务组");
        String taskGroupName = getJobNameById(taskGroupId);
        fileChooser.setInitialFileName(taskGroupName != null ? taskGroupName + ".cel" : "taskgroup_" + taskGroupId + ".cel");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CEL文件 (*.cel)", "*.cel");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showSaveDialog(ownerStage);
        if (file == null) {
            return;
        }

        logger.info("📤 开始导出任务组: " + taskGroupName);
        new Thread(() -> {
            try {
                JobPartService jobPartService = new JobPartService();
                byte[] data = jobPartService.exportTaskGroupData(taskGroupId);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(data);
                    fos.flush();
                }
                Platform.runLater(() -> {
                    NotificationToast.showSuccess("任务组已导出到: " + file.getAbsolutePath());
                });
            } catch (Exception e) {
                logger.error("导出任务组失败", e);
                Platform.runLater(() ->
                    NotificationToast.showError("导出任务组时发生错误: " + e.getMessage()));
            }
        }, "export-taskgroup").start();
    }
    
    /**
     * 显示查找节点对话框
     */
    private void showFindNodeDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("查找节点");
        dialog.setHeaderText(null);
        dialog.setContentText("请输入节点名称:");
        dialog.setGraphic(null);
        String themeCss2 = ThemeManager.getInstance().getStylesheetUrl();
        if (themeCss2 != null && !themeCss2.isEmpty()) {
            dialog.getDialogPane().getStylesheets().add(themeCss2);
        }
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nodeName -> {
            ProcessNode foundNode = canvas.findNodeByName(nodeName);
            if (foundNode != null) {
                canvas.locateNode(foundNode);
                canvas.selectNode(foundNode);
                logger.info("✓ 已找到并定位到节点: " + nodeName);
            } else {
                NotificationToast.showWarning("⚠ 未找到节点: " + nodeName);
                logger.warn("⚠ 未找到节点: " + nodeName);
            }
        });
    }
    
    /**
     * 显示转到节点对话框
     */
    private void showGoToNodeDialog() {
        showFindNodeDialog();
    }
    
    /**
     * 切换到指定任务组
     */
    private void switchToTaskGroup(Long taskGroupId, String taskGroupName) {
        if (taskGroupId == null) {
            NotificationToast.showWarning("⚠ 任务组ID无效");
            logger.warn("⚠ 任务组ID无效");
            return;
        }
        
        // 切换任务组前，先保存当前任务组的数据和滚动位置
        Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
        if (currentTaskGroupId != null && currentTaskGroupId != 0 && !currentTaskGroupId.equals(taskGroupId)) {
            if (canvas.hasUnsavedChanges()) {
                dataManager.saveOrUpdateJob(currentTaskGroupId);
                canvas.markAsSaved();
            }
            // 保存当前任务组的滚动位置
            saveCurrentScrollPosition();
            saveCurrentCanvasSize();
        }
        
        String displayName = (taskGroupName != null && !taskGroupName.isBlank())
            ? taskGroupName
            : ("任务组 " + taskGroupId);
        
        taskGroupNameToIdMap.put(displayName, taskGroupId);
        pageStoreHelper.setCurrentPage(taskGroupId);
        navigationBar.addOrSelectTask(displayName, taskGroupId);
        toolBar.setCurrentTaskGroupId(taskGroupId);
        // 数据加载后会自动配置节点回调，并恢复滚动位置
        dataManager.loadTaskGroupData(taskGroupId, displayName);
        restoreCanvasSize(taskGroupId);
        // 添加到最近打开的文件列表
        com.cc.job.gui.util.RecentFilesManager.getInstance().addRecentFile(taskGroupId, displayName);
        toolBar.refreshRecentFilesMenu();
    }
    
    /**
     * 显示转到任务组对话框
     * 使用分区树（JobPartService.getTree）中的任务组（type=1）填充下拉，不再使用执行器列表。
     */
    private void showGoToTaskGroupDialog() {
        new Thread(() -> {
            try {
                List<JobPartVo> tree = new JobPartService().getTree();
                List<JobPartVo> taskGroups = new ArrayList<>();
                if (tree != null) {
                    for (JobPartVo part : tree) {
                        if (part.getType() != null && part.getType() == 0 && part.getChildren() != null) {
                            for (JobPartVo child : part.getChildren()) {
                                if (child.getType() != null && child.getType() == 1) {
                                    taskGroups.add(child);
                                }
                            }
                        }
                    }
                }
                List<JobPartVo> finalList = taskGroups;
                Platform.runLater(() -> {
                    ChoiceDialog<String> dialog = new ChoiceDialog<>();
                    dialog.setTitle("转到任务组");
                    dialog.setHeaderText(null);
                    dialog.setContentText("请选择任务组:");
                    List<String> names = new ArrayList<>();
                    for (JobPartVo g : finalList) {
                        names.add(g.getLabel() != null ? g.getLabel() : "任务组 " + g.getId());
                    }
                    dialog.getItems().addAll(names);
                    String themeCss3 = ThemeManager.getInstance().getStylesheetUrl();
                    if (themeCss3 != null && !themeCss3.isEmpty()) {
                        dialog.getDialogPane().getStylesheets().add(themeCss3);
                    }
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(taskGroupName -> {
                        for (JobPartVo g : finalList) {
                            String name = g.getLabel() != null ? g.getLabel() : "任务组 " + g.getId();
                            if (name.equals(taskGroupName)) {
                                switchToTaskGroup(g.getId(), name);
                                break;
                            }
                        }
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationToast.showError("✗ 加载任务组列表失败: " + e.getMessage());
                });
                logger.error("✗ 加载任务组列表失败: {}", e.getMessage());
            }
        }).start();
    }
    
    /**
     * 显示转到分区对话框
     */
    private void showGoToPartitionDialog() {
        // TODO: 实现转到分区
        logger.info("转到分区功能开发中...");
    }
    
    /**
     * 显示快捷键列表对话框
     */
    private void showShortcutsDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("快捷键列表");
        
        VBox content = new VBox(10);
        content.getStyleClass().add("popup-window-root");
        content.setPadding(new Insets(20));
        
        Label title = new Label("快捷键列表");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        VBox shortcutsBox = new VBox(5);
        
        // 编辑快捷键
        Label editLabel = new Label("编辑操作:");
        editLabel.setStyle("-fx-font-weight: bold;");
        shortcutsBox.getChildren().add(editLabel);
        shortcutsBox.getChildren().add(new Label("Ctrl+Z - 撤销"));
        shortcutsBox.getChildren().add(new Label("Ctrl+Y - 重做"));
        shortcutsBox.getChildren().add(new Label("Ctrl+X - 剪切"));
        shortcutsBox.getChildren().add(new Label("Ctrl+C - 复制"));
        shortcutsBox.getChildren().add(new Label("Ctrl+V - 粘贴"));
        shortcutsBox.getChildren().add(new Label("Delete - 删除"));
        shortcutsBox.getChildren().add(new Label("Ctrl+A - 全选"));
        shortcutsBox.getChildren().add(new Label("Ctrl+F - 查找节点"));
        shortcutsBox.getChildren().add(new Label("F3 - 查找下一个"));
        shortcutsBox.getChildren().add(new Label("Shift+F3 - 查找上一个"));
        
        shortcutsBox.getChildren().add(new Separator());
        
        // 视图快捷键
        Label viewLabel = new Label("视图操作:");
        viewLabel.setStyle("-fx-font-weight: bold;");
        shortcutsBox.getChildren().add(viewLabel);
        shortcutsBox.getChildren().add(new Label("Ctrl+= - 放大"));
        shortcutsBox.getChildren().add(new Label("Ctrl+- - 缩小"));
        shortcutsBox.getChildren().add(new Label("Ctrl+0 - 适应窗口"));
        shortcutsBox.getChildren().add(new Label("F11 - 全屏"));
        
        shortcutsBox.getChildren().add(new Separator());
        
        // 功能快捷键
        Label funcLabel = new Label("功能操作:");
        funcLabel.setStyle("-fx-font-weight: bold;");
        shortcutsBox.getChildren().add(funcLabel);
        shortcutsBox.getChildren().add(new Label("Ctrl+N - 新建任务"));
        shortcutsBox.getChildren().add(new Label("Ctrl+O - 打开"));
        shortcutsBox.getChildren().add(new Label("Ctrl+S - 保存"));
        shortcutsBox.getChildren().add(new Label("Ctrl+Shift+S - 另存为"));
        shortcutsBox.getChildren().add(new Label("F5 - 运行任务组"));
        shortcutsBox.getChildren().add(new Label("Shift+F5 - 停止任务"));
        shortcutsBox.getChildren().add(new Label("Ctrl+G - 转到节点"));
        shortcutsBox.getChildren().add(new Label("Ctrl+Shift+? - 快捷键列表"));
        
        ScrollPane scrollPane = new ScrollPane(shortcutsBox);
        scrollPane.getStyleClass().add("popup-content-scroll");
        scrollPane.setFitToWidth(true);
        
        Button closeButton = new Button("关闭");
        closeButton.setOnAction(e -> dialog.close());
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().add(closeButton);
        
        content.getChildren().addAll(title, scrollPane, buttonBox);
        
        Scene scene = new Scene(content, 400, 500);
        scene.getStylesheets().add(com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl());
        dialog.setScene(scene);
        dialog.show();
    }
    
    /**
     * 显示更新日志对话框
     */
    private void showChangelogDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("更新日志");
        
        VBox content = new VBox(10);
        content.getStyleClass().add("popup-window-root");
        content.setPadding(new Insets(20));
        
        Label title = new Label("更新日志");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        TextArea changelogArea = new TextArea();
        changelogArea.setEditable(false);
        changelogArea.setText("版本 2.0.0\n" +
            "- 新增完整的导航栏菜单系统\n" +
            "- 新增最近打开文件功能\n" +
            "- 新增系统设置对话框\n" +
            "- 优化用户体验\n" +
            "\n更多更新信息请查看项目文档。");
        changelogArea.setPrefRowCount(15);
        
        Button closeButton = new Button("关闭");
        closeButton.setOnAction(e -> dialog.close());
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonBox.getChildren().add(closeButton);
        
        content.getChildren().addAll(title, changelogArea, buttonBox);
        
        Scene scene = new Scene(content, 500, 400);
        scene.getStylesheets().add(com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl());
        dialog.setScene(scene);
        dialog.show();
    }
    
    /**
     * 显示关于对话框
     */
    private void showAboutDialog() {
        Alert aboutAlert = new Alert(Alert.AlertType.INFORMATION);
        aboutAlert.setTitle("关于 CcETL");
        aboutAlert.setHeaderText("CcETL - 可视化任务调度平台");
        aboutAlert.setContentText("版本: 2.0.0\n\n" +
            "基于 XXL-Job 深度改造的可视化任务调度平台\n" +
            "支持拖拽式任务编排、DataX 数据同步、多端管理界面\n\n" +
            "许可证: MIT License\n\n" +
            "项目地址:\n" +
            "GitHub: https://github.com/xiaozhaoCcz/CC_ETL\n" +
            "Gitee: https://gitee.com/xzjsccz/Cc_ETL");
        String aboutCss = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (aboutCss != null && !aboutCss.isEmpty()) {
            aboutAlert.getDialogPane().getStylesheets().add(aboutCss);
        }
        aboutAlert.showAndWait();
    }
}

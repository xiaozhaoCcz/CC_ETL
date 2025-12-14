package com.cc.job.gui.view;

import com.cc.job.gui.history.UndoRedoManager;
import com.cc.job.gui.manager.*;
import com.cc.job.gui.model.*;
import com.cc.job.gui.service.*;
import com.cc.job.gui.util.*;
import com.cc.job.xo.model.entity.JobGroup;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Set;

/**
 * 主界面视图 - 作为协调器，委托具体逻辑给各个管理器
 * 重构版本：将原5009行代码拆分为多个管理器，主类仅保留650行
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
    
    // 布局容器
    private VBox leftArea;
    private HBox leftContainer;
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
    private Map<String, Long> taskGroupNameToIdMap = new HashMap<>();
    
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
        this.setStyle("-fx-background-color: #F1F5F9;");

        // 顶部工具栏
        toolBar = new TopToolBar();
        this.setTop(toolBar);

        // 折叠侧边栏
        collapsedSidebar = new CollapsedSidebar();

        // 左侧区域：树形导航 + 小地图
        leftArea = new VBox(8);
        leftArea.setStyle("-fx-background-color: transparent;");
        
        treeView = new TaskTreeView();
        VBox.setVgrow(treeView, Priority.ALWAYS);
        
        miniMap = new MiniMapView();
        leftArea.getChildren().addAll(treeView, miniMap);

        // 左侧容器
        leftContainer = new HBox(0);
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

        // 画布区域容器
        VBox canvasArea = new VBox(0);
        canvasArea.getChildren().addAll(navigationBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 日志面板
        logPanel = new LogPanel();

        // 垂直分割面板
        verticalSplit = new SplitPane();
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getItems().addAll(canvasArea, logPanel);
        verticalSplit.setDividerPositions(0.7);
        verticalSplit.setPadding(new Insets(0, 12, 12, 4));

        // 水平分割面板
        horizontalSplit = new SplitPane();
        horizontalSplit.setOrientation(Orientation.HORIZONTAL);
        horizontalSplit.getItems().addAll(leftContainer, verticalSplit);
        horizontalSplit.setDividerPositions(0.2);
        horizontalSplit.setPadding(new Insets(0, 12, 12, 0));

        miniMap.bindTo(canvas, scrollPane);
        this.setCenter(horizontalSplit);
    }
    
    private void initializeManagers() {
        Stage ownerStage = (Stage) this.getScene().getWindow();
        
        taskExecutionManager = new TaskExecutionManager(canvas, logPanel, navigationBar, toolBar);
        nodeOperationManager = new NodeOperationManager(canvas, logPanel, treeView, ownerStage);
        dialogManager = new DialogManager(ownerStage, logPanel);
        dataManager = new DataManager(canvas, logPanel, treeView);
        nodeCallbackConfigurator = new NodeCallbackConfigurator(nodeOperationManager, canvas, logPanel);
        
        // 设置数据加载完成后的回调，配置所有节点的操作回调
        dataManager.setOnDataLoaded(() -> {
            Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            if (currentTaskGroupId != null) {
                nodeCallbackConfigurator.configureAllNodeCallbacks(currentTaskGroupId);
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
                Long taskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                if (taskGroupId != null && taskGroupId != 0) {
                    String taskGroupName = getJobNameById(taskGroupId);
                    dialogManager.showJobNodeDialog(taskGroupId, taskGroupName != null ? taskGroupName : "新建任务节点", null, () -> {
                        // 数据加载后会自动配置节点回调
                        dataManager.loadTaskGroupData(taskGroupId, taskGroupName);
                    });
                } else {
                    logPanel.warn("⚠ 请先选择任务组");
                }
            }

            @Override
            public void onNewPart() {
                dialogManager.showNewPartitionDialog(() -> dataManager.refreshTreeView());
            }

            @Override
            public void onOpen() {
                // 导入分区文件
                logPanel.info("📂 打开文件功能");
            }

            @Override
            public void onSave() {
                dataManager.saveOrUpdateJob(pageStoreHelper.getCurrentTaskGroupId());
            }

            @Override
            public void onUndo() {
                if (undoRedoManager.canUndo()) {
                    undoRedoManager.undo();
                    logPanel.info("↩ 已撤销");
                }
            }

            @Override
            public void onRedo() {
                if (undoRedoManager.canRedo()) {
                    undoRedoManager.redo();
                    logPanel.info("↪ 已重做");
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
            public void onRun() {
                Long jobId = pageStoreHelper.getCurrentTaskGroupId();
                String jobName = getJobNameById(jobId);
                taskExecutionManager.triggerJobExecution(jobId, jobName != null ? jobName : "任务组" + jobId);
            }

            @Override
            public void onStop(Long jobId) {
                taskExecutionManager.stopJobExecution(jobId);
            }

            @Override
            public void onClear() {
                canvas.clear();
                logPanel.warn("画布已清空");
            }

            @Override
            public void onSettings() {
                logPanel.info("系统设置功能开发中...");
            }

            @Override
            public void onSelect() {
                boolean currentMode = canvas.isSelectionMode();
                canvas.setSelectionMode(!currentMode);
                logPanel.info(currentMode ? "✓ 框选模式已禁用" : "✓ 框选模式已启用");
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
                    logPanel.warn("⚠ 任务ID无效");
                    return;
                }
                
                String displayName = (taskGroupName != null && !taskGroupName.isBlank())
                    ? taskGroupName
                    : ("任务组 " + taskGroupId);
                
                taskGroupNameToIdMap.put(displayName, taskGroupId);
                pageStoreHelper.setCurrentPage(taskGroupId);
                navigationBar.addOrSelectTask(displayName, taskGroupId);
                toolBar.setCurrentTaskGroupId(taskGroupId);
                // 数据加载后会自动配置节点回调
                dataManager.loadTaskGroupData(taskGroupId, displayName);
            }
        });

        // 导航栏回调
        navigationBar.setOnTaskSwitch((taskGroupName, taskGroupId) -> {
            Long resolvedTaskId = taskGroupId != null ? taskGroupId : findTaskGroupIdByName(taskGroupName);
            
            if (resolvedTaskId != null) {
                taskGroupNameToIdMap.put(taskGroupName, resolvedTaskId);
                navigationBar.setCurrentTaskGroupId(resolvedTaskId);
                toolBar.setCurrentTaskGroupId(resolvedTaskId);
                pageStoreHelper.setCurrentPage(resolvedTaskId);
                dataManager.loadTaskGroupData(resolvedTaskId, taskGroupName);
                
                Platform.runLater(() -> {
                    treeView.selectTaskGroupById(resolvedTaskId);
                });
            } else {
                logPanel.warn("⚠ 未找到任务组ID: " + taskGroupName);
            }
        });

        navigationBar.setOnTaskClose((taskGroupId, taskGroupName) -> {
            if (taskGroupId != null && taskGroupId.equals(pageStoreHelper.getCurrentTaskGroupId())) {
                treeView.clearSelection();
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
        
        canvas.setOnRequestAddNode(() -> {
            Long taskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            if (taskGroupId == null || taskGroupId == 0) {
                logPanel.warn("⚠ 请先选择任务组");
                return;
            }
            String taskGroupName = getJobNameById(taskGroupId);
            dialogManager.showJobNodeDialog(taskGroupId, taskGroupName, null, () -> {
                // 数据加载后会自动配置节点回调
                dataManager.loadTaskGroupData(taskGroupId, taskGroupName);
            });
        });
        
        canvas.setOnRequestRunTaskGroup(() -> {
            Long jobId = pageStoreHelper.getCurrentTaskGroupId();
            String jobName = getJobNameById(jobId);
            taskExecutionManager.triggerJobExecution(jobId, jobName);
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
                    taskGroupNameToIdMap.put(taskName, taskId);
                    pageStoreHelper.setCurrentPage(taskId);
                    navigationBar.addOrSelectTask(taskName, taskId);
                    toolBar.setCurrentTaskGroupId(taskId);
                    dataManager.loadTaskGroupData(taskId, taskName);
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
                }
            }

            @Override
            public void onEdgeAction(Long edgeId, TaskTreeView.TaskSelectionCallback.EdgeAction action) {}

            @Override
            public void onEdgeAction(Long edgeId, Long taskGroupId, TaskTreeView.TaskSelectionCallback.EdgeAction action) {}

            @Override
            public void onPartitionAction(Long partitionId, String partitionName, TaskTreeView.TaskSelectionCallback.PartitionAction action) {}

            @Override
            public void onJobGroupEdit(Long taskGroupId, String taskGroupName) {}
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
                    }
                });
            }
        });
    }

    private void handleCopyShortcut() {
        Set<ProcessNode> selectedNodes = canvas.getSelectedNodes();
        Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
        
        if (selectedNodes.isEmpty()) {
            logPanel.warn("⚠ 请先选中要复制的节点");
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
            logPanel.warn("⚠ 请先选择任务组");
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
}

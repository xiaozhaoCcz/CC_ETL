package com.cc.job.gui.view;

import com.cc.job.gui.history.UndoRedoManager;
import com.cc.job.gui.manager.*;
import com.cc.job.gui.model.*;
import com.cc.job.gui.service.*;
import com.cc.job.gui.util.*;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobInfoForm;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
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
import java.io.IOException;
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

        dialogManager = new DialogManager(ownerStage, logPanel);
        taskExecutionManager = new TaskExecutionManager(canvas, logPanel, navigationBar, toolBar);
        nodeOperationManager = new NodeOperationManager(canvas, logPanel, treeView, ownerStage);
        dataManager = new DataManager(canvas, logPanel, treeView);
        nodeCallbackConfigurator = new NodeCallbackConfigurator(nodeOperationManager, canvas, logPanel);
        
        // 重要：设置对话框管理器的任务执行管理器（用于获取预测时间）
        dialogManager.setTaskExecutionManager(taskExecutionManager);
        
        // 重要：设置节点操作管理器的回调配置器（用于新增节点时自动配置回调）
        nodeOperationManager.setNodeCallbackConfigurator(nodeCallbackConfigurator);
        
        // 重要：设置对话框管理器（用于显示节点详情对话框）
        nodeCallbackConfigurator.setDialogManager(dialogManager);
        
        // 设置数据加载完成后的回调，配置所有节点的操作回调
        dataManager.setOnDataLoaded(() -> {
            Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            if (currentTaskGroupId != null) {
                nodeCallbackConfigurator.configureAllNodeCallbacks(currentTaskGroupId);
                
                // ⭐ 修复：切换任务组标签后，如果任务组正在运行，恢复边的运行状态
                if (taskExecutionManager != null && taskExecutionManager.isTaskGroupRunning(currentTaskGroupId)) {
                    canvas.setAllConnectionsRunning(true);
                }
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
            public void onNodeHistory() {
                Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
                if (currentTaskGroupId == null) {
                    logPanel.warn("⚠ 请先选择一个任务组");
                    return;
                }
                String taskGroupName = getJobNameById(currentTaskGroupId);
                showNodeHistoryDialog(currentTaskGroupId, taskGroupName != null ? taskGroupName : "任务组" + currentTaskGroupId);
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
            Long currentTaskGroupId = pageStoreHelper.getCurrentTaskGroupId();
            
            // 如果关闭的是当前显示的任务组，需要清空画布
            if (taskGroupId != null && taskGroupId.equals(currentTaskGroupId)) {
                treeView.clearSelection();
                // 清空画布
                canvas.clear();
                // 清空当前页面状态
                pageStoreHelper.setCurrentPage(null);
                toolBar.setCurrentTaskGroupId(null);
                logPanel.info("📋 已关闭任务组: " + taskGroupName);
            }
            
            // 检查是否所有标签页都已关闭
            Long remainingTaskGroupId = navigationBar.getCurrentTaskGroupId();
            if (remainingTaskGroupId == null) {
                // 所有标签页都已关闭，确保画布是空的
                canvas.clear();
                pageStoreHelper.setCurrentPage(null);
                toolBar.setCurrentTaskGroupId(null);
                logPanel.info("📋 所有任务组已关闭");
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
                NotificationToast.showWarning("请先选择任务组");
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
                } else if (action == TaskTreeView.TaskSelectionCallback.JobNodeAction.LOCATE) {
                    // 定位节点：先检查是否需要切换任务组
                    locateNodeWithTaskGroupSwitch(jobId, nodeName, taskGroupId);
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
                        logPanel.info("📍 已定位到节点: " + nodeName);
                        return;
                    } else {
                        // 节点不在当前画布中，无法定位
                        logPanel.warn("⚠ 未找到节点: " + nodeName + "，请先打开对应的任务组");
                        return;
                    }
                }
                
                // 检查是否需要切换任务组
                if (currentTaskGroupId == null || !currentTaskGroupId.equals(targetTaskGroupId)) {
                    // 需要切换任务组
                    logPanel.info("🔄 切换到任务组: " + targetTaskGroupId);
                    
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
                    
                    // 加载任务组数据，加载完成后定位节点
                    dataManager.loadTaskGroupData(targetTaskGroupId, taskGroupName);
                    
                    // 延迟定位节点（等待数据加载完成）
                    Platform.runLater(() -> {
                        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                        delay.setOnFinished(e -> {
                            ProcessNode targetNode = canvas.getNodeByJobId(jobId);
                            if (targetNode != null) {
                                canvas.locateNode(targetNode);
                                logPanel.info("📍 已定位到节点: " + nodeName);
                            } else {
                                logPanel.warn("⚠ 节点加载后仍未找到: " + nodeName);
                            }
                        });
                        delay.play();
                    });
                } else {
                    // 当前任务组已匹配，直接定位
                    ProcessNode targetNode = canvas.getNodeByJobId(jobId);
                    if (targetNode != null) {
                        canvas.locateNode(targetNode);
                        logPanel.info("📍 已定位到节点: " + nodeName);
                    } else {
                        logPanel.warn("⚠ 未找到节点: " + nodeName);
                    }
                }
            }

            @Override
            public void onEdgeAction(Long edgeId, TaskTreeView.TaskSelectionCallback.EdgeAction action) {
                if (action == TaskTreeView.TaskSelectionCallback.EdgeAction.LOCATE) {
                    // 定位连接线
                    if (canvas.locateConnectionByEdgeId(edgeId != null ? edgeId.toString() : null)) {
                        logPanel.info("📍 已定位到连接线");
                    } else {
                        logPanel.warn("⚠ 未找到连接线");
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
                        logPanel.info("📍 已定位到连接线");
                        return;
                    } else {
                        logPanel.warn("⚠ 未找到连接线，请先打开对应的任务组");
                        return;
                    }
                }
                
                // 检查是否需要切换任务组
                if (currentTaskGroupId == null || !currentTaskGroupId.equals(targetTaskGroupId)) {
                    // 需要切换任务组
                    logPanel.info("🔄 切换到任务组: " + targetTaskGroupId);
                    
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
                    
                    // 加载任务组数据，加载完成后定位连接线
                    dataManager.loadTaskGroupData(targetTaskGroupId, taskGroupName);
                    
                    // 延迟定位连接线（等待数据加载完成）
                    Platform.runLater(() -> {
                        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                        delay.setOnFinished(e -> {
                            if (canvas.locateConnectionByEdgeId(edgeId != null ? edgeId.toString() : null)) {
                                logPanel.info("📍 已定位到连接线");
                            } else {
                                logPanel.warn("⚠ 连接线加载后仍未找到");
                            }
                        });
                        delay.play();
                    });
                } else {
                    // 当前任务组已匹配，直接定位
                    if (canvas.locateConnectionByEdgeId(edgeId != null ? edgeId.toString() : null)) {
                        logPanel.info("📍 已定位到连接线");
                    } else {
                        logPanel.warn("⚠ 未找到连接线");
                    }
                }
            }

            @Override
            public void onPartitionAction(Long partitionId, String partitionName, TaskTreeView.TaskSelectionCallback.PartitionAction action) {
                if (action == TaskTreeView.TaskSelectionCallback.PartitionAction.EXPORT) {
                    // 导出分区数据
                    exportPartitionData(partitionId, partitionName);
                }
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
                            Platform.runLater(() -> logPanel.error("✗ 加载任务组数据失败: " + e.getMessage()));
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
        container.setStyle("-fx-background-color: #F1F5F9;");
        container.getChildren().add(panel);
        VBox.setVgrow(panel, Priority.ALWAYS);
        
        // 设置窗口大小
        Scene scene = new Scene(container, 400, 500);
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
            if (panel instanceof TaskTreeView) {
                if (!leftArea.getChildren().contains(panel)) {
                    leftArea.getChildren().add(0, panel);
                }
            } else if (panel instanceof MiniMapView) {
                if (!leftArea.getChildren().contains(panel)) {
                    leftArea.getChildren().add(panel);
                }
            }
            
            // 执行回调
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
        container.setStyle("-fx-background-color: #FFFFFF;");
        
        // 将日志面板的所有子节点移动到弹出窗口
        logPanel.detachContent(container);
        
        // 隐藏主界面的日志面板（但不移除）
        logPanelVisible = false;
        updateLeftSidebar();
        
        // 设置窗口大小
        Scene scene = new Scene(container, 800, 500);
        detachedStage.setScene(scene);
        
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
            logPanel.warn("⚠ 分区ID无效");
            return;
        }
        
        Stage ownerStage = (Stage) this.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出分区数据");
        fileChooser.setInitialFileName(partitionName != null ? partitionName + ".ce" : "partition_" + partitionId + ".ce");
        
        // 设置文件过滤器
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CE文件 (*.ce)", "*.ce");
        fileChooser.getExtensionFilters().add(extFilter);
        
        File file = fileChooser.showSaveDialog(ownerStage);
        if (file == null) {
            return; // 用户取消了保存
        }
        
        logPanel.info("📤 开始导出分区数据: " + partitionName);
        
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
        
        logPanel.info("📥 开始导入分区数据: " + file.getName());
        
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
     * 显示节点历史对话框
     */
    private void showNodeHistoryDialog(Long taskGroupId, String taskGroupName) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("节点执行历史 - " + taskGroupName);
        
        NodeHistoryView historyView = new NodeHistoryView(taskGroupId, taskGroupName);
        
        Scene scene = new Scene(historyView, 1000, 600);
        dialog.setScene(scene);
        dialog.show();
        
        logPanel.info("✓ 打开节点历史页面: " + taskGroupName);
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

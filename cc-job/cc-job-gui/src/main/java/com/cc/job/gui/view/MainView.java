package com.cc.job.gui.view;

import com.cc.job.gui.history.UndoRedoManager;
import com.cc.job.gui.model.JobComposeData;
import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.model.RunningJobGroup;
import com.cc.job.gui.service.JobGroupService;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.service.JobLogService;
import com.cc.job.gui.service.JobPartService;
import com.cc.job.gui.service.WebSocketService;
import com.cc.job.gui.util.ApiUtil;
import com.cc.job.gui.util.DetachablePanel;
import com.cc.job.gui.util.SnowflakeIdGenerator;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobInfoForm;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * 主界面视图
 */
public class MainView extends BorderPane {

    private NodeCanvas canvas;
    private TaskTreeView treeView;
    private TopToolBar toolBar;
    private LogPanel logPanel;
    private MiniMapView miniMap;
    private CollapsedSidebar collapsedSidebar;
    private TaskNavigationBar navigationBar;

    private UndoRedoManager undoRedoManager;

    private javafx.scene.layout.VBox leftArea;
    private boolean treeViewVisible = true;
    private boolean miniMapVisible = true;
    private boolean suppressNextTaskLoad = false;

    private ScrollPane scrollPane;
    private double currentZoom = 1.0;
    private static final double ZOOM_STEP = 0.1;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 3.0;

    // API 服务
    private final JobPartService jobPartService;
    private final JobInfoService jobInfoService;
    private final JobLogService jobLogService;
    private final JobGroupService jobGroupService;
    private final ApiUtil apiUtil;

    // 可分离面板管理器
    private DetachablePanel treeViewDetachable;
    private DetachablePanel miniMapDetachable;
    private DetachablePanel logPanelDetachable;

    // 任务组名称到ID的映射
    private java.util.Map<String, Long> taskGroupNameToIdMap = new java.util.HashMap<>();

    // 雪花算法ID生成器
    private final SnowflakeIdGenerator snowflake = SnowflakeIdGenerator.getInstance();

    // 多个任务组的执行状态管理（类似Vue中的logTabs）
    private java.util.Map<Long, RunningJobGroup> runningJobs = new java.util.HashMap<>();

    public MainView() {
        this.jobPartService = new JobPartService();
        this.jobInfoService = new JobInfoService();
        this.jobLogService = new JobLogService();
        this.jobGroupService = new JobGroupService();
        this.apiUtil = ApiUtil.getInstance();
        initializeUI();
        setupCallbacks();
    }

    private void initializeUI() {
        this.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, rgba(248,250,252,0.98), rgba(224,231,255,0.98));"
        );
        
        // 顶部工具栏
        toolBar = new TopToolBar();
        this.setTop(toolBar);

        // 创建折叠侧边栏（始终显示）
        collapsedSidebar = new CollapsedSidebar();

        // 左侧内容区域：树形导航 + 小地图
        leftArea = new javafx.scene.layout.VBox();
        leftArea.setSpacing(12);
        leftArea.setPadding(new Insets(16, 12, 16, 16));
        leftArea.setStyle(
            "-fx-background-color: rgba(255,255,255,0.92); " +
            "-fx-border-color: rgba(148,163,184,0.2); " +
            "-fx-border-width: 0 1 0 0;"
        );

        // 树形导航
        treeView = new TaskTreeView();
        javafx.scene.layout.VBox.setVgrow(treeView, javafx.scene.layout.Priority.ALWAYS);

        // 小地图
        miniMap = new MiniMapView();

        leftArea.getChildren().addAll(treeView, miniMap);

        // 创建左侧容器：折叠栏 + 内容区域
        HBox leftContainer = new HBox();
        leftContainer.getChildren().addAll(collapsedSidebar, leftArea);

        // 让 leftArea 能够水平扩展以填充可用空间
        HBox.setHgrow(leftArea, javafx.scene.layout.Priority.ALWAYS);

        // 任务组导航栏
        navigationBar = new TaskNavigationBar();

        // 画布区域
        canvas = new NodeCanvas();
        undoRedoManager = new UndoRedoManager();
        undoRedoManager.setOnChange(this::updateUndoRedoButtons);
        canvas.setUndoRedoManager(undoRedoManager);
        scrollPane = new ScrollPane(canvas);
        canvas.setScrollPane(scrollPane);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.getStyleClass().add("canvas-scroller");
        scrollPane.setPannable(true);

        // 设置滚动条策略：只在需要时显示
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // 创建画布区域容器：导航栏 + 画布
        javafx.scene.layout.VBox canvasArea = new javafx.scene.layout.VBox();
        canvasArea.setSpacing(0);
        canvasArea.setPadding(new Insets(10, 10, 0, 10));
        canvasArea.setStyle(
            "-fx-background-color: rgba(255,255,255,0.96); " +
            "-fx-background-radius: 20 20 12 12; " +
            "-fx-border-radius: 20 20 12 12; " +
            "-fx-border-color: rgba(148,163,184,0.18); " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 18, 0, 0, 6);"
        );
        canvasArea.getChildren().addAll(navigationBar, scrollPane);
        javafx.scene.layout.VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        // 日志面板
        logPanel = new LogPanel();

        // 创建垂直分割面板：画布区域和日志面板
        SplitPane verticalSplit = new SplitPane();
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getItems().addAll(canvasArea, logPanel);
        verticalSplit.setDividerPositions(0.7); // 初始位置：70% 给画布，30% 给日志
        verticalSplit.setStyle("-fx-background-color: transparent;");
        verticalSplit.setPadding(new Insets(12, 12, 12, 4));

        // 创建水平分割面板：左侧容器和右侧（画布+日志）
        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.setOrientation(Orientation.HORIZONTAL);
        horizontalSplit.getItems().addAll(leftContainer, verticalSplit);
        horizontalSplit.setDividerPositions(0.2); // 初始位置：20% 给左侧，80% 给右侧
        horizontalSplit.setStyle("-fx-background-color: transparent;");
        horizontalSplit.setPadding(new Insets(12));

        // 绑定小地图到画布
        miniMap.bindTo(canvas, scrollPane);

        this.setCenter(horizontalSplit);

        // 添加示例节点
        addSampleNodes();

        // 初始化左侧边栏状态（重要！确保图标正确显示/隐藏）
        updateLeftSidebar();
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

    private void setupCallbacks() {
        // 树形视图关闭回调
        treeView.setOnClose(() -> {
            if (treeViewDetachable != null && treeViewDetachable.isDetached()) {
                treeViewDetachable.reattach();
            }
            treeViewVisible = false;
            updateLeftSidebar();
        });

        // 小地图关闭回调
        miniMap.setOnClose(() -> {
            if (miniMapDetachable != null && miniMapDetachable.isDetached()) {
                miniMapDetachable.reattach();
            }
            miniMapVisible = false;
            updateLeftSidebar();
        });

        // 折叠侧边栏恢复回调
        collapsedSidebar.setOnTreeViewRestore(() -> {
            treeViewVisible = true;
            updateLeftSidebar();
        });

        collapsedSidebar.setOnMiniMapRestore(() -> {
            miniMapVisible = true;
            updateLeftSidebar();
        });

        // 初始化可分离面板
        setupDetachablePanels();

        // 树形视图回调
        setupTreeViewCallback();

        // 导航栏切换任务组回调
        navigationBar.setOnTaskSwitch((TaskNavigationBar.TaskSwitchCallback) taskGroupName -> {
            logPanel.info("导航栏切换到任务组: " + taskGroupName);
            // 根据任务组名称查找对应的ID并加载流程图
            Long taskId = taskGroupNameToIdMap.get(taskGroupName);
            if (taskId == null) {
                // 如果映射中没有，尝试从树形视图中查找
                taskId = findTaskGroupIdByName(taskGroupName);
                if (taskId != null) {
                    taskGroupNameToIdMap.put(taskGroupName, taskId);
                }
            }
            if (taskId != null) {
                // 更新导航栏当前任务组ID
                navigationBar.setCurrentTaskGroupId(taskId);
                // 更新顶部工具栏当前任务组ID
                toolBar.setCurrentTaskGroupId(taskId);
                // 更新页面Store
                usePageStoreHook().setCurrentPage(taskId);
                loadTaskGroupData(taskId, taskGroupName);
                
                // 检查任务运行状态并更新小绿点显示
                checkAndUpdateTaskGroupRunningStatus(taskId, taskGroupName);
            } else {
                logPanel.warn("⚠ 未找到任务组ID，无法加载流程图: " + taskGroupName);
                logPanel.info("提示: 请先在左侧任务树中选择该任务组");
            }
        });

        // 注意：导航栏的运行/停止按钮已移至顶部工具栏，不再需要设置回调

        // 画布日志回调
        canvas.setOnLog(logPanel::info);

        // 画布节点移动回调 - 实时更新小地图
        canvas.setOnNodeMoved(() -> {
            if (miniMap != null) {
                miniMap.refresh();
            }
        });

        // 工具栏回调
        toolBar.setCallback(new TopToolBar.ToolBarCallback() {
            @Override
            public void onNew() {
                showNewPartitionDialog();
            }

            @Override
            public void onOpen() {
                logPanel.info("打开流程图功能开发中...");
            }

            @Override
            public void onSave() {
                saveOrUpdateJob();
            }

            @Override
            public void onUndo() {
                if (undoRedoManager != null && undoRedoManager.canUndo()) {
                    undoRedoManager.undo();
                    logPanel.info("↩ 已撤销上一条操作");
                } else {
                    logPanel.warn("目前没有可撤销的操作");
                }
            }

            @Override
            public void onRedo() {
                if (undoRedoManager != null && undoRedoManager.canRedo()) {
                    undoRedoManager.redo();
                    logPanel.info("↪ 已恢复上一条操作");
                } else {
                    logPanel.warn("目前没有可重做的操作");
                }
            }

            @Override
            public void onZoomIn() {
                zoomCanvas(currentZoom + ZOOM_STEP);
            }

            @Override
            public void onZoomOut() {
                zoomCanvas(currentZoom - ZOOM_STEP);
            }

            @Override
            public void onZoomFit() {
                zoomCanvas(1.0);
                logPanel.info("画布已适应窗口大小 (100%)");
            }

            @Override
            public void onRun() {
                System.out.println("onRun 回调被触发");
                logPanel.info("收到运行请求...");
                triggerJobExecution();
            }

            @Override
            public void onStop(Long jobId) {
                stopJobExecution(jobId);
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
        });

        // 定期更新工具栏显示运行中的任务组
        updateToolBarRunningJobs();
    }

    /**
     * 缩放画布
     */
    private void zoomCanvas(double newZoom) {
        // 限制缩放范围
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));

        if (newZoom == currentZoom) {
            return;
        }

        // 应用缩放
        Scale scale = new Scale(newZoom, newZoom, 0, 0);
        canvas.getTransforms().clear();
        canvas.getTransforms().add(scale);

        currentZoom = newZoom;
        toolBar.updateZoomLevel(newZoom);

        logPanel.debug(String.format("画布缩放: %.0f%%", newZoom * 100));
    }

    /**
     * 任务选择回调（带详细信息）
     */
    private void onTaskSelectedWithDetails(Long taskId, String taskName, Integer type) {
        if (suppressNextTaskLoad) {
            suppressNextTaskLoad = false;
            Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
            if (taskId != null && currentTaskGroupId != null && taskId.equals(currentTaskGroupId)) {
                logPanel.debug("跳过任务组重新加载（保持当前位置）: " + taskName);
                return;
            }
        }

        logPanel.info("选择节点: " + taskName + " [类型: " + getTypeNameByType(type) + "]");

        // ⚠️ 重要：只有任务组（type=1）才添加到导航栏，过滤掉分区（type=0）等其他节点
        if (type != null && type == 1) {
            navigationBar.addOrSelectTask(taskName);
        }

        // 只有任务组（type=1）才加载流程图
        if (type != null && type == 1 && taskId != null) {
            // 更新任务组名称到ID的映射
            taskGroupNameToIdMap.put(taskName, taskId);
            // 更新当前选中的任务组ID
            usePageStoreHook().setCurrentPage(taskId);
            // 更新导航栏当前任务组ID
            navigationBar.setCurrentTaskGroupId(taskId);
            // 更新顶部工具栏当前任务组ID
            toolBar.setCurrentTaskGroupId(taskId);
            loadTaskGroupData(taskId, taskName);
            
            // 检查任务运行状态并更新小绿点显示
            checkAndUpdateTaskGroupRunningStatus(taskId, taskName);
        } else {
            // 如果是分区或其他类型，不显示提示（因为这是正常行为）
            if (type != null && type == 0) {
                // 分区节点不需要任何操作，静默处理
            } else {
                logPanel.info("提示: 只有任务组节点才能展示流程图");
            }
        }
    }

    /**
     * 加载任务组数据
     */
    private void loadTaskGroupData(Long taskId, String taskName) {
        logPanel.info("════════════════════════════════");
        logPanel.info("开始加载任务组: " + taskName);
        logPanel.info("任务组ID: " + taskId);
        
        logPanel.info("准备同步当前任务组的节点运行状态...");

        // 在后台线程中加载数据
        new Thread(() -> {
            try {
                canvas.syncPendingNodeStatusBlocking();
                Platform.runLater(() -> logPanel.info("节点状态同步完成，开始加载最新数据"));

                JobComposeData composeData = jobPartService.getJobCompose(taskId);

                // 在 JavaFX 主线程中更新 UI
                Platform.runLater(() -> {
                    if (composeData != null) {
                        canvas.loadFromComposeData(composeData);

                        // 为所有加载的节点设置编辑回调
                        setupEditCallbacksForLoadedNodes(composeData);

                        int nodeCount = composeData.getNodes() != null ? composeData.getNodes().size() : 0;
                        int edgeCount = composeData.getEdges() != null ? composeData.getEdges().size() : 0;

                        logPanel.success("✓ 任务组加载成功！");
                        logPanel.info("节点数: " + nodeCount + ", 连接数: " + edgeCount);
                        logPanel.info("════════════════════════════════");
                    } else {
                        logPanel.warn("⚠ 任务组数据为空");
                        canvas.clear();
                    }
                });

            } catch (IOException e) {
                System.err.println("加载任务组数据失败: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    logPanel.error("✗ 加载任务组数据失败: " + e.getMessage());
                    logPanel.warn("提示: 请检查后端服务是否正常运行");
                });
            }
        }).start();
    }

    /**
     * 根据类型码获取类型名称
     */
    private String getTypeNameByType(Integer type) {
        if (type == null) return "未知";
        return switch (type) {
            case 0 -> "分区";
            case 1 -> "任务组";
            case 2 -> "任务容器";
            case 3 -> "关系容器";
            case 4 -> "任务节点";
            case 5 -> "关系边";
            default -> "未知";
        };
    }

    /**
     * 根据任务组名称从树形视图中查找任务组ID
     */
    private Long findTaskGroupIdByName(String taskGroupName) {
        if (taskGroupName == null || taskGroupName.isEmpty()) {
            return null;
        }

        // 使用树形视图的方法查找任务组ID
        return treeView.findTaskGroupIdByName(taskGroupName);
    }

    /**
     * 任务选择回调（旧版本，保持兼容）
     */
    private void onTaskSelected(String taskName) {
        onTaskSelectedWithDetails(null, taskName, null);
    }

    /**
     * 添加示例节点
     */
    private void addSampleNodes() {
        ProcessNode node1 = new ProcessNode("node1", "demoJobHandler1");
        ProcessNode node2 = new ProcessNode("node2", "demoJobHandler2");
        ProcessNode node3 = new ProcessNode("node3", "demoJobHandler3");

        node1.setLayoutX(100);
        node1.setLayoutY(100);

        node2.setLayoutX(100);
        node2.setLayoutY(250);

        node3.setLayoutX(400);
        node3.setLayoutY(175);

        canvas.addNode(node1, false);
        canvas.addNode(node2, false);
        canvas.addNode(node3, false);
        configureNodeCallbacks(node1);
        configureNodeCallbacks(node2);
        configureNodeCallbacks(node3);

        canvas.addConnection(node1, node1.getRightConnector(), node3, node3.getLeftConnector(), false);
        canvas.addConnection(node1, node1.getBottomConnector(), node2, node2.getTopConnector(), false);

        logPanel.success("示例流程图加载完成");
        logPanel.info("共 3 个节点, 2 条连接");
    }

    /**
     * 初始化可分离面板
     */
    private void setupDetachablePanels() {
        // 树形视图可分离面板
        treeViewDetachable = new DetachablePanel(treeView, "任务组导航");
        treeViewDetachable.setDefaultSize(400, 600);
        treeViewDetachable.setOnDetach(() -> {
            logPanel.info("任务组面板已弹出为独立窗口");
        });
        treeViewDetachable.setOnReattach(() -> {
            logPanel.info("任务组面板已恢复到原位置");
        });

        // 连接弹出按钮
        treeView.setOnDetach(() -> {
            treeViewDetachable.detach();
        });

        // 小地图可分离面板
        miniMapDetachable = new DetachablePanel(miniMap, "小地图");
        miniMapDetachable.setDefaultSize(350, 350);
        miniMapDetachable.setOnDetach(() -> {
            logPanel.info("小地图面板已弹出为独立窗口");
        });
        miniMapDetachable.setOnReattach(() -> {
            logPanel.info("小地图面板已恢复到原位置");
        });

        // 连接弹出按钮
        miniMap.setOnDetach(() -> {
            miniMapDetachable.detach();
        });

        // 日志面板可分离面板
        logPanelDetachable = new DetachablePanel(logPanel, "日志监控");
        logPanelDetachable.setDefaultSize(1000, 300);
        logPanelDetachable.setOnDetach(() -> {
            System.out.println("日志面板已弹出为独立窗口");
        });
        logPanelDetachable.setOnReattach(() -> {
            System.out.println("日志面板已恢复到原位置");
        });

        // 连接弹出按钮
        logPanel.setOnDetach(() -> {
            logPanelDetachable.detach();
        });
    }

    /**
     * 更新左侧边栏状态
     */
    private void updateLeftSidebar() {
        // 更新组件的可见性
        treeView.setVisible(treeViewVisible);
        treeView.setManaged(treeViewVisible);
        miniMap.setVisible(miniMapVisible);
        miniMap.setManaged(miniMapVisible);

        // 判断是否有任何组件可见
        boolean anyVisible = treeViewVisible || miniMapVisible;

        // leftArea 根据是否有组件可见来决定是否显示
        leftArea.setVisible(anyVisible);
        leftArea.setManaged(anyVisible);

        // 折叠侧边栏始终显示，只控制按钮的显示
        // 只显示已隐藏组件对应的按钮
        collapsedSidebar.showTreeViewButton(!treeViewVisible);
        collapsedSidebar.showMiniMapButton(!miniMapVisible);
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

    /**
     * 触发任务执行
     */
    private void triggerJobExecution() {
        System.out.println("triggerJobExecution 开始执行");

        // 获取当前选中的任务组
        Long currentJobId = usePageStoreHook().getCurrentPage();
        System.out.println("当前任务组ID: " + currentJobId);

        if (currentJobId == null || currentJobId == 0) {
            logPanel.warn("⚠ 请先选择一个任务组");
            System.out.println("错误: 未选择任务组");
            return;
        }

        // 检查该任务组是否正在运行
        RunningJobGroup existingJob = runningJobs.get(currentJobId);
        if (existingJob != null && existingJob.isRunning()) {
            logPanel.warn("⚠ 任务组 " + currentJobId + " 正在运行中，请稍后再试");
            return;
        }

        // 获取任务组名称
        String jobName = getJobNameById(currentJobId);
        if (jobName == null) {
            jobName = "任务组 " + currentJobId;
        }

        // 生成新的randomId
        String randomId = snowflake.nextIdStr();
        
        // 获取当前登录用户ID
        String currentUserId = com.cc.job.gui.util.SessionManager.getInstance().getUserId();

        // 创建新的运行任务组记录（包含触发用户ID）
        RunningJobGroup runningJob = new RunningJobGroup(currentJobId, jobName, randomId, currentUserId);
        runningJobs.put(currentJobId, runningJob);

        // 创建或切换到任务组的日志标签页
        logPanel.addOrSwitchToTaskGroup(currentJobId, jobName);

        logPanel.info(currentJobId, "════════════════════════════════");
        logPanel.success(currentJobId, "✨ 开始执行任务组: " + jobName + " (ID: " + currentJobId + ")");
        logPanel.info(currentJobId, "执行批次ID: " + randomId);
        logPanel.info(currentJobId, "════════════════════════════════");

        // 更新导航栏中的小绿点（任务开始运行）
        navigationBar.updateTaskGroupRunningStatus(jobName, true);

        // 更新工具栏显示
        updateToolBarRunningJobs();

        // 设置所有边为运行状态（虚线动画）
        canvas.setAllConnectionsRunning(true);

        // 重置所有节点状态为空闲（紫色），等待WebSocket消息来更新节点状态
        // 只有实际运行到的节点才会通过WebSocket消息变成黄色
        Platform.runLater(() -> {
            System.out.println("════════════════════════════════");
            System.out.println("📋 任务组启动前，检查画布中的节点:");
            System.out.println("   画布中共有 " + canvas.getNodes().size() + " 个节点");
            
            logPanel.info(currentJobId, "════════════════════════════════");
            logPanel.info(currentJobId, "📋 任务组启动前，检查画布中的节点:");
            logPanel.info(currentJobId, "   画布中共有 " + canvas.getNodes().size() + " 个节点");
            
            for (ProcessNode node : canvas.getNodes()) {
                String nodeInfo = "   - 节点: " + node.getJobHandlerName() + 
                                 ", nodeId: " + node.getNodeId() + 
                                 ", jobId: " + node.getJobId();
                System.out.println(nodeInfo);
                logPanel.info(currentJobId, nodeInfo);
                node.updateStatus(ProcessNode.NodeStatus.IDLE);
            }
            System.out.println("════════════════════════════════");
            logPanel.info(currentJobId, "════════════════════════════════");
        });

        // 连接WebSocket以接收节点状态更新
        System.out.println("🔌 准备连接WebSocket");
        System.out.println("   任务组ID: " + currentJobId);
        System.out.println("   randomId: " + randomId);
        System.out.println("   连接ID: " + currentJobId + ":" + randomId);
        
        logPanel.info(currentJobId, "🔌 准备连接WebSocket");
        logPanel.info(currentJobId, "   任务组ID: " + currentJobId);
        logPanel.info(currentJobId, "   randomId: " + randomId);
        logPanel.info(currentJobId, "   连接ID: " + currentJobId + ":" + randomId);
        
        WebSocketService wsService = WebSocketService.getInstance();
        wsService.connect(currentJobId, randomId, message -> {
            handleWebSocketMessage(message, randomId);
        });
        
        System.out.println("✅ WebSocket连接请求已发送");
        logPanel.info(currentJobId, "✅ WebSocket连接请求已发送");
        
        // 等待一段时间后检查连接状态
        new Thread(() -> {
            try {
                Thread.sleep(3000); // 等待3秒
                javafx.application.Platform.runLater(() -> {
                    if (!wsService.isConnected(currentJobId, randomId)) {
                        logPanel.warn(currentJobId, "⚠️ WebSocket连接可能未成功建立");
                        logPanel.warn(currentJobId, "   请检查后端WebSocket服务是否正常运行");
                        logPanel.warn(currentJobId, "   连接ID: " + currentJobId + ":" + randomId);
                    } else {
                        logPanel.info(currentJobId, "✅ WebSocket连接状态检查: 已连接");
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // 在后台线程中执行任务
        new Thread(() -> {
            try {
                // 调用后端API触发任务
                Long logId = jobInfoService.triggerJob(currentJobId, randomId);
                runningJob.setLogId(logId);

                Platform.runLater(() -> {
                    logPanel.success("✓ 任务已提交，日志ID: " + logId);
                    logPanel.info("开始获取执行日志...");
                });

                // 启动日志轮询（针对该任务组）
                startLogPolling(runningJob);

            } catch (Exception e) {
                System.err.println("触发任务执行失败: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    logPanel.error("✗ 任务执行失败: " + e.getMessage());
                    // 清理失败的任务组
                    runningJob.cleanup();
                    runningJobs.remove(currentJobId);
                    updateToolBarRunningJobs();
                    // 恢复边的正常状态
                    canvas.setAllConnectionsRunning(false);
                    // 断开WebSocket连接
                    WebSocketService.getInstance().disconnect(currentJobId, randomId);
                });
            }
        }).start();
    }

    /**
     * 处理WebSocket消息
     * @param message WebSocket消息
     * @param expectedRandomId 期望的randomId（用于验证消息）
     */
    private void handleWebSocketMessage(WebSocketService.WebSocketMessage message, String expectedRandomId) {
        System.out.println("========================================");
        System.out.println("📨 收到WebSocket消息");
        System.out.println("   消息randomId: " + message.getRandomId());
        System.out.println("   期望randomId: " + expectedRandomId);
        System.out.println("   消息jobId: " + message.getJobId());
        System.out.println("   消息status: " + message.getStatus());
        System.out.println("   消息result: " + message.getResult());
        
        logPanel.info("========================================");
        logPanel.info("📨 收到WebSocket消息");
        logPanel.info("   消息randomId: " + message.getRandomId());
        logPanel.info("   期望randomId: " + expectedRandomId);
        logPanel.info("   消息jobId: " + message.getJobId());
        logPanel.info("   消息status: " + message.getStatus());
        logPanel.info("   消息result: " + message.getResult());
        
        // 处理连接错误消息（status=-1表示连接错误）
        if (message.getStatus() != null && message.getStatus() == -1) {
            String errorMsg = "❌ WebSocket连接错误: " + message.getResult();
            System.err.println(errorMsg);
            logPanel.error(errorMsg);
            return;
        }
        
        // 验证randomId是否匹配
        if (message.getRandomId() != null && !expectedRandomId.equals(message.getRandomId())) {
            String warnMsg = "⚠️ WebSocket消息randomId不匹配，忽略: " + message.getRandomId() + " != " + expectedRandomId;
            System.out.println(warnMsg);
            System.out.println("========================================");
            logPanel.warn(warnMsg);
            logPanel.info("========================================");
            return;
        }

        Long jobId = message.getJobId();
        Integer status = message.getStatus();

        System.out.println("✅ randomId匹配，开始处理消息");
        System.out.println("📊 准备更新节点状态: jobId=" + jobId + ", status=" + status);
        
        logPanel.info("✅ randomId匹配，开始处理消息");
        logPanel.info("📊 准备更新节点状态: jobId=" + jobId + ", status=" + status);

        // 更新节点状态（必须在JavaFX线程中执行）
        if (jobId != null && status != null) {
            System.out.println("✅ jobId和status都不为空，准备更新节点状态");
            logPanel.info("✅ jobId和status都不为空，准备更新节点状态");
            Platform.runLater(() -> {
                System.out.println("🔄 在JavaFX线程中更新节点状态");
                logPanel.info("🔄 在JavaFX线程中更新节点状态");
                canvas.updateNodeStatusByJobId(jobId, status);
            });
        } else {
            String warnMsg = "⚠️ jobId或status为null，无法更新节点状态";
            System.out.println(warnMsg);
            System.out.println("   jobId: " + jobId);
            System.out.println("   status: " + status);
            logPanel.warn(warnMsg);
            logPanel.warn("   jobId: " + jobId);
            logPanel.warn("   status: " + status);
        }

        // 如果状态是5（任务完成），只恢复边的正常状态，但保留节点状态
        if (status != null && status == 5) {
            System.out.println("🏁 任务完成（status=5），恢复边的正常状态，保留节点状态");
            logPanel.info("🏁 任务完成（status=5），恢复边的正常状态，保留节点状态");
            Platform.runLater(() -> {
                // 只恢复边的运行状态（停止虚线动画），不重置节点状态
                canvas.setAllConnectionsRunning(false);
                // 注意：不重置节点状态，让节点保持最终状态（成功/失败）
            });
        }
        
        System.out.println("========================================");
        logPanel.info("========================================");
    }

    /**
     * 启动日志轮询（针对特定任务组）
     */
    private void startLogPolling(RunningJobGroup runningJob) {
        // 清理旧的定时器（如果存在）
        if (runningJob.getLogTimer() != null) {
            runningJob.getLogTimer().cancel();
        }

        java.util.Timer logTimer = new java.util.Timer("LogPollingTimer-" + runningJob.getJobId(), true);
        runningJob.setLogTimer(logTimer);

        logTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                fetchExecutionLog(runningJob);
            }
        }, 1000, 2000); // 1秒后开始，每2秒轮询一次
    }

    /**
     * 获取执行日志（针对特定任务组）
     */
    private void fetchExecutionLog(RunningJobGroup runningJob) {
        if (runningJob.getLogId() == null) {
            return;
        }

        // 防止无限轮询
        if (runningJob.getPullFailCount() > 20) {
            stopLogPolling(runningJob, "日志加载完成");
            return;
        }

        try {
            JobLogService.LogDetailResponse response = jobLogService.getLogDetail(
                    runningJob.getLogId(),
                    runningJob.getFromLineNum()
            );

            if (response != null && response.isSuccess()) {
                JobLogService.LogContent content = response.getContent();

                if (content == null) {
                    runningJob.setPullFailCount(runningJob.getPullFailCount() + 1);
                    return;
                }

                // 检查行号是否匹配
                if (runningJob.getFromLineNum() != content.getFromLineNum()) {
                    return;
                }

                // 检查是否有新日志
                if (runningJob.getFromLineNum() > content.getToLineNum()) {
                    if (content.isEnd()) {
                        stopLogPolling(runningJob, "任务执行完成");
                    }
                    return;
                }

                // 更新行号
                runningJob.setFromLineNum(content.getToLineNum() + 1);
                runningJob.setPullFailCount(0);

                // 获取日志内容
                String logContent = content.getLogContent();
                if (logContent != null && !logContent.isEmpty()) {
                    // 在UI线程中更新日志
                    Platform.runLater(() -> {
                        // 转换日志内容（处理特殊字符）
                        String processedLog = convertLogContent(logContent);
                        logPanel.appendText(runningJob.getJobId(), "[" + runningJob.getJobName() + "] " + processedLog);
                    });
                }

                // 检查是否结束
                if (content.isEnd()) {
                    stopLogPolling(runningJob, "任务执行完成");
                }

            } else {
                runningJob.setPullFailCount(runningJob.getPullFailCount() + 1);
                if (response != null) {
                    System.err.println("获取日志失败: " + response.getMsg());
                }
            }

        } catch (Exception e) {
            System.err.println("获取执行日志失败: " + e.getMessage());
            runningJob.setPullFailCount(runningJob.getPullFailCount() + 1);
        }
    }

    /**
     * 停止日志轮询（针对特定任务组）
     */
    private void stopLogPolling(RunningJobGroup runningJob, String message) {
        runningJob.cleanup();

        Platform.runLater(() -> {
            logPanel.info(runningJob.getJobId(), "════════════════════════════════");
            logPanel.success(runningJob.getJobId(), "✓ " + runningJob.getJobName() + " " + message);
            logPanel.info(runningJob.getJobId(), "════════════════════════════════");

            // 从运行列表中移除
            runningJobs.remove(runningJob.getJobId());
            
            // 更新导航栏中的小绿点（任务完成）
            String jobNameStr = getJobNameById(runningJob.getJobId());
            if (jobNameStr != null) {
                navigationBar.updateTaskGroupRunningStatus(jobNameStr, false);
            }
            
            updateToolBarRunningJobs();

            // 恢复边的正常状态（停止虚线动画）
            canvas.setAllConnectionsRunning(false);

            // 断开WebSocket连接
            WebSocketService.getInstance().disconnect(runningJob.getJobId(), runningJob.getRandomId());
            
            // ⭐ 任务执行完成后，立即同步所有节点状态到数据库
            canvas.syncPendingNodeStatus();
            logPanel.info(runningJob.getJobId(), "已触发节点状态批量同步");

            // ⚠️ 重要：不重置节点状态，让节点保持最终状态（成功/失败）
            // 节点状态会在以下情况重置：
            // 1. 下次任务启动时（triggerJobExecution）
            // 2. 切换任务组时（loadTaskGroupData -> clear）
            // 3. 手动停止任务时（stopJobExecution）
        });
    }

    /**
     * 转换日志内容
     * 将特殊字符转换为可读格式
     */
    private String convertLogContent(String content) {
        if (content == null) {
            return "";
        }

        // 替换一些特殊字符
        content = content.replace("\r\n", "\n");
        content = content.replace("\r", "\n");

        return content;
    }

    /**
     * 停止任务执行（针对特定任务组）
     */
    private void stopJobExecution(Long jobId) {
        if (jobId == null || jobId == 0) {
            logPanel.warn("⚠ 无效的任务组ID");
            return;
        }

        RunningJobGroup runningJob = runningJobs.get(jobId);
        if (runningJob == null || !runningJob.isRunning()) {
            logPanel.warn("⚠ 任务组 " + jobId + " 当前没有运行");
            return;
        }

        if (runningJob.getRandomId() == null) {
            logPanel.warn("⚠ 未找到执行批次ID");
            return;
        }

        logPanel.info("正在停止任务组: " + runningJob.getJobName() + " (ID: " + jobId + ")...");

        // 在后台线程中停止任务
        new Thread(() -> {
            try {
                jobInfoService.stopJobCompose(jobId, runningJob.getRandomId());

                Platform.runLater(() -> {
                    logPanel.success("✓ 任务组 " + runningJob.getJobName() + " 已停止");

                    // 清理该任务组的状态
                    runningJob.cleanup();
                    runningJobs.remove(jobId);
                    
                    // 更新导航栏中的小绿点（任务停止）
                    String jobNameStr = getJobNameById(jobId);
                    if (jobNameStr != null) {
                        navigationBar.updateTaskGroupRunningStatus(jobNameStr, false);
                    }

                    // 更新工具栏显示
                    updateToolBarRunningJobs();

                    // 恢复边的正常状态
                    canvas.setAllConnectionsRunning(false);

                    // 断开WebSocket连接
                    WebSocketService.getInstance().disconnect(jobId, runningJob.getRandomId());

                    // 重置所有节点状态为空闲
                    for (ProcessNode node : canvas.getNodes()) {
                        node.updateStatus(ProcessNode.NodeStatus.IDLE);
                    }
                });

            } catch (Exception e) {
                System.err.println("停止任务失败: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    logPanel.error("✗ 停止任务失败: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 更新工具栏显示运行中的任务组
     */
    private void updateToolBarRunningJobs() {
        if (toolBar != null) {
            // 过滤出正在运行的任务组
            java.util.Map<Long, RunningJobGroup> activeJobs = new java.util.HashMap<>();
            for (java.util.Map.Entry<Long, RunningJobGroup> entry : runningJobs.entrySet()) {
                if (entry.getValue().isRunning()) {
                    activeJobs.put(entry.getKey(), entry.getValue());
                }
            }
            toolBar.updateRunningJobs(activeJobs);
        }

        // 同时更新导航栏
        if (navigationBar != null) {
            // 过滤出正在运行的任务组
            java.util.Map<Long, RunningJobGroup> activeJobs = new java.util.HashMap<>();
            for (java.util.Map.Entry<Long, RunningJobGroup> entry : runningJobs.entrySet()) {
                if (entry.getValue().isRunning()) {
                    activeJobs.put(entry.getKey(), entry.getValue());
                }
            }
            navigationBar.updateRunningJobs(activeJobs);
        }
    }

    /**
     * 根据任务组ID获取任务组名称
     */
    private String getJobNameById(Long jobId) {
        // 从导航栏中查找
        String name = navigationBar.getTaskNameById(jobId);
        if (name != null) {
            return name;
        }

        // 从映射中查找
        for (java.util.Map.Entry<String, Long> entry : taskGroupNameToIdMap.entrySet()) {
            if (entry.getValue().equals(jobId)) {
                return entry.getKey();
            }
        }

        return null;
    }
    
    /**
     * 检查并更新任务组运行状态（显示/隐藏小绿点）
     */
    private void checkAndUpdateTaskGroupRunningStatus(Long taskId, String taskGroupName) {
        // 在后台线程调用API
        new Thread(() -> {
            try {
                boolean isRunning = jobInfoService.getJobStatus(taskId);
                System.out.println("📊 任务组 " + taskId + " (" + taskGroupName + ") 运行状态: " + isRunning);
                
                // 更新导航栏中的小绿点
                navigationBar.updateTaskGroupRunningStatus(taskGroupName, isRunning);
                
                if (isRunning) {
                    logPanel.debug("✅ 任务组 " + taskGroupName + " 正在运行中");
                } else {
                    logPanel.debug("⚪ 任务组 " + taskGroupName + " 未运行");
                }
            } catch (Exception e) {
                System.err.println("❌ 获取任务组运行状态失败: " + e.getMessage());
                logPanel.warn("获取任务组运行状态失败: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 获取页面Store的Helper方法（临时实现）
     */
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

        public void removePage(Long pageId) {
            // 实现页面移除逻辑
        }

        public void getLastPage() {
            // 实现获取最后一个页面的逻辑
        }
    }

    // 页面Store单例
    private static final PageStoreHelper pageStoreHelper = new PageStoreHelper();

    /**
     * 获取页面Store
     */
    private PageStoreHelper usePageStoreHook() {
        return pageStoreHelper;
    }

    /**
     * 保存或更新任务组
     * 学习Vue的saveOrUpdateJob方法实现
     */
    private void saveOrUpdateJob() {
        logPanel.info("════════════════════════════════");
        logPanel.info("💾 开始保存任务组数据...");

        // 1. 获取当前任务组ID
        Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
        if (currentTaskGroupId == null || currentTaskGroupId == 0) {
            logPanel.error("✗ 无法保存：未选择任务组");
            logPanel.warn("提示: 请先在导航栏选择要保存的任务组");
            logPanel.info("════════════════════════════════");
            return;
        }

        logPanel.info("当前任务组ID: " + currentTaskGroupId);

        // 2. 获取画布上的所有节点和连接
        List<ProcessNode> nodes = canvas.getNodes();
        List<NodeConnection> connections = canvas.getConnections();

        logPanel.info(String.format("节点数量: %d, 连接数量: %d", nodes.size(), connections.size()));

        // 3. 转换为后端需要的格式
        try {
            // 3.1 转换节点数据
            List<java.util.Map<String, Object>> nodesData = new java.util.ArrayList<>();
            for (ProcessNode node : nodes) {
                java.util.Map<String, Object> nodeData = new java.util.HashMap<>();
                nodeData.put("id", node.getNodeId());
                nodeData.put("type", node.getType() != null ? node.getType() : "rect");
                nodeData.put("x", node.getX());
                nodeData.put("y", node.getY());

                // text字段
                java.util.Map<String, Object> text = new java.util.HashMap<>();
                text.put("value", node.getJobHandlerName());
                nodeData.put("text", text);

                // properties字段
                java.util.Map<String, Object> properties = new java.util.HashMap<>();
                // ⭐ 传递jobId，后端需要这个字段
                if (node.getJobId() != null) {
                    properties.put("jobId", node.getJobId());
                }

                // ⭐ 添加节点宽度和高度（后端需要这些字段）
                properties.put("width", (int) node.getWidth());
                properties.put("height", (int) node.getHeight());

                // ⭐ 将显示类型转换为后端GlueType（如 "Bean" -> "BEAN"）
                String glueType = convertNodeTypeToGlueType(node.getType());
                properties.put("glueType", glueType);

                nodeData.put("properties", properties);

                nodesData.add(nodeData);
            }

            // 3.2 转换连接数据
            List<java.util.Map<String, Object>> edgesData = new java.util.ArrayList<>();
            for (NodeConnection conn : connections) {
                java.util.Map<String, Object> edgeData = new java.util.HashMap<>();

                // 为每条边生成一个唯一ID
                String edgeId = String.valueOf(System.currentTimeMillis() + edgesData.size());
                edgeData.put("id", edgeId);
                edgeData.put("type", "bezier");
                edgeData.put("sourceNodeId", conn.getSourceNode().getNodeId());
                edgeData.put("targetNodeId", conn.getTargetNode().getNodeId());

                // 锚点信息
                String sourceAnchor = conn.getSourceConnector() != null ?
                        getAnchorPosition(conn.getSourceNode(), conn.getSourceConnector()) : "right";
                String targetAnchor = conn.getTargetConnector() != null ?
                        getAnchorPosition(conn.getTargetNode(), conn.getTargetConnector()) : "left";
                edgeData.put("sourceAnchor", sourceAnchor);
                edgeData.put("targetAnchor", targetAnchor);

                edgesData.add(edgeData);
            }

            // 4. 将节点和边转换为JSON字符串
            String nodesJson = apiUtil.getGson().toJson(nodesData);
            String edgesJson = apiUtil.getGson().toJson(edgesData);

            logPanel.info("✓ 数据转换完成");
            System.out.println("Nodes JSON: " + nodesJson);
            System.out.println("Edges JSON: " + edgesJson);

            // 5. 在后台线程中保存到数据库
            new Thread(() -> {
                try {
                    logPanel.info("正在获取任务组表单数据...");

                    // 获取当前任务组的表单数据
                    com.cc.job.xo.model.form.JobInfoForm formData =
                            jobInfoService.getFormData(currentTaskGroupId);

                    if (formData == null) {
                        Platform.runLater(() -> {
                            logPanel.error("✗ 获取任务组数据失败");
                            logPanel.info("════════════════════════════════");
                        });
                        return;
                    }

                    // 更新nodes和edges字段
                    formData.setNodes(nodesJson);
                    formData.setEdges(edgesJson);

                    // 设置任务组必需的字段（参考Vue代码）
                    formData.setGlueType("BEAN");
                    formData.setExecutorHandler("runJobGroupXxlJob");

                    // 确保必填字段不为空
                    if (formData.getExecutorRouteStrategy() == null || formData.getExecutorRouteStrategy().isEmpty()) {
                        formData.setExecutorRouteStrategy("FIRST"); // 默认路由策略
                    }

                    // 清除时间字段，避免格式不匹配错误
                    // 这些字段由后端自动管理，不需要前端设置
                    formData.setGlueUpdatetime(null);

                    logPanel.info("正在保存到数据库...");

                    // 调用更新接口
                    boolean success = jobInfoService.updateJobCompose(currentTaskGroupId, formData);

                    if (success) {
                        Platform.runLater(() -> {
                            logPanel.success("✓ 保存成功！");
                            logPanel.info("节点: " + nodes.size() + ", 连接: " + connections.size());
                            logPanel.info("正在刷新任务树...");

                            // 刷新任务树
                            refreshTreeView();

                            logPanel.info("════════════════════════════════");
                        });
                    } else {
                        Platform.runLater(() -> {
                            logPanel.error("✗ 保存失败");
                            logPanel.info("════════════════════════════════");
                        });
                    }

                } catch (Exception e) {
                    System.err.println("保存任务组失败: " + e.getMessage());
                    e.printStackTrace();

                    Platform.runLater(() -> {
                        logPanel.error("✗ 保存失败: " + e.getMessage());
                        logPanel.warn("提示: 请检查后端服务是否正常运行");
                        logPanel.info("════════════════════════════════");
                    });
                }
            }).start();

        } catch (Exception e) {
            System.err.println("转换数据失败: " + e.getMessage());
            e.printStackTrace();
            logPanel.error("✗ 数据转换失败: " + e.getMessage());
            logPanel.info("════════════════════════════════");
        }
    }

    /**
     * 获取连接点的位置（顶部/底部/左侧/右侧）
     */
    private String getAnchorPosition(ProcessNode node, javafx.scene.shape.Circle connector) {
        if (connector == node.getTopConnector()) {
            return "top";
        } else if (connector == node.getBottomConnector()) {
            return "bottom";
        } else if (connector == node.getLeftConnector()) {
            return "left";
        } else if (connector == node.getRightConnector()) {
            return "right";
        }
        return "right"; // 默认右侧
    }

    /**
     * 显示新建分区对话框
     */
    private void showNewPartitionDialog() {
        try {
            // 获取当前窗口
            javafx.stage.Window window = this.getScene().getWindow();
            javafx.stage.Stage ownerStage = (javafx.stage.Stage) window;

            // 创建并显示对话框
            NewPartitionDialog dialog = new NewPartitionDialog(ownerStage);
            java.util.Optional<String> result = dialog.showAndWait();

            // 处理结果
            result.ifPresent(partitionName -> {
                logPanel.info("════════════════════════════════");
                logPanel.info("📝 创建新分区: " + partitionName);

                // 在后台线程中保存分区
                new Thread(() -> {
                    try {
                        boolean success = jobPartService.saveJobPart(partitionName);

                        if (success) {
                            Platform.runLater(() -> {
                                logPanel.success("✓ 分区创建成功: " + partitionName);
                                logPanel.info("正在刷新任务树...");

                                // 刷新任务树
                                refreshTreeView();
                            });
                        } else {
                            Platform.runLater(() -> {
                                logPanel.error("✗ 分区创建失败");
                            });
                        }

                    } catch (Exception e) {
                        System.err.println("保存分区失败: " + e.getMessage());
                        e.printStackTrace();

                        Platform.runLater(() -> {
                            logPanel.error("✗ 保存分区失败: " + e.getMessage());
                            logPanel.warn("提示: 请检查后端服务是否正常运行");
                        });
                    } finally {
                        Platform.runLater(() -> {
                            logPanel.info("════════════════════════════════");
                        });
                    }
                }).start();
            });

        } catch (Exception e) {
            System.err.println("显示新建分区对话框失败: " + e.getMessage());
            e.printStackTrace();
            logPanel.error("✗ 打开对话框失败: " + e.getMessage());
        }
    }

    /**
     * 刷新任务树视图
     */
    private void refreshTreeView() {
        if (treeView != null) {
            treeView.refreshTreeData();
            logPanel.success("✓ 任务树刷新成功");
        }
    }

    /**
     * 刷新任务树但不触发导航（保持当前选中状态）
     */
    private void refreshTreeViewWithoutNavigation(Long currentTaskGroupId) {
        if (treeView != null) {
            // 临时禁用选择回调
            treeView.setSelectionCallback(null);

            // 刷新树数据
            treeView.refreshTreeData();

            // 延迟一段时间后重新启用回调并恢复选中状态
            new Thread(() -> {
                try {
                    Thread.sleep(500); // 等待树数据加载完成

                    Platform.runLater(() -> {
                        // 重新设置选择回调
                        setupTreeViewCallback();

                        // 尝试重新选中当前任务组（避免触发重新加载）
                        if (currentTaskGroupId != null) {
                            suppressNextTaskLoad = true;
                            treeView.selectTaskGroupById(currentTaskGroupId);
                        }

                        logPanel.success("✓ 任务树刷新成功（保持当前页面）");
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * 设置树视图选择回调（提取为独立方法以便重用）
     */
    private void setupTreeViewCallback() {
        treeView.setSelectionCallback(new TaskTreeView.TaskSelectionCallback() {
            @Override
            public void onTaskSelected(String taskName) {
                // 兼容旧的回调
                onTaskSelected(null, taskName, null);
            }

            @Override
            public void onTaskSelected(Long taskId, String taskName, Integer type) {
                // 新的回调方法
                onTaskSelectedWithDetails(taskId, taskName, type);
            }

            @Override
            public void onNewJobGroup(Long partitionId, String partitionName) {
                System.out.println("新建任务组 - 分区ID: " + partitionId + ", 分区名称: " + partitionName);
                showNewJobGroupDialog(partitionId, partitionName, null);
            }

            @Override
            public void onNewJobNode(Long taskGroupId, String taskGroupName) {
                System.out.println("新建任务节点 - 任务组ID: " + taskGroupId + ", 任务组名称: " + taskGroupName);
                showNewJobNodeDialog(taskGroupId, taskGroupName, null);
            }

            @Override
            public void onJobNodeAction(Long jobNodeId, Long jobId, String nodeName, TaskTreeView.TaskSelectionCallback.JobNodeAction action) {
                if (jobId == null) {
                    logPanel.warn("⚠ 该树节点缺少任务ID，无法执行操作: " + nodeName);
                    return;
                }

                ProcessNode targetNode = canvas.getNodeByJobId(jobId);
                if (targetNode == null) {
                    logPanel.warn("⚠ 在画布上未找到任务节点: " + nodeName + " (jobId=" + jobId + ")");
                    return;
                }

                switch (action) {
                    case EDIT -> editNode(jobId, targetNode);
                    case LOCATE -> locateNodeOnCanvas(targetNode);
                    default -> { }
                }
            }

            @Override
            public void onEdgeAction(Long edgeId, TaskTreeView.TaskSelectionCallback.EdgeAction action) {
                if (edgeId == null) {
                    logPanel.warn("⚠ 未提供边ID，无法执行操作");
                    return;
                }

                String edgeKey = String.valueOf(edgeId);
                switch (action) {
                    case DELETE -> {
                        boolean removed = canvas.removeConnectionByEdgeId(edgeKey);
                        if (removed) {
                            logPanel.success("✓ 已删除连接 " + edgeKey + "，请记得保存任务组以持久化修改");
                        } else {
                            logPanel.warn("⚠ 画布上未找到ID为 " + edgeKey + " 的连接");
                        }
                    }
                    case LOCATE -> {
                        boolean located = canvas.locateConnectionByEdgeId(edgeKey);
                        if (!located) {
                            logPanel.warn("⚠ 未在画布上找到该连接: " + edgeKey);
                        }
                    }
                    default -> { }
                }
            }
        });
    }

    /**
     * 显示新建/编辑任务组对话框
     */
    private void showNewJobGroupDialog(Long partitionId, String partitionName, com.cc.job.xo.model.form.JobInfoForm editData) {
        try {
            logPanel.info("════════════════════════════════");
            logPanel.info(editData == null ? "📝 新建任务组 - 分区: " + partitionName : "✏️ 编辑任务组");

            // 在后台线程中加载JobGroup列表
            new Thread(() -> {
                try {
                    java.util.List<com.cc.job.xo.model.entity.JobGroup> jobGroupList = jobGroupService.getAllJobGroupList();

                    Platform.runLater(() -> {
                        try {
                            // 获取当前窗口
                            javafx.stage.Window window = this.getScene().getWindow();
                            javafx.stage.Stage ownerStage = (javafx.stage.Stage) window;

                            // 创建并显示对话框
                            NewJobGroupDialog dialog = new NewJobGroupDialog(ownerStage, partitionId, editData, jobGroupList);
                            java.util.Optional<com.cc.job.xo.model.form.JobInfoForm> result = dialog.showAndWait();

                            // 处理结果
                            result.ifPresent(formData -> {
                                logPanel.info("开始保存任务组数据...");

                                // 在后台线程中保存
                                new Thread(() -> {
                                    try {
                                        boolean success;
                                        if (editData == null) {
                                            // 新建模式
                                            success = jobInfoService.saveJobCompose(formData);
                                        } else {
                                            // 编辑模式
                                            success = jobInfoService.updateJobCompose(editData.getId(), formData);
                                        }

                                        if (success) {
                                            Platform.runLater(() -> {
                                                logPanel.success(editData == null ? "✓ 任务组创建成功" : "✓ 任务组更新成功");
                                                logPanel.info("正在刷新任务树...");
                                                refreshTreeView();
                                            });
                                        } else {
                                            Platform.runLater(() -> {
                                                logPanel.error(editData == null ? "✗ 任务组创建失败" : "✗ 任务组更新失败");
                                            });
                                        }

                                    } catch (Exception e) {
                                        System.err.println("保存任务组失败: " + e.getMessage());
                                        e.printStackTrace();

                                        Platform.runLater(() -> {
                                            logPanel.error("✗ 保存失败: " + e.getMessage());
                                            logPanel.warn("提示: 请检查后端服务是否正常运行");
                                        });
                                    } finally {
                                        Platform.runLater(() -> {
                                            logPanel.info("════════════════════════════════");
                                        });
                                    }
                                }).start();
                            });

                        } catch (Exception e) {
                            System.err.println("显示新建任务组对话框失败: " + e.getMessage());
                            e.printStackTrace();
                            logPanel.error("✗ 打开对话框失败: " + e.getMessage());
                        }
                    });

                } catch (Exception e) {
                    System.err.println("加载执行器列表失败: " + e.getMessage());
                    e.printStackTrace();

                    Platform.runLater(() -> {
                        logPanel.error("✗ 加载执行器列表失败: " + e.getMessage());
                        logPanel.warn("提示: 请检查后端服务是否正常运行");
                        logPanel.info("════════════════════════════════");
                    });
                }
            }).start();

        } catch (Exception e) {
            System.err.println("显示新建任务组对话框失败: " + e.getMessage());
            e.printStackTrace();
            logPanel.error("✗ 打开对话框失败: " + e.getMessage());
            logPanel.info("════════════════════════════════");
        }
    }

    /**
     * 显示新建/编辑任务节点对话框
     */
    private void showNewJobNodeDialog(Long taskGroupId, String taskGroupName, com.cc.job.xo.model.form.JobInfoForm editData) {
        try {
            logPanel.info("════════════════════════════════");
            logPanel.info(editData == null ? "📝 新建任务节点 - 任务组: " + taskGroupName : "✏️ 编辑任务节点");

            // 在后台线程中加载JobGroup列表
            new Thread(() -> {
                try {
                    java.util.List<com.cc.job.xo.model.entity.JobGroup> jobGroupList = jobGroupService.getAllJobGroupList();

                    Platform.runLater(() -> {
                        try {
                            // 获取当前窗口
                            javafx.stage.Window window = this.getScene().getWindow();
                            javafx.stage.Stage ownerStage = (javafx.stage.Stage) window;

                            // 创建并显示对话框
                            NewJobNodeDialog dialog = new NewJobNodeDialog(ownerStage, taskGroupId, editData, jobGroupList);
                            java.util.Optional<com.cc.job.xo.model.form.JobInfoForm> result = dialog.showAndWait();

                            // 处理结果
                            result.ifPresent(formData -> {
                                logPanel.info("开始保存任务节点数据...");

                                // 在后台线程中保存
                                new Thread(() -> {
                                    try {
                                        com.cc.job.xo.model.entity.JobNode jobNode = jobInfoService.saveJobNode(formData);

                                        if (jobNode != null) {
                                            Platform.runLater(() -> {
                                                logPanel.success(editData == null ? "✓ 任务节点创建成功" : "✓ 任务节点更新成功");
                                                logPanel.info("节点ID: " + jobNode.getId() + ", 任务ID: " + jobNode.getJobId());

                                                // 在画布上添加节点
                                                addNodeToCanvas(jobNode, formData);

                                                // 刷新任务树但保持当前页面
                                                logPanel.info("正在刷新任务树...");
                                                refreshTreeViewWithoutNavigation(taskGroupId);
                                            });
                                        } else {
                                            Platform.runLater(() -> {
                                                logPanel.error(editData == null ? "✗ 任务节点创建失败" : "✗ 任务节点更新失败");
                                            });
                                        }

                                    } catch (Exception e) {
                                        System.err.println("保存任务节点失败: " + e.getMessage());
                                        e.printStackTrace();

                                        Platform.runLater(() -> {
                                            logPanel.error("✗ 保存失败: " + e.getMessage());
                                            logPanel.warn("提示: 请检查后端服务是否正常运行");
                                        });
                                    } finally {
                                        Platform.runLater(() -> {
                                            logPanel.info("════════════════════════════════");
                                        });
                                    }
                                }).start();
                            });

                        } catch (Exception e) {
                            System.err.println("显示新建任务节点对话框失败: " + e.getMessage());
                            e.printStackTrace();
                            logPanel.error("✗ 打开对话框失败: " + e.getMessage());
                        }
                    });

                } catch (Exception e) {
                    System.err.println("加载执行器列表失败: " + e.getMessage());
                    e.printStackTrace();

                    Platform.runLater(() -> {
                        logPanel.error("✗ 加载执行器列表失败: " + e.getMessage());
                        logPanel.warn("提示: 请检查后端服务是否正常运行");
                        logPanel.info("════════════════════════════════");
                    });
                }
            }).start();

        } catch (Exception e) {
            System.err.println("显示新建任务节点对话框失败: " + e.getMessage());
            e.printStackTrace();
            logPanel.error("✗ 打开对话框失败: " + e.getMessage());
            logPanel.info("════════════════════════════════");
        }
    }

    /**
     * 在画布上添加节点
     */
    private void addNodeToCanvas(com.cc.job.xo.model.entity.JobNode jobNode, com.cc.job.xo.model.form.JobInfoForm formData) {
        try {
            logPanel.info("正在画布上添加节点...");

            // 计算节点位置：如果后端没有返回坐标，则自动计算一个不重叠的位置
            double x, y;
            Double rawX = jobNode.getNodePositionX();
            Double rawY = jobNode.getNodePositionY();
            if (hasValidCoordinates(rawX, rawY)) {
                x = rawX;
                y = rawY;
            } else {
                // 自动计算位置，避免节点重叠
                double[] position = calculateNewNodePosition();
                x = position[0];
                y = position[1];
                logPanel.info("自动计算节点位置: (" + x + ", " + y + ")");
            }

            // 创建节点
            ProcessNode node = new ProcessNode(
                    String.valueOf(jobNode.getId()),  // 节点ID
                    formData.getJobDesc(),             // 节点标签（任务描述）
                    x,                                  // X坐标
                    y                                   // Y坐标
            );

            // ⭐ 设置任务ID（jobId）
            node.setJobId(jobNode.getJobId());

            // 设置节点类型图标
            String nodeType = getNodeTypeIcon(formData.getGlueType());
            node.setType(nodeType);

            // 添加节点到画布
            canvas.addNode(node, true);
            configureNodeCallbacks(node);

            logPanel.success("✓ 节点已添加到画布: " + formData.getJobDesc());
            logPanel.info("节点坐标: (" + x + ", " + y + ")");

        } catch (Exception e) {
            System.err.println("添加节点到画布失败: " + e.getMessage());
            e.printStackTrace();
            logPanel.error("✗ 添加节点到画布失败: " + e.getMessage());
        }
    }

    private void configureNodeCallbacks(ProcessNode node) {
        if (node == null) {
            return;
        }
        node.setOnEdit(() -> {
            Long jobId = node.getJobId();
            if (jobId == null) {
                logPanel.warn("⚠ 该节点未绑定后端任务，无法编辑");
                return;
            }
            editNode(jobId, node);
        });
        node.setOnCopy(() -> copyNode(node));
        node.setOnShowDetails(() -> showNodeDetails(node));
    }

    private void copyNode(ProcessNode sourceNode) {
        if (sourceNode == null) {
            logPanel.warn("⚠ 当前节点为空，无法复制");
            return;
        }
        Long sourceJobId = sourceNode.getJobId();
        if (sourceJobId == null) {
            logPanel.warn("⚠ 该节点未绑定后端任务，无法复制");
            return;
        }
        Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
        if (currentTaskGroupId == null) {
            logPanel.warn("⚠ 请先选择任务组后再复制节点");
            return;
        }

        logPanel.info("════════════════════════════════");
        logPanel.info("📋 正在复制节点: " + safeString(sourceNode.getJobHandlerName()));

        new Thread(() -> {
            try {
                JobInfoForm originalForm = jobInfoService.getJobNodeFormData(sourceJobId);
                if (originalForm == null) {
                    Platform.runLater(() -> logPanel.error("✗ 获取原节点数据失败"));
                    return;
                }

                JobInfoForm copyForm = deepCopyJobInfoForm(originalForm);
                if (copyForm == null) {
                    Platform.runLater(() -> logPanel.error("✗ 复制节点数据失败"));
                    return;
                }

                copyForm.setId(null);
                copyForm.setParentId(currentTaskGroupId);
                copyForm.setJobDesc(generateCopyName(copyForm.getJobDesc()));
                copyForm.setNodePositionX(sourceNode.getLayoutX() + 60);
                copyForm.setNodePositionY(sourceNode.getLayoutY() + 40);
                if (copyForm.getExecutorParam() == null) {
                    copyForm.setExecutorParam("");
                }

                com.cc.job.xo.model.entity.JobNode newJobNode = jobInfoService.saveJobNode(copyForm);
                if (newJobNode == null) {
                    Platform.runLater(() -> logPanel.error("✗ 复制节点失败：后端返回空数据"));
                    return;
                }

                if (copyForm.getNodePositionX() != null) {
                    newJobNode.setNodePositionX(copyForm.getNodePositionX());
                }
                if (copyForm.getNodePositionY() != null) {
                    newJobNode.setNodePositionY(copyForm.getNodePositionY());
                }

                Platform.runLater(() -> {
                    try {
                        addNodeToCanvas(newJobNode, copyForm);
                        refreshTreeViewWithoutNavigation(currentTaskGroupId);
                        logPanel.success("✓ 节点复制成功: " + copyForm.getJobDesc());
                    } catch (Exception e) {
                        logPanel.error("✗ 添加复制节点到画布失败: " + e.getMessage());
                    } finally {
                        logPanel.info("════════════════════════════════");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    logPanel.error("✗ 节点复制失败: " + e.getMessage());
                    logPanel.info("════════════════════════════════");
                });
            }
        }, "copy-node-thread").start();
    }

    private void showNodeDetails(ProcessNode node) {
        if (node == null) {
            logPanel.warn("⚠ 当前节点为空，无法查看详情");
            return;
        }
        Long jobId = node.getJobId();
        if (jobId == null) {
            logPanel.warn("⚠ 该节点未绑定后端任务，无法查看详情");
            return;
        }

        logPanel.info("════════════════════════════════");
        logPanel.info("🔍 正在获取节点详情: " + safeString(node.getJobHandlerName()));

        new Thread(() -> {
            try {
                JobInfoForm form = jobInfoService.getJobNodeFormData(jobId);
                List<JobGroup> jobGroups = jobGroupService.getAllJobGroupList();
                Platform.runLater(() -> {
                    showJobNodeDetailDialog(node, form, jobGroups);
                    logPanel.info("════════════════════════════════");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    logPanel.error("✗ 获取节点详情失败: " + e.getMessage());
                    logPanel.info("════════════════════════════════");
                });
            }
        }, "detail-node-thread").start();
    }

    private void showJobNodeDetailDialog(ProcessNode node, JobInfoForm form, List<JobGroup> jobGroups) {
        if (form == null) {
            logPanel.warn("⚠ 未获取到节点详情数据");
            return;
        }
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("节点详情 - " + safeString(node.getJobHandlerName()));
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));

        addDetailRow(grid, 0, "任务描述", form.getJobDesc());
        addDetailRow(grid, 1, "执行器", findJobGroupName(jobGroups, form.getJobGroup()));
        addDetailRow(grid, 2, "JobHandler", form.getExecutorHandler());
        addDetailRow(grid, 3, "运行模式", form.getGlueType());
        addDetailRow(grid, 4, "任务参数", form.getExecutorParam());
        addDetailRow(grid, 5, "负责人", form.getAuthor());
        addDetailRow(grid, 6, "报警邮件", form.getAlarmEmail());
        addDetailRow(grid, 7, "阻塞策略", form.getExecutorBlockStrategy());
        addDetailRow(grid, 8, "超时时间(秒)", form.getExecutorTimeout());
        addDetailRow(grid, 9, "失败重试次数", form.getExecutorFailRetryCount());
        addDetailRow(grid, 10, "所属任务组ID", form.getParentId());
        addDetailRow(grid, 11, "Job ID", node.getJobId());

        javafx.scene.control.TextArea advancedArea = new javafx.scene.control.TextArea(buildAdvancedDetailText(form));
        advancedArea.setEditable(false);
        advancedArea.setWrapText(true);
        advancedArea.setPrefRowCount(8);

        javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(12, grid, advancedArea);
        container.setPadding(new javafx.geometry.Insets(10));

        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().setPrefHeight(520);
        dialog.showAndWait();
    }

    private void addDetailRow(javafx.scene.layout.GridPane grid, int rowIndex, String label, Object value) {
        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(label + "：");
        nameLabel.setStyle("-fx-text-fill: #4B5563; -fx-font-size: 13; -fx-font-weight: 600;");
        javafx.scene.control.Label valueLabel = new javafx.scene.control.Label(safeString(value));
        valueLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 13;");
        valueLabel.setWrapText(true);
        grid.add(nameLabel, 0, rowIndex);
        grid.add(valueLabel, 1, rowIndex);
    }

    private String buildAdvancedDetailText(JobInfoForm form) {
        StringBuilder sb = new StringBuilder();
        appendDetailLine(sb, "调度类型", form.getScheduleType());
        appendDetailLine(sb, "调度配置", form.getScheduleConf());
        appendDetailLine(sb, "调度过期策略", form.getMisfireStrategy());
        appendDetailLine(sb, "路由策略", form.getExecutorRouteStrategy());
        appendDetailLine(sb, "请求类型", form.getReqType());
        appendDetailLine(sb, "请求地址", form.getReqUrl());
        appendDetailLine(sb, "请求头", form.getReqHeader());
        appendDetailLine(sb, "请求体", form.getReqBody());
        appendDetailLine(sb, "节点X坐标", form.getNodePositionX());
        appendDetailLine(sb, "节点Y坐标", form.getNodePositionY());
        appendDetailLine(sb, "子任务", form.getChildJobid());
        appendDetailLine(sb, "增量类型", form.getIncrType());
        appendDetailLine(sb, "增量内容", form.getIncrContent());
        return sb.length() == 0 ? "暂无更多配置信息" : sb.toString();
    }

    private void appendDetailLine(StringBuilder sb, String label, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String str && str.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(label).append(": ").append(value);
    }

    private String findJobGroupName(List<JobGroup> jobGroups, Long id) {
        if (id == null) {
            return "未设置";
        }
        if (jobGroups == null || jobGroups.isEmpty()) {
            return "ID: " + id;
        }
        return jobGroups.stream()
                .filter(group -> id.equals(group.getId()))
                .map(JobGroup::getTitle)
                .findFirst()
                .orElse("ID: " + id);
    }

    private JobInfoForm deepCopyJobInfoForm(JobInfoForm original) {
        if (original == null) {
            return null;
        }
        String json = apiUtil.getGson().toJson(original);
        return apiUtil.getGson().fromJson(json, JobInfoForm.class);
    }

    private String generateCopyName(String originalName) {
        String base = (originalName == null || originalName.trim().isEmpty())
                ? "新任务"
                : originalName.trim();

        java.util.Set<String> existingNames = new java.util.HashSet<>();
        for (ProcessNode node : canvas.getNodes()) {
            if (node.getJobHandlerName() != null) {
                existingNames.add(node.getJobHandlerName());
            }
        }

        String candidate = base + "_copy";
        int index = 2;
        while (existingNames.contains(candidate)) {
            candidate = base + "_copy" + index;
            index++;
        }
        return candidate;
    }

    private String safeString(Object value) {
        if (value == null) {
            return "无";
        }
        String str = String.valueOf(value);
        return str.isBlank() ? "无" : str;
    }

    /**
     * 计算新节点的位置，避免与现有节点重叠
     * @return [x, y] 坐标数组
     */
    private double[] calculateNewNodePosition() {
        List<ProcessNode> existingNodes = canvas.getNodes();

        // 如果画布上没有节点，返回起始位置
        if (existingNodes == null || existingNodes.isEmpty()) {
            return new double[]{100, 100};
        }

        // 节点的默认尺寸和间距（与ProcessNode保持一致）
        final double NODE_WIDTH = 180;
        final double NODE_HEIGHT = 80;
        final double HORIZONTAL_SPACING = 100;  // 水平间距
        final double VERTICAL_SPACING = 100;    // 垂直间距
        final int NODES_PER_ROW = 4;            // 每行最多节点数

        // 计算当前节点数量，决定在第几行第几列
        int nodeCount = existingNodes.size();
        int row = nodeCount / NODES_PER_ROW;
        int col = nodeCount % NODES_PER_ROW;

        // 计算新节点的位置（网格布局）
        double x = 100 + col * (NODE_WIDTH + HORIZONTAL_SPACING);
        double y = 100 + row * (NODE_HEIGHT + VERTICAL_SPACING);

        // 确保位置不与任何现有节点重叠
        while (isPositionOccupied(x, y, existingNodes, NODE_WIDTH, NODE_HEIGHT)) {
            // 如果位置被占用，尝试下一个位置
            col++;
            if (col >= NODES_PER_ROW) {
                col = 0;
                row++;
            }
            x = 100 + col * (NODE_WIDTH + HORIZONTAL_SPACING);
            y = 100 + row * (NODE_HEIGHT + VERTICAL_SPACING);
        }

        return new double[]{x, y};
    }

    /**
     * 判断后端返回的坐标是否有效（过滤为0或异常值的坐标）
     */
    private boolean hasValidCoordinates(Double x, Double y) {
        if (!isCoordinateNumber(x) || !isCoordinateNumber(y)) {
            return false;
        }
        // 当后端未初始化坐标时通常返回(0,0)，视为无效
        return Math.abs(x) + Math.abs(y) > 1e-3;
    }

    private boolean isCoordinateNumber(Double value) {
        if (value == null) {
            return false;
        }
        return !value.isNaN() && !value.isInfinite();
    }

    /**
     * 检查指定位置是否被其他节点占用
     */
    private boolean isPositionOccupied(double x, double y, List<ProcessNode> existingNodes, double width, double height) {
        final double OVERLAP_THRESHOLD = 20; // 允许的最小间距

        for (ProcessNode node : existingNodes) {
            double nodeX = node.getX();
            double nodeY = node.getY();

            // 检查是否在矩形区域内重叠
            boolean xOverlap = Math.abs(x - nodeX) < (width + OVERLAP_THRESHOLD);
            boolean yOverlap = Math.abs(y - nodeY) < (height + OVERLAP_THRESHOLD);

            if (xOverlap && yOverlap) {
                return true; // 位置被占用
            }
        }

        return false; // 位置可用
    }

    /**
     * 为加载的节点设置编辑回调
     * @param composeData 任务组合数据
     */
    private void setupEditCallbacksForLoadedNodes(JobComposeData composeData) {
        if (composeData == null || composeData.getNodes() == null) {
            logPanel.warn("⚠ composeData 或节点数据为 null，无法设置编辑回调");
            return;
        }

        List<ProcessNode> loadedNodes = canvas.getNodes();
        if (loadedNodes == null || loadedNodes.isEmpty()) {
            logPanel.warn("⚠ 画布节点列表为空，无法设置编辑回调");
            return;
        }

        logPanel.info("开始为 " + composeData.getNodes().size() + " 个节点设置编辑回调");

        // 为每个节点设置编辑回调
        int callbackSetCount = 0;
        for (JobComposeData.NodeData nodeData : composeData.getNodes()) {
            String nodeId = nodeData.getId();
            Long jobId = nodeData.getJobId();  // 这是任务ID

            System.out.println("检查节点 - nodeId: " + nodeId + ", jobId: " + jobId);

            // 跳过没有 jobId 的节点
            if (nodeId == null || jobId == null) {
                logPanel.warn("⚠ 节点 " + nodeId + " 缺少 jobId，跳过设置编辑回调");
                continue;
            }

            // 在画布节点列表中找到对应的节点
            loadedNodes.stream()
                    .filter(node -> node.getNodeId() != null && node.getNodeId().equals(nodeId))
                    .findFirst()
                    .ifPresent(node -> {
                        node.setJobId(jobId);
                        configureNodeCallbacks(node);
                        System.out.println("✓ 已为节点 " + nodeId + " (jobId: " + jobId + ") 设置回调");
                    });
            callbackSetCount++;
        }

        logPanel.info("✓ 完成编辑回调设置，共设置 " + callbackSetCount + " 个节点");
    }

    /**
     * 编辑节点
     * @param jobId 任务ID
     * @param node 画布上的节点对象
     */
    private void editNode(Long jobId, ProcessNode node) {
        try {
            logPanel.info("════════════════════════════════");
            logPanel.info("📝 编辑任务节点 - 任务ID: " + jobId + ", 节点ID: " + node.getNodeId());

            if (jobId == null) {
                logPanel.error("✗ jobId 为 null，无法编辑");
                return;
            }

            // 获取当前任务组ID
            Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
            logPanel.info("当前任务组ID: " + currentTaskGroupId);

            if (currentTaskGroupId == null) {
                logPanel.error("✗ 无法获取当前任务组ID");
                return;
            }

            // 在后台线程中获取节点数据
            new Thread(() -> {
                try {
                    // 获取节点表单数据
                    com.cc.job.xo.model.form.JobInfoForm formData = jobInfoService.getJobNodeFormData(jobId);

                    if (formData == null) {
                        Platform.runLater(() -> {
                            logPanel.error("✗ 无法获取节点数据");
                        });
                        return;
                    }

                    // 获取执行器列表
                    List<JobGroup> jobGroupList = jobGroupService.getAllJobGroupList();

                    Platform.runLater(() -> {
                        logPanel.info("正在打开编辑对话框...");

                        // 显示编辑对话框
                        NewJobNodeDialog dialog = new NewJobNodeDialog(
                                (Stage) getScene().getWindow(),
                                currentTaskGroupId,
                                formData,  // 传入现有数据进行编辑
                                jobGroupList
                        );

                        dialog.showAndWait().ifPresent(updatedFormData -> {
                            // 用户点击了保存按钮
                            logPanel.info("正在更新任务节点...");

                            // 在后台线程中更新
                            new Thread(() -> {
                                try {
                                    boolean success = jobInfoService.updateJobNode(jobId, updatedFormData);

                                    if (success) {
                                        Platform.runLater(() -> {
                                            logPanel.success("✓ 任务节点更新成功");

                                            // 更新画布上的节点显示
                                            String newNodeType = getNodeTypeIcon(updatedFormData.getGlueType());
                                            node.updateNodeInfo(updatedFormData.getJobDesc(), newNodeType);
                                            logPanel.info("✓ 画布节点已更新");

                                            if (treeView != null && node.getJobId() != null) {
                                                treeView.updateJobNode(node.getJobId(), updatedFormData.getJobDesc());
                                            }

                                            // 刷新任务树但保持当前页面
                                            logPanel.info("正在刷新任务树...");
                                            refreshTreeViewWithoutNavigation(currentTaskGroupId);
                                        });
                                    } else {
                                        Platform.runLater(() -> {
                                            logPanel.error("✗ 任务节点更新失败");
                                        });
                                    }

                                } catch (Exception e) {
                                    System.err.println("更新任务节点失败: " + e.getMessage());
                                    e.printStackTrace();

                                    Platform.runLater(() -> {
                                        logPanel.error("✗ 更新失败: " + e.getMessage());
                                        logPanel.warn("提示: 请检查后端服务是否正常运行");
                                    });
                                } finally {
                                    Platform.runLater(() -> {
                                        logPanel.info("════════════════════════════════");
                                    });
                                }
                            }).start();
                        });
                    });

                } catch (Exception e) {
                    System.err.println("获取节点数据失败: " + e.getMessage());
                    e.printStackTrace();

                    Platform.runLater(() -> {
                        logPanel.error("✗ 获取节点数据失败: " + e.getMessage());
                        logPanel.info("════════════════════════════════");
                    });
                }
            }).start();

        } catch (Exception e) {
            System.err.println("打开编辑对话框失败: " + e.getMessage());
            e.printStackTrace();
            logPanel.error("✗ 打开编辑对话框失败: " + e.getMessage());
            logPanel.info("════════════════════════════════");
        }
    }

    /**
     * 根据GlueType获取节点类型图标
     */
    private String getNodeTypeIcon(String glueType) {
        if (glueType == null) return "Bean";

        switch (glueType) {
            case "BEAN":
                return "Bean";
            case "API":
                return "API";
            case "SQL":
                return "SQL";
            case "GLUE_GROOVY":
                return "Java";
            case "GLUE_SHELL":
                return "Shell";
            case "GLUE_PYTHON":
                return "Python";
            case "GLUE_PHP":
                return "PHP";
            case "GLUE_NODEJS":
                return "Node";
            case "GLUE_POWERSHELL":
                return "PS";
            default:
                return "Bean";
        }
    }

    /**
     * 将节点显示类型转换为后端GlueType
     * 用于保存时将前端显示类型转回后端期望的大写格式
     */
    private String convertNodeTypeToGlueType(String nodeType) {
        if (nodeType == null) return "BEAN";

        switch (nodeType) {
            case "Bean":
                return "BEAN";
            case "API":
                return "API";
            case "SQL":
                return "SQL";
            case "Java":
                return "GLUE_GROOVY";
            case "Shell":
                return "GLUE_SHELL";
            case "Python":
                return "GLUE_PYTHON";
            case "PHP":
                return "GLUE_PHP";
            case "Node":
                return "GLUE_NODEJS";
            case "PS":
                return "GLUE_POWERSHELL";
            default:
                return "BEAN";
        }
    }

    private void removeNode(ProcessNode node) {
        if (node == null) {
            return;
        }

        canvas.removeNode(node);
        logPanel.info("🗑️ 已从画布移除节点: " + node.getJobHandlerName());
    }

    private void locateNodeOnCanvas(ProcessNode node) {
        if (node == null) {
            return;
        }

        Bounds viewport = scrollPane.getViewportBounds();
        Bounds contentBounds = canvas.getBoundsInLocal();

        double contentWidth = contentBounds.getWidth();
        double contentHeight = contentBounds.getHeight();

        Bounds nodeBounds = node.getBoundsInParent();
        double nodeCenterX = nodeBounds.getMinX() + nodeBounds.getWidth() / 2;
        double nodeCenterY = nodeBounds.getMinY() + nodeBounds.getHeight() / 2;

        double hMax = Math.max(contentWidth - viewport.getWidth(), 1);
        double vMax = Math.max(contentHeight - viewport.getHeight(), 1);

        double targetH = (nodeCenterX - viewport.getWidth() / 2) / hMax;
        double targetV = (nodeCenterY - viewport.getHeight() / 2) / vMax;

        scrollPane.setHvalue(clampScrollValue(targetH));
        scrollPane.setVvalue(clampScrollValue(targetV));

        node.toFront();
        node.playLocateAnimation();
    }

    private double clampScrollValue(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0;
        }
        return Math.max(0, Math.min(1, value));
    }
}

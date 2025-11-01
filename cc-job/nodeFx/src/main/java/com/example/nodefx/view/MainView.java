package com.example.nodefx.view;

import com.example.nodefx.model.JobComposeData;
import com.example.nodefx.model.ProcessNode;
import com.example.nodefx.service.JobPartService;
import com.example.nodefx.service.JobInfoService;
import com.example.nodefx.service.JobLogService;
import com.example.nodefx.util.DetachablePanel;
import com.example.nodefx.util.SnowflakeIdGenerator;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.transform.Scale;

import java.io.IOException;

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
    
    private javafx.scene.layout.VBox leftArea;
    private boolean treeViewVisible = true;
    private boolean miniMapVisible = true;
    
    private ScrollPane scrollPane;
    private double currentZoom = 1.0;
    private static final double ZOOM_STEP = 0.1;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 3.0;
    
    // API 服务
    private final JobPartService jobPartService;
    private final JobInfoService jobInfoService;
    private final JobLogService jobLogService;
    
    // 可分离面板管理器
    private DetachablePanel treeViewDetachable;
    private DetachablePanel miniMapDetachable;
    private DetachablePanel logPanelDetachable;
    
    // 任务组名称到ID的映射
    private java.util.Map<String, Long> taskGroupNameToIdMap = new java.util.HashMap<>();
    
    // 雪花算法ID生成器
    private final SnowflakeIdGenerator snowflake = SnowflakeIdGenerator.getInstance();
    
    // 任务执行状态
    private String currentRandomId = null;
    private Long currentLogId = null;
    private java.util.Timer logTimer = null;
    private int fromLineNum = 0;
    private int pullFailCount = 0;
    private boolean isRunning = false;
    
    public MainView() {
        this.jobPartService = new JobPartService();
        this.jobInfoService = new JobInfoService();
        this.jobLogService = new JobLogService();
        initializeUI();
        setupCallbacks();
    }
    
    private void initializeUI() {
        // 顶部工具栏
        toolBar = new TopToolBar();
        this.setTop(toolBar);
        
        // 创建折叠侧边栏（始终显示）
        collapsedSidebar = new CollapsedSidebar();
        
        // 左侧内容区域：树形导航 + 小地图
        leftArea = new javafx.scene.layout.VBox();
        
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
        scrollPane = new ScrollPane(canvas);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setStyle("-fx-background-color: #F3F4F6;");
        scrollPane.setPannable(true);
        
        // 设置滚动条策略：只在需要时显示
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        // 创建画布区域容器：导航栏 + 画布
        javafx.scene.layout.VBox canvasArea = new javafx.scene.layout.VBox();
        canvasArea.getChildren().addAll(navigationBar, scrollPane);
        javafx.scene.layout.VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        
        // 日志面板
        logPanel = new LogPanel();
        
        // 创建垂直分割面板：画布区域和日志面板
        SplitPane verticalSplit = new SplitPane();
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getItems().addAll(canvasArea, logPanel);
        verticalSplit.setDividerPositions(0.7); // 初始位置：70% 给画布，30% 给日志
        
        // 创建水平分割面板：左侧容器和右侧（画布+日志）
        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.setOrientation(Orientation.HORIZONTAL);
        horizontalSplit.getItems().addAll(leftContainer, verticalSplit);
        horizontalSplit.setDividerPositions(0.2); // 初始位置：20% 给左侧，80% 给右侧
        
        // 绑定小地图到画布
        miniMap.bindTo(canvas, scrollPane);
        
        this.setCenter(horizontalSplit);
        
        // 添加示例节点
        addSampleNodes();
        
        // 初始化左侧边栏状态（重要！确保图标正确显示/隐藏）
        updateLeftSidebar();
    }
    
    private void setupCallbacks() {
        // 树形视图关闭回调
        treeView.setOnClose(() -> {
            treeViewVisible = false;
            updateLeftSidebar();
        });
        
        // 小地图关闭回调
        miniMap.setOnClose(() -> {
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
        });
        
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
                loadTaskGroupData(taskId, taskGroupName);
            } else {
                logPanel.warn("⚠ 未找到任务组ID，无法加载流程图: " + taskGroupName);
                logPanel.info("提示: 请先在左侧任务树中选择该任务组");
            }
        });
        
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
                canvas.clear();
                logPanel.info("创建新的流程图");
            }
            
            @Override
            public void onOpen() {
                logPanel.info("打开流程图功能开发中...");
            }
            
            @Override
            public void onSave() {
                int nodeCount = canvas.getNodes().size();
                int connCount = canvas.getConnections().size();
                logPanel.success(String.format("保存成功！节点: %d, 连接: %d", nodeCount, connCount));
            }
            
            @Override
            public void onUndo() {
                logPanel.info("撤销操作功能开发中...");
            }
            
            @Override
            public void onRedo() {
                logPanel.info("重做操作功能开发中...");
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
                triggerJobExecution();
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
        navigationBar.addOrSelectTask(taskName);
        logPanel.info("选择节点: " + taskName + " [类型: " + getTypeNameByType(type) + "]");
        
        // 只有任务组（type=1）才加载流程图
        if (type != null && type == 1 && taskId != null) {
            // 更新任务组名称到ID的映射
            taskGroupNameToIdMap.put(taskName, taskId);
            // 更新当前选中的任务组ID
            usePageStoreHook().setCurrentPage(taskId);
            loadTaskGroupData(taskId, taskName);
        } else {
            logPanel.info("提示: 只有任务组节点才能展示流程图");
        }
    }
    
    /**
     * 加载任务组数据
     */
    private void loadTaskGroupData(Long taskId, String taskName) {
        logPanel.info("════════════════════════════════");
        logPanel.info("开始加载任务组: " + taskName);
        logPanel.info("任务组ID: " + taskId);
        
        // 在后台线程中加载数据
        new Thread(() -> {
            try {
                JobComposeData composeData = jobPartService.getJobCompose(taskId);
                
                // 在 JavaFX 主线程中更新 UI
                Platform.runLater(() -> {
                    if (composeData != null) {
                        canvas.loadFromComposeData(composeData);
                        
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
        
        canvas.addNode(node1);
        canvas.addNode(node2);
        canvas.addNode(node3);
        
        canvas.addConnection(node1, node1.getRightConnector(), node3, node3.getLeftConnector());
        canvas.addConnection(node1, node1.getBottomConnector(), node2, node2.getTopConnector());
        
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
        // 获取当前选中的任务组
        Long currentJobId = usePageStoreHook().getCurrentPage();
        if (currentJobId == null || currentJobId == 0) {
            logPanel.warn("⚠ 请先选择一个任务组");
            return;
        }
        
        // 检查是否正在运行
        if (isRunning) {
            logPanel.warn("⚠ 任务组正在运行中，请稍后再试");
            return;
        }
        
        // 清理旧的状态
        cleanupRunningState();
        
        // 生成新的randomId
        currentRandomId = snowflake.nextIdStr();
        fromLineNum = 0;
        pullFailCount = 0;
        isRunning = true;
        
        logPanel.info("════════════════════════════════");
        logPanel.success("✨ 开始执行任务组 ID: " + currentJobId);
        logPanel.info("执行批次ID: " + currentRandomId);
        logPanel.info("════════════════════════════════");
        
        // 在后台线程中执行任务
        new Thread(() -> {
            try {
                // 调用后端API触发任务
                Long logId = jobInfoService.triggerJob(currentJobId, currentRandomId);
                currentLogId = logId;
                
                Platform.runLater(() -> {
                    logPanel.success("✓ 任务已提交，日志ID: " + logId);
                    logPanel.info("开始获取执行日志...");
                });
                
                // 启动日志轮询
                startLogPolling();
                
            } catch (Exception e) {
                System.err.println("触发任务执行失败: " + e.getMessage());
                e.printStackTrace();
                
                Platform.runLater(() -> {
                    logPanel.error("✗ 任务执行失败: " + e.getMessage());
                    isRunning = false;
                });
            }
        }).start();
    }
    
    /**
     * 启动日志轮询
     */
    private void startLogPolling() {
        if (logTimer != null) {
            logTimer.cancel();
        }
        
        logTimer = new java.util.Timer("LogPollingTimer", true);
        logTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                fetchExecutionLog();
            }
        }, 1000, 2000); // 1秒后开始，每2秒轮询一次
    }
    
    /**
     * 获取执行日志
     */
    private void fetchExecutionLog() {
        if (currentLogId == null) {
            return;
        }
        
        // 防止无限轮询
        if (pullFailCount > 20) {
            stopLogPolling("日志加载完成");
            return;
        }
        
        try {
            JobLogService.LogDetailResponse response = jobLogService.getLogDetail(currentLogId, fromLineNum);
            
            if (response != null && response.isSuccess()) {
                JobLogService.LogContent content = response.getContent();
                
                if (content == null) {
                    pullFailCount++;
                    return;
                }
                
                // 检查行号是否匹配
                if (fromLineNum != content.getFromLineNum()) {
                    return;
                }
                
                // 检查是否有新日志
                if (fromLineNum > content.getToLineNum()) {
                    if (content.isEnd()) {
                        stopLogPolling("任务执行完成");
                    }
                    return;
                }
                
                // 更新行号
                fromLineNum = content.getToLineNum() + 1;
                pullFailCount = 0;
                
                // 获取日志内容
                String logContent = content.getLogContent();
                if (logContent != null && !logContent.isEmpty()) {
                    // 在UI线程中更新日志
                    Platform.runLater(() -> {
                        // 转换日志内容（处理特殊字符）
                        String processedLog = convertLogContent(logContent);
                        logPanel.appendText(processedLog);
                    });
                }
                
                // 检查是否结束
                if (content.isEnd()) {
                    stopLogPolling("任务执行完成");
                }
                
            } else {
                pullFailCount++;
                if (response != null) {
                    System.err.println("获取日志失败: " + response.getMsg());
                }
            }
            
        } catch (Exception e) {
            System.err.println("获取执行日志失败: " + e.getMessage());
            pullFailCount++;
        }
    }
    
    /**
     * 停止日志轮询
     */
    private void stopLogPolling(String message) {
        if (logTimer != null) {
            logTimer.cancel();
            logTimer = null;
        }
        
        Platform.runLater(() -> {
            logPanel.info("════════════════════════════════");
            logPanel.success("✓ " + message);
            logPanel.info("════════════════════════════════");
            isRunning = false;
        });
    }
    
    /**
     * 清理运行状态
     */
    private void cleanupRunningState() {
        if (logTimer != null) {
            logTimer.cancel();
            logTimer = null;
        }
        currentRandomId = null;
        currentLogId = null;
        fromLineNum = 0;
        pullFailCount = 0;
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
     * 停止任务执行
     */
    private void stopJobExecution() {
        Long currentJobId = usePageStoreHook().getCurrentPage();
        if (currentJobId == null || currentJobId == 0) {
            logPanel.warn("⚠ 请先选择一个任务组");
            return;
        }
        
        if (!isRunning) {
            logPanel.warn("⚠ 当前没有运行的任务");
            return;
        }
        
        if (currentRandomId == null) {
            logPanel.warn("⚠ 未找到执行批次ID");
            return;
        }
        
        logPanel.info("正在停止任务...");
        
        // 在后台线程中停止任务
        new Thread(() -> {
            try {
                jobInfoService.stopJobCompose(currentJobId, currentRandomId);
                
                Platform.runLater(() -> {
                    logPanel.success("✓ 任务已停止");
                    cleanupRunningState();
                    isRunning = false;
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
}

package com.cc.job.gui.view;

import com.cc.job.gui.history.UndoRedoManager;
import com.cc.job.gui.model.*;
import com.cc.job.gui.service.JobGroupService;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.service.JobLogService;
import com.cc.job.gui.service.JobPartService;
import com.cc.job.gui.service.SSEService;
import com.cc.job.gui.util.*;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobInfoForm;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * 主界面视图
 */
public class MainView extends BorderPane {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

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
    private boolean logPanelVisible = true;
    private boolean suppressNextTaskLoad = false;
    // 一次性抑制树选中同步（用于容器点击但希望维持容器节点高亮）
    private boolean suppressNextTreeSelectionSync = false;

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
    
    // 复制粘贴相关：存储复制的节点数据
    private com.cc.job.xo.model.form.JobInfoForm copiedNodeForm = null; // 兼容单节点复制
    
    // 多节点复制粘贴数据结构
    private static class CopiedNodesData {
        List<NodeFormData> nodeForms = new java.util.ArrayList<>();
        List<ConnectionInfo> connections = new java.util.ArrayList<>();
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        
        static class NodeFormData {
            com.cc.job.xo.model.form.JobInfoForm form;
            Long originalJobId; // 原始jobId，用于匹配连接关系
            String originalNodeId; // 原始节点ID
        }
        
        static class ConnectionInfo {
            Long sourceJobId; // 源节点jobId
            Long targetJobId; // 目标节点jobId
            String sourceAnchor; // 源锚点位置
            String targetAnchor; // 目标锚点位置
        }
    }
    private CopiedNodesData copiedNodesData = null;
    
    // 任务组克隆过程中防重入标识（防止重复创建两遍）
    private final java.util.concurrent.atomic.AtomicBoolean cloningTaskGroupInProgress = new java.util.concurrent.atomic.AtomicBoolean(false);

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
        this.setStyle("-fx-background-color: #F1F5F9;");
        
        // 顶部工具栏
        toolBar = new TopToolBar();
        this.setTop(toolBar);

        // 创建折叠侧边栏（始终显示）
        collapsedSidebar = new CollapsedSidebar();

        // 左侧内容区域：树形导航 + 小地图
        leftArea = new javafx.scene.layout.VBox();
        leftArea.setSpacing(8);
        leftArea.setPadding(new Insets(0)); // 确保没有左边距
        leftArea.setStyle("-fx-background-color: transparent;");

        // 树形导航
        treeView = new TaskTreeView();
        javafx.scene.layout.VBox.setVgrow(treeView, javafx.scene.layout.Priority.ALWAYS);

        // 小地图
        miniMap = new MiniMapView();

        leftArea.getChildren().addAll(treeView, miniMap);

        // 创建左侧容器：折叠栏 + 内容区域
        HBox leftContainer = new HBox();
        leftContainer.setSpacing(0); // 移除间距，确保分割线紧贴左边
        HBox.setMargin(leftArea, new Insets(0));
        leftContainer.setPadding(new Insets(0)); // 确保容器没有padding
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
        // 注册页面右键菜单回调
        canvas.setOnRequestAddNode(() -> {
            Long taskGroupId = usePageStoreHook().getCurrentTaskGroupId();
            if (taskGroupId == null || taskGroupId == 0) {
                logPanel.warn("⚠ 请先选择任务组，再新增节点");
                return;
            }
            String taskGroupName = getJobNameById(taskGroupId);
            showNewJobNodeDialog(taskGroupId, taskGroupName != null ? taskGroupName : ("任务组 " + taskGroupId), null);
        });
        canvas.setOnRequestSelectTaskGroup(() -> {
            try {
                // 打开级联弹窗：分区 -> 任务组
                javafx.stage.Window window = this.getScene().getWindow();
                javafx.stage.Stage ownerStage = (javafx.stage.Stage) window;
                java.util.Optional<SelectTaskGroupDialog.Selection> result = SelectTaskGroupDialog.showDialog(ownerStage);
                result.ifPresent(sel -> {
                    // 拉取该任务组的流程数据，作为子任务渲染到任务组容器中
                    new Thread(() -> {
                        try {
                            // 防重入：正在克隆则直接忽略本次请求
                            if (!cloningTaskGroupInProgress.compareAndSet(false, true)) {
                                Platform.runLater(() -> logPanel.warn("⚠ 正在处理上一次“选择任务组”，请稍候..."));
                                return;
                            }
                            JobComposeData compose = jobPartService.getJobCompose(sel.taskGroupId);
                            // 在后台执行：复制每个子任务为数据库新节点，并保存连线
                            new Thread(() -> {
                                try {
                                    Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
                                    if (currentTaskGroupId == null) {
                                        Platform.runLater(() -> logPanel.error("✗ 未选择当前任务组，无法保存"));
                                        return;
                                    }
                                    // 0) 先创建“任务组容器节点”（jobType=2，isNode=Y），作为一个特殊的组节点
                                    final Long[] groupContainerJobIdRef = new Long[1];
                                    try {
                                        JobInfoForm parentForm = jobInfoService.getFormData(currentTaskGroupId);
                                        if (parentForm != null) {
                                            JobInfoForm groupNodeForm = new JobInfoForm();
                                            groupNodeForm.setParentId(currentTaskGroupId);
                                            groupNodeForm.setJobGroup(parentForm.getJobGroup());
                                            groupNodeForm.setJobDesc(sel.taskGroupName);
                                            groupNodeForm.setAuthor(parentForm.getAuthor());
                                            groupNodeForm.setAlarmEmail(parentForm.getAlarmEmail());
                                            groupNodeForm.setScheduleType(parentForm.getScheduleType());
                                            groupNodeForm.setScheduleConf(parentForm.getScheduleConf());
                                            groupNodeForm.setMisfireStrategy(parentForm.getMisfireStrategy());
                                            groupNodeForm.setExecutorRouteStrategy(
                                                parentForm.getExecutorRouteStrategy() != null ? parentForm.getExecutorRouteStrategy() : "FIRST");
                                            groupNodeForm.setExecutorBlockStrategy(
                                                parentForm.getExecutorBlockStrategy() != null ? parentForm.getExecutorBlockStrategy() : "SERIAL_EXECUTION");
                                            groupNodeForm.setExecutorTimeout(parentForm.getExecutorTimeout());
                                            groupNodeForm.setExecutorFailRetryCount(parentForm.getExecutorFailRetryCount());
                                            groupNodeForm.setJobType(2);
                                            double[] basePos = calculateNewNodePosition();
                                            groupNodeForm.setNodePositionX(basePos[0]);
                                            groupNodeForm.setNodePositionY(basePos[1]);
                                            // 设为 CUSTOM_GROUP，后端 NODE_TYPE_MAP 映射为 custom-group
                                            groupNodeForm.setGlueType("CUSTOM_GROUP");
                                            groupNodeForm.setExecutorHandler("runJobGroupXxlJob");
                                            groupNodeForm.setGlueUpdatetime(null);
                                            com.cc.job.xo.model.entity.JobNode savedGroupNode = jobInfoService.saveJobNode(groupNodeForm);
                                            if (savedGroupNode != null) {
                                                groupContainerJobIdRef[0] = savedGroupNode.getJobId();
                                            }
                                        }
                                    } catch (Exception e) {
                                    }
                                    
                                    // 1) 先创建节点（持久化），记录 oldNodeId -> new ProcessNode
                                    java.util.Map<String, ProcessNode> idToNode = new java.util.HashMap<>();
                                    java.util.List<ProcessNode> newNodes = new java.util.ArrayList<>();
                                    
                                    // ⭐ 修复：先找到任务组节点的位置，用于计算子节点的相对位置
                                    // 任务组节点的位置可能在 jobNode 中，或者在 nodes 中
                                    double originalGroupX = 0;
                                    double originalGroupY = 0;
                                    boolean foundGroupNode = false;
                                    
                                    // 首先检查 jobNode（任务组节点本身）
                                    if (compose != null && compose.getJobNode() != null) {
                                        JobComposeData.NodeData jobNode = compose.getJobNode();
                                        if (jobNode.getX() != null && jobNode.getY() != null) {
                                            originalGroupX = jobNode.getX();
                                            originalGroupY = jobNode.getY();
                                            foundGroupNode = true;
                                        }
                                    }
                                    
                                    // 如果jobNode中没有，再从nodes中查找
                                    if (!foundGroupNode && compose != null && compose.getNodes() != null) {
                                        for (JobComposeData.NodeData nd : compose.getNodes()) {
                                            // 检查是否是任务组节点（通过type或properties中的children判断）
                                            boolean isGroupNode = false;
                                            if (nd.getType() != null && 
                                                (nd.getType().equals("CustomGroup") || nd.getType().equalsIgnoreCase("custom-group"))) {
                                                isGroupNode = true;
                                            } else if (nd.getProperties() != null) {
                                                // properties 已经是 Map 类型，直接使用
                                                if (nd.getProperties().containsKey("children")) {
                                                    isGroupNode = true;
                                                }
                                            }
                                            
                                            if (isGroupNode && nd.getX() != null && nd.getY() != null) {
                                                originalGroupX = nd.getX();
                                                originalGroupY = nd.getY();
                                                foundGroupNode = true;
                                                break;
                                            }
                                        }
                                    }
                                    
                                    // 如果还是没找到，使用子节点的最小X和Y作为参考点
                                    if (!foundGroupNode && compose != null && compose.getNodes() != null) {
                                        double minX = Double.MAX_VALUE;
                                        double minY = Double.MAX_VALUE;
                                        for (JobComposeData.NodeData nd : compose.getNodes()) {
                                            if (nd.getX() != null && nd.getY() != null) {
                                                minX = Math.min(minX, nd.getX());
                                                minY = Math.min(minY, nd.getY());
                                            }
                                        }
                                        if (minX != Double.MAX_VALUE && minY != Double.MAX_VALUE) {
                                            // 使用子节点的最小位置减去一个偏移量作为任务组节点的位置
                                            originalGroupX = minX - 50;
                                            originalGroupY = minY - 50;
                                            foundGroupNode = true;
                                        }
                                    }
                                    
                                    if (compose != null && compose.getNodes() != null) {
                                        java.util.Set<Long> processedOriginalJobIds = new java.util.HashSet<>();
                                        int index = 0;
                                        for (JobComposeData.NodeData nd : compose.getNodes()) {
                                            Long originalJobId = nd.getJobId();
                                            if (originalJobId == null) {
                                                continue;
                                            }
                                            // 去重：同一次克隆中，原jobId只处理一次
                                            if (!processedOriginalJobIds.add(originalJobId)) {
                                                continue;
                                            }
                                            // 拉取原节点表单并克隆
                                            JobInfoForm originalForm = jobInfoService.getJobNodeFormData(originalJobId);
                                            if (originalForm == null) {
                                                continue;
                                            }
                                            JobInfoForm copyForm = deepCopyJobInfoForm(originalForm);
                                            if (copyForm == null) {
                                                continue;
                                            }
                                            // 设置归属与基础信息
                                            copyForm.setId(null);
                                            // 子节点父ID：优先挂到“任务组节点”jobId；若组节点创建失败，则退化为当前任务组ID
                                            Long effectiveParentId = groupContainerJobIdRef[0] != null ? groupContainerJobIdRef[0] : currentTaskGroupId;
                                            copyForm.setParentId(effectiveParentId);
                                            String label = nd.getJobName() != null ? nd.getJobName() : originalForm.getJobDesc();
                                            copyForm.setJobDesc(label);
                                            // ⭐ 修复：判断是否是任务组节点（声明为final以便在lambda中使用）
                                            final boolean isGroupNode;
                                            if (nd.getType() != null && 
                                                (nd.getType().equals("CustomGroup") || nd.getType().equalsIgnoreCase("custom-group"))) {
                                                isGroupNode = true;
                                            } else if (nd.getProperties() != null) {
                                                // properties 已经是 Map 类型，直接使用
                                                isGroupNode = nd.getProperties().containsKey("children");
                                            } else {
                                                isGroupNode = false;
                                            }
                                            
                                            // 计算新任务组节点的位置
                                            double[] base = calculateNewNodePosition();
                                            double newGroupX = base[0];
                                            double newGroupY = base[1];
                                            
                                            // 计算子节点相对于原始任务组节点的偏移量
                                            double nx = nd.getX() != null ? nd.getX() : 60 + index * 20;
                                            double ny = nd.getY() != null ? nd.getY() : 60 + index * 14;
                                            double offsetX = foundGroupNode ? (nx - originalGroupX) : 0;
                                            double offsetY = foundGroupNode ? (ny - originalGroupY) : 0;
                                            
                                            // 保持相对位置
                                            copyForm.setNodePositionX(newGroupX + offsetX);
                                            copyForm.setNodePositionY(newGroupY + offsetY);
                                            
                                            // ⭐ 修复：如果是任务组节点，需要设置任务组相关属性
                                            if (isGroupNode) {
                                                // 设置为任务组类型
                                                copyForm.setJobType(2);
                                                copyForm.setGlueType("CUSTOM_GROUP");
                                                copyForm.setExecutorHandler("runJobGroupXxlJob");
                                            }
                                            
                                            // 其他必填默认
                                            if (copyForm.getExecutorRouteStrategy() == null || copyForm.getExecutorRouteStrategy().isEmpty()) {
                                                copyForm.setExecutorRouteStrategy("FIRST");
                                            }
                                            // 避免 LocalDateTime 反序列化格式错误（后端自行维护该时间）
                                            copyForm.setGlueUpdatetime(null);
                                            
                                            // 保存为新节点（包括任务组节点本身）
                                            com.cc.job.xo.model.entity.JobNode saved = jobInfoService.saveJobNode(copyForm);
                                            
                                            // ⭐ 修复：如果是任务组节点，递归处理子节点（在保存节点之后）
                                            if (isGroupNode && saved != null) {
                                                // 递归获取并保存嵌套的任务组节点的子节点
                                                try {
                                                    // 获取嵌套任务组的数据
                                                    JobComposeData nestedCompose = jobPartService.getJobCompose(originalJobId);
                                                    if (nestedCompose != null && nestedCompose.getNodes() != null) {
                                                        // 递归处理嵌套任务组的子节点
                                                        java.util.Map<String, ProcessNode> nestedIdToNode = new java.util.HashMap<>();
                                                        java.util.List<ProcessNode> nestedNewNodes = new java.util.ArrayList<>();
                                                        
                                                        // 递归复制嵌套任务组的子节点
                                                        copyNestedTaskGroupNodes(
                                                            nestedCompose, 
                                                            saved.getJobId(), 
                                                            newGroupX + offsetX, 
                                                            newGroupY + offsetY,
                                                            nestedIdToNode,
                                                            nestedNewNodes,
                                                            new java.util.HashSet<>() // 用于去重的已处理jobId集合
                                                        );
                                                        
                                                        // 将嵌套的节点也添加到主节点映射中
                                                        idToNode.putAll(nestedIdToNode);
                                                        newNodes.addAll(nestedNewNodes);
                                                    }
                                                } catch (Exception e) {
                                                    logger.error("递归复制嵌套任务组节点失败: jobId={}, error={}", originalJobId, e.getMessage(), e);
                                                }
                                            }
                                            if (saved != null) {
                                                // 在UI线程添加到画布
                                                final String nodeIdStr = String.valueOf(saved.getId());
                                                final Long newJobId = saved.getJobId();
                                                final String title = label;
                                                final double px = copyForm.getNodePositionX() != null ? copyForm.getNodePositionX() : base[0];
                                                final double py = copyForm.getNodePositionY() != null ? copyForm.getNodePositionY() : base[1];
                                                Platform.runLater(() -> {
                                                    ProcessNode node = new ProcessNode(nodeIdStr, title, px, py);
                                                    node.setJobId(newJobId);
                                                    // ⭐ 修复：如果是任务组节点，设置正确的类型
                                                    if (isGroupNode) {
                                                        node.setType("CustomGroup");
                                                    } else {
                                                        node.setType("Bean");
                                                    }
                                                    canvas.addNode(node, true);
                                                    configureNodeCallbacks(node);
                                                    idToNode.put(nd.getId(), node);
                                                    newNodes.add(node);
                                                });
                                            }
                                            index++;
                                        }
                                    }
                                    
                                    // 等待UI线程把节点渲染完成
                                    Thread.sleep(200);
                                    
                                    // 2) 创建连线（持久化 + 画布）
                                    java.util.List<NodeConnection> newConnections = new java.util.ArrayList<>();
                                    if (compose != null && compose.getEdges() != null) {
                                        for (JobComposeData.EdgeData ed : compose.getEdges()) {
                                            ProcessNode s = idToNode.get(ed.getSourceNodeId());
                                            ProcessNode t = idToNode.get(ed.getTargetNodeId());
                                            if (s != null && t != null && s.getNodeId() != null && t.getNodeId() != null) {
                                                // 保存到数据库
                                                try {
                                                    com.cc.job.xo.model.form.JobEdgeForm edgeForm = new com.cc.job.xo.model.form.JobEdgeForm();
                                                    Long effectiveParentId = groupContainerJobIdRef[0] != null ? groupContainerJobIdRef[0] : currentTaskGroupId;
                                                    edgeForm.setJobParentId(effectiveParentId);
                                                    edgeForm.setFromNodeId(Long.parseLong(s.getNodeId()));
                                                    edgeForm.setEndNodeId(Long.parseLong(t.getNodeId()));
                                                    edgeForm.setStartPoint("right");
                                                    edgeForm.setEndPoint("left");
                                                    jobInfoService.saveJobEdge(edgeForm);
                                                } catch (Exception ignore) {}
                                                // 添加到画布
                                                Platform.runLater(() -> {
                                                    NodeConnection c = canvas.addConnection(s, t);
                                                    if (c != null) {
                                                        newConnections.add(c);
                                                    }
                                                });
                                            }
                                        }
                                    }
                                    
                                    // 3) UI上创建容器并绑定"节点+连线"，支持收起隐藏连线
                                    Thread.sleep(120);
                                    Platform.runLater(() -> {
                                        com.cc.job.gui.model.GroupContainer container =
                                                new com.cc.job.gui.model.GroupContainer("group:" + sel.taskGroupId, sel.taskGroupId, sel.taskGroupName);
                                        container.bindCanvasNodes(newNodes);
                                        container.bindConnections(newConnections);
                                        // ⭐ 修复：添加容器到UI和集合，确保getGroupContainers()能正确返回
                                        canvas.getChildren().add(0, container);
                                        canvas.addGroupContainerToCollection(container);
                                        container.expand();
                                        canvas.selectNodes(newNodes);
                                        logPanel.success("✓ 已复制并持久化任务组: " + sel.taskGroupName);
                                    });
                                } catch (Exception ex) {
                                    logger.error("复制并保存任务组合失败: {}", ex.getMessage(), ex);
                                    Platform.runLater(() -> logPanel.error("✗ 复制并保存任务组失败: " + ex.getMessage()));
                                } finally {
                                    cloningTaskGroupInProgress.set(false);
                                    // ⚠️ 注意：不再自动保存，因为我们已经手动保存了每个节点和连线
                                    // 再次调用 saveOrUpdateJob() 会导致重复创建节点（后端 updateJobCompose 会重新创建）
                                }
                            }, "clone-and-save-taskgroup-thread").start();
                        } catch (Exception ex) {
                            logger.error("加载任务组流程失败: {}", ex.getMessage(), ex);
                            javafx.application.Platform.runLater(() ->
                                logPanel.error("✗ 加载任务组流程失败: " + ex.getMessage())
                            );
                        }
                    }).start();
                });
            } catch (Exception e) {
                logger.error("选择任务组失败: {}", e.getMessage(), e);
                logPanel.error("✗ 选择任务组失败: " + e.getMessage());
            }
        });
        canvas.setOnRequestClearCanvas(() -> {
            canvas.clearViewOnly();
            logPanel.warn("已清空页面（仅视图）。如需同步数据库，请点击保存。");
        });
        canvas.setOnRequestRunTaskGroup(this::triggerJobExecution);
        
        // 设置框选模式改变回调，更新工具栏按钮状态
        canvas.setOnSelectionModeChanged(isActive -> {
            if (toolBar != null) {
                toolBar.updateSelectionButtonState(isActive);
            }
        });
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
        canvasArea.setPadding(new Insets(0));
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
        verticalSplit.setPadding(new Insets(0, 12, 12, 4));

        // 创建水平分割面板：左侧容器和右侧（画布+日志）
        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.setOrientation(Orientation.HORIZONTAL);
        horizontalSplit.getItems().addAll(leftContainer, verticalSplit);
        horizontalSplit.setDividerPositions(0.2); // 初始位置：20% 给左侧，80% 给右侧
        horizontalSplit.setStyle("-fx-background-color: transparent;");
        horizontalSplit.setPadding(new Insets(0, 12, 12, 0)); // 左边距设为0，确保分割线从最左边开始

        // 绑定小地图到画布
        miniMap.bindTo(canvas, scrollPane);

        this.setCenter(horizontalSplit);

        // 添加示例节点
        addSampleNodes();

        // 初始化左侧边栏状态（重要！确保图标正确显示/隐藏）
        updateLeftSidebar();
        
        // 设置键盘快捷键
        setupKeyboardShortcuts();
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

        // 日志面板关闭回调
        logPanel.setOnClose(() -> {
            if (logPanelDetachable != null && logPanelDetachable.isDetached()) {
                logPanelDetachable.reattach();
            }
            logPanelVisible = false;
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

        collapsedSidebar.setOnLogPanelRestore(() -> {
            logPanelVisible = true;
            updateLeftSidebar();
        });

        // 初始化可分离面板
        setupDetachablePanels();

        // 树形视图回调
        setupTreeViewCallback();

        // 导航栏切换任务组回调
        navigationBar.setOnTaskSwitch((taskGroupName, taskGroupId) -> {
            logPanel.info("导航栏切换到任务组: " + taskGroupName);
            
            Long resolvedTaskId = null;
            
            // ⚠️ 关键修复：优先使用导航栏存储的ID（这是最准确的，因为它是在添加任务组时从树形视图直接获取的）
            if (taskGroupId != null) {
                // 验证导航栏存储的ID是否在树形视图中存在且名称匹配
                javafx.scene.control.TreeItem<TreeNodeData> taskGroupItem = treeView.findTreeItemById(taskGroupId);
                if (taskGroupItem != null) {
                    TreeNodeData nodeData = taskGroupItem.getValue();
                    if (nodeData != null && taskGroupName.equals(nodeData.getLabel()) && 
                        nodeData.getType() != null && nodeData.getType() == 1) {
                        // ID存在且名称匹配，使用这个ID
                        resolvedTaskId = taskGroupId;
                    } else {
                        // ID存在但名称不匹配，说明可能是旧的ID
                        logPanel.warn("⚠ 导航栏存储的ID和名称不匹配，重新查找...");
                    }
                } else {
                    // ID在树形视图中不存在，说明可能是旧的ID
                    logPanel.warn("⚠ 导航栏存储的ID在树形视图中不存在，重新查找...");
                }
            }
            
            // 如果导航栏没有存储ID或验证失败，从树形视图中查找ID
            if (resolvedTaskId == null) {
                Long foundId = findTaskGroupIdByName(taskGroupName);
                
                // 验证从树形视图中找到的ID
                if (foundId != null) {
                    javafx.scene.control.TreeItem<TreeNodeData> taskGroupItem = treeView.findTreeItemById(foundId);
                    if (taskGroupItem != null) {
                        TreeNodeData nodeData = taskGroupItem.getValue();
                        if (nodeData != null && taskGroupName.equals(nodeData.getLabel()) && 
                            nodeData.getType() != null && nodeData.getType() == 1) {
                            // ID存在且名称匹配，使用这个ID
                            resolvedTaskId = foundId;
                        } else {
                            // ID存在但名称不匹配，说明可能是旧的ID，清除它并重新查找
                            logPanel.warn("⚠ 检测到任务组ID和名称不匹配，重新查找...");
                        }
                    } else {
                        // ID在树形视图中不存在，说明可能是旧的ID，清除它
                        logPanel.warn("⚠ 检测到任务组ID在树形视图中不存在，重新查找...");
                    }
                }
            }
            
            // 如果从树形视图中找不到或验证失败，尝试从缓存中查找（兼容旧逻辑）
            if (resolvedTaskId == null) {
                Long cachedId = taskGroupNameToIdMap.get(taskGroupName);
                if (cachedId != null) {
                    // 验证缓存的ID是否在树形视图中存在且名称匹配
                    javafx.scene.control.TreeItem<TreeNodeData> cachedItem = treeView.findTreeItemById(cachedId);
                    if (cachedItem != null) {
                        TreeNodeData nodeData = cachedItem.getValue();
                        if (nodeData != null && taskGroupName.equals(nodeData.getLabel()) && 
                            nodeData.getType() != null && nodeData.getType() == 1) {
                            // 缓存的ID有效，使用它
                            resolvedTaskId = cachedId;
                        } else {
                            // 缓存的ID无效，清除它
                            logPanel.warn("⚠ 缓存的ID无效，已清除");
                            taskGroupNameToIdMap.remove(taskGroupName);
                        }
                    } else {
                        // 缓存的ID在树形视图中不存在，清除它
                        logPanel.warn("⚠ 缓存的ID在树形视图中不存在，已清除");
                        taskGroupNameToIdMap.remove(taskGroupName);
                    }
                }
            }
            
            final Long finalTaskId = resolvedTaskId; // 创建final变量用于lambda
            
            if (finalTaskId != null) {
                // 更新缓存（使用验证过的ID）
                taskGroupNameToIdMap.put(taskGroupName, finalTaskId);
                // 更新导航栏当前任务组ID（确保导航栏也存储了正确的ID）
                navigationBar.setCurrentTaskGroupId(finalTaskId);
                // 更新顶部工具栏当前任务组ID
                toolBar.setCurrentTaskGroupId(finalTaskId);
                // 更新页面Store
                usePageStoreHook().setCurrentPage(finalTaskId);
                loadTaskGroupData(finalTaskId, taskGroupName);
                
                // 同步树形视图的选中状态：选中对应的任务组
                // 使用 Platform.runLater 确保在 UI 更新后执行，避免时序问题
                javafx.application.Platform.runLater(() -> {
                    if (!suppressNextTreeSelectionSync) {
                        treeView.selectTaskGroupById(finalTaskId); // ⚠️ 关键修复：使用ID而不是名称来选择，避免同名问题
                    } else {
                        // 仅抑制一次
                        suppressNextTreeSelectionSync = false;
                    }
                });
                
                // 注意：checkAndUpdateTaskGroupRunningStatus 现在在 loadTaskGroupData 内部调用
            } else {
                logPanel.warn("⚠ 未找到任务组ID，无法加载流程图: " + taskGroupName);
                logPanel.info("提示: 请先在左侧任务树中选择该任务组");
            }
        });
        
        // 导航栏关闭任务组标签回调
        navigationBar.setOnTaskClose((taskGroupId, taskGroupName) -> {
            // 当标签页关闭时，清除树形视图中对应任务组的选中状态
            // 注意：此回调只在关闭的不是当前标签页时触发（当前标签页关闭时会直接切换到新标签页）
            if (taskGroupId != null) {
                // 如果当前选中的是关闭的任务组，清除选中状态
                Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
                if (taskGroupId.equals(currentTaskGroupId)) {
                    treeView.clearSelection();
                }
                // 清除缓存中的映射
                taskGroupNameToIdMap.remove(taskGroupName);
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
        
        // 任务组容器删除回调
        canvas.setOnDeleteGroupContainer((com.cc.job.gui.model.GroupContainer container) -> {
            deleteGroupContainer(container);
        });

        // 工具栏回调
        toolBar.setCallback(new TopToolBar.ToolBarCallback() {
            @Override
            public void onNew() {
                showNewPartitionDialog();
            }

            @Override
            public void onOpen() {
                openPartitionFile();
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

            @Override
            public void onSelect() {
                // 切换框选模式
                boolean currentMode = canvas.isSelectionMode();
                canvas.setSelectionMode(!currentMode);
                if (!currentMode) {
                    logPanel.info("✓ 框选模式已启用，请在画布上拖拽鼠标框选节点和边");
                } else {
                    logPanel.info("✓ 框选模式已禁用");
                }
            }

            @Override
            public void onLayoutHorizontal() {
                // 横向布局：将所有选中节点的 Y 坐标对齐
                canvas.alignHorizontal();
            }

            @Override
            public void onLayoutVertical() {
                // 纵向布局：将所有选中节点的 X 坐标对齐
                canvas.alignVertical();
            }
        });

        // 定期更新工具栏显示运行中的任务组
        updateToolBarRunningJobs();
    }
    
    /**
     * 设置键盘快捷键
     */
    private void setupKeyboardShortcuts() {
        // 使用场景的键盘事件监听器
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // 使用事件过滤器监听键盘事件
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    // Ctrl+C: 复制选中的节点
                    if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.C) {
                        handleCopyShortcut();
                        event.consume();
                    }
                    // Ctrl+V: 粘贴节点
                    else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.V) {
                        handlePasteShortcut();
                        event.consume();
                    }
                });
            }
        });
    }
    
    /**
     * 处理 Ctrl+C 快捷键（复制）
     */
    private void handleCopyShortcut() {
        // 获取选中的节点
        java.util.Set<ProcessNode> selectedNodes = canvas.getSelectedNodes();
        
        if (selectedNodes.isEmpty()) {
            // 如果没有选中的节点，尝试获取当前鼠标位置下的节点
            // 或者提示用户先选中节点
            logPanel.warn("⚠ 请先选中要复制的节点");
            return;
        }
        
        // 支持多节点复制
        if (selectedNodes.size() == 1) {
            // 单个节点：使用原有逻辑
            ProcessNode nodeToCopy = selectedNodes.iterator().next();
            if (nodeToCopy == null) {
                logPanel.warn("⚠ 选中的节点无效");
                return;
            }
            copyNodeToClipboard(nodeToCopy);
        } else {
            // 多个节点：使用新的多节点复制逻辑
            copyNodesToClipboard(selectedNodes);
        }
    }
    
    /**
     * 复制多个节点到剪贴板（用于键盘快捷键）
     */
    private void copyNodesToClipboard(java.util.Set<ProcessNode> sourceNodes) {
        if (sourceNodes == null || sourceNodes.isEmpty()) {
            logPanel.warn("⚠ 没有选中的节点，无法复制");
            return;
        }
        
        Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
        if (currentTaskGroupId == null) {
            logPanel.warn("⚠ 请先选择任务组后再复制节点");
            return;
        }
        
        logPanel.info("════════════════════════════════");
        logPanel.info("📋 正在复制 " + sourceNodes.size() + " 个节点到剪贴板...");
        
        new Thread(() -> {
            try {
                CopiedNodesData data = new CopiedNodesData();
                
                // 1. 复制所有节点数据
                for (ProcessNode node : sourceNodes) {
                    if (node == null) {
                        continue;
                    }
                    Long jobId = node.getJobId();
                    if (jobId == null) {
                        logPanel.warn("⚠ 节点 " + node.getJobHandlerName() + " 未绑定后端任务，跳过");
                        continue;
                    }
                    
                    JobInfoForm originalForm = jobInfoService.getJobNodeFormData(jobId);
                    if (originalForm == null) {
                        logPanel.warn("⚠ 获取节点 " + node.getJobHandlerName() + " 数据失败，跳过");
                        continue;
                    }
                    
                    JobInfoForm copyForm = deepCopyJobInfoForm(originalForm);
                    if (copyForm == null) {
                        logPanel.warn("⚠ 复制节点 " + node.getJobHandlerName() + " 数据失败，跳过");
                        continue;
                    }
                    
                    // 记录节点位置，用于计算偏移量
                    double nodeX = node.getLayoutX();
                    double nodeY = node.getLayoutY();
                    data.minX = Math.min(data.minX, nodeX);
                    data.minY = Math.min(data.minY, nodeY);
                    
                    // 保存节点数据（包含原始jobId和nodeId）
                    CopiedNodesData.NodeFormData nodeData = new CopiedNodesData.NodeFormData();
                    nodeData.form = copyForm;
                    nodeData.originalJobId = jobId;
                    nodeData.originalNodeId = node.getNodeId();
                    
                    data.nodeForms.add(nodeData);
                }
                
                // 2. 复制节点之间的连接关系
                List<NodeConnection> allConnections = canvas.getConnections();
                for (NodeConnection conn : allConnections) {
                    // ⭐ 修复：使用 getSourceOwner() 和 getTargetOwner()，支持任务组容器
                    javafx.scene.Node sourceOwner = conn.getSourceOwner();
                    javafx.scene.Node targetOwner = conn.getTargetOwner();
                    
                    // 只复制选中节点之间的连接（暂不支持任务组容器的连接复制）
                    if (sourceOwner instanceof ProcessNode && targetOwner instanceof ProcessNode) {
                        ProcessNode sourceNode = (ProcessNode) sourceOwner;
                        ProcessNode targetNode = (ProcessNode) targetOwner;
                        
                        if (sourceNodes.contains(sourceNode) && sourceNodes.contains(targetNode)) {
                            CopiedNodesData.ConnectionInfo connInfo = new CopiedNodesData.ConnectionInfo();
                            connInfo.sourceJobId = sourceNode.getJobId();
                            connInfo.targetJobId = targetNode.getJobId();
                            
                            // 获取锚点位置
                            connInfo.sourceAnchor = getAnchorPosition(sourceNode, conn.getSourceConnector());
                            connInfo.targetAnchor = getAnchorPosition(targetNode, conn.getTargetConnector());
                            
                            data.connections.add(connInfo);
                        }
                    }
                }
                
                // 保存到剪贴板
                copiedNodesData = data;
                copiedNodeForm = null; // 清除单节点复制数据
                
                Platform.runLater(() -> {
                    // 复制时不清除选中状态，保持原始节点的框框显示
                    logPanel.success("✓ " + data.nodeForms.size() + " 个节点已复制到剪贴板");
                    if (!data.connections.isEmpty()) {
                        logPanel.info("包含 " + data.connections.size() + " 条连接关系");
                    }
                    logPanel.info("按 Ctrl+V 可以粘贴节点");
                    logPanel.info("════════════════════════════════");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    logPanel.error("✗ 节点复制失败: " + e.getMessage());
                    logPanel.info("════════════════════════════════");
                });
            }
        }, "copy-nodes-clipboard-thread").start();
    }
    
    /**
     * 复制节点到剪贴板（用于键盘快捷键）- 单节点版本
     */
    private void copyNodeToClipboard(ProcessNode sourceNode) {
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
        logPanel.info("📋 正在复制节点到剪贴板: " + safeString(sourceNode.getJobHandlerName()));

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

                // 保存到剪贴板（用于粘贴）
                copiedNodeForm = copyForm;
                copiedNodesData = null; // 清除多节点复制数据
                
                Platform.runLater(() -> {
                    // 复制时不清除选中状态，保持原始节点的框框显示
                    logPanel.success("✓ 节点已复制到剪贴板: " + safeString(copyForm.getJobDesc()));
                    logPanel.info("按 Ctrl+V 可以粘贴节点");
                    logPanel.info("════════════════════════════════");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    logPanel.error("✗ 节点复制失败: " + e.getMessage());
                    logPanel.info("════════════════════════════════");
                });
            }
        }, "copy-node-clipboard-thread").start();
    }
    
    /**
     * 处理 Ctrl+V 快捷键（粘贴）
     */
    private void handlePasteShortcut() {
        // 优先使用多节点粘贴
        if (copiedNodesData != null && !copiedNodesData.nodeForms.isEmpty()) {
            pasteNodes();
            return;
        }
        
        // 兼容单节点粘贴
        if (copiedNodeForm != null) {
            pasteNode();
            return;
        }
        
        logPanel.warn("⚠ 没有可粘贴的节点，请先复制一个节点");
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
        // 若设置了抑制树同步的标志，则在处理完一次后清除
        // 注意：此标志仅控制"树的选中同步"，不影响数据加载
        if (suppressNextTaskLoad) {
            suppressNextTaskLoad = false;
            Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
            if (taskId != null && currentTaskGroupId != null && taskId.equals(currentTaskGroupId)) {
                logPanel.debug("跳过任务组重新加载（保持当前位置）: " + taskName);
                return;
            }
        }

        logPanel.info("选择节点: " + taskName + " [类型: " + getTypeNameByType(type) + "]");
        logPanel.info("任务组ID: " + taskId); // 添加ID日志，便于调试

        // ⚠️ 重要：只有任务组（type=1）才添加到导航栏，过滤掉分区（type=0）等其他节点
        if (type != null && type == 1) {
            // ⚠️ 关键修复：传递任务组ID，确保导航栏存储正确的ID映射
            navigationBar.addOrSelectTask(taskName, taskId);
        }

        // 只有任务组（type=1）才加载流程图
        if (type != null && type == 1 && taskId != null) {
            // ⚠️ 关键修复1：验证ID是否在缓存中，如果缓存中的ID与传入的ID不同，说明可能是旧缓存，清除它
            Long cachedId = taskGroupNameToIdMap.get(taskName);
            if (cachedId != null && !cachedId.equals(taskId)) {
                logPanel.warn("⚠ 检测到任务组ID不一致！缓存ID: " + cachedId + ", 实际ID: " + taskId);
                logPanel.warn("⚠ 清除旧缓存，使用新的ID: " + taskId);
                taskGroupNameToIdMap.remove(taskName);
                // 同时清除所有同名任务组的缓存（可能存在多个同名任务组）
                taskGroupNameToIdMap.entrySet().removeIf(entry -> entry.getKey().equals(taskName));
            }
            
            // ⚠️ 关键修复2：验证taskId是否真的存在，如果不存在可能是旧ID
            // 通过检查树形视图中是否存在该ID来验证
            javafx.scene.control.TreeItem<TreeNodeData> taskGroupItem = treeView.findTreeItemById(taskId);
            if (taskGroupItem == null) {
                logPanel.error("✗ 错误：任务组ID " + taskId + " 在树形视图中不存在！");
                logPanel.warn("⚠ 这可能是因为使用了旧的ID，请刷新树形视图后重试");
                logger.error("🔍 [DEBUG] 任务组ID {} 在树形视图中不存在，taskName: {}", taskId, taskName);
                return;
            }
            
            // 验证节点名称和类型是否匹配
            TreeNodeData nodeData = taskGroupItem.getValue();
            if (nodeData != null) {
                // ⚠️ 关键修复：如果名称不匹配，说明ID和名称不一致，可能是使用了错误的ID，直接返回
                if (!taskName.equals(nodeData.getLabel())) {
                    logPanel.error("✗ 错误：任务组名称不匹配！期望: " + taskName + ", 实际: " + nodeData.getLabel());
                    logPanel.warn("⚠ 这可能是因为使用了错误的ID，请刷新树形视图后重试");
                    logger.error("🔍 [DEBUG] 任务组名称不匹配 - 期望: {}, 实际: {}, ID: {}", taskName, nodeData.getLabel(), taskId);
                    return;
                }
                // 验证节点类型
                if (nodeData.getType() == null || nodeData.getType() != 1) {
                    logPanel.error("✗ 错误：节点类型不正确！期望: 1(任务组), 实际: " + nodeData.getType());
                    logger.error("🔍 [DEBUG] 节点类型不正确 - ID: {}, type: {}", taskId, nodeData.getType());
                    return;
                }
            }
            
            // 更新任务组名称到ID的映射（使用最新的ID）
            taskGroupNameToIdMap.put(taskName, taskId);
            // 更新当前选中的任务组ID
            usePageStoreHook().setCurrentPage(taskId);
            // 更新导航栏当前任务组ID
            navigationBar.setCurrentTaskGroupId(taskId);
            // 更新顶部工具栏当前任务组ID
            toolBar.setCurrentTaskGroupId(taskId);
            
            logPanel.info("开始加载任务组数据，ID: " + taskId + ", 名称: " + taskName);
            loadTaskGroupData(taskId, taskName);
            
            // 注意：checkAndUpdateTaskGroupRunningStatus 现在在 loadTaskGroupData 内部调用
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
     * 切换到指定任务组并执行定位操作
     * @param taskGroupId 任务组ID
     * @param taskGroupName 任务组名称
     * @param locateCallback 定位回调，在任务组加载完成后执行
     */
    private void switchToTaskGroupAndLocate(Long taskGroupId, String taskGroupName, Runnable locateCallback) {
        // 更新任务组名称到ID的映射
        taskGroupNameToIdMap.put(taskGroupName, taskGroupId);
        // 更新当前选中的任务组ID
        usePageStoreHook().setCurrentPage(taskGroupId);
        // 更新导航栏当前任务组ID
        navigationBar.setCurrentTaskGroupId(taskGroupId);
        // 更新顶部工具栏当前任务组ID
        toolBar.setCurrentTaskGroupId(taskGroupId);
        
        // 添加到导航栏（如果还没有），传递任务组ID
        navigationBar.addOrSelectTask(taskGroupName, taskGroupId);
        
        // 加载任务组数据，并在加载完成后执行定位
        loadTaskGroupDataWithCallback(taskGroupId, taskGroupName, locateCallback);
    }
    
    /**
     * 加载任务组数据（带回调）
     */
    private void loadTaskGroupDataWithCallback(Long taskId, String taskName, Runnable callback) {
        logPanel.info("════════════════════════════════");
        logPanel.info("开始加载任务组: " + taskName);
        logPanel.info("任务组ID: " + taskId);

        // 在后台线程中加载数据
        new Thread(() -> {
            try {
                // 同步节点状态
                canvas.syncPendingNodeStatusBlocking();
                Platform.runLater(() -> logPanel.info("节点状态同步完成，开始加载最新数据"));

                // 加载任务组数据
                JobComposeData composeData = jobPartService.getJobCompose(taskId);

                // 在 JavaFX 主线程中更新 UI
                Platform.runLater(() -> {
                    // ⚠️ 注意：不再清空缓存，而是使用智能合并策略
                    // 在 loadFromComposeData 中会智能合并后端状态和缓存状态，避免显示过时的状态

                    // 清空画布
                    canvas.clear();

                    // 加载节点和连接
                    if (composeData != null) {
                        canvas.loadFromComposeData(composeData);
                        // 为所有加载的节点设置编辑回调
                        setupEditCallbacksForLoadedNodes(composeData);

                        int nodeCount = composeData.getNodes() != null ? composeData.getNodes().size() : 0;
                        int edgeCount = composeData.getEdges() != null ? composeData.getEdges().size() : 0;

                        logPanel.success("✓ 任务组加载成功！");
                        logPanel.info("节点数: " + nodeCount + ", 连接数: " + edgeCount);
                    } else {
                        logPanel.warn("⚠ 任务组数据为空");
                    }

                    // 检查任务运行状态并更新小绿点显示
                    checkAndUpdateTaskGroupRunningStatus(taskId, taskName);

                    // 同步树形视图的选中状态（可一次性抑制）
                    if (!suppressNextTreeSelectionSync) {
                        treeView.selectTaskGroupByName(taskName);
                    } else {
                        suppressNextTreeSelectionSync = false;
                    }

                    // 延迟执行定位回调，确保画布已经完全渲染
                    javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
                    delay.setOnFinished(e -> {
                        if (callback != null) {
                            callback.run();
                        }
                    });
                    delay.play();

                    logPanel.info("════════════════════════════════");
                });

            } catch (Exception ex) {
                logger.error("加载任务组数据失败: {}", ex.getMessage(), ex);
                Platform.runLater(() -> {
                    logPanel.error("✗ 加载任务组数据失败: " + ex.getMessage());
                    logPanel.warn("提示: 请检查后端服务是否正常运行");
                });
            }
        }, "load-task-group-" + taskId).start();
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
                    // ⚠️ 关键修复：在加载新任务组数据前，清除节点状态缓存
                    // 这样可以避免使用旧任务组的节点状态缓存
                    com.cc.job.gui.util.NodeStatusSyncManager.getInstance().clearCacheForTaskGroupSwitch();

                    if (composeData != null) {
                        // 记录所有节点的jobId，用于调试
                        if (composeData.getNodes() != null) {
                            for (com.cc.job.gui.model.JobComposeData.NodeData nodeData : composeData.getNodes()) {
                                if (nodeData.getJobId() != null) {
                                }
                            }
                        }
                        canvas.loadFromComposeData(composeData);

                        // 为所有加载的节点设置编辑回调
                        setupEditCallbacksForLoadedNodes(composeData);

                        int nodeCount = composeData.getNodes() != null ? composeData.getNodes().size() : 0;
                        int edgeCount = composeData.getEdges() != null ? composeData.getEdges().size() : 0;

                        logPanel.success("✓ 任务组加载成功！");
                        logPanel.info("节点数: " + nodeCount + ", 连接数: " + edgeCount);
                        logPanel.info("════════════════════════════════");

                        // 画布数据加载完成后，检查并恢复任务组的运行状态（包括连接线动画）
                        checkAndUpdateTaskGroupRunningStatus(taskId, taskName);
                    } else {
                        logPanel.warn("⚠ 任务组数据为空");
                        canvas.clear();
                    }
                });

            } catch (IOException e) {
                logger.error("加载任务组数据失败: {}", e.getMessage(), e);

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
        });
        logPanelDetachable.setOnReattach(() -> {
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
        logPanel.setVisible(logPanelVisible);
        logPanel.setManaged(logPanelVisible);

        // 判断是否有任何组件可见
        boolean anyVisible = treeViewVisible || miniMapVisible;

        // leftArea 根据是否有组件可见来决定是否显示
        leftArea.setVisible(anyVisible);
        leftArea.setManaged(anyVisible);

        // 折叠侧边栏始终显示，只控制按钮的显示
        // 只显示已隐藏组件对应的按钮
        collapsedSidebar.showTreeViewButton(!treeViewVisible);
        collapsedSidebar.showMiniMapButton(!miniMapVisible);
        collapsedSidebar.showLogPanelButton(!logPanelVisible);
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

        // 1. 先检查本地状态（快速检查）
        RunningJobGroup existingJob = runningJobs.get(currentJobId);
        if (existingJob != null && existingJob.isRunning()) {
            NotificationToast.show("任务组"+ currentJobId + " 正在运行中，请稍后再试", NotificationToast.NotificationType.WARNING);
            return;
        }

        // 2. 【分布式检查】调用后端API检查实际运行状态
        // 在后台线程中检查，避免阻塞UI
        new Thread(() -> {
            try {
                boolean isRunningOnServer = jobInfoService.getJobStatus(currentJobId);
                
                Platform.runLater(() -> {
                    if (isRunningOnServer) {
                        String jobName = getJobNameById(currentJobId);
                        NotificationToast.show("任务组"+ currentJobId + " 正在运行中，请稍后再试", NotificationToast.NotificationType.WARNING);
                        logPanel.info("提示：该任务组可能正在其他客户端或服务器实例上运行");
                        return;
                    }
                    
                    // 后端检查通过，继续执行任务
                    continueJobExecution(currentJobId);
                });
            } catch (Exception e) {
                logger.error("❌ 检查任务组运行状态失败: {}", e.getMessage(), e);
                
                Platform.runLater(() -> {
                    logPanel.warn("⚠ 检查任务组运行状态失败: " + e.getMessage());
                    logPanel.warn("为安全起见，取消本次任务启动");
                    // 可以选择继续执行或阻止执行，这里选择阻止执行以保证安全
                });
            }
        }).start();
    }

    /**
     * 继续执行任务（在确认未运行后调用）
     * 
     * @param currentJobId 任务组ID
     */
    private void continueJobExecution(Long currentJobId) {
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
        navigationBar.updateTaskGroupRunningStatus(currentJobId, true);

        // 更新工具栏显示
        updateToolBarRunningJobs();

        // 设置所有边为运行状态（虚线动画）
        canvas.setAllConnectionsRunning(true);

        // 重置所有节点状态为空闲（紫色），等待SSE消息来更新节点状态
        // 只有实际运行到的节点才会通过SSE消息变成黄色
        Platform.runLater(() -> {
            
            logPanel.info(currentJobId, "════════════════════════════════");
            logPanel.info(currentJobId, "📋 任务组启动前，检查画布中的节点:");
            logPanel.info(currentJobId, "   画布中共有 " + canvas.getNodes().size() + " 个节点");
            
            for (ProcessNode node : canvas.getNodes()) {
                String nodeInfo = "   - 节点: " + node.getJobHandlerName() + 
                                 ", nodeId: " + node.getNodeId() + 
                                 ", jobId: " + node.getJobId();
                logPanel.info(currentJobId, nodeInfo);
                node.updateStatus(ProcessNode.NodeStatus.IDLE);
            }
            logPanel.info(currentJobId, "════════════════════════════════");
        });

        // 连接SSE以接收节点状态更新
        
        logPanel.info(currentJobId, "🔌 准备连接SSE");
        logPanel.info(currentJobId, "   任务组ID: " + currentJobId);
        logPanel.info(currentJobId, "   randomId: " + randomId);
        logPanel.info(currentJobId, "   连接ID: " + currentJobId + ":" + randomId);
        
        SSEService sseService = SSEService.getInstance();
        final RunningJobGroup finalRunningJob = runningJob; // 用于内部类访问
        sseService.connect(currentJobId, randomId, message -> {
            handleSSEMessage(message, randomId);
        });
        
        logPanel.info(currentJobId, "✅ SSE连接请求已发送");
        
        // 等待一段时间后检查连接状态
        new Thread(() -> {
            try {
                Thread.sleep(3000); // 等待3秒
                javafx.application.Platform.runLater(() -> {
                    if (!sseService.isConnected(currentJobId, randomId)) {
                        logPanel.warn(currentJobId, "⚠️ SSE连接可能未成功建立");
                        logPanel.warn(currentJobId, "   请检查后端SSE服务是否正常运行");
                        logPanel.warn(currentJobId, "   连接ID: " + currentJobId + ":" + randomId);
                    } else {
                        logPanel.info(currentJobId, "✅ SSE连接状态检查: 已连接");
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
                finalRunningJob.setLogId(logId);

                Platform.runLater(() -> {
                    logPanel.success("✓ 任务已提交，日志ID: " + logId);
                    logPanel.info("开始获取执行日志...");
                });

                // 启动日志轮询（针对该任务组）
                startLogPolling(finalRunningJob);

            } catch (Exception e) {
                logger.error("触发任务执行失败: {}", e.getMessage(), e);

                Platform.runLater(() -> {
                    logPanel.error("✗ 任务执行失败: " + e.getMessage());
                    // 清理失败的任务组
                    finalRunningJob.cleanup();
                    runningJobs.remove(currentJobId);
                    updateToolBarRunningJobs();
                    // 恢复边的正常状态
                    canvas.setAllConnectionsRunning(false);
                    // 断开SSE连接
                    SSEService.getInstance().disconnect(currentJobId, randomId);
                });
            }
        }).start();
    }

    /**
     * 处理SSE消息
     * @param message SSE消息
     * @param expectedRandomId 期望的randomId（用于验证消息）
     */
    private void handleSSEMessage(SSEService.SSEMessage message, String expectedRandomId) {
        
        logPanel.info("========================================");
        logPanel.info("📨 收到SSE消息");
        logPanel.info("   消息randomId: " + message.getRandomId());
        logPanel.info("   期望randomId: " + expectedRandomId);
        logPanel.info("   消息jobId: " + message.getJobId());
        logPanel.info("   消息status: " + message.getStatus());
        logPanel.info("   消息result: " + message.getResult());
        
        // 处理连接错误消息（status=-1表示连接错误）
        if (message.getStatus() != null && message.getStatus() == -1) {
            String errorMsg = "❌ SSE连接错误: " + message.getResult();
            logger.error(errorMsg);
            logPanel.error(errorMsg);
            return;
        }
        
        // 验证randomId是否匹配
        if (message.getRandomId() != null && !expectedRandomId.equals(message.getRandomId())) {
            String warnMsg = "⚠️ SSE消息randomId不匹配，忽略: " + message.getRandomId() + " != " + expectedRandomId;
            logPanel.warn(warnMsg);
            logPanel.info("========================================");
            return;
        }

        Long jobId = message.getJobId();
        Integer status = message.getStatus();

        // 当status为9时，表示后端发送了所有节点的预测开始/结束时间
        if (status != null && status == 9 && message.getResult() != null) {
            try {
                // 结果是 String[][]，每项为 [nodeId, startTime, endTime]
                String[][] times = apiUtil.getGson().fromJson(message.getResult(), String[][].class);
                if (times != null) {
                    // 缓存到内存映射中，供“节点详情”展示
                    ensurePredictedTimeCache();
                    for (String[] row : times) {
                        if (row != null && row.length >= 3 && row[0] != null) {
                            predictedNodeTimes.put(row[0], new String[]{row[1], row[2]});
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        
        logPanel.info("✅ randomId匹配，开始处理消息");
        logPanel.info("📊 准备更新节点状态: jobId=" + jobId + ", status=" + status);

        // 更新节点状态（必须在JavaFX线程中执行）
        if (jobId != null && status != null) {
            logPanel.info("✅ jobId和status都不为空，准备更新节点状态");
            Platform.runLater(() -> {
                logPanel.info("🔄 在JavaFX线程中更新节点状态");
                canvas.updateNodeStatusByJobId(jobId, status);
            });
        } else {
            String warnMsg = "⚠️ jobId或status为null，无法更新节点状态";
            logPanel.warn(warnMsg);
            logPanel.warn("   jobId: " + jobId);
            logPanel.warn("   status: " + status);
        }

        // 如果状态是5（任务完成），只恢复边的正常状态，但保留节点状态
        if (status != null && status == 5) {
            logPanel.info("🏁 任务完成（status=5），恢复边的正常状态，保留节点状态");
            Platform.runLater(() -> {
                // 只恢复边的运行状态（停止虚线动画），不重置节点状态
                canvas.setAllConnectionsRunning(false);
                // 注意：不重置节点状态，让节点保持最终状态（成功/失败）
            });
        }
        
        logPanel.info("========================================");
    }

    // 预测时间缓存：key=nodeId, value=[startTime, endTime]
    private java.util.Map<String, String[]> predictedNodeTimes;

    private void ensurePredictedTimeCache() {
        if (predictedNodeTimes == null) {
            predictedNodeTimes = new java.util.concurrent.ConcurrentHashMap<>();
        }
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
        }, 1000, 3000); // 1秒后开始，每3秒轮询一次（从2秒增加到3秒，减少请求频率）
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
                }
            }

        } catch (Exception e) {
            logger.error("获取执行日志失败: {}", e.getMessage(), e);
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
            navigationBar.updateTaskGroupRunningStatus(runningJob.getJobId(), false);
            
            updateToolBarRunningJobs();

            // 恢复边的正常状态（停止虚线动画）
            canvas.setAllConnectionsRunning(false);

            // 断开SSE连接
            SSEService.getInstance().disconnect(runningJob.getJobId(), runningJob.getRandomId());
            
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
                    navigationBar.updateTaskGroupRunningStatus(jobId, false);

                    // 更新工具栏显示
                    updateToolBarRunningJobs();

                    // 恢复边的正常状态
                    canvas.setAllConnectionsRunning(false);

                    // 断开SSE连接
                    SSEService.getInstance().disconnect(jobId, runningJob.getRandomId());

                    // 重置所有节点状态为空闲
                    for (ProcessNode node : canvas.getNodes()) {
                        node.updateStatus(ProcessNode.NodeStatus.IDLE);
                    }
                });

            } catch (Exception e) {
                logger.error("停止任务失败: {}", e.getMessage(), e);

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
                
                // 更新导航栏中的小绿点
                navigationBar.updateTaskGroupRunningStatus(taskId, isRunning);
                
                // 在 JavaFX 主线程中更新连接线的运行状态
                Platform.runLater(() -> {
                    if (isRunning) {
                        logPanel.debug("✅ 任务组 " + taskGroupName + " 正在运行中");
                        // 恢复连接线的动画状态（如果任务组正在运行）
                        // 延迟执行，确保画布数据已经加载完成
                        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
                        delay.setOnFinished(e -> {
                            canvas.setAllConnectionsRunning(true);
                            // 刷新所有节点的状态（从缓存中获取最新状态）
                            // 这样可以确保切换回来时，节点状态是最新的
                            canvas.refreshAllNodeStatusFromCache();
                        });
                        delay.play();
                    } else {
                        logPanel.debug("⚪ 任务组 " + taskGroupName + " 未运行");
                        // 确保连接线处于非运行状态
                        canvas.setAllConnectionsRunning(false);
                        // 即使任务组未运行，也尝试从缓存中恢复节点状态（可能任务刚完成）
                        canvas.refreshAllNodeStatusFromCache();
                    }
                });
            } catch (Exception e) {
                logger.error("❌ 获取任务组运行状态失败: {}", e.getMessage(), e);
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

        Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
        if (currentTaskGroupId == null || currentTaskGroupId == 0) {
            logPanel.error("✗ 无法保存：未选择任务组");
            logPanel.warn("提示: 请先在导航栏选择要保存的任务组");
            logPanel.info("════════════════════════════════");
            return;
        }

        List<ProcessNode> nodes = canvas.getNodes();
        List<NodeConnection> connections = canvas.getConnections();
        List<GroupContainer> groups = canvas.getGroupContainers();

        logPanel.info(String.format("节点数量: %d, 连接数量: %d, 任务组数量: %d", nodes.size(), connections.size(), groups.size()));
        
        // ⭐ 修复：调试日志，检查任务组节点信息
        if (groups.isEmpty()) {
            logPanel.warn("⚠️ 警告：没有找到任务组容器！");
        } else {
            for (GroupContainer group : groups) {
                String groupNodeId = group.getNodeId();
                Long groupId = group.getGroupId();
                String groupName = group.getGroupName();
                logPanel.info(String.format("任务组: name=%s, nodeId=%s, groupId=%s", 
                    groupName, groupNodeId, groupId != null ? groupId.toString() : "null"));
            }
        }

        // 构建节点到任务组的映射，用于识别哪些节点属于任务组容器
        Map<String, GroupContainer> nodeToGroupMap = new HashMap<>();
        Set<String> managedNodeIds = new HashSet<>(); // 被任务组管理的节点ID集合
        for (GroupContainer group : groups) {
            List<ProcessNode> managedNodes = group.getManagedCanvasNodes();
            if (managedNodes != null) {
                for (ProcessNode managedNode : managedNodes) {
                    if (managedNode.getNodeId() != null) {
                        nodeToGroupMap.put(managedNode.getNodeId(), group);
                        managedNodeIds.add(managedNode.getNodeId());
                    }
                }
            }
        }

        try {
            // ⭐ 修复：使用Set来去重，确保每个节点只添加一次
            Set<String> addedNodeIds = new HashSet<>();
            List<Map<String, Object>> nodesData = new ArrayList<>();
            
            // 1. 先添加所有普通节点（包括被任务组管理的节点，因为后端需要这些节点的完整信息）
            for (ProcessNode node : nodes) {
                String nodeId = node.getNodeId();
                if (nodeId == null || addedNodeIds.contains(nodeId)) {
                    continue; // 跳过已添加的节点
                }
                
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("id", nodeId);
                nodeData.put("type", node.getType() != null ? node.getType() : "rect");
                nodeData.put("x", node.getX());
                nodeData.put("y", node.getY());

                Map<String, Object> propertiesMap = new HashMap<>();
                if (node.getJobId() != null) {
                    propertiesMap.put("jobId", node.getJobId());
                }
                String propertiesJson = apiUtil.getGson().toJson(propertiesMap);
                nodeData.put("properties", propertiesJson);

                nodesData.add(nodeData);
                addedNodeIds.add(nodeId);
            }

            // 2. 添加任务组节点（每个任务组节点只添加一次）
            for (GroupContainer group : canvas.getGroupContainers()) {
                String groupNodeId = group.getNodeId();
                if (groupNodeId == null) {
                    logPanel.warn("⚠️ 警告：任务组节点ID为空，跳过: " + group.getGroupName());
                    continue; // 跳过nodeId为null的任务组节点
                }
                // ⭐ 修复：如果任务组节点的nodeId已经在普通节点循环中被添加，需要更新为任务组节点类型
                if (addedNodeIds.contains(groupNodeId)) {
                    logPanel.info("⚠️ 任务组节点ID已存在，更新为任务组节点类型: " + groupNodeId);
                    // 查找并更新已存在的节点数据
                    for (Map<String, Object> existingNode : nodesData) {
                        if (groupNodeId.equals(existingNode.get("id"))) {
                            // 更新为任务组节点类型
                            existingNode.put("type", "CustomGroup");
                            // 更新位置
                            existingNode.put("x", group.getLayoutX());
                            existingNode.put("y", group.getLayoutY());
                            
                            // 更新properties，添加children属性
                            Map<String, Object> propertiesMap = new HashMap<>();
                            if (group.getGroupId() != null) {
                                propertiesMap.put("jobId", group.getGroupId());
                            }
                            
                            // 收集子节点ID
                            Set<String> childNodeIdSet = new HashSet<>();
                            List<ProcessNode> managedNodes = group.getManagedCanvasNodes();
                            if (managedNodes != null) {
                                for (ProcessNode childNode : managedNodes) {
                                    if (childNode.getNodeId() != null) {
                                        childNodeIdSet.add(childNode.getNodeId());
                                    }
                                }
                            }
                            
                            // 添加嵌套的任务组节点
                            for (GroupContainer otherGroup : canvas.getGroupContainers()) {
                                if (otherGroup != group) {
                                    double groupX = group.getLayoutX();
                                    double groupY = group.getLayoutY();
                                    double groupWidth = group.getFrame().getWidth();
                                    double groupHeight = group.getFrame().getHeight();
                                    double nestedX = otherGroup.getLayoutX();
                                    double nestedY = otherGroup.getLayoutY();
                                    if (nestedX >= groupX && nestedX <= groupX + groupWidth &&
                                            nestedY >= groupY && nestedY <= groupY + groupHeight) {
                                        String nestedGroupId = otherGroup.getNodeId();
                                        if (nestedGroupId != null) {
                                            childNodeIdSet.add(nestedGroupId);
                                        }
                                    }
                                }
                            }
                            
                            List<String> childNodeIds = new ArrayList<>(childNodeIdSet);
                            propertiesMap.put("children", childNodeIds);
                            String propertiesJson = apiUtil.getGson().toJson(propertiesMap);
                            existingNode.put("properties", propertiesJson);
                            
                            logPanel.info("✓ 已更新任务组节点数据: " + group.getGroupName() + 
                                " (id: " + groupNodeId + ", children: " + childNodeIds.size() + ")");
                            break;
                        }
                    }
                    continue; // 已经处理过，跳过
                }
                
                logPanel.info("✓ 添加任务组节点: " + group.getGroupName() + " (nodeId: " + groupNodeId + ")");
                
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("id", groupNodeId);
                nodeData.put("type", "CustomGroup");
                nodeData.put("x", group.getLayoutX());
                nodeData.put("y", group.getLayoutY());

                Map<String, Object> propertiesMap = new HashMap<>();
                if (group.getGroupId() != null) {
                    propertiesMap.put("jobId", group.getGroupId());
                }

                // 收集直接子节点ID（包括普通节点和嵌套的任务组节点）
                // ⭐ 修复：使用Set去重，避免重复的子节点ID
                Set<String> childNodeIdSet = new HashSet<>();
                
                // 1. 添加管理的普通节点
                List<ProcessNode> managedNodes = group.getManagedCanvasNodes();
                if (managedNodes != null) {
                    for (ProcessNode childNode : managedNodes) {
                        if (childNode.getNodeId() != null) {
                            childNodeIdSet.add(childNode.getNodeId());
                        }
                    }
                }

                // 2. 添加嵌套的任务组节点（通过位置判断）
                for (GroupContainer otherGroup : canvas.getGroupContainers()) {
                    if (otherGroup != group) {
                        double groupX = group.getLayoutX();
                        double groupY = group.getLayoutY();
                        double groupWidth = group.getFrame().getWidth();
                        double groupHeight = group.getFrame().getHeight();

                        double nestedX = otherGroup.getLayoutX();
                        double nestedY = otherGroup.getLayoutY();

                        // 判断嵌套任务组是否在当前任务组内部
                        if (nestedX >= groupX && nestedX <= groupX + groupWidth &&
                                nestedY >= groupY && nestedY <= groupY + groupHeight) {
                            String nestedGroupId = otherGroup.getNodeId();
                            if (nestedGroupId != null) {
                                childNodeIdSet.add(nestedGroupId);
                            }
                        }
                    }
                }
                
                // 转换为List（保持顺序，虽然Set不保证顺序，但这里主要是去重）
                List<String> childNodeIds = new ArrayList<>(childNodeIdSet);

                // 3. 设置children属性（即使为空也要设置，以便后端识别这是任务组节点）
                propertiesMap.put("children", childNodeIds);

                String propertiesJson = apiUtil.getGson().toJson(propertiesMap);
                nodeData.put("properties", propertiesJson);

                nodesData.add(nodeData);
                addedNodeIds.add(groupNodeId);
                logPanel.info("✓ 任务组节点已添加到nodesData: " + group.getGroupName() + 
                    " (id: " + groupNodeId + ", children: " + childNodeIds.size() + ")");
            }
            
            // ⭐ 修复：检查任务组节点是否成功添加到nodesData
            long groupNodeCount = nodesData.stream()
                .filter(node -> "CustomGroup".equals(node.get("type")))
                .count();
            logPanel.info("✓ nodesData中的任务组节点数量: " + groupNodeCount + " / 总节点数: " + nodesData.size());

            List<Map<String, Object>> edgesData = new ArrayList<>();
            // ⭐ 修复：使用Set去重，确保每个连线只添加一次
            Set<String> addedEdgeKeys = new HashSet<>();
            
            for (NodeConnection conn : connections) {
                Map<String, Object> edgeData = new HashMap<>();
                String sourceId = null;
                
                // ⭐ 修复：获取源节点ID（支持 ProcessNode 和 GroupContainer）
                javafx.scene.Node sourceOwner = conn.getSourceOwner();
                if (sourceOwner instanceof ProcessNode) {
                    sourceId = ((ProcessNode) sourceOwner).getNodeId();
                } else if (sourceOwner instanceof GroupContainer) {
                    sourceId = ((GroupContainer) sourceOwner).getNodeId();
                }
                
                if (sourceId == null || sourceId.isEmpty()) {
                    continue; // 跳过无法获取源节点ID的连线
                }
                
                String targetId = null;
                // ⭐ 修复：获取目标节点ID（支持 ProcessNode 和 GroupContainer）
                javafx.scene.Node targetOwner = conn.getTargetOwner();
                if (targetOwner instanceof ProcessNode) {
                    targetId = ((ProcessNode) targetOwner).getNodeId();
                } else if (targetOwner instanceof GroupContainer) {
                    targetId = ((GroupContainer) targetOwner).getNodeId();
                }
                
                if (targetId == null || targetId.isEmpty()) {
                    continue; // 跳过无法获取目标节点ID的连线
                }
                
                // ⭐ 修复：去重处理，避免重复的连线
                String edgeKey = sourceId + "->" + targetId;
                if (addedEdgeKeys.contains(edgeKey)) {
                    continue; // 跳过重复的连线
                }
                addedEdgeKeys.add(edgeKey);
                
                // ⭐ 修复：获取连接点的位置（startPoint和endPoint）
                String startPoint = getConnectorPosition(conn.getSourceOwner(), conn.getSourceConnector());
                String endPoint = getConnectorPosition(conn.getTargetOwner(), conn.getTargetConnector());
                
                edgeData.put("sourceNodeId", sourceId);
                edgeData.put("targetNodeId", targetId);
                edgeData.put("startPoint", startPoint);
                edgeData.put("endPoint", endPoint);
                edgesData.add(edgeData);
            }

            // ⭐ 修复：调试日志，检查JSON中是否包含任务组节点
            long finalGroupNodeCount = nodesData.stream()
                .filter(node -> "CustomGroup".equals(node.get("type")))
                .count();
            logPanel.info("✓ 准备发送到后端 - 节点总数: " + nodesData.size() + ", 任务组节点数: " + finalGroupNodeCount);
            if (finalGroupNodeCount == 0 && !groups.isEmpty()) {
                logPanel.error("✗ 错误：有任务组容器但JSON中没有任务组节点数据！");
                logPanel.error("✗ 请检查任务组节点的nodeId是否正确设置");
            }
            
            String nodesJson = apiUtil.getGson().toJson(nodesData);
            String edgesJson = apiUtil.getGson().toJson(edgesData);

            new Thread(() -> {
                try {
                    logPanel.info("正在获取任务组表单数据...");

                    JobInfoForm formData = jobInfoService.getFormData(currentTaskGroupId);

                    if (formData == null) {
                        Platform.runLater(() -> {
                            logPanel.error("✗ 获取任务组数据失败");
                            logPanel.info("════════════════════════════════");
                        });
                        return;
                    }

                    formData.setNodes(nodesJson);
                    formData.setEdges(edgesJson);
                    formData.setGlueType("BEAN");
                    formData.setExecutorHandler("runJobGroupXxlJob");
                    if (formData.getExecutorRouteStrategy() == null || formData.getExecutorRouteStrategy().isEmpty()) {
                        formData.setExecutorRouteStrategy("FIRST");
                    }
                    formData.setGlueUpdatetime(null);

                    logPanel.info("正在保存到数据库...");

                    boolean success = jobInfoService.updateJobCompose(currentTaskGroupId, formData);

                    if (success) {
                        Platform.runLater(() -> {
                            logPanel.success("✓ 保存成功！");
                            logPanel.info("节点: " + nodes.size() + ", 连接: " + connections.size());
                            logPanel.info("正在刷新任务树...");
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
                    logger.error("保存任务组失败: {}", e.getMessage(), e);

                    Platform.runLater(() -> {
                        logPanel.error("✗ 保存失败: " + e.getMessage());
                        logPanel.warn("提示: 请检查后端服务是否正常运行");
                        logPanel.info("════════════════════════════════");
                    });
                }
            }).start();

        } catch (Exception e) {
            logger.error("转换数据失败: {}", e.getMessage(), e);
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
     * 获取连接点位置（支持 ProcessNode 和 GroupContainer）
     * @param owner 节点或任务组容器
     * @param connector 连接点
     * @return 连接点位置（"top", "bottom", "left", "right"）
     */
    private String getConnectorPosition(javafx.scene.Node owner, javafx.scene.shape.Circle connector) {
        if (owner == null || connector == null) {
            return "right"; // 默认右侧
        }
        if (owner instanceof ProcessNode) {
            ProcessNode node = (ProcessNode) owner;
            if (connector == node.getTopConnector()) return "top";
            if (connector == node.getBottomConnector()) return "bottom";
            if (connector == node.getLeftConnector()) return "left";
            if (connector == node.getRightConnector()) return "right";
        } else if (owner instanceof GroupContainer) {
            GroupContainer group = (GroupContainer) owner;
            if (connector == group.getTopConnector()) return "top";
            if (connector == group.getBottomConnector()) return "bottom";
            if (connector == group.getLeftConnector()) return "left";
            if (connector == group.getRightConnector()) return "right";
        }
        return "right"; // 默认右侧
    }
    
    /**
     * 根据锚点字符串获取对应的连接点
     * @param node 节点
     * @param anchor 锚点字符串，可能为 "top", "bottom", "left", "right" 或 null
     * @param isSource 是否为源节点（true=源节点，false=目标节点）
     * @return 连接点Circle对象
     */
    private javafx.scene.shape.Circle getConnectorByAnchor(ProcessNode node, String anchor, boolean isSource) {
        if (anchor != null && !anchor.isEmpty()) {
            // 根据锚点字符串返回对应的连接点（不区分大小写）
            String anchorLower = anchor.toLowerCase();
            if ("top".equals(anchorLower)) {
                return node.getTopConnector();
            } else if ("bottom".equals(anchorLower)) {
                return node.getBottomConnector();
            } else if ("left".equals(anchorLower)) {
                return node.getLeftConnector();
            } else if ("right".equals(anchorLower)) {
                return node.getRightConnector();
            }
        }
        
        // 如果没有指定锚点，使用默认值
        // 源节点默认使用右侧（数据流出）
        // 目标节点默认使用左侧（数据流入）
        if (isSource) {
            return node.getRightConnector();
        } else {
            return node.getLeftConnector();
        }
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
                        logger.error("保存分区失败: {}", e.getMessage(), e);

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
            logger.error("显示新建分区对话框失败: {}", e.getMessage(), e);
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
            public void onSuppressTreeSelectionOnce() {
                suppressNextTreeSelectionSync = true;
            }
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
                showNewJobGroupDialog(partitionId, partitionName, null);
            }

            @Override
            public void onNewJobNode(Long taskGroupId, String taskGroupName) {
                showNewJobNodeDialog(taskGroupId, taskGroupName, null);
            }

            @Override
            public void onJobNodeAction(Long jobNodeId, Long jobId, String nodeName, TaskTreeView.TaskSelectionCallback.JobNodeAction action) {
                onJobNodeAction(jobNodeId, jobId, nodeName, null, action);
            }
            
            @Override
            public void onJobNodeAction(Long jobNodeId, Long jobId, String nodeName, Long taskGroupId, TaskTreeView.TaskSelectionCallback.JobNodeAction action) {
                if (jobId == null) {
                    logPanel.warn("⚠ 该树节点缺少任务ID，无法执行操作: " + nodeName);
                    return;
                }

                // 如果提供了任务组ID，检查是否需要切换任务组
                if (taskGroupId != null && action == TaskTreeView.TaskSelectionCallback.JobNodeAction.LOCATE) {
                    Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
                    if (currentTaskGroupId == null || !currentTaskGroupId.equals(taskGroupId)) {
                        // 需要切换到对应的任务组
                        String taskGroupName = getJobNameById(taskGroupId);
                        if (taskGroupName == null) {
                            taskGroupName = "任务组 " + taskGroupId;
                        }
                        // 切换任务组
                        switchToTaskGroupAndLocate(taskGroupId, taskGroupName, () -> {
                            ProcessNode targetNode = canvas.getNodeByJobId(jobId);
                            if (targetNode != null) {
                                locateNodeOnCanvas(targetNode);
                            } else {
                                logPanel.warn("⚠ 在画布上未找到任务节点: " + nodeName + " (jobId=" + jobId + ")");
                            }
                        });
                        return;
                    }
                }

                switch (action) {
                    case EDIT -> {
                        // 编辑操作不依赖画布上的节点，可以直接打开编辑对话框
                        ProcessNode targetNode = canvas.getNodeByJobId(jobId);
                        editNode(jobId, targetNode, taskGroupId);
                    }
                    case LOCATE -> {
                        // 定位操作需要画布上有节点
                        ProcessNode targetNode = canvas.getNodeByJobId(jobId);
                        if (targetNode == null) {
                            logPanel.warn("⚠ 在画布上未找到任务节点: " + nodeName + " (jobId=" + jobId + ")");
                            return;
                        }
                        locateNodeOnCanvas(targetNode);
                    }
                    case PROPERTIES -> {
                        // 属性操作不依赖画布上的节点，可以直接显示详情
                        showNodeDetailsByJobId(jobId, nodeName);
                    }
                    default -> { }
                }
            }

            @Override
            public void onEdgeAction(Long edgeId, TaskTreeView.TaskSelectionCallback.EdgeAction action) {
                onEdgeAction(edgeId, null, action);
            }
            
            @Override
            public void onEdgeAction(Long edgeId, Long taskGroupId, TaskTreeView.TaskSelectionCallback.EdgeAction action) {
                if (edgeId == null) {
                    logPanel.warn("⚠ 未提供边ID，无法执行操作");
                    return;
                }

                String edgeKey = String.valueOf(edgeId);
                
                // 如果提供了任务组ID，检查是否需要切换任务组
                if (taskGroupId != null && action == TaskTreeView.TaskSelectionCallback.EdgeAction.LOCATE) {
                    Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
                    if (currentTaskGroupId == null || !currentTaskGroupId.equals(taskGroupId)) {
                        // 需要切换到对应的任务组
                        String taskGroupName = getJobNameById(taskGroupId);
                        if (taskGroupName == null) {
                            taskGroupName = "任务组 " + taskGroupId;
                        }
                        // 切换任务组
                        switchToTaskGroupAndLocate(taskGroupId, taskGroupName, () -> {
                            boolean located = canvas.locateConnectionByEdgeId(edgeKey);
                            if (!located) {
                                logPanel.warn("⚠ 未在画布上找到该连接: " + edgeKey);
                            }
                        });
                        return;
                    }
                }
                
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
            
            @Override
            public void onPartitionAction(Long partitionId, String partitionName, TaskTreeView.TaskSelectionCallback.PartitionAction action) {
                switch (action) {
                    case EDIT -> editPartition(partitionId, partitionName);
                    case EXPORT -> exportPartition(partitionId, partitionName);
                    default -> { }
                }
            }
            
            @Override
            public void onJobGroupEdit(Long taskGroupId, String taskGroupName) {
                editJobGroup(taskGroupId, taskGroupName);
            }
        });
    }
    
    /**
     * 编辑分区
     */
    private void editPartition(Long partitionId, String partitionName) {
        try {
            logPanel.info("✏️ 编辑分区: " + partitionName);
            
            // 创建文本输入对话框
            TextInputDialog dialog = new TextInputDialog(partitionName);
            dialog.setTitle("编辑分区");
            dialog.setHeaderText("请输入新的分区名称");
            dialog.setContentText("分区名称:");
            
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newName -> {
                if (newName.trim().isEmpty()) {
                    logPanel.warn("⚠ 分区名称不能为空");
                    return;
                }
                
                if (newName.equals(partitionName)) {
                    logPanel.info("ℹ 分区名称未更改");
                    return;
                }
                
                // 在后台线程中更新分区
                new Thread(() -> {
                    try {
                        boolean success = jobPartService.updateJobPart(partitionId, newName.trim());
                        Platform.runLater(() -> {
                            if (success) {
                                logPanel.success("✓ 分区名称已更新: " + partitionName + " → " + newName.trim());
                                refreshTreeView();
                            } else {
                                logPanel.error("✗ 更新分区失败");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            logPanel.error("✗ 更新分区时发生错误: " + e.getMessage());
                            logger.error("更新分区失败", e);
                        });
                    }
                }).start();
            });
        } catch (Exception e) {
            logPanel.error("✗ 显示编辑对话框失败: " + e.getMessage());
            logger.error("显示编辑分区对话框失败", e);
        }
    }
    
    /**
     * 打开分区文件并导入
     */
    private void openPartitionFile() {
        try {
            logPanel.info("📂 正在打开分区文件...");
            
            Platform.runLater(() -> {
                // 显示文件选择对话框
                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                fileChooser.setTitle("选择要导入的分区文件");
                fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("加密数据文件", "*.cetl")
                );
                
                javafx.stage.Window window = this.getScene().getWindow();
                java.io.File file = fileChooser.showOpenDialog(window);
                
                if (file != null) {
                    // 在后台线程中导入数据
                    new Thread(() -> {
                        try {
                            logPanel.info("📥 正在导入分区数据: " + file.getName());
                            boolean success = jobPartService.importData(file);
                            
                            if (success) {
                                // 等待一小段时间，确保后端数据已完全保存
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                
                                Platform.runLater(() -> {
                                    logPanel.success("✓ 分区数据导入成功");
                                    
                                    // ⚠️ 关键修复1：清除所有任务组名称到ID的映射缓存，避免使用旧的ID
                                    taskGroupNameToIdMap.clear();
                                    logPanel.info("ℹ 已清除任务组ID缓存");
                                    
                                    // ⚠️ 关键修复2：清除节点状态缓存，避免使用旧的jobId状态
                                    com.cc.job.gui.util.NodeStatusSyncManager.getInstance().clearCacheForTaskGroupSwitch();
                                    logPanel.info("ℹ 已清除节点状态缓存");
                                    
                                    // ⚠️ 关键修复3：清空画布，避免显示旧的节点
                                    canvas.clear();
                                    logPanel.info("ℹ 已清空画布");
                                    
                                    // ⚠️ 关键修复4：清空当前页面存储，避免使用旧的任务组ID
                                    usePageStoreHook().setCurrentPage(0L);
                                    navigationBar.setCurrentTaskGroupId(null);
                                    toolBar.setCurrentTaskGroupId(null);
                                    logPanel.info("ℹ 已清空页面存储");
                                    
                                    // ⚠️ 关键修复5：清空导航栏的所有任务组标签，避免使用旧的任务组ID
                                    navigationBar.clearAllTasks();
                                    logPanel.info("ℹ 已清空导航栏任务组标签");
                                    
                                    // 刷新树形视图（这会重新从后端获取最新数据，包括新的任务组ID）
                                    if (treeView != null) {
                                        logPanel.info("🔄 正在刷新树形视图，获取最新的任务组数据...");
                                        // 先清空树形视图，确保完全重新加载
                                        treeView.clearSelection();
                                        // 刷新树形数据（异步操作）
                                        treeView.refreshTreeData();
                                        
                                        // ⚠️ 关键修复6：等待树形视图刷新完成后再提示用户
                                        // 使用延迟确保树形视图数据已完全加载
                                        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(800));
                                        delay.setOnFinished(e -> {
                                            logPanel.info("ℹ 已刷新树形视图，请查看导入的分区");
                                            logPanel.warn("⚠ 重要提示：导入后的任务组是全新的，ID已更新，请点击新导入的任务组查看");
                                            logPanel.info("💡 提示：如果点击后还是跳转到旧页面，请检查日志中的任务组ID是否正确");
                                        });
                                        delay.play();
                                    }
                                });
                            } else {
                                Platform.runLater(() -> {
                                    logPanel.error("✗ 分区数据导入失败");
                                });
                            }
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                logPanel.error("✗ 导入分区数据失败: " + e.getMessage());
                                logger.error("导入分区数据失败", e);
                            });
                        }
                    }, "import-partition-thread").start();
                } else {
                    logPanel.info("ℹ 已取消打开文件");
                }
            });
        } catch (Exception e) {
            logPanel.error("✗ 打开文件失败: " + e.getMessage());
            logger.error("打开文件失败", e);
        }
    }
    
    /**
     * 导出分区数据
     */
    private void exportPartition(Long partitionId, String partitionName) {
        try {
            logPanel.info("📤 正在导出分区: " + partitionName);
            
            // 在后台线程中导出数据
            new Thread(() -> {
                try {
                    byte[] data = jobPartService.exportData(partitionId);
                    
                    Platform.runLater(() -> {
                        // 显示文件保存对话框
                        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                        fileChooser.setTitle("保存分区数据");
                        fileChooser.setInitialFileName("encryptedData.cetl");
                        fileChooser.getExtensionFilters().add(
                            new javafx.stage.FileChooser.ExtensionFilter("加密数据文件", "*.cetl")
                        );
                        
                        javafx.stage.Window window = this.getScene().getWindow();
                        java.io.File file = fileChooser.showSaveDialog(window);
                        
                        if (file != null) {
                            try {
                                java.nio.file.Files.write(file.toPath(), data);
                                logPanel.success("✓ 分区数据已导出到: " + file.getAbsolutePath());
                            } catch (java.io.IOException e) {
                                logPanel.error("✗ 保存文件失败: " + e.getMessage());
                                logger.error("保存导出文件失败", e);
                            }
                        } else {
                            logPanel.info("ℹ 已取消导出");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        logPanel.error("✗ 导出分区数据失败: " + e.getMessage());
                        logger.error("导出分区数据失败", e);
                    });
                }
            }).start();
        } catch (Exception e) {
            logPanel.error("✗ 导出分区失败: " + e.getMessage());
            logger.error("导出分区失败", e);
        }
    }

    /**
     * 编辑任务组
     */
    private void editJobGroup(Long taskGroupId, String taskGroupName) {
        try {
            logPanel.info("✏️ 编辑任务组: " + taskGroupName);
            
            // 在后台线程中获取任务组数据
            new Thread(() -> {
                try {
                    // 获取任务组表单数据
                    com.cc.job.xo.model.form.JobInfoForm formData = jobInfoService.getFormData(taskGroupId);
                    
                    if (formData == null) {
                        Platform.runLater(() -> {
                            logPanel.error("✗ 无法获取任务组数据");
                        });
                        return;
                    }
                    
                    // 获取分区ID（从任务组数据中获取）
                    Integer jobPartId = formData.getJobPartId();
                    if (jobPartId == null) {
                        Platform.runLater(() -> {
                            logPanel.error("✗ 无法获取分区ID");
                        });
                        return;
                    }
                    Long partitionId = jobPartId.longValue();
                    
                    // 获取分区名称（从树中查找）
                    String partitionName = "未知分区";
                    
                    Platform.runLater(() -> {
                        // 显示编辑对话框
                        showNewJobGroupDialog(partitionId, partitionName, formData);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        logPanel.error("✗ 获取任务组数据失败: " + e.getMessage());
                        logger.error("获取任务组数据失败", e);
                    });
                }
            }).start();
        } catch (Exception e) {
            logPanel.error("✗ 编辑任务组失败: " + e.getMessage());
            logger.error("编辑任务组失败", e);
        }
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
                                        logger.error("保存任务组失败: {}", e.getMessage(), e);

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
                            logger.error("显示新建任务组对话框失败: {}", e.getMessage(), e);
                            logPanel.error("✗ 打开对话框失败: " + e.getMessage());
                        }
                    });

                } catch (Exception e) {
                    logger.error("加载执行器列表失败: {}", e.getMessage(), e);

                    Platform.runLater(() -> {
                        logPanel.error("✗ 加载执行器列表失败: " + e.getMessage());
                        logPanel.warn("提示: 请检查后端服务是否正常运行");
                        logPanel.info("════════════════════════════════");
                    });
                }
            }).start();

        } catch (Exception e) {
            logger.error("显示新建任务组对话框失败: {}", e.getMessage(), e);
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
                                        logger.error("保存任务节点失败: {}", e.getMessage(), e);

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
                            logger.error("显示新建任务节点对话框失败: {}", e.getMessage(), e);
                            logPanel.error("✗ 打开对话框失败: " + e.getMessage());
                        }
                    });

                } catch (Exception e) {
                    logger.error("加载执行器列表失败: {}", e.getMessage(), e);

                    Platform.runLater(() -> {
                        logPanel.error("✗ 加载执行器列表失败: " + e.getMessage());
                        logPanel.warn("提示: 请检查后端服务是否正常运行");
                        logPanel.info("════════════════════════════════");
                    });
                }
            }).start();

        } catch (Exception e) {
            logger.error("显示新建任务节点对话框失败: {}", e.getMessage(), e);
            logPanel.error("✗ 打开对话框失败: " + e.getMessage());
            logPanel.info("════════════════════════════════");
        }
    }

    /**
     * 在画布上添加节点
     */
    private void addNodeToCanvas(com.cc.job.xo.model.entity.JobNode jobNode, com.cc.job.xo.model.form.JobInfoForm formData) {
        addNodeToCanvasAndReturn(jobNode, formData);
    }
    
    /**
     * 添加节点到画布并返回节点对象
     * @param jobNode 任务节点实体
     * @param formData 任务表单数据
     * @return 创建的ProcessNode对象，如果失败返回null
     */
    private ProcessNode addNodeToCanvasAndReturn(com.cc.job.xo.model.entity.JobNode jobNode, com.cc.job.xo.model.form.JobInfoForm formData) {
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
            
            return node;

        } catch (Exception e) {
            logger.error("添加节点到画布失败: {}", e.getMessage(), e);
            logPanel.error("✗ 添加节点到画布失败: " + e.getMessage());
            return null;
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
        
        // 设置禁用/启用节点回调
        node.setOnDisable((Long jobId, boolean isDisabled) -> {
            if (jobId == null) {
                logPanel.warn("⚠ 该节点未绑定后端任务，无法禁用/启用");
                return;
            }
            disableNode(jobId, isDisabled, node);
        });
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

                // 保存到剪贴板（用于粘贴）
                copiedNodeForm = copyForm;
                
                // 立即创建新节点（右键菜单行为）
                copyForm.setId(null);
                copyForm.setParentId(currentTaskGroupId);
                copyForm.setJobDesc(generateCopyName(copyForm.getJobDesc()));
                copyForm.setNodePositionX(sourceNode.getLayoutX() + 60);
                copyForm.setNodePositionY(sourceNode.getLayoutY() + 40);
                
                // 清除不应该复制的字段
                copyForm.setGlueUpdatetime(null); // GLUE更新时间应该由后端管理
                
                // 确保必填字段不为空
                if (copyForm.getExecutorParam() == null) {
                    copyForm.setExecutorParam("");
                }
                if (copyForm.getExecutorRouteStrategy() == null || copyForm.getExecutorRouteStrategy().isEmpty()) {
                    copyForm.setExecutorRouteStrategy("FIRST"); // 默认路由策略
                }
                if (copyForm.getExecutorBlockStrategy() == null || copyForm.getExecutorBlockStrategy().isEmpty()) {
                    copyForm.setExecutorBlockStrategy("SERIAL_EXECUTION"); // 默认阻塞策略
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
                        // 添加节点到画布
                        ProcessNode newNode = addNodeToCanvasAndReturn(newJobNode, copyForm);
                        
                        // 选中新复制的节点
                        if (newNode != null) {
                            canvas.selectNode(newNode);
                        }
                        
                        // 刷新任务树
                        refreshTreeViewWithoutNavigation(currentTaskGroupId);
                        logPanel.success("✓ 节点复制成功: " + copyForm.getJobDesc());
                        logPanel.info("新节点已自动选中，位置: (" + copyForm.getNodePositionX() + ", " + copyForm.getNodePositionY() + ")");
                    } catch (Exception e) {
                        logger.error("添加复制节点到画布失败: {}", e.getMessage(), e);
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
    
    /**
     * 粘贴节点（从剪贴板）
     */
    private void pasteNode() {
        if (copiedNodeForm == null) {
            logPanel.warn("⚠ 没有可粘贴的节点，请先复制一个节点");
            return;
        }
        
        Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
        if (currentTaskGroupId == null) {
            logPanel.warn("⚠ 请先选择任务组后再粘贴节点");
            return;
        }
        
        logPanel.info("════════════════════════════════");
        logPanel.info("📋 正在粘贴节点: " + safeString(copiedNodeForm.getJobDesc()));
        
        new Thread(() -> {
            try {
                // 创建新的表单数据（深拷贝）
                JobInfoForm pasteForm = deepCopyJobInfoForm(copiedNodeForm);
                if (pasteForm == null) {
                    Platform.runLater(() -> logPanel.error("✗ 粘贴节点数据失败"));
                    return;
                }
                
                // 设置新节点的属性
                pasteForm.setId(null);
                pasteForm.setParentId(currentTaskGroupId);
                pasteForm.setJobDesc(generateCopyName(copiedNodeForm.getJobDesc()));
                
                // 计算粘贴位置：在画布中心或鼠标位置附近
                // 这里简单处理，放在画布中心附近
                double[] pastePosition = calculatePastePosition();
                pasteForm.setNodePositionX(pastePosition[0]);
                pasteForm.setNodePositionY(pastePosition[1]);
                
                // 清除不应该复制的字段
                pasteForm.setGlueUpdatetime(null);
                
                // 确保必填字段不为空
                if (pasteForm.getExecutorParam() == null) {
                    pasteForm.setExecutorParam("");
                }
                if (pasteForm.getExecutorRouteStrategy() == null || pasteForm.getExecutorRouteStrategy().isEmpty()) {
                    pasteForm.setExecutorRouteStrategy("FIRST");
                }
                if (pasteForm.getExecutorBlockStrategy() == null || pasteForm.getExecutorBlockStrategy().isEmpty()) {
                    pasteForm.setExecutorBlockStrategy("SERIAL_EXECUTION");
                }
                
                // 保存到后端
                com.cc.job.xo.model.entity.JobNode newJobNode = jobInfoService.saveJobNode(pasteForm);
                if (newJobNode == null) {
                    Platform.runLater(() -> logPanel.error("✗ 粘贴节点失败：后端返回空数据"));
                    return;
                }
                
                if (pasteForm.getNodePositionX() != null) {
                    newJobNode.setNodePositionX(pasteForm.getNodePositionX());
                }
                if (pasteForm.getNodePositionY() != null) {
                    newJobNode.setNodePositionY(pasteForm.getNodePositionY());
                }
                
                Platform.runLater(() -> {
                    try {
                        // 清除原始节点的选中状态（框框消失）
                        canvas.clearSelection();
                        
                        // 添加节点到画布
                        ProcessNode newNode = addNodeToCanvasAndReturn(newJobNode, pasteForm);
                        
                        // 选中新粘贴的节点（显示红色框框）
                        if (newNode != null) {
                            canvas.selectNode(newNode);
                        }
                        
                        // 刷新任务树
                        refreshTreeViewWithoutNavigation(currentTaskGroupId);
                        logPanel.success("✓ 节点粘贴成功: " + pasteForm.getJobDesc());
                        logPanel.info("新节点已自动选中，位置: (" + pasteForm.getNodePositionX() + ", " + pasteForm.getNodePositionY() + ")");
                        logPanel.info("💡 提示: 请点击保存按钮以持久化节点");
                    } catch (Exception e) {
                        logger.error("添加粘贴节点到画布失败: {}", e.getMessage(), e);
                        logPanel.error("✗ 添加粘贴节点到画布失败: " + e.getMessage());
                    } finally {
                        logPanel.info("════════════════════════════════");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    logPanel.error("✗ 节点粘贴失败: " + e.getMessage());
                    logPanel.info("════════════════════════════════");
                });
            }
        }, "paste-node-thread").start();
    }
    
    /**
     * 粘贴多个节点（从剪贴板）
     */
    private void pasteNodes() {
        if (copiedNodesData == null || copiedNodesData.nodeForms.isEmpty()) {
            logPanel.warn("⚠ 没有可粘贴的节点，请先复制节点");
            return;
        }
        
        Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
        if (currentTaskGroupId == null) {
            logPanel.warn("⚠ 请先选择任务组后再粘贴节点");
            return;
        }
        
        logPanel.info("════════════════════════════════");
        logPanel.info("📋 正在粘贴 " + copiedNodesData.nodeForms.size() + " 个节点...");
        
        new Thread(() -> {
            try {
                // 计算粘贴位置（画布中心）
                double[] pastePosition = calculatePastePosition();
                double offsetX = pastePosition[0] - copiedNodesData.minX;
                double offsetY = pastePosition[1] - copiedNodesData.minY;
                
                // jobId映射：原始jobId -> 新创建的节点
                java.util.List<ProcessNode> newNodes = new java.util.ArrayList<>();
                
                // 1. 创建所有节点
                for (CopiedNodesData.NodeFormData nodeData : copiedNodesData.nodeForms) {
                    try {
                        JobInfoForm originalForm = nodeData.form;
                        
                        // 创建新的表单数据（深拷贝）
                        JobInfoForm pasteForm = deepCopyJobInfoForm(originalForm);
                        if (pasteForm == null) {
                            logPanel.warn("⚠ 粘贴节点数据失败: " + safeString(originalForm.getJobDesc()));
                            continue;
                        }
                        
                        // 设置新节点的属性
                        pasteForm.setId(null);
                        pasteForm.setParentId(currentTaskGroupId);
                        pasteForm.setJobDesc(generateCopyName(originalForm.getJobDesc()));
                        
                        // 计算新位置（保持相对位置）
                        double originalX = originalForm.getNodePositionX() != null ? originalForm.getNodePositionX() : 0;
                        double originalY = originalForm.getNodePositionY() != null ? originalForm.getNodePositionY() : 0;
                        pasteForm.setNodePositionX(originalX + offsetX);
                        pasteForm.setNodePositionY(originalY + offsetY);
                        
                        // 清除不应该复制的字段
                        pasteForm.setGlueUpdatetime(null);
                        
                        // 确保必填字段不为空
                        if (pasteForm.getExecutorParam() == null) {
                            pasteForm.setExecutorParam("");
                        }
                        if (pasteForm.getExecutorRouteStrategy() == null || pasteForm.getExecutorRouteStrategy().isEmpty()) {
                            pasteForm.setExecutorRouteStrategy("FIRST");
                        }
                        if (pasteForm.getExecutorBlockStrategy() == null || pasteForm.getExecutorBlockStrategy().isEmpty()) {
                            pasteForm.setExecutorBlockStrategy("SERIAL_EXECUTION");
                        }
                        
                        // 保存到后端
                        com.cc.job.xo.model.entity.JobNode newJobNode = jobInfoService.saveJobNode(pasteForm);
                        if (newJobNode == null) {
                            logPanel.warn("⚠ 粘贴节点失败：后端返回空数据: " + safeString(pasteForm.getJobDesc()));
                            continue;
                        }
                        
                        if (pasteForm.getNodePositionX() != null) {
                            newJobNode.setNodePositionX(pasteForm.getNodePositionX());
                        }
                        if (pasteForm.getNodePositionY() != null) {
                            newJobNode.setNodePositionY(pasteForm.getNodePositionY());
                        }
                        
                        final Long originalJobId = nodeData.originalJobId;
                        final Long newJobId = newJobNode.getJobId();
                        
                        // 在UI线程中添加节点到画布
                        Platform.runLater(() -> {
                            try {
                                ProcessNode newNode = addNodeToCanvasAndReturn(newJobNode, pasteForm);
                                if (newNode != null) {
                                    newNodes.add(newNode);
                                }
                            } catch (Exception e) {
                                logger.error("添加粘贴节点到画布失败: {}", e.getMessage(), e);
                            }
                        });
                    } catch (Exception e) {
                        logger.error("粘贴节点失败: {}", e.getMessage(), e);
                    }
                }
                
                // 等待所有节点创建完成
                Thread.sleep(500);
                
                // 2. 恢复连接关系
                Platform.runLater(() -> {
                    try {
                        // 新的节点
                        java.util.Map<Integer, ProcessNode> indexToNewNodeMap = new java.util.HashMap<>();
                        for (int i = 0; i < newNodes.size() && i < copiedNodesData.nodeForms.size(); i++) {
                            indexToNewNodeMap.put(i, newNodes.get(i));
                        }
                        
                        // 建立原始jobId到索引的映射
                        java.util.Map<Long, Integer> originalJobIdToIndex = new java.util.HashMap<>();
                        for (int i = 0; i < copiedNodesData.nodeForms.size(); i++) {
                            CopiedNodesData.NodeFormData nodeData = copiedNodesData.nodeForms.get(i);
                            if (nodeData.originalJobId != null) {
                                originalJobIdToIndex.put(nodeData.originalJobId, i);
                            }
                        }
                        
                        // 恢复连接关系
                        int connectionCount = 0;
                        java.util.concurrent.atomic.AtomicInteger savedConnectionCount = new java.util.concurrent.atomic.AtomicInteger(0);
                        for (CopiedNodesData.ConnectionInfo connInfo : copiedNodesData.connections) {
                            try {
                                // 通过原始jobId找到索引，再通过索引找到新节点
                                Integer sourceIndex = originalJobIdToIndex.get(connInfo.sourceJobId);
                                Integer targetIndex = originalJobIdToIndex.get(connInfo.targetJobId);
                                
                                if (sourceIndex != null && targetIndex != null) {
                                    ProcessNode sourceNode = indexToNewNodeMap.get(sourceIndex);
                                    ProcessNode targetNode = indexToNewNodeMap.get(targetIndex);
                                    
                                    if (sourceNode != null && targetNode != null) {
                                        // 根据锚点位置获取连接点
                                        javafx.scene.shape.Circle sourceConnector = getConnectorByAnchor(
                                            sourceNode, connInfo.sourceAnchor, true);
                                        javafx.scene.shape.Circle targetConnector = getConnectorByAnchor(
                                            targetNode, connInfo.targetAnchor, false);
                                        
                                        if (sourceConnector != null && targetConnector != null) {
                                            // 创建连线
                                            NodeConnection connection = canvas.addConnection(sourceNode, sourceConnector, 
                                                                targetNode, targetConnector, true);
                                            connectionCount++;
                                            
                                            // 保存连线到数据库
                                            if (connection != null && sourceNode.getNodeId() != null && targetNode.getNodeId() != null) {
                                                try {
                                                    com.cc.job.xo.model.form.JobEdgeForm edgeForm = new com.cc.job.xo.model.form.JobEdgeForm();
                                                    edgeForm.setJobParentId(currentTaskGroupId);

                                                    edgeForm.setFromNodeId(Long.parseLong(sourceNode.getNodeId()));
                                                    edgeForm.setEndNodeId(Long.parseLong(targetNode.getNodeId()));
                                                    edgeForm.setStartPoint(connInfo.sourceAnchor);
                                                    edgeForm.setEndPoint(connInfo.targetAnchor);
                                                    
                                                    // 在后台线程中保存连线
                                                    new Thread(() -> {
                                                        try {
                                                            com.cc.job.xo.model.entity.JobEdge savedEdge = jobInfoService.saveJobEdge(edgeForm);
                                                            if (savedEdge != null) {
                                                                savedConnectionCount.incrementAndGet();
                                                            }
                                                        } catch (Exception e) {
                                                            logger.error("保存连线到数据库失败: {}", e.getMessage(), e);
                                                        }
                                                    }, "save-edge-thread").start();
                                                } catch (Exception e) {
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                throw new Exception(e.getMessage());
                            }
                        }
                        
                        // 等待连线保存完成
                        Thread.sleep(300);
                        
                        // 清除原始节点的选中状态（框框消失），然后选中所有新粘贴的节点（显示红色框框）
                        canvas.clearSelection();
                        canvas.selectNodes(newNodes);
                        
                        // 刷新任务树
                        refreshTreeViewWithoutNavigation(currentTaskGroupId);
                        logPanel.success("✓ " + newNodes.size() + " 个节点粘贴成功");
                        if (connectionCount > 0) {
                            logPanel.info("✓ 已恢复 " + connectionCount + " 条连接关系");
                            int savedCount = savedConnectionCount.get();
                            if (savedCount > 0) {
                                logPanel.info("✓ 已保存 " + savedCount + " 条连线到数据库");
                            }
                        } else if (!copiedNodesData.connections.isEmpty()) {
                            logPanel.info("提示: 部分连接关系未能恢复，请手动检查");
                        }
                    } catch (Exception e) {
                        logger.error("恢复连接关系失败: {}", e.getMessage(), e);
                        logPanel.error("✗ 恢复连接关系失败: " + e.getMessage());
                    } finally {
                        logPanel.info("════════════════════════════════");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    logPanel.error("✗ 节点粘贴失败: " + e.getMessage());
                    logPanel.info("════════════════════════════════");
                });
            }
        }, "paste-nodes-thread").start();
    }
    
    /**
     * 计算粘贴位置
     * @return [x, y] 坐标数组
     */
    private double[] calculatePastePosition() {
        // 获取画布的视口中心位置
        if (scrollPane != null) {
            javafx.geometry.Bounds viewportBounds = scrollPane.getViewportBounds();
            if (viewportBounds != null) {
                double centerX = viewportBounds.getWidth() / 2;
                double centerY = viewportBounds.getHeight() / 2;
                
                // 转换为画布坐标
                javafx.geometry.Point2D canvasPoint = scrollPane.localToParent(centerX, centerY);
                if (canvasPoint != null) {
                    return new double[]{canvasPoint.getX(), canvasPoint.getY()};
                }
            }
        }
        
        // 如果无法获取视口中心，使用默认位置
        return new double[]{300, 200};
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
                // 如果是SQL模式，获取数据源列表
                List<com.cc.job.xo.model.entity.JobJdbcDatasource> datasources = null;
                if (form != null && "SQL".equals(form.getGlueType())) {
                    try {
                        com.cc.job.gui.service.JobJdbcDatasourceService datasourceService = new com.cc.job.gui.service.JobJdbcDatasourceService();
                        datasources = datasourceService.getDatasourceList();
                    } catch (Exception e) {
                    }
                }
                List<com.cc.job.xo.model.entity.JobJdbcDatasource> finalDatasources = datasources;
                Platform.runLater(() -> {
                    showJobNodeDetailDialog(node, form, jobGroups, null, null, finalDatasources);
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

    /**
     * 禁用/启用节点
     * @param jobId 任务ID
     * @param isDisabled 是否禁用（true=禁用，false=启用）
     * @param node 画布上的节点对象
     */
    private void disableNode(Long jobId, boolean isDisabled, ProcessNode node) {
        if (jobId == null) {
            logPanel.warn("⚠ 任务ID为空，无法禁用/启用节点");
            // 恢复节点状态
            if (node != null) {
                javafx.application.Platform.runLater(() -> {
                    node.restoreEnabledState(!isDisabled);
                });
            }
            return;
        }
        
        String action = isDisabled ? "禁用" : "启用";
        boolean originalState = !isDisabled; // 保存原始状态，用于失败时恢复
        logPanel.info("正在" + action + "节点: " + (node != null ? node.getJobHandlerName() : "未知") + " (ID: " + jobId + ")");
        
        // 在后台线程中调用后端API
        new Thread(() -> {
            try {
                // isPause: 0=启用, 1=禁用
                Integer isPause = isDisabled ? 1 : 0;
                boolean success = jobInfoService.pauseJob(jobId, isPause);
                
                Platform.runLater(() -> {
                    if (success) {
                        logPanel.success("✓ 节点已" + action + ": " + (node != null ? node.getJobHandlerName() : "未知"));
                    } else {
                        logPanel.error("✗ 节点" + action + "失败: " + (node != null ? node.getJobHandlerName() : "未知"));
                        // 如果后端调用失败，恢复UI状态
                        if (node != null) {
                            node.restoreEnabledState(originalState);
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("禁用/启用节点失败: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    logPanel.error("✗ 节点" + action + "失败: " + e.getMessage());
                    // 如果后端调用失败，恢复UI状态
                    if (node != null) {
                        node.restoreEnabledState(originalState);
                    }
                });
            }
        }, "disable-node-thread").start();
    }

    /**
     * 根据jobId显示节点详情（不依赖画布上的节点）
     */
    private void showNodeDetailsByJobId(Long jobId, String nodeName) {
        if (jobId == null) {
            logPanel.warn("⚠ 该节点缺少任务ID，无法查看详情: " + nodeName);
            return;
        }

        logPanel.info("════════════════════════════════");
        logPanel.info("🔍 正在获取节点详情: " + nodeName);

        new Thread(() -> {
            try {
                JobInfoForm form = jobInfoService.getJobNodeFormData(jobId);
                List<JobGroup> jobGroups = jobGroupService.getAllJobGroupList();
                // 如果是SQL模式，获取数据源列表
                List<com.cc.job.xo.model.entity.JobJdbcDatasource> datasources = null;
                if (form != null && "SQL".equals(form.getGlueType())) {
                    try {
                        com.cc.job.gui.service.JobJdbcDatasourceService datasourceService = new com.cc.job.gui.service.JobJdbcDatasourceService();
                        datasources = datasourceService.getDatasourceList();
                    } catch (Exception e) {
                    }
                }
                List<com.cc.job.xo.model.entity.JobJdbcDatasource> finalDatasources = datasources;
                Platform.runLater(() -> {
                    showJobNodeDetailDialog(null, form, jobGroups, jobId, nodeName, finalDatasources);
                    logPanel.info("════════════════════════════════");
                });
            } catch (Exception e) {
                logger.error("获取节点详情失败: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    logPanel.error("✗ 获取节点详情失败: " + e.getMessage());
                    logPanel.info("════════════════════════════════");
                });
            }
        }, "detail-node-thread").start();
    }

    private void showJobNodeDetailDialog(ProcessNode node, JobInfoForm form, List<JobGroup> jobGroups) {
        // 兼容旧方法
        Long jobId = node != null ? node.getJobId() : null;
        String nodeName = node != null ? node.getJobHandlerName() : null;
        showJobNodeDetailDialog(node, form, jobGroups, jobId, nodeName, null);
    }
    
    private void showJobNodeDetailDialog(ProcessNode node, JobInfoForm form, List<JobGroup> jobGroups, Long jobId, String nodeName) {
        showJobNodeDetailDialog(node, form, jobGroups, jobId, nodeName, null);
    }
    
    private void showJobNodeDetailDialog(ProcessNode node, JobInfoForm form, List<JobGroup> jobGroups, Long jobId, String nodeName, List<com.cc.job.xo.model.entity.JobJdbcDatasource> datasources) {
        if (form == null) {
            logPanel.warn("⚠ 未获取到节点详情数据");
            return;
        }
        
        // 确定节点名称：优先使用传入的nodeName，其次使用form中的jobDesc，最后使用节点的jobHandlerName
        String displayName = nodeName != null ? nodeName : 
                            (form.getJobDesc() != null ? form.getJobDesc() : 
                            (node != null ? node.getJobHandlerName() : "未知节点"));
        
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("节点详情 - " + safeString(displayName));
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));

        int rowIndex = 0;
        addDetailRow(grid, rowIndex++, "任务描述", form.getJobDesc());
        addDetailRow(grid, rowIndex++, "执行器", findJobGroupName(jobGroups, form.getJobGroup()));
        addDetailRow(grid, rowIndex++, "JobHandler", form.getExecutorHandler());
        addDetailRow(grid, rowIndex++, "运行模式", form.getGlueType());
        
        // SQL模式：显示数据库信息
        if ("SQL".equals(form.getGlueType()) && form.getJdbcDatasourceId() != null) {
            String datasourceName = null;
            if (datasources != null) {
                datasourceName = datasources.stream()
                    .filter(ds -> ds.getId().equals(form.getJdbcDatasourceId()))
                    .findFirst()
                    .map(com.cc.job.xo.model.entity.JobJdbcDatasource::getDatabaseName)
                    .orElse(null);
            }
            addDetailRow(grid, rowIndex++, "数据库", datasourceName != null ? datasourceName : ("ID: " + form.getJdbcDatasourceId()));
        }
        
        addDetailRow(grid, rowIndex++, "任务参数", form.getExecutorParam());
        addDetailRow(grid, rowIndex++, "负责人", form.getAuthor());
        addDetailRow(grid, rowIndex++, "报警邮件", form.getAlarmEmail());
        addDetailRow(grid, rowIndex++, "阻塞策略", form.getExecutorBlockStrategy());
        addDetailRow(grid, rowIndex++, "超时时间(秒)", form.getExecutorTimeout());
        addDetailRow(grid, rowIndex++, "失败重试次数", form.getExecutorFailRetryCount());
        addDetailRow(grid, rowIndex++, "所属任务组ID", form.getParentId());
        addDetailRow(grid, rowIndex++, "Job ID", jobId);

        // 节点ID优先取表单返回，其次取画布节点
        String nodeIdStr = form.getNodeId() != null ? form.getNodeId()
                : (node != null ? node.getNodeId() : null);
        addDetailRow(grid, rowIndex++, "节点ID", nodeIdStr);

        // 运行时长（毫秒）
        addDetailRow(grid, rowIndex++, "运行时长(ms)", form.getRunTime());

        // 运行开始时间 / 预测结束时间（来自SSE缓存）
        String startTime = "无";
        String endTime = "无";
        if (nodeIdStr != null) {
            ensurePredictedTimeCache();
            String[] pair = predictedNodeTimes != null ? predictedNodeTimes.get(nodeIdStr) : null;
            if (pair != null && pair.length >= 2) {
                startTime = pair[0];
                endTime = pair[1];
            }
        }
        addDetailRow(grid, rowIndex++, "运行开始时间", startTime);
        addDetailRow(grid, rowIndex++, "预测任务结束时间", endTime);

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
        // 使用当前任务组ID
        Long currentTaskGroupId = usePageStoreHook().getCurrentTaskGroupId();
        editNode(jobId, node, currentTaskGroupId);
    }
    
    private void editNode(Long jobId, ProcessNode node, Long taskGroupId) {
        try {
            logPanel.info("════════════════════════════════");
            if (node != null) {
                logPanel.info("📝 编辑任务节点 - 任务ID: " + jobId + ", 节点ID: " + node.getNodeId());
            } else {
                logPanel.info("📝 编辑任务节点 - 任务ID: " + jobId);
            }

            if (jobId == null) {
                logPanel.error("✗ jobId 为 null，无法编辑");
                return;
            }

            // 使用传入的任务组ID，如果没有则使用当前任务组ID
            Long finalTaskGroupId = taskGroupId != null ? taskGroupId : usePageStoreHook().getCurrentTaskGroupId();
            
            if (finalTaskGroupId == null) {
                logPanel.error("✗ 无法获取任务组ID");
                return;
            }
            
            logPanel.info("任务组ID: " + finalTaskGroupId);

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
                                finalTaskGroupId,
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

                                            // 更新画布上的节点显示（如果节点存在）
                                            if (node != null) {
                                                String newNodeType = getNodeTypeIcon(updatedFormData.getGlueType());
                                                node.updateNodeInfo(updatedFormData.getJobDesc(), newNodeType);
                                                logPanel.info("✓ 画布节点已更新");
                                            }

                                            // 更新树形视图（只更新节点文本，不刷新整个树，避免跳转）
                                            if (treeView != null && jobId != null) {
                                                treeView.updateJobNode(jobId, updatedFormData.getJobDesc());
                                                logPanel.info("✓ 树形视图节点已更新");
                                            }
                                        });
                                    } else {
                                        Platform.runLater(() -> {
                                            logPanel.error("✗ 任务节点更新失败");
                                        });
                                    }

                                } catch (Exception e) {
                                    logger.error("更新任务节点失败: {}", e.getMessage(), e);

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
                    logger.error("获取节点数据失败: {}", e.getMessage(), e);

                    Platform.runLater(() -> {
                        logPanel.error("✗ 获取节点数据失败: " + e.getMessage());
                        logPanel.info("════════════════════════════════");
                    });
                }
            }).start();

        } catch (Exception e) {
            logger.error("打开编辑对话框失败: {}", e.getMessage(), e);
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
    
    /**
     * 删除任务组容器
     */
    private void deleteGroupContainer(com.cc.job.gui.model.GroupContainer container) {
        if (container == null) {
            logPanel.warn("⚠ 任务组容器为空，无法删除");
            return;
        }
        
        String nodeId = container.getNodeId();
        String groupName = container.getGroupName();
        
        if (nodeId == null || nodeId.isEmpty()) {
            logPanel.warn("⚠ 任务组容器节点ID为空，无法删除");
            return;
        }
        
        // 显示确认对话框
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除任务组节点");
        alert.setHeaderText("确认删除 \"" + groupName + "\" ?");
        alert.setContentText("删除后，该任务组节点及其所有子节点都将被移除，且不可恢复。");
        
        javafx.scene.control.ButtonType confirmButton = new javafx.scene.control.ButtonType("确认删除", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType cancelButton = new javafx.scene.control.ButtonType("取消", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmButton, cancelButton);
        
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != confirmButton) {
            return;
        }
        
        // 解析节点ID
        Long nodeIdLong;
        try {
            nodeIdLong = Long.parseLong(nodeId);
        } catch (NumberFormatException e) {
            logPanel.error("✗ 节点ID格式错误: " + nodeId);
            return;
        }
        
        logPanel.info("🗑️ 正在删除任务组节点: " + groupName + " (ID: " + nodeId + ")");
        
        // 在后台线程中执行删除
        new Thread(() -> {
            boolean success = false;
            String errorMessage = null;
            
            try {
                success = jobPartService.deleteJobNode(nodeIdLong);
            } catch (IOException ex) {
                errorMessage = ex.getMessage();
                logger.error("删除任务组节点发生异常: {}", ex.getMessage(), ex);
            }
            
            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;
            
            Platform.runLater(() -> {
                if (finalSuccess) {
                    // 从画布中移除容器
                    canvas.removeGroupContainer(container);
                    logPanel.success("✓ 任务组节点 \"" + groupName + "\" 已删除");
                    
                    // 刷新树形视图（可选，如果需要的话）
                    // treeView.loadTreeData();
                } else {
                    String message = finalErrorMessage != null ? finalErrorMessage : "删除失败，请稍后重试。";
                    logPanel.error("✗ 删除任务组节点失败: " + message);
                    
                    javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    errorAlert.setTitle("删除失败");
                    errorAlert.setHeaderText("删除任务组节点失败");
                    errorAlert.setContentText(message);
                    errorAlert.showAndWait();
                }
            });
        }, "delete-group-container-" + nodeId).start();
    }

    /**
     * 递归复制嵌套任务组的子节点
     * 
     * @param nestedCompose 嵌套任务组的数据
     * @param parentJobId 父任务组ID（新创建的任务组节点ID）
     * @param baseX 基础X坐标（任务组节点的X坐标）
     * @param baseY 基础Y坐标（任务组节点的Y坐标）
     * @param idToNode 节点ID到ProcessNode的映射（输出参数）
     * @param newNodes 新创建的节点列表（输出参数）
     * @param processedJobIds 已处理的jobId集合（用于去重）
     */
    private void copyNestedTaskGroupNodes(
            JobComposeData nestedCompose,
            Long parentJobId,
            double baseX,
            double baseY,
            java.util.Map<String, ProcessNode> idToNode,
            java.util.List<ProcessNode> newNodes,
            java.util.Set<Long> processedJobIds) {
        
        if (nestedCompose == null || nestedCompose.getNodes() == null) {
            return;
        }
        
        // 找到嵌套任务组节点的位置（用于计算相对位置）
        double nestedGroupX = baseX;
        double nestedGroupY = baseY;
        boolean foundNestedGroupNode = false;
        
        if (nestedCompose.getJobNode() != null) {
            JobComposeData.NodeData jobNode = nestedCompose.getJobNode();
            if (jobNode.getX() != null && jobNode.getY() != null) {
                nestedGroupX = jobNode.getX();
                nestedGroupY = jobNode.getY();
                foundNestedGroupNode = true;
            }
        }
        
        // 遍历嵌套任务组的所有子节点
        for (JobComposeData.NodeData nd : nestedCompose.getNodes()) {
            Long originalJobId = nd.getJobId();
            if (originalJobId == null) {
                continue;
            }
            
            // 去重：同一次克隆中，原jobId只处理一次
            if (!processedJobIds.add(originalJobId)) {
                continue;
            }
            
            try {
                // 拉取原节点表单并克隆
                JobInfoForm originalForm = jobInfoService.getJobNodeFormData(originalJobId);
                if (originalForm == null) {
                    continue;
                }
                
                JobInfoForm copyForm = deepCopyJobInfoForm(originalForm);
                if (copyForm == null) {
                    continue;
                }
                
                // 设置归属与基础信息
                copyForm.setId(null);
                copyForm.setParentId(parentJobId);
                String label = nd.getJobName() != null ? nd.getJobName() : originalForm.getJobDesc();
                copyForm.setJobDesc(label);
                
                // 判断是否是任务组节点
                boolean isNestedGroupNode = false;
                if (nd.getType() != null && 
                    (nd.getType().equals("CustomGroup") || nd.getType().equalsIgnoreCase("custom-group"))) {
                    isNestedGroupNode = true;
                } else if (nd.getProperties() != null && nd.getProperties().containsKey("children")) {
                    isNestedGroupNode = true;
                }
                
                // 计算子节点相对于嵌套任务组节点的偏移量
                double nx = nd.getX() != null ? nd.getX() : 0;
                double ny = nd.getY() != null ? nd.getY() : 0;
                double offsetX = foundNestedGroupNode ? (nx - nestedGroupX) : 0;
                double offsetY = foundNestedGroupNode ? (ny - nestedGroupY) : 0;
                
                // 保持相对位置
                copyForm.setNodePositionX(baseX + offsetX);
                copyForm.setNodePositionY(baseY + offsetY);
                
                // 如果是嵌套的任务组节点，递归处理
                if (isNestedGroupNode) {
                    copyForm.setJobType(2);
                    copyForm.setGlueType("CUSTOM_GROUP");
                    copyForm.setExecutorHandler("runJobGroupXxlJob");
                    
                    if (copyForm.getExecutorRouteStrategy() == null || copyForm.getExecutorRouteStrategy().isEmpty()) {
                        copyForm.setExecutorRouteStrategy("FIRST");
                    }
                    copyForm.setGlueUpdatetime(null);
                    
                    // 先保存当前任务组节点
                    com.cc.job.xo.model.entity.JobNode savedNestedGroup = jobInfoService.saveJobNode(copyForm);
                    if (savedNestedGroup != null) {
                        // 递归获取并保存更深层的嵌套任务组节点
                        try {
                            JobComposeData deeperNestedCompose = jobPartService.getJobCompose(originalJobId);
                            if (deeperNestedCompose != null && deeperNestedCompose.getNodes() != null) {
                                // 递归处理更深层的子节点
                                copyNestedTaskGroupNodes(
                                    deeperNestedCompose,
                                    savedNestedGroup.getJobId(),
                                    copyForm.getNodePositionX(),
                                    copyForm.getNodePositionY(),
                                    idToNode,
                                    newNodes,
                                    processedJobIds
                                );
                            }
                        } catch (Exception e) {
                            logger.error("递归复制更深层嵌套任务组节点失败: jobId={}, error={}", originalJobId, e.getMessage(), e);
                        }
                        
                        // 添加到UI
                        final String nodeIdStr = String.valueOf(savedNestedGroup.getId());
                        final Long newJobId = savedNestedGroup.getJobId();
                        final String title = label;
                        final double px = copyForm.getNodePositionX();
                        final double py = copyForm.getNodePositionY();
                        Platform.runLater(() -> {
                            ProcessNode node = new ProcessNode(nodeIdStr, title, px, py);
                            node.setJobId(newJobId);
                            node.setType("CustomGroup");
                            canvas.addNode(node, true);
                            configureNodeCallbacks(node);
                            idToNode.put(nd.getId(), node);
                            newNodes.add(node);
                        });
                    }
                } else {
                    // 普通节点，直接保存
                    if (copyForm.getExecutorRouteStrategy() == null || copyForm.getExecutorRouteStrategy().isEmpty()) {
                        copyForm.setExecutorRouteStrategy("FIRST");
                    }
                    copyForm.setGlueUpdatetime(null);
                    
                    com.cc.job.xo.model.entity.JobNode saved = jobInfoService.saveJobNode(copyForm);
                    if (saved != null) {
                        final String nodeIdStr = String.valueOf(saved.getId());
                        final Long newJobId = saved.getJobId();
                        final String title = label;
                        final double px = copyForm.getNodePositionX();
                        final double py = copyForm.getNodePositionY();
                        Platform.runLater(() -> {
                            ProcessNode node = new ProcessNode(nodeIdStr, title, px, py);
                            node.setJobId(newJobId);
                            node.setType("Bean");
                            canvas.addNode(node, true);
                            configureNodeCallbacks(node);
                            idToNode.put(nd.getId(), node);
                            newNodes.add(node);
                        });
                    }
                }
            } catch (Exception e) {
                logger.error("复制嵌套任务组子节点失败: nodeId={}, jobId={}, error={}", 
                    nd.getId(), originalJobId, e.getMessage(), e);
            }
        }
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

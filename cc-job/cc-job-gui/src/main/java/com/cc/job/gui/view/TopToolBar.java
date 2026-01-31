package com.cc.job.gui.view;

import com.cc.job.gui.model.RunningJobGroup;
import com.cc.job.gui.util.IconUtil;
import com.cc.job.gui.util.SessionManager;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.model.entity.JobGroup;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 顶部工具栏组件
 */
public class TopToolBar extends VBox {
    
    private static final Logger logger = LoggerFactory.getLogger(TopToolBar.class);
    
    // 回调接口
    public interface ToolBarCallback {
        void onNew();
        void onNewPart();
        void onOpen();
        void onSave();
        void onUndo();
        void onRedo();
        void onZoomIn();
        void onZoomOut();
        void onZoomFit();
        void onNodeHistory(); // 节点历史
        void onRun();
        void onStop(Long jobId);
        void onClear();
        void onSettings();
        void onSelect(); // 框选功能
        void onLayoutHorizontal(); // 横向布局
        void onLayoutVertical(); // 纵向布局
        void onDistributeHorizontally(); // 水平等距分布
        void onDistributeVertically(); // 垂直等距分布
        void onAlignToCenter(); // 对齐到画布中心
        void onToggleSnapToGrid(); // 切换网格吸附
        void onDetectCycles(); // 检测循环依赖
        void onJobList();
        void onJobGroupList();
        void onJobLogList();
        void onDatasourceList();
        void onDataxSync();
        void onDataxGroupSync();
        void onExportCanvas(); // 导出画布

        /**
         * 任务菜单需要的任务列表（供“任务”下拉菜单展示）
         */
        default List<JobGroup> onRequestTaskList() {
            return Collections.emptyList();
        }

        /**
         * 点击任务菜单中的任务后触发
         * @param taskGroupId 任务组ID
         * @param taskGroupName 任务组名称
         */
        default void onTaskSelected(Long taskGroupId, String taskGroupName) {
        }
        
        // 文件菜单扩展
        default void onSaveAs() {} // 另存为
        default void onExit() {} // 退出
        default void onRecentFile(Long taskGroupId, String taskGroupName) {} // 最近打开的文件
        
        // 编辑菜单扩展
        default void onCut() {} // 剪切
        default void onCopy() {} // 复制
        default void onPaste() {} // 粘贴
        default void onDelete() {} // 删除
        default void onSelectAll() {} // 全选
        default void onBatchEdit() {} // 批量编辑
        default void onFindNode() {} // 查找节点
        default void onFindNext() {} // 查找下一个
        default void onFindPrevious() {} // 查找上一个
        default void onAutoLayout() {} // 自动布局（默认网格布局）
        default void onAutoLayout(com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm algorithm) {} // 自动布局（指定算法）
        
        // 选择菜单
        default void onInvertSelection() {} // 反选
        default void onSelectByType(String type) {} // 按类型选择
        default void onClearSelection() {} // 清除选择
        default void onSelectUpstream() {} // 选择上游节点
        default void onSelectDownstream() {} // 选择下游节点
        
        // 查看菜单
        default void onZoomActualSize() {} // 实际大小
        default void onToggleTreeView() {} // 显示/隐藏树形视图
        default void onToggleMiniMap() {} // 显示/隐藏小地图
        default void onToggleLogPanel() {} // 显示/隐藏日志面板
        default void onResetLayout() {} // 重置布局
        default void onToggleGrid() {} // 显示/隐藏网格
        default void onToggleRuler() {} // 显示/隐藏标尺
        default void onToggleNodeLabels() {} // 显示/隐藏节点标签
        default void onToggleEdgeLabels() {} // 显示/隐藏连线标签
        default void onSetTheme(String theme) {} // 设置主题
        default void onToggleFullScreen() {} // 全屏
        
        // 转到菜单
        default void onGoToNode() {} // 转到节点
        default void onGoToTaskGroup() {} // 转到任务组
        default void onGoToPartition() {} // 转到分区
        default void onPreviousNode() {} // 上一个节点
        default void onNextNode() {} // 下一个节点
        default void onLocateSelectedNode() {} // 定位到选中节点
        default void onLocateRunningNode() {} // 定位到运行中的节点
        
        // 运行菜单扩展
        default void onRerun() {} // 重新运行
        default void onRunSelectedNodes() {} // 运行选中的节点
        default void onRunToHere() {} // 运行到此处
        
        // 任务菜单扩展
        default void onNewJobGroup() {} // 新建任务组
        default void onEditJobGroup() {} // 编辑任务组
        default void onDeleteJobGroup() {} // 删除任务组
        default void onCopyJobGroup() {} // 复制任务组
        
        // 窗口菜单
        default void onMinimize() {} // 最小化
        default void onZoomWindow() {} // 缩放窗口
        default void onDetachTreeView() {} // 弹出树形视图
        default void onDetachMiniMap() {} // 弹出小地图
        default void onDetachLogPanel() {} // 弹出日志面板
        default void onRestoreAllPanels() {} // 恢复所有面板
        default void onSaveLayout() {} // 保存布局
        default void onRestoreDefaultLayout() {} // 恢复默认布局
        
        // 帮助菜单
        default void onUserManual() {} // 用户手册
        default void onShortcutsList() {} // 快捷键列表
        default void onApiDocumentation() {} // API文档
        default void onChangelog() {} // 更新日志
        default void onAbout() {} // 关于
        default void onCheckUpdate() {} // 检查更新
        default void onReportIssue() {} // 报告问题
        default void onFeedback() {} // 反馈建议
        default void onOnlineHelp() {} // 在线帮助
        
        /**
         * 获取最近打开的文件列表
         */
        default List<RecentFile> getRecentFiles() {
            return Collections.emptyList();
        }
    }
    
    /**
     * 最近打开的文件信息
     */
    public static class RecentFile {
        private Long taskGroupId;
        private String taskGroupName;
        private long lastAccessTime;
        
        public RecentFile(Long taskGroupId, String taskGroupName, long lastAccessTime) {
            this.taskGroupId = taskGroupId;
            this.taskGroupName = taskGroupName;
            this.lastAccessTime = lastAccessTime;
        }
        
        public Long getTaskGroupId() { return taskGroupId; }
        public String getTaskGroupName() { return taskGroupName; }
        public long getLastAccessTime() { return lastAccessTime; }
    }
    
    private ToolBarCallback callback;
    private Label zoomLabel;
    private double currentZoom = 1.0;
    
    // 运行按钮和下拉菜单
    private Button runButton;
    private Button stopButton;  // 独立的停止按钮
    private Button retryButton;
    private MenuButton runningTasksMenu;
    private HBox runGroup;

    private Button undoButton;
    private Button redoButton;
    private Button selectButton; // 框选按钮
    private Button snapToGridButton; // 网格吸附按钮
    
    // 当前任务组ID（用于判断是否正在运行）
    private Long currentTaskGroupId;
    
    // 运行中的任务组列表
    private Map<Long, RunningJobGroup> runningJobs = new java.util.HashMap<>();

    //private Menu taskMenu;
    private boolean loadingTaskMenu = false;
    
    // 最近打开的文件菜单引用
    private Menu recentFilesMenu;
    
    public TopToolBar() {
        initializeUI();
    }
    
    private void initializeUI() {
        setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: transparent transparent rgba(148,163,184,0.35) transparent; " +
            "-fx-border-width: 0 0 1 0;"
        );
        setPadding(new Insets(0));
        
        // 顶部菜单栏
        MenuBar menuBar = createMenuBar();
        // 工具栏
        HBox toolBar = createToolBar();
        
        getChildren().addAll(menuBar, toolBar);
    }
    
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setPadding(new Insets(0, 12, 0, 12));
        menuBar.setStyle("-fx-background-color: #F8FAFC;");

        // 文件菜单
        Menu fileMenu = createFileMenu();
        
        // 编辑菜单
        Menu editMenu = createEditMenu();
        
        // 选择菜单
        Menu selectMenu = createSelectMenu();
        
        // 查看菜单
        Menu viewMenu = createViewMenu();
        
        // 转到菜单
        Menu goMenu = createGoMenu();
        
        // 运行菜单
        Menu runMenu = createRunMenu();
        
        // 任务菜单
        Menu jobMenu = createTaskMenu();
        
        // 窗口菜单
        Menu windowMenu = createWindowMenu();
        
        // 帮助菜单
        Menu helpMenu = createHelpMenu();

        menuBar.getMenus().addAll(fileMenu, editMenu, selectMenu, viewMenu, goMenu, runMenu, jobMenu, windowMenu, helpMenu);
        return menuBar;
    }
    
    private Menu createFileMenu() {
        Menu fileMenu = new Menu("文件");
        
        // 新建子菜单
        Menu newItem = new Menu("新建");
        MenuItem newJobItem = new MenuItem("新建任务");
        newJobItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newJobItem.setOnAction(e -> safeCall(ToolBarCallback::onNew));
        MenuItem newJobPartItem = new MenuItem("新建分区");
        newJobPartItem.setAccelerator(new KeyCodeCombination(KeyCode.P,KeyCombination.CONTROL_DOWN));
        newJobPartItem.setOnAction(e -> safeCall(ToolBarCallback::onNewPart));
        newItem.getItems().addAll(newJobItem, newJobPartItem);
        
        // 打开
        MenuItem openItem = new MenuItem("打开");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(e -> safeCall(ToolBarCallback::onOpen));
        
        // 保存
        MenuItem saveItem = new MenuItem("保存");
        saveItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        saveItem.setOnAction(e -> safeCall(ToolBarCallback::onSave));
        
        // 另存为
        MenuItem saveAsItem = new MenuItem("另存为");
        saveAsItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S, 
            javafx.scene.input.KeyCombination.CONTROL_DOWN, javafx.scene.input.KeyCombination.SHIFT_DOWN));
        saveAsItem.setOnAction(e -> safeCall(ToolBarCallback::onSaveAs));
        
        // 最近打开的文件
        recentFilesMenu = new Menu("最近打开的文件");
        updateRecentFilesMenu(recentFilesMenu);
        
        fileMenu.getItems().addAll(newItem, openItem, saveItem, saveAsItem, new SeparatorMenuItem(), recentFilesMenu);
        
        // 导入子菜单
        Menu importMenu = new Menu("导入");
        MenuItem importPartitionItem = new MenuItem("导入分区文件");
        importPartitionItem.setOnAction(e -> safeCall(ToolBarCallback::onOpen));
        importMenu.getItems().add(importPartitionItem);
        
        // 导出子菜单
        Menu exportMenu = new Menu("导出");
        MenuItem exportTaskGroupItem = new MenuItem("导出当前任务组");
        exportTaskGroupItem.setOnAction(e -> safeCall(ToolBarCallback::onSaveAs));
        exportMenu.getItems().add(exportTaskGroupItem);
        
        fileMenu.getItems().addAll(new SeparatorMenuItem(), importMenu, exportMenu, new SeparatorMenuItem());
        
        // 退出
        MenuItem exitItem = new MenuItem("退出");
        exitItem.setOnAction(e -> safeCall(ToolBarCallback::onExit));
        fileMenu.getItems().add(exitItem);
        
        return fileMenu;
    }
    
    private void updateRecentFilesMenu(Menu recentFilesMenu) {
        recentFilesMenu.getItems().clear();
        if (callback != null) {
            List<RecentFile> recentFiles = callback.getRecentFiles();
            if (recentFiles != null && !recentFiles.isEmpty()) {
                for (RecentFile file : recentFiles) {
                    MenuItem item = new MenuItem(file.getTaskGroupName());
                    item.setOnAction(e -> callback.onRecentFile(file.getTaskGroupId(), file.getTaskGroupName()));
                    recentFilesMenu.getItems().add(item);
                }
            } else {
                MenuItem emptyItem = new MenuItem("(无)");
                emptyItem.setDisable(true);
                recentFilesMenu.getItems().add(emptyItem);
            }
        }
    }
    
    private Menu createEditMenu() {
        Menu editMenu = new Menu("编辑");
        
        // 撤销
        MenuItem undoItem = new MenuItem("撤销");
        undoItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Z, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        undoItem.setOnAction(e -> safeCall(ToolBarCallback::onUndo));
        
        // 重做
        MenuItem redoItem = new MenuItem("重做");
        redoItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Y, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        redoItem.setOnAction(e -> safeCall(ToolBarCallback::onRedo));
        
        editMenu.getItems().addAll(undoItem, redoItem, new SeparatorMenuItem());
        
        // 剪切
        MenuItem cutItem = new MenuItem("剪切");
        cutItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.X, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        cutItem.setOnAction(e -> safeCall(ToolBarCallback::onCut));
        
        // 复制
        MenuItem copyItem = new MenuItem("复制");
        copyItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.C, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        copyItem.setOnAction(e -> safeCall(ToolBarCallback::onCopy));
        
        // 粘贴
        MenuItem pasteItem = new MenuItem("粘贴");
        pasteItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.V, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        pasteItem.setOnAction(e -> safeCall(ToolBarCallback::onPaste));
        
        // 删除
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.DELETE));
        deleteItem.setOnAction(e -> safeCall(ToolBarCallback::onDelete));
        
        // 全选
        MenuItem selectAllItem = new MenuItem("全选");
        selectAllItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.A, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        selectAllItem.setOnAction(e -> safeCall(ToolBarCallback::onSelectAll));
        
        editMenu.getItems().addAll(cutItem, copyItem, pasteItem, deleteItem, selectAllItem, new SeparatorMenuItem());
        
        // 查找
        MenuItem findItem = new MenuItem("查找节点");
        findItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        findItem.setOnAction(e -> safeCall(ToolBarCallback::onFindNode));
        
        MenuItem findNextItem = new MenuItem("查找下一个");
        findNextItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F3));
        findNextItem.setOnAction(e -> safeCall(ToolBarCallback::onFindNext));
        
        MenuItem findPreviousItem = new MenuItem("查找上一个");
        findPreviousItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F3, javafx.scene.input.KeyCombination.SHIFT_DOWN));
        findPreviousItem.setOnAction(e -> safeCall(ToolBarCallback::onFindPrevious));
        
        editMenu.getItems().addAll(findItem, findNextItem, findPreviousItem, new SeparatorMenuItem());
        
        // 框选
        MenuItem selectItem = new MenuItem("框选");
        selectItem.setOnAction(e -> safeCall(ToolBarCallback::onSelect));
        
        // 布局
        MenuItem layoutHorizontalItem = new MenuItem("横向布局");
        layoutHorizontalItem.setOnAction(e -> safeCall(ToolBarCallback::onLayoutHorizontal));
        
        MenuItem layoutVerticalItem = new MenuItem("纵向布局");
        layoutVerticalItem.setOnAction(e -> safeCall(ToolBarCallback::onLayoutVertical));
        
        // 批量编辑
        MenuItem batchEditItem = new MenuItem("批量编辑");
        batchEditItem.setOnAction(e -> safeCall(ToolBarCallback::onBatchEdit));
        
        Menu autoLayoutMenu = new Menu("自动布局");
        MenuItem gridLayoutMenuItem = new MenuItem("网格布局");
        gridLayoutMenuItem.setOnAction(e -> safeCall(cb -> cb.onAutoLayout(com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.GRID)));
        MenuItem hierarchicalLayoutMenuItem = new MenuItem("层次化布局");
        hierarchicalLayoutMenuItem.setOnAction(e -> safeCall(cb -> cb.onAutoLayout(com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.HIERARCHICAL)));
        MenuItem forceDirectedLayoutMenuItem = new MenuItem("力导向布局");
        forceDirectedLayoutMenuItem.setOnAction(e -> safeCall(cb -> cb.onAutoLayout(com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.FORCE_DIRECTED)));
        MenuItem treeLayoutMenuItem = new MenuItem("树形布局");
        treeLayoutMenuItem.setOnAction(e -> safeCall(cb -> cb.onAutoLayout(com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.TREE)));
        autoLayoutMenu.getItems().addAll(gridLayoutMenuItem, hierarchicalLayoutMenuItem, forceDirectedLayoutMenuItem, treeLayoutMenuItem);
        
        editMenu.getItems().addAll(selectItem, layoutHorizontalItem, layoutVerticalItem, batchEditItem, autoLayoutMenu);
        
        return editMenu;
    }
    
    private Menu createSelectMenu() {
        Menu selectMenu = new Menu("选择");
        
        MenuItem selectAllItem = new MenuItem("全选节点");
        selectAllItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.A, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        selectAllItem.setOnAction(e -> safeCall(ToolBarCallback::onSelectAll));
        
        MenuItem invertItem = new MenuItem("反选");
        invertItem.setOnAction(e -> safeCall(ToolBarCallback::onInvertSelection));
        
        Menu selectByTypeMenu = new Menu("按类型选择");
        MenuItem selectTaskNodesItem = new MenuItem("选择所有任务节点");
        selectTaskNodesItem.setOnAction(e -> safeCall(cb -> cb.onSelectByType("TASK")));
        MenuItem selectConditionNodesItem = new MenuItem("选择所有条件节点");
        selectConditionNodesItem.setOnAction(e -> safeCall(cb -> cb.onSelectByType("CONDITION")));
        MenuItem selectEdgesItem = new MenuItem("选择所有连线");
        selectEdgesItem.setOnAction(e -> safeCall(cb -> cb.onSelectByType("EDGE")));
        selectByTypeMenu.getItems().addAll(
                selectTaskNodesItem,
                //selectConditionNodesItem,
                selectEdgesItem);
        
        MenuItem clearSelectionItem = new MenuItem("清除选择");
        clearSelectionItem.setOnAction(e -> safeCall(ToolBarCallback::onClearSelection));
        
        selectMenu.getItems().addAll(selectAllItem, invertItem, selectByTypeMenu, clearSelectionItem, new SeparatorMenuItem());
        
        MenuItem selectUpstreamItem = new MenuItem("选择上游节点");
        selectUpstreamItem.setOnAction(e -> safeCall(ToolBarCallback::onSelectUpstream));
        
        MenuItem selectDownstreamItem = new MenuItem("选择下游节点");
        selectDownstreamItem.setOnAction(e -> safeCall(ToolBarCallback::onSelectDownstream));
        
        selectMenu.getItems().addAll(selectUpstreamItem, selectDownstreamItem);
        
        return selectMenu;
    }
    
    private Menu createViewMenu() {
        Menu viewMenu = new Menu("查看");
        
        // 缩放
        MenuItem zoomInItem = new MenuItem("放大");
        zoomInItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.EQUALS, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        zoomInItem.setOnAction(e -> safeCall(ToolBarCallback::onZoomIn));
        
        MenuItem zoomOutItem = new MenuItem("缩小");
        zoomOutItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.MINUS, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        zoomOutItem.setOnAction(e -> safeCall(ToolBarCallback::onZoomOut));
        
        MenuItem zoomFitItem = new MenuItem("适应窗口");
        zoomFitItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.DIGIT0, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        zoomFitItem.setOnAction(e -> safeCall(ToolBarCallback::onZoomFit));
        
        MenuItem zoomActualItem = new MenuItem("实际大小");
        zoomActualItem.setOnAction(e -> safeCall(ToolBarCallback::onZoomActualSize));
        
        viewMenu.getItems().addAll(zoomInItem, zoomOutItem, zoomFitItem, zoomActualItem, new SeparatorMenuItem());
        
        // 面板管理
        MenuItem toggleTreeViewItem = new MenuItem("显示/隐藏树形视图");
        toggleTreeViewItem.setOnAction(e -> safeCall(ToolBarCallback::onToggleTreeView));
        
        MenuItem toggleMiniMapItem = new MenuItem("显示/隐藏小地图");
        toggleMiniMapItem.setOnAction(e -> safeCall(ToolBarCallback::onToggleMiniMap));
        
        MenuItem toggleLogPanelItem = new MenuItem("显示/隐藏日志面板");
        toggleLogPanelItem.setOnAction(e -> safeCall(ToolBarCallback::onToggleLogPanel));
        
        MenuItem resetLayoutItem = new MenuItem("重置布局");
        resetLayoutItem.setOnAction(e -> safeCall(ToolBarCallback::onResetLayout));
        
        viewMenu.getItems().addAll(toggleTreeViewItem, toggleMiniMapItem, toggleLogPanelItem, new SeparatorMenuItem(), resetLayoutItem, new SeparatorMenuItem());
        
        // 视图选项
        MenuItem toggleGridItem = new MenuItem("显示网格");
        toggleGridItem.setOnAction(e -> safeCall(ToolBarCallback::onToggleGrid));
        
        MenuItem toggleNodeLabelsItem = new MenuItem("显示节点标签");
        toggleNodeLabelsItem.setOnAction(e -> safeCall(ToolBarCallback::onToggleNodeLabels));
        
        MenuItem toggleEdgeLabelsItem = new MenuItem("显示连线标签");
        toggleEdgeLabelsItem.setOnAction(e -> safeCall(ToolBarCallback::onToggleEdgeLabels));
        
        viewMenu.getItems().addAll(toggleGridItem, toggleNodeLabelsItem, toggleEdgeLabelsItem, new SeparatorMenuItem());
        
        // 主题
        Menu themeMenu = new Menu("主题");
        MenuItem lightThemeItem = new MenuItem("浅色主题");
        lightThemeItem.setOnAction(e -> safeCall(cb -> cb.onSetTheme("light")));
        MenuItem darkThemeItem = new MenuItem("深色主题");
        darkThemeItem.setOnAction(e -> safeCall(cb -> cb.onSetTheme("dark")));
        MenuItem autoThemeItem = new MenuItem("自动");
        autoThemeItem.setOnAction(e -> safeCall(cb -> cb.onSetTheme("auto")));
        themeMenu.getItems().addAll(lightThemeItem, darkThemeItem, autoThemeItem);
        
        MenuItem fullScreenItem = new MenuItem("全屏");
        fullScreenItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F11));
        fullScreenItem.setOnAction(e -> safeCall(ToolBarCallback::onToggleFullScreen));
        
        viewMenu.getItems().addAll(themeMenu, new SeparatorMenuItem(), fullScreenItem);
        
        return viewMenu;
    }
    
    private Menu createGoMenu() {
        Menu goMenu = new Menu("转到");
        
        MenuItem goToNodeItem = new MenuItem("转到节点");
        goToNodeItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.G, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        goToNodeItem.setOnAction(e -> safeCall(ToolBarCallback::onGoToNode));
        
        MenuItem goToTaskGroupItem = new MenuItem("转到任务组");
        goToTaskGroupItem.setOnAction(e -> safeCall(ToolBarCallback::onGoToTaskGroup));
        
        MenuItem goToPartitionItem = new MenuItem("转到分区");
        goToPartitionItem.setOnAction(e -> safeCall(ToolBarCallback::onGoToPartition));
        
        goMenu.getItems().addAll(goToNodeItem, goToTaskGroupItem, goToPartitionItem, new SeparatorMenuItem());
        
        MenuItem previousNodeItem = new MenuItem("上一个节点");
        previousNodeItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.UP, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        previousNodeItem.setOnAction(e -> safeCall(ToolBarCallback::onPreviousNode));
        
        MenuItem nextNodeItem = new MenuItem("下一个节点");
        nextNodeItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.DOWN, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        nextNodeItem.setOnAction(e -> safeCall(ToolBarCallback::onNextNode));
        
        goMenu.getItems().addAll(previousNodeItem, nextNodeItem, new SeparatorMenuItem());
        
        MenuItem locateSelectedItem = new MenuItem("定位到选中节点");
        locateSelectedItem.setOnAction(e -> safeCall(ToolBarCallback::onLocateSelectedNode));
        
        MenuItem locateRunningItem = new MenuItem("定位到运行中的节点");
        locateRunningItem.setOnAction(e -> safeCall(ToolBarCallback::onLocateRunningNode));
        
        goMenu.getItems().addAll(locateSelectedItem, locateRunningItem);
        
        return goMenu;
    }
    
    private Menu createRunMenu() {
        Menu runMenu = new Menu("运行");
        
        MenuItem runItem = new MenuItem("运行任务组");
        runItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F5));
        runItem.setOnAction(e -> safeCall(ToolBarCallback::onRun));
        
        MenuItem stopItem = new MenuItem("停止任务");
        stopItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F5, javafx.scene.input.KeyCombination.SHIFT_DOWN));
        stopItem.setOnAction(e -> {
            if (currentTaskGroupId != null) {
                safeCall(cb -> cb.onStop(currentTaskGroupId));
            }
        });
        
        MenuItem rerunItem = new MenuItem("重新运行");
        rerunItem.setOnAction(e -> safeCall(ToolBarCallback::onRerun));
        
        runMenu.getItems().addAll(runItem, stopItem, rerunItem, new SeparatorMenuItem());
        
        MenuItem runSelectedItem = new MenuItem("运行选中的节点");
        runSelectedItem.setOnAction(e -> safeCall(ToolBarCallback::onRunSelectedNodes));
        
        MenuItem runToHereItem = new MenuItem("运行到此处");
        runToHereItem.setOnAction(e -> safeCall(ToolBarCallback::onRunToHere));
        
        runMenu.getItems().addAll(runSelectedItem, runToHereItem, new SeparatorMenuItem());
        
        MenuItem historyItem = new MenuItem("查看执行历史");
        historyItem.setOnAction(e -> safeCall(ToolBarCallback::onNodeHistory));
        
        runMenu.getItems().add(historyItem);
        
        return runMenu;
    }
    
    private Menu createTaskMenu() {
        Menu jobMenu = new Menu("任务");
        
        MenuItem jobMenuList = new MenuItem("任务列表");
        jobMenuList.setOnAction(e -> safeCall(ToolBarCallback::onJobList));
        
        MenuItem jobGroupMenuList = new MenuItem("任务执行器");
        jobGroupMenuList.setOnAction(e -> safeCall(ToolBarCallback::onJobGroupList));
        
        MenuItem jobLogMenuList = new MenuItem("任务日志");
        jobLogMenuList.setOnAction(e -> safeCall(ToolBarCallback::onJobLogList));
        
        jobMenu.getItems().addAll(jobMenuList, jobGroupMenuList, jobLogMenuList, new SeparatorMenuItem());
        
        MenuItem datasourceMenuList = new MenuItem("数据源管理");
        datasourceMenuList.setOnAction(e -> safeCall(ToolBarCallback::onDatasourceList));
        
        MenuItem dataxSyncMenuItem = new MenuItem("数据源同步");
        dataxSyncMenuItem.setOnAction(e -> safeCall(ToolBarCallback::onDataxSync));
        
        MenuItem dataxGroupSyncMenuItem = new MenuItem("多数据源同步");
        dataxGroupSyncMenuItem.setOnAction(e -> safeCall(ToolBarCallback::onDataxGroupSync));
        
        jobMenu.getItems().addAll(datasourceMenuList, new SeparatorMenuItem(), dataxSyncMenuItem, dataxGroupSyncMenuItem, new SeparatorMenuItem());
        
        // 任务组操作
        MenuItem newJobGroupItem = new MenuItem("新建任务组");
        newJobGroupItem.setOnAction(e -> safeCall(ToolBarCallback::onNewJobGroup));
        
        MenuItem editJobGroupItem = new MenuItem("编辑任务组");
        editJobGroupItem.setOnAction(e -> safeCall(ToolBarCallback::onEditJobGroup));
        
        MenuItem deleteJobGroupItem = new MenuItem("删除任务组");
        deleteJobGroupItem.setOnAction(e -> safeCall(ToolBarCallback::onDeleteJobGroup));
        
        MenuItem copyJobGroupItem = new MenuItem("复制任务组");
        copyJobGroupItem.setOnAction(e -> safeCall(ToolBarCallback::onCopyJobGroup));
        
        jobMenu.getItems().addAll(newJobGroupItem, editJobGroupItem, deleteJobGroupItem, copyJobGroupItem);
        
        return jobMenu;
    }
    
    private Menu createWindowMenu() {
        Menu windowMenu = new Menu("窗口");
        
        MenuItem minimizeItem = new MenuItem("最小化");
        minimizeItem.setOnAction(e -> safeCall(ToolBarCallback::onMinimize));
        
        MenuItem zoomWindowItem = new MenuItem("缩放");
        zoomWindowItem.setOnAction(e -> safeCall(ToolBarCallback::onZoomWindow));
        
        windowMenu.getItems().addAll(minimizeItem, zoomWindowItem, new SeparatorMenuItem());
        
        // 面板窗口
        MenuItem detachTreeViewItem = new MenuItem("弹出树形视图");
        detachTreeViewItem.setOnAction(e -> safeCall(ToolBarCallback::onDetachTreeView));
        
        MenuItem detachMiniMapItem = new MenuItem("弹出小地图");
        detachMiniMapItem.setOnAction(e -> safeCall(ToolBarCallback::onDetachMiniMap));
        
        MenuItem detachLogPanelItem = new MenuItem("弹出日志面板");
        detachLogPanelItem.setOnAction(e -> safeCall(ToolBarCallback::onDetachLogPanel));
        
        MenuItem restoreAllItem = new MenuItem("恢复所有面板");
        restoreAllItem.setOnAction(e -> safeCall(ToolBarCallback::onRestoreAllPanels));
        
        windowMenu.getItems().addAll(detachTreeViewItem, detachMiniMapItem, detachLogPanelItem, new SeparatorMenuItem(), restoreAllItem, new SeparatorMenuItem());
        
        MenuItem saveLayoutItem = new MenuItem("保存当前布局");
        saveLayoutItem.setOnAction(e -> safeCall(ToolBarCallback::onSaveLayout));
        
        MenuItem restoreLayoutItem = new MenuItem("恢复默认布局");
        restoreLayoutItem.setOnAction(e -> safeCall(ToolBarCallback::onRestoreDefaultLayout));
        
        windowMenu.getItems().addAll(saveLayoutItem, restoreLayoutItem);
        
        return windowMenu;
    }
    
    private Menu createHelpMenu() {
        Menu helpMenu = new Menu("帮助");
        
        MenuItem userManualItem = new MenuItem("用户手册");
        userManualItem.setOnAction(e -> safeCall(ToolBarCallback::onUserManual));
        
        MenuItem shortcutsItem = new MenuItem("快捷键列表");
        shortcutsItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.SLASH, 
            javafx.scene.input.KeyCombination.CONTROL_DOWN, javafx.scene.input.KeyCombination.SHIFT_DOWN));
        shortcutsItem.setOnAction(e -> safeCall(ToolBarCallback::onShortcutsList));
        
        MenuItem apiDocItem = new MenuItem("API文档");
        apiDocItem.setOnAction(e -> safeCall(ToolBarCallback::onApiDocumentation));
        
        MenuItem changelogItem = new MenuItem("更新日志");
        changelogItem.setOnAction(e -> safeCall(ToolBarCallback::onChangelog));
        
        helpMenu.getItems().addAll(userManualItem, shortcutsItem, apiDocItem, changelogItem, new SeparatorMenuItem());
        
        MenuItem settingsItem = new MenuItem("系统设置");
        settingsItem.setOnAction(e -> safeCall(ToolBarCallback::onSettings));
        
        MenuItem aboutItem = new MenuItem("关于 CcETL");
        aboutItem.setOnAction(e -> safeCall(ToolBarCallback::onAbout));
        
        helpMenu.getItems().addAll(settingsItem, aboutItem, new SeparatorMenuItem());
        
        MenuItem checkUpdateItem = new MenuItem("检查更新");
        checkUpdateItem.setOnAction(e -> safeCall(ToolBarCallback::onCheckUpdate));
        
        MenuItem reportIssueItem = new MenuItem("报告问题");
        reportIssueItem.setOnAction(e -> safeCall(ToolBarCallback::onReportIssue));
        
        MenuItem feedbackItem = new MenuItem("反馈建议");
        feedbackItem.setOnAction(e -> safeCall(ToolBarCallback::onFeedback));
        
        MenuItem onlineHelpItem = new MenuItem("在线帮助");
        onlineHelpItem.setOnAction(e -> safeCall(ToolBarCallback::onOnlineHelp));
        
        helpMenu.getItems().addAll(checkUpdateItem, reportIssueItem, feedbackItem, new SeparatorMenuItem(), onlineHelpItem);
        
        return helpMenu;
    }
    
    private HBox createToolBar() {
        HBox toolBar = new HBox(8);
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setPadding(new Insets(8, 12, 8, 12));
        
        // 文件操作组
        HBox fileGroup = createToolGroup(
            createNewMenuButton(),
            createIconButton(IconUtil.folderIcon(), "打开", "打开已有流程图", () -> safeCall(ToolBarCallback::onOpen)),
            createIconButton(IconUtil.saveIcon(), "保存", "保存当前流程图", () -> safeCall(ToolBarCallback::onSave))
        );
        
        Region sep1 = createSeparator();
        
        // 编辑操作组
        undoButton = createIconButton(IconUtil.undoIcon(), "撤销", "撤销上一步操作", () -> safeCall(ToolBarCallback::onUndo));
        redoButton = createIconButton(IconUtil.redoIcon(), "重做", "重做上一步操作", () -> safeCall(ToolBarCallback::onRedo));
        selectButton = createIconButton(IconUtil.selectIcon(), "框选", "框选节点和边", () -> safeCall(ToolBarCallback::onSelect));
        snapToGridButton = createIconButton(IconUtil.gridIcon(), "网格吸附", "切换网格吸附功能", () -> safeCall(ToolBarCallback::onToggleSnapToGrid));
        
        // 布局下拉菜单
        javafx.scene.control.MenuButton layoutMenuButton = new javafx.scene.control.MenuButton("布局");
        layoutMenuButton.setGraphic(IconUtil.layoutHorizontalIcon());
        layoutMenuButton.setGraphicTextGap(6);
        
        // 横向布局
        javafx.scene.control.MenuItem layoutHorizontalItem = new javafx.scene.control.MenuItem("横向布局");
        layoutHorizontalItem.setGraphic(IconUtil.layoutHorizontalIcon());
        layoutHorizontalItem.setOnAction(e -> safeCall(ToolBarCallback::onLayoutHorizontal));
        
        // 纵向布局
        javafx.scene.control.MenuItem layoutVerticalItem = new javafx.scene.control.MenuItem("纵向布局");
        layoutVerticalItem.setGraphic(IconUtil.layoutVerticalIcon());
        layoutVerticalItem.setOnAction(e -> safeCall(ToolBarCallback::onLayoutVertical));
        
        // 水平等距
        javafx.scene.control.MenuItem distributeHorizontalItem = new javafx.scene.control.MenuItem("水平等距");
        distributeHorizontalItem.setGraphic(IconUtil.layoutHorizontalIcon());
        distributeHorizontalItem.setOnAction(e -> safeCall(ToolBarCallback::onDistributeHorizontally));
        
        // 垂直等距
        javafx.scene.control.MenuItem distributeVerticalItem = new javafx.scene.control.MenuItem("垂直等距");
        distributeVerticalItem.setGraphic(IconUtil.layoutVerticalIcon());
        distributeVerticalItem.setOnAction(e -> safeCall(ToolBarCallback::onDistributeVertically));
        
        // 居中对齐
        javafx.scene.control.MenuItem alignToCenterItem = new javafx.scene.control.MenuItem("居中对齐");
        alignToCenterItem.setGraphic(IconUtil.expandIcon());
        alignToCenterItem.setOnAction(e -> safeCall(ToolBarCallback::onAlignToCenter));
        
        layoutMenuButton.getItems().addAll(
            layoutHorizontalItem, 
            layoutVerticalItem, 
            new SeparatorMenuItem(),
            distributeHorizontalItem, 
            distributeVerticalItem,
            new SeparatorMenuItem(),
            alignToCenterItem
        );
        
        javafx.scene.control.Tooltip layoutTip = new javafx.scene.control.Tooltip("节点布局功能");
        layoutTip.setStyle("-fx-font-size: 12px;");
        layoutMenuButton.setTooltip(layoutTip);
        
        // 布局算法选择下拉菜单
        javafx.scene.control.MenuButton autoLayoutMenuButton = new javafx.scene.control.MenuButton("自动布局");
        autoLayoutMenuButton.setGraphicTextGap(6);
        // MenuButton不是Button类型，不能使用applyIconButtonHover
        
        javafx.scene.control.MenuItem gridLayoutItem = new javafx.scene.control.MenuItem("网格布局");
        gridLayoutItem.setOnAction(e -> safeCall(cb -> cb.onAutoLayout(com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.GRID)));
        
        javafx.scene.control.MenuItem hierarchicalLayoutItem = new javafx.scene.control.MenuItem("层次化布局");
        hierarchicalLayoutItem.setOnAction(e -> safeCall(cb -> cb.onAutoLayout(com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.HIERARCHICAL)));
        
        javafx.scene.control.MenuItem forceDirectedLayoutItem = new javafx.scene.control.MenuItem("力导向布局");
        forceDirectedLayoutItem.setOnAction(e -> safeCall(cb -> cb.onAutoLayout(com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.FORCE_DIRECTED)));
        
        javafx.scene.control.MenuItem treeLayoutItem = new javafx.scene.control.MenuItem("树形布局");
        treeLayoutItem.setOnAction(e -> safeCall(cb -> cb.onAutoLayout(com.cc.job.gui.manager.CanvasLayoutManager.LayoutAlgorithm.TREE)));
        
        autoLayoutMenuButton.getItems().addAll(gridLayoutItem, hierarchicalLayoutItem, forceDirectedLayoutItem, treeLayoutItem);
        
        javafx.scene.control.Tooltip autoLayoutTip = new javafx.scene.control.Tooltip("选择自动布局算法");
        autoLayoutTip.setStyle("-fx-font-size: 12px;");
        autoLayoutMenuButton.setTooltip(autoLayoutTip);
        
        // 检测循环依赖按钮
        Button detectCyclesButton = createIconButton(IconUtil.warnIcon(), "检测循环", "检测画布中的循环依赖", () -> safeCall(ToolBarCallback::onDetectCycles));
        
        undoButton.setDisable(true);
        redoButton.setDisable(true);
        HBox editGroup = createToolGroup(undoButton, redoButton, selectButton, layoutMenuButton, snapToGridButton, autoLayoutMenuButton, detectCyclesButton);
        
        Region sep2 = createSeparator();
        
        // 视图操作组
        zoomLabel = new Label("100%");
        zoomLabel.setStyle(StyleUtil.body() + "-fx-font-weight: 700; -fx-padding: 0 8 0 8;");
        
        HBox viewGroup = createToolGroup(
            createIconButton(IconUtil.zoomInIcon(), "放大", "放大画布", () -> safeCall(ToolBarCallback::onZoomIn)),
            createIconButton(IconUtil.zoomOutIcon(), "缩小", "缩小画布", () -> safeCall(ToolBarCallback::onZoomOut)),
            zoomLabel,
            createIconButton(IconUtil.expandIcon(), "适应", "适应窗口大小", () -> safeCall(ToolBarCallback::onZoomFit)),
            createIconButton(IconUtil.historyIcon(), "节点历史", "查看节点历史执行记录", () -> safeCall(ToolBarCallback::onNodeHistory)),
            createIconButton(IconUtil.exportIcon(), "画布", "导出画布为图片", () -> safeCall(ToolBarCallback::onExportCanvas))
        );
        
        // 右侧空白区域
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 用户信息区域
        HBox userInfoArea = createUserInfoArea();
        
        Region sep3 = createSeparator();
        
        // 运行操作组
        runGroup = createToolGroup();
        createRunButton();
        createStopButton();  // 创建停止按钮
        createRunningTasksMenu();
        updateRunGroupButtons();
        
        toolBar.getChildren().addAll(
            fileGroup, sep1,
            editGroup, sep2,
            viewGroup,
            spacer,
            userInfoArea,
            sep3, runGroup
        );
        
        return toolBar;
    }
    
    /**
     * 创建用户信息区域
     */
    private HBox createUserInfoArea() {
        HBox userInfo = new HBox(10);
        userInfo.setAlignment(Pos.CENTER_RIGHT);
        userInfo.setPadding(new Insets(0, 12, 0, 12));
        
        // 从SessionManager获取用户信息
        SessionManager session = SessionManager.getInstance();
        String username = session.getUsername();
        String userId = session.getUserId();
        
        // 如果未登录，返回空容器
        if (username == null || username.isEmpty()) {
            return userInfo;
        }
        
        // 创建用户头像（使用首字母）
        StackPane avatar = createUserAvatar(username);
        
        // 创建用户信息文本区域
        VBox textArea = new VBox(2);
        textArea.setAlignment(Pos.CENTER_RIGHT);
        
        // 用户名
        Label nameLabel = new Label(username);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        nameLabel.setTextFill(Color.web("#1F2937"));
        
        // 用户ID
        Label idLabel = new Label("ID: " + (userId != null ? userId : "N/A"));
        idLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        idLabel.setTextFill(Color.web("#6B7280"));
        
        textArea.getChildren().addAll(nameLabel, idLabel);
        
        // 组合头像和文本
        HBox userCard = new HBox(8);
        userCard.setAlignment(Pos.CENTER);
        userCard.setPadding(new Insets(4, 12, 4, 12));
        userCard.setStyle(
            "-fx-cursor: hand;"
        );
        userCard.getChildren().addAll(avatar, textArea);
        
        // 添加悬停效果
        userCard.setOnMouseEntered(e -> {
            userCard.setStyle(
                "-fx-background-color: #F3F4F6; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 4, 0, 0, 2);"
            );
        });
        
        userCard.setOnMouseExited(e -> {
            userCard.setStyle(
                "-fx-background-color: #F9FAFB; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand;"
            );
        });
        
        // 添加点击事件（可选：显示用户菜单）
        userCard.setOnMouseClicked(e -> {
            showUserMenu(userCard);
        });
        
        userInfo.getChildren().add(userCard);
        
        return userInfo;
    }
    
    /**
     * 创建用户头像（使用首字母圆形图标）
     */
    private StackPane createUserAvatar(String username) {
        StackPane avatar = new StackPane();
        avatar.setPrefSize(32, 32);
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        
        // 圆形背景
        Circle circle = new Circle(16);
        circle.setFill(Color.web("#6366F1"));
        circle.setStroke(Color.web("#4F46E5"));
        circle.setStrokeWidth(2);
        
        // 首字母
        String initial = username.substring(0, 1).toUpperCase();
        Label initialLabel = new Label(initial);
        initialLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        initialLabel.setTextFill(Color.WHITE);
        
        avatar.getChildren().addAll(circle, initialLabel);
        
        return avatar;
    }
    
    /**
     * 显示用户菜单
     */
    private void showUserMenu(Node node) {
        ContextMenu userMenu = new ContextMenu();
        
        // 用户信息菜单项（不可点击）
        SessionManager session = SessionManager.getInstance();
        MenuItem infoItem = new MenuItem("用户: " + session.getUsername());
        infoItem.setStyle("-fx-font-weight: bold; -fx-text-fill: #1F2937;");
        infoItem.setDisable(true);
        
        MenuItem idItem = new MenuItem("ID: " + session.getUserId());
        idItem.setStyle("-fx-text-fill: #6B7280;");
        idItem.setDisable(true);
        
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
            // 退出登录菜单项
            MenuItem logoutItem = new MenuItem("退出登录");
            logoutItem.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
            logoutItem.setOnAction(e -> {
                
                // 确认对话框
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("确认退出");
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText("确定要退出登录吗？");
                
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // 清除会话
                        SessionManager.getInstance().logout();
                        
                        // 关闭当前窗口
                        Stage stage = (Stage) getScene().getWindow();
                        stage.close();
                        
                        // 重新启动应用（显示登录界面）
                        Platform.runLater(() -> {
                            try {
                                new com.cc.job.gui.CcJobGuiApplication().start(new Stage());
                            } catch (Exception ex) {
                                logger.error("重新启动失败: {}", ex.getMessage(), ex);
                            }
                        });
                    }
                });
            });
        
        userMenu.getItems().addAll(infoItem, idItem, separator, logoutItem);
        
        // 显示菜单
        userMenu.show(node, javafx.geometry.Side.BOTTOM, 0, 0);
    }
    
    /**
     * 创建分隔线
     */
    private Region createSeparator() {
        Region separator = new Region();
        separator.setPrefWidth(1);
        separator.setMinWidth(1);
        separator.setMaxWidth(1);
        separator.setPrefHeight(28);
        separator.setMinHeight(20);
        separator.setStyle("-fx-background-color: #E2E8F0;");
        return separator;
    }

    
    private HBox createToolGroup(Node... buttons) {
        HBox group = new HBox(4);
        group.setAlignment(Pos.CENTER_LEFT);
        group.getChildren().addAll(buttons);
        return group;
    }
    
    /**
     * 创建新建下拉菜单按钮
     */
    private MenuButton createNewMenuButton() {
        MenuButton menuButton = new MenuButton("新建", IconUtil.plusIcon());
        menuButton.setGraphicTextGap(6);
        
        // 应用图标按钮样式和悬停效果
        String normalStyle = StyleUtil.iconButton();
        String hoverStyle = normalStyle.replace("transparent", "#F3F4F6");
        menuButton.setStyle(normalStyle);
        menuButton.setOnMouseEntered(e -> menuButton.setStyle(hoverStyle));
        menuButton.setOnMouseExited(e -> menuButton.setStyle(normalStyle));
        
        Tooltip tip = new Tooltip("创建新的任务或分区");
        tip.setStyle("-fx-font-size: 12px;");
        menuButton.setTooltip(tip);
        
        // 创建菜单项
        MenuItem newJobItem = new MenuItem("新建任务");
        newJobItem.setOnAction(e -> safeCall(ToolBarCallback::onNew));
        
        MenuItem newPartItem = new MenuItem("新建分区");
        newPartItem.setOnAction(e -> safeCall(ToolBarCallback::onNewPart));
        
        menuButton.getItems().addAll(newJobItem, newPartItem);
        
        return menuButton;
    }
    
    /**
     * 创建带图标的工具按钮
     */
    private Button createIconButton(Node icon, String text, String tooltip, Runnable action) {
        Button btn = new Button(text, icon);
        btn.setGraphicTextGap(6);
        StyleUtil.applyIconButtonHover(btn);
        
        if (tooltip != null) {
            Tooltip tip = new Tooltip(tooltip);
            tip.setStyle("-fx-font-size: 12px;");
            btn.setTooltip(tip);
        }
        
        btn.setOnAction(e -> action.run());
        
        return btn;
    }
    
    private Button createActionButton(String text, String color, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        
        btn.setOnMouseEntered(e -> {
            String darkerColor = color.equals("#10B981") ? "#059669" : color;
            btn.setStyle(
                "-fx-background-color: " + darkerColor + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 13; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 6 16 6 16; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-cursor: hand;"
            );
        });
        
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        ));
        
        btn.setOnAction(e -> action.run());
        
        return btn;
    }
    
    private void safeCall(Consumer<ToolBarCallback> method) {
        if (callback != null) {
            method.accept(callback);
        }
    }
    
    public void setCallback(ToolBarCallback callback) {
        this.callback = callback;
    }
    
    public void updateZoomLevel(double zoom) {
        this.currentZoom = zoom;
        zoomLabel.setText(String.format("%.0f%%", zoom * 100));
    }
    
    public double getCurrentZoom() {
        return currentZoom;
    }

    public void updateUndoRedoState(boolean canUndo, boolean canRedo) {
        if (undoButton != null) {
            undoButton.setDisable(!canUndo);
        }
        if (redoButton != null) {
            redoButton.setDisable(!canRedo);
        }
    }
    
    /**
     * 更新框选按钮的状态
     * @param isActive 是否处于框选模式
     */
    public void updateSelectionButtonState(boolean isActive) {
        Platform.runLater(() -> {
            if (selectButton != null) {
                if (isActive) {
                    // 框选模式激活：显示高亮效果（蓝色背景）
                    selectButton.setStyle(
                        "-fx-background-color: #2563EB; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 6 12 6 12; " +
                        "-fx-border-radius: 4; " +
                        "-fx-background-radius: 4; " +
                        "-fx-cursor: hand;"
                    );
                    selectButton.setTooltip(new Tooltip("框选模式已启用，点击可关闭"));
                    
                    // 添加悬停效果
                    selectButton.setOnMouseEntered(e -> {
                        selectButton.setStyle(
                            "-fx-background-color: #1D4ED8; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 12; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12 6 12; " +
                            "-fx-border-radius: 4; " +
                            "-fx-background-radius: 4; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.3), 4, 0, 0, 2);"
                        );
                    });
                    selectButton.setOnMouseExited(e -> {
                        selectButton.setStyle(
                            "-fx-background-color: #2563EB; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 12; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12 6 12; " +
                            "-fx-border-radius: 4; " +
                            "-fx-background-radius: 4; " +
                            "-fx-cursor: hand;"
                        );
                    });
                } else {
                    // 框选模式未激活：恢复默认样式
                    StyleUtil.applyIconButtonHover(selectButton);
                    selectButton.setTooltip(new Tooltip("框选节点和边"));
                }
            }
        });
    }
    
    /**
     * 更新网格吸附按钮的状态
     * @param isActive 是否启用网格吸附
     */
    public void updateSnapToGridButtonState(boolean isActive) {
        Platform.runLater(() -> {
            if (snapToGridButton != null) {
                if (isActive) {
                    // 网格吸附启用：显示高亮效果（蓝色背景）
                    snapToGridButton.setStyle(
                        "-fx-background-color: #2563EB; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 6 12 6 12; " +
                        "-fx-border-radius: 4; " +
                        "-fx-background-radius: 4; " +
                        "-fx-cursor: hand;"
                    );
                    snapToGridButton.setTooltip(new Tooltip("网格吸附已启用，点击可关闭"));
                    
                    // 添加悬停效果
                    snapToGridButton.setOnMouseEntered(e -> {
                        snapToGridButton.setStyle(
                            "-fx-background-color: #1D4ED8; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 12; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12 6 12; " +
                            "-fx-border-radius: 4; " +
                            "-fx-background-radius: 4; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.3), 4, 0, 0, 2);"
                        );
                    });
                    snapToGridButton.setOnMouseExited(e -> {
                        snapToGridButton.setStyle(
                            "-fx-background-color: #2563EB; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 12; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12 6 12; " +
                            "-fx-border-radius: 4; " +
                            "-fx-background-radius: 4; " +
                            "-fx-cursor: hand;"
                        );
                    });
                } else {
                    // 网格吸附未启用：恢复默认样式
                    StyleUtil.applyIconButtonHover(snapToGridButton);
                    snapToGridButton.setTooltip(new Tooltip("切换网格吸附功能"));
                }
            }
        });
    }
    
    /**
     * 创建开始按钮
     */
    private void createRunButton() {
        runButton = new Button("开始", IconUtil.playIcon());
        runButton.setGraphicTextGap(8);
        StyleUtil.applySuccessButtonHover(runButton);
        
        // 点击开始按钮时，运行当前任务组
        runButton.setOnAction(e -> {
            if (callback != null) {
                callback.onRun();
            }
        });
    }
    
    /**
     * 创建停止按钮（独立的停止按钮，只在满足条件时显示）
     */
    private void createStopButton() {
        stopButton = new Button("停止", IconUtil.stopIcon());
        stopButton.setGraphicTextGap(8);
        stopButton.setStyle(
            "-fx-background-color: #EF4444; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        StyleUtil.applyErrorButtonHover(stopButton);
        
        // 点击停止按钮时，停止当前任务组
        stopButton.setOnAction(e -> {
            if (callback != null && currentTaskGroupId != null) {
                callback.onStop(currentTaskGroupId);
            }
        });
        
        // 默认隐藏
        stopButton.setVisible(false);
        stopButton.setManaged(false);  // 不占用空间
    }
    
    /**
     * 创建运行中任务下拉菜单
     */
    private void createRunningTasksMenu() {
        runningTasksMenu = new MenuButton("运行中任务");
        runningTasksMenu.setStyle(
            "-fx-background-color: #EF4444; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 4 12 4 12; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;"
        );
        runningTasksMenu.setVisible(false);
    }
    
    /**
     * 更新运行组按钮状态
     */
    private void updateRunGroupButtons() {
        runGroup.getChildren().clear();
        runGroup.getChildren().add(runButton);
        runGroup.getChildren().add(stopButton);  // 添加停止按钮
        runGroup.getChildren().add(runningTasksMenu);
    }
    
    /**
     * 设置当前任务组ID
     */
    public void setCurrentTaskGroupId(Long taskGroupId) {
        this.currentTaskGroupId = taskGroupId;
        updateButtonState();
    }
    
    /**
     * 设置运行按钮的加载状态
     * @param loading true显示加载动画，false恢复正常
     */
    public void setRunButtonLoading(boolean loading) {
        Platform.runLater(() -> {
            if (loading) {
                // 显示加载中状态
                runButton.setText("准备中");
                runButton.setGraphic(createSpinnerIcon());
                runButton.setDisable(true);
            } else {
                // 恢复正常状态
                runButton.setText("开始");
                runButton.setGraphic(IconUtil.playIcon());
                runButton.setDisable(false);
            }
        });
    }
    
    /**
     * 创建旋转的加载图标
     */
    private Node createSpinnerIcon() {
        Circle circle = new Circle(6);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);
        circle.getStrokeDashArray().addAll(3.0, 2.0);
        
        // 创建旋转动画
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), circle);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setInterpolator(Interpolator.LINEAR);
        rotateTransition.play();
        
        return circle;
    }
    
    /**
     * 更新按钮状态
     */
    private void updateButtonState() {
        Platform.runLater(() -> {
            // 判断当前任务组是否正在运行
            boolean isCurrentRunning = currentTaskGroupId != null && 
                runningJobs.containsKey(currentTaskGroupId) && 
                runningJobs.get(currentTaskGroupId).isRunning();
            
            // 如果任务开始运行了，清除加载状态
            if (isCurrentRunning) {
                // 恢复按钮正常状态（如果之前是加载状态）
                runButton.setText("开始");
                runButton.setGraphic(IconUtil.playIcon());
            }
            
            // 获取当前登录用户ID
            String currentUserId = SessionManager.getInstance().getUserId();
            
            // 判断是否应该显示停止按钮
            boolean shouldShowStopButton = false;
            if (isCurrentRunning && currentUserId != null) {
                RunningJobGroup runningJob = runningJobs.get(currentTaskGroupId);
                String triggerUserId = runningJob.getTriggerUserId();
                // 只有当触发用户ID等于当前登录用户ID时才显示停止按钮
                shouldShowStopButton = currentUserId.equals(triggerUserId);
            }
            
            // 显示/隐藏停止按钮
            stopButton.setVisible(shouldShowStopButton);
            stopButton.setManaged(shouldShowStopButton);  // 控制是否占用空间
            
            // 开始按钮始终显示，但在任务运行时禁用
            if (isCurrentRunning) {
                runButton.setDisable(true);
                runButton.setStyle(
                    "-fx-background-color: #9CA3AF; " +
                    "-fx-text-fill: white; " +
                    "-fx-opacity: 0.6; " +
                    "-fx-cursor: default;"
                );
            } else {
                // 任务未运行时，恢复按钮正常状态（清除可能的加载状态）
                runButton.setText("开始");
                runButton.setGraphic(IconUtil.playIcon());
                runButton.setDisable(false);
                StyleUtil.applySuccessButtonHover(runButton);
            }
            
            // 更新运行中任务下拉菜单
            if (runningJobs.isEmpty()) {
                runningTasksMenu.setVisible(false);
            } else {
                runningTasksMenu.setVisible(true);
                runningTasksMenu.setText("运行中 (" + runningJobs.size() + ")");
                runningTasksMenu.getItems().clear();
                
                // 添加每个运行中的任务组 - 扁平化设计
                for (RunningJobGroup job : runningJobs.values()) {
                    if (job.isRunning()) {
                        MenuItem menuItem = new MenuItem(
                            job.getJobName() + " (ID: " + job.getJobId() + ")"
                        );
                        menuItem.setStyle(
                            "-fx-text-fill: #EF4444; " +
                            "-fx-font-weight: bold;"
                        );
                        
                        menuItem.setOnAction(e -> {
                            if (callback != null) {
                                callback.onStop(job.getJobId());
                            }
                        });
                        
                        runningTasksMenu.getItems().add(menuItem);
                    }
                }
                
                // 添加分隔线
                if (!runningTasksMenu.getItems().isEmpty()) {
                    runningTasksMenu.getItems().add(new SeparatorMenuItem());
                    
                    // 添加"停止所有"选项 - 扁平化设计
                    MenuItem stopAllItem = new MenuItem("停止所有");
                    stopAllItem.setStyle(
                        "-fx-text-fill: #EF4444; " +
                        "-fx-font-weight: bold;"
                    );
                    stopAllItem.setOnAction(e -> {
                        for (RunningJobGroup job : runningJobs.values()) {
                            if (job.isRunning() && callback != null) {
                                callback.onStop(job.getJobId());
                            }
                        }
                    });
                    runningTasksMenu.getItems().add(stopAllItem);
                }
            }
        });
    }
    
    /**
     * 更新运行按钮显示，显示运行中的任务组列表
     * @param runningJobs 运行中的任务组列表
     */
    public void updateRunningJobs(Map<Long, RunningJobGroup> runningJobs) {
        this.runningJobs = runningJobs != null ? new java.util.HashMap<>(runningJobs) : new java.util.HashMap<>();
        updateButtonState();
    }
    
    /**
     * 刷新最近打开的文件菜单
     */
    public void refreshRecentFilesMenu() {
        if (recentFilesMenu != null) {
            updateRecentFilesMenu(recentFilesMenu);
        }
    }
}


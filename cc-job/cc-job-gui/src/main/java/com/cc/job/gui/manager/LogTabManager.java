package com.cc.job.gui.manager;

import com.cc.job.gui.util.StyleUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志标签页管理器 - 负责标签页的创建、切换和管理
 */
public class LogTabManager {
    
    public final Map<Long, LogTab> logTabs = new HashMap<>();
    public final Map<Long, LogContentManager> tabDataMap = new HashMap<>();
    private Long currentTaskGroupId;
    
    public LogTabManager() {
        // 创建默认系统日志标签
        tabDataMap.put(null, new LogContentManager());
    }
    
    /**
     * 添加或切换到任务组
     */
    public void addOrSwitchToTaskGroup(Long taskGroupId, String taskGroupName, HBox tabContainer) {
        if (taskGroupId == null) return;
        
        if (!logTabs.containsKey(taskGroupId)) {
            LogTab tab = new LogTab(taskGroupId, taskGroupName);
            logTabs.put(taskGroupId, tab);
            tabContainer.getChildren().add(tab);
            tabDataMap.put(taskGroupId, new LogContentManager());
        }
        
        switchToTaskGroup(taskGroupId);
    }
    
    /**
     * 切换到指定任务组
     */
    public void switchToTaskGroup(Long taskGroupId) {
        logTabs.forEach((id, tab) -> tab.setActive(id.equals(taskGroupId)));
        currentTaskGroupId = taskGroupId;
    }
    
    /**
     * 移除任务组标签
     */
    public void removeTaskGroup(Long taskGroupId, HBox tabContainer) {
        LogTab tab = logTabs.remove(taskGroupId);
        if (tab != null) {
            tabContainer.getChildren().remove(tab);
            tabDataMap.remove(taskGroupId);
            
            if (taskGroupId.equals(currentTaskGroupId)) {
                if (!logTabs.isEmpty()) {
                    switchToTaskGroup(logTabs.keySet().iterator().next());
                } else {
                    switchToTaskGroup(null);
                }
            }
        }
    }
    
    public LogContentManager getCurrentTabData() {
        return tabDataMap.get(currentTaskGroupId);
    }
    
    public LogContentManager getTabData(Long taskGroupId) {
        LogContentManager data = tabDataMap.get(taskGroupId);
        return data != null ? data : getCurrentTabData();
    }
    
    public boolean isCurrentTab(Long taskGroupId) {
        return (taskGroupId == null && currentTaskGroupId == null) ||
               (taskGroupId != null && taskGroupId.equals(currentTaskGroupId));
    }
    
    public Long getCurrentTaskGroupId() {
        return currentTaskGroupId;
    }
    
    public LogTab getLogTab(Long taskGroupId) {
        return logTabs.get(taskGroupId);
    }
    
    /**
     * 清空指定任务组的日志内容（用于重新执行任务组时）
     * @param taskGroupId 任务组ID
     */
    public void clearTaskGroupLogs(Long taskGroupId) {
        LogContentManager tabData = tabDataMap.get(taskGroupId);
        if (tabData != null) {
            tabData.clearEntries();
            tabData.status = "就绪";
            tabData.statusColor = "#10B981";
        }
    }
    
    /**
     * 重新启动任务组 - 清空日志并切换到该标签页
     * 如果标签页不存在则创建新的
     * @param taskGroupId 任务组ID
     * @param taskGroupName 任务组名称
     * @param tabContainer 标签页容器
     */
    public void restartTaskGroup(Long taskGroupId, String taskGroupName, HBox tabContainer) {
        if (taskGroupId == null) return;
        
        // 如果标签页已存在，清空其日志
        if (logTabs.containsKey(taskGroupId)) {
            clearTaskGroupLogs(taskGroupId);
        } else {
            // 创建新标签页
            LogTab tab = new LogTab(taskGroupId, taskGroupName);
            logTabs.put(taskGroupId, tab);
            tabContainer.getChildren().add(tab);
            tabDataMap.put(taskGroupId, new LogContentManager());
        }
        
        // 切换到该标签页
        switchToTaskGroup(taskGroupId);
    }
    
    /**
     * 日志标签页UI组件
     */
    public static class LogTab extends StackPane {
        private final Long taskGroupId;
        private final String taskGroupName;
        private boolean active;
        private Runnable onClickCallback;
        private Runnable onCloseCallback;
        private final Label nameLabel;
        
        private static final String BASE_STYLE = "-fx-background-radius: 0; -fx-border-radius: 0; -fx-cursor: hand; -fx-effect: null;";
        private static final String NORMAL_STYLE = BASE_STYLE + "-fx-background-color: #F1F5F9; -fx-border-color: transparent transparent transparent rgba(243,244,246,0.9); -fx-border-width: 0 0 0 1;";
        private static final String HOVER_STYLE = BASE_STYLE + "-fx-background-color: rgba(241,245,249,0.95); -fx-border-color: transparent transparent transparent rgba(243,244,246,0.9); -fx-border-width: 0 0 0 1;";
        private static final String ACTIVE_STYLE = BASE_STYLE + "-fx-background-color: rgba(243,244,246,0.9); -fx-border-width: 0;";
        
        public LogTab(Long taskGroupId, String taskGroupName) {
            this.taskGroupId = taskGroupId;
            this.taskGroupName = taskGroupName;
            
            setPadding(new Insets(6, 8, 6, 8));
            setPrefHeight(38);
            setMinHeight(38);
            setMaxHeight(38);
            setStyle(NORMAL_STYLE);
            
            HBox content = new HBox(6);
            content.setAlignment(Pos.CENTER_LEFT);
            
            nameLabel = new Label(taskGroupName);
            nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + StyleUtil.GRAY_600 + "; -fx-font-weight: 600;");
            nameLabel.setPrefWidth(100);
            nameLabel.setMaxWidth(100);
            nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            Tooltip.install(nameLabel, new Tooltip(taskGroupName));
            
            Label closeIcon = new Label("×");
            closeIcon.setStyle("-fx-text-fill: " + StyleUtil.GRAY_400 + "; -fx-font-size: 13px; -fx-cursor: hand;");
            
            StackPane closeBtn = new StackPane(closeIcon);
            closeBtn.setPrefSize(20, 20);
            closeBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;");
            
            closeBtn.setOnMouseEntered(e -> {
                closeBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.12); -fx-background-radius: 10; -fx-cursor: hand;");
                closeIcon.setStyle("-fx-text-fill: " + StyleUtil.ERROR + "; -fx-font-size: 13px;");
            });
            
            closeBtn.setOnMouseExited(e -> {
                closeBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;");
                closeIcon.setStyle("-fx-text-fill: " + StyleUtil.GRAY_400 + "; -fx-font-size: 13px;");
            });
            
            closeBtn.setOnMouseClicked(e -> {
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
                e.consume();
            });
            
            content.getChildren().addAll(nameLabel, closeBtn);
            getChildren().add(content);
            
            setOnMouseClicked(e -> {
                if (onClickCallback != null) onClickCallback.run();
            });
            
            setOnMouseEntered(e -> {
                if (!active) setStyle(HOVER_STYLE);
            });
            
            setOnMouseExited(e -> updateStyle());
        }
        
        public void setActive(boolean active) {
            this.active = active;
            updateStyle();
        }
        
        private void updateStyle() {
            if (active) {
                setStyle(ACTIVE_STYLE);
                nameLabel.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + StyleUtil.PRIMARY_DARK + "; -fx-font-weight: 700;");
            } else {
                setStyle(NORMAL_STYLE);
                nameLabel.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + StyleUtil.GRAY_600 + "; -fx-font-weight: 600;");
            }
        }
        
        public void setOnClick(Runnable callback) {
            this.onClickCallback = callback;
        }
        
        public void setOnClose(Runnable callback) {
            this.onCloseCallback = callback;
        }
    }
}


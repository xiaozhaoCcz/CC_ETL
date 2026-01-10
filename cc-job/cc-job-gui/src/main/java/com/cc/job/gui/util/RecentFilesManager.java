package com.cc.job.gui.util;

import com.cc.job.gui.view.TopToolBar.RecentFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 最近打开的文件管理器
 */
public class RecentFilesManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RecentFilesManager.class);
    private static final int MAX_RECENT_FILES = 10;
    private static volatile RecentFilesManager instance;
    
    private final ConfigManager configManager;
    private final List<RecentFile> recentFiles;
    
    private RecentFilesManager() {
        this.configManager = ConfigManager.getInstance();
        this.recentFiles = new ArrayList<>();
        loadRecentFiles();
    }
    
    public static RecentFilesManager getInstance() {
        if (instance == null) {
            synchronized (RecentFilesManager.class) {
                if (instance == null) {
                    instance = new RecentFilesManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 加载最近打开的文件列表
     */
    private void loadRecentFiles() {
        try {
            String recentFilesStr = configManager.getProperty("recent.files", "");
            if (recentFilesStr != null && !recentFilesStr.isEmpty()) {
                String[] entries = recentFilesStr.split("\\|");
                for (String entry : entries) {
                    if (entry != null && !entry.isEmpty()) {
                        String[] parts = entry.split("::");
                        if (parts.length == 3) {
                            try {
                                Long taskGroupId = Long.parseLong(parts[0]);
                                String taskGroupName = parts[1];
                                long lastAccessTime = Long.parseLong(parts[2]);
                                recentFiles.add(new RecentFile(taskGroupId, taskGroupName, lastAccessTime));
                            } catch (NumberFormatException e) {
                                logger.warn("解析最近文件条目失败: {}", entry, e);
                            }
                        }
                    }
                }
                // 按访问时间排序
                recentFiles.sort((a, b) -> Long.compare(b.getLastAccessTime(), a.getLastAccessTime()));
            }
        } catch (Exception e) {
            logger.error("加载最近打开的文件列表失败", e);
        }
    }
    
    /**
     * 保存最近打开的文件列表
     */
    private void saveRecentFiles() {
        try {
            String recentFilesStr = recentFiles.stream()
                .map(f -> f.getTaskGroupId() + "::" + f.getTaskGroupName() + "::" + f.getLastAccessTime())
                .collect(Collectors.joining("|"));
            configManager.setProperty("recent.files", recentFilesStr);
        } catch (Exception e) {
            logger.error("保存最近打开的文件列表失败", e);
        }
    }
    
    /**
     * 添加最近打开的文件
     */
    public void addRecentFile(Long taskGroupId, String taskGroupName) {
        if (taskGroupId == null || taskGroupName == null || taskGroupName.isEmpty()) {
            return;
        }
        
        // 移除已存在的相同文件
        recentFiles.removeIf(f -> f.getTaskGroupId().equals(taskGroupId));
        
        // 添加到列表开头
        recentFiles.add(0, new RecentFile(taskGroupId, taskGroupName, System.currentTimeMillis()));
        
        // 限制最大数量
        if (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.remove(recentFiles.size() - 1);
        }
        
        // 保存
        saveRecentFiles();
    }
    
    /**
     * 获取最近打开的文件列表
     */
    public List<RecentFile> getRecentFiles() {
        return new ArrayList<>(recentFiles);
    }
    
    /**
     * 清除最近打开的文件列表
     */
    public void clearRecentFiles() {
        recentFiles.clear();
        saveRecentFiles();
    }
}

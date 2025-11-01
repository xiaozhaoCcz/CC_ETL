package com.cc.job.gui.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 会话管理器 - 单例模式
 * 管理用户登录状态和会话信息
 */
public class SessionManager {
    
    private static SessionManager instance;
    
    private String token;
    private String userId;
    private String username;
    private boolean loggedIn;
    
    // 会话文件路径
    private static final String SESSION_DIR = System.getProperty("user.home") + File.separator + ".cc-job";
    private static final String SESSION_FILE = SESSION_DIR + File.separator + "session.json";
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private SessionManager() {
        this.loggedIn = false;
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * 登录成功后设置会话信息
     */
    public void login(String token, String userId, String username) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.loggedIn = true;
        
        // 保存会话到本地文件
        saveSessionToFile();
        
        System.out.println("✓ 会话已建立");
        System.out.println("  用户: " + username);
        System.out.println("  用户ID: " + userId);
        System.out.println("  Token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "N/A"));
    }
    
    /**
     * 登出，清除会话信息
     */
    public void logout() {
        this.token = null;
        this.userId = null;
        this.username = null;
        this.loggedIn = false;
        
        // 删除本地会话文件
        deleteSessionFile();
        
        System.out.println("✓ 会话已清除");
    }
    
    /**
     * 检查是否已登录
     */
    public boolean isLoggedIn() {
        return loggedIn && token != null;
    }
    
    /**
     * 获取Token
     */
    public String getToken() {
        return token;
    }
    
    /**
     * 获取用户ID
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * 获取用户名
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * 获取带Token的请求头
     */
    public String getAuthorizationHeader() {
        if (token != null) {
            return "Bearer " + token;
        }
        return null;
    }
    
    /**
     * 保存会话信息到本地文件
     */
    private void saveSessionToFile() {
        try {
            // 确保目录存在
            Path dirPath = Paths.get(SESSION_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            
            // 创建会话数据对象
            SessionData sessionData = new SessionData();
            sessionData.token = this.token;
            sessionData.userId = this.userId;
            sessionData.username = this.username;
            sessionData.timestamp = System.currentTimeMillis();
            
            // 序列化为JSON并保存
            String json = gson.toJson(sessionData);
            Files.write(Paths.get(SESSION_FILE), json.getBytes("UTF-8"));
            
            System.out.println("✓ 会话已保存到本地文件: " + SESSION_FILE);
        } catch (Exception e) {
            System.err.println("⚠ 保存会话文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从本地文件加载会话信息
     */
    public boolean loadSessionFromFile() {
        try {
            Path sessionPath = Paths.get(SESSION_FILE);
            if (!Files.exists(sessionPath)) {
                System.out.println("⚠ 未找到本地会话文件");
                return false;
            }
            
            // 读取文件
            String json = new String(Files.readAllBytes(sessionPath), "UTF-8");
            SessionData sessionData = gson.fromJson(json, SessionData.class);
            
            if (sessionData == null || sessionData.token == null) {
                System.out.println("⚠ 会话文件无效");
                return false;
            }
            
            // 检查会话是否过期（30天）
            long age = System.currentTimeMillis() - sessionData.timestamp;
            long maxAge = 30L * 24 * 60 * 60 * 1000; // 30天
            if (age > maxAge) {
                System.out.println("⚠ 会话已过期");
                deleteSessionFile();
                return false;
            }
            
            // 恢复会话信息
            this.token = sessionData.token;
            this.userId = sessionData.userId;
            this.username = sessionData.username;
            this.loggedIn = true;
            
            System.out.println("✓ 会话已从本地文件恢复");
            System.out.println("  用户: " + username);
            System.out.println("  用户ID: " + userId);
            System.out.println("  会话时间: " + (age / (1000 * 60 * 60 * 24)) + " 天前");
            
            return true;
        } catch (Exception e) {
            System.err.println("⚠ 加载会话文件失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 删除本地会话文件
     */
    private void deleteSessionFile() {
        try {
            Path sessionPath = Paths.get(SESSION_FILE);
            if (Files.exists(sessionPath)) {
                Files.delete(sessionPath);
                System.out.println("✓ 本地会话文件已删除");
            }
        } catch (Exception e) {
            System.err.println("⚠ 删除会话文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 会话数据类（用于JSON序列化）
     */
    private static class SessionData {
        String token;
        String userId;
        String username;
        long timestamp;
    }
}


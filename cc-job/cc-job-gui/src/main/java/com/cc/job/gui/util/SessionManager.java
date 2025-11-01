package com.cc.job.gui.util;

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
}


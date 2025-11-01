package com.cc.job.gui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 登录服务
 */
public class LoginService extends BaseService {
    
    private static final String LOGIN_API = "/api/v1/auth/login";
    private static final String REGISTER_API = "/api/v1/auth/register";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public LoginService() {
        super();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    public LoginResult login(String username, String password) throws IOException, InterruptedException {
        try {
            // 获取API基础URL
            String baseUrl = apiUtil.getBaseUrl();
            
            // 构建URL参数（使用表单格式）
            String urlParams = String.format("username=%s&password=%s", 
                java.net.URLEncoder.encode(username, "UTF-8"),
                java.net.URLEncoder.encode(password, "UTF-8")
            );
            String url = baseUrl + LOGIN_API + "?" + urlParams;
            
            // 创建HTTP请求（使用POST方法但参数在URL中）
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(10))
                .build();
            
            System.out.println("🔐 发送登录请求: " + url);
            
            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 解析响应
            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                
                // 检查响应结构
                if (jsonNode.has("code")) {
                    int code = jsonNode.get("code").asInt();
                    String message = jsonNode.has("msg") ? jsonNode.get("msg").asText() : "未知错误";
                    
                    if (code == 200 || code == 0) {
                        // 登录成功
                        String token = jsonNode.has("data") && jsonNode.get("data").has("token") 
                            ? jsonNode.get("data").get("token").asText() 
                            : "";
                        
                        String userId = jsonNode.has("data") && jsonNode.get("data").has("userId") 
                            ? jsonNode.get("data").get("userId").asText() 
                            : "";
                        
                        System.out.println("✓ 登录成功: " + username);
                        return new LoginResult(true, "登录成功", token, userId, username);
                    } else {
                        // 登录失败
                        System.out.println("✗ 登录失败: " + message);
                        return new LoginResult(false, message, null, null, null);
                    }
                } else {
                    return new LoginResult(false, "响应格式错误", null, null, null);
                }
            } else {
                System.out.println("✗ 登录请求失败，状态码: " + response.statusCode());
                return new LoginResult(false, "登录请求失败，状态码: " + response.statusCode(), null, null, null);
            }
            
        } catch (Exception e) {
            System.err.println("✗ 登录异常: " + e.getMessage());
            e.printStackTrace();
            return new LoginResult(false, "登录失败: " + e.getMessage(), null, null, null);
        }
    }
    
    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @return 注册结果
     */
    public LoginResult register(String username, String password) throws IOException, InterruptedException {
        try {
            // 获取API基础URL
            String baseUrl = apiUtil.getBaseUrl();
            
            // 构建URL参数（使用表单格式）
            String urlParams = String.format("username=%s&password=%s", 
                java.net.URLEncoder.encode(username, "UTF-8"),
                java.net.URLEncoder.encode(password, "UTF-8")
            );
            String url = baseUrl + REGISTER_API + "?" + urlParams;
            
            // 创建HTTP请求（使用POST方法但参数在URL中）
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(10))
                .build();
            
            System.out.println("📝 发送注册请求: " + url);
            
            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 解析响应
            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                
                // 检查响应结构
                if (jsonNode.has("code")) {
                    int code = jsonNode.get("code").asInt();
                    String message = jsonNode.has("msg") ? jsonNode.get("msg").asText() : "未知错误";
                    
                    if (code == 200 || code == 0) {
                        // 注册成功
                        String token = jsonNode.has("data") && jsonNode.get("data").has("token") 
                            ? jsonNode.get("data").get("token").asText() 
                            : "";
                        
                        String userId = jsonNode.has("data") && jsonNode.get("data").has("userId") 
                            ? jsonNode.get("data").get("userId").asText() 
                            : "";
                        
                        System.out.println("✓ 注册成功: " + username);
                        return new LoginResult(true, "注册成功", token, userId, username);
                    } else {
                        // 注册失败
                        System.out.println("✗ 注册失败: " + message);
                        return new LoginResult(false, message, null, null, null);
                    }
                } else {
                    return new LoginResult(false, "响应格式错误", null, null, null);
                }
            } else {
                System.out.println("✗ 注册请求失败，状态码: " + response.statusCode());
                return new LoginResult(false, "注册请求失败，状态码: " + response.statusCode(), null, null, null);
            }
            
        } catch (Exception e) {
            System.err.println("✗ 注册异常: " + e.getMessage());
            e.printStackTrace();
            return new LoginResult(false, "注册失败: " + e.getMessage(), null, null, null);
        }
    }
    
    /**
     * 登录结果类
     */
    public static class LoginResult {
        private final boolean success;
        private final String message;
        private final String token;
        private final String userId;
        private final String username;
        
        public LoginResult(boolean success, String message, String token, String userId, String username) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.userId = userId;
            this.username = username;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getToken() {
            return token;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getUsername() {
            return username;
        }
    }
}


package com.cc.job.gui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);
    
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
            
            
            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 解析响应
            
            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                
                // 检查响应结构
                if (jsonNode.has("code")) {
                    // 后端返回的code是字符串类型，如 "00000"
                    String code = jsonNode.get("code").asText();
                    String message = jsonNode.has("msg") ? jsonNode.get("msg").asText() : "未知错误";
                    
                    // 成功码是 "00000"
                    if ("00000".equals(code)) {
                        // 登录成功，从data中获取LoginResult对象
                        JsonNode dataNode = jsonNode.get("data");
                        if (dataNode != null && !dataNode.isNull()) {
                            // 后端返回的字段是 accessToken，不是 token
                            String token = dataNode.has("accessToken") 
                                ? dataNode.get("accessToken").asText() 
                                : "";
                            
                            // userId 是 Long 类型，需要转换为字符串
                            String userId = null;
                            if (dataNode.has("userId")) {
                                JsonNode userIdNode = dataNode.get("userId");
                                if (userIdNode.isNumber()) {
                                    userId = String.valueOf(userIdNode.asLong());
                                } else if (userIdNode.isTextual()) {
                                    userId = userIdNode.asText();
                                }
                            }
                            
                            // username 是字符串
                            String resultUsername = dataNode.has("username") 
                                ? dataNode.get("username").asText() 
                                : username;
                            
                            if (token != null && !token.isEmpty()) {
                                return new LoginResult(true, "登录成功", token, userId, resultUsername);
                            } else {
                                return new LoginResult(false, "登录响应中缺少token", null, null, null);
                            }
                        } else {
                            return new LoginResult(false, "登录响应中缺少data字段", null, null, null);
                        }
                    } else {
                        // 登录失败
                        return new LoginResult(false, message, null, null, null);
                    }
                } else {
                    logger.error("✗ 响应格式错误，缺少code字段");
                    return new LoginResult(false, "响应格式错误，缺少code字段", null, null, null);
                }
            } else {
                logger.error("✗ 登录请求失败，状态码: {}, 响应体: {}", response.statusCode(), response.body());
                return new LoginResult(false, "登录请求失败，状态码: " + response.statusCode(), null, null, null);
            }
            
        } catch (Exception e) {
            logger.error("✗ 登录异常: {}", e.getMessage(), e);
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
            
            
            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 解析响应
            
            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                
                // 检查响应结构
                if (jsonNode.has("code")) {
                    // 后端返回的code是字符串类型，如 "00000"
                    String code = jsonNode.get("code").asText();
                    String message = jsonNode.has("msg") ? jsonNode.get("msg").asText() : "未知错误";
                    
                    // 成功码是 "00000"
                    if ("00000".equals(code)) {
                        // 注册成功，从data中获取LoginResult对象
                        JsonNode dataNode = jsonNode.get("data");
                        if (dataNode != null && !dataNode.isNull()) {
                            // 后端返回的字段是 accessToken，不是 token
                            String token = dataNode.has("accessToken") 
                                ? dataNode.get("accessToken").asText() 
                                : "";
                            
                            // userId 是 Long 类型，需要转换为字符串
                            String userId = null;
                            if (dataNode.has("userId")) {
                                JsonNode userIdNode = dataNode.get("userId");
                                if (userIdNode.isNumber()) {
                                    userId = String.valueOf(userIdNode.asLong());
                                } else if (userIdNode.isTextual()) {
                                    userId = userIdNode.asText();
                                }
                            }
                            
                            // username 是字符串
                            String resultUsername = dataNode.has("username") 
                                ? dataNode.get("username").asText() 
                                : username;
                            
                            if (token != null && !token.isEmpty()) {
                                return new LoginResult(true, "注册成功", token, userId, resultUsername);
                            } else {
                                return new LoginResult(false, "注册响应中缺少token", null, null, null);
                            }
                        } else {
                            return new LoginResult(false, "注册响应中缺少data字段", null, null, null);
                        }
                    } else {
                        // 注册失败
                        return new LoginResult(false, message, null, null, null);
                    }
                } else {
                    logger.error("✗ 响应格式错误，缺少code字段");
                    return new LoginResult(false, "响应格式错误，缺少code字段", null, null, null);
                }
            } else {
                logger.error("✗ 注册请求失败，状态码: {}, 响应体: {}", response.statusCode(), response.body());
                return new LoginResult(false, "注册请求失败，状态码: " + response.statusCode(), null, null, null);
            }
            
        } catch (Exception e) {
            logger.error("✗ 注册异常: {}", e.getMessage(), e);
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


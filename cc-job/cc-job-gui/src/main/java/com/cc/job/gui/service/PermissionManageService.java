package com.cc.job.gui.service;

import com.cc.job.gui.util.SessionManager;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobRole;
import com.cc.job.xo.model.vo.UserListVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限管理相关 API（拉取当前用户权限、用户列表、角色、用户-角色绑定）
 */
public class PermissionManageService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionManageService.class);

    private static final String PERMISSIONS_API = "/api/v1/auth/permissions";

    private final HttpClient javaHttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 拉取当前用户权限码并写入 SessionManager（主窗口显示后调用）
     */
    public void fetchAndStorePermissions() {
        try {
            String baseUrl = apiUtil.getBaseUrl();
            String url = baseUrl + PERMISSIONS_API;
            String auth = SessionManager.getInstance().getAuthorizationHeader();
            if (auth == null) {
                SessionManager.getInstance().setPermissions(List.of());
                return;
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", auth)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = javaHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                SessionManager.getInstance().setPermissions(List.of());
                return;
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("code") && "00000".equals(root.get("code").asText())) {
                JsonNode data = root.get("data");
                List<String> list = objectMapper.convertValue(data, new TypeReference<List<String>>() {});
                SessionManager.getInstance().setPermissions(list != null ? list : List.of());
            } else {
                SessionManager.getInstance().setPermissions(List.of());
            }
        } catch (Exception e) {
            logger.warn("拉取权限列表失败: {}", e.getMessage());
            SessionManager.getInstance().setPermissions(List.of());
        }
    }

    public List<UserListVO> listUsers() throws IOException {
        Result<List<UserListVO>> result = httpClient.get("/api/v1/users", new TypeToken<List<UserListVO>>() {});
        return httpClient.extractData(result, "获取用户列表失败");
    }

    public List<JobRole> listRoles() throws IOException {
        Result<List<JobRole>> result = httpClient.get("/api/v1/roles", new TypeToken<List<JobRole>>() {});
        return httpClient.extractData(result, "获取角色列表失败");
    }

    public List<Long> getUserRoles(Long userId) throws IOException {
        Result<List<Long>> result = httpClient.get("/api/v1/users/" + userId + "/roles", new TypeToken<List<Long>>() {});
        return httpClient.extractDataOrNull(result, "获取用户角色失败") != null ? httpClient.extractData(result, "获取用户角色失败") : new ArrayList<>();
    }

    public void assignRole(Long userId, Long roleId) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("roleId", roleId);
        httpClient.postForBoolean("/api/v1/users/" + userId + "/roles", body);
    }

    public void removeRole(Long userId, Long roleId) throws IOException {
        httpClient.deleteForBoolean("/api/v1/users/" + userId + "/roles/" + roleId);
    }

    /** 返回项为 Map，包含 id、userId、resourceType、resourceId、permissionType；id 用于 revokeResourcePermission。 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listResourcePermissions(Long userId, String resourceType) throws IOException {
        String path = "/api/v1/resourcePermissions/list?userId=" + userId;
        if (resourceType != null && !resourceType.isEmpty()) {
            path += "&resourceType=" + resourceType;
        }
        TypeToken<List<Map<String, Object>>> token = new TypeToken<List<Map<String, Object>>>() {};
        Result<List<Map<String, Object>>> result = httpClient.get(path, token);
        List<Map<String, Object>> list = httpClient.extractDataOrNull(result, "获取资源授权列表失败");
        return list != null ? list : new ArrayList<>();
    }

    public void grantResourcePermission(Long userId, String resourceType, Long resourceId, String permissionType) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("resourceType", resourceType);
        body.put("resourceId", resourceId);
        body.put("permissionType", permissionType);
        httpClient.postForBoolean("/api/v1/resourcePermissions/grant", body);
    }

    public void revokeResourcePermission(Long id) throws IOException {
        httpClient.deleteForBoolean("/api/v1/resourcePermissions/" + id);
    }
}

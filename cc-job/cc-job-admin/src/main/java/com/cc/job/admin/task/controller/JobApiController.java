package com.cc.job.admin.task.controller;

import cn.hutool.core.lang.Pair;
import com.cc.job.admin.task.thread.JobLogHelper;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.GsonTool;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by xuxueli on 17/5/10.
 */
@Controller
@RequestMapping("/xxl-job-admin/api")
public class JobApiController {

    @Resource
    private AdminBiz adminBiz;

    // SSE连接池
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> SSE_CONNECTIONS = new ConcurrentHashMap<>();

    /**
     * api
     *
     * @param uri
     * @param data
     * @return
     */
    @RequestMapping("/{uri}")
    @ResponseBody
    public ReturnT<String> api(HttpServletRequest request, @PathVariable("uri") String uri,
            @RequestBody(required = false) String data) {

        // valid
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
        }
        if (uri == null || uri.trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
        }
        // if (XxlJobAdminConfig.getAdminConfig().getAccessToken()!=null
        // && XxlJobAdminConfig.getAdminConfig().getAccessToken().trim().length()>0
        // &&
        // !XxlJobAdminConfig().getAccessToken().equals(request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN)))
        // {
        // return new ReturnT<>(ReturnT.FAIL_CODE, "The access token is wrong.");
        // }

        // services mapping
        if ("callback".equals(uri)) {
            List<HandleCallbackParam> callbackParamList = GsonTool.fromJson(data, List.class,
                    HandleCallbackParam.class);
            return adminBiz.callback(callbackParamList);
        } else if ("registry".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registry(registryParam);
        } else if ("registryRemove".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registryRemove(registryParam);
        } else if ("jobLogId".equals(uri)) {
            Pair<String, String> pair = GsonTool.fromJson(data, Pair.class);
            JobLogHelper.addJobLog(pair);
            return new ReturnT<>(ReturnT.SUCCESS_CODE, "success");
        } else if ("updateRegistryValue".equals(uri)) {
            // ⭐ 更新注册信息的registryValue（用于executor-compose更新HTTP端口信息）
            Map<String, String> params = GsonTool.fromJson(data, Map.class);
            String registryGroup = params.get("registryGroup");
            String registryKey = params.get("registryKey");
            String oldRegistryValue = params.get("oldRegistryValue");
            String newRegistryValue = params.get("newRegistryValue");
            return com.cc.job.admin.task.thread.JobRegistryHelper.getInstance()
                    .updateRegistryValue(registryGroup, registryKey, oldRegistryValue, newRegistryValue);
        } else {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping(" + uri + ") not found.");
        }

    }

    /**
     * 建立SSE连接
     */
    @GetMapping("/sse/{jobId}/{randomId}")
    public SseEmitter subscribeToEvents(@PathVariable Long jobId, @PathVariable String randomId) {
        String connectionKey = jobId + ":" + randomId;

        SseEmitter emitter = new SseEmitter(0L); // 无超时时间

        // 设置连接建立时的回调
        emitter.onCompletion(() -> {
            // log.info("SSE连接完成: {}", connectionKey); // Original code had this line
            // commented out
            removeSseConnection(connectionKey, emitter);
        });

        emitter.onTimeout(() -> {
            // log.info("SSE连接超时: {}", connectionKey); // Original code had this line
            // commented out
            removeSseConnection(connectionKey, emitter);
        });

        emitter.onError((ex) -> {
            // log.error("SSE连接错误: {}", ex.getMessage(), ex); // Original code had this line
            // commented out
            removeSseConnection(connectionKey, emitter);
        });

        // 添加到连接池
        SSE_CONNECTIONS.computeIfAbsent(connectionKey, k -> new CopyOnWriteArrayList<>()).add(emitter);

        try {
            // 发送连接成功消息
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("连接成功"));
        } catch (IOException e) {
            // log.error("发送连接消息失败: {}", e.getMessage(), e); // Original code had this line
            // commented out
        }

        // log.info("建立SSE连接: {}, 当前连接数: {}", connectionKey, SSE_CONNECTIONS.size()); //
        // Original code had this line commented out
        return emitter;
    }

    /**
     * 发送SSE消息
     */
    public static void sendSseMessage(Long jobId, String randomId, Object data) {
        String connectionKey = jobId + ":" + randomId;
        CopyOnWriteArrayList<SseEmitter> emitters = SSE_CONNECTIONS.get(connectionKey);

        if (emitters != null && !emitters.isEmpty()) {
            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(data));
                } catch (IOException e) {
                    // log.error("发送SSE消息失败: {}", e.getMessage(), e); // Original code had this line
                    // commented out
                    deadEmitters.add(emitter);
                }
            }

            // 移除失效的连接
            emitters.removeAll(deadEmitters);
        }
    }

    /**
     * 移除SSE连接
     */
    private void removeSseConnection(String connectionKey, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = SSE_CONNECTIONS.get(connectionKey);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                SSE_CONNECTIONS.remove(connectionKey);
            }
        }
    }

    /**
     * 获取SSE连接统计
     */
    @GetMapping("/sse/stats")
    public Map<String, Object> getSseStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConnections", SSE_CONNECTIONS.size());
        stats.put("connectionDetails", SSE_CONNECTIONS.keySet());
        return stats;
    }

}

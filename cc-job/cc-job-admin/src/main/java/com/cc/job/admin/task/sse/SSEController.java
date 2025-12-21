package com.cc.job.admin.task.sse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE控制器
 * 提供SSE连接端点
 * 
 * @author xiaozhao
 */
@Tag(name = "SSE接口")
@RestController
@RequestMapping("/api/v1/sse")
public class SSEController {

    private static final Logger log = LoggerFactory.getLogger(SSEController.class);

    private final SSEService sseService;

    public SSEController(SSEService sseService) {
        this.sseService = sseService;
    }

    /**
     * 建立SSE连接
     * 
     * @param parentJobId 父任务ID
     * @param randomId 随机ID
     * @return SseEmitter
     */
    @Operation(summary = "建立SSE连接，接收节点状态更新")
    @GetMapping(value = "/nodeStatus/{parentJobId}/{randomId}", 
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNodeStatus(
            @Parameter(description = "父任务ID") @PathVariable Long parentJobId,
            @Parameter(description = "随机ID") @PathVariable String randomId) {
        
        SseEmitter emitter = sseService.createConnection(parentJobId, randomId);
        
        // 发送初始连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\",\"parentJobId\":" + parentJobId + ",\"randomId\":\"" + randomId + "\"}"));
        } catch (Exception e) {
            log.error("[SSE] 发送初始消息失败", e);
        }
        
        return emitter;
    }

    /**
     * 关闭SSE连接
     * 
     * @param parentJobId 父任务ID
     * @param randomId 随机ID
     */
    @Operation(summary = "关闭SSE连接")
    @DeleteMapping("/nodeStatus/{parentJobId}/{randomId}")
    public void closeConnection(
            @Parameter(description = "父任务ID") @PathVariable Long parentJobId,
            @Parameter(description = "随机ID") @PathVariable String randomId) {
        
        sseService.closeConnection(parentJobId, randomId);
    }
}


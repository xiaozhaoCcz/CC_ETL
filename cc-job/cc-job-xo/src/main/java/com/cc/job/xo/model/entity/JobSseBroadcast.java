package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * SSE 多实例广播队列表实体
 * 用于多 Admin 实例时跨实例推送节点状态到 PC 端
 *
 * @author cc-job
 */
@TableName("job_sse_broadcast")
public class JobSseBroadcast {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * SSE 连接键 parentJobId:randomId
     */
    private String connectionKey;

    /**
     * SSE 消息体 JSON
     */
    private String messageJson;

    /**
     * 0=待处理 1=已处理
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConnectionKey() {
        return connectionKey;
    }

    public void setConnectionKey(String connectionKey) {
        this.connectionKey = connectionKey;
    }

    public String getMessageJson() {
        return messageJson;
    }

    public void setMessageJson(String messageJson) {
        this.messageJson = messageJson;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

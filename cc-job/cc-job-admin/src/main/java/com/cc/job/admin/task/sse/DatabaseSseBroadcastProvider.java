package com.cc.job.admin.task.sse;

import com.cc.job.xo.model.entity.JobSseBroadcast;
import com.cc.job.xo.mapper.JobSseBroadcastMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 基于数据库的 SSE 广播实现
 * 将节点状态写入 job_sse_broadcast 表，由各实例轮询后在本机推送
 *
 * @author cc-job
 */
@Component
@ConditionalOnProperty(prefix = "cc-job.sse.broadcast", name = "enabled", havingValue = "true")
public class DatabaseSseBroadcastProvider implements SseBroadcastProvider {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSseBroadcastProvider.class);

    private final JobSseBroadcastMapper jobSseBroadcastMapper;

    public DatabaseSseBroadcastProvider(JobSseBroadcastMapper jobSseBroadcastMapper) {
        this.jobSseBroadcastMapper = jobSseBroadcastMapper;
    }

    @Override
    public void publishNodeStatus(String connectionKey, String messageJson) {
        if (connectionKey == null || messageJson == null) {
            log.warn("[SSE-Broadcast] 参数为空，跳过发布");
            return;
        }
        try {
            JobSseBroadcast record = new JobSseBroadcast();
            record.setConnectionKey(connectionKey);
            record.setMessageJson(messageJson);
            record.setStatus(0);
            jobSseBroadcastMapper.insert(record);
            log.debug("[SSE-Broadcast] 已写入广播表 - key: {}", connectionKey);
        } catch (Exception e) {
            log.error("[SSE-Broadcast] 写入广播表失败 - key: {}", connectionKey, e);
        }
    }
}

package com.cc.job.admin.task.sse;

import com.cc.job.xo.model.entity.JobSseBroadcast;
import com.cc.job.xo.mapper.JobSseBroadcastMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SSE 广播表轮询任务
 * 各 Admin 实例定时拉取待处理记录，仅在本机存在对应 SSE 连接时推送并标记已处理
 *
 * @author cc-job
 */
@Component
@ConditionalOnProperty(prefix = "cc-job.sse.broadcast", name = "enabled", havingValue = "true")
public class SseBroadcastPollingRunner {

    private static final Logger log = LoggerFactory.getLogger(SseBroadcastPollingRunner.class);

    /** 只拉取该秒数内创建的待处理记录，避免积压 */
    private static final int PENDING_SECONDS = 30;
    private static final int POLL_LIMIT = 100;

    private final JobSseBroadcastMapper jobSseBroadcastMapper;
    private final SSEService sseService;

    public SseBroadcastPollingRunner(JobSseBroadcastMapper jobSseBroadcastMapper, SSEService sseService) {
        this.jobSseBroadcastMapper = jobSseBroadcastMapper;
        this.sseService = sseService;
    }

    @Scheduled(fixedDelayString = "${cc-job.sse.broadcast.poll-interval-ms:300}")
    public void pollAndPush() {
        try {
            LocalDateTime createdAfter = LocalDateTime.now().minusSeconds(PENDING_SECONDS);
            List<JobSseBroadcast> list = jobSseBroadcastMapper.selectPending(createdAfter, POLL_LIMIT);
            if (list == null || list.isEmpty()) {
                return;
            }
            for (JobSseBroadcast record : list) {
                boolean pushed = sseService.sendMessageToLocalOnly(record.getConnectionKey(), record.getMessageJson());
                if (pushed) {
                    jobSseBroadcastMapper.markProcessed(record.getId());
                }
            }
        } catch (Exception e) {
            log.warn("[SSE-Broadcast] 轮询推送异常", e);
        }
    }

    @Scheduled(fixedDelayString = "${cc-job.sse.broadcast.cleanup-interval-ms:300000}")
    public void cleanupProcessed() {
        try {
            LocalDateTime before = LocalDateTime.now().minusHours(1);
            int deleted = jobSseBroadcastMapper.deleteProcessedBefore(before);
            if (deleted > 0) {
                log.debug("[SSE-Broadcast] 清理已处理记录 {} 条", deleted);
            }
        } catch (Exception e) {
            log.warn("[SSE-Broadcast] 清理已处理记录异常", e);
        }
    }
}

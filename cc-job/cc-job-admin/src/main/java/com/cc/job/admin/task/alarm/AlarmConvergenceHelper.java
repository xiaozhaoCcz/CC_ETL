package com.cc.job.admin.task.alarm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警收敛：同一 jobId 在 N 分钟内只告警一次，避免重复轰炸。
 *
 * @author cc-job-team
 */
@Component
public class AlarmConvergenceHelper {

    private static final Logger logger = LoggerFactory.getLogger(AlarmConvergenceHelper.class);

    /**
     * 收敛时间（分钟），0 表示关闭收敛
     */
    @Value("${cc-job.alarm.convergence-minutes:0}")
    private int convergenceMinutes;

    private final Map<Long, Long> lastAlarmTimeByJobId = new ConcurrentHashMap<>();

    /**
     * 是否应跳过本次告警（同一任务在收敛时间窗内已告警过）
     */
    public boolean shouldSkipAlarm(Long jobId) {
        if (convergenceMinutes <= 0 || jobId == null) {
            return false;
        }
        Long last = lastAlarmTimeByJobId.get(jobId);
        if (last == null) {
            return false;
        }
        long windowMs = convergenceMinutes * 60L * 1000L;
        if (System.currentTimeMillis() - last < windowMs) {
            logger.debug("[AlarmConvergence] skip alarm for jobId={}, lastAlarm={}ms ago", jobId, System.currentTimeMillis() - last);
            return true;
        }
        return false;
    }

    /**
     * 记录本次告警时间，用于后续收敛判断
     */
    public void recordAlarm(Long jobId) {
        if (convergenceMinutes <= 0 || jobId == null) {
            return;
        }
        lastAlarmTimeByJobId.put(jobId, System.currentTimeMillis());
        // 简单清理：若 map 过大则移除最旧的一部分（按插入顺序无法保证，这里仅做数量限制）
        if (lastAlarmTimeByJobId.size() > 10_000) {
            long cutoff = System.currentTimeMillis() - (convergenceMinutes * 60L * 1000L * 2);
            lastAlarmTimeByJobId.entrySet().removeIf(e -> e.getValue() < cutoff);
        }
    }
}

package com.cc.job.admin.core.schedule;

import com.cc.job.admin.cron.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 任务调度服务
 * 
 * 负责任务调度相关的业务逻辑，如计算下次执行时间等
 *
 * @author cc-job
 * @since 2025-12-02
 */
@Service
public class JobScheduleService {

    private static final Logger log = LoggerFactory.getLogger(JobScheduleService.class);

    /** 预测次数 */
    private static final int PREDICT_COUNT = 5;

    /**
     * 计算任务的下一次执行时间列表
     *
     * @param scheduleType 调度类型
     * @param scheduleConf 调度配置
     * @return 下次执行时间列表
     */
    public List<String> calculateNextTriggerTimes(String scheduleType, String scheduleConf) {
        log.debug("[JobSchedule] 计算下次执行时间 - scheduleType: {}, scheduleConf: {}", 
                scheduleType, scheduleConf);

        List<String> result = new ArrayList<>();

        try {
            if ("CRON".equals(scheduleType)) {
                result = calculateCronNextTimes(scheduleConf);
            } else if ("FIX_RATE".equals(scheduleType)) {
                result = calculateFixRateNextTimes(scheduleConf);
            } else if ("FIX_DELAY".equals(scheduleType)) {
                result = calculateFixDelayNextTimes(scheduleConf);
            } else {
                log.warn("[JobSchedule] 不支持的调度类型 - scheduleType: {}", scheduleType);
                result.add("不支持的调度类型");
            }
        } catch (Exception e) {
            log.error("[JobSchedule] 计算下次执行时间失败 - scheduleType: {}, scheduleConf: {}", 
                    scheduleType, scheduleConf, e);
            result.add("计算失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 计算CRON表达式的下几次执行时间
     */
    private List<String> calculateCronNextTimes(String cronExpression) throws ParseException {
        List<String> result = new ArrayList<>();
        
        CronExpression cron = new CronExpression(cronExpression);
        Date now = new Date();

        for (int i = 0; i < PREDICT_COUNT; i++) {
            Date nextTime = cron.getNextValidTimeAfter(now);
            if (nextTime != null) {
                result.add(formatDate(nextTime));
                now = nextTime;
            } else {
                break;
            }
        }

        log.debug("[JobSchedule] CRON下次执行时间 - count: {}", result.size());
        return result;
    }

    /**
     * 计算固定速率的下几次执行时间
     */
    private List<String> calculateFixRateNextTimes(String fixRateConf) {
        List<String> result = new ArrayList<>();
        
        try {
            int rateSeconds = Integer.parseInt(fixRateConf);
            long now = System.currentTimeMillis();

            for (int i = 1; i <= PREDICT_COUNT; i++) {
                long nextTime = now + (rateSeconds * 1000L * i);
                result.add(formatTimestamp(nextTime));
            }
        } catch (NumberFormatException e) {
            log.error("[JobSchedule] 固定速率配置错误 - fixRateConf: {}", fixRateConf, e);
            result.add("配置错误: " + e.getMessage());
        }

        log.debug("[JobSchedule] 固定速率下次执行时间 - count: {}", result.size());
        return result;
    }

    /**
     * 计算固定延迟的下几次执行时间
     */
    private List<String> calculateFixDelayNextTimes(String fixDelayConf) {
        List<String> result = new ArrayList<>();
        
        try {
            int delaySeconds = Integer.parseInt(fixDelayConf);
            long now = System.currentTimeMillis();

            // 固定延迟模式：每次执行完成后延迟指定时间再执行
            // 这里假设每次执行耗时相同（简化处理）
            int assumedExecutionTime = 10; // 假设10秒
            
            for (int i = 1; i <= PREDICT_COUNT; i++) {
                long nextTime = now + ((delaySeconds + assumedExecutionTime) * 1000L * i);
                result.add(formatTimestamp(nextTime) + " (预估)");
            }
        } catch (NumberFormatException e) {
            log.error("[JobSchedule] 固定延迟配置错误 - fixDelayConf: {}", fixDelayConf, e);
            result.add("配置错误: " + e.getMessage());
        }

        log.debug("[JobSchedule] 固定延迟下次执行时间 - count: {}", result.size());
        return result;
    }

    /**
     * 计算下一次执行时间
     *
     * @param scheduleType 调度类型
     * @param scheduleConf 调度配置
     * @param fromTime     起始时间
     * @return 下次执行时间（毫秒）
     */
    public long calculateNextTriggerTime(String scheduleType, String scheduleConf, long fromTime) {
        try {
            if ("CRON".equals(scheduleType)) {
                CronExpression cron = new CronExpression(scheduleConf);
                Date nextTime = cron.getNextValidTimeAfter(new Date(fromTime));
                return nextTime != null ? nextTime.getTime() : 0L;
            } else if ("FIX_RATE".equals(scheduleType)) {
                int rateSeconds = Integer.parseInt(scheduleConf);
                return fromTime + (rateSeconds * 1000L);
            } else if ("FIX_DELAY".equals(scheduleType)) {
                int delaySeconds = Integer.parseInt(scheduleConf);
                return fromTime + (delaySeconds * 1000L);
            }
        } catch (Exception e) {
            log.error("[JobSchedule] 计算下次执行时间失败", e);
        }
        
        return 0L;
    }

    /**
     * 验证CRON表达式
     *
     * @param cronExpression CRON表达式
     * @return 是否有效
     */
    public boolean validateCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }

        try {
            new CronExpression(cronExpression);
            return true;
        } catch (Exception e) {
            log.warn("[JobSchedule] CRON表达式验证失败 - expression: {}", cronExpression, e);
            return false;
        }
    }

    /**
     * 格式化日期
     */
    private String formatDate(Date date) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(
                date.toInstant(), ZoneId.systemDefault());
        return localDateTime.toString().replace("T", " ");
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        return formatDate(date);
    }
}


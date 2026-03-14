package com.cc.job.admin.task.utils;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {

    public static final String DATE_FORMAT1 = "yyyy-MM-dd HH:mm:ss";

    public static Date asDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date asDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate asLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDateTime asLocalDateTime(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 将日期时间字符串格式化为 yyyy-MM-dd HH:mm:ss。
     * 兼容无时区的 ISO_LOCAL_DATE_TIME（如 2025-03-14T00:00:00）与带时区的 ZonedDateTime 格式。
     */
    public static String formatDate(String isoDateString) {
        if (isoDateString == null || isoDateString.trim().isEmpty()) {
            return null;
        }
        DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT1);
        try {
            // 先尝试按无时区的 LocalDateTime 解析（前端任务日志清理传入的格式）
            LocalDateTime localDateTime = LocalDateTime.parse(isoDateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return localDateTime.atZone(ZoneId.systemDefault()).format(targetFormatter);
        } catch (Exception ignored) {
            // 再尝试带时区的 ZonedDateTime
        }
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(isoDateString);
        return zonedDateTime.format(targetFormatter);
    }

    public static String formatDate(String timeStamp, String timeFormat) {
        // 解析字符串为ZonedDateTime对象
        final SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
        // 使用format方法将时间戳转换为日期字符串
        String formattedDate = sdf.format(new Date(timeStamp));
        return formattedDate;
    }

    public static LocalDateTime processDate(String timeStamp) {
        // 将字符串转换为Instant对象
        Instant instant = Instant.ofEpochMilli(Long.parseLong(timeStamp));

        // 将Instant转换为LocalDateTime
        LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        return localDateTime;
    }
}

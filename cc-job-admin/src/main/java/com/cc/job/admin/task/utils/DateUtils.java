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

    public static String formatDate(String isoDateString) {
        // 解析字符串为ZonedDateTime对象
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(isoDateString);

        // 定义目标格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT1);

        // 格式化日期时间
        return zonedDateTime.format(formatter);
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

/**
 * 告警 SPI：新增告警渠道只需实现 {@link com.cc.job.admin.task.alarm.JobAlarm}，
 * 实现 {@link com.cc.job.admin.task.alarm.JobAlarm#doAlarm(com.cc.job.xo.model.entity.JobInfo, com.cc.job.xo.model.entity.JobLog)}，
 * 并标注 {@link org.springframework.stereotype.Component}，Admin 启动时自动注册，任务失败时与邮件/Webhook/钉钉/企微/飞书等一并调用。
 */
package com.cc.job.admin.task.alarm;

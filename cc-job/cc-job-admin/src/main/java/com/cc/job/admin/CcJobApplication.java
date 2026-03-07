package com.cc.job.admin;

import com.cc.job.admin.config.properties.AlarmDingTalkProperties;
import com.cc.job.admin.config.properties.AlarmFeishuProperties;
import com.cc.job.admin.config.properties.AlarmRuleProperties;
import com.cc.job.admin.config.properties.AlarmWebhookProperties;
import com.cc.job.admin.config.properties.AlarmWeComProperties;
import com.cc.job.admin.config.properties.LifecycleWebhookProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类
 *
 * @author Ray
 * @since 0.0.1
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.cc.job.xo.mapper")
@EnableConfigurationProperties({AlarmWebhookProperties.class, AlarmDingTalkProperties.class, AlarmWeComProperties.class, AlarmFeishuProperties.class, AlarmRuleProperties.class, LifecycleWebhookProperties.class})
public class CcJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(CcJobApplication.class, args);
    }

}

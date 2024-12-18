package com.cc.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类
 *
 * @author Ray
 * @since 0.0.1
 */
@SpringBootApplication
//@ConfigurationPropertiesScan
//@EnableScheduling
public class CcJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(CcJobApplication.class, args);
    }

}

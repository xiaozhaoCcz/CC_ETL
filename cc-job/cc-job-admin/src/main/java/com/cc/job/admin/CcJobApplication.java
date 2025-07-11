package com.cc.job.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类
 *
 * @author Ray
 * @since 0.0.1
 */
@SpringBootApplication
@MapperScan("com.cc.job.xo.mapper")
public class CcJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(CcJobApplication.class, args);
    }

}

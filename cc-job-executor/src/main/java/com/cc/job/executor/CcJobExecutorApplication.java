package com.cc.job.executor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author xuxueli 2018-10-28 00:38:13
 */

@SpringBootApplication
@MapperScan("com.cc.job.xo.mapper")
public class CcJobExecutorApplication {

	public static void main(String[] args) {
        SpringApplication.run(CcJobExecutorApplication.class, args);
	}

}
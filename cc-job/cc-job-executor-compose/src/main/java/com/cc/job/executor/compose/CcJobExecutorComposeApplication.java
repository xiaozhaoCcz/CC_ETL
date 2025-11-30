package com.cc.job.executor.compose;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 任务组编排执行器启动类
 * 
 * <p>这是一个独立的执行器应用，专门负责任务组的编排执行。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>接收来自 Admin 的任务组执行请求</li>
 *   <li>进行任务依赖分析和拓扑排序</li>
 *   <li>并行/串行执行任务</li>
 *   <li>向 Admin 上报执行状态</li>
 * </ul>
 * 
 * @author xiaozhao
 */
@SpringBootApplication
public class CcJobExecutorComposeApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CcJobExecutorComposeApplication.java, args);
        System.out.println("========================================");
        System.out.println("CC-Job 任务组编排执行器启动成功！");
        System.out.println("========================================");
    }
}

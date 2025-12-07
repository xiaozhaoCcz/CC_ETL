package com.cc.job.executor.core.service.datax;

import com.xxl.job.core.context.XxlJobHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.FutureTask;

/**
 * DataX 进程运行器
 * 
 * <p>负责启动和管理 DataX 进程
 *
 * @author cc-job-team
 */
@Component
public class DataxProcessRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataxProcessRunner.class);
    
    /**
     * 运行 DataX 进程
     * 
     * @param command DataX命令数组
     * @return 退出码
     * @throws Exception 执行异常
     */
    public int runProcess(String[] command) throws Exception {
        logger.info("[DataxProcessRunner] 启动DataX进程");
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        
        // 启动标准输出监听线程
        Thread stdoutThread = createOutputThread(process.getInputStream(), "STDOUT");
        stdoutThread.start();
        
        // 启动错误输出监听线程
        Thread stderrThread = createOutputThread(process.getErrorStream(), "STDERR");
        stderrThread.start();
        
        // 等待进程结束
        int exitCode = process.waitFor();
        
        // 等待输出线程结束
        stdoutThread.join();
        stderrThread.join();
        
        logger.info("[DataxProcessRunner] DataX进程结束 - 退出码: {}", exitCode);
        return exitCode;
    }
    
    /**
     * 创建输出监听线程
     */
    private Thread createOutputThread(InputStream inputStream, String streamType) {
        FutureTask<Boolean> task = new FutureTask<>(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[DataxProcessRunner] {}: {}", streamType, line);
                    XxlJobHelper.log(line);
                }
            } catch (Exception e) {
                logger.error("[DataxProcessRunner] 读取{}失败", streamType, e);
            }
            return true;
        });
        return new Thread(task);
    }
}


package com.cc.job.executor.handler;

import com.cc.job.executor.utils.DataxUtils;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.FutureTask;

@Component
public class DataxHandler {

    private static Logger logger = LoggerFactory.getLogger(DataxHandler.class);

    @Value("${cc-job.executor.jsonpath}")
    private String jsonPath;

    @Value("${cc-job.pypath}")
    private String dataxPy;

    @XxlJob("runDataxHandler")
    public void runDataxHandler() {
        String json = XxlJobHelper.getJobParam();

        String temJsonFile = DataxUtils.generateTemJsonFile(jsonPath, json);
        String[] command = {"python", dataxPy, temJsonFile};
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        try {
            Process process = processBuilder.start();
            FutureTask<Boolean> futureTask = new FutureTask<>(() -> {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 处理每行输出
                        logger.info(line);
                        XxlJobHelper.log(line);
                    }
                } catch (Exception e) {

                }
                return true;
            });
            Thread startThread = new Thread(futureTask);
            startThread.start();

            FutureTask<Boolean> errorFutureTask = new FutureTask<>(() -> {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    logger.info(errorLine);
                    XxlJobHelper.log( errorLine);
                }
                return true;
            });
            Thread errorThread = new Thread(errorFutureTask);
            errorThread.start();

            int exitValue = process.waitFor();
            startThread.join();
            errorThread.join();

        } catch (Exception e) {
            XxlJobHelper.log("ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DataxUtils.deleteTemJsonFile(temJsonFile);
        }
    }
}

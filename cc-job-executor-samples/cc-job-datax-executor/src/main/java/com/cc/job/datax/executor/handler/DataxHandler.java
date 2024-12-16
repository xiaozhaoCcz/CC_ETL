package com.cc.job.datax.executor.handler;

import com.cc.job.datax.executor.utils.DataxUtils;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.FutureTask;

@Component
public class DataxHandler {

    @Value("${datax.executor.jsonpath}")
    private String jsonPath;

    @Value("${datax.pypath}")
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

package com.cc.job.datax.executor.handler;

import com.cc.job.datax.executor.utils.DataxUtils;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.FutureTask;

@Component
@AllArgsConstructor
public class DataxHandler {

    @Value("${datax.executor.jsonpath}")
    private String jsonPath;

    @Value("${datax.pypath}")
    private String dataxPy;

    @XxlJob("runDataxHandler")
    public void runDataxHandler(){
        String json = XxlJobHelper.getJobParam();

        String temJsonFile = DataxUtils.generateTemJsonFile(jsonPath,json);
        ProcessBuilder processBuilder = new ProcessBuilder("python", dataxPy, temJsonFile);
        processBuilder.inheritIO();
        try {
            Process process = processBuilder.start();
            FutureTask<Boolean> futureTask = new FutureTask<>(()->{
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line=reader.readLine())!=null){
                    XxlJobHelper.log(line);
                }
                return true;
            });
            Thread startThread= new Thread(futureTask);
            startThread.start();

            FutureTask<Boolean> errorfutureTask = new FutureTask<>(()->{
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                while ((line=reader.readLine())!=null){
                    XxlJobHelper.log(line);
                }
                return true;
            });
            Boolean b = futureTask.get();
            Thread errorThread= new Thread(errorfutureTask);
            errorThread.start();
            int exitValue = process.waitFor();
            errorThread.join();
        }catch (Exception e){
             e.printStackTrace();
        }finally {
            DataxUtils.deleteTemJsonFile(temJsonFile);
        }
    }
}

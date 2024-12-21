package com.cc.job.executor.handler;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.cc.job.executor.command.JdbcCommand;
import com.cc.job.executor.utils.DataxUtils;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.mapper.JobJdbcDatasourceMapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;

@Component
public class DataxHandler {

    private static Logger logger = LoggerFactory.getLogger(DataxHandler.class);

    @Resource
    private JobInfoMapper jobInfoMapper;

    @Resource
    private JobJdbcDatasourceMapper jobJdbcDatasourceMapper;

    @Value("${cc-job.executor.jsonpath}")
    private String jsonPath;

    @Value("${cc-job.pypath}")
    private String dataxPy;


    @XxlJob("runDataxHandler")
    public void runDataxHandler() {
        String json = XxlJobHelper.getJobParam();
        Long jobId = XxlJobHelper.getJobId();

        JobInfo jobInfo = jobInfoMapper.selectById(jobId);

        if (jobInfo==null){
            throw new BusinessException("任务不存在");
        }

        String temJsonFile = DataxUtils.generateTemJsonFile(jsonPath, json);
        ArrayList<String> cmdList = new ArrayList<>();
        cmdList.add("python");
        cmdList.add(dataxPy);
        cmdList.add(temJsonFile);

        if(jobInfo.getIncrType()==1){
            StringBuilder sb = new StringBuilder();
            sb.append("-p");
            sb.append("\"");
            if(jobInfo.getIncrColumnType()==0){
                sb.append("-D"+jobInfo.getIncrParam()+"="+jobInfo.getIncrId());
                sb.append("\"");
                cmdList.add(sb.toString().replaceAll(" ","\" \""));
            }else{
                LocalDateTime incrTime = jobInfo.getIncrTime();
                DateTimeFormatter dateTimeFormatter =DateTimeFormatter.ofPattern(jobInfo.getTimeFormat());
                String format = incrTime.format(dateTimeFormatter);
                sb.append("-D"+jobInfo.getIncrParam()+"='"+format+"'");
                sb.append("\"");
                cmdList.add(sb.toString());
            }
        }

        String[] command = cmdList.toArray(new String[0]);
//        String[] command = {"python", dataxPy, temJsonFile};

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

            //更改数据库字段
            if (exitValue == 0) {
                refreshJobInfo(jobInfo);
                XxlJobHelper.log("Datax job completed successfully.");
            } else {
                XxlJobHelper.log("Datax job failed with exit value: " + exitValue);
            }
        } catch (Exception e) {
            XxlJobHelper.log("ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DataxUtils.deleteTemJsonFile(temJsonFile);
        }
    }

    private void refreshJobInfo(JobInfo jobInfo) {
        JobJdbcDatasource jobJdbcDatasource = jobJdbcDatasourceMapper.selectById(jobInfo.getJdbcDatasourceId());
        String tableName = getTableName(jobInfo.getExecutorParam());
        String incrParam = jobInfo.getIncrParam();
        try (Connection con = getJdbcConnection(jobJdbcDatasource)) {
            if (jobInfo.getIncrColumnType() == 0) {
                updateIncrId(con, tableName, jobInfo);
            } else {
                updateIncrTime(con, tableName, incrParam, jobInfo);
            }
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    private Connection getJdbcConnection(JobJdbcDatasource jobJdbcDatasource) throws Exception {
        JdbcCommand jdbcCommand = new JdbcCommand(
                jobJdbcDatasource.getJdbcDriverClass(),
                jobJdbcDatasource.getJdbcUrl(),
                jobJdbcDatasource.getJdbcUsername(),
                jobJdbcDatasource.getJdbcPassword()
        );
        return jdbcCommand.getConnection();
    }

    private void updateIncrId(Connection con, String tableName, JobInfo jobInfo) throws Exception {
        String sql = "SELECT MAX(id) FROM " + tableName + " t";
        PreparedStatement ps=null;
        ResultSet rs =null;
        try {
            ps= con.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                long aLong = rs.getLong(1);
                if(aLong!=0){
                    jobInfo.setIncrId(aLong);
                    jobInfoMapper.updateById(jobInfo);
                }
                jobInfoMapper.updateById(jobInfo);
            }
        }finally {
            JdbcCommand.close(ps);
            JdbcCommand.close(rs);
            JdbcCommand.close(con);
        }
    }

    private void updateIncrTime(Connection con, String tableName, String incrParam, JobInfo jobInfo) throws Exception {
        String sql = "SELECT MAX(" + incrParam + ") FROM " + tableName + " t";
        PreparedStatement ps=null;
        ResultSet rs =null;
        try {
             ps = con.prepareStatement(sql);
             rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp timestamp = rs.getTimestamp(1);
                if(timestamp!=null){
                    jobInfo.setIncrTime(timestamp.toLocalDateTime());
                    jobInfoMapper.updateById(jobInfo);
                }
            }
        }finally {
            JdbcCommand.close(ps);
            JdbcCommand.close(rs);
            JdbcCommand.close(con);
        }
    }


    private String getTableName(String jsonStr){
        JSONObject jsonObject = new JSONObject(jsonStr);
        JSONObject job = jsonObject.getJSONObject("job");
        JSONArray content= job.getJSONArray("content");
        JSONObject writer = ((JSONObject) content.get(0)).getJSONObject("writer");
        JSONObject parameter = writer.getJSONObject("parameter");
        JSONArray connection = parameter.getJSONArray("connection");

        // 由于只有一个连接，我们取第一个元素
        JSONObject connectionObj = connection.getJSONObject(0);
        JSONArray tables = connectionObj.getJSONArray("table");
        // 构建连接参数
        return tables.getStr(0);
    }
}

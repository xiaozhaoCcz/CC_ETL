package com.cc.job.executor.handler;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.cc.job.executor.command.JdbcCommand;
import com.cc.job.executor.constant.DataxConstant;
import com.cc.job.executor.utils.DataxUtils;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.mapper.JobJdbcDatasourceMapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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

    /**
     * TODO 1.实现主键自增
     *      2.使用多表关联同步时，不能使用columns字段，只能使用querySql
     *      3.页面构建json需要优化
     */
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
        cmdList.add(DataxConstant.PYTHON);
        cmdList.add(dataxPy);
        cmdList.add(temJsonFile);

        if(jobInfo.getIncrType()==1){
            StringBuilder sb = new StringBuilder();
            sb.append(DataxConstant.PARAM);
            sb.append(DataxConstant.QUOTATION_MARK);
            if(jobInfo.getIncrColumnType()==0){
                sb.append(DataxConstant.DASH).append(jobInfo.getIncrParam()).append(DataxConstant.EQUALS).append(jobInfo.getIncrId());
                sb.append(DataxConstant.QUOTATION_MARK);
                cmdList.add(sb.toString().replaceAll(DataxConstant.SPACE,DataxConstant.MULTI_QUOTATION_MARK));
            }else{
                LocalDateTime incrTime = jobInfo.getIncrTime();
                DateTimeFormatter dateTimeFormatter =DateTimeFormatter.ofPattern(jobInfo.getTimeFormat());
                String format = incrTime.format(dateTimeFormatter);
                sb.append(DataxConstant.DASH).append(jobInfo.getIncrParam()).append(DataxConstant.EQUALS).append(DataxConstant.SINGLE_QUOTE).append(format).append(DataxConstant.SINGLE_QUOTE);
                sb.append(DataxConstant.QUOTATION_MARK);
                cmdList.add(sb.toString());
            }
        }

        String[] command = cmdList.toArray(new String[0]);
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
//        JobJdbcDatasource jobJdbcDatasource = jobJdbcDatasourceMapper.selectById(jobInfo.getJdbcDatasourceId());
        Map<String, String> conMap = getTableName(jobInfo.getExecutorParam());
        String jdbcUrl = conMap.get("jdbcUrl");
        String username = conMap.get("username");
        String password = conMap.get("password");
        String tableName = conMap.get("table");
        String name = conMap.get("name");
        JobJdbcDatasource jobJdbcDatasource = new JobJdbcDatasource();
        jobJdbcDatasource.setJdbcUrl(jdbcUrl);
        jobJdbcDatasource.setJdbcUsername(username);
        jobJdbcDatasource.setJdbcPassword(password);
        if("mysqlreader".equalsIgnoreCase(name)){
            jobJdbcDatasource.setJdbcDriverClass("com.mysql.cj.jdbc.Driver");
        }
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
        String sql = "SELECT MAX(id) FROM " + tableName + " t" + " WHERE t.id > " + jobInfo.getIncrId();
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
            }
        }finally {
            JdbcCommand.close(ps);
            JdbcCommand.close(rs);
            JdbcCommand.close(con);
        }
    }

    private void updateIncrTime(Connection con, String tableName, String columnName, JobInfo jobInfo) throws Exception {
        String sql = "SELECT MAX(" + columnName + ") FROM " + tableName + " t" + " WHERE t." + columnName + " > '" + jobInfo.getIncrTime().format(DateTimeFormatter.ofPattern(jobInfo.getTimeFormat())) + "'";
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


    private Map<String,String> getTableName(String jsonStr){
        Map<String,String> result =  new HashMap<>();
        JSONObject jsonObject = new JSONObject(jsonStr);
        JSONObject job = jsonObject.getJSONObject(DataxConstant.JOB);
        JSONArray content= job.getJSONArray(DataxConstant.CONTENT);
        JSONObject reader = ((JSONObject) content.get(0)).getJSONObject(DataxConstant.READER);
        JSONObject parameter = reader.getJSONObject(DataxConstant.PARAMETER);
        JSONArray connection = parameter.getJSONArray(DataxConstant.CONNECTION);
        // 由于只有一个连接，我们取第一个元素
        JSONObject connectionObj = connection.getJSONObject(0);
        JSONArray tables = connectionObj.getJSONArray(DataxConstant.TABLE);
        JSONArray jdbcUrl = connectionObj.getJSONArray("jdbcUrl");
        String username = parameter.getStr("username");
        String password = parameter.getStr("password");
        String name = reader.getStr("name");
        // TODO 需要支持querySql
        if(tables==null || tables.isEmpty()){

        }else{

        }
        result.put("table",tables.getStr(0));
        result.put("jdbcUrl",jdbcUrl.getStr(0));
        result.put("username",username);
        result.put("password",password);
        result.put("name",name);
        return result;
    }
}

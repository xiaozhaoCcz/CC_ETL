package com.cc.job.executor.handler;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cc.job.executor.command.JdbcCommand;
import com.cc.job.xo.constant.DataxConstant;
import com.cc.job.executor.utils.DataxUtils;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.mapper.JobJdbcDatasourceMapper;
import com.cc.job.xo.model.datax.DataxColumn;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.cc.job.xo.constant.DataxConstant.*;

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
            cmdList.add(PARAM);
            StringBuilder sb = new StringBuilder();
            JSONArray jsonArray = JSONUtil.parseArray(jobInfo.getIncrContent());
            List<DataxColumn> dataxColumns = jsonArray.toList(DataxColumn.class);
            for (DataxColumn dataxColumn : dataxColumns) {
                sb.append(DASH)
                        .append(dataxColumn.getColumnParam())
                        .append(EQUALS)
                        .append(SINGLE_QUOTE);
                if(dataxColumn.getColumnType()==1){
//                    Instant instant = Instant.ofEpochMilli(Long.parseLong(dataxColumn.getColumnValue()));
//                    // 将Instant转换为LocalDateTime
//                    LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
                    //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    // 解析字符串为LocalDateTime对象
//                    LocalDateTime dateTime = LocalDateTime.parse(dataxColumn.getColumnValue(), formatter);
//                    long timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    sb.append(Long.parseLong(dataxColumn.getColumnValue())/1000);;
                }else {
                    sb.append(dataxColumn.getColumnValue());
                }
                sb.append(SINGLE_QUOTE);
                sb.append(SPACE);
            }
            cmdList.add(sb.toString());
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
        Map<String, String> conMap = getTableName(jobInfo.getExecutorParam());
        String jdbcUrl = conMap.get("jdbcUrl");
        String username = conMap.get("username");
        String password = conMap.get("password");
        String tableName = conMap.get("table");
        String name = conMap.get("name");
        String querySql = conMap.get("querySql");
        JobJdbcDatasource jobJdbcDatasource = new JobJdbcDatasource();
        jobJdbcDatasource.setJdbcUrl(jdbcUrl);
        jobJdbcDatasource.setJdbcUsername(username);
        jobJdbcDatasource.setJdbcPassword(password);
        if("mysqlreader".equalsIgnoreCase(name)){
            jobJdbcDatasource.setJdbcDriverClass("com.mysql.cj.jdbc.Driver");
        }
        try (Connection con = getJdbcConnection(jobJdbcDatasource)) {
            updateJobInfo(con,querySql,tableName,jobInfo);
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
        JSONArray querySqls = connectionObj.getJSONArray(QUERY_SQL);
        JSONArray jdbcUrl = connectionObj.getJSONArray("jdbcUrl");
        String username = parameter.getStr("username");
        String password = parameter.getStr("password");
        String name = reader.getStr("name");
        result.put("table",tables.getStr(0));
        result.put("querySql",querySqls.getStr(0));
        result.put("jdbcUrl",jdbcUrl.getStr(0));
        result.put("username",username);
        result.put("password",password);
        result.put("name",name);
        return result;
    }


    private void updateJobInfo(Connection con,String querySql,String tableName,JobInfo jobInfo) {
        JSONArray jsonArray = JSONUtil.parseArray(jobInfo.getIncrContent());
        List<DataxColumn> columnList = jsonArray.toList(DataxColumn.class);
        if (StringUtils.isNotBlank(querySql)) {
            String pattern = "\\$\\{(.*?)}";
            Pattern compile = Pattern.compile(pattern);
            Matcher matcher = compile.matcher(querySql);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String group = matcher.group();
                String str = columnList.stream().filter(v -> group.equalsIgnoreCase("${" + v.getColumnParam() + "}")).map(DataxColumn::getColumnValue).findFirst().orElse("");
                matcher.appendReplacement(sb, str);
            }
            matcher.appendTail(sb);
            String sql = sb + " limit 1";
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = con.prepareStatement(sql);
                rs = ps.executeQuery();
                if (rs.next()) {
                    for (DataxColumn dataxColumn : columnList) {
                        Object val = rs.getObject(dataxColumn.getColumnKey());
                        //判断是否是时间类型
                        long time = isDate((String) val);
                        if (time > 0) {
                            dataxColumn.setColumnValue(String.valueOf(time));
                        } else {
                            dataxColumn.setColumnValue(val.toString());
                        }
                    }
                }
                String jsonStr = JSONUtil.toJsonStr(columnList);
                jobInfo.setIncrContent(jsonStr);
                jobInfoMapper.updateById(jobInfo);
            } catch (Exception e) {
                throw new BusinessException(e);
            } finally {
                JdbcCommand.close(ps);
                JdbcCommand.close(rs);
                JdbcCommand.close(con);
            }
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("select");
            sb.append(SPACE);
            for (DataxColumn dataxColumn : columnList) {
                sb.append("MAX").append("(").append(dataxColumn.getColumnKey()).append(")").append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(SPACE);
            sb.append("from");
            sb.append(SPACE);
            sb.append(tableName);
            sb.append(SPACE);
            sb.append("t");
            sb.append(SPACE);
            sb.append(WHERE);
            sb.append(SPACE);
            StringBuilder whereSql = new StringBuilder();
            for (DataxColumn dataxColumn : columnList) {
                whereSql.append("t.").append(dataxColumn.getColumnKey()).append(">").append("'").append(dataxColumn.getColumnValue()).append("'").append(SPACE).append(AND).append(SPACE);
            }
            whereSql.delete(whereSql.length() - 4, whereSql.length());
            sb.append(whereSql);
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = con.prepareStatement(sb.toString());
                rs = ps.executeQuery();
                if (rs.next()) {
                    int size = columnList.size();
                    for (int i = 0; i < size; i++) {
                        Object val = rs.getObject(i + 1);
                        //判断是否是时间类型
                        long time = isDate((String) val);
                        if (time > 0) {
                            columnList.get(i).setColumnValue(String.valueOf(time));
                        } else {
                            columnList.get(i).setColumnValue(val.toString());
                        }
                    }
                }
                String jsonStr = JSONUtil.toJsonStr(columnList);
                jobInfo.setIncrContent(jsonStr);
                jobInfoMapper.updateById(jobInfo);
            } catch (Exception e) {
                throw new BusinessException(e);
            } finally {
                JdbcCommand.close(ps);
                JdbcCommand.close(rs);
                JdbcCommand.close(con);
            }
        }
    }

    private long isDate(String time){
        long res = -1;
        final SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        final SimpleDateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd ");
        final SimpleDateFormat dateFormat4 = new SimpleDateFormat("yyyy/MM/dd ");
        try {
            LocalDateTime dateTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            res = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            try {
                java.util.Date date1 = dateFormat1.parse(time);
                res = date1.getTime();
            } catch (ParseException e1) {
                try {
                    java.util.Date date2 = dateFormat2.parse(time);
                    res = date2.getTime();
                } catch (ParseException ex) {
                    try {
                        java.util.Date date3 = dateFormat3.parse(time);
                        res = date3.getTime();
                    } catch (ParseException exception) {
                        try {
                            Date date4 = dateFormat4.parse(time);
                            res = date4.getTime();
                        }
                        catch (ParseException exx) {
                            try {
                                res = Long.parseLong(time);
                            }catch (NumberFormatException exxx ){
                            }
                        }
                    }
                }
            }
        }
        return res;
    }
}

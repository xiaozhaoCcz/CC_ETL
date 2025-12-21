package com.cc.job.executor.core.service.datax;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cc.job.executor.command.JdbcCommand;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.constant.DataxConstant;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.model.datax.DataxColumn;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.cc.job.xo.constant.DataxConstant.*;

/**
 * 增量数据刷新器
 * 
 * <p>负责刷新增量同步的标记数据
 *
 * @author cc-job-team
 */
@Component
public class IncrementalDataRefresher {
    
    private static final Logger logger = LoggerFactory.getLogger(IncrementalDataRefresher.class);
    
    private final JobInfoMapper jobInfoMapper;

    public IncrementalDataRefresher(JobInfoMapper jobInfoMapper) {
        this.jobInfoMapper = jobInfoMapper;
    }
    
    /** Oracle 时间格式映射 */
    private static final Map<String, String> ORACLE_TIME_FORMAT_MAP = Map.of(
            "YYYY-MM-DD hh:mm:ss", "YYYY-MM-DD HH24:MI:SS",
            "YYYY/MM/DD hh:mm:ss", "YYYY/MM/DD HH24:MI:SS",
            "YYYY-MM-DD", "YYYY-MM-DD",
            "YYYY/MM/DD", "YYYY/MM/DD"
    );
    
    /**
     * 刷新增量数据
     * 
     * @param jobInfo 任务信息
     */
    public void refreshIncrementalData(JobInfo jobInfo) {
        logger.info("[IncrementalRefresher] 开始刷新增量数据 - jobId: {}", jobInfo.getId());
        
        try {
            Map<String, String> configMap = parseDataxConfig(jobInfo.getExecutorParam());
            JobJdbcDatasource datasource = createDatasource(configMap);
            
            try (Connection connection = createConnection(datasource)) {
                updateIncrementalMarkers(connection, configMap, jobInfo, datasource);
            }
            
            logger.info("[IncrementalRefresher] 增量数据刷新完成 - jobId: {}", jobInfo.getId());
            
        } catch (Exception e) {
            logger.error("[IncrementalRefresher] 刷新增量数据失败 - jobId: {}", jobInfo.getId(), e);
            throw new BusinessException("刷新增量数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析 DataX 配置
     */
    private Map<String, String> parseDataxConfig(String jsonStr) {
        Map<String, String> result = new HashMap<>();
        
        JSONObject jsonObject = new JSONObject(jsonStr);
        JSONObject job = jsonObject.getJSONObject(DataxConstant.JOB);
        JSONArray content = job.getJSONArray(DataxConstant.CONTENT);
        JSONObject reader = ((JSONObject) content.get(0)).getJSONObject(DataxConstant.READER);
        JSONObject parameter = reader.getJSONObject(DataxConstant.PARAMETER);
        JSONArray connection = parameter.getJSONArray(DataxConstant.CONNECTION);
        JSONObject connectionObj = connection.getJSONObject(0);
        
        JSONArray tables = connectionObj.getJSONArray(DataxConstant.TABLE);
        JSONArray querySqls = connectionObj.getJSONArray(QUERY_SQL);
        JSONArray jdbcUrls = connectionObj.getJSONArray(JDBC_URL);
        
        result.put(TABLE, tables != null ? tables.getStr(0) : "");
        result.put(QUERY_SQL, querySqls != null ? querySqls.getStr(0) : "");
        result.put(JDBC_URL, jdbcUrls.getStr(0));
        result.put(USERNAME, parameter.getStr(USERNAME));
        result.put(PASSWORD, parameter.getStr(PASSWORD));
        result.put(NAME, reader.getStr(NAME));
        
        return result;
    }
    
    /**
     * 创建数据源对象
     */
    private JobJdbcDatasource createDatasource(Map<String, String> configMap) {
        JobJdbcDatasource datasource = new JobJdbcDatasource();
        datasource.setJdbcUrl(configMap.get(JDBC_URL));
        datasource.setJdbcUsername(configMap.get(USERNAME));
        datasource.setJdbcPassword(configMap.get(PASSWORD));
        
        String name = configMap.get(NAME);
        if (MYSQL_READER.equalsIgnoreCase(name)) {
            datasource.setJdbcDriverClass(MYSQL_DRIVER);
        } else if (ORACLE_READER.equalsIgnoreCase(name)) {
            datasource.setJdbcDriverClass(ORACLE_DRIVER);
        }
        
        return datasource;
    }
    
    /**
     * 创建数据库连接
     */
    private Connection createConnection(JobJdbcDatasource datasource) throws Exception {
        JdbcCommand jdbcCommand = new JdbcCommand(
                datasource.getJdbcDriverClass(),
                datasource.getJdbcUrl(),
                datasource.getJdbcUsername(),
                datasource.getJdbcPassword()
        );
        return jdbcCommand.getConnection();
    }
    
    /**
     * 更新增量标记
     */
    private void updateIncrementalMarkers(Connection connection, Map<String, String> configMap,
                                         JobInfo jobInfo, JobJdbcDatasource datasource) {
        String querySql = configMap.get(QUERY_SQL);
        String tableName = configMap.get(TABLE);
        
        JSONArray jsonArray = JSONUtil.parseArray(jobInfo.getIncrementContent());
        List<DataxColumn> columnList = jsonArray.toList(DataxColumn.class);
        
        if (StringUtils.isNotBlank(querySql)) {
            updateByQuerySql(connection, querySql, jobInfo, columnList);
        } else {
            updateByTableName(connection, tableName, jobInfo, columnList, datasource);
        }
    }
    
    /**
     * 通过查询SQL更新
     */
    private void updateByQuerySql(Connection connection, String querySql, 
                                 JobInfo jobInfo, List<DataxColumn> columnList) {
        String sql = replaceVariables(querySql, columnList);
        String countSql = "SELECT COUNT(1) FROM (" + sql + ") t";
        
        try (PreparedStatement ps = connection.prepareStatement(countSql);
             ResultSet rs = ps.executeQuery()) {
            
            long count = 0;
            if (rs.next()) {
                count = rs.getLong(1);
                if (count == 0) {
                    return;
                }
            }
            
            queryAndUpdateLastRow(connection, sql, count, columnList, jobInfo);
            
        } catch (Exception e) {
            throw new BusinessException("查询SQL执行失败", e);
        }
    }
    
    /**
     * 通过表名更新
     */
    private void updateByTableName(Connection connection, String tableName, JobInfo jobInfo,
                                  List<DataxColumn> columnList, JobJdbcDatasource datasource) {
        String sql = buildMaxValueQuery(tableName, columnList, datasource);
        
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                updateColumnValues(rs, columnList);
                saveIncrementalData(jobInfo, columnList);
            }
            
        } catch (Exception e) {
            throw new BusinessException("表名查询执行失败", e);
        }
    }
    
    /**
     * 替换SQL中的变量
     */
    private String replaceVariables(String querySql, List<DataxColumn> columnList) {
        Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
        Matcher matcher = pattern.matcher(querySql);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            String variable = matcher.group();
            String value = columnList.stream()
                    .filter(c -> variable.equalsIgnoreCase("${" + c.getColumnParam() + "}"))
                    .map(DataxColumn::getColumnValue)
                    .findFirst()
                    .orElse("");
            matcher.appendReplacement(sb, value);
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 构建最大值查询SQL
     */
    private String buildMaxValueQuery(String tableName, List<DataxColumn> columnList,
                                     JobJdbcDatasource datasource) {
        StringBuilder sql = new StringBuilder("SELECT ");
        
        // 添加 MAX 字段
        for (DataxColumn column : columnList) {
            sql.append("MAX(").append(column.getColumnKey()).append("),");
        }
        sql.deleteCharAt(sql.length() - 1);
        
        sql.append(" FROM ").append(tableName).append(" t WHERE ");
        
        // 添加 WHERE 条件
        buildWhereClause(sql, columnList, datasource);
        
        return sql.toString();
    }
    
    /**
     * 构建 WHERE 子句
     */
    private void buildWhereClause(StringBuilder sql, List<DataxColumn> columnList,
                                 JobJdbcDatasource datasource) {
        for (int i = 0; i < columnList.size(); i++) {
            DataxColumn column = columnList.get(i);
            
            if (i > 0) {
                sql.append(" AND ");
            }
            
            sql.append("t.").append(column.getColumnKey()).append(" > ");
            
            if (column.getColumnType() == 1) {
                appendTimeCondition(sql, column, datasource);
            } else {
                sql.append("'").append(column.getColumnValue()).append("'");
            }
        }
    }
    
    /**
     * 添加时间条件
     */
    private void appendTimeCondition(StringBuilder sql, DataxColumn column, 
                                    JobJdbcDatasource datasource) {
        long seconds = Long.parseLong(column.getColumnValue()) / 1000;
        String driver = datasource.getJdbcDriverClass();
        
        if (MYSQL_DRIVER.equalsIgnoreCase(driver)) {
            sql.append("FROM_UNIXTIME(").append(seconds)
                    .append(", '").append(column.getColumnTimeFormat()).append("')");
        } else if (ORACLE_DRIVER.equalsIgnoreCase(driver)) {
            sql.append("TO_DATE(TO_CHAR(").append(seconds).append(" / 86400 + ")
                    .append("TO_DATE('1970-01-01', 'YYYY-MM-DD')), '")
                    .append(getOracleTimeFormat(column.getColumnTimeFormat()))
                    .append("')");
        }
    }
    
    /**
     * 查询并更新最后一行
     */
    private void queryAndUpdateLastRow(Connection connection, String sql, long count,
                                      List<DataxColumn> columnList, JobInfo jobInfo) 
            throws SQLException {
        String limitSql = sql + " LIMIT " + (count - 1) + ", 1";
        
        try (PreparedStatement ps = connection.prepareStatement(limitSql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                updateColumnValues(rs, columnList);
                saveIncrementalData(jobInfo, columnList);
            }
        }
    }
    
    /**
     * 更新列值
     */
    private void updateColumnValues(ResultSet rs, List<DataxColumn> columnList) throws SQLException {
        for (int i = 0; i < columnList.size(); i++) {
            Object value = rs.getObject(i + 1);
            if (value != null) {
                long timestamp = parseTimestamp(value.toString());
                if (timestamp > 0) {
                    columnList.get(i).setColumnValue(String.valueOf(timestamp));
                } else {
                    columnList.get(i).setColumnValue(value.toString());
                }
            }
        }
    }
    
    /**
     * 保存增量数据
     */
    private void saveIncrementalData(JobInfo jobInfo, List<DataxColumn> columnList) {
        String jsonStr = JSONUtil.toJsonStr(columnList);
        jobInfo.setIncrementContent(jsonStr);
        jobInfoMapper.updateById(jobInfo);
        logger.info("[IncrementalRefresher] 增量标记已更新 - jobId: {}", jobInfo.getId());
    }
    
    /**
     * 解析时间戳
     */
    private long parseTimestamp(String timeStr) {
        // 尝试多种日期格式
        String[] formats = {
                ExecutorConstants.DateFormat.DATETIME_1,
                ExecutorConstants.DateFormat.DATETIME_2,
                ExecutorConstants.DateFormat.DATE_1,
                ExecutorConstants.DateFormat.DATE_2
        };
        
        // 先尝试 ISO 格式
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }
        
        // 尝试其他格式
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                java.util.Date date = sdf.parse(timeStr);
                return date.getTime();
            } catch (ParseException ignored) {
            }
        }
        
        // 尝试直接解析为long
        try {
            return Long.parseLong(timeStr);
        } catch (NumberFormatException ignored) {
        }
        
        return -1;
    }
    
    /**
     * 获取 Oracle 时间格式
     */
    private String getOracleTimeFormat(String format) {
        return ORACLE_TIME_FORMAT_MAP.getOrDefault(format, format);
    }
}


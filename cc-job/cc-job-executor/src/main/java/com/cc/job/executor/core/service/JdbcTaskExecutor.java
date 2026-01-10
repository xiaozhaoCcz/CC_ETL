package com.cc.job.executor.core.service;

import com.cc.job.executor.command.JdbcCommand;
import com.cc.job.executor.enums.SqlEnum;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.cc.job.executor.infrastructure.exception.TaskExecutionException;
import com.cc.job.xo.model.result.NodeResult;
import com.cc.job.xo.model.result.SqlResult;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.xxl.job.core.context.XxlJobHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC 任务执行器
 * 
 * <p>负责执行 JDBC SQL 任务
 *
 * @author cc-job-team
 */
@Component
public class JdbcTaskExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(JdbcTaskExecutor.class);
    
    /**
     * 执行 JDBC 任务
     * 
     * @param jobInfo 任务信息
     * @param datasource 数据源信息
     */
    public void execute(JobInfo jobInfo, JobJdbcDatasource datasource) {
        logger.info("[JdbcTaskExecutor] 开始执行JDBC任务 - jobId: {}", jobInfo.getId());
        
        long startTime = System.currentTimeMillis();
        String sql = validateAndPrepareSql(jobInfo);
        NodeResult nodeResult = null;
        
        try (Connection connection = createConnection(datasource)) {
            nodeResult = executeSql(jobInfo.getId(), sql, connection, startTime);
            logger.info("[JdbcTaskExecutor] JDBC任务执行完成 - jobId: {}", jobInfo.getId());
            
            // 设置执行结果
            if (nodeResult != null) {
                XxlJobHelper.executeResult(nodeResult);
            }
        } catch (Exception e) {
            logger.error("[JdbcTaskExecutor] JDBC任务执行失败 - jobId: {}", jobInfo.getId(), e);
            long duration = System.currentTimeMillis() - startTime;
            NodeResult errorResult = NodeResult.failure("JDBC任务执行失败: " + e.getMessage(), duration);
            XxlJobHelper.executeResult(errorResult);
            throw new TaskExecutionException(jobInfo.getId(), "JDBC任务执行失败", e);
        }
    }
    
    /**
     * 验证并准备 SQL
     */
    private String validateAndPrepareSql(JobInfo jobInfo) {
        String sql = jobInfo.getExecutorParam();
        
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException(ExecutorConstants.ErrorMessage.SQL_EMPTY);
        }
        
        sql = sql.trim();
        
        // 移除末尾的分号
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        
        XxlJobHelper.log("执行SQL: {}", sql);
        return sql;
    }
    
    /**
     * 创建数据库连接
     */
    private Connection createConnection(JobJdbcDatasource datasource) {
        JdbcCommand jdbcCommand = new JdbcCommand(
                datasource.getJdbcDriverClass(),
                datasource.getJdbcUrl(),
                datasource.getJdbcUsername(),
                datasource.getJdbcPassword()
        );
        return jdbcCommand.getConnection();
    }
    
    /**
     * 执行 SQL
     */
    private NodeResult executeSql(Long jobId, String sql, Connection connection, long startTime) throws SQLException {
        String sqlType = extractSqlType(sql);
        long executionStartTime = System.currentTimeMillis();
        
        logger.debug("[JdbcTaskExecutor] SQL类型: {} - jobId: {}", sqlType, jobId);
        
        SqlResult sqlResult = new SqlResult();
        sqlResult.setSqlType(sqlType);
        sqlResult.setExecutedSql(sql);
        
        NodeResult nodeResult;
        
        switch (sqlType.toUpperCase()) {
            case "SELECT":
                nodeResult = executeQuery(jobId, sql, connection, sqlResult, executionStartTime, startTime);
                break;
            case "INSERT":
            case "UPDATE":
            case "DELETE":
                nodeResult = executeUpdate(jobId, sql, connection, sqlResult, executionStartTime, startTime);
                break;
            case "CALL":
                nodeResult = executeCall(jobId, sql, connection, sqlResult, executionStartTime, startTime);
                break;
            default:
                throw new IllegalArgumentException("不支持的SQL类型: " + sqlType);
        }
        
        return nodeResult;
    }
    
    /**
     * 提取 SQL 类型
     */
    private String extractSqlType(String sql) {
        int firstSpaceIndex = sql.indexOf(" ");
        if (firstSpaceIndex == -1) {
            throw new IllegalArgumentException(ExecutorConstants.ErrorMessage.INVALID_SQL);
        }
        return sql.substring(0, firstSpaceIndex).trim();
    }
    
    /**
     * 执行查询
     */
    private NodeResult executeQuery(Long jobId, String sql, Connection connection, 
                                     SqlResult sqlResult, long executionStartTime, long totalStartTime) throws SQLException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        List<String> columnTypes = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            // 获取列信息
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
                columnTypes.add(metaData.getColumnTypeName(i));
            }
            
            // 获取数据
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                dataList.add(row);
            }
        }
        
        long executionTime = System.currentTimeMillis() - executionStartTime;
        long totalDuration = System.currentTimeMillis() - totalStartTime;
        
        sqlResult.setData(dataList);
        sqlResult.setCount(dataList.size());
        sqlResult.setColumns(columns);
        sqlResult.setColumnTypes(columnTypes);
        sqlResult.setExecutionTime(executionTime);
        
        XxlJobHelper.log("查询完成 - 结果数量: {}, 执行时间: {}ms", dataList.size(), executionTime);
        logger.info("[JdbcTaskExecutor] 查询完成 - jobId: {}, 结果数: {}, 执行时间: {}ms", 
                jobId, dataList.size(), executionTime);
        
        NodeResult nodeResult = NodeResult.success("查询成功，返回 " + dataList.size() + " 条数据", totalDuration);
        nodeResult.setSqlResult(sqlResult);
        nodeResult.setData(dataList);
        
        return nodeResult;
    }
    
    /**
     * 执行更新
     */
    private NodeResult executeUpdate(Long jobId, String sql, Connection connection, 
                                      SqlResult sqlResult, long executionStartTime, long totalStartTime) throws SQLException {
        int affectedRows;
        long executionTime;
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            long start = System.currentTimeMillis();
            affectedRows = ps.executeUpdate();
            executionTime = System.currentTimeMillis() - start;
        }
        
        long totalDuration = System.currentTimeMillis() - totalStartTime;
        
        sqlResult.setAffectedRows(affectedRows);
        sqlResult.setExecutionTime(executionTime);
        
        XxlJobHelper.log("更新完成 - 影响行数: {}, 执行时间: {}ms", affectedRows, executionTime);
        logger.info("[JdbcTaskExecutor] 更新完成 - jobId: {}, 影响行数: {}, 执行时间: {}ms", 
                jobId, affectedRows, executionTime);
        
        NodeResult nodeResult = NodeResult.success("更新成功，影响 " + affectedRows + " 行", totalDuration);
        nodeResult.setSqlResult(sqlResult);
        nodeResult.setData(affectedRows);
        
        return nodeResult;
    }
    
    /**
     * 执行存储过程
     */
    private NodeResult executeCall(Long jobId, String sql, Connection connection, 
                                    SqlResult sqlResult, long executionStartTime, long totalStartTime) throws SQLException {
        String callSql = "{" + sql + "}";
        long executionTime;
        
        try (CallableStatement cs = connection.prepareCall(callSql)) {
            long start = System.currentTimeMillis();
            boolean hasResult = cs.execute();
            executionTime = System.currentTimeMillis() - start;
            
            // 存储过程可能返回结果集
            if (hasResult) {
                try (ResultSet rs = cs.getResultSet()) {
                    List<Map<String, Object>> dataList = new ArrayList<>();
                    if (rs != null) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        List<String> columns = new ArrayList<>();
                        List<String> columnTypes = new ArrayList<>();
                        
                        for (int i = 1; i <= columnCount; i++) {
                            columns.add(metaData.getColumnName(i));
                            columnTypes.add(metaData.getColumnTypeName(i));
                        }
                        
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                String columnName = metaData.getColumnName(i);
                                Object value = rs.getObject(i);
                                row.put(columnName, value);
                            }
                            dataList.add(row);
                        }
                        
                        sqlResult.setData(dataList);
                        sqlResult.setCount(dataList.size());
                        sqlResult.setColumns(columns);
                        sqlResult.setColumnTypes(columnTypes);
                    }
                }
            }
        }
        
        long totalDuration = System.currentTimeMillis() - totalStartTime;
        
        sqlResult.setExecutionTime(executionTime);
        
        XxlJobHelper.log("存储过程执行完成 - 执行时间: {}ms", executionTime);
        logger.info("[JdbcTaskExecutor] 存储过程执行完成 - jobId: {}, 执行时间: {}ms", jobId, executionTime);
        
        NodeResult nodeResult = NodeResult.success("存储过程执行成功", totalDuration);
        nodeResult.setSqlResult(sqlResult);
        
        return nodeResult;
    }
}


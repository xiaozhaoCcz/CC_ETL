package com.cc.job.executor.core.service;

import com.cc.job.executor.command.JdbcCommand;
import com.cc.job.executor.enums.SqlEnum;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.cc.job.executor.infrastructure.exception.TaskExecutionException;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.xxl.job.core.context.XxlJobHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;

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
        
        String sql = validateAndPrepareSql(jobInfo);
        
        try (Connection connection = createConnection(datasource)) {
            executeSql(jobInfo.getId(), sql, connection);
            logger.info("[JdbcTaskExecutor] JDBC任务执行完成 - jobId: {}", jobInfo.getId());
        } catch (Exception e) {
            logger.error("[JdbcTaskExecutor] JDBC任务执行失败 - jobId: {}", jobInfo.getId(), e);
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
    private void executeSql(Long jobId, String sql, Connection connection) throws SQLException {
        String sqlType = extractSqlType(sql);
        
        logger.debug("[JdbcTaskExecutor] SQL类型: {} - jobId: {}", sqlType, jobId);
        
        switch (sqlType.toUpperCase()) {
            case "SELECT":
                executeQuery(jobId, sql, connection);
                break;
            case "INSERT":
            case "UPDATE":
            case "DELETE":
                executeUpdate(jobId, sql, connection);
                break;
            case "CALL":
                executeCall(jobId, sql, connection);
                break;
            default:
                throw new IllegalArgumentException("不支持的SQL类型: " + sqlType);
        }
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
    private void executeQuery(Long jobId, String sql, Connection connection) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") t";
        
        XxlJobHelper.log("执行统计SQL: {}", countSql);
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            
            if (rs.next()) {
                int count = rs.getInt(1);
                XxlJobHelper.log("查询结果数量: {}", count);
                logger.info("[JdbcTaskExecutor] 查询完成 - jobId: {}, 结果数: {}", jobId, count);
            }
        }
    }
    
    /**
     * 执行更新
     */
    private void executeUpdate(Long jobId, String sql, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int affectedRows = ps.executeUpdate();
            XxlJobHelper.log("影响行数: {}", affectedRows);
            logger.info("[JdbcTaskExecutor] 更新完成 - jobId: {}, 影响行数: {}", jobId, affectedRows);
        }
    }
    
    /**
     * 执行存储过程
     */
    private void executeCall(Long jobId, String sql, Connection connection) throws SQLException {
        String callSql = "{" + sql + "}";
        
        try (CallableStatement cs = connection.prepareCall(callSql)) {
            cs.execute();
            XxlJobHelper.log("存储过程执行完成");
            logger.info("[JdbcTaskExecutor] 存储过程执行完成 - jobId: {}", jobId);
        }
    }
}


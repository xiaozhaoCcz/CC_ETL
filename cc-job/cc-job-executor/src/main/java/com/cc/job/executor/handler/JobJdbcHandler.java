package com.cc.job.executor.handler;

import com.cc.job.executor.command.JdbcCommand;
import com.cc.job.executor.enums.SqlEnum;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.mapper.JobJdbcDatasourceMapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.Optional;

@Component
@AllArgsConstructor
public class JobJdbcHandler {

    final JobJdbcDatasourceMapper jobJdbcDatasourceMapper;

    final JobInfoMapper jobInfoMapper;

    @XxlJob("runJobJdbcXxlJob")
    public void runJobJdbcXxlJob() {
        long jobId = XxlJobHelper.getJobId();
        JobInfo jobInfo = Optional.ofNullable(jobInfoMapper.selectById(jobId))
                .orElseThrow(() -> new BusinessException("jobInfo not found for jobId: " + jobId));

        JobJdbcDatasource jobJdbcDatasource = Optional.ofNullable(jobJdbcDatasourceMapper.selectById(jobInfo.getJdbcDatasourceId()))
                .orElseThrow(() -> new BusinessException("jobJdbcDatasource not found"));

        JdbcCommand jdbcCommand = new JdbcCommand(jobJdbcDatasource.getJdbcDriverClass(), jobJdbcDatasource.getJdbcUrl(), jobJdbcDatasource.getJdbcUsername(), jobJdbcDatasource.getJdbcPassword());
        Connection con = jdbcCommand.getConnection();
        String sql = jobInfo.getExecutorParam().trim();
        if (!StringUtils.hasText(sql)) {
            throw new RuntimeException("sql is empty");
        }
        
        // 移除 SQL 末尾的分号（如果存在），避免语法错误
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        
        XxlJobHelper.log("execute sql: {} ", sql);
        PreparedStatement ps = null;
        ResultSet rs = null;
        CallableStatement cs = null;
        Statement stmt = null;
        try {
            // 提取 SQL 类型（第一个单词）
            int firstSpaceIndex = sql.indexOf(" ");
            if (firstSpaceIndex == -1) {
                throw new RuntimeException("Invalid SQL syntax: " + sql);
            }
            String type = sql.substring(0, firstSpaceIndex);
            
            if (type.equalsIgnoreCase(SqlEnum.DELETE.getName()) || 
                type.equalsIgnoreCase(SqlEnum.INSERT.getName()) || 
                type.equalsIgnoreCase(SqlEnum.UPDATE.getName())) {
                // DELETE/INSERT/UPDATE: 使用 PreparedStatement
            ps = con.prepareStatement(sql);
                int i = ps.executeUpdate();
                XxlJobHelper.log("result {} executeUpdate {}", jobId, i > 0 ? "success" : "fail");
            } else if (type.equalsIgnoreCase(SqlEnum.CALL.getName())) {
                // CALL: 使用 CallableStatement
                cs = con.prepareCall("{" + sql + "}");
                cs.execute();
                XxlJobHelper.log("execute call jobId:{}", jobId);
            } else {
                // SELECT: 使用 Statement 执行 count 查询（因为需要动态拼接 SQL）
                stmt = con.createStatement();
                String countSql = "select count(*) from (" + sql + ") t";
                XxlJobHelper.log("execute count sql: {} ", countSql);
                rs = stmt.executeQuery(countSql);
                int count = 0;
                while (rs.next()) {
                    count = rs.getInt(1);
                }
                // 获取数量
                XxlJobHelper.log("jobId:{},data row: {} ", jobId, count);
            }
        } catch (SQLException e) {
            XxlJobHelper.log("SQL execution error: {}", e.getMessage());
            throw new RuntimeException("SQL execution failed: " + e.getMessage(), e);
        } finally {
            JdbcCommand.close(rs);
            JdbcCommand.close(cs);
            JdbcCommand.close(ps);
            JdbcCommand.close(stmt);
            JdbcCommand.close(con);
        }
    }
}


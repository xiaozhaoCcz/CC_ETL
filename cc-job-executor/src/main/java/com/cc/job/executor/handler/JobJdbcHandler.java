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
        System.out.println("runJobJdbcXxlJob");
        JobInfo jobInfo = Optional.ofNullable(jobInfoMapper.selectById(jobId))
                .orElseThrow(() -> new BusinessException("TaskInfo not found for jobId: " + jobId));

        JobJdbcDatasource jobJdbcDatasource = Optional.ofNullable(jobJdbcDatasourceMapper.selectById(jobInfo.getJdbcDatasourceId()))
                .orElseThrow(() -> new BusinessException("TaskInfo not found for jobId: " + jobId));

        JdbcCommand jdbcCommand = new JdbcCommand(jobJdbcDatasource.getJdbcDriverClass(), jobJdbcDatasource.getJdbcUrl(), jobJdbcDatasource.getJdbcUsername(), jobJdbcDatasource.getJdbcPassword());
        Connection con = jdbcCommand.getConnection();
        String sql = jobInfo.getExecutorParam().trim();
        if (!StringUtils.hasText(sql)) {
            throw new RuntimeException("sql is empty");
        }
        XxlJobHelper.log("execute sql: {} ", sql);
        PreparedStatement ps = null;
        ResultSet rs = null;
        CallableStatement cs = null;
        try {
            ps = con.prepareStatement(sql);
            String type = sql.substring(0, sql.indexOf(" "));
            if (type.equalsIgnoreCase(SqlEnum.DELETE.getName()) || type.equalsIgnoreCase(SqlEnum.INSERT.getName()) || type.equalsIgnoreCase(SqlEnum.UPDATE.getName())) {
                int i = ps.executeUpdate();
                XxlJobHelper.log("result {} executeUpdate {}", jobId, i > 0 ? "success" : "fail");
            } else if (type.equalsIgnoreCase(SqlEnum.CALL.getName())) {
                cs = con.prepareCall("{" + sql + "}");
                cs.execute();
                XxlJobHelper.log("execute call jobId:{}", jobId);
            } else {
                rs = ps.executeQuery("select count(*) from (" + sql + ") t");
                int count = 0;
                while (rs.next()) {
                    count = rs.getInt(1);
                }
                // 获取数量
                XxlJobHelper.log("jobId:{},data row: {} ", jobId, count);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            JdbcCommand.close(rs);
            JdbcCommand.close(cs);
            JdbcCommand.close(ps);
        }
    }
}


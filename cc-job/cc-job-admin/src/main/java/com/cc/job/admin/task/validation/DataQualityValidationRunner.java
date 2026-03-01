package com.cc.job.admin.task.validation;

import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.cc.job.xo.model.entity.JobValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 同步后数据质量校验：在指定数据源上执行校验 SQL，取第一行第一列作为行数并与期望值比较。
 */
public final class DataQualityValidationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataQualityValidationRunner.class);

    private DataQualityValidationRunner() {}

    /**
     * 执行校验：在 datasource 上执行 validation.getValidationSql()，取第一列数值；
     * 若 actualCount < expectedMinRows 返回失败。
     *
     * @return 校验通过返回 null；不通过返回错误信息
     */
    public static String runValidation(JobValidation validation, JobJdbcDatasource datasource) {
        if (validation == null || datasource == null || validation.getValidationSql() == null
                || validation.getValidationSql().isBlank()) {
            return null;
        }
        Integer expected = validation.getExpectedMinRows() != null ? validation.getExpectedMinRows() : 0;
        String url = datasource.getJdbcUrl();
        String user = datasource.getJdbcUsername();
        String password = datasource.getJdbcPassword() != null ? datasource.getJdbcPassword() : "";
        String sql = validation.getValidationSql().trim();
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) {
                return "校验SQL未返回行，视为行数0，小于期望 " + expected;
            }
            long actual = rs.getLong(1);
            if (actual < expected) {
                return "校验行数 " + actual + " 小于期望最小行数 " + expected;
            }
            log.info("[DataQuality] jobId={} 校验通过: actual={}, expectedMin={}", validation.getJobId(), actual, expected);
            return null;
        } catch (Exception e) {
            log.error("[DataQuality] jobId={} 校验执行异常", validation.getJobId(), e);
            return "校验执行异常: " + e.getMessage();
        }
    }
}

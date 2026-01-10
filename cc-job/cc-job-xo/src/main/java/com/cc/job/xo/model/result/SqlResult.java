package com.cc.job.xo.model.result;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SQL节点执行结果
 * 
 * <p>包含SQL查询结果、统计信息、元信息和执行信息
 *
 * @author cc-job-team
 */
public class SqlResult {
    
    /** 查询结果数据（SELECT） */
    private List<Map<String, Object>> data;
    
    /** 数据条数（SELECT查询） */
    private Integer count;
    
    /** 受影响行数（UPDATE/INSERT/DELETE） */
    private Integer affectedRows;
    
    /** SQL类型：SELECT, UPDATE, INSERT, DELETE, CALL */
    private String sqlType;
    
    /** 列名列表（SELECT查询） */
    private List<String> columns;
    
    /** 列类型列表（SELECT查询） */
    private List<String> columnTypes;
    
    /** 实际执行的SQL（参数替换后） */
    private String executedSql;
    
    /** SQL执行时间（毫秒） */
    private Long executionTime;

    public SqlResult() {
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
        // 自动设置count
        if (data != null) {
            this.count = data.size();
        }
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(Integer affectedRows) {
        this.affectedRows = affectedRows;
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<String> getColumnTypes() {
        return columnTypes;
    }

    public void setColumnTypes(List<String> columnTypes) {
        this.columnTypes = columnTypes;
    }

    public String getExecutedSql() {
        return executedSql;
    }

    public void setExecutedSql(String executedSql) {
        this.executedSql = executedSql;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlResult sqlResult = (SqlResult) o;
        return Objects.equals(count, sqlResult.count) &&
                Objects.equals(affectedRows, sqlResult.affectedRows) &&
                Objects.equals(sqlType, sqlResult.sqlType) &&
                Objects.equals(executionTime, sqlResult.executionTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, affectedRows, sqlType, executionTime);
    }

    @Override
    public String toString() {
        return "SqlResult{" +
                "data=" + (data != null ? data.size() + " rows" : "null") +
                ", count=" + count +
                ", affectedRows=" + affectedRows +
                ", sqlType='" + sqlType + '\'' +
                ", columns=" + columns +
                ", executionTime=" + executionTime +
                '}';
    }
}

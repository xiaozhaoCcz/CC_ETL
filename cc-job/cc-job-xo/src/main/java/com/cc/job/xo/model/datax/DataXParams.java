package com.cc.job.xo.model.datax;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class DataXParams implements Serializable {

    private String name;

    private List<String> columns;

    private String sourceType;

    private String username;

    private String password;

    private String dbName;

    private String tableName;

    private String jdbcUrl;

    private String ip;

    private Integer port;

    private String querySql;

    private String writeMode;

    private Integer type;

    // 0全量，1增量
    private Integer incrementType;

    // 增量字段，json
    private String incrementContent;

    // 自定义增量参数模板，如 -DstartId=%s -DendId=%s
    private String incrementParamTemplate;

    private String schemaName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getQuerySql() {
        return querySql;
    }

    public void setQuerySql(String querySql) {
        this.querySql = querySql;
    }

    public String getWriteMode() {
        return writeMode;
    }

    public void setWriteMode(String writeMode) {
        this.writeMode = writeMode;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getIncrementType() {
        return incrementType;
    }

    public void setIncrementType(Integer incrementType) {
        this.incrementType = incrementType;
    }

    public String getIncrementContent() {
        return incrementContent;
    }

    public void setIncrementContent(String incrementContent) {
        this.incrementContent = incrementContent;
    }

    public String getIncrementParamTemplate() {
        return incrementParamTemplate;
    }

    public void setIncrementParamTemplate(String incrementParamTemplate) {
        this.incrementParamTemplate = incrementParamTemplate;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataXParams that = (DataXParams) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(columns, that.columns) &&
                Objects.equals(sourceType, that.sourceType) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(dbName, that.dbName) &&
                Objects.equals(tableName, that.tableName) &&
                Objects.equals(jdbcUrl, that.jdbcUrl) &&
                Objects.equals(ip, that.ip) &&
                Objects.equals(port, that.port) &&
                Objects.equals(querySql, that.querySql) &&
                Objects.equals(writeMode, that.writeMode) &&
                Objects.equals(type, that.type) &&
                Objects.equals(incrementType, that.incrementType) &&
                Objects.equals(incrementContent, that.incrementContent) &&
                Objects.equals(incrementParamTemplate, that.incrementParamTemplate) &&
                Objects.equals(schemaName, that.schemaName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columns, sourceType, username, password, dbName, tableName, jdbcUrl, ip, port, querySql, writeMode, type, incrementType, incrementContent, incrementParamTemplate, schemaName);
    }

    @Override
    public String toString() {
        return "DataXParams{" +
                "name='" + name + '\'' +
                ", columns=" + columns +
                ", sourceType='" + sourceType + '\'' +
                ", username='" + username + '\'' +
                ", dbName='" + dbName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", jdbcUrl='" + jdbcUrl + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", querySql='" + querySql + '\'' +
                ", writeMode='" + writeMode + '\'' +
                ", type=" + type +
                ", incrementType=" + incrementType +
                ", incrementContent='" + incrementContent + '\'' +
                ", incrementParamTemplate='" + incrementParamTemplate + '\'' +
                ", schemaName='" + schemaName + '\'' +
                '}';
    }
}

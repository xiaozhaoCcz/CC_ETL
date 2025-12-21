package com.cc.job.xo.model.datax;


import java.io.Serializable;

public class DataxColumn implements Serializable {

    private Integer id;

    private String columnKey;

    private String columnValue;

    private String columnParam;

    private String columnTimeFormat;

    private Integer columnType;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getColumnKey() {
        return columnKey;
    }

    public void setColumnKey(String columnKey) {
        this.columnKey = columnKey;
    }

    public String getColumnValue() {
        return columnValue;
    }

    public void setColumnValue(String columnValue) {
        this.columnValue = columnValue;
    }

    public String getColumnParam() {
        return columnParam;
    }

    public void setColumnParam(String columnParam) {
        this.columnParam = columnParam;
    }

    public String getColumnTimeFormat() {
        return columnTimeFormat;
    }

    public void setColumnTimeFormat(String columnTimeFormat) {
        this.columnTimeFormat = columnTimeFormat;
    }

    public Integer getColumnType() {
        return columnType;
    }

    public void setColumnType(Integer columnType) {
        this.columnType = columnType;
    }
}

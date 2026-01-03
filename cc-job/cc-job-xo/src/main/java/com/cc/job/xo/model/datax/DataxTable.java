package com.cc.job.xo.model.datax;


/**
 * @author astro
 * @date 2025/12/29 15:25
 */
public class DataxTable {
    private String tableSchema;
    private String tableName;

    public String getTableSchema() {
        return tableSchema;
    }

    public void setTableSchema(String tableSchema) {
        this.tableSchema = tableSchema;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public DataxTable() {
    }

    public DataxTable(String tableSchema, String tableName) {
        this.tableSchema = tableSchema;
        this.tableName = tableName;
    }
}

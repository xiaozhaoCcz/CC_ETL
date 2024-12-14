package com.cc.job.task.enums;

public enum datasourceEnum {

    MYSQL("mysql"),
    ORACLE("oracle");

    private final String datasourceName;

    private datasourceEnum(String datasourceName){
        this.datasourceName = datasourceName;
    }

    public String getTitle() {
        return datasourceName;
    }
}

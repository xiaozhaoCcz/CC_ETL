package com.cc.job.task.enums;

public enum DatasourceEnum {

    MYSQL("mysql"),
    ORACLE("oracle");

    private final String datasourceName;

    private DatasourceEnum(String datasourceName){
        this.datasourceName = datasourceName;
    }

    public static DatasourceEnum getSourceType(String sourceType) {
        for (DatasourceEnum datasourceEnum : DatasourceEnum.values()) {
            if (datasourceEnum.datasourceName.equalsIgnoreCase(sourceType)) {
                return datasourceEnum;
            }
        }
        return null;
    }

    public String getTitle() {
        return datasourceName;
    }
}

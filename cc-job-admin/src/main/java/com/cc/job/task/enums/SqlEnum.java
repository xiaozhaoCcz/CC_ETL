package com.cc.job.task.enums;

public enum SqlEnum {

    SELECT("select"),
    UPDATE("update"),
    DELETE("delete"),
    INSERT("insert"),
    CALL("call");

    private final String name;

    private SqlEnum(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

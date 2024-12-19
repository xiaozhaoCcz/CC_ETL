package com.cc.job.xo.model.datax;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class DataXParams implements Serializable {

    private String name;

    private List<String> columns;

    private String sourceType;

    private String username;

    private String password;

    private String dbName;

    private String tableName;

    private String ip;

    private Integer port;

    private String querySql;

    private String writeMode;

    private Integer type;
}

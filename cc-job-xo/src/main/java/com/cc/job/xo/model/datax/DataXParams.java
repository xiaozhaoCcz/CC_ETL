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

    // 0全量，1增量
    private Integer incrType;

    private String incrColumnName;

    private String incrParam;

    // 增量字段，json
    private String incrContent;
}

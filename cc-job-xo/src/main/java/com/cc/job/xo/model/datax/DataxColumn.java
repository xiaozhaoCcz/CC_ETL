package com.cc.job.xo.model.datax;

import lombok.Data;

import java.io.Serializable;

@Data
public class DataxColumn implements Serializable {

    private Integer id;

    private String columnKey;

    private String columnValue;

    private String columnParam;

    private String columnTimeFormat;

    private String columnType;
}

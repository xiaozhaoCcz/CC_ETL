package com.cc.job.admin.task.constant;

public interface DataxConstant {

    String NAME = "name";

    String MYSQL_READER = "mysqlreader";

    String MYSQL_WRITER = "mysqlwriter";

    String ORACLE_READER = "oraclereader";

    String ORACLE_WRITER = "oraclewriter";

    String USERNAME = "username";

    String PASSWORD = "password";

    String JDBC_URL = "jdbcUrl";

    String MYSQL_JDBC_URL = "jdbc:mysql://%s:%s/%s";

    String ORACLE_JDBC_URL = "jdbc:oracle:thin:@%s:%s/%s";

    String QUERY_SQL = "querySql";

    String TABLE = "table";

    String COLUMN = "column";

    String WHERE = "where";

    String CONNECTION = "connection";

    String PARAMETER = "parameter";

    String WRITE_MODE = "writeMode";

    String SPLIT = ",";

    String SPACE = " ";

    String AND = "and";

    String GREATER = ">";

    String COLUMN_KEY = "columnKey";

    String COLUMN_VALUE = "columnValue";


    String ERROR_COLUMN_EMPTY = "列名不能为空";

    String ERROR_INCREMENT_CONTENT_EMPTY = "增量同步内容不能为空";
}

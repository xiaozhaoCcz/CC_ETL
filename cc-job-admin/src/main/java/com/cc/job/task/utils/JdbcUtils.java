package com.cc.job.task.utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;

public class JdbcUtils {

    private static final Logger logger = LoggerFactory.getLogger(JdbcUtils.class);

    private static Connection dbConn = null;

    public static Connection getConnection(String driverClassName, String url, String username, String password) {
        if(dbConn!=null){
            return dbConn;
        }
        try {
            Class.forName(driverClassName);
            dbConn = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dbConn;
    }

    public static void close(AutoCloseable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            logger.error("关闭出错", e);
        }
    }
}

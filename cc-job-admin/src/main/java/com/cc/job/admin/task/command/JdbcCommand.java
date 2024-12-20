package com.cc.job.admin.task.command;

import com.cc.job.xo.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;

public class JdbcCommand {
    private static final Logger logger = LoggerFactory.getLogger(JdbcCommand.class);

    private String driverClassName;

    private String url;

    private String username;

    private String password;

    private Connection con;

    public JdbcCommand(String driverClassName, String url, String username, String password) {
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public  Connection getConnection() {
        if(con!=null){
            return con;
        }
        try {
            Class.forName(driverClassName);
            con = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
        return con;
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

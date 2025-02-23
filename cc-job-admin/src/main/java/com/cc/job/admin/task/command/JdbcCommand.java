package com.cc.job.admin.task.command;

import com.alibaba.excel.util.StringUtils;
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

    private String schemaName;

    public JdbcCommand(String driverClassName, String url, String username, String password) {
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public JdbcCommand(String driverClassName, String url, String username, String password,String schemaName) {
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
        this.schemaName = schemaName;
    }

    public  Connection getConnection() {
        if(con!=null){
            return con;
        }
        try {
            Class.forName(driverClassName);
            if(StringUtils.isNotBlank(schemaName)){
                url = url + "?user="+username+"&password="+password+"&currentSchema="+schemaName;
                con = DriverManager.getConnection(url);
            }else{
                con = DriverManager.getConnection(url, username, password);
            }
        } catch (Exception e) {
            throw new BusinessException(e);
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

package com.cc.job.test;

import com.cc.job.admin.CcJobApplication;
import com.cc.job.admin.task.command.JdbcCommand;
import com.cc.job.admin.task.service.JobJdbcDatasourceService;
import com.cc.job.xo.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Connection;

@SpringBootTest(classes = CcJobApplication.class,webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CcJobApplicationTest {

    @Autowired
    private JobJdbcDatasourceService jobJdbcDatasourceService;

    @Test
    public void test1(){
        JdbcCommand jdbcCommand = new JdbcCommand("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@//127.0.0.1:1521/helowin", "system", "root","DT");
        Connection con = null;
        try {
            con = jdbcCommand.getConnection();
            System.out.println(con);
        }catch (Exception e){
            throw new BusinessException(e);
        }finally {
            JdbcCommand.close(con);
        }
    }
}





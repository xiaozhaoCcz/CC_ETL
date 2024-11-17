package com.xxl.job.executor.service.jobhandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    public void test1(){
        System.out.println("test1");
    }

    public void test2(){
        System.out.println("test2");
    }
}

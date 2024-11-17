package com.xxl.job.executor.mvc.controller;

import com.xxl.job.executor.service.jobhandler.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@EnableAutoConfiguration
public class IndexController {

    @RequestMapping("/")
    @ResponseBody
    String index() {
        return "xxl job executor running.";
    }

}
package com.cc.job.admin.task.controller;


import com.cc.job.xo.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单控制层
 *
 * @author Ray
 * @since 2020/11/06
 */
@Tag(name = "04.菜单接口")
@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {

    private static final Logger log = LoggerFactory.getLogger(MenuController.class);


    @Operation(summary = "菜单路由列表")
    @GetMapping("/routes")
    public Result<List> listRoutes() {
        return Result.success(new ArrayList<>());
    }
}


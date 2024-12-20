package com.cc.job.task.controller;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.datax.DataXParams;
import com.cc.job.task.service.DataxService;
import com.cc.job.task.service.JobJdbcDatasourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "datax接口")
@RestController
@RequestMapping("/api/v1/datax")
@RequiredArgsConstructor
public class JobDataxController {

    final JobJdbcDatasourceService jobJdbcDatasourceService;

    final DataxService dataxService;

    @Operation(summary = "根据数据源获取所有的表")
    @GetMapping("/getTables/{id}")
    public Result<List<String>> getTables(@PathVariable  Long id) {
        List<String> tables = jobJdbcDatasourceService.getTables(id);
        return Result.success(tables);
    }


    @Operation(summary = "根据表获取所有的字段")
    @PostMapping("/getColumns/{id}")
    public Result<List<String>> getColumns(@PathVariable Long id, @RequestBody Map<String,String> params) {
        List<String> tables = jobJdbcDatasourceService.getColumns(id,params);
        return Result.success(tables);
    }

    @Operation(summary = "得到json")
    @PostMapping("/getJson")
    public Result<String> getJson(@RequestBody DataXParams dataXParams) {
        String json = dataxService.getJson(dataXParams);
        return Result.success(json);
    }


}

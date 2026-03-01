package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.datax.DataXParams;
import com.cc.job.admin.task.service.DataxService;
import com.cc.job.admin.task.service.JobJdbcDatasourceService;
import com.cc.job.xo.model.datax.DataxTable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "datax接口")
@RestController
@RequestMapping("/api/v1/datax")
public class JobDataxController {

    final JobJdbcDatasourceService jobJdbcDatasourceService;

    final DataxService dataxService;

    public JobDataxController(JobJdbcDatasourceService jobJdbcDatasourceService, DataxService dataxService) {
        this.jobJdbcDatasourceService = jobJdbcDatasourceService;
        this.dataxService = dataxService;
    }

    @RequirePermission(PermissionConstants.DATASOURCE_VIEW)
    @Operation(summary = "根据数据源获取所有的表")
    @GetMapping("/getTables/{id}")
    public Result<List<DataxTable>> getTables(@PathVariable Long id) {
        List<DataxTable> tables = jobJdbcDatasourceService.getTables(id);
        return Result.success(tables);
    }


    @RequirePermission(PermissionConstants.DATASOURCE_VIEW)
    @Operation(summary = "根据表获取所有的字段")
    @PostMapping("/getColumns/{id}")
    public Result<List<String>> getColumns(@PathVariable Long id, @RequestBody Map<String,Object> params) {
        List<String> tables = jobJdbcDatasourceService.getColumns(id, params);
        return Result.success(tables);
    }

    @RequirePermission(PermissionConstants.DATASOURCE_EDIT)
    @Operation(summary = "得到json")
    @PostMapping("/getJson")
    public Result<String> getJson(@RequestBody DataXParams dataXParams) {
        String json = dataxService.getJson(dataXParams);
        return Result.success(json);
    }

    @RequirePermission(PermissionConstants.DATASOURCE_EDIT)
    @Operation(summary = "预览（前N条）Reader JSON，用于 dry-run 或预览同步结果")
    @PostMapping("/previewReaderJson")
    public Result<String> previewReaderJson(@RequestBody Map<String, Object> body) {
        DataXParams dataXParams = null;
        if (body.get("dataXParams") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) body.get("dataXParams");
            dataXParams = new com.fasterxml.jackson.databind.ObjectMapper().convertValue(map, DataXParams.class);
        }
        int limit = body.get("previewLimit") instanceof Number ? ((Number) body.get("previewLimit")).intValue() : 10;
        if (dataXParams == null) {
            return Result.failed("dataXParams 不能为空");
        }
        String json = dataxService.getPreviewReaderJson(dataXParams, limit);
        return Result.success(json);
    }

    @RequirePermission(PermissionConstants.DATASOURCE_EDIT)
    @Operation(summary = "执行")
    @PostMapping("/batchBuildJson")
    public Result<List<String> > batchBuildJson(@RequestBody Map<String,Object> params) {
        List<String> res = dataxService.batchBuildJson(params);
        return Result.success(res);
    }
}

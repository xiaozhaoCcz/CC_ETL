package com.cc.job.admin.task.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.cc.job.xo.model.form.JobJdbcDatasourceForm;
import com.cc.job.xo.model.query.JobJdbcDatasourceQuery;
import com.cc.job.xo.model.vo.JobJdbcDatasourceVO;
import com.cc.job.admin.task.service.JobJdbcDatasourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "jdbcDatasource接口")
@RestController
@RequestMapping("/api/v1/jobJdbcDatasource")
@RequiredArgsConstructor
public class JobJdbcDatasourceController {

    private final JobJdbcDatasourceService jobJdbcDatasourceService;

    @Operation(summary = "jdbc数据源配置分页列表")
    @GetMapping("/page")
    public PageResult<JobJdbcDatasourceVO> getJdbcDatasourcePage(JobJdbcDatasourceQuery queryParams ) {
        IPage<JobJdbcDatasourceVO> result = jobJdbcDatasourceService.getJdbcDatasourcePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "jdbc数据源配置列表")
    @GetMapping("/list")
    public Result<List<JobJdbcDatasource>> getJdbcDatasourceList() {
        List<JobJdbcDatasource> list = jobJdbcDatasourceService.list();
        return Result.success(list);
    }

    @Operation(summary = "新增jdbc数据源配置")
    @PostMapping
    public Result<Void> saveJdbcDatasource(@RequestBody @Valid JobJdbcDatasourceForm formData ) {
        boolean result = jobJdbcDatasourceService.saveJdbcDatasource(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取jdbc数据源配置表单数据")
    @GetMapping("/{id}/form")
    public Result<JobJdbcDatasourceForm> getJdbcDatasourceForm(
            @Parameter(description = "jdbc数据源配置ID") @PathVariable Long id
    ) {
        JobJdbcDatasourceForm formData = jobJdbcDatasourceService.getJdbcDatasourceFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改jdbc数据源配置")
    @PutMapping(value = "/{id}")
    public Result<Void> updateJdbcDatasource(
            @Parameter(description = "jdbc数据源配置ID") @PathVariable Long id,
            @RequestBody @Validated JobJdbcDatasourceForm formData
    ) {
        boolean result = jobJdbcDatasourceService.updateJdbcDatasource(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除jdbc数据源配置")
    @DeleteMapping("/{ids}")
    public Result<Void> deleteJdbcDatasources(
            @Parameter(description = "jdbc数据源配置ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = jobJdbcDatasourceService.deleteJdbcDatasources(ids);
        return Result.judge(result);
    }

    @Operation(summary = "判断是否连接成功")
    @PostMapping("/isConnect")
    public Result<Boolean> isConnect(@RequestBody @Valid JobJdbcDatasourceForm formData ) {
        boolean result = jobJdbcDatasourceService.isConnect(formData);
        return Result.success(result);
    }
}

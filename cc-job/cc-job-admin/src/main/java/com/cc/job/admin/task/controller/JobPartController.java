package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.service.JobPartService;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobPart;
import com.cc.job.xo.model.vo.JobPartVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "job_part接口")
@RestController
@RequestMapping("/api/v1/jobParts")
public class JobPartController {

    private final JobPartService jobPartService;

    public JobPartController(JobPartService jobPartService) {
        this.jobPartService = jobPartService;
    }

    @RequirePermission(PermissionConstants.PART_VIEW)
    @Operation(summary = "获取所有的树形数据")
    @GetMapping("getTree")
    public Result<List<JobPartVo>> getTree() {
        List<JobPartVo> list = jobPartService.getTree();
        return Result.success(list);
    }

    @RequirePermission(PermissionConstants.PART_EDIT)
    @Operation(summary = "保存数据")
    @PostMapping("saveJobPart")
    public Result<Void> saveJobPart(@RequestBody JobPart jobPart) {
        boolean save = jobPartService.save(jobPart);
        return Result.judge(save);
    }

    @RequirePermission(PermissionConstants.PART_EDIT)
    @Operation(summary = "修改数据")
    @PostMapping("updateJobPart")
    public Result<Void> updateJobPart(@RequestBody JobPart jobPart) {
        boolean save = jobPartService.updateById(jobPart);
        return Result.judge(save);
    }

    @RequirePermission(PermissionConstants.PART_DELETE)
    @Operation(summary = "删除数据")
    @GetMapping("deleteJobPart/{id}")
    public Result<Void> deleteJobPart(@PathVariable("id") Long id) {
        jobPartService.delete(id);
        return Result.success();
    }

    @RequirePermission(PermissionConstants.PART_VIEW)
    @Operation(summary = "获取子节点数据")
    @GetMapping("getChildren/{id}/{type}")
    public Result<Object> getChildren(@PathVariable Long id, @PathVariable Integer type) {
        Object o = jobPartService.getChildren(id, type);
        return Result.success(o);
    }

    @RequirePermission(PermissionConstants.PART_VIEW)
    @Operation(summary = "导出数据")
    @GetMapping("exportData/{id}")
    public ResponseEntity<byte[]> exportData(@PathVariable Long id){
        byte[] b =  jobPartService.exportData(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        // 设置自定义后缀文件名
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("encryptedData.cetl") // 自定义后缀
                .build());
        return new ResponseEntity<>(b, headers, HttpStatus.OK);
    }

    @RequirePermission(PermissionConstants.JOB_INFO_VIEW)
    @Operation(summary = "导出任务组数据")
    @GetMapping("exportTaskGroup/{jobId}")
    public ResponseEntity<byte[]> exportTaskGroup(@PathVariable Long jobId) {
        byte[] b = jobPartService.exportTaskGroupData(jobId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("taskgroup.cel")
                .build());
        return new ResponseEntity<>(b, headers, HttpStatus.OK);
    }

    @RequirePermission(PermissionConstants.PART_EDIT)
    @Operation(summary = "导入数据")
    @PostMapping("importData")
    public Result<Void> importData(@RequestParam("file") MultipartFile file) {
        jobPartService.importData(file);
        return Result.success();
    }

    @RequirePermission(PermissionConstants.PART_EDIT)
    @Operation(summary = "导入任务组到指定分区")
    @PostMapping("importTaskGroup")
    public Result<Void> importTaskGroup(
            @RequestParam("partitionId") Long partitionId,
            @RequestParam("file") MultipartFile file) {
        jobPartService.importTaskGroup(partitionId, file);
        return Result.success();
    }
}

package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.service.JobCanvasBookmarkService;
import com.cc.job.admin.task.utils.JwtUtil;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobCanvasBookmark;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Tag(name = "画布书签接口")
@RestController
@RequestMapping("/api/v1/jobCanvasBookmarks")
public class JobCanvasBookmarkController {

    private final JobCanvasBookmarkService jobCanvasBookmarkService;

    public JobCanvasBookmarkController(JobCanvasBookmarkService jobCanvasBookmarkService) {
        this.jobCanvasBookmarkService = jobCanvasBookmarkService;
    }

    @Operation(summary = "按任务组获取书签列表")
    @GetMapping
    public Result<List<JobCanvasBookmark>> list(
            @Parameter(description = "任务组ID") @RequestParam Long taskGroupId) {
        List<JobCanvasBookmark> list = jobCanvasBookmarkService.listByTaskGroupId(taskGroupId);
        return Result.success(list);
    }

    @Operation(summary = "新增书签")
    @PostMapping
    public Result<Long> save(@RequestBody JobCanvasBookmark bookmark, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId != null && bookmark.getCreateUserId() == null) {
            bookmark.setCreateUserId(userId);
        }
        boolean ok = jobCanvasBookmarkService.save(bookmark);
        return ok ? Result.success(bookmark.getId()) : Result.failed("保存失败");
    }

    @Operation(summary = "删除书签")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "书签ID") @PathVariable Long id) {
        boolean ok = jobCanvasBookmarkService.removeById(id);
        return Result.judge(ok);
    }

    @Operation(summary = "根据ID获取书签")
    @GetMapping("/{id}")
    public Result<JobCanvasBookmark> getById(@Parameter(description = "书签ID") @PathVariable Long id) {
        JobCanvasBookmark bookmark = jobCanvasBookmarkService.getById(id);
        return Result.success(bookmark);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        if (request == null) return null;
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return JwtUtil.getUserIdFromToken(token);
        }
        return null;
    }
}

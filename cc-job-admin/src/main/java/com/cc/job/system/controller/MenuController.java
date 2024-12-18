package com.cc.job.system.controller;

import com.cc.job.system.model.form.MenuForm;
import com.cc.job.system.model.query.MenuQuery;
import com.cc.job.system.model.vo.MenuVO;
import com.cc.job.system.model.vo.RouteVO;
import com.cc.job.common.result.Result;
import com.cc.job.common.enums.LogModuleEnum;
import com.cc.job.common.annotation.RepeatSubmit;
import com.cc.job.common.model.Option;
import com.cc.job.common.annotation.Log;
import com.cc.job.system.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 菜单控制层
 *
 * @author Ray
 * @since 2020/11/06
 */
@Tag(name = "04.菜单接口")
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "菜单列表")
    @GetMapping
    @Log( value = "菜单列表",module = LogModuleEnum.MENU)
    public Result<List<MenuVO>> listMenus(MenuQuery queryParams) {
        List<MenuVO> menuList = menuService.listMenus(queryParams);
        return Result.success(menuList);
    }

    @Operation(summary = "菜单下拉列表")
    @GetMapping("/options")
    public Result<?> listMenuOptions(
          @Parameter(description = "是否只查询父级菜单")
          @RequestParam(required = false, defaultValue = "false") boolean onlyParent
    ) {
        List<Option> menus = menuService.listMenuOptions(onlyParent);
        return Result.success(menus);
    }

    @Operation(summary = "菜单路由列表")
    @GetMapping("/routes")
    public Result<List<RouteVO>> listRoutes() {
        Set<String> roles = new HashSet<>(){{
            add("admin");
        }};
        List<RouteVO> routeList = menuService.listRoutes(roles);
        return Result.success(routeList);
    }

    @Operation(summary = "菜单表单数据")
    @GetMapping("/{id}/form")
    public Result<MenuForm> getMenuForm(
            @Parameter(description = "菜单ID") @PathVariable Long id
    ) {
        MenuForm menu = menuService.getMenuForm(id);
        return Result.success(menu);
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @RepeatSubmit
    public Result<?> addMenu(@RequestBody MenuForm menuForm) {
        boolean result = menuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    @Operation(summary = "修改菜单")
    @PutMapping(value = "/{id}")
    public Result<?> updateMenu(
            @RequestBody MenuForm menuForm
    ) {
        boolean result = menuService.saveMenu(menuForm);
        return Result.judge(result);
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public Result<?> deleteMenu(
            @Parameter(description = "菜单ID，多个以英文(,)分割") @PathVariable("id") Long id
    ) {
        boolean result = menuService.deleteMenu(id);
        return Result.judge(result);
    }

    @Operation(summary = "修改菜单显示状态")
    @PatchMapping("/{menuId}")
    public Result<?> updateMenuVisible(
            @Parameter(description = "菜单ID") @PathVariable Long menuId,
            @Parameter(description = "显示状态(1:显示;0:隐藏)") Integer visible

    ) {
        boolean result = menuService.updateMenuVisible(menuId, visible);
        return Result.judge(result);
    }

}


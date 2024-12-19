package com.cc.job.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.system.model.entity.Menu;
import com.cc.job.system.model.form.MenuForm;
import com.cc.job.system.model.query.MenuQuery;
import com.cc.job.system.model.vo.MenuVO;
import com.cc.job.system.model.vo.RouteVO;


import java.util.List;
import java.util.Set;

/**
 * 菜单业务接口
 * 
 * @author haoxr
 * @since 2020/11/06
 */
public interface MenuService extends IService<Menu> {


    List<RouteVO> listRoutes(Set<String> roles);
}

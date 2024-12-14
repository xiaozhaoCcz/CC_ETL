package com.cc.job.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.system.model.entity.Dict;
import com.cc.job.system.model.query.DictPageQuery;
import com.cc.job.system.model.vo.DictPageVO;
import com.cc.job.system.model.vo.DictVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 字典 访问层
 *
 * @author Ray Hao
 * @since 2.9.0
 */
@Mapper
public interface DictMapper extends BaseMapper<Dict> {

    /**
     * 字典分页列表
     *
     * @param page 分页参数
     * @param queryParams 查询参数
     * @return
     */
    Page<DictPageVO> getDictPage(Page<DictPageVO> page, DictPageQuery queryParams);

    /**
     * 获取所有字典和字典数据
     *
     * @return
     */
    List<DictVO> getAllDictWithData();
}





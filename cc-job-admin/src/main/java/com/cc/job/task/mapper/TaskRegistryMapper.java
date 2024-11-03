package com.cc.job.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.task.model.entity.TaskRegistry;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.task.model.query.TaskRegistryQuery;
import com.cc.job.task.model.vo.TaskRegistryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 执行器Mapper接口
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Mapper
public interface TaskRegistryMapper extends BaseMapper<TaskRegistry> {

    /**
     * 获取执行器分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return
     */
    Page<TaskRegistryVO> getTaskRegistryPage(Page<TaskRegistryVO> page, TaskRegistryQuery queryParams);

    List<Long> findDead(@Param("timeout") int timeout,
                                  @Param("nowTime") Date nowTime);

    List<TaskRegistry> findAll(@Param("timeout") int timeout,
                                 @Param("nowTime") Date nowTime);

    int registryUpdate(@Param("registryGroup") String registryGroup,
                              @Param("registryKey") String registryKey,
                              @Param("registryValue") String registryValue,
                              @Param("updateTime") Date updateTime);

    int registrySave(@Param("registryGroup") String registryGroup,
                            @Param("registryKey") String registryKey,
                            @Param("registryValue") String registryValue,
                            @Param("updateTime") Date updateTime);

    int registryDelete(@Param("registryGroup") String registryGroup,
                              @Param("registryKey") String registryKey,
                              @Param("registryValue") String registryValue);
}

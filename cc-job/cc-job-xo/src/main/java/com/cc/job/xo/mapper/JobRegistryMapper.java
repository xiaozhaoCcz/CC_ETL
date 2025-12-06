package com.cc.job.xo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.xo.model.entity.JobRegistry;
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
public interface JobRegistryMapper extends BaseMapper<JobRegistry> {


    List<Long> findDead(@Param("timeout") int timeout,
                                  @Param("nowTime") Date nowTime);

    List<JobRegistry> findAll(@Param("timeout") int timeout,
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
    
    /**
     * 更新注册信息的registryValue（用于executor-compose更新HTTP端口信息）
     * 
     * @param registryGroup 注册组
     * @param registryKey 注册键（appName）
     * @param oldRegistryValue 旧的registryValue（执行器地址）
     * @param newRegistryValue 新的registryValue（JSON格式，包含执行器地址和HTTP端口）
     * @param updateTime 更新时间
     * @return 更新的记录数
     */
    int updateRegistryValue(@Param("registryGroup") String registryGroup,
                           @Param("registryKey") String registryKey,
                           @Param("oldRegistryValue") String oldRegistryValue,
                           @Param("newRegistryValue") String newRegistryValue,
                           @Param("updateTime") Date updateTime);
}

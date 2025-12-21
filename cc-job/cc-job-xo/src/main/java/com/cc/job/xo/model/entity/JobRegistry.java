package com.cc.job.xo.model.entity;

import com.cc.job.xo.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 执行器实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@TableName("job_registry")
public class JobRegistry extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String registryGroup;
    private String registryKey;
    private String registryValue;

    public String getRegistryGroup() {
        return registryGroup;
    }

    public void setRegistryGroup(String registryGroup) {
        this.registryGroup = registryGroup;
    }

    public String getRegistryKey() {
        return registryKey;
    }

    public void setRegistryKey(String registryKey) {
        this.registryKey = registryKey;
    }

    public String getRegistryValue() {
        return registryValue;
    }

    public void setRegistryValue(String registryValue) {
        this.registryValue = registryValue;
    }
}

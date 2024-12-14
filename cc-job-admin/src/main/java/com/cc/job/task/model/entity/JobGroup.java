package com.cc.job.task.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.cc.job.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * task_group实体对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Getter
@Setter
@TableName("job_group")
public class JobGroup extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 执行器AppName
     */
    private String appName;
    /**
     * 执行器名称
     */
    private String title;
    /**
     * 执行器地址类型：0=自动注册、1=手动录入
     */
    private Integer addressType;
    /**
     * 执行器地址列表，多地址逗号分隔
     */
    private String addressList;

    @TableField(exist = false)
    private List<String> registryList;  // 执行器地址列表(系统注册)
    public List<String> getRegistryList() {
        if (addressList!=null && addressList.trim().length()>0) {
            registryList = new ArrayList<String>(Arrays.asList(addressList.split(",")));
        }
        return registryList;
    }

}

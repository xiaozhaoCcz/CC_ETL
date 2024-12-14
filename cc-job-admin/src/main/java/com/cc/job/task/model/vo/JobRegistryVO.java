package com.cc.job.task.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 执行器视图对象
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Getter
@Setter
@Schema( description = "执行器视图对象")
public class JobRegistryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String registryGroup;
    private String registryKey;
    private String registryValue;
    private LocalDateTime updateTime;
}

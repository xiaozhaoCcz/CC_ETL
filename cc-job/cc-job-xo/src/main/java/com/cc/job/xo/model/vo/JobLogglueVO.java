package com.cc.job.xo.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * task_logglue视图对象
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Getter
@Setter
@Schema( description = "task_logglue视图对象")
public class JobLogglueVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;
    @Schema(description = "任务，主键ID")
    private Integer jobId;
    @Schema(description = "GLUE类型")
    private String glueType;
    @Schema(description = "GLUE源代码")
    private String glueSource;
    @Schema(description = "GLUE备注")
    private String glueRemark;
    private LocalDateTime addTime;
    private LocalDateTime updateTime;
}

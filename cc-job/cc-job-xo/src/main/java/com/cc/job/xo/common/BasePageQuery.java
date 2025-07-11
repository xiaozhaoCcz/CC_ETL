package com.cc.job.xo.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 基础分页请求对象
 *
 * @author haoxr
 * @since 2021/2/28
 */
@Data
public class BasePageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int pageNum = 1;

    private int pageSize = 10;
}

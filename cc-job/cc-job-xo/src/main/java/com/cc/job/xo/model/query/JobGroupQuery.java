package com.cc.job.xo.model.query;

import com.cc.job.xo.common.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * task_group分页查询对象
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Schema(description ="task_group查询对象")
public class JobGroupQuery extends BasePageQuery {

    private String appName;

    private String title;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

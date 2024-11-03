import request from "@/utils/request";

const TASKLOGREPORT_BASE_URL = "/api/v1/taskLogReports";

const TaskLogReportAPI = {
    /** 获取task_log_report分页数据 */
    getPage(queryParams?: TaskLogReportPageQuery) {
        return request<any, PageResult<TaskLogReportPageVO[]>>({
            url: `${TASKLOGREPORT_BASE_URL}/page`,
            method: "get",
            params: queryParams,
        });
    },
    /**
     * 获取task_log_report表单数据
     *
     * @param id TaskLogReportID
     * @returns TaskLogReport表单数据
     */
    getFormData(id: number) {
        return request<any, TaskLogReportForm>({
            url: `${TASKLOGREPORT_BASE_URL}/${id}/form`,
            method: "get",
        });
    },

    /** 添加task_log_report*/
    add(data: TaskLogReportForm) {
        return request({
            url: `${TASKLOGREPORT_BASE_URL}`,
            method: "post",
            data: data,
        });
    },

    /**
     * 更新task_log_report
     *
     * @param id TaskLogReportID
     * @param data TaskLogReport表单数据
     */
     update(id: number, data: TaskLogReportForm) {
        return request({
            url: `${TASKLOGREPORT_BASE_URL}/${id}`,
            method: "put",
            data: data,
        });
    },

    /**
     * 批量删除task_log_report，多个以英文逗号(,)分割
     *
     * @param ids task_log_reportID字符串，多个以英文逗号(,)分割
     */
     deleteByIds(ids: string) {
        return request({
            url: `${TASKLOGREPORT_BASE_URL}/${ids}`,
            method: "delete",
        });
    }
}

export default TaskLogReportAPI;

/** task_log_report分页查询参数 */
export interface TaskLogReportPageQuery extends PageQuery {
}

/** task_log_report表单对象 */
export interface TaskLogReportForm {
    id?:  number;
    /** 调度-时间 */
    triggerDay?:  Date;
    /** 运行中-日志数量 */
    runningCount?:  number;
    /** 执行成功-日志数量 */
    sucCount?:  number;
    /** 执行失败-日志数量 */
    failCount?:  number;
    updateTime?:  Date;
}

/** task_log_report分页对象 */
export interface TaskLogReportPageVO {
    id?: number;
    /** 调度-时间 */
    triggerDay?: Date;
    /** 运行中-日志数量 */
    runningCount?: number;
    /** 执行成功-日志数量 */
    sucCount?: number;
    /** 执行失败-日志数量 */
    failCount?: number;
    updateTime?: Date;
}

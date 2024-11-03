import request from "@/utils/request";

const TASKLOG_BASE_URL = "/api/v1/taskLogs";

const TaskLogAPI = {
    /** 获取task_log分页数据 */
    getPage(queryParams?: TaskLogPageQuery) {
        return request<any, PageResult<TaskLogPageVO[]>>({
            url: `${TASKLOG_BASE_URL}/page`,
            method: "get",
            params: queryParams,
        });
    },
    /**
     * 获取task_log表单数据
     *
     * @param id TaskLogID
     * @returns TaskLog表单数据
     */
    getFormData(id: number) {
        return request<any, TaskLogForm>({
            url: `${TASKLOG_BASE_URL}/${id}/form`,
            method: "get",
        });
    },

    /** 添加task_log*/
    add(data: TaskLogForm) {
        return request({
            url: `${TASKLOG_BASE_URL}`,
            method: "post",
            data: data,
        });
    },

    /**
     * 更新task_log
     *
     * @param id TaskLogID
     * @param data TaskLog表单数据
     */
     update(id: number, data: TaskLogForm) {
        return request({
            url: `${TASKLOG_BASE_URL}/${id}`,
            method: "put",
            data: data,
        });
    },

    /**
     * 批量删除task_log，多个以英文逗号(,)分割
     *
     * @param ids task_logID字符串，多个以英文逗号(,)分割
     */
     deleteByIds(ids: string) {
        return request({
            url: `${TASKLOG_BASE_URL}/${ids}`,
            method: "delete",
        });
    }
}

export default TaskLogAPI;

/** task_log分页查询参数 */
export interface TaskLogPageQuery extends PageQuery {
}

/** task_log表单对象 */
export interface TaskLogForm {
    id?:  number;
    /** 执行器主键ID */
    jobGroup?:  number;
    /** 任务，主键ID */
    jobId?:  number;
    /** 执行器地址，本次执行的地址 */
    executorAddress?:  string;
    /** 执行器任务handler */
    executorHandler?:  string;
    /** 执行器任务参数 */
    executorParam?:  string;
    /** 执行器任务分片参数，格式如 1/2 */
    executorShardingParam?:  string;
    /** 失败重试次数 */
    executorFailRetryCount?:  number;
    /** 调度-时间 */
    triggerTime?:  Date;
    /** 调度-结果 */
    triggerCode?:  number;
    /** 调度-日志 */
    triggerMsg?:  string;
    /** 执行-时间 */
    handleTime?:  Date;
    /** 执行-状态 */
    handleCode?:  number;
    /** 执行-日志 */
    handleMsg?:  string;
    /** 告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败 */
    alarmStatus?:  number;
}

/** task_log分页对象 */
export interface TaskLogPageVO {
    id?: number;
    /** 执行器主键ID */
    jobGroup?: number;
    /** 任务，主键ID */
    jobId?: number;
    /** 执行器地址，本次执行的地址 */
    executorAddress?: string;
    /** 执行器任务handler */
    executorHandler?: string;
    /** 执行器任务参数 */
    executorParam?: string;
    /** 执行器任务分片参数，格式如 1/2 */
    executorShardingParam?: string;
    /** 失败重试次数 */
    executorFailRetryCount?: number;
    /** 调度-时间 */
    triggerTime?: Date;
    /** 调度-结果 */
    triggerCode?: number;
    /** 调度-日志 */
    triggerMsg?: string;
    /** 执行-时间 */
    handleTime?: Date;
    /** 执行-状态 */
    handleCode?: number;
    /** 执行-日志 */
    handleMsg?: string;
    /** 告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败 */
    alarmStatus?: number;
}

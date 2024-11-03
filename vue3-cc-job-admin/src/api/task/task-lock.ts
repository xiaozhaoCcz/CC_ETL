import request from "@/utils/request";

const TASKLOCK_BASE_URL = "/api/v1/taskLocks";

const TaskLockAPI = {
    /** 获取task_lock分页数据 */
    getPage(queryParams?: TaskLockPageQuery) {
        return request<any, PageResult<TaskLockPageVO[]>>({
            url: `${TASKLOCK_BASE_URL}/page`,
            method: "get",
            params: queryParams,
        });
    },
    /**
     * 获取task_lock表单数据
     *
     * @param id TaskLockID
     * @returns TaskLock表单数据
     */
    getFormData(id: number) {
        return request<any, TaskLockForm>({
            url: `${TASKLOCK_BASE_URL}/${id}/form`,
            method: "get",
        });
    },

    /** 添加task_lock*/
    add(data: TaskLockForm) {
        return request({
            url: `${TASKLOCK_BASE_URL}`,
            method: "post",
            data: data,
        });
    },

    /**
     * 更新task_lock
     *
     * @param id TaskLockID
     * @param data TaskLock表单数据
     */
     update(id: number, data: TaskLockForm) {
        return request({
            url: `${TASKLOCK_BASE_URL}/${id}`,
            method: "put",
            data: data,
        });
    },

    /**
     * 批量删除task_lock，多个以英文逗号(,)分割
     *
     * @param ids task_lockID字符串，多个以英文逗号(,)分割
     */
     deleteByIds(ids: string) {
        return request({
            url: `${TASKLOCK_BASE_URL}/${ids}`,
            method: "delete",
        });
    }
}

export default TaskLockAPI;

/** task_lock分页查询参数 */
export interface TaskLockPageQuery extends PageQuery {
}

/** task_lock表单对象 */
export interface TaskLockForm {
    /** 锁名称 */
    lockName?:  string;
}

/** task_lock分页对象 */
export interface TaskLockPageVO {
    /** 锁名称 */
    lockName?: string;
}

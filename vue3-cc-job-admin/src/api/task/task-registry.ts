import request from "@/utils/request";

const TASKREGISTRY_BASE_URL = "/api/v1/taskRegistrys";

const TaskRegistryAPI = {
    /** 获取执行器分页数据 */
    getPage(queryParams?: TaskRegistryPageQuery) {
        return request<any, PageResult<TaskRegistryPageVO[]>>({
            url: `${TASKREGISTRY_BASE_URL}/page`,
            method: "get",
            params: queryParams,
        });
    },
    /**
     * 获取执行器表单数据
     *
     * @param id TaskRegistryID
     * @returns TaskRegistry表单数据
     */
    getFormData(id: number) {
        return request<any, TaskRegistryForm>({
            url: `${TASKREGISTRY_BASE_URL}/${id}/form`,
            method: "get",
        });
    },

    /** 添加执行器*/
    add(data: TaskRegistryForm) {
        return request({
            url: `${TASKREGISTRY_BASE_URL}`,
            method: "post",
            data: data,
        });
    },

    /**
     * 更新执行器
     *
     * @param id TaskRegistryID
     * @param data TaskRegistry表单数据
     */
     update(id: number, data: TaskRegistryForm) {
        return request({
            url: `${TASKREGISTRY_BASE_URL}/${id}`,
            method: "put",
            data: data,
        });
    },

    /**
     * 批量删除执行器，多个以英文逗号(,)分割
     *
     * @param ids 执行器ID字符串，多个以英文逗号(,)分割
     */
     deleteByIds(ids: string) {
        return request({
            url: `${TASKREGISTRY_BASE_URL}/${ids}`,
            method: "delete",
        });
    }
}

export default TaskRegistryAPI;

/** 执行器分页查询参数 */
export interface TaskRegistryPageQuery extends PageQuery {
}

/** 执行器表单对象 */
export interface TaskRegistryForm {
    id?:  number;
    registryGroup?:  string;
    registryKey?:  string;
    registryValue?:  string;
    updateTime?:  Date;
}

/** 执行器分页对象 */
export interface TaskRegistryPageVO {
    id?: number;
    registryGroup?: string;
    registryKey?: string;
    registryValue?: string;
    updateTime?: Date;
}

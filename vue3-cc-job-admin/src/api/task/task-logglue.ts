import request from "@/utils/request";

const TASKLOGGLUE_BASE_URL = "/api/v1/taskLogglues";

const TaskLogglueAPI = {
  /** 获取task_logglue分页数据 */
  getPage(queryParams?: TaskLoggluePageQuery) {
    return request<any, PageResult<TaskLoggluePageVO[]>>({
      url: `${TASKLOGGLUE_BASE_URL}/page`,
      method: "get",
      params: queryParams,
    });
  },
  /**
   * 获取task_logglue表单数据
   *
   * @param id TaskLogglueID
   * @returns TaskLogglue表单数据
   */
  getFormData(id: number) {
    return request<any, TaskLogglueForm>({
      url: `${TASKLOGGLUE_BASE_URL}/${id}/form`,
      method: "get",
    });
  },

  /** 添加task_logglue*/
  add(data: TaskLogglueForm) {
    return request({
      url: `${TASKLOGGLUE_BASE_URL}`,
      method: "post",
      data: data,
    });
  },

  /**
   * 更新task_logglue
   *
   * @param id TaskLogglueID
   * @param data TaskLogglue表单数据
   */
  update(id: number, data: TaskLogglueForm) {
    return request({
      url: `${TASKLOGGLUE_BASE_URL}/${id}`,
      method: "put",
      data: data,
    });
  },

  /**
   * 批量删除task_logglue，多个以英文逗号(,)分割
   *
   * @param ids task_logglueID字符串，多个以英文逗号(,)分割
   */
  deleteByIds(ids: string) {
    return request({
      url: `${TASKLOGGLUE_BASE_URL}/${ids}`,
      method: "delete",
    });
  }
}

export default TaskLogglueAPI;

/** task_logglue分页查询参数 */
export interface TaskLoggluePageQuery extends PageQuery {
}

/** task_logglue表单对象 */
export interface TaskLogglueForm {
  id?: number;
  /** 任务，主键ID */
  jobId?: number;
  /** GLUE类型 */
  glueType?: string;
  /** GLUE源代码 */
  glueSource?: string;
  /** GLUE备注 */
  glueRemark?: string;
  addTime?: Date;
  updateTime?: Date;
}

/** task_logglue分页对象 */
export interface TaskLoggluePageVO {
  id?: number;
  /** 任务，主键ID */
  jobId?: number;
  /** GLUE类型 */
  glueType?: string;
  /** GLUE源代码 */
  glueSource?: string;
  /** GLUE备注 */
  glueRemark?: string;
  addTime?: Date;
  updateTime?: Date;
}

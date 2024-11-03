import request from "@/utils/request";

const TASKINFO_BASE_URL = "/api/v1/taskInfos";

const TaskInfoAPI = {
  /** 获取task_info分页数据 */
  getPage(queryParams?: TaskInfoPageQuery) {
    return request<any, PageResult<TaskInfoPageVO[]>>({
      url: `${TASKINFO_BASE_URL}/page`,
      method: "get",
      params: queryParams,
    });
  },
  /**
   * 获取task_info表单数据
   *
   * @param id TaskInfoID
   * @returns TaskInfo表单数据
   */
  getFormData(id: number) {
    return request<any, TaskInfoForm>({
      url: `${TASKINFO_BASE_URL}/${id}/form`,
      method: "get",
    });
  },

  /** 添加task_info*/
  add(data: TaskInfoForm) {
    return request({
      url: `${TASKINFO_BASE_URL}`,
      method: "post",
      data: data,
    });
  },

  /**
   * 更新task_info
   *
   * @param id TaskInfoID
   * @param data TaskInfo表单数据
   */
  update(id: number, data: TaskInfoForm) {
    return request({
      url: `${TASKINFO_BASE_URL}/${id}`,
      method: "put",
      data: data,
    });
  },

  /**
   * 批量删除task_info，多个以英文逗号(,)分割
   *
   * @param ids task_infoID字符串，多个以英文逗号(,)分割
   */
  deleteByIds(ids: string) {
    return request({
      url: `${TASKINFO_BASE_URL}/${ids}`,
      method: "delete",
    });
  }
}

export default TaskInfoAPI;

/** task_info分页查询参数 */
export interface TaskInfoPageQuery extends PageQuery {
}

/** task_info表单对象 */
export interface TaskInfoForm {
  id?: number;
  /** 执行器主键ID */
  jobGroup?: number;
  jobDesc?: string;
  addTime?: Date;
  updateTime?: Date;
  /** 作者 */
  author?: string;
  /** 报警邮件 */
  alarmEmail?: string;
  /** 调度类型 */
  scheduleType?: string;
  /** 调度配置，值含义取决于调度类型 */
  scheduleConf?: string;
  /** 调度过期策略 */
  misfireStrategy?: string;
  /** 执行器路由策略 */
  executorRouteStrategy?: string;
  /** 执行器任务handler */
  executorHandler?: string;
  /** 执行器任务参数 */
  executorParam?: string;
  /** 阻塞处理策略 */
  executorBlockStrategy?: string;
  /** 任务执行超时时间，单位秒 */
  executorTimeout?: number;
  /** 失败重试次数 */
  executorFailRetryCount?: number;
  /** GLUE类型 */
  glueType?: string;
  /** GLUE源代码 */
  glueSource?: string;
  /** GLUE备注 */
  glueRemark?: string;
  /** GLUE更新时间 */
  glueUpdatetime?: Date;
  /** 子任务ID，多个逗号分隔 */
  childJobid?: string;
  /** 调度状态：0-停止，1-运行 */
  triggerStatus?: number;
  /** 上次调度时间 */
  triggerLastTime?: number;
  /** 下次调度时间 */
  triggerNextTime?: number;
}

/** task_info分页对象 */
export interface TaskInfoPageVO {
  id?: number;
  /** 执行器主键ID */
  jobGroup?: number;
  jobDesc?: string;
  addTime?: Date;
  updateTime?: Date;
  /** 作者 */
  author?: string;
  /** 报警邮件 */
  alarmEmail?: string;
  /** 调度类型 */
  scheduleType?: string;
  /** 调度配置，值含义取决于调度类型 */
  scheduleConf?: string;
  /** 调度过期策略 */
  misfireStrategy?: string;
  /** 执行器路由策略 */
  executorRouteStrategy?: string;
  /** 执行器任务handler */
  executorHandler?: string;
  /** 执行器任务参数 */
  executorParam?: string;
  /** 阻塞处理策略 */
  executorBlockStrategy?: string;
  /** 任务执行超时时间，单位秒 */
  executorTimeout?: number;
  /** 失败重试次数 */
  executorFailRetryCount?: number;
  /** GLUE类型 */
  glueType?: string;
  /** GLUE源代码 */
  glueSource?: string;
  /** GLUE备注 */
  glueRemark?: string;
  /** GLUE更新时间 */
  glueUpdatetime?: Date;
  /** 子任务ID，多个逗号分隔 */
  childJobid?: string;
  /** 调度状态：0-停止，1-运行 */
  triggerStatus?: number;
  /** 上次调度时间 */
  triggerLastTime?: number;
  /** 下次调度时间 */
  triggerNextTime?: number;
}

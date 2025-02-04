import request from "@/utils/request";

const JOBLOG_BASE_URL = "/api/v1/jobLogs";

const JobLogAPI = {
  /** 获取task_log分页数据 */
  getPage(queryParams?: TaskLogPageQuery) {
    return request<any, PageResult<TaskLogPageVO[]>>({
      url: `${JOBLOG_BASE_URL}/page`,
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
      url: `${JOBLOG_BASE_URL}/${id}/form`,
      method: "get",
    });
  },

  deleteJobLogs(queryParams?: TaskLogPageQuery) {
    return request({
      url: `${JOBLOG_BASE_URL}`,
      method: "delete",
      params: queryParams,
    });
  },

  logDetailCat(logId: number, fromLineNum: number) {
    return request({
      url: `${JOBLOG_BASE_URL}/logDetailCat`,
      method: "get",
      params: { logId, fromLineNum },
    });
  }
}

export default JobLogAPI;

/** task_log分页查询参数 */
export interface TaskLogPageQuery extends PageQuery {
}

/** task_log表单对象 */
export interface TaskLogForm {
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

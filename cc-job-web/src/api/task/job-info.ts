/**
 * 任务信息 API
 * 
 * @author cc-job-team
 */

import request from "@/utils/request";

const BASE_URL = "/api/v1/jobInfos";

/**
 * 任务信息 API
 */
const JobInfoAPI = {
  /**
   * 初始化数据
   */
  initData() {
    return request({
      url: `${BASE_URL}/initData`,
      method: "get",
    });
  },

  /**
   * 获取任务分页列表
   */
  getPage(queryParams?: JobInfoPageQuery) {
    return request<any, PageResult<TaskInfoPageVO[]>>({
      url: `${BASE_URL}/page`,
      method: "get",
      params: queryParams,
    });
  },

  /**
   * 获取任务列表
   */
  getList(jobType?: number) {
    return request({
      url: `${BASE_URL}/list`,
      method: "get",
      params: { jobType },
    });
  },

  /**
   * 获取任务表单数据
   */
  getFormData(id: number) {
    return request<any, JobInfoForm>({
      url: `${BASE_URL}/${id}/form`,
      method: "get",
    });
  },

  /**
   * 新增任务
   */
  add(data: JobInfoForm) {
    return request({
      url: BASE_URL,
      method: "post",
      data,
    });
  },

  /**
   * 更新任务
   */
  update(id: number, data: JobInfoForm) {
    return request({
      url: `${BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },

  /**
   * 批量删除任务
   */
  deleteByIds(ids: string) {
    return request({
      url: `${BASE_URL}/${ids}`,
      method: "delete",
    });
  },

  /**
   * 触发任务执行
   */
  triggerJob(data: TriggerJobRequest) {
    return request({
      url: `${BASE_URL}/trigger`,
      method: "post",
      data,
    });
  },

  /**
   * 启动任务
   */
  startJob(id: number) {
    return request({
      url: `${BASE_URL}/startJob/${id}`,
      method: "get",
    });
  },

  /**
   * 停止任务
   */
  stopJob(id: number) {
    return request({
      url: `${BASE_URL}/stopJob/${id}`,
      method: "get",
    });
  },

  /**
   * 停止任务组
   */
  stopJobCompose(id: number, randomId: string) {
    return request({
      url: `${BASE_URL}/stopJobCompose/${id}/${randomId}`,
      method: "get",
    });
  },

  /**
   * 获取下次触发时间
   */
  nextTriggerTime(scheduleType: string, scheduleConf: string) {
    return request({
      url: `${BASE_URL}/nextTriggerTime`,
      method: "get",
      params: { scheduleType, scheduleConf },
    });
  },

  /**
   * 保存任务组
   */
  saveJobCompose(data: JobInfoForm) {
    return request({
      url: `${BASE_URL}/saveJobCompose`,
      method: "post",
      data,
    });
  },

  /**
   * 更新任务组
   */
  updateJobCompose(id: number, data: JobInfoForm) {
    return request({
      url: `${BASE_URL}/updateJobCompose/${id}`,
      method: "put",
      data,
    });
  },

  /**
   * 保存 GLUE 源码
   */
  saveGlueSource(data: SaveGlueSourceRequest) {
    return request({
      url: `${BASE_URL}/saveGlueSource`,
      method: "post",
      data,
    });
  },

  /**
   * 获取 GLUE 历史版本列表
   */
  getGlueList(id: number) {
    return request({
      url: `${BASE_URL}/getGlueList/${id}`,
      method: "get",
    });
  },

  /**
   * 获取任务组编排数据
   */
  getJobCompose(data: GetJobComposeRequest) {
    return request({
      url: `${BASE_URL}/getJobCompose`,
      method: "post",
      data,
    });
  },

  /**
   * 验证任务组边是否有效
   */
  validateJobComposeEdge(data: JobInfoForm) {
    return request({
      url: `${BASE_URL}/validateJobComposeEdge`,
      method: "post",
      data,
    });
  },

  /**
   * 暂停/恢复任务
   */
  pauseJob(id: number, isPause: number) {
    return request({
      url: `${BASE_URL}/pauseJob/${id}`,
      method: "get",
      params: { isPause },
    });
  },
};

export default JobInfoAPI;

/**
 * 类型定义
 */

/** 任务分页查询参数 */
export interface JobInfoPageQuery extends PageQuery {
  jobGroup?: number;
  jobDesc?: string;
  author?: string;
  triggerStatus?: number;
}

/** 触发任务请求 */
export interface TriggerJobRequest {
  id: number;
  executorParam?: string;
  triggerUserId?: number;
}

/** 保存 GLUE 源码请求 */
export interface SaveGlueSourceRequest {
  id: number;
  glueSource: string;
  glueRemark?: string;
}

/** 获取任务组请求 */
export interface GetJobComposeRequest {
  id: number;
  randomId?: string;
}

/** 任务表单对象 */
export interface JobInfoForm {
  id?: number;
  jobGroup?: number;
  jobDesc?: string;
  author?: string;
  alarmEmail?: string;
  scheduleType?: string;
  scheduleConf?: string;
  misfireStrategy?: string;
  executorRouteStrategy?: string;
  executorHandler?: string;
  executorParam?: string;
  executorBlockStrategy?: string;
  executorTimeout?: number;
  executorFailRetryCount?: number;
  glueType?: string;
  glueSource?: string;
  glueRemark?: string;
  glueUpdatetime?: Date;
  childJobid?: string;
  triggerStatus?: number;
  triggerLastTime?: number;
  triggerNextTime?: number;
  jobType?: number;
  parentId?: number;
  reqType?: string;
  reqHeader?: string;
  reqBody?: string;
  reqUrl?: string;
  nodes?: string;
  edges?: string;
  incrType?: number;
  jdbcDatasourceId?: number;
  incrContent?: string;
  runTime?: number;
  isPause?: number;
  jobPartId?: number;
  triggerUserId?: number;
}

/** 任务分页对象 */
export interface TaskInfoPageVO {
  id?: number;
  jobGroup?: number;
  jobDesc?: string;
  addTime?: Date;
  updateTime?: Date;
  author?: string;
  alarmEmail?: string;
  scheduleType?: string;
  scheduleConf?: string;
  misfireStrategy?: string;
  executorRouteStrategy?: string;
  executorHandler?: string;
  executorParam?: string;
  executorBlockStrategy?: string;
  executorTimeout?: number;
  executorFailRetryCount?: number;
  glueType?: string;
  glueSource?: string;
  glueRemark?: string;
  glueUpdatetime?: Date;
  childJobid?: string;
  triggerStatus?: number;
  triggerLastTime?: number;
  triggerNextTime?: number;
  jobType?: number;
}

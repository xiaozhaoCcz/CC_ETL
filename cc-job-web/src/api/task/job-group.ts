import request from "@/utils/request";

const JOB_GROUP_BASE_URL = "/api/v1/jobGroups";

const JobGroupAPI = {
  /** 获取task_group分页数据 */
  getPage(queryParams?: JobGroupPageQuery) {
    return request<any, PageResult<JobGroupPageVO[]>>({
      url: `${JOB_GROUP_BASE_URL}/page`,
      method: "get",
      params: queryParams,
    });
  },
  /**
   * 获取task_group表单数据
   *
   * @param id TaskGroupID
   * @returns TaskGroup表单数据
   */
  getFormData(id: number) {
    return request<any, JobGroupForm>({
      url: `${JOB_GROUP_BASE_URL}/${id}/form`,
      method: "get",
    });
  },

  /** 添加task_group*/
  add(data: JobGroupForm) {
    return request({
      url: `${JOB_GROUP_BASE_URL}`,
      method: "post",
      data: data,
    });
  },

  /**
   * 更新task_group
   *
   * @param id TaskGroupID
   * @param data TaskGroup表单数据
   */
  update(id: number, data: JobGroupForm) {
    return request({
      url: `${JOB_GROUP_BASE_URL}/${id}`,
      method: "put",
      data: data,
    });
  },

  /**
   * 批量删除task_group，多个以英文逗号(,)分割
   *
   * @param ids task_groupID字符串，多个以英文逗号(,)分割
   */
  deleteByIds(ids: string) {
    return request({
      url: `${JOB_GROUP_BASE_URL}/${ids}`,
      method: "delete",
    });
  },

  findAddressList(id: number) {
    return request({
      url: `${JOB_GROUP_BASE_URL}/findAddressList/${id}`,
      method: "get",
    });
  },

  getAllJobGroupList() {
    return request({
      url: `${JOB_GROUP_BASE_URL}/getAllJobGroupList`,
      method: "get",
    });
  },
};

export default JobGroupAPI;

/** task_group分页查询参数 */
export interface JobGroupPageQuery extends PageQuery { }

/** task_group表单对象 */
export interface JobGroupForm {
  id?: number;
  /** 执行器AppName */
  appName?: string;
  /** 执行器名称 */
  title?: string;
  /** 执行器地址类型：0=自动注册、1=手动录入 */
  addressType?: number;
  /** 执行器地址列表，多地址逗号分隔 */
  addressList?: string;
  updateTime?: Date;
}

/** task_group分页对象 */
export interface JobGroupPageVO {
  id?: number;
  /** 执行器AppName */
  appName?: string;
  /** 执行器名称 */
  title?: string;
  /** 执行器地址类型：0=自动注册、1=手动录入 */
  addressType?: number;
  /** 执行器地址列表，多地址逗号分隔 */
  addressList?: string;
  updateTime?: Date;
}

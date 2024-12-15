import request from "@/utils/request";

const JOBJDBCDATASOURCE_BASE_URL = "/api/v1/jobJdbcDatasource";

const JobJdbcDatasourceAPI = {
  /** 获取jdbc数据源配置分页数据 */
  getPage(queryParams?: JobJdbcDatasourcePageQuery) {
    return request<any, PageResult<JobJdbcDatasourcePageVO[]>>({
      url: `${JOBJDBCDATASOURCE_BASE_URL}/page`,
      method: "get",
      params: queryParams,
    });
  },
  /**
   * 获取jdbc数据源配置表单数据
   *
   * @param id JobJdbcDatasourceID
   * @returns JobJdbcDatasource表单数据
   */
  getFormData(id: number) {
    return request<any, JobJdbcDatasourceForm>({
      url: `${JOBJDBCDATASOURCE_BASE_URL}/${id}/form`,
      method: "get",
    });
  },

  getJdbcDatasourceList() {
    return request<any, JobJdbcDatasourceForm>({
      url: `${JOBJDBCDATASOURCE_BASE_URL}/list`,
      method: "get",
    });
  },

  /** 添加jdbc数据源配置*/
  add(data: JobJdbcDatasourceForm) {
    return request({
      url: `${JOBJDBCDATASOURCE_BASE_URL}`,
      method: "post",
      data: data,
    });
  },

  /**
   * 更新jdbc数据源配置
   *
   * @param id JobJdbcDatasourceID
   * @param data JobJdbcDatasource表单数据
   */
  update(id: number, data: JobJdbcDatasourceForm) {
    return request({
      url: `${JOBJDBCDATASOURCE_BASE_URL}/${id}`,
      method: "put",
      data: data,
    });
  },

  /**
   * 批量删除jdbc数据源配置，多个以英文逗号(,)分割
   *
   * @param ids jdbc数据源配置ID字符串，多个以英文逗号(,)分割
   */
  deleteByIds(ids: string) {
    return request({
      url: `${JOBJDBCDATASOURCE_BASE_URL}/${ids}`,
      method: "delete",
    });
  },
};

export default JobJdbcDatasourceAPI;

/** jdbc数据源配置分页查询参数 */
export interface JobJdbcDatasourcePageQuery extends PageQuery {}

/** jdbc数据源配置表单对象 */
export interface JobJdbcDatasourceForm {
  /** 自增主键 */
  id?: number;
  /** 数据源名称 */
  datasourceName?: string;
  /** 数据源 */
  datasource?: string;
  /** 数据源分组 */
  datasourceGroup?: string;
  /** 数据库名 */
  databaseName?: string;
  /** 用户名 */
  jdbcUsername?: string;
  /** 密码 */
  jdbcPassword?: string;
  /** jdbc url */
  jdbcUrl?: string;
  /** jdbc驱动类 */
  jdbcDriverClass?: string;
  /** 状态：0删除 1启用 2禁用 */
  status?: number;
  /** 创建人 */
  createBy?: string;
  /** 创建时间 */
  createTime?: Date;
  /** 更新人 */
  updateBy?: string;
  /** 更新时间 */
  updateTime?: Date;
  /** 备注 */
  comments?: string;

  ip?: string;

  port?: string;
}

/** jdbc数据源配置分页对象 */
export interface JobJdbcDatasourcePageVO {
  /** 自增主键 */
  id?: number;
  /** 数据源名称 */
  datasourceName?: string;
  /** 数据源 */
  datasource?: string;
  /** 数据源分组 */
  datasourceGroup?: string;
  /** 数据库名 */
  databaseName?: string;
  /** 用户名 */
  jdbcUsername?: string;
  /** 密码 */
  jdbcPassword?: string;
  /** jdbc url */
  jdbcUrl?: string;
  /** jdbc驱动类 */
  jdbcDriverClass?: string;
  /** 状态：0删除 1启用 2禁用 */
  status?: number;
  /** 创建人 */
  createBy?: string;
  /** 创建时间 */
  createTime?: Date;
  /** 更新人 */
  updateBy?: string;
  /** 更新时间 */
  updateTime?: Date;
  /** 备注 */
  comments?: string;
}

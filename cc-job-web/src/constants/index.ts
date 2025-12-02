/**
 * 统一导出所有常量
 * 
 * @author cc-job-team
 */

// 导出任务相关常量
export * from './task';

// 导出UI相关常量
export * from './ui';

// HTTP 状态码
export const HTTP_STATUS = {
  SUCCESS: 200,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500,
} as const;

// 通用常量
export const COMMON = {
  /** 默认分页大小 */
  PAGE_SIZE: 10,
  /** 最大分页大小 */
  MAX_PAGE_SIZE: 100,
  /** 请求超时时间（毫秒） */
  REQUEST_TIMEOUT: 50000,
} as const;

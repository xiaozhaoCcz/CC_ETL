/**
 * 分页组合函数
 * 
 * <p>封装通用的分页逻辑
 * 
 * @author cc-job-team
 */

import { reactive } from 'vue';
import { DEFAULT_PAGINATION, PAGE_SIZES } from '@/constants';

export interface PaginationState {
  pageNum: number;
  pageSize: number;
  total: number;
}

export function usePagination(initialPageSize: number = DEFAULT_PAGINATION.pageSize) {
  const pagination = reactive<PaginationState>({
    pageNum: DEFAULT_PAGINATION.pageNum,
    pageSize: initialPageSize,
    total: 0
  });
  
  /**
   * 重置分页
   */
  const resetPagination = () => {
    pagination.pageNum = DEFAULT_PAGINATION.pageNum;
    pagination.pageSize = initialPageSize;
    pagination.total = 0;
  };
  
  /**
   * 处理页码变化
   */
  const handlePageChange = (pageNum: number) => {
    pagination.pageNum = pageNum;
  };
  
  /**
   * 处理每页大小变化
   */
  const handleSizeChange = (pageSize: number) => {
    pagination.pageSize = pageSize;
    pagination.pageNum = 1; // 重置到第一页
  };
  
  /**
   * 设置总数
   */
  const setTotal = (total: number) => {
    pagination.total = total;
  };
  
  /**
   * 获取分页参数
   */
  const getPaginationParams = () => {
    return {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    };
  };
  
  return {
    pagination,
    pageSizes: PAGE_SIZES,
    resetPagination,
    handlePageChange,
    handleSizeChange,
    setTotal,
    getPaginationParams
  };
}


/**
 * 表格选择组合函数
 * 
 * <p>封装表格行选择的通用逻辑
 * 
 * @author cc-job-team
 */

import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { MESSAGE_CONFIG } from '@/constants';

export function useTableSelection<T = any>() {
  const selectedIds = ref<number[]>([]);
  const selectedRows = ref<T[]>([]);
  
  /**
   * 处理选择变化
   */
  const handleSelectionChange = (selection: T[]) => {
    selectedRows.value = selection;
    selectedIds.value = selection.map((item: any) => item.id);
  };
  
  /**
   * 清空选择
   */
  const clearSelection = () => {
    selectedIds.value = [];
    selectedRows.value = [];
  };
  
  /**
   * 检查是否有选中项
   */
  const hasSelection = () => {
    return selectedIds.value.length > 0;
  };
  
  /**
   * 获取选中的ID字符串
   */
  const getSelectedIdsString = () => {
    return selectedIds.value.join(',');
  };
  
  /**
   * 检查选择并提示
   */
  const checkSelection = (message: string = '请至少选择一条数据') => {
    if (!hasSelection()) {
      ElMessage.warning({
        message,
        ...MESSAGE_CONFIG
      });
      return false;
    }
    return true;
  };
  
  return {
    selectedIds,
    selectedRows,
    handleSelectionChange,
    clearSelection,
    hasSelection,
    getSelectedIdsString,
    checkSelection
  };
}


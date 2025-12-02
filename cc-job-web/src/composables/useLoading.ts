/**
 * 加载状态组合函数
 * 
 * <p>封装异步操作的加载状态管理
 * 
 * @author cc-job-team
 */

import { ref } from 'vue';

export function useLoading(initialState: boolean = false) {
  const loading = ref(initialState);
  
  /**
   * 执行带加载状态的异步操作
   */
  const withLoading = async <T>(asyncFn: () => Promise<T>): Promise<T | undefined> => {
    try {
      loading.value = true;
      return await asyncFn();
    } catch (error) {
      console.error('操作失败:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };
  
  /**
   * 设置加载状态
   */
  const setLoading = (value: boolean) => {
    loading.value = value;
  };
  
  return {
    loading,
    withLoading,
    setLoading
  };
}


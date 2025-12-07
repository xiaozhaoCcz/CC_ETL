/**
 * 任务状态管理 Composable
 * 
 * <p>封装任务状态相关的逻辑
 * 
 * @author cc-job-team
 */

import { ref, computed } from 'vue';
import { NODE_STATUS } from '@/constants/task';
import { getNodeStatusText, getNodeStatusColor, isTaskRunning } from '@/utils/taskHelper';

export function useTaskStatus() {
  const currentStatus = ref<number>(0);
  const triggerStatus = ref<number>(0);
  
  /**
   * 状态文本
   */
  const statusText = computed(() => {
    return getNodeStatusText(currentStatus.value);
  });
  
  /**
   * 状态颜色
   */
  const statusColor = computed(() => {
    return getNodeStatusColor(currentStatus.value);
  });
  
  /**
   * 是否运行中
   */
  const isRunning = computed(() => {
    return isTaskRunning(triggerStatus.value);
  });
  
  /**
   * 是否成功
   */
  const isSuccess = computed(() => {
    return currentStatus.value === NODE_STATUS.SUCCESS;
  });
  
  /**
   * 是否失败
   */
  const isFailed = computed(() => {
    return currentStatus.value === NODE_STATUS.FAILED;
  });
  
  /**
   * 更新状态
   */
  const updateStatus = (status: number) => {
    currentStatus.value = status;
  };
  
  /**
   * 更新触发状态
   */
  const updateTriggerStatus = (status: number) => {
    triggerStatus.value = status;
  };
  
  /**
   * 重置状态
   */
  const resetStatus = () => {
    currentStatus.value = 0;
    triggerStatus.value = 0;
  };
  
  return {
    currentStatus,
    triggerStatus,
    statusText,
    statusColor,
    isRunning,
    isSuccess,
    isFailed,
    updateStatus,
    updateTriggerStatus,
    resetStatus,
  };
}


/**
 * 任务相关辅助函数
 * 
 * @author cc-job-team
 */

import { TASK_TYPE, TASK_STATUS, NODE_STATUS, NODE_STATUS_COLORS } from '@/constants/task';
import type { JobInfoForm } from '@/api/task/job-info';

/**
 * 判断是否为任务组
 */
export function isTaskGroup(taskType: number): boolean {
  return taskType === TASK_TYPE.GROUP;
}

/**
 * 判断是否为DataX任务
 */
export function isDataxTask(taskType: number): boolean {
  return taskType === TASK_TYPE.DATAX;
}

/**
 * 判断任务是否运行中
 */
export function isTaskRunning(triggerStatus: number): boolean {
  return triggerStatus === TASK_STATUS.RUNNING;
}

/**
 * 获取任务类型文本
 */
export function getTaskTypeText(taskType: number): string {
  const typeMap: Record<number, string> = {
    [TASK_TYPE.NORMAL]: '普通任务',
    [TASK_TYPE.DATAX]: 'DataX任务',
    [TASK_TYPE.GROUP]: '任务组',
  };
  return typeMap[taskType] || '未知类型';
}

/**
 * 获取任务状态文本
 */
export function getTaskStatusText(triggerStatus: number): string {
  const statusMap: Record<number, string> = {
    [TASK_STATUS.STOPPED]: '已停止',
    [TASK_STATUS.RUNNING]: '运行中',
  };
  return statusMap[triggerStatus] || '未知状态';
}

/**
 * 获取节点状态文本
 */
export function getNodeStatusText(status: number): string {
  const statusMap: Record<number, string> = {
    [NODE_STATUS.FAILED]: '失败',
    [NODE_STATUS.SUCCESS]: '成功',
    [NODE_STATUS.RUNNING]: '执行中',
    [NODE_STATUS.COMPLETED]: '完成',
    [NODE_STATUS.PREDICTED]: '预测时间',
  };
  return statusMap[status] || '未知';
}

/**
 * 获取节点状态颜色
 */
export function getNodeStatusColor(status: number): string {
  return NODE_STATUS_COLORS[status as keyof typeof NODE_STATUS_COLORS] || '#909399';
}

/**
 * 验证 CRON 表达式
 */
export function validateCronExpression(cron: string): boolean {
  if (!cron || !cron.trim()) {
    return false;
  }
  
  // 简单的CRON表达式验证（6或7个字段）
  const parts = cron.trim().split(/\s+/);
  return parts.length >= 6 && parts.length <= 7;
}

/**
 * 格式化任务执行时间
 */
export function formatExecutionTime(seconds: number): string {
  if (seconds < 60) {
    return `${seconds}秒`;
  } else if (seconds < 3600) {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return remainingSeconds > 0 ? `${minutes}分${remainingSeconds}秒` : `${minutes}分钟`;
  } else {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    return minutes > 0 ? `${hours}小时${minutes}分钟` : `${hours}小时`;
  }
}

/**
 * 生成任务组执行批次ID
 */
export function generateExecutionBatchId(): string {
  return `${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * 验证任务表单
 */
export function validateTaskForm(form: JobInfoForm): string[] {
  const errors: string[] = [];
  
  if (!form.jobGroup) {
    errors.push('请选择执行器');
  }
  
  if (!form.jobDesc?.trim()) {
    errors.push('请输入任务描述');
  }
  
  if (!form.scheduleType) {
    errors.push('请选择调度类型');
  }
  
  if (!form.scheduleConf?.trim()) {
    errors.push('请输入调度配置');
  }
  
  if (form.scheduleType === 'CRON' && !validateCronExpression(form.scheduleConf || '')) {
    errors.push('CRON表达式格式错误');
  }
  
  return errors;
}

/**
 * 深拷贝对象
 */
export function deepClone<T>(obj: T): T {
  return JSON.parse(JSON.stringify(obj));
}

/**
 * 防抖函数
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number = 300
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout | null = null;
  
  return function(this: any, ...args: Parameters<T>) {
    if (timeout) {
      clearTimeout(timeout);
    }
    
    timeout = setTimeout(() => {
      func.apply(this, args);
    }, wait);
  };
}

/**
 * 节流函数
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  wait: number = 300
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout | null = null;
  let previous = 0;
  
  return function(this: any, ...args: Parameters<T>) {
    const now = Date.now();
    const remaining = wait - (now - previous);
    
    if (remaining <= 0 || remaining > wait) {
      if (timeout) {
        clearTimeout(timeout);
        timeout = null;
      }
      previous = now;
      func.apply(this, args);
    } else if (!timeout) {
      timeout = setTimeout(() => {
        previous = Date.now();
        timeout = null;
        func.apply(this, args);
      }, remaining);
    }
  };
}


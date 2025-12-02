/**
 * 任务操作组合函数
 * 
 * <p>封装任务的常用操作（启动、停止、触发等）
 * 
 * @author cc-job-team
 */

import { ElMessage, ElMessageBox } from 'element-plus';
import JobInfoAPI from '@/api/task/job-info';
import { MESSAGE_CONFIG } from '@/constants';

export function useTaskOperation() {
  
  /**
   * 启动任务
   */
  const startTask = async (taskId: number, taskName: string) => {
    try {
      await ElMessageBox.confirm(
        `确定要启动任务【${taskName}】吗？`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      
      await JobInfoAPI.startJob(taskId);
      ElMessage.success({
        message: '任务启动成功',
        ...MESSAGE_CONFIG
      });
      
      return true;
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error({
          message: error.message || '任务启动失败',
          ...MESSAGE_CONFIG
        });
      }
      return false;
    }
  };
  
  /**
   * 停止任务
   */
  const stopTask = async (taskId: number, taskName: string) => {
    try {
      await ElMessageBox.confirm(
        `确定要停止任务【${taskName}】吗？`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      
      await JobInfoAPI.stopJob(taskId);
      ElMessage.success({
        message: '任务停止成功',
        ...MESSAGE_CONFIG
      });
      
      return true;
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error({
          message: error.message || '任务停止失败',
          ...MESSAGE_CONFIG
        });
      }
      return false;
    }
  };
  
  /**
   * 触发任务执行一次
   */
  const triggerTask = async (taskId: number, executorParam: string = '') => {
    try {
      await JobInfoAPI.triggerJob({
        id: taskId,
        executorParam: executorParam
      });
      
      ElMessage.success({
        message: '任务触发成功',
        ...MESSAGE_CONFIG
      });
      
      return true;
    } catch (error: any) {
      ElMessage.error({
        message: error.message || '任务触发失败',
        ...MESSAGE_CONFIG
      });
      return false;
    }
  };
  
  /**
   * 停止任务组
   */
  const stopTaskGroup = async (taskId: number, randomId: string, taskName: string) => {
    try {
      await ElMessageBox.confirm(
        `确定要停止任务组【${taskName}】吗？`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      
      await JobInfoAPI.stopJobCompose(taskId, randomId);
      ElMessage.success({
        message: '任务组停止成功',
        ...MESSAGE_CONFIG
      });
      
      return true;
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error({
          message: error.message || '任务组停止失败',
          ...MESSAGE_CONFIG
        });
      }
      return false;
    }
  };
  
  /**
   * 暂停/恢复任务
   */
  const pauseTask = async (taskId: number, isPause: number, taskName: string) => {
    const action = isPause === 1 ? '暂停' : '恢复';
    
    try {
      await ElMessageBox.confirm(
        `确定要${action}任务【${taskName}】吗？`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      
      await JobInfoAPI.pauseJob(taskId, isPause);
      ElMessage.success({
        message: `任务${action}成功`,
        ...MESSAGE_CONFIG
      });
      
      return true;
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error({
          message: error.message || `任务${action}失败`,
          ...MESSAGE_CONFIG
        });
      }
      return false;
    }
  };
  
  /**
   * 删除任务
   */
  const deleteTask = async (taskIds: string, taskName?: string) => {
    try {
      const message = taskName 
        ? `确定要删除任务【${taskName}】吗？` 
        : `确定要删除选中的任务吗？`;
        
      await ElMessageBox.confirm(message, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      });
      
      await JobInfoAPI.deleteByIds(taskIds);
      ElMessage.success({
        message: '任务删除成功',
        ...MESSAGE_CONFIG
      });
      
      return true;
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error({
          message: error.message || '任务删除失败',
          ...MESSAGE_CONFIG
        });
      }
      return false;
    }
  };
  
  return {
    startTask,
    stopTask,
    triggerTask,
    stopTaskGroup,
    pauseTask,
    deleteTask
  };
}


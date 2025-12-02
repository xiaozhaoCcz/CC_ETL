/**
 * 任务表单 Composable
 * 
 * <p>封装任务表单的通用逻辑
 * 
 * @author cc-job-team
 */

import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import type { JobInfoForm } from '@/api/task/job-info';
import { validateTaskForm } from '@/utils/taskHelper';

export function useTaskForm() {
  const formData = reactive<JobInfoForm>({
    jobGroup: undefined,
    jobDesc: '',
    author: '',
    alarmEmail: '',
    scheduleType: 'CRON',
    scheduleConf: '',
    misfireStrategy: 'DO_NOTHING',
    executorRouteStrategy: 'FIRST',
    executorHandler: '',
    executorParam: '',
    executorBlockStrategy: 'SERIAL_EXECUTION',
    executorTimeout: 0,
    executorFailRetryCount: 0,
    glueType: 'BEAN',
  });
  
  const loading = ref(false);
  
  /**
   * 重置表单
   */
  const resetForm = () => {
    Object.assign(formData, {
      jobGroup: undefined,
      jobDesc: '',
      author: '',
      alarmEmail: '',
      scheduleType: 'CRON',
      scheduleConf: '',
      misfireStrategy: 'DO_NOTHING',
      executorRouteStrategy: 'FIRST',
      executorHandler: '',
      executorParam: '',
      executorBlockStrategy: 'SERIAL_EXECUTION',
      executorTimeout: 0,
      executorFailRetryCount: 0,
      glueType: 'BEAN',
    });
  };
  
  /**
   * 填充表单数据
   */
  const fillForm = (data: JobInfoForm) => {
    Object.assign(formData, data);
  };
  
  /**
   * 验证表单
   */
  const validateForm = (): boolean => {
    const errors = validateTaskForm(formData);
    
    if (errors.length > 0) {
      ElMessage.error(errors[0]);
      return false;
    }
    
    return true;
  };
  
  /**
   * 获取表单数据
   */
  const getFormData = (): JobInfoForm => {
    return { ...formData };
  };
  
  return {
    formData,
    loading,
    resetForm,
    fillForm,
    validateForm,
    getFormData,
  };
}


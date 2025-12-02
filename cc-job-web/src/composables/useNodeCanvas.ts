/**
 * 节点画布 Composable
 * 
 * <p>封装节点画布的通用逻辑
 * 
 * @author cc-job-team
 */

import { ref, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';

export function useNodeCanvas() {
  const lfRef = ref(null);
  const lf = ref<any>(null);
  
  /**
   * 初始化画布
   */
  const initCanvas = (container: HTMLElement, config?: any) => {
    try {
      // LogicFlow 初始化逻辑
      // 这里需要根据实际使用的库进行初始化
      console.log('初始化画布', container, config);
      
      return true;
    } catch (error) {
      console.error('初始化画布失败', error);
      ElMessage.error('初始化画布失败');
      return false;
    }
  };
  
  /**
   * 清空画布
   */
  const clearCanvas = () => {
    if (!lf.value) {
      return;
    }
    
    try {
      lf.value.clearData();
      ElMessage.success('画布已清空');
    } catch (error) {
      console.error('清空画布失败', error);
      ElMessage.error('清空画布失败');
    }
  };
  
  /**
   * 获取画布数据
   */
  const getCanvasData = () => {
    if (!lf.value) {
      return null;
    }
    
    try {
      return lf.value.getGraphData();
    } catch (error) {
      console.error('获取画布数据失败', error);
      return null;
    }
  };
  
  /**
   * 渲染画布数据
   */
  const renderCanvasData = (data: any) => {
    if (!lf.value || !data) {
      return false;
    }
    
    try {
      lf.value.render(data);
      return true;
    } catch (error) {
      console.error('渲染画布数据失败', error);
      ElMessage.error('渲染画布数据失败');
      return false;
    }
  };
  
  /**
   * 适配画布视图
   */
  const fitView = () => {
    if (!lf.value) {
      return;
    }
    
    try {
      lf.value.fitView();
    } catch (error) {
      console.error('适配视图失败', error);
    }
  };
  
  /**
   * 缩放画布
   */
  const zoom = (zoomSize: number) => {
    if (!lf.value) {
      return;
    }
    
    try {
      if (zoomSize > 0) {
        lf.value.zoom(zoomSize);
      } else {
        lf.value.resetZoom();
      }
    } catch (error) {
      console.error('缩放画布失败', error);
    }
  };
  
  /**
   * 销毁画布
   */
  const destroyCanvas = () => {
    if (lf.value) {
      try {
        lf.value.destroy();
        lf.value = null;
      } catch (error) {
        console.error('销毁画布失败', error);
      }
    }
  };
  
  // 组件卸载时销毁画布
  onUnmounted(() => {
    destroyCanvas();
  });
  
  return {
    lfRef,
    lf,
    initCanvas,
    clearCanvas,
    getCanvasData,
    renderCanvasData,
    fitView,
    zoom,
    destroyCanvas,
  };
}


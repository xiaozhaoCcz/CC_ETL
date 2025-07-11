import { store } from "@/store";

import { defineStore } from "pinia";
import { ref } from "vue";

export const useJobInfoStore = defineStore("jobInfo", () => {
  const jobInfo = ref({});
  const treeNeedsRefresh = ref(false);

  function setJobInfo(data: any) {
    jobInfo.value = data;
  }

  function getJobInfo() {
    return jobInfo.value;
  }

  /**
   * 触发任务树刷新事件
   * 当新增、编辑或删除任务后调用此方法
   */
  function triggerTreeRefresh() {
    treeNeedsRefresh.value = true;
    // 重置标志，以便下次能够再次触发
    setTimeout(() => {
      treeNeedsRefresh.value = false;
    }, 100);
  }

  /**
   * 获取任务树是否需要刷新的标志
   */
  function getTreeRefreshState() {
    return treeNeedsRefresh.value;
  }

  return {
    getJobInfo,
    setJobInfo,
    triggerTreeRefresh,
    getTreeRefreshState,
  };
});

/**
 * 用于在组件外部（如在Pinia Store 中）使用 Pinia 提供的 store 实例。
 * 官方文档解释了如何在组件外部使用 Pinia Store：
 * https://pinia.vuejs.org/core-concepts/outside-component-usage.html#using-a-store-outside-of-a-component
 */
export function useJobInfoStoreHook() {
  return useJobInfoStore(store);
}

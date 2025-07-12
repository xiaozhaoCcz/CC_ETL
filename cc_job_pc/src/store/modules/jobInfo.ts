import { store } from "@/store";

import { defineStore } from "pinia";
import { ref } from "vue";

export const useJobInfoStore = defineStore("jobInfo", () => {
  const jobInfo = ref({});
  const treeNeedsRefresh = ref(false);
  const nodeToDelete = ref(null as number | null);
  const nodeToEdit = ref({});

  function setJobInfo(data: any) {
    jobInfo.value = data;
  }

  function getJobInfo() {
    return jobInfo.value;
  }

  function setNodeToDelete(nodeId: number) {
    nodeToDelete.value = nodeId;
  }

  function clearNodeToDelete() {
    nodeToDelete.value = null;
  }

  function getNodeToDelete() {
    return nodeToDelete.value;
  }

  function getNodeToEdit() {
    return nodeToEdit.value;
  }

  function setNodeToEdit(node: object) {
    nodeToEdit.value = node;
  }

  function clearNodeToEdit() {
    nodeToEdit.value = {};
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
    setNodeToDelete,
    clearNodeToDelete,
    getNodeToDelete,
    getNodeToEdit,
    setNodeToEdit,
    clearNodeToEdit,
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

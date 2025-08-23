import { store } from "@/store";

import { defineStore } from "pinia";
import { ref } from "vue";

export const usePageStore = defineStore("page", () => {
  const pages = ref<Array<any>>([]);
  const currentPage = ref<number>(0);
  const isRunning = ref<boolean>(false);
  // 为每个任务组维护独立的运行状态
  const pageRunningStatus = ref<Map<number, boolean>>(new Map());

  const loactionObject = ref<object>({});

  // 新增：剪贴板状态管理
  const clipboard = ref<{
    nodes: any[];
    edges: any[];
    timestamp: number;
  } | null>(null);

  function addPage(data: any) {
    currentPage.value = data.id;
    const p = pages.value.find((item) => item.id === data.id);
    if (!p) {
      pages.value.push(data);
      // 初始化新任务组的运行状态为false
      pageRunningStatus.value.set(data.id, false);
    }
  }

  function removePage(id: number) {
    pages.value = pages.value.filter((item) => item.id != id);
    // 清理对应的运行状态
    pageRunningStatus.value.delete(id);
  }

  function getPageById(id: number) {
    return pages.value.find((item) => item.id === id);
  }

  function getPages() {
    return pages.value;
  }

  function getPageSize() {
    return pages.value.length;
  }

  function getCurrentPage() {
    return currentPage.value;
  }

  function setCurrentPage(id: number) {
    currentPage.value = id;
  }

  function getLastPage() {
    const page = pages.value[pages.value.length - 1];
    if (page) {
      currentPage.value = page.id;
    } else {
      currentPage.value = 0;
    }
  }

  function updateRunStatus(status: boolean) {
    isRunning.value = status;
  }

  // 新增：更新指定任务组的运行状态
  function updatePageRunStatus(pageId: number, status: boolean) {
    pageRunningStatus.value.set(pageId, status);
  }

  // 新增：获取指定任务组的运行状态
  function getPageRunStatus(pageId: number): boolean {
    return pageRunningStatus.value.get(pageId) || false;
  }

  // 新增：获取当前任务组的运行状态
  function getCurrentPageRunStatus(): boolean {
    return pageRunningStatus.value.get(currentPage.value) || false;
  }

  function setLoactionObject(object: object) {
    loactionObject.value = object;
  }

  function getLoactionObject() {
    return loactionObject.value;
  }

  function clearLoactionObject() {
    loactionObject.value = {};
  }

  // 新增：设置剪贴板内容
  function setClipboard(nodes: any[], edges: any[]) {
    clipboard.value = {
      nodes: JSON.parse(JSON.stringify(nodes)), // 深拷贝
      edges: JSON.parse(JSON.stringify(edges)), // 深拷贝
      timestamp: Date.now(),
    };
  }

  // 新增：获取剪贴板内容
  function getClipboard() {
    return clipboard.value;
  }

  // 新增：清空剪贴板
  function clearClipboard() {
    clipboard.value = null;
  }

  // 新增：检查剪贴板是否为空
  function isClipboardEmpty(): boolean {
    return !clipboard.value || 
           (!clipboard.value.nodes.length && !clipboard.value.edges.length);
  }

  // 新增：检查剪贴板是否过期（超过1小时）
  function isClipboardExpired(): boolean {
    if (!clipboard.value) return true;
    const oneHour = 60 * 60 * 1000; // 1小时的毫秒数
    return Date.now() - clipboard.value.timestamp > oneHour;
  }

  return {
    pages,
    addPage,
    removePage,
    getPageById,
    getPages,
    getPageSize,
    getCurrentPage,
    setCurrentPage,
    getLastPage,
    updateRunStatus,
    isRunning,
    updatePageRunStatus,
    getPageRunStatus,
    getCurrentPageRunStatus,
    setLoactionObject,
    getLoactionObject,
    clearLoactionObject,
    // 剪贴板相关方法
    setClipboard,
    getClipboard,
    clearClipboard,
    isClipboardEmpty,
    isClipboardExpired,
  };
});

/**
 * 用于在组件外部（如在Pinia Store 中）使用 Pinia 提供的 store 实例。
 * 官方文档解释了如何在组件外部使用 Pinia Store：
 * https://pinia.vuejs.org/core-concepts/outside-component-usage.html#usage-outside-of-components.html
 */
export const usePageStoreHook = () => usePageStore(store);

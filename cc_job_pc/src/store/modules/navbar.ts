import { store } from "@/store";

import { defineStore } from "pinia";
import { ref } from "vue";

export const useNavbarStore = defineStore("navbar", () => {
    const actionObj = ref<Object>({
        actionName: "",
        nowDate: null
    })

    // 添加侧边栏显示状态
    const sideVisible = ref<boolean>(true);

    function setAction(data: Object) {
        actionObj.value = data;
    }

    function getAction() {
        return actionObj.value;
    }

    // 侧边栏显示/隐藏相关方法
    function setSideVisible(visible: boolean) {
        sideVisible.value = visible;
    }

    function getSideVisible() {
        return sideVisible.value;
    }

    function toggleSideVisible() {
        sideVisible.value = !sideVisible.value;
    }

    return {
        setAction,
        getAction,
        setSideVisible,
        getSideVisible,
        toggleSideVisible,
    };
});

/**
 * 用于在组件外部（如在Pinia Store 中）使用 Pinia 提供的 store 实例。
 * 官方文档解释了如何在组件外部使用 Pinia Store：
 * https://pinia.vuejs.org/core-concepts/outside-component-usage.html#using-a-store-outside-of-a-component
 */
export function useNavbarStoreHook() {
    return useNavbarStore(store);
}
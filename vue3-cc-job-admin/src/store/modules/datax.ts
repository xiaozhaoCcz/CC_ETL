import { store } from "@/store";

export const useDataxStore = defineStore("datax", () => {
  const dataxGroups = useStorage("dataxGroups", {
    readers: {},
    writers: {},
    formData: {},
  });

  function clearDataxGroups() {
    dataxGroups.value.readers = {};
    dataxGroups.value.writers = {};
    dataxGroups.value.formData = {};
  }

  return {
    dataxGroups,
    clearDataxGroups,
  };
});

export function useDataxStoreHook() {
  return useDataxStore(store);
}

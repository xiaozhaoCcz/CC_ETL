import type { App } from "vue";
import { createPinia } from "pinia";

const store = createPinia();

// 全局注册 store
export function setupStore(app: App<Element>) {
  app.use(store);
}

export * from "./modules/user";
export * from "./modules/page";
export * from "./modules/jobInfo";
export * from "./modules/navbar";
export { store };

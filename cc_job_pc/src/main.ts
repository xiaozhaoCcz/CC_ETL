import { createApp } from "vue";
import "./style.css";
import App from "./App.vue";

// 扩展Window接口
declare global {
    interface Window {
        __VUE_APP__?: any;
    }
}

const app = createApp(App);

import router from "./router/index";

import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import { InstallCodeMirror } from "codemirror-editor-vue3";
import resizeDialog from '@/directives/resizeDialog'
import '@/utils/dialogResizePatch'

app.use(router);
app.use(ElementPlus);
app.use(InstallCodeMirror);
app.directive('resize-dialog', resizeDialog)

// 添加调试信息
console.log('=== Vue App Starting ===');
console.log('App element exists:', !!document.getElementById('app'));
console.log('Router ready:', !!router);

// 挂载应用
const mountedApp = app.mount("#app");

// 添加全局引用用于调试
window.__VUE_APP__ = mountedApp;

console.log('=== Vue App Mounted ===');
console.log('Mounted app:', mountedApp);
console.log('App element after mount:', document.getElementById('app'));
console.log('App element children:', document.getElementById('app')?.children?.length || 0);

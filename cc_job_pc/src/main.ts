import { createApp } from "vue";
import "./style.css";
import App from "./App.vue";

// 扩展Window接口
declare global {
    interface Window {
        __VUE_APP__?: any;
    }
}

// 性能监控
import { PerformanceMonitor, preloadResource } from "@/utils/performance";

// 创建性能监控实例
const performanceMonitor = new PerformanceMonitor();

// 预加载关键资源
const preloadCriticalResources = async () => {
    try {
        // 预加载Element Plus样式
        await preloadResource('https://unpkg.com/element-plus/dist/index.css', 'style');
        
        // 预加载常用图标
        await preloadResource('https://unpkg.com/@element-plus/icons-vue@2.3.1/dist/index.js', 'script');
        
        console.log('关键资源预加载完成');
    } catch (error) {
        console.warn('资源预加载失败:', error);
    }
};

// 启动预加载
preloadCriticalResources();

const app = createApp(App);

import router from "./router/index";

import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import { InstallCodeMirror } from "codemirror-editor-vue3";
import resizeDialog from '@/directives/resizeDialog'
import '@/utils/dialogResizePatch'

// 导入性能优化指令
import { performanceDirectives } from '@/directives/performance';

// 导入智能HMR插件
import smartHMR from '@/plugins/smart-hmr';

app.use(router);
app.use(ElementPlus);
app.use(InstallCodeMirror);
app.use(smartHMR);

// 注册性能优化指令
app.directive('resize-dialog', resizeDialog);
Object.entries(performanceDirectives).forEach(([name, directive]) => {
    app.directive(name, directive);
});

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

// 应用启动完成后的性能优化
setTimeout(() => {
    // 清理不必要的内存
    if ('gc' in window) {
        (window as any).gc();
    }
    
    // 输出性能指标
    const memoryUsage = performanceMonitor.getMemoryUsage();
    console.log('内存使用情况:', memoryUsage);
    
    // 输出性能指标
    const metrics = performanceMonitor.getMetrics();
    console.log('性能指标:', metrics);
}, 1000);

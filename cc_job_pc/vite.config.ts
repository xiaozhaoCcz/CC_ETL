import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { resolve } from "path";

// https://vitejs.dev/config/
export default defineConfig({
  base: './', // 设置为相对路径，适配Electron
  plugins: [
    vue({
      // 优化Vue组件的热更新
      include: [/\.vue$/],
    }),
  ],
  resolve: {
    alias: {
      "@": resolve(__dirname, "./src"),
    },
    extensions: [".mjs", ".js", ".ts", ".jsx", ".tsx", ".json", ".vue"],
  },
  server: {
    // // 热更新配置 - 完全禁用HMR
    // hmr: false,
    // 热更新配置
    hmr: {
      timeout: 3000,
      // 关闭错误覆盖层
      overlay: {
        warnings: false,
        errors: true,
      },
      // 设置端口
      port: 24678,
    },
    // 文件监听配置
    watch: {
      // 忽略不必要的文件和目录
      ignored: [
        "**/node_modules/**",
        "**/.git/**",
        "**/dist/**",
        "**/.vscode/**",
        "**/.idea/**",
        "**/coverage/**",
        "**/*.log",
        "**/.DS_Store",
        "**/Thumbs.db",
      ],
      //待研究
      // // 使用原生文件监听，更高效
      // usePolling: false,
      // // 增加聚合时间，减少频繁触发
      // aggregateTimeout: 5000,
      // // 忽略初始扫描
      // ignoreInitial: true,
      // // 监听间隔
      // interval: 1500,
    },
    // 设置端口和主机
    port: 3000,
    host: "0.0.0.0",
    // 自动打开浏览器
    open: true,
    // 启用 CORS
    cors: true,
    // 预热文件
    warmup: {
      clientFiles: ["./src/views/main/main.vue", "./src/components/**/*.vue"],
    },
  },
  // 构建优化
  build: {
    // 启用源码映射，便于调试
    sourcemap: true,
    // 代码分割阈值
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        // 手动分割代码块
        manualChunks: {
          logicflow: ["@logicflow/core", "@logicflow/extension"],
          "element-plus": ["element-plus"],
          "vue-vendor": ["vue", "vue-router", "pinia"],
        },
      },
    },
  },
  // 依赖优化
  optimizeDeps: {
    // 预构建依赖，提高开发服务器启动速度
    include: [
      "vue",
      "vue-router",
      "pinia",
      "element-plus",
      "@logicflow/core",
      "@logicflow/extension",
      "@element-plus/icons-vue",
    ],
    // 不强制预构建，让 Vite 自动判断
    force: false,
  },
  // CSS 配置
  css: {
    // 启用 CSS 源码映射
    devSourcemap: true,
    // CSS 预处理器配置
    preprocessorOptions: {
      scss: {
        // 减少 SCSS 编译时间
        charset: false,
        additionalData: `$injectedColor: orange;`,
      },
    },
  },
  // 日志级别设置为 info，但过滤掉不重要的日志
  logLevel: "info",
  // 清屏设置为 false，保留历史日志
  clearScreen: false,
  // 定义全局常量
  define: {
    __DEV__: JSON.stringify(process.env.NODE_ENV === "development"),
    __VERSION__: JSON.stringify(process.env.npm_package_version),
  },
});

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  base: './',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    // 启用代码分割优化
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router'],
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          'logicflow': ['@logicflow/core', '@logicflow/extension'],
          'monaco': ['monaco-editor', '@monaco-editor/loader'],
          'codemirror': ['codemirror', 'codemirror-editor-vue3'],
          'utils': ['axios', '@vueuse/core'],
          'pinia': ['pinia', 'pinia-plugin-persistedstate'],
        },
        // 优化chunk大小
        chunkFileNames: 'assets/js/[name]-[hash].js',
        entryFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
      },
    },
    // 启用压缩优化
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
        pure_funcs: ['console.log', 'console.info'],
        passes: 2,
      },
      mangle: {
        safari10: true,
      },
    },
    // 启用CSS代码分割
    cssCodeSplit: true,
    // 设置chunk大小警告阈值
    chunkSizeWarningLimit: 1000,
    // 启用源码映射（生产环境可关闭）
    sourcemap: false,
    // 启用目标优化
    target: 'es2015',
    // 启用模块预加载
    modulePreload: {
      polyfill: false,
    },
  },
  server: {
    host: '0.0.0.0',
    port: 3000,
    // 启用HMR优化
    hmr: {
      overlay: false,
    },
    // 启用预构建优化
    force: false,
  },
  // 优化依赖预构建
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'element-plus',
      '@element-plus/icons-vue',
      '@logicflow/core',
      '@logicflow/extension',
      'monaco-editor',
      'codemirror',
      'axios',
      '@vueuse/core',
      'pinia',
      'pinia-plugin-persistedstate',
    ],
    exclude: ['@monaco-editor/loader'],
  },
  // 启用CSS优化
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@import "@/style/variables.scss";`,
      },
    },
  },
})

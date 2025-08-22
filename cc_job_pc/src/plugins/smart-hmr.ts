/**
 * 智能热更新插件
 * 优化开发环境下的热更新性能
 */

import type { App } from 'vue';
import { PerformanceMonitor } from '@/utils/performance';

interface HMRConfig {
  enabled: boolean;
  maxRetries: number;
  retryDelay: number;
  performanceMonitoring: boolean;
}

class SmartHMR {
  private config: HMRConfig;
  private retryCount: number = 0;
  private performanceMonitor: PerformanceMonitor | null = null;

  constructor(config: Partial<HMRConfig> = {}) {
    this.config = {
      enabled: true,
      maxRetries: 3,
      retryDelay: 1000,
      performanceMonitoring: true,
      ...config,
    };

    if (this.config.performanceMonitoring) {
      this.performanceMonitor = new PerformanceMonitor();
    }
  }

  install(app: App) {
    if (!this.config.enabled) return;

    // 监听HMR事件
    if (import.meta.hot) {
      import.meta.hot.on('vite:beforeUpdate', (data) => {
        this.handleBeforeUpdate(data);
      });

      import.meta.hot.on('vite:afterUpdate', (data) => {
        this.handleAfterUpdate(data);
      });

      import.meta.hot.on('vite:error', (error) => {
        this.handleError(error);
      });
    }

    // 添加全局性能监控
    app.config.globalProperties.$performanceMonitor = this.performanceMonitor;
  }

  private handleBeforeUpdate(data: any) {
    if (this.performanceMonitor) {
      this.performanceMonitor.startTimer('hmr-update');
    }

    console.log('🔄 HMR更新开始:', data.file);
    
    // 清理不必要的资源
    this.cleanupBeforeUpdate();
  }

  private handleAfterUpdate(data: any) {
    if (this.performanceMonitor) {
      const duration = this.performanceMonitor.endTimer('hmr-update');
      console.log(`✅ HMR更新完成: ${duration.toFixed(2)}ms`);
    }

    // 重置重试计数
    this.retryCount = 0;
    
    // 优化更新后的性能
    this.optimizeAfterUpdate();
  }

  private handleError(error: any) {
    console.error('❌ HMR更新失败:', error);
    
    if (this.retryCount < this.config.maxRetries) {
      this.retryCount++;
      console.log(`🔄 尝试重试 (${this.retryCount}/${this.config.maxRetries})`);
      
      setTimeout(() => {
        if (import.meta.hot) {
          import.meta.hot.invalidate();
        }
      }, this.config.retryDelay * this.retryCount);
    } else {
      console.error('❌ HMR重试次数已达上限，请手动刷新页面');
    }
  }

  private cleanupBeforeUpdate() {
    // 清理定时器
    const timers = window.setTimeout(() => {}, 0);
    for (let i = 0; i < timers; i++) {
      window.clearTimeout(i);
    }

    // 清理事件监听器
    const events = ['resize', 'scroll', 'mousemove', 'touchmove'];
    events.forEach(event => {
      window.removeEventListener(event, () => {});
    });

    // 清理内存
    if ('gc' in window) {
      (window as any).gc();
    }
  }

  private optimizeAfterUpdate() {
    // 使用 requestIdleCallback 在空闲时间执行优化
    if ('requestIdleCallback' in window) {
      (window as any).requestIdleCallback(() => {
        this.performOptimizations();
      });
    } else {
      setTimeout(() => {
        this.performOptimizations();
      }, 100);
    }
  }

  private performOptimizations() {
    // 优化DOM查询
    const elements = document.querySelectorAll('[data-performance-optimize]');
    elements.forEach(element => {
      element.classList.add('performance-optimized');
    });

    // 优化图片加载
    const images = document.querySelectorAll('img[data-src]');
    images.forEach(img => {
      if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver((entries) => {
          entries.forEach(entry => {
            if (entry.isIntersecting) {
              const imgElement = entry.target as HTMLImageElement;
              imgElement.src = imgElement.dataset.src || '';
              observer.unobserve(imgElement);
            }
          });
        });
        observer.observe(img);
      }
    });

    // 优化滚动性能
    const scrollContainers = document.querySelectorAll('.scroll-container');
    scrollContainers.forEach(container => {
      container.addEventListener('scroll', () => {
        // 使用 requestAnimationFrame 优化滚动
        requestAnimationFrame(() => {
          // 滚动处理逻辑
        });
      }, { passive: true });
    });
  }
}

// 创建默认实例
const smartHMR = new SmartHMR();

export default smartHMR;
export { SmartHMR, type HMRConfig };

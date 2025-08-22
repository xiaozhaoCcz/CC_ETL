/**
 * 性能优化指令
 * 提供各种性能优化相关的Vue指令
 */

import type { Directive, DirectiveBinding } from 'vue';
import { debounce, throttle } from '@/utils/performance';

// 防抖指令
export const vDebounce: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value, arg = 'click' } = binding;
    const delay = parseInt(arg) || 300;
    
    if (typeof value === 'function') {
      const debouncedFn = debounce(value, delay);
      el._debouncedHandler = debouncedFn;
      el.addEventListener(arg, debouncedFn);
    }
  },
  
  unmounted(el: HTMLElement, binding: DirectiveBinding) {
    const { arg = 'click' } = binding;
    if (el._debouncedHandler) {
      el.removeEventListener(arg, el._debouncedHandler);
      delete el._debouncedHandler;
    }
  }
};

// 节流指令
export const vThrottle: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value, arg = 'scroll' } = binding;
    const limit = parseInt(arg) || 100;
    
    if (typeof value === 'function') {
      const throttledFn = throttle(value, limit);
      el._throttledHandler = throttledFn;
      el.addEventListener(arg, throttledFn, { passive: true });
    }
  },
  
  unmounted(el: HTMLElement, binding: DirectiveBinding) {
    const { arg = 'scroll' } = binding;
    if (el._throttledHandler) {
      el.removeEventListener(arg, el._throttledHandler);
      delete el._throttledHandler;
    }
  }
};

// 懒加载指令
export const vLazy: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding;
    
    if (el.tagName === 'IMG') {
      const img = el as HTMLImageElement;
      img.dataset.src = value;
      img.classList.add('lazy');
      
      // 使用 Intersection Observer
      if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver((entries) => {
          entries.forEach(entry => {
            if (entry.isIntersecting) {
              const imgElement = entry.target as HTMLImageElement;
              imgElement.src = imgElement.dataset.src || '';
              imgElement.classList.remove('lazy');
              observer.unobserve(imgElement);
            }
          });
        });
        
        observer.observe(img);
        el._lazyObserver = observer;
      }
    }
  },
  
  unmounted(el: HTMLElement) {
    if (el._lazyObserver) {
      el._lazyObserver.disconnect();
      delete el._lazyObserver;
    }
  }
};

// 性能优化指令
export const vPerformance: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding;
    
    // 启用硬件加速
    el.style.transform = 'translateZ(0)';
    el.style.willChange = 'transform';
    
    // 添加性能优化类
    el.classList.add('performance-optimized');
    
    // 根据值设置不同的优化策略
    if (value === 'scroll') {
      el.style.overflow = 'auto';
      el.style.webkitOverflowScrolling = 'touch';
    } else if (value === 'animation') {
      el.style.willChange = 'transform, opacity';
    }
  }
};

// 虚拟滚动指令
export const vVirtualScroll: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding;
    const { itemHeight = 50, visibleItems = 10 } = value || {};
    
    el.classList.add('virtual-scroll-container');
    el.style.position = 'relative';
    el.style.overflow = 'auto';
    
    // 创建虚拟滚动容器
    const virtualContainer = document.createElement('div');
    virtualContainer.className = 'virtual-scroll-content';
    virtualContainer.style.position = 'relative';
    virtualContainer.style.height = `${itemHeight * visibleItems}px`;
    
    el.appendChild(virtualContainer);
    el._virtualContainer = virtualContainer;
    el._virtualConfig = { itemHeight, visibleItems };
  },
  
  updated(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding;
    if (value && value.totalItems && el._virtualContainer) {
      const { itemHeight } = el._virtualConfig;
      el._virtualContainer.style.height = `${itemHeight * value.totalItems}px`;
    }
  }
};

// 批量更新指令
export const vBatchUpdate: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding;
    
    if (typeof value === 'function') {
      el._batchUpdateHandler = value;
      
      // 使用 requestAnimationFrame 进行批量更新
      const batchUpdate = () => {
        requestAnimationFrame(() => {
          value();
        });
      };
      
      el.addEventListener('update', batchUpdate);
    }
  },
  
  unmounted(el: HTMLElement) {
    if (el._batchUpdateHandler) {
      el.removeEventListener('update', el._batchUpdateHandler);
      delete el._batchUpdateHandler;
    }
  }
};

// 内存优化指令
export const vMemoryOptimize: Directive = {
  mounted(el: HTMLElement) {
    // 添加内存优化标记
    el.setAttribute('data-memory-optimized', 'true');
    
    // 监听元素移除事件
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        mutation.removedNodes.forEach((node) => {
          if (node === el) {
            // 清理相关资源
            this.cleanup(el);
            observer.disconnect();
          }
        });
      });
    });
    
    observer.observe(document.body, {
      childList: true,
      subtree: true
    });
    
    el._memoryObserver = observer;
  },
  
  unmounted(el: HTMLElement) {
    this.cleanup(el);
  },
  
  cleanup(el: HTMLElement) {
    // 清理事件监听器
    const events = ['click', 'scroll', 'resize', 'mousemove'];
    events.forEach(event => {
      el.removeEventListener(event, () => {});
    });
    
    // 清理定时器
    if (el._timers) {
      el._timers.forEach(timer => clearTimeout(timer));
      delete el._timers;
    }
    
    // 清理观察器
    if (el._memoryObserver) {
      el._memoryObserver.disconnect();
      delete el._memoryObserver;
    }
  }
};

// 导出所有指令
export const performanceDirectives = {
  debounce: vDebounce,
  throttle: vThrottle,
  lazy: vLazy,
  performance: vPerformance,
  virtualScroll: vVirtualScroll,
  batchUpdate: vBatchUpdate,
  memoryOptimize: vMemoryOptimize,
};

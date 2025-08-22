/**
 * 性能优化工具函数
 */

import { defineAsyncComponent } from 'vue';

// 防抖函数
export function debounce<T extends (...args: any[]) => any>(
    func: T,
    wait: number,
    immediate = false
): (...args: Parameters<T>) => void {
    let timeout: NodeJS.Timeout | null = null;

    return function executedFunction(...args: Parameters<T>) {
        const later = () => {
            timeout = null;
            if (!immediate) func(...args);
        };

        const callNow = immediate && !timeout;

        if (timeout) clearTimeout(timeout);
        timeout = setTimeout(later, wait);

        if (callNow) func(...args);
    };
}

// 节流函数
export function throttle<T extends (...args: any[]) => any>(
    func: T,
    limit: number
): (...args: Parameters<T>) => void {
    let inThrottle: boolean;

    return function executedFunction(...args: Parameters<T>) {
        if (!inThrottle) {
            func(...args);
            inThrottle = true;
            setTimeout(() => (inThrottle = false), limit);
        }
    };
}

// 虚拟滚动工具类
export class VirtualScroller {
    private container: HTMLElement;
    private itemHeight: number;
    private totalItems: number;
    private visibleItems: number;
    private scrollTop: number = 0;
    private startIndex: number = 0;
    private endIndex: number = 0;

    constructor(
        container: HTMLElement,
        itemHeight: number,
        totalItems: number,
        visibleItems: number
    ) {
        this.container = container;
        this.itemHeight = itemHeight;
        this.totalItems = totalItems;
        this.visibleItems = visibleItems;
        this.init();
    }

    private init() {
        this.container.style.position = 'relative';
        this.container.style.overflow = 'auto';
        this.updateVisibleRange();
        this.container.addEventListener('scroll', this.handleScroll.bind(this));
    }

    private handleScroll() {
        this.scrollTop = this.container.scrollTop;
        this.updateVisibleRange();
    }

    private updateVisibleRange() {
        this.startIndex = Math.floor(this.scrollTop / this.itemHeight);
        this.endIndex = Math.min(
            this.startIndex + this.visibleItems + 1,
            this.totalItems
        );
    }

    public getVisibleRange() {
        return {
            startIndex: this.startIndex,
            endIndex: this.endIndex,
            startOffset: this.startIndex * this.itemHeight,
        };
    }

    public updateTotalItems(totalItems: number) {
        this.totalItems = totalItems;
        this.container.style.height = `${totalItems * this.itemHeight}px`;
    }
}

// 图片懒加载
export function createImageLazyLoader() {
    const imageObserver = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                const img = entry.target as HTMLImageElement;
                img.src = img.dataset.src || '';
                img.classList.remove('lazy');
                imageObserver.unobserve(img);
            }
        });
    });

    return {
        observe: (img: HTMLImageElement) => imageObserver.observe(img),
        unobserve: (img: HTMLImageElement) => imageObserver.unobserve(img),
    };
}

// 组件懒加载
export function createLazyComponent(importFn: () => Promise<any>) {
    return defineAsyncComponent({
        loader: importFn,
        loadingComponent: {
            template: `
        <div class="lazy-loading">
          <div class="loading-spinner"></div>
          <div class="loading-text">加载中...</div>
        </div>
      `,
        },
        errorComponent: {
            template: `
        <div class="lazy-error">
          <div class="error-icon">⚠️</div>
          <div class="error-text">加载失败</div>
          <button @click="retry" class="retry-btn">重试</button>
        </div>
      `,
            methods: {
                retry(this: any) {
                    this.$emit('retry');
                },
            },
        },
        delay: 200,
        timeout: 10000,
    });
}

// 性能监控
export class PerformanceMonitor {
    private metrics: Map<string, number[]> = new Map();
    private observers: Map<string, PerformanceObserver> = new Map();

    constructor() {
        this.initObservers();
    }

    private initObservers() {
        // 监控长任务
        if ('PerformanceObserver' in window) {
            const longTaskObserver = new PerformanceObserver((list) => {
                for (const entry of list.getEntries()) {
                    console.warn('检测到长任务:', entry.duration, 'ms');
                }
            });
            longTaskObserver.observe({ entryTypes: ['longtask'] });
            this.observers.set('longtask', longTaskObserver);
        }
    }

    public startTimer(name: string): void {
        performance.mark(`${name}-start`);
    }

    public endTimer(name: string): number {
        performance.mark(`${name}-end`);
        performance.measure(name, `${name}-start`, `${name}-end`);

        const measure = performance.getEntriesByName(name)[0];
        const duration = measure.duration;

        if (!this.metrics.has(name)) {
            this.metrics.set(name, []);
        }
        this.metrics.get(name)!.push(duration);

        // 清理性能标记
        performance.clearMarks(`${name}-start`);
        performance.clearMarks(`${name}-end`);
        performance.clearMeasures(name);

        return duration;
    }

    public getAverageTime(name: string): number {
        const times = this.metrics.get(name);
        if (!times || times.length === 0) return 0;

        return times.reduce((sum, time) => sum + time, 0) / times.length;
    }

    public getMetrics(): Record<string, number> {
        const result: Record<string, number> = {};
        for (const [name, times] of this.metrics) {
            result[name] = this.getAverageTime(name);
        }
        return result;
    }

    public getMemoryUsage(): Record<string, number> {
        if ('memory' in performance) {
            const memory = (performance as any).memory;
            return {
                usedJSHeapSize: memory.usedJSHeapSize,
                totalJSHeapSize: memory.totalJSHeapSize,
                jsHeapSizeLimit: memory.jsHeapSizeLimit,
            };
        }
        return {};
    }

    public destroy(): void {
        this.observers.forEach(observer => observer.disconnect());
        this.observers.clear();
        this.metrics.clear();
    }
}

// 内存优化 - 清理无用引用
export function cleanupReferences(obj: any): void {
    if (obj && typeof obj === 'object') {
        Object.keys(obj).forEach(key => {
            if (obj[key] === null || obj[key] === undefined) {
                delete obj[key];
            } else if (typeof obj[key] === 'object') {
                cleanupReferences(obj[key]);
            }
        });
    }
}

// 批量DOM操作优化
export function batchDOMUpdate(updates: (() => void)[]): void {
    if (updates.length === 0) return;

    // 使用 requestAnimationFrame 确保在下一帧执行
    requestAnimationFrame(() => {
        // 批量执行DOM更新
        updates.forEach(update => update());
    });
}

// 缓存优化
export class SimpleCache<K, V> {
    private cache = new Map<K, V>();
    private maxSize: number;

    constructor(maxSize = 100) {
        this.maxSize = maxSize;
    }

    get(key: K): V | undefined {
        return this.cache.get(key);
    }

    set(key: K, value: V): void {
        if (this.cache.size >= this.maxSize) {
            const firstKey = this.cache.keys().next().value;
            if (firstKey !== undefined) {
                this.cache.delete(firstKey);
            }
        }
        this.cache.set(key, value);
    }

    clear(): void {
        this.cache.clear();
    }

    size(): number {
        return this.cache.size;
    }
}

// 资源预加载
export function preloadResource(url: string, type: 'script' | 'style' | 'image' = 'script'): Promise<void> {
    return new Promise((resolve, reject) => {
        const link = document.createElement('link');
        link.rel = 'preload';
        link.as = type;
        link.href = url;
        
        link.onload = () => resolve();
        link.onerror = () => reject(new Error(`Failed to preload: ${url}`));
        
        document.head.appendChild(link);
    });
}

// 防抖的resize监听器
export function createDebouncedResizeListener(callback: () => void, delay = 150): () => void {
    const debouncedCallback = debounce(callback, delay);
    
    const handleResize = () => {
        requestAnimationFrame(debouncedCallback);
    };
    
    window.addEventListener('resize', handleResize, { passive: true });
    
    return () => {
        window.removeEventListener('resize', handleResize);
    };
}

// 性能标记工具
export class PerformanceMarker {
    private static markers: Map<string, number> = new Map();

    static mark(name: string): void {
        this.markers.set(name, performance.now());
    }

    static measure(name: string, startMark: string, endMark: string): number {
        const start = this.markers.get(startMark);
        const end = this.markers.get(endMark);
        
        if (start && end) {
            const duration = end - start;
            console.log(`${name}: ${duration.toFixed(2)}ms`);
            return duration;
        }
        
        return 0;
    }

    static clear(): void {
        this.markers.clear();
    }
} 
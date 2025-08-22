/**
 * 性能优化配置文件
 * 集中管理所有性能相关的配置选项
 */

export interface PerformanceConfig {
  // 懒加载配置
  lazyLoading: {
    enabled: boolean;
    delay: number;
    timeout: number;
    threshold: number;
  };
  
  // 缓存配置
  cache: {
    enabled: boolean;
    maxSize: number;
    ttl: number;
  };
  
  // 防抖节流配置
  debounceThrottle: {
    defaultDebounceDelay: number;
    defaultThrottleLimit: number;
    scrollThrottleLimit: number;
    resizeThrottleLimit: number;
  };
  
  // 虚拟滚动配置
  virtualScroll: {
    enabled: boolean;
    defaultItemHeight: number;
    defaultVisibleItems: number;
    bufferSize: number;
  };
  
  // 图片懒加载配置
  imageLazyLoading: {
    enabled: boolean;
    threshold: number;
    rootMargin: string;
  };
  
  // 性能监控配置
  monitoring: {
    enabled: boolean;
    logLevel: 'debug' | 'info' | 'warn' | 'error';
    metricsCollection: boolean;
    memoryMonitoring: boolean;
  };
  
  // HMR配置
  hmr: {
    enabled: boolean;
    maxRetries: number;
    retryDelay: number;
    performanceMonitoring: boolean;
  };
  
  // 构建优化配置
  build: {
    minify: boolean;
    sourcemap: boolean;
    chunkSizeWarningLimit: number;
    cssCodeSplit: boolean;
  };
}

// 默认配置
export const defaultPerformanceConfig: PerformanceConfig = {
  lazyLoading: {
    enabled: true,
    delay: 200,
    timeout: 10000,
    threshold: 0.1,
  },
  
  cache: {
    enabled: true,
    maxSize: 100,
    ttl: 5 * 60 * 1000, // 5分钟
  },
  
  debounceThrottle: {
    defaultDebounceDelay: 300,
    defaultThrottleLimit: 100,
    scrollThrottleLimit: 16, // 约60fps
    resizeThrottleLimit: 150,
  },
  
  virtualScroll: {
    enabled: true,
    defaultItemHeight: 50,
    defaultVisibleItems: 10,
    bufferSize: 5,
  },
  
  imageLazyLoading: {
    enabled: true,
    threshold: 0.1,
    rootMargin: '50px',
  },
  
  monitoring: {
    enabled: true,
    logLevel: 'info',
    metricsCollection: true,
    memoryMonitoring: true,
  },
  
  hmr: {
    enabled: true,
    maxRetries: 3,
    retryDelay: 1000,
    performanceMonitoring: true,
  },
  
  build: {
    minify: true,
    sourcemap: false,
    chunkSizeWarningLimit: 1000,
    cssCodeSplit: true,
  },
};

// 开发环境配置
export const devPerformanceConfig: PerformanceConfig = {
  ...defaultPerformanceConfig,
  monitoring: {
    ...defaultPerformanceConfig.monitoring,
    logLevel: 'debug',
    metricsCollection: true,
  },
  build: {
    ...defaultPerformanceConfig.build,
    sourcemap: true,
  },
};

// 生产环境配置
export const prodPerformanceConfig: PerformanceConfig = {
  ...defaultPerformanceConfig,
  monitoring: {
    ...defaultPerformanceConfig.monitoring,
    logLevel: 'warn',
    metricsCollection: false,
  },
  build: {
    ...defaultPerformanceConfig.build,
    sourcemap: false,
  },
};

// 性能配置管理器
export class PerformanceConfigManager {
  private config: PerformanceConfig;
  
  constructor(config?: Partial<PerformanceConfig>) {
    this.config = {
      ...defaultPerformanceConfig,
      ...config,
    };
  }
  
  // 获取配置
  get<K extends keyof PerformanceConfig>(key: K): PerformanceConfig[K] {
    return this.config[key];
  }
  
  // 更新配置
  update<K extends keyof PerformanceConfig>(key: K, value: PerformanceConfig[K]): void {
    this.config[key] = value;
  }
  
  // 获取完整配置
  getAll(): PerformanceConfig {
    return { ...this.config };
  }
  
  // 根据环境获取配置
  static getConfigForEnvironment(env: 'development' | 'production'): PerformanceConfig {
    return env === 'development' ? devPerformanceConfig : prodPerformanceConfig;
  }
}

// 创建全局配置实例
export const performanceConfig = new PerformanceConfigManager(
  PerformanceConfigManager.getConfigForEnvironment(
    import.meta.env.MODE as 'development' | 'production'
  )
);

// 导出便捷方法
export const getPerformanceConfig = <K extends keyof PerformanceConfig>(key: K): PerformanceConfig[K] => {
  return performanceConfig.get(key);
};

export const updatePerformanceConfig = <K extends keyof PerformanceConfig>(key: K, value: PerformanceConfig[K]): void => {
  performanceConfig.update(key, value);
};

/**
 * 日志管理工具类
 * 提供统一的日志处理、状态管理和性能优化
 */

import { ref, computed, watch, nextTick } from "vue";
import { ElMessage } from "element-plus";

/**
 * 日志级别枚举
 */
export enum LogLevel {
    DEBUG = "debug",
    INFO = "info",
    WARNING = "warning",
    ERROR = "error",
}

/**
 * 日志项接口
 */
export interface LogItem {
    id: number;
    time: string;
    message: string;
    level: LogLevel;
    jobId?: number;
    randomId?: string;
    metadata?: Record<string, any>;
}

/**
 * 日志统计信息
 */
export interface LogStats {
    total: number;
    error: number;
    warning: number;
    info: number;
    debug: number;
}

/**
 * 日志配置选项
 */
export interface LogConfig {
    maxLogs: number;
    autoScroll: boolean;
    enableStats: boolean;
    logBufferSize: number;
    flushInterval: number;
    enablePerformance: boolean;
}

/**
 * 日志管理器类
 */
export class LogManager {
    private logs = ref<LogItem[]>([]);
    private logBuffer: string = "";
    private logId = 0;
    private config: LogConfig;
    private flushTimer: NodeJS.Timeout | null = null;
    private performanceMonitor: PerformanceMonitor | null = null;

    constructor(config: Partial<LogConfig> = {}) {
        this.config = {
            maxLogs: 10000,
            autoScroll: true,
            enableStats: true,
            logBufferSize: 1024,
            flushInterval: 100,
            enablePerformance: false,
            ...config,
        };

        if (this.config.enablePerformance) {
            this.performanceMonitor = new PerformanceMonitor();
        }

        this.startFlushTimer();
    }

    /**
     * 获取日志列表
     */
    public getLogs() {
        return this.logs;
    }

    /**
     * 获取日志统计信息
     */
    public getStats(): LogStats {
        const logs = this.logs.value;
        return {
            total: logs.length,
            error: logs.filter(log => log.level === LogLevel.ERROR).length,
            warning: logs.filter(log => log.level === LogLevel.WARNING).length,
            info: logs.filter(log => log.level === LogLevel.INFO).length,
            debug: logs.filter(log => log.level === LogLevel.DEBUG).length,
        };
    }

    /**
     * 添加日志
     */
    public addLog(message: string, level: LogLevel = LogLevel.INFO, metadata?: Record<string, any>): void {
        if (this.performanceMonitor) {
            this.performanceMonitor.startTimer("addLog");
        }

        const logItem: LogItem = {
            id: this.logId++,
            time: new Date().toLocaleTimeString("zh-CN", {
                hour12: false,
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
            }),
            message: message.trim(),
            level,
            metadata,
        };

        this.logs.value.push(logItem);

        // 限制日志数量
        if (this.logs.value.length > this.config.maxLogs) {
            this.logs.value = this.logs.value.slice(-this.config.maxLogs);
        }

        if (this.performanceMonitor) {
            const duration = this.performanceMonitor.endTimer("addLog");
            if (duration > 16) { // 超过16ms的添加操作
                console.warn(`日志添加耗时过长: ${duration}ms`);
            }
        }
    }

    /**
     * 批量添加日志
     */
    public addLogs(messages: string[], level: LogLevel = LogLevel.INFO): void {
        if (this.performanceMonitor) {
            this.performanceMonitor.startTimer("addLogs");
        }

        const logItems: LogItem[] = messages.map(message => ({
            id: this.logId++,
            time: new Date().toLocaleTimeString("zh-CN", {
                hour12: false,
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
            }),
            message: message.trim(),
            level,
        }));

        this.logs.value.push(...logItems);

        // 限制日志数量
        if (this.logs.value.length > this.config.maxLogs) {
            this.logs.value = this.logs.value.slice(-this.config.maxLogs);
        }

        if (this.performanceMonitor) {
            const duration = this.performanceMonitor.endTimer("addLogs");
            console.log(`批量添加 ${messages.length} 条日志耗时: ${duration}ms`);
        }
    }

    /**
     * 从文本添加日志（支持流式处理）
     */
    public addLogsFromText(text: string, jobId?: number, randomId?: string): void {
        if (this.performanceMonitor) {
            this.performanceMonitor.startTimer("addLogsFromText");
        }

        // 追加到缓冲区并处理换行
        this.logBuffer += text.replace(/\r\n/g, "\n"); // 统一换行符

        // 按换行分割并保留未完成行
        const lines = this.logBuffer.split("\n");
        this.logBuffer = lines.pop() || ""; // 最后未完成行保留在缓冲区

        // 解析每行日志
        const logItems: LogItem[] = lines
            .map(line => line.trim())
            .filter(line => line.length > 0)
            .map(line => ({
                id: this.logId++,
                time: new Date().toLocaleTimeString("zh-CN", {
                    hour12: false,
                    hour: "2-digit",
                    minute: "2-digit",
                    second: "2-digit",
                }),
                message: line,
                level: this.detectLogLevel(line),
                jobId,
                randomId,
            }));

        if (logItems.length > 0) {
            this.logs.value.push(...logItems);

            // 限制日志数量
            if (this.logs.value.length > this.config.maxLogs) {
                this.logs.value = this.logs.value.slice(-this.config.maxLogs);
            }
        }

        if (this.performanceMonitor) {
            const duration = this.performanceMonitor.endTimer("addLogsFromText");
            if (duration > 50) { // 超过50ms的处理操作
                console.warn(`日志文本处理耗时过长: ${duration}ms, 处理了 ${logItems.length} 条日志`);
            }
        }
    }

    /**
     * 检测日志级别
     */
    private detectLogLevel(line: string): LogLevel {
        const lowerLine = line.toLowerCase();

        // 错误级别检测
        if (
            /exception|error|失败|err|code：500|handlecode=500|任务运行状态:false|exception|error|失败|err|code：500|handlecode=500|任务运行状态:false/.test(lowerLine)
        ) {
            return LogLevel.ERROR;
        }

        // 警告级别检测
        if (/warn|warning|警告/.test(lowerLine)) {
            return LogLevel.WARNING;
        }

        // 信息级别检测
        if (/info|信息|开始|结束|完成/.test(lowerLine)) {
            return LogLevel.INFO;
        }

        return LogLevel.DEBUG;
    }

    /**
     * 清空日志
     */
    public clearLogs(): void {
        this.logs.value = [];
        this.logId = 0;
        this.logBuffer = "";
        console.log("日志已清空");
    }

    /**
     * 重置日志管理器
     */
    public reset(): void {
        this.clearLogs();
        console.log("日志管理器已重置");
    }

    /**
     * 获取指定级别的日志
     */
    public getLogsByLevel(level: LogLevel): LogItem[] {
        return this.logs.value.filter(log => log.level === level);
    }

    /**
     * 搜索日志
     */
    public searchLogs(keyword: string, caseSensitive: boolean = false): LogItem[] {
        const searchText = caseSensitive ? keyword : keyword.toLowerCase();
        return this.logs.value.filter(log => {
            const logText = caseSensitive ? log.message : log.message.toLowerCase();
            return logText.includes(searchText);
        });
    }

    /**
     * 导出日志
     */
    public exportLogs(format: "json" | "txt" = "txt"): string {
        if (format === "json") {
            return JSON.stringify(this.logs.value, null, 2);
        } else {
            return this.logs.value
                .map(log => `[${log.time}] [${log.level.toUpperCase()}] ${log.message}`)
                .join("\n");
        }
    }

    /**
     * 获取日志缓冲区状态
     */
    public getBufferStatus(): { size: number; isEmpty: boolean } {
        return {
            size: this.logBuffer.length,
            isEmpty: this.logBuffer.length === 0,
        };
    }

    /**
     * 刷新日志缓冲区
     */
    public flushBuffer(): void {
        if (this.logBuffer.length > 0) {
            const level = this.detectLogLevel(this.logBuffer);
            this.addLog(this.logBuffer, level);
            this.logBuffer = "";
        }
    }

    /**
     * 启动定时刷新
     */
    private startFlushTimer(): void {
        if (this.flushTimer) {
            clearInterval(this.flushTimer);
        }

        this.flushTimer = setInterval(() => {
            this.flushBuffer();
        }, this.config.flushInterval);
    }

    /**
     * 停止定时刷新
     */
    public stopFlushTimer(): void {
        if (this.flushTimer) {
            clearInterval(this.flushTimer);
            this.flushTimer = null;
        }
    }

    /**
     * 销毁日志管理器
     */
    public destroy(): void {
        this.stopFlushTimer();
        this.clearLogs();
        console.log("日志管理器已销毁");
    }
}

/**
 * 日志管理器工厂类
 */
export class LogManagerFactory {
    private static instances = new Map<string, LogManager>();

    /**
     * 创建或获取日志管理器实例
     */
    public static getInstance(name: string, config?: Partial<LogConfig>): LogManager {
        if (!this.instances.has(name)) {
            this.instances.set(name, new LogManager(config));
        }
        return this.instances.get(name)!;
    }

    /**
     * 销毁指定实例
     */
    public static destroyInstance(name: string): void {
        const instance = this.instances.get(name);
        if (instance) {
            instance.destroy();
            this.instances.delete(name);
        }
    }

    /**
     * 销毁所有实例
     */
    public static destroyAll(): void {
        this.instances.forEach(instance => instance.destroy());
        this.instances.clear();
    }

    /**
     * 获取所有实例名称
     */
    public static getInstanceNames(): string[] {
        return Array.from(this.instances.keys());
    }
}

/**
 * 性能监控类
 */
class PerformanceMonitor {
    private timers = new Map<string, number>();

    public startTimer(name: string): void {
        this.timers.set(name, performance.now());
    }

    public endTimer(name: string): number {
        const startTime = this.timers.get(name);
        if (startTime) {
            const duration = performance.now() - startTime;
            this.timers.delete(name);
            return duration;
        }
        return 0;
    }
}
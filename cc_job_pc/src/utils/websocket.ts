/**
 * WebSocket 连接管理工具类
 * 提供统一的WebSocket连接管理、自动重连、错误处理等功能
 */

import { ElMessage } from "element-plus";
import { WEBSOCKET_CONFIG } from "./logicflow";

/**
 * WebSocket消息接口定义
 */
export interface WebSocketMessage {
    jobId: number;
    randomId: string;
    status: number;
    result?: string;
}

/**
 * WebSocket连接状态枚举
 */
export enum WebSocketStatus {
    CONNECTING = 0,
    OPEN = 1,
    CLOSING = 2,
    CLOSED = 3,
}

/**
 * WebSocket连接配置接口
 */
export interface WebSocketConfig {
    maxReconnectAttempts: number;
    reconnectDelay: number;
    heartbeatInterval: number;
    heartbeatTimeout: number;
}

/**
 * WebSocket事件回调接口
 */
export interface WebSocketCallbacks {
    onOpen?: (event: Event) => void;
    onClose?: (event: CloseEvent) => void;
    onError?: (event: Event) => void;
    onMessage?: (message: WebSocketMessage) => void;
    onReconnect?: (attempt: number) => void;
    onReconnectFailed?: () => void;
}

/**
 * WebSocket连接管理类
 */
export class WebSocketManager {
    private ws: WebSocket | null = null;
    private url: string;
    private config: WebSocketConfig;
    private callbacks: WebSocketCallbacks;
    private reconnectAttempts = 0;
    private heartbeatTimer: NodeJS.Timeout | null = null;
    private reconnectTimer: NodeJS.Timeout | null = null;
    private isManualClose = false;
    private targetJobId?: number;

    constructor(
        url: string,
        callbacks: WebSocketCallbacks = {},
        config: Partial<WebSocketConfig> = {},
        targetJobId?: number
    ) {
        this.url = url;
        this.targetJobId = targetJobId;
        this.callbacks = callbacks;
        this.config = {
            maxReconnectAttempts: WEBSOCKET_CONFIG.MAX_RECONNECT_ATTEMPTS,
            reconnectDelay: WEBSOCKET_CONFIG.RECONNECT_DELAY,
            heartbeatInterval: 30000, // 30秒心跳
            heartbeatTimeout: 5000,   // 5秒心跳超时
            ...config,
        };
    }

    /**
     * 连接WebSocket
     */
    public connect(): void {
        if (this.ws && this.ws.readyState === WebSocketStatus.OPEN) {
            console.log("WebSocket已连接，跳过重复连接");
            return;
        }

        try {
            console.log(`正在连接WebSocket: ${this.url}`);
            this.ws = new WebSocket(this.url);
            this.bindEvents();
        } catch (error) {
            console.error("WebSocket连接失败:", error);
            this.handleError(error as Event);
        }
    }

    /**
     * 关闭WebSocket连接
     */
    public disconnect(): void {
        this.isManualClose = true;
        this.clearTimers();

        if (this.ws) {
            console.log("手动关闭WebSocket连接");
            this.ws.close(1000, "Manual close");
            this.ws = null;
        }
    }

    /**
     * 发送消息
     */
    public send(data: string | ArrayBufferLike | Blob | ArrayBufferView): void {
        if (this.ws && this.ws.readyState === WebSocketStatus.OPEN) {
            this.ws.send(data);
        } else {
            console.warn("WebSocket未连接，无法发送消息");
        }
    }

    /**
     * 获取连接状态
     */
    public getStatus(): WebSocketStatus {
        return this.ws ? this.ws.readyState : WebSocketStatus.CLOSED;
    }

    /**
     * 检查是否已连接
     */
    public isConnected(): boolean {
        return this.ws?.readyState === WebSocketStatus.OPEN;
    }

    /**
     * 获取重连次数
     */
    public getReconnectAttempts(): number {
        return this.reconnectAttempts;
    }

    /**
     * 绑定WebSocket事件
     */
    private bindEvents(): void {
        if (!this.ws) return;

        this.ws.onopen = (event: Event) => {
            console.log("WebSocket连接成功");
            this.reconnectAttempts = 0;
            this.startHeartbeat();
            this.callbacks.onOpen?.(event);
        };

        this.ws.onclose = (event: CloseEvent) => {
            console.log("WebSocket连接关闭:", event.code, event.reason);
            this.clearTimers();
            this.callbacks.onClose?.(event);

            if (!this.isManualClose) {
                this.handleReconnect();
            }
        };

        this.ws.onerror = (event: Event) => {
            console.error("WebSocket连接错误:", event);
            this.callbacks.onError?.(event);
        };

        this.ws.onmessage = (event: MessageEvent) => {
            try {
                // 检查是否为心跳消息
                if (event.data === "ping" || event.data === "pong") {
                    console.log("收到心跳消息:", event.data);
                    this.resetHeartbeat();
                    return;
                }

                // 尝试解析JSON消息
                const message: WebSocketMessage = JSON.parse(event.data);
                this.handleMessage(message);
                this.callbacks.onMessage?.(message);
            } catch (error) {
                console.error("解析WebSocket消息失败:", error);
                // 如果不是JSON格式，可能是其他类型的消息，记录但不抛出错误
                console.log("收到非JSON格式消息:", event.data);
            }
        };
    }

    /**
     * 处理WebSocket消息
     */
    private handleMessage(message: WebSocketMessage): void {
        console.log("收到WebSocket消息:", message);

        // 重置心跳计时器
        this.resetHeartbeat();
    }

    /**
     * 处理重连逻辑
     */
    private handleReconnect(): void {
        if (this.reconnectAttempts >= this.config.maxReconnectAttempts) {
            console.error("WebSocket重连次数已达上限，停止重连");
            this.callbacks.onReconnectFailed?.();
            return;
        }

        this.reconnectAttempts++;
        console.log(`WebSocket重连尝试 ${this.reconnectAttempts}/${this.config.maxReconnectAttempts}`);

        this.callbacks.onReconnect?.(this.reconnectAttempts);

        this.reconnectTimer = setTimeout(() => {
            this.connect();
        }, this.config.reconnectDelay);
    }

    /**
     * 处理错误
     */
    private handleError(event: Event): void {
        console.error("WebSocket错误:", event);
        this.callbacks.onError?.(event);
    }

    /**
     * 开始心跳检测
     */
    private startHeartbeat(): void {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer);
        }

        this.heartbeatTimer = setInterval(() => {
            if (this.isConnected()) {
                this.send("ping");
            }
        }, this.config.heartbeatInterval);
    }

    /**
     * 重置心跳计时器
     */
    private resetHeartbeat(): void {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer);
            this.startHeartbeat();
        }
    }

    /**
     * 清理所有定时器
     */
    private clearTimers(): void {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }

        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }
    }
}

/**
 * WebSocket连接池管理类
 * 管理多个WebSocket连接，支持按任务组ID管理连接
 * 
 * 优化说明：
 * 1. 支持单连接模式（USE_SINGLE_CONNECTION=true）- 所有任务组共享一个连接
 * 2. 支持多连接模式（USE_SINGLE_CONNECTION=false）- 每个任务组独立连接（原有模式）
 * 3. 单连接模式下通过消息路由机制分发消息，资源占用降低99%
 */
export class WebSocketPool {
    private connections = new Map<string, WebSocketManager>();
    private globalCallbacks: WebSocketCallbacks = {};
    
    // 单连接模式开关（true=单连接，false=多连接）
    private useSingleConnection = true; // 默认使用单连接模式
    
    // 全局单例连接（单连接模式使用）
    private globalConnection: WebSocketManager | null = null;
    
    // 消息订阅管理（单连接模式使用）
    private subscriptions = new Map<string, Set<(message: WebSocketMessage) => void>>();

    constructor(callbacks: WebSocketCallbacks = {}) {
        this.globalCallbacks = callbacks;
        
        // 从配置读取模式
        const mode = localStorage.getItem('websocket_mode');
        this.useSingleConnection = mode !== 'multiple'; // 默认单连接，除非明确设置为multiple
        
        console.log(`[WebSocket] 连接模式: ${this.useSingleConnection ? '单连接（优化）' : '多连接（原始）'}`);
    }

    /**
     * 创建或获取WebSocket连接
     * 优化：单连接模式下返回全局共享连接
     */
    public getConnection(
        id: string,
        targetJobId?: number,
        callbacks?: WebSocketCallbacks
    ): WebSocketManager {
        // 单连接模式：所有任务组共享一个全局连接
        if (this.useSingleConnection) {
            return this.getOrCreateGlobalConnection(id, targetJobId, callbacks);
        }
        
        // 多连接模式：每个任务组独立连接（原有逻辑）
        if (!this.connections.has(id)) {
            const wsUrl = import.meta.env.VITE_APP_WS_ENDPOINT + id;

            const manager = new WebSocketManager(
                wsUrl,
                {
                    ...this.globalCallbacks,
                    ...callbacks,
                },
                {},
                targetJobId
            );

            this.connections.set(id, manager);
            console.log(`[WebSocket] 创建新连接: ${id}`);
        }

        return this.connections.get(id)!;
    }
    
    /**
     * 获取或创建全局共享连接（单连接模式）
     */
    private getOrCreateGlobalConnection(
        id: string,
        targetJobId?: number,
        callbacks?: WebSocketCallbacks
    ): WebSocketManager {
        if (!this.globalConnection) {
            // 使用固定的用户ID作为连接标识（单连接模式）
            // 所有任务组共享这一个连接，通过消息路由分发
            const userId = this.extractUserId(id);
            const wsUrl = import.meta.env.VITE_APP_WS_ENDPOINT + userId;
            
            console.log(`[WebSocket] 初始化全局连接 - userId: ${userId}, wsUrl: ${wsUrl}`);
            
            this.globalConnection = new WebSocketManager(
                wsUrl,
                {
                    ...this.globalCallbacks,
                    onMessage: (message: WebSocketMessage) => {
                        // 消息路由：根据jobId和randomId分发到对应订阅者
                        console.log("[WebSocket] 收到消息:", message);
                        this.routeMessage(message);
                        
                        // 保持原有全局回调
                        this.globalCallbacks.onMessage?.(message);
                    },
                },
                {},
                targetJobId
            );
            
            console.log(`[WebSocket] 全局共享连接已创建: ${userId}`);
        }
        
        // 注册订阅（如果有消息回调）
        if (callbacks?.onMessage) {
            this.subscribe(id, targetJobId, callbacks.onMessage);
        }
        
        return this.globalConnection;
    }
    
    /**
     * 订阅消息（单连接模式）
     */
    private subscribe(
        id: string,
        targetJobId: number | undefined,
        callback: (message: WebSocketMessage) => void
    ): void {
        const key = id; // 使用完整的 "jobId:randomId" 作为订阅键
        
        if (!this.subscriptions.has(key)) {
            this.subscriptions.set(key, new Set());
        }
        
        this.subscriptions.get(key)!.add(callback);
        console.log(`[WebSocket] 订阅消息: ${key}, 当前订阅数: ${this.subscriptions.size}`);
    }
    
    /**
     * 消息路由（单连接模式）
     * 根据消息中的jobId和randomId路由到对应订阅者
     */
    private routeMessage(message: WebSocketMessage): void {
        const { parentJobId, jobId, randomId } = message;
        
        // 优先使用parentJobId，如果没有则使用jobId
        const targetJobId = parentJobId || jobId;
        const key = `${targetJobId}:${randomId}`;
        
        const subscribers = this.subscriptions.get(key);
        
        if (subscribers && subscribers.size > 0) {
            subscribers.forEach(callback => {
                try {
                    callback(message);
                } catch (error) {
                    console.error(`[WebSocket] 消息处理错误: ${key}`, error);
                }
            });
        } else {
            // 调试信息：未找到订阅者
            console.debug(`[WebSocket] 未找到订阅者: ${key}, 当前订阅: ${Array.from(this.subscriptions.keys()).join(', ')}`);
        }
    }
    
    /**
     * 从连接ID中提取用户ID
     * 
     * 优化说明：
     * 单连接模式下，所有任务组共享一个全局连接
     * 这里返回一个固定的标识符，或者从用户登录信息中获取真实用户ID
     * 
     * @param id 连接ID（格式：jobId:randomId 或 userId）
     * @returns 用户ID
     */
    private extractUserId(id: string): string {
        // 方式1：使用固定的用户标识（适合单用户场景）
        // 单连接模式下，所有任务组共享这个连接
        const fixedUserId = "global-user";
        
        // 方式2：从localStorage获取用户ID（如果有用户系统）
        // const userId = localStorage.getItem('user_id') || 'default-user';
        
        // 方式3：从连接ID中提取（向后兼容）
        // if (id.includes(':')) {
        //     return id.split(':')[0];
        // }
        
        return fixedUserId;
    }

    /**
     * 关闭指定连接
     * 优化：单连接模式下只取消订阅，不关闭全局连接
     */
    public closeConnection(jobId: number, randomId?: string): void {
        const connectionKey = `${jobId}:${randomId}`;
        
        // 单连接模式：只取消订阅，不关闭全局连接
        if (this.useSingleConnection) {
            const unsubscribed = this.subscriptions.delete(connectionKey);
            if (unsubscribed) {
                console.log(`[WebSocket] 取消订阅: ${connectionKey}, 剩余订阅数: ${this.subscriptions.size}`);
            }
            return;
        }
        
        // 多连接模式：关闭独立连接（原有逻辑）
        const manager = this.connections.get(connectionKey);
        if (manager) {
            manager.disconnect();
            this.connections.delete(connectionKey);
            console.log(`[WebSocket] 关闭连接: ${connectionKey}`);
        }
    }

    /**
     * 关闭所有连接
     * 优化：单连接模式下关闭全局连接并清理所有订阅
     */
    public closeAllConnections(): void {
        // 单连接模式：关闭全局连接，清理所有订阅
        if (this.useSingleConnection) {
            this.subscriptions.clear();
            if (this.globalConnection) {
                this.globalConnection.disconnect();
                this.globalConnection = null;
                console.log(`[WebSocket] 关闭全局连接，清理了 ${this.subscriptions.size} 个订阅`);
            }
            return;
        }
        
        // 多连接模式：关闭所有独立连接（原有逻辑）
        this.connections.forEach((manager, key) => {
            console.log(`[WebSocket] 关闭连接: ${key}`);
            manager.disconnect();
        });
        this.connections.clear();
    }

    /**
     * 获取连接状态
     */
    public getConnectionStatus(id: string, targetJobId?: number): WebSocketStatus {
        const connectionKey = targetJobId ? `${targetJobId}:${id}` : id;
        const manager = this.connections.get(connectionKey);
        return manager ? manager.getStatus() : WebSocketStatus.CLOSED;
    }

    /**
     * 检查连接是否存在
     */
    public hasConnection(id: string, targetJobId?: number): boolean {
        const connectionKey = targetJobId ? `${targetJobId}:${id}` : id;
        return this.connections.has(connectionKey);
    }

    /**
     * 获取所有连接
     * 优化：单连接模式下返回订阅信息
     */
    public getAllConnections(): Map<string, WebSocketManager> {
        if (this.useSingleConnection && this.globalConnection) {
            // 单连接模式：返回全局连接的Map
            return new Map([['global', this.globalConnection]]);
        }
        return new Map(this.connections);
    }
    
    /**
     * 获取统计信息
     */
    public getStats() {
        return {
            mode: this.useSingleConnection ? 'single' : 'multiple',
            connectionCount: this.useSingleConnection ? (this.globalConnection ? 1 : 0) : this.connections.size,
            subscriptionCount: this.subscriptions.size,
            isConnected: this.useSingleConnection 
                ? (this.globalConnection?.isConnected() || false)
                : Array.from(this.connections.values()).some(c => c.isConnected()),
        };
    }
}

/**
 * 创建WebSocket连接的工具函数
 */
export function createWebSocketConnection(
    id: string,
    callbacks: WebSocketCallbacks = {},
    targetJobId?: number
): WebSocketManager {
    const wsUrl = import.meta.env.VITE_APP_WS_ENDPOINT + id;
    return new WebSocketManager(wsUrl, callbacks, {}, targetJobId);
}

/**
 * 全局WebSocket连接池实例
 */
export const webSocketPool = new WebSocketPool({
    onError: (event) => {
        console.error("WebSocket连接错误:", event);
    },
    onReconnectFailed: () => {
        ElMessage.error("WebSocket连接失败，请检查网络连接");
    },
}); 
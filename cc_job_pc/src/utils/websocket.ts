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
 */
export class WebSocketPool {
    private connections = new Map<string, WebSocketManager>();
    private globalCallbacks: WebSocketCallbacks = {};

    constructor(callbacks: WebSocketCallbacks = {}) {
        this.globalCallbacks = callbacks;
    }

    /**
     * 创建或获取WebSocket连接
     */
    public getConnection(
        id: string,
        targetJobId?: number,
        callbacks?: WebSocketCallbacks
    ): WebSocketManager {
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
            console.log(`创建新的WebSocket连接: ${id}`);
        }

        return this.connections.get(id)!;
    }

    /**
     * 关闭指定连接
     */
    public closeConnection(jobId: number, randomId?: string): void {
        const connectionKey = `${jobId}:${randomId}` ;
        console.log("this.connections.delete(connectionKey):", this.connections);
        const manager = this.connections.get(connectionKey);

        if (manager) {
            manager.disconnect();
            this.connections.delete(connectionKey);
            console.log(`关闭WebSocket连接: ${connectionKey}`);
        }
    }

    /**
     * 关闭所有连接
     */
    public closeAllConnections(): void {
        this.connections.forEach((manager, key) => {
            console.log(`关闭WebSocket连接: ${key}`);
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
     */
    public getAllConnections(): Map<string, WebSocketManager> {
        return new Map(this.connections);
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
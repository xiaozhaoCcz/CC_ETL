import axios, { InternalAxiosRequestConfig, AxiosResponse } from "axios";
import {getToken} from "@/utils/auth.ts";
import {ResultEnum} from "@/enums/ResultEnum.ts";
import {ElMessage, ElNotification} from "element-plus";



// 创建 axios 实例
const service = axios.create({
    baseURL: "http://localhost:8989",
    timeout: 50000,
    headers: { "Content-Type": "application/json;charset=utf-8" },
});

// 请求拦截器
service.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const accessToken = getToken();
        if (accessToken) {
            config.headers.Authorization = accessToken;
        }
        return config;
    },
    (error: any) => {
        return Promise.reject(error);
    }
);

// 响应拦截器
service.interceptors.response.use(
    (response: AxiosResponse) => {
        // 检查配置的响应类型是否为二进制类型（'blob' 或 'arraybuffer'）, 如果是，直接返回响应对象
        if (
            response.config.responseType === "blob" ||
            response.config.responseType === "arraybuffer"
        ) {
            return response;
        }

        const { code, data, msg } = response.data;
        if (code === ResultEnum.SUCCESS) {
            return data;
        }

        ElMessage.error(msg || "系统出错");
        return Promise.reject(new Error(msg || "Error"));
    },
    (error: any) => {
        // 异常处理 非 2xx 状态码 会进入这里
        if (error.response.data) {
            const { code, msg } = error.response.data;
            if (code === ResultEnum.TOKEN_INVALID) {
                ElNotification({
                    title: "提示",
                    message: "您的会话已过期，请重新登录",
                    type: "info",
                });
                location.reload();
            } else {
                ElMessage.error(msg || "系统出错");
            }
        }
        return Promise.reject(error.message);
    }
);

// 导出 axios 实例
export default service;

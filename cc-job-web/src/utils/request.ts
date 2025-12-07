/**
 * Axios 请求封装
 * 
 * <p>统一的HTTP请求配置和拦截器
 * 
 * @author cc-job-team
 */

import axios, {
  type InternalAxiosRequestConfig,
  type AxiosResponse,
} from "axios";
import qs from "qs";
import { useUserStoreHook } from "@/store/modules/user";
import { ResultEnum } from "@/enums/ResultEnum";
import { getToken } from "@/utils/auth";
import { HTTP_STATUS } from "@/constants";

/**
 * 创建 axios 实例
 */
const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 50000,
  headers: { "Content-Type": "application/json;charset=utf-8" },
  paramsSerializer: (params) => qs.stringify(params),
});

/**
 * 请求拦截器
 * 
 * <p>在请求发送前添加认证信息
 */
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

/**
 * 响应拦截器
 * 
 * <p>统一处理响应数据和错误
 */
service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 二进制类型响应直接返回
    if (isBinaryResponse(response)) {
      return response;
    }

    const { code, data, msg } = response.data;
    
    // 请求成功
    if (code === ResultEnum.SUCCESS) {
      return data;
    }

    // 请求失败，显示错误消息
    ElMessage.error(msg || "系统出错");
    return Promise.reject(new Error(msg || "Error"));
  },
  (error: any) => {
    // 处理非 2xx 状态码
    handleErrorResponse(error);
    return Promise.reject(error.message);
  }
);

/**
 * 判断是否为二进制响应
 */
function isBinaryResponse(response: AxiosResponse): boolean {
  return (
    response.config.responseType === "blob" ||
    response.config.responseType === "arraybuffer"
  );
}

/**
 * 处理错误响应
 */
function handleErrorResponse(error: any): void {
  if (!error.response?.data) {
    return;
  }

  const { code, msg } = error.response.data;
  
  if (code === ResultEnum.TOKEN_INVALID) {
    handleTokenInvalid();
  } else {
    ElMessage.error(msg || "系统出错");
  }
}

/**
 * 处理令牌失效
 */
function handleTokenInvalid(): void {
  ElNotification({
    title: "提示",
    message: "您的会话已过期，请重新登录",
    type: "info",
  });
  
  useUserStoreHook()
    .clearUserSession()
    .then(() => {
      location.reload();
    });
}

export default service;

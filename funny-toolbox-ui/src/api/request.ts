import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResult } from './types';
import { toast } from 'vue-sonner';

/**
 * 获取 API 基础路径
 * 独立部署模式：使用完整的后端地址 + /api
 * 嵌入式部署模式：使用相对路径 /api
 */
const getBaseURL = (): string => {
  const deploymentMode = import.meta.env.VITE_DEPLOYMENT_MODE || 'standalone';
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '';
  
  if (deploymentMode === 'embedded') {
    // 嵌入式部署：使用相对路径
    return '/api';
  } else {
    // 独立部署：使用配置的后端地址
    return apiBaseUrl ? `${apiBaseUrl}/api` : '/api';
  }
};

/**
 * 创建 axios 实例
 */
const request: AxiosInstance = axios.create({
  baseURL: getBaseURL(),
  timeout: 30000,
  withCredentials: true, // 携带 cookie
});

/**
 * 请求拦截器
 */
request.interceptors.request.use(
  (config) => {
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * 处理登录失效的统一方法
 */
let isRedirecting = false;
const UNAUTHORIZED_REDIRECT_DELAY = 1500;
const handleUnauthorized = () => {
  if (isRedirecting) return;
  
  isRedirecting = true;
  toast.error('登录已失效，即将返回首页');
  
  setTimeout(() => {
    window.location.href = '/';
  }, UNAUTHORIZED_REDIRECT_DELAY);
};

/**
 * 响应拦截器
 */
request.interceptors.response.use(
  (response) => {
    // 如果响应类型是 blob（文件下载），直接返回，不做业务逻辑检查
    if (response.config.responseType === 'blob') {
      return response;
    }
    
    const data = response.data as ApiResult;
    
    // 如果返回的 code 不是 200，视为业务错误
    if (data.code !== 200) {
      return Promise.reject(new Error(data.message || '请求失败'));
    }
    
    return response;
  },
  (error: AxiosError) => {
    const config = error.config as (InternalAxiosRequestConfig & { isLoginRequest?: boolean; skipAuthHandler?: boolean }) | undefined;
    const isLoginRequest = config?.url?.includes('/auth/login');
    
    // 如果是登录请求的 401，不做特殊处理，让登录页面自己处理
    if (isLoginRequest && error.response?.status === 401) {
      return Promise.reject(error);
    }
    
    // 处理 HTTP 401 错误（session 过期）
    if (error.response?.status === 401) {
      if (!config?.skipAuthHandler) {
        handleUnauthorized();
      }
      return Promise.reject(new Error('登录已过期'));
    }
    
    // 处理网络错误（可能是 CORS 阻止的 401）
    if (!error.response && error.request) {
      return Promise.reject(new Error('网络错误，请检查网络连接'));
    }
    
    // 处理其他 HTTP 错误
    if (error.response) {
      const status = error.response.status;
      if (status === 403) {
        return Promise.reject(new Error('没有权限'));
      } else if (status === 404) {
        return Promise.reject(new Error('请求的资源不存在'));
      } else if (status >= 500) {
        return Promise.reject(new Error('服务器错误'));
      }
    }
    
    return Promise.reject(error);
  }
);

/**
 * 获取后端基础地址（不含 /api）
 * 用于拼接插件静态资源等非 API 路径
 */
export const getBackendBaseURL = (): string => {
  const deploymentMode = import.meta.env.VITE_DEPLOYMENT_MODE || 'standalone';
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '';
  
  if (deploymentMode === 'embedded') {
    // 嵌入式部署：使用当前域名
    return '';
  } else {
    // 独立部署：使用配置的后端地址
    return apiBaseUrl || '';
  }
};

export default request;

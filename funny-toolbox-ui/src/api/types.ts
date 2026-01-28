/**
 * API 响应通用结构
 */
export interface ApiResult<T = any> {
  code: number;
  message: string;
  data: T;
}

/**
 * 登录请求参数
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * 插件信息
 */
export interface Plugin {
  id: string;
  name: string;
  version: string;
  description: string;
  author: string;
  icon?: string;
  status: 'LOADED' | 'ENABLED' | 'DISABLED' | 'ERROR';
  loadTime?: string;
  startTime?: string;
  frontendEntry?: string;
  apiPrefix?: string;
  tags?: string[];
}

/**
 * 插件列表响应
 */
export type PluginListResponse = ApiResult<Plugin[]>;

/**
 * 插件操作响应
 */
export type PluginActionResponse = ApiResult<void>;

/**
 * 上传插件响应
 */
export type PluginInstallResponse = ApiResult<string>;

/**
 * 插件图标信息
 */
export interface PluginIcon {
  type: 'EMOJI' | 'URL' | 'SVG' | 'FONT_AWESOME' | 'MATERIAL' | 'CUSTOM';
  value: string;
  color?: string;
  style?: string;
}

/**
 * 前端插件打包元数据
 */
export interface FrontendPluginPackMeta {
  id: string;
  name: string;
  version: string;
  description: string;
  author: string;
  iconMeta?: PluginIcon;
  frontendEntry: string;
  frontendBasePath?: string;
}

/**
 * 前端插件打包结果
 */
export interface FrontendPluginPackResult {
  pluginId: string;
  fileName: string;
  fileSize: number;
  downloadUrl: string;
  imported: boolean;
}

/**
 * 前端插件打包响应
 */
export type FrontendPluginPackResponse = ApiResult<FrontendPluginPackResult>;

/**
 * 认证状态信息
 */
export interface AuthStatus {
  authenticated: boolean;
  user?: string;
  sessionId?: string;
  serverTime?: number;
}

/**
 * 认证状态响应
 */
export type AuthStatusResponse = ApiResult<AuthStatus>;

/**
 * 标签列表响应
 */
export type TagListResponse = ApiResult<string[]>;

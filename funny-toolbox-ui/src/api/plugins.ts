import request from './request';
import type { PluginListResponse, PluginActionResponse, PluginInstallResponse, FrontendPluginPackMeta, FrontendPluginPackResponse, TagListResponse } from './types';

/**
 * 插件管理相关 API
 */

/**
 * 获取插件列表
 */
export const getPluginList = () => {
  return request.get<PluginListResponse>('/platform/plugins');
};

/**
 * 启用插件
 */
export const enablePlugin = (id: string) => {
  return request.post<PluginActionResponse>(`/platform/plugins/${id}/enable`);
};

/**
 * 禁用插件
 */
export const disablePlugin = (id: string) => {
  return request.post<PluginActionResponse>(`/platform/plugins/${id}/disable`);
};

/**
 * 卸载插件
 */
export const uninstallPlugin = (id: string) => {
  return request.delete<PluginActionResponse>(`/platform/plugins/${id}`);
};

/**
 * 重新加载插件
 */
export const reloadPlugin = (id: string) => {
  return request.post<PluginActionResponse>(`/platform/plugins/${id}/reload`);
};

/**
 * 上传并安装插件
 */
export const installPlugin = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  
  return request.post<PluginInstallResponse>('/platform/plugins/install', formData);
};

/**
 * 打包前端插件
 * @param meta 插件元数据
 * @param files 静态资源文件数组
 * @param paths 每个文件的相对路径
 * @param importPlugin 是否在打包完成后直接导入系统
 * @param tags 可选的默认标签列表，逗号分隔写入 plugin.yml
 */
export const packFrontendPlugin = (
  meta: FrontendPluginPackMeta,
  files: File[],
  paths: string[],
  importPlugin: boolean = false,
  tags?: string[]
) => {
  const formData = new FormData();
  
  // 添加元数据
  formData.append('meta', new Blob([JSON.stringify(meta)], { type: 'application/json' }));
  
  // 添加文件和路径
  files.forEach((file) => {
    formData.append('files', file);
  });
  
  paths.forEach((path) => {
    formData.append('paths', path);
  });
  
  const params = new URLSearchParams();
  params.append('import', String(importPlugin));
  if (tags && tags.length > 0) {
    params.append('tags', tags.join(','));
  }
  
  return request.post<FrontendPluginPackResponse>(
    `/platform/plugins/pack/frontend?${params.toString()}`,
    formData
  );
};

/**
 * 下载打包产物
 * @param fileName 文件名
 */
export const downloadPackedPlugin = (fileName: string) => {
  return request.get(`/platform/plugins/pack/download/${fileName}`, {
    responseType: 'blob',
  });
};

/**
 * 导入已打包的前端插件ZIP
 * @param fileName 打包生成的 ZIP 文件名
 * @param enable 是否在导入后立即启用该插件
 */
export const importPackedPlugin = (fileName: string, enable: boolean = true) => {
  return request.post<FrontendPluginPackResponse>(
    `/platform/plugins/pack/import?fileName=${encodeURIComponent(fileName)}&enable=${enable}`
  );
};

/**
 * 下载插件包
 * @param pluginId 插件ID
 * @param ext 首选扩展名：jar 或 zip（可选）
 */
export const downloadPluginPackage = (pluginId: string, ext?: string) => {
  const params = new URLSearchParams();
  params.append('pluginId', pluginId);
  if (ext) {
    params.append('ext', ext);
  }
  
  return request.get(`/platform/plugins/download?${params.toString()}`, {
    responseType: 'blob',
  });
};

/**
 * 获取所有标签
 */
export const getAllTags = () => {
  return request.get<TagListResponse>('/platform/plugins/tags');
};

/**
 * 获取插件标签
 * @param pluginId 插件ID
 */
export const getPluginTags = (pluginId: string) => {
  return request.get<TagListResponse>(`/platform/plugins/${pluginId}/tags`);
};

/**
 * 设置插件标签
 * @param pluginId 插件ID
 * @param tags 标签数组
 */
export const setPluginTags = (pluginId: string, tags: string[]) => {
  return request.put<PluginActionResponse>(`/platform/plugins/${pluginId}/tags`, tags);
};

/**
 * 新增插件标签
 * @param pluginId 插件ID
 * @param tag 标签
 */
export const addPluginTag = (pluginId: string, tag: string) => {
  return request.post<PluginActionResponse>(`/platform/plugins/${pluginId}/tags?tag=${encodeURIComponent(tag)}`);
};

/**
 * 移除插件标签
 * @param pluginId 插件ID
 * @param tag 标签
 */
export const removePluginTag = (pluginId: string, tag: string) => {
  return request.delete<PluginActionResponse>(`/platform/plugins/${pluginId}/tags?tag=${encodeURIComponent(tag)}`);
};

/**
 * 按单个标签筛选插件
 * @param tag 标签
 */
export const searchPluginsByTag = (tag: string) => {
  return request.get<PluginListResponse>(`/platform/plugins/search/by-tag?tag=${encodeURIComponent(tag)}`);
};

/**
 * 按多个标签筛选插件
 * @param tags 标签数组
 * @param matchAll 是否匹配所有标签（AND），默认false（OR）
 */
export const searchPluginsByTags = (tags: string[], matchAll: boolean = false) => {
  const tagsParam = tags.join(',');
  return request.get<PluginListResponse>(`/platform/plugins/search/by-tags?tags=${encodeURIComponent(tagsParam)}&matchAll=${matchAll}`);
};

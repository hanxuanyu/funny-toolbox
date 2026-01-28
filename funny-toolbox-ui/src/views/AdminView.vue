<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 顶部导航 -->
    <header class="bg-white shadow-sm border-b">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div class="flex justify-between items-center">
          <div class="flex items-center space-x-4">
            <h1 class="text-2xl font-bold text-gray-900">插件管理</h1>
            <Badge variant="outline">后台管理</Badge>
          </div>
          <div class="flex items-center space-x-4">
            <Button variant="outline" size="sm" @click="router.push('/')">
              <Home class="h-4 w-4 mr-2" />
              返回首页
            </Button>
            <Button variant="outline" size="sm" @click="handleLogout">
              <LogOut class="h-4 w-4 mr-2" />
              退出登录
            </Button>
          </div>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- 操作栏 -->
      <div class="mb-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 class="text-lg font-semibold text-gray-900">插件列表</h2>
          <p class="text-sm text-gray-500 mt-1">管理平台所有插件</p>
        </div>
        <div class="flex gap-2">
          <Button @click="loadPlugins" variant="outline">
            <RefreshCw class="h-4 w-4 mr-2" />
            刷新
          </Button>
          <Button @click="showPackDialog = true" variant="outline">
            <Package class="h-4 w-4 mr-2" />
            打包前端插件
          </Button>
          <Button @click="showUploadDialog = true">
            <Upload class="h-4 w-4 mr-2" />
            上传插件
          </Button>
        </div>
      </div>

      <!-- 加载状态 -->
      <Card v-if="loading" class="p-8">
        <div class="text-center text-gray-500">加载中...</div>
      </Card>

      <!-- 错误提示 -->
      <Alert v-else-if="error" variant="destructive" class="mb-6">
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <!-- 插件表格 -->
      <Card v-else>
        <div class="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead class="w-[100px]">ID</TableHead>
                <TableHead>名称</TableHead>
                <TableHead class="hidden md:table-cell">版本</TableHead>
                <TableHead class="hidden lg:table-cell">作者</TableHead>
                <TableHead class="hidden xl:table-cell">标签</TableHead>
                <TableHead class="hidden sm:table-cell">状态</TableHead>
                <TableHead class="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="plugins.length === 0">
                <TableCell colspan="7" class="text-center text-gray-500 py-8">
                  暂无插件
                </TableCell>
              </TableRow>
              <TableRow v-for="plugin in plugins" :key="plugin.id">
                <TableCell class="font-medium">{{ plugin.id }}</TableCell>
                <TableCell>
                  <div>
                    <div class="font-medium">{{ plugin.name }}</div>
                    <div 
                      class="text-sm text-gray-500 hidden xl:block max-w-md truncate" 
                      :title="plugin.description"
                    >
                      {{ plugin.description }}
                    </div>
                  </div>
                </TableCell>
                <TableCell class="hidden md:table-cell">{{ plugin.version }}</TableCell>
                <TableCell class="hidden lg:table-cell">{{ plugin.author }}</TableCell>
                <TableCell class="hidden xl:table-cell">
                  <div class="flex flex-wrap gap-1 max-w-[200px]">
                    <Badge 
                      v-for="tag in plugin.tags?.slice(0, 3)" 
                      :key="tag" 
                      variant="outline"
                      class="text-xs"
                    >
                      {{ tag }}
                    </Badge>
                    <Badge 
                      v-if="plugin.tags && plugin.tags.length > 3"
                      variant="outline"
                      class="text-xs"
                    >
                      +{{ plugin.tags.length - 3 }}
                    </Badge>
                  </div>
                </TableCell>
                <TableCell class="hidden sm:table-cell">
                  <Badge :variant="getStatusVariant(plugin.status)">
                    {{ getStatusText(plugin.status) }}
                  </Badge>
                </TableCell>
                <TableCell class="text-right">
                  <div class="flex gap-1 flex-wrap justify-end">
                    <Button
                      v-if="plugin.status === 'DISABLED' || plugin.status === 'LOADED'"
                      size="icon"
                      variant="ghost"
                      @click="handleEnable(plugin.id)"
                      title="启用插件"
                      class="h-8 w-8"
                    >
                      <Play class="h-4 w-4" />
                    </Button>
                    <Button
                      v-if="plugin.status === 'ENABLED'"
                      size="icon"
                      variant="ghost"
                      @click="handleDisable(plugin.id)"
                      title="禁用插件"
                      class="h-8 w-8"
                    >
                      <Square class="h-4 w-4" />
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      @click="handleReload(plugin.id)"
                      title="重新加载插件"
                      class="h-8 w-8"
                    >
                      <RotateCw class="h-4 w-4" />
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      @click="handleDownload(plugin.id)"
                      title="下载插件包"
                      class="h-8 w-8"
                    >
                      <Download class="h-4 w-4" />
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      @click="handleManageTags(plugin)"
                      title="管理标签"
                      class="h-8 w-8"
                    >
                      <Tag class="h-4 w-4" />
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      @click="handleUninstall(plugin.id)"
                      title="卸载插件"
                      class="h-8 w-8 text-red-600 hover:text-red-700 hover:bg-red-50"
                    >
                      <Trash2 class="h-4 w-4" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </Card>
    </main>

    <!-- 上传插件对话框 -->
    <Dialog v-model:open="showUploadDialog">
      <DialogContent class="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>上传插件</DialogTitle>
          <DialogDescription>
            选择插件 JAR 或 ZIP 文件上传并安装
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4">
          <div
            class="flex items-center justify-center w-full"
            @drop.prevent="handleFileDrop"
            @dragover.prevent="isFileDialogDragging = true"
            @dragenter.prevent="isFileDialogDragging = true"
            @dragleave.prevent="isFileDialogDragging = false"
          >
            <label
              for="file-upload"
              class="flex flex-col items-center justify-center w-full h-40 border-2 border-dashed rounded-lg cursor-pointer transition-all"
              :class="isFileDialogDragging ? 'border-blue-500 bg-blue-50' : 'border-gray-300 bg-gray-50 hover:bg-gray-100'"
            >
              <div class="flex flex-col items-center justify-center pt-5 pb-6">
                <Upload class="h-10 w-10 mb-3" :class="isFileDialogDragging ? 'text-blue-500' : 'text-gray-400'" />
                <p class="mb-2 text-sm text-gray-500">
                  <span class="font-semibold">点击选择或拖拽文件</span>
                </p>
                <p class="text-xs text-gray-500">JAR 或 ZIP 文件</p>
                <p v-if="uploadFile" class="mt-3 text-sm text-gray-700 font-medium">
                  {{ uploadFile.name }}
                </p>
                <p v-if="uploadFile" class="text-xs text-gray-500">
                  {{ formatUploadFileSize(uploadFile.size) }}
                </p>
              </div>
              <input
                id="file-upload"
                type="file"
                accept=".jar,.zip"
                class="hidden"
                @change="handleFileChange"
              />
            </label>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showUploadDialog = false">
            取消
          </Button>
          <Button @click="handleUpload" :disabled="!uploadFile || uploading">
            <span v-if="uploading">上传中...</span>
            <span v-else>上传</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 卸载确认对话框 -->
    <Dialog v-model:open="showConfirmDialog">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>确认卸载</DialogTitle>
          <DialogDescription>
            确定要卸载插件 "{{ confirmPluginId }}" 吗？此操作不可恢复。
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" @click="showConfirmDialog = false">
            取消
          </Button>
          <Button variant="destructive" @click="confirmUninstall">
            确定卸载
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 打包前端插件对话框 -->
    <PackPluginModal
      v-model:open="showPackDialog"
      @success="handlePackSuccess"
    />

    <!-- 标签管理对话框 -->
    <TagManagementModal
      v-model:open="showTagDialog"
      :plugin="tagManagementPlugin"
      @success="handleTagSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  getPluginList,
  enablePlugin,
  disablePlugin,
  uninstallPlugin,
  reloadPlugin,
  installPlugin,
  downloadPluginPackage,
  logout,
  checkAuthStatus,
  type Plugin,
} from '@/api';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { RefreshCw, Upload, Home, LogOut, Play, Square, RotateCw, Trash2, Package, Download, Tag } from 'lucide-vue-next';
import { toast } from 'vue-sonner';
import PackPluginModal from '@/components/PackPluginModal.vue';
import TagManagementModal from '@/components/TagManagementModal.vue';

const router = useRouter();

const plugins = ref<Plugin[]>([]);
const loading = ref(false);
const error = ref('');

const showUploadDialog = ref(false);
const uploadFile = ref<File | null>(null);
const uploading = ref(false);
const isFileDialogDragging = ref(false);

// 打包前端插件对话框状态
const showPackDialog = ref(false);

// 确认对话框状态（仅用于卸载插件）
const showConfirmDialog = ref(false);
const confirmPluginId = ref('');

// 标签管理对话框状态
const showTagDialog = ref(false);
const tagManagementPlugin = ref<Plugin | null>(null);

// 定时器ID
let authCheckTimer: number | null = null;
const SESSION_REDIRECT_DELAY = 1000;
let sessionExpiredHandled = false;

const handleSessionExpiration = () => {
  if (sessionExpiredHandled) return;
  sessionExpiredHandled = true;

  if (authCheckTimer) {
    clearInterval(authCheckTimer);
    authCheckTimer = null;
  }

  toast.error('登录已过期，即将返回首页');

  setTimeout(() => {
    router.push('/');
  }, SESSION_REDIRECT_DELAY);
};

// 检查认证状态
const checkAuthSession = async () => {
  try {
    const response = await checkAuthStatus({ silent: true });
    const authenticated = Boolean(response.data.data?.authenticated);
    if (!authenticated) {
      handleSessionExpiration();
    }
  } catch (err: any) {
    if (err.response?.status === 401 || err.message?.includes('401')) {
      handleSessionExpiration();
    }
  }
};

// 加载插件列表
const loadPlugins = async () => {
  loading.value = true;
  error.value = '';
  
  try {
    const response = await getPluginList();
    plugins.value = response.data.data || [];
  } catch (err: any) {
    error.value = err.message || '加载插件列表失败';
  } finally {
    loading.value = false;
  }
};

// 启用插件
const handleEnable = async (id: string) => {
  try {
    await enablePlugin(id);
    await loadPlugins();
    toast.success('插件已启用');
  } catch (err: any) {
    toast.error(err.message || '启用插件失败');
  }
};

// 禁用插件
const handleDisable = async (id: string) => {
  try {
    await disablePlugin(id);
    await loadPlugins();
    toast.success('插件已禁用');
  } catch (err: any) {
    toast.error(err.message || '禁用插件失败');
  }
};

// 重新加载插件
const handleReload = async (id: string) => {
  try {
    await reloadPlugin(id);
    await loadPlugins();
    toast.success('插件已重载');
  } catch (err: any) {
    toast.error(err.message || '重新加载插件失败');
  }
};

// 卸载插件
const handleUninstall = (id: string) => {
  confirmPluginId.value = id;
  showConfirmDialog.value = true;
};

// 确认卸载插件
const confirmUninstall = async () => {
  const id = confirmPluginId.value;
  showConfirmDialog.value = false;
  
  try {
    await uninstallPlugin(id);
    await loadPlugins();
    toast.success('插件已卸载');
  } catch (err: any) {
    toast.error(err.message || '卸载插件失败');
  }
};

// 下载插件包
const handleDownload = async (pluginId: string) => {
  try {
    toast.info('正在下载插件包...');
    const response = await downloadPluginPackage(pluginId);
    
    // 检查响应数据
    if (!response.data || !(response.data instanceof Blob)) {
      throw new Error('下载失败：响应数据格式错误');
    }
    
    // 从响应头获取文件名
    const contentDisposition = response.headers['content-disposition'];
    let fileName = `${pluginId}.jar`; // 默认文件名
    
    if (contentDisposition) {
      // 支持两种格式：
      // 1. filename="xxx.jar"
      // 2. filename*=UTF-8''xxx.jar
      const fileNameMatch = contentDisposition.match(/filename\*?=(UTF-8'')?(['"]?)(.+?)\2($|;)/i);
      if (fileNameMatch && fileNameMatch[3]) {
        fileName = fileNameMatch[1] 
          ? decodeURIComponent(fileNameMatch[3]) 
          : fileNameMatch[3].replace(/['"]/g, '');
      }
    }
    
    // 创建下载链接
    const blob = response.data;
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    
    // 延迟清理，确保下载开始
    setTimeout(() => {
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    }, 100);
    
    toast.success(`插件包下载成功：${fileName}`);
  } catch (err: any) {
    console.error('下载插件包失败:', err);
    toast.error(err.message || '下载插件包失败');
  }
};

// 处理文件选择
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
    const file = target.files[0];
    // 检查文件类型
    if (file && (file.name.endsWith('.jar') || file.name.endsWith('.zip'))) {
      uploadFile.value = file;
    } else {
      uploadFile.value = null;
      toast.error('请选择 JAR 或 ZIP 文件');
    }
  }
};

// 处理文件拖拽
const handleFileDrop = (event: DragEvent) => {
  isFileDialogDragging.value = false;
  
  if (!event.dataTransfer) return;
  
  const files = event.dataTransfer.files;
  if (files && files.length > 0) {
    const file = files[0];
    // 检查文件类型
    if (file && (file.name.endsWith('.jar') || file.name.endsWith('.zip'))) {
      uploadFile.value = file;
    } else {
      uploadFile.value = null;
      toast.error('请上传 JAR 或 ZIP 文件');
    }
  }
};

// 格式化文件大小
const formatUploadFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// 上传插件
const handleUpload = async () => {
  if (!uploadFile.value) {
    toast.error('请选择要上传的文件');
    return;
  }
  
  uploading.value = true;
  
  try {
    await installPlugin(uploadFile.value);
    showUploadDialog.value = false;
    uploadFile.value = null;
    await loadPlugins();
    toast.success('插件上传成功');
  } catch (err: any) {
    toast.error(err.message || '上传插件失败');
  } finally {
    uploading.value = false;
  }
};

// 退出登录
const handleLogout = async () => {
  try {
    await logout();
  } catch {
    // 忽略错误
  } finally {
    router.push('/login');
  }
};

// 处理打包成功
const handlePackSuccess = async () => {
  await loadPlugins();
};

// 打开标签管理对话框
const handleManageTags = (plugin: Plugin) => {
  tagManagementPlugin.value = plugin;
  showTagDialog.value = true;
};

// 标签管理成功后刷新插件列表
const handleTagSuccess = async () => {
  await loadPlugins();
};

// 获取状态变体
const getStatusVariant = (status: string) => {
  switch (status) {
    case 'ENABLED':
      return 'default';
    case 'LOADED':
      return 'secondary';
    case 'DISABLED':
      return 'outline';
    case 'ERROR':
      return 'destructive';
    default:
      return 'secondary';
  }
};

// 获取状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'ENABLED':
      return '已启用';
    case 'LOADED':
      return '已加载';
    case 'DISABLED':
      return '已禁用';
    case 'ERROR':
      return '错误';
    default:
      return status;
  }
};

onMounted(() => {
  // 首次检查认证状态
  checkAuthSession();
  // 加载插件列表
  loadPlugins();
  // 启动定时检查（每30秒检查一次）
  authCheckTimer = window.setInterval(() => {
    checkAuthSession();
  }, 30000);
});

// 组件卸载时清除定时器
onUnmounted(() => {
  if (authCheckTimer) {
    clearInterval(authCheckTimer);
    authCheckTimer = null;
  }
});
</script>

<style scoped>
/* 可以添加额外的样式 */
</style>

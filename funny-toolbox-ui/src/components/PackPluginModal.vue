<template>
  <Dialog v-model:open="isOpen">
    <DialogContent class="sm:max-w-[600px] max-h-[90vh] flex flex-col p-0">
      <DialogHeader class="px-6 pt-6 pb-4 border-b flex-shrink-0">
        <DialogTitle>打包前端插件</DialogTitle>
        <DialogDescription>
          上传前端构建产物和填写插件元数据，生成符合平台规范的 ZIP 插件包
        </DialogDescription>
      </DialogHeader>

      <div class="space-y-6 px-6 py-4 overflow-y-auto flex-1">
        <!-- 文件上传区 -->
        <div class="space-y-2">
          <Label>上传静态资源文件 *</Label>
          <div
            class="flex items-center justify-center w-full"
            @drop.prevent="handleDrop"
            @dragover.prevent="isDragging = true"
            @dragenter.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
          >
            <label
              for="files-upload"
              class="flex flex-col items-center justify-center w-full h-32 border-2 border-dashed rounded-lg cursor-pointer transition-all"
              :class="isDragging ? 'border-blue-500 bg-blue-50' : 'border-gray-300 bg-gray-50 hover:bg-gray-100'"
            >
              <div class="flex flex-col items-center justify-center pt-5 pb-6">
                <Upload class="h-8 w-8 mb-2" :class="isDragging ? 'text-blue-500' : 'text-gray-400'" />
                <p class="mb-2 text-sm text-gray-500">
                  <span class="font-semibold">点击选择或拖拽文件/文件夹</span>
                </p>
                <p class="text-xs text-gray-500">支持多选文件和文件夹，自动递归包含子文件</p>
              </div>
              <input
                ref="fileInputRef"
                id="files-upload"
                type="file"
                multiple
                webkitdirectory
                class="hidden"
                @click="prepareFileInput"
                @change="handleFilesChange"
              />
            </label>
          </div>
          
          <!-- 文件列表 -->
          <div v-if="uploadedFiles.length > 0" class="mt-3 space-y-2">
            <div class="text-sm font-medium text-gray-700">
              已选择 {{ uploadedFiles.length }} 个文件 ({{ formatFileSize(getTotalSize()) }})
            </div>
            <div class="max-h-32 overflow-y-auto space-y-1 text-sm">
              <div
                v-for="(file, index) in uploadedFiles"
                :key="index"
                class="flex items-center justify-between p-2 bg-gray-50 rounded hover:bg-gray-100 transition-colors"
              >
                <span class="text-gray-700 truncate flex-1 text-xs" :title="getFilePath(file)">{{ getFilePath(file) }}</span>
                <span class="text-gray-500 text-xs mx-2 flex-shrink-0">{{ formatFileSize(file.size) }}</span>
                <Button
                  variant="ghost"
                  size="sm"
                  class="h-6 w-6 p-0 flex-shrink-0"
                  @click="removeFile(index)"
                >
                  <X class="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </div>

        <!-- 插件信息表单 -->
        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="plugin-id">插件 ID *</Label>
            <Input
              id="plugin-id"
              v-model="formData.id"
              placeholder="例如: my-plugin"
              required
            />
          </div>

          <div class="space-y-2">
            <Label for="plugin-name">插件名称 *</Label>
            <Input
              id="plugin-name"
              v-model="formData.name"
              placeholder="例如: 我的插件"
              required
            />
          </div>

          <div class="space-y-2">
            <Label for="plugin-version">版本号 *</Label>
            <Input
              id="plugin-version"
              v-model="formData.version"
              placeholder="例如: 1.0.0"
              required
            />
          </div>

          <div class="space-y-2">
            <Label for="plugin-description">描述</Label>
            <Input
              id="plugin-description"
              v-model="formData.description"
              placeholder="插件功能描述"
            />
          </div>

          <div class="space-y-2">
            <Label for="plugin-author">作者 *</Label>
            <Input
              id="plugin-author"
              v-model="formData.author"
              placeholder="作者名称"
              required
            />
          </div>

          <div class="space-y-2">
            <Label for="plugin-tags">默认标签</Label>
            <div class="space-y-2">
              <div class="flex gap-2">
                <Input
                  id="plugin-tags"
                  v-model="tagInput"
                  placeholder="输入标签后按回车"
                  @keydown.enter.prevent="addTag"
                  :disabled="tagLimitReached"
                />
                <Button
                  type="button"
                  variant="outline"
                  @click="addTag"
                  :disabled="!canAddTag"
                >
                  添加
                </Button>
              </div>
              <p class="text-xs text-gray-500">标签会写入 plugin.yml 的 tags 字段，方便后续筛选</p>
              <div v-if="tags.length" class="flex flex-wrap gap-2">
                <Badge
                  v-for="tag in tags"
                  :key="tag"
                  variant="secondary"
                  class="flex items-center gap-1 py-1 pr-1 pl-2"
                >
                  <span>{{ tag }}</span>
                  <button
                    type="button"
                    class="rounded-full p-0.5 hover:bg-white/40"
                    @click="removeTag(tag)"
                    aria-label="移除标签"
                  >
                    <X class="h-3 w-3" />
                  </button>
                </Badge>
              </div>
              <p v-else class="text-xs text-gray-400">暂未添加标签</p>
              <p v-if="tagLimitReached" class="text-xs text-red-500">最多支持 {{ MAX_TAGS }} 个标签</p>
            </div>
          </div>

          <div class="space-y-2">
            <Label for="plugin-entry">前端入口文件</Label>
            <Input
              id="plugin-entry"
              v-model="formData.frontendEntry"
              placeholder="例如: index.html（默认：index.html）"
            />
            <p class="text-xs text-gray-500">可选，相对于上传文件根目录的入口文件路径</p>
          </div>

          <!-- 图标配置 -->
          <div class="space-y-2">
            <Label>插件图标</Label>
            <div class="flex gap-2">
              <Select v-model="iconType">
                <SelectTrigger class="w-40">
                  <SelectValue placeholder="选择图标类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="FONT_AWESOME">Font Awesome</SelectItem>
                  <SelectItem value="MATERIAL">Material</SelectItem>
                  <SelectItem value="EMOJI">Emoji</SelectItem>
                  <SelectItem value="URL">URL</SelectItem>
                  <SelectItem value="SVG">SVG</SelectItem>
                </SelectContent>
              </Select>
              <Input
                v-model="iconValue"
                :placeholder="getIconPlaceholder()"
                class="flex-1"
              />
            </div>
          </div>
        </div>
      </div>

      <DialogFooter class="px-6 py-4 border-t flex-shrink-0">
        <Button variant="outline" @click="closeDialog" :disabled="packing">
          取消
        </Button>
        <Button @click="handlePack" :disabled="!isFormValid || packing">
          <span v-if="packing">打包中...</span>
          <span v-else>开始打包</span>
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { packFrontendPlugin, downloadPackedPlugin, importPackedPlugin } from '@/api';
import type { FrontendPluginPackMeta } from '@/api/types';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Upload, X } from 'lucide-vue-next';
import { toast } from 'vue-sonner';

interface Props {
  open: boolean;
}

interface Emits {
  (e: 'update:open', value: boolean): void;
  (e: 'success'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const uploadedFiles = ref<File[]>([]);
const isDragging = ref(false);
const fileInputRef = ref<HTMLInputElement | null>(null);
const formData = ref<Omit<FrontendPluginPackMeta, 'iconMeta'>>({
  id: '',
  name: '',
  version: '',
  description: '',
  author: '',
  frontendEntry: '',
});

const iconType = ref<'EMOJI' | 'URL' | 'SVG' | 'FONT_AWESOME' | 'MATERIAL'>('FONT_AWESOME');
const iconValue = ref('fas fa-tools');
const packing = ref(false);
const tags = ref<string[]>([]);
const tagInput = ref('');
const MAX_TAGS = 10;

const isOpen = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
});

const tagLimitReached = computed(() => tags.value.length >= MAX_TAGS);
const canAddTag = computed(() => tagInput.value.trim() !== '' && !tagLimitReached.value);

// 表单验证
const isFormValid = computed(() => {
  return (
    uploadedFiles.value.length > 0 &&
    formData.value.id.trim() !== '' &&
    formData.value.name.trim() !== '' &&
    formData.value.version.trim() !== '' &&
    formData.value.author.trim() !== ''
  );
});

// 处理文件选择
const handleFilesChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
    addFiles(Array.from(target.files));
  }
  
  // 重置input以允许再次选择相同的文件
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

// 添加文件到列表（去重）
const addFiles = (newFiles: File[]) => {
  const existingPaths = new Set(uploadedFiles.value.map(f => getFilePath(f)));
  const uniqueFiles = newFiles.filter(file => !existingPaths.has(getFilePath(file)));
  
  if (uniqueFiles.length > 0) {
    uploadedFiles.value = [...uploadedFiles.value, ...uniqueFiles];
    toast.success(`已添加 ${uniqueFiles.length} 个文件`);
  } else if (newFiles.length > 0) {
    toast.info('所有文件已存在，未添加重复文件');
  }
};

// 准备文件输入（移除webkitdirectory以支持文件和文件夹混选）
const prepareFileInput = () => {
  if (fileInputRef.value) {
    // 移除webkitdirectory属性，允许选择普通文件
    fileInputRef.value.removeAttribute('webkitdirectory');
  }
};

// 处理拖拽放置
const handleDrop = async (event: DragEvent) => {
  isDragging.value = false;
  
  if (!event.dataTransfer) return;
  
  const items = event.dataTransfer.items;
  const files: File[] = [];
  
  // 处理拖拽的项目
  if (items) {
    // 使用DataTransferItem API处理文件和文件夹
    const promises: Promise<void>[] = [];
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      if (item && item.kind === 'file') {
        const entry = item.webkitGetAsEntry();
        if (entry) {
          // 对于每个条目，判断是文件还是文件夹
          if (entry.isFile) {
            // 直接处理文件
            promises.push(traverseFileTree(entry, files, ''));
          } else if (entry.isDirectory) {
            // 处理文件夹，路径从文件夹名称开始
            promises.push(traverseFileTree(entry, files, entry.name));
          }
        }
      }
    }
    // 等待所有文件处理完成
    await Promise.all(promises);
  } else {
    // 降级处理：直接使用files
    const droppedFiles = event.dataTransfer.files;
    if (droppedFiles && droppedFiles.length > 0) {
      files.push(...Array.from(droppedFiles));
    }
  }
  
  if (files.length > 0) {
    addFiles(files);
  }
};

// 递归遍历文件树
const traverseFileTree = async (entry: any, files: File[], parentPath: string): Promise<void> => {
  return new Promise((resolve) => {
    if (entry.isFile) {
      entry.file((file: File) => {
        // 创建新的File对象，添加webkitRelativePath属性
        const relativePath = parentPath ? `${parentPath}/${file.name}` : file.name;
        const newFile = new File([file], file.name, { type: file.type });
        Object.defineProperty(newFile, 'webkitRelativePath', {
          value: relativePath,
          writable: false,
          configurable: true
        });
        files.push(newFile);
        resolve();
      });
    } else if (entry.isDirectory) {
      const dirReader = entry.createReader();
      dirReader.readEntries(async (entries: any[]) => {
        // 递归处理所有子项
        const promises: Promise<void>[] = [];
        for (const childEntry of entries) {
          promises.push(traverseFileTree(childEntry, files, parentPath));
        }
        await Promise.all(promises);
        resolve();
      });
    } else {
      resolve();
    }
  });
};

// 移除文件
const removeFile = (index: number) => {
  uploadedFiles.value.splice(index, 1);
};

// 获取文件路径
const getFilePath = (file: File): string => {
  return file.webkitRelativePath || file.name;
};

// 计算总大小
const getTotalSize = (): number => {
  return uploadedFiles.value.reduce((total, file) => total + file.size, 0);
};

// 获取图标占位符
const getIconPlaceholder = () => {
  switch (iconType.value) {
    case 'FONT_AWESOME':
      return '例如: fas fa-tools';
    case 'MATERIAL':
      return '例如: build';
    case 'EMOJI':
      return '例如: 🔧';
    case 'URL':
      return '例如: https://example.com/icon.png';
    case 'SVG':
      return '例如: <svg>...</svg>';
    default:
      return '';
  }
};

// 关闭对话框
const closeDialog = () => {
  isOpen.value = false;
  resetForm();
};

// 重置表单
const resetForm = () => {
  uploadedFiles.value = [];
  formData.value = {
    id: '',
    name: '',
    version: '',
    description: '',
    author: '',
    frontendEntry: '',
  };
  iconType.value = 'FONT_AWESOME';
  iconValue.value = 'fas fa-tools';
  isDragging.value = false;
  tags.value = [];
  tagInput.value = '';
  
  // 重置文件输入
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

const addTag = () => {
  const value = tagInput.value.trim();
  if (!value) {
    return;
  }
  if (value.includes(',')) {
    toast.error('标签不能包含逗号');
    return;
  }
  if (tags.value.includes(value)) {
    toast.info('标签已存在');
    tagInput.value = '';
    return;
  }
  if (tagLimitReached.value) {
    toast.error(`最多支持 ${MAX_TAGS} 个标签`);
    return;
  }
  tags.value = [...tags.value, value];
  tagInput.value = '';
};

const removeTag = (tag: string) => {
  tags.value = tags.value.filter((item) => item !== tag);
};

// 生成文件路径
const generateFilePaths = (files: File[]): string[] => {
  return files.map(file => {
    // 如果文件有webkitRelativePath，使用它；否则使用文件名
    if (file.webkitRelativePath) {
      return file.webkitRelativePath;
    }
    return file.name;
  });
};

// 处理打包
const handlePack = async () => {
  if (!isFormValid.value) {
    toast.error('请填写完整的插件信息并上传文件');
    return;
  }

  packing.value = true;

  try {
    const normalizedTags = tags.value
      .map((tag) => tag.trim())
      .filter((tag) => tag.length > 0);

    // 构建元数据
    const meta: FrontendPluginPackMeta = {
      ...formData.value,
      iconMeta: iconValue.value.trim()
        ? {
            type: iconType.value,
            value: iconValue.value,
          }
        : undefined,
    };

    // 生成文件路径
    const paths = generateFilePaths(uploadedFiles.value);

    // 调用打包接口，不立即导入
    const response = await packFrontendPlugin(
      meta,
      uploadedFiles.value,
      paths,
      false,
      normalizedTags.length ? normalizedTags : undefined
    );
    
    if (response.data.code === 200) {
      const result = response.data.data;
      const fileName = result.fileName;
      const fileSize = formatFileSize(result.fileSize);
      
      // 关闭对话框
      isOpen.value = false;
      resetForm();
      
      // 显示成功 Toast 并提供操作按钮
      toast(`插件打包成功！`, {
        description: `文件: ${fileName} (${fileSize})`,
        duration: 15000,
        action: {
          label: '下载',
          onClick: () => handleDownload(fileName),
        },
        cancel: {
          label: '导入',
          onClick: () => handleImport(fileName),
        },
      });
      
      emit('success');
    } else {
      toast.error(response.data.message || '打包失败');
    }
  } catch (err: any) {
    toast.error(err.message || '打包失败');
  } finally {
    packing.value = false;
  }
};

// 下载插件
const handleDownload = async (fileName: string) => {
  try {
    const response = await downloadPackedPlugin(fileName);
    
    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    
    toast.success('插件包下载成功');
  } catch (err: any) {
    toast.error(err.message || '下载失败');
  }
};

// 导入插件
const handleImport = async (fileName: string) => {
  try {
    const response = await importPackedPlugin(fileName, true);
    
    if (response.data.code === 200) {
      toast.success('插件导入成功，已自动启用');
      emit('success');
    } else {
      toast.error(response.data.message || '导入失败');
    }
  } catch (err: any) {
    toast.error(err.message || '导入失败');
  }
};

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};
</script>

<style scoped>
/* 滚动条样式 */
.overflow-y-auto::-webkit-scrollbar {
  width: 8px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>

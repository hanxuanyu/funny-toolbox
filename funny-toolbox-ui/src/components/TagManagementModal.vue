<template>
  <Dialog :open="open" @update:open="handleOpenChange">
    <DialogContent class="sm:max-w-[600px]">
      <DialogHeader>
        <DialogTitle>管理标签 - {{ plugin?.name }}</DialogTitle>
        <DialogDescription>
          为插件添加或移除标签，帮助用户更好地分类和查找插件
        </DialogDescription>
      </DialogHeader>

      <div class="space-y-4">
        <!-- 当前标签列表 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-2 block">当前标签</label>
          <div class="flex flex-wrap gap-2 min-h-[60px] p-3 border rounded-md bg-gray-50">
            <template v-if="currentTags.length > 0">
              <Badge
                v-for="tag in currentTags"
                :key="tag"
                variant="secondary"
                class="px-3 py-1 text-sm flex items-center gap-1.5"
              >
                {{ tag }}
                <button
                  @click="handleRemoveTag(tag)"
                  class="ml-1 hover:bg-gray-300 rounded-full p-0.5 transition-colors"
                  :disabled="removing"
                >
                  <X class="h-3 w-3" />
                </button>
              </Badge>
            </template>
            <span v-else class="text-sm text-gray-400 py-1">暂无标签</span>
          </div>
        </div>

        <!-- 添加新标签 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-2 block">添加标签</label>
          <div class="flex gap-2">
            <Input
              v-model="newTag"
              placeholder="输入新标签名称"
              @keyup.enter="handleAddTag"
              :disabled="adding"
              class="flex-1"
            />
            <Button @click="handleAddTag" :disabled="!newTag.trim() || adding">
              <Plus class="h-4 w-4 mr-1" />
              添加
            </Button>
          </div>
        </div>

        <!-- 所有可用标签 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-2 block">所有可用标签</label>
          <div class="flex flex-wrap gap-2 max-h-[120px] overflow-y-auto p-3 border rounded-md bg-gray-50">
            <template v-if="availableTags.length > 0">
              <Badge
                v-for="tag in availableTags"
                :key="tag"
                variant="outline"
                class="px-3 py-1 text-sm cursor-pointer hover:bg-blue-50 transition-colors"
                @click="quickAddTag(tag)"
              >
                {{ tag }}
                <Plus class="h-3 w-3 ml-1" />
              </Badge>
            </template>
            <span v-else class="text-sm text-gray-400 py-1">加载中...</span>
          </div>
          <p class="text-xs text-gray-500 mt-1">点击标签快速添加到当前插件</p>
        </div>

        <!-- 批量设置标签 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-2 block">批量设置（覆盖模式）</label>
          <div class="flex gap-2">
            <Input
              v-model="batchTags"
              placeholder="输入标签，用逗号分隔（如：工具,开发,实用）"
              :disabled="setting"
              class="flex-1"
            />
            <Button @click="handleSetTags" :disabled="setting" variant="outline">
              <Save class="h-4 w-4 mr-1" />
              覆盖
            </Button>
          </div>
          <p class="text-xs text-gray-500 mt-1">此操作将覆盖当前所有标签</p>
        </div>
      </div>

      <DialogFooter>
        <Button variant="outline" @click="handleClose">
          关闭
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
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
import { Badge } from '@/components/ui/badge';
import { X, Plus, Save } from 'lucide-vue-next';
import { toast } from 'vue-sonner';
import {
  getAllTags,
  getPluginTags,
  setPluginTags,
  addPluginTag,
  removePluginTag,
  type Plugin,
} from '@/api';

interface Props {
  open: boolean;
  plugin: Plugin | null;
}

interface Emits {
  (e: 'update:open', value: boolean): void;
  (e: 'success'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const currentTags = ref<string[]>([]);
const availableTags = ref<string[]>([]);
const newTag = ref('');
const batchTags = ref('');
const adding = ref(false);
const removing = ref(false);
const setting = ref(false);

// 加载所有可用标签
const loadAllTags = async () => {
  try {
    const response = await getAllTags();
    availableTags.value = response.data.data || [];
  } catch (err: any) {
    console.error('加载所有标签失败:', err);
    toast.error('加载标签列表失败');
  }
};

// 加载插件标签
const loadPluginTags = async () => {
  if (!props.plugin) return;
  
  try {
    const response = await getPluginTags(props.plugin.id);
    currentTags.value = response.data.data || [];
  } catch (err: any) {
    console.error('加载插件标签失败:', err);
    toast.error('加载插件标签失败');
  }
};

// 添加标签
const handleAddTag = async () => {
  const tag = newTag.value.trim();
  if (!tag || !props.plugin) return;
  
  if (currentTags.value.includes(tag)) {
    toast.warning('标签已存在');
    return;
  }
  
  adding.value = true;
  try {
    await addPluginTag(props.plugin.id, tag);
    currentTags.value.push(tag);
    newTag.value = '';
    toast.success('标签添加成功');
    emit('success');
    // 重新加载所有标签以更新可用标签列表
    await loadAllTags();
  } catch (err: any) {
    toast.error(err.message || '添加标签失败');
  } finally {
    adding.value = false;
  }
};

// 快速添加标签
const quickAddTag = async (tag: string) => {
  if (!props.plugin) return;
  
  if (currentTags.value.includes(tag)) {
    toast.warning('标签已存在');
    return;
  }
  
  try {
    await addPluginTag(props.plugin.id, tag);
    currentTags.value.push(tag);
    toast.success('标签添加成功');
    emit('success');
  } catch (err: any) {
    toast.error(err.message || '添加标签失败');
  }
};

// 移除标签
const handleRemoveTag = async (tag: string) => {
  if (!props.plugin) return;
  
  removing.value = true;
  try {
    await removePluginTag(props.plugin.id, tag);
    currentTags.value = currentTags.value.filter(t => t !== tag);
    toast.success('标签已移除');
    emit('success');
  } catch (err: any) {
    toast.error(err.message || '移除标签失败');
  } finally {
    removing.value = false;
  }
};

// 批量设置标签
const handleSetTags = async () => {
  if (!props.plugin) return;
  
  const tags = batchTags.value
    .split(',')
    .map(t => t.trim())
    .filter(t => t.length > 0);
  
  if (tags.length === 0) {
    toast.warning('请输入至少一个标签');
    return;
  }
  
  setting.value = true;
  try {
    await setPluginTags(props.plugin.id, tags);
    currentTags.value = tags;
    batchTags.value = '';
    toast.success('标签已更新');
    emit('success');
    // 重新加载所有标签以更新可用标签列表
    await loadAllTags();
  } catch (err: any) {
    toast.error(err.message || '设置标签失败');
  } finally {
    setting.value = false;
  }
};

// 关闭对话框
const handleClose = () => {
  emit('update:open', false);
};

// 处理对话框打开/关闭
const handleOpenChange = (value: boolean) => {
  emit('update:open', value);
};

// 监听对话框打开状态
watch(() => props.open, (isOpen) => {
  if (isOpen) {
    loadPluginTags();
    loadAllTags();
  } else {
    // 重置状态
    newTag.value = '';
    batchTags.value = '';
  }
});
</script>

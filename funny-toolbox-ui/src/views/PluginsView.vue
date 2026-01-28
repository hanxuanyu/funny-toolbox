<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 flex flex-col relative overflow-hidden">
    <!-- 静态背景装饰 -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="absolute -top-1/2 -left-1/2 w-full h-full bg-gradient-to-br from-blue-200/20 to-transparent rounded-full blur-3xl"></div>
      <div class="absolute -bottom-1/2 -right-1/2 w-full h-full bg-gradient-to-tl from-purple-200/20 to-transparent rounded-full blur-3xl"></div>
      <div class="absolute top-1/4 left-1/3 w-96 h-96 bg-indigo-200/10 rounded-full blur-3xl"></div>
    </div>

    <!-- 主内容区 -->
    <main class="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 w-full relative z-10">
      <!-- 顶部工具栏：标题 + 搜索 -->
      <div class="mb-6">
        <div class="flex items-center justify-between gap-4">
          <!-- 网站标题 -->
          <div class="flex-shrink-0">
            <h1 class="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600 bg-clip-text text-transparent">
              Funny Toolbox
            </h1>
          </div>
          
          <!-- 搜索框 - 右上角 -->
          <div class="w-full sm:w-auto sm:min-w-[280px] lg:min-w-[360px]">
            <div class="relative">
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Search class="h-4 w-4 text-gray-400" />
              </div>
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索工具..."
                class="block w-full pl-10 pr-3 py-2 border border-gray-200 rounded-lg text-sm placeholder-gray-400 bg-white/80 backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all hover:bg-white hover:shadow-sm"
              />
              <div v-if="searchQuery" class="absolute inset-y-0 right-0 pr-3 flex items-center">
                <button
                  @click="searchQuery = ''"
                  class="text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <X class="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 标签筛选工具栏 -->
      <div class="mb-8">
        <div class="bg-white/60 backdrop-blur-md rounded-xl shadow p-3">
          <div class="flex flex-wrap items-center gap-2">
            <!-- 筛选图标和标题 -->
            <div class="flex items-center gap-1.5 text-gray-700">
              <Filter class="h-3.5 w-3.5" />
              <span class="text-xs font-medium">标签:</span>
            </div>

            <!-- 已选标签 -->
            <template v-if="selectedTags.size > 0">
              <Badge
                v-for="tag in Array.from(selectedTags)"
                :key="tag"
                class="px-2 py-0.5 text-xs cursor-pointer bg-blue-100 text-blue-700 hover:bg-blue-200 transition-colors"
                @click="toggleTag(tag)"
              >
                {{ tag }}
                <X class="h-3 w-3 ml-1" />
              </Badge>
              <Button
                variant="ghost"
                size="sm"
                @click="clearTagFilters"
                class="text-xs h-6 px-2"
              >
                <X class="h-3 w-3 mr-1" />
                清除
              </Button>
              <div class="w-px h-4 bg-gray-300"></div>
            </template>

            <!-- 可用标签（始终显示前几个） -->
            <template v-if="allTags.length > 0">
              <Badge
                v-for="tag in (showTagFilter ? allTags : allTags.slice(0, 8))"
                :key="tag"
                variant="outline"
                class="px-2 py-0.5 text-xs cursor-pointer transition-all"
                :class="selectedTags.has(tag) ? 'hidden' : 'hover:bg-gray-50'"
                @click="toggleTag(tag)"
              >
                {{ tag }}
              </Badge>
              <Button
                v-if="allTags.length > 8"
                variant="ghost"
                size="sm"
                @click="showTagFilter = !showTagFilter"
                class="text-xs h-6 px-2"
              >
                {{ showTagFilter ? '收起' : `更多 (${allTags.length - 8})` }}
              </Button>
            </template>
            <span v-else class="text-xs text-gray-400">暂无标签</span>

            <!-- 匹配全部标签开关 -->
            <div class="ml-auto flex items-center gap-2 text-xs text-gray-600">
              <label class="flex items-center gap-2 cursor-pointer select-none">
                <Switch v-model="matchAllTags" />
                <span>匹配全部标签</span>
              </label>
              <span class="text-[11px] text-gray-400 hidden sm:inline">开启后需要包含所有选中标签</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="flex justify-center items-center py-20">
        <div class="text-center bg-white/60 backdrop-blur-md rounded-2xl p-8 shadow-lg">
          <div class="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
          <p class="text-gray-600">加载中...</p>
        </div>
      </div>

      <!-- 错误提示 -->
      <Alert v-else-if="error" variant="destructive" class="max-w-2xl mx-auto mb-8 bg-white/60 backdrop-blur-md border-0 shadow-lg">
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <!-- 空状态 -->
      <Card v-else-if="activePlugins.length === 0" class="max-w-2xl mx-auto p-12 bg-white/60 backdrop-blur-md border-0 shadow-lg">
        <div class="text-center text-gray-500">
          <p class="text-lg mb-2">
            {{ searchQuery ? '没有找到匹配的工具' : '暂无可用的插件' }}
          </p>
          <p class="text-sm" v-if="!searchQuery">
            <template v-if="isAuthenticated">
              请前往管理后台上传插件
            </template>
            <template v-else>
              请联系管理员上传插件
            </template>
          </p>
        </div>
      </Card>

      <!-- 插件网格 -->
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <Card
          v-for="plugin in activePlugins"
          :key="plugin.id"
          class="group hover:shadow-2xl transition-all duration-300 overflow-hidden border-0 bg-white/60 backdrop-blur-md hover:bg-white/70 hover:scale-[1.02] cursor-pointer relative flex flex-col"
          @click="openPluginInModal(plugin)"
        >
          <!-- 收藏按钮 - 绝对定位 -->
          <button
            @click.stop="toggleFavorite(plugin.id, $event)"
            class="absolute top-3 right-3 z-10 p-1.5 rounded-full transition-all duration-200 hover:bg-white/80 backdrop-blur-sm"
            :class="isFavorite(plugin.id) ? 'text-yellow-500' : 'text-gray-400 hover:text-yellow-500'"
            :title="isFavorite(plugin.id) ? '取消收藏' : '收藏'"
          >
            <Star 
              :class="isFavorite(plugin.id) ? 'fill-current' : ''"
              class="h-4 w-4"
            />
          </button>

          <CardHeader class="pb-0 pr-12">
            <div class="flex items-start gap-3">
              <!-- 左侧：图标 -->
              <div class="flex-shrink-0 w-12 h-12 flex items-center justify-center text-4xl group-hover:scale-110 transition-transform bg-gradient-to-br from-blue-100/80 to-indigo-100/80 backdrop-blur-sm rounded-lg shadow-inner">
                <PluginIcon :icon="plugin.icon" :alt="plugin.name" />
              </div>
              
              <!-- 右侧：标题 -->
              <div class="flex-1 min-w-0">
                <CardTitle class="text-base font-semibold group-hover:text-blue-600 transition-colors line-clamp-2 leading-tight mb-1">
                  {{ plugin.name }}
                </CardTitle>
                <Badge variant="outline" class="text-xs px-1.5 py-0 bg-white/50 backdrop-blur-sm border-gray-300/50">
                  v{{ plugin.version }}
                </Badge>
              </div>
            </div>
          </CardHeader>
          
          <CardContent class="pt-1.5 pb-3 flex-1 flex flex-col">
            <CardDescription class="line-clamp-3 text-sm text-gray-600 leading-relaxed flex-1">
              {{ plugin.description || '暂无描述' }}
            </CardDescription>
            
            <!-- 标签显示 -->
            <div v-if="plugin.tags && plugin.tags.length > 0" class="mt-2 flex flex-wrap gap-1">
              <Badge
                v-for="tag in plugin.tags.slice(0, 3)"
                :key="tag"
                variant="secondary"
                class="text-xs px-2 py-0 bg-blue-100/80 text-blue-700"
              >
                {{ tag }}
              </Badge>
              <Badge
                v-if="plugin.tags.length > 3"
                variant="secondary"
                class="text-xs px-2 py-0 bg-gray-100/80 text-gray-600"
              >
                +{{ plugin.tags.length - 3 }}
              </Badge>
            </div>
            
            <!-- 固定在底部的作者和链接信息 -->
            <div class="mt-2.5 flex items-center justify-between text-xs text-gray-500 flex-shrink-0">
              <span class="truncate">{{ plugin.author }}</span>
              <!-- 新窗口打开按钮 -->
              <button
                @click.stop="openPluginInNewWindow(plugin)"
                class="flex items-center gap-1 px-2 py-1 rounded-md text-blue-500 hover:text-blue-600 hover:bg-blue-50/80 transition-all duration-200 group/link"
                title="在新窗口中打开"
              >
                <ExternalLink class="h-3.5 w-3.5 group-hover/link:scale-110 transition-transform" />
                <span class="text-xs font-medium">新窗口</span>
              </button>
            </div>
          </CardContent>
        </Card>
      </div>
    </main>

    <!-- 插件模态框 -->
    <PluginModal
      v-model:open="showPluginModal"
      :plugin="selectedPlugin"
    />

    <!-- 页脚 -->
    <footer class="mt-auto py-6 border-t border-white/30 bg-white/40 backdrop-blur-md flex-shrink-0 relative z-10 shadow-lg">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex flex-col sm:flex-row justify-between items-center gap-4">
          <div class="text-sm text-gray-600">
            <p>© 2025 Funny Toolbox. 一个可扩展的工具平台.</p>
          </div>
          <div class="flex items-center gap-3">
            <Button
              variant="ghost"
              size="sm"
              @click="loadPlugins"
              title="刷新"
              class="text-gray-600 hover:text-gray-800 hover:bg-white/50 backdrop-blur-sm"
            >
              <RefreshCw class="h-4 w-4 mr-1.5" />
              刷新
            </Button>
            <Button
              v-if="isAuthenticated"
              variant="ghost"
              size="sm"
              @click="router.push('/admin')"
              class="text-gray-600 hover:text-gray-800 hover:bg-white/50 backdrop-blur-sm"
            >
              <Settings class="h-4 w-4 mr-1.5" />
              管理
            </Button>
            <Button
              v-else
              variant="ghost"
              size="sm"
              @click="router.push('/login')"
              class="text-gray-600 hover:text-gray-800 hover:bg-white/50 backdrop-blur-sm"
            >
              登录
            </Button>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getPluginList, getAllTags, checkAuthStatus, type Plugin } from '@/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Switch } from '@/components/ui/switch';
import PluginModal from '@/components/PluginModal.vue';
import PluginIcon from '@/components/PluginIcon.vue';
import { RefreshCw, Settings, Search, Star, ExternalLink, X, Filter } from 'lucide-vue-next';

const router = useRouter();

const plugins = ref<Plugin[]>([]);
const loading = ref(false);
const error = ref('');
const showPluginModal = ref(false);
const selectedPlugin = ref<Plugin | null>(null);
const searchQuery = ref('');
const favorites = ref<Set<string>>(new Set());
const isAuthenticated = ref(false);

// 标签相关状态
const allTags = ref<string[]>([]);
const selectedTags = ref<Set<string>>(new Set());
const showTagFilter = ref(false);
const matchAllTags = ref(false);

// 从本地存储加载收藏
const loadFavorites = () => {
  try {
    const saved = localStorage.getItem('plugin_favorites');
    if (saved) {
      favorites.value = new Set(JSON.parse(saved));
    }
  } catch (err) {
    console.error('加载收藏失败:', err);
  }
};

// 保存收藏到本地存储
const saveFavorites = () => {
  try {
    localStorage.setItem('plugin_favorites', JSON.stringify(Array.from(favorites.value)));
  } catch (err) {
    console.error('保存收藏失败:', err);
  }
};

// 切换收藏状态
const toggleFavorite = (pluginId: string, event: Event) => {
  event.stopPropagation(); // 阻止事件冒泡，避免触发打开插件
  
  if (favorites.value.has(pluginId)) {
    favorites.value.delete(pluginId);
  } else {
    favorites.value.add(pluginId);
  }
  saveFavorites();
};

// 检查是否收藏
const isFavorite = (pluginId: string) => {
  return favorites.value.has(pluginId);
};

// 加载所有标签
const loadAllTags = async () => {
  try {
    const response = await getAllTags();
    allTags.value = response.data.data || [];
  } catch (err: any) {
    console.error('加载标签失败:', err);
  }
};

// 切换标签选择
const toggleTag = (tag: string) => {
  if (selectedTags.value.has(tag)) {
    selectedTags.value.delete(tag);
  } else {
    selectedTags.value.add(tag);
  }
};

// 清除所有标签筛选
const clearTagFilters = () => {
  selectedTags.value.clear();
};

// 探测会话状态
const probeSessionStatus = async () => {
  try {
    const response = await checkAuthStatus({ silent: true });
    isAuthenticated.value = Boolean(response.data.data?.authenticated);
  } catch (_error) {
    isAuthenticated.value = false;
  }
};

// 过滤出已启用的插件
const activePlugins = computed(() => {
  // 只显示 ENABLED 状态的插件
  const enabled = plugins.value.filter(p => p.status === 'ENABLED');
  
  // 按标签筛选
  let filtered = enabled;
  if (selectedTags.value.size > 0) {
    filtered = enabled.filter(p => {
      if (!p.tags || p.tags.length === 0) return false;
      const tags = Array.from(selectedTags.value);
      return matchAllTags.value
        ? tags.every(tag => p.tags!.includes(tag))
        : tags.some(tag => p.tags!.includes(tag));
    });
  }
  
  // 如果有搜索关键词，进行过滤
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase();
    filtered = filtered.filter(p => 
      p.name.toLowerCase().includes(query) ||
      p.description?.toLowerCase().includes(query) ||
      p.author?.toLowerCase().includes(query) ||
      p.tags?.some(tag => tag.toLowerCase().includes(query))
    );
  }
  
  // 按收藏状态排序：收藏的插件排在前面
  return filtered.sort((a, b) => {
    const aFav = isFavorite(a.id) ? 1 : 0;
    const bFav = isFavorite(b.id) ? 1 : 0;
    return bFav - aFav;
  });
});

// 加载插件列表
const loadPlugins = async () => {
  loading.value = true;
  error.value = '';
  
  try {
    const response = await getPluginList();
    plugins.value = response.data.data || [];
    // 刷新时也重新加载标签列表
    await loadAllTags();
    console.log('加载的插件列表:', plugins.value);
    console.log('已启用的插件:', activePlugins.value);
  } catch (err: any) {
    error.value = err.message || '加载插件列表失败';
    console.error('加载插件失败:', err);
  } finally {
    loading.value = false;
  }
};

// 在模态框中打开插件
const openPluginInModal = (plugin: Plugin) => {
  if (!plugin.frontendEntry) {
    alert('此插件暂无前端页面');
    return;
  }
  
  selectedPlugin.value = plugin;
  showPluginModal.value = true;
};

// 在新窗口中打开插件
const openPluginInNewWindow = (plugin: Plugin) => {
  if (!plugin.frontendEntry) {
    alert('此插件暂无前端页面');
    return;
  }
  
  // 构建插件URL，使用与PluginModal相同的逻辑
  const backendBaseURL = import.meta.env.VITE_API_BASE_URL || '';
  
  let pluginUrl = plugin.frontendEntry;
  
  // 如果不是完整URL，需要拼接后端地址
  if (!pluginUrl.startsWith('http://') && !pluginUrl.startsWith('https://')) {
    if (pluginUrl.startsWith('/')) {
      pluginUrl = backendBaseURL + pluginUrl;
    } else {
      pluginUrl = `${backendBaseURL}/${pluginUrl}`;
    }
  }
  
  window.open(pluginUrl, '_blank', 'noopener,noreferrer');
};

onMounted(() => {
  loadFavorites();
  loadPlugins();
  loadAllTags();
  probeSessionStatus();
});
</script>

<style scoped>
/* 确保卡片内容不超出 */
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.leading-tight {
  line-height: 1.3;
}
</style>

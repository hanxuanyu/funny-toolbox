<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4 sm:px-6 lg:px-8">
    <Card class="w-full max-w-md shadow-xl">
      <CardHeader class="space-y-1">
        <CardTitle class="text-2xl font-bold text-center">Funny Toolbox</CardTitle>
        <CardDescription class="text-center">
          登录后台管理系统
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form @submit.prevent="handleLogin" class="space-y-4">
          <div class="space-y-2">
            <Label for="username">用户名</Label>
            <Input
              id="username"
              v-model="formData.username"
              type="text"
              placeholder="请输入用户名"
              required
              autocomplete="username"
            />
          </div>
          <div class="space-y-2">
            <Label for="password">密码</Label>
            <Input
              id="password"
              v-model="formData.password"
              type="password"
              placeholder="请输入密码"
              required
              autocomplete="current-password"
            />
          </div>
          
          <Alert v-if="error" variant="destructive">
            <AlertDescription>{{ error }}</AlertDescription>
          </Alert>
          
          <Button type="submit" class="w-full" :disabled="loading">
            <span v-if="loading">登录中...</span>
            <span v-else>登录</span>
          </Button>
        </form>
      </CardContent>
      <CardFooter class="text-center text-sm text-gray-500">
        <p class="w-full">登录后可管理平台插件</p>
      </CardFooter>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { login } from '@/api';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';

const router = useRouter();
const route = useRoute();

const formData = reactive({
  username: '',
  password: '',
});

const loading = ref(false);
const error = ref('');

const handleLogin = async () => {
  error.value = '';
  loading.value = true;
  
  try {
    await login(formData);
    
    // 跳转到目标页面或管理页
    const redirect = route.query.redirect as string || '/admin';
    router.push(redirect);
  } catch (err: any) {
    error.value = err.message || '登录失败，请检查用户名和密码';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* 可以添加额外的样式 */
</style>

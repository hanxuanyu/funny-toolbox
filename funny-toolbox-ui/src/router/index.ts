import { createRouter, createWebHistory } from 'vue-router';
import { checkAuthStatus } from '@/api';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/PluginsView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/AdminView.vue'),
      meta: { requiresAuth: true },
    },
  ],
});

/**
 * 路由守卫：检查认证状态
 */
router.beforeEach(async (to) => {
  const needsAuthCheck = Boolean(to.meta.requiresAuth || to.name === 'login');

  if (!needsAuthCheck) {
    return true;
  }

  let authenticated = false;

  try {
    const response = await checkAuthStatus({ silent: true });
    authenticated = Boolean(response.data.data?.authenticated);
  } catch (_error) {
    authenticated = false;
  }

  if (to.meta.requiresAuth) {
    return authenticated ? true : { name: 'home' };
  }

  if (to.name === 'login' && authenticated) {
    return { name: 'home' };
  }

  return true;
});

export default router;

// router/index.js 完整重构

import { createWebHistory, createRouter } from 'vue-router'
import { useTokenStore } from '@/stores/token'
import useUserInfoStore from '@/stores/userInfo.js'
import { normalizeRole, routeRoleAllows, canEnterAdmin } from '@/utils/roles'

// 通用页面
import LoginVue from '@/views/Login.vue'

// Admin布局和页面
import AdminLayoutVue from '@/layouts/AdminLayout.vue'
import AdminHomeVue from '@/views/admin/AdminHome.vue' // 新建：后台首页仪表盘
import NoteCategoryVue from '@/views/admin/NoteCategory.vue' // 移入admin目录
import NoteManageVue from '@/views/admin/NoteManage.vue' // 移入admin目录
import RoleManageVue from '@/views/admin/RoleManage.vue' // 新建：角色管理
// User个人中心（Admin和User共用）
import UserAvatarVue from '@/views/user/UserAvatar.vue'
import UserInfo from '@/views/user/UserInfo.vue'
import UserResetPasswordVue from '@/views/user/UserResetPassword.vue'

// User布局和页面
import UserLayoutVue from '@/layouts/UserLayout.vue'
import UserHomeVue from '@/views/user/UserHome.vue' // 新建：发现页瀑布流
import PublishVue from '@/views/user/Publish.vue' // 新建：发布页
import NotificationVue from '@/views/user/Notification.vue' // 新建：通知页

const routes = [
  { path: '/login', component: LoginVue },

  // Admin路由
  {
    path: '/admin',
    component: AdminLayoutVue,
    meta: { requiresAuth: true, role: 'admin' },
    redirect: '/admin/home',
    children: [
      { path: 'home', component: AdminHomeVue, meta: { title: '后台首页' } },
      { path: 'topic', component: NoteCategoryVue, meta: { title: '话题管理' } },
      { path: 'note', component: NoteManageVue, meta: { title: '笔记管理' } },
      { path: 'role', component: RoleManageVue, meta: { title: '角色管理' } },

    ],
  },

  // User路由
  {
    path: '/',
    component: UserLayoutVue,
    meta: { requiresAuth: true, role: 'user' },
    redirect: '/home',
    children: [
      { path: 'home', component: UserHomeVue, meta: { title: '发现' } },
      { path: 'publish', component: PublishVue, meta: { title: '发布' } },
      { path: 'notification', component: NotificationVue, meta: { title: '通知' } },
    ],
  },
  // 个人中心路由 - Admin和User共用
  {
    path: '/user',
    meta: { requiresAuth: true },
    redirect: '/user/info',
    children: [
      { path: 'info', component: UserInfo, meta: { title: '基本资料' } },
      { path: 'avatar', component: UserAvatarVue, meta: { title: '更换头像' } },
      { path: 'resetPassword', component: UserResetPasswordVue, meta: { title: '重置密码' } },
    ],
  },

  // 403无权限页面
  { path: '/403', component: () => import('@/views/403.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const tokenStore = useTokenStore()
  const userInfoStore = useUserInfoStore()
  const token = tokenStore.token
  const resolvedRole = normalizeRole(
    userInfoStore.info?.role ?? userInfoStore.role,
  )

  // 登录页直接放行
  if (to.path === '/login') {
    if (token && resolvedRole) {
      // 已登录，根据角色跳转
      next(canEnterAdmin(resolvedRole) ? '/admin/home' : '/home')
    } else {
      next()
    }
    return
  }

  // 需要登录但无token
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  // 角色权限验证（大小写与 admin/user 规则见 utils/roles）
  if (to.meta.role && !routeRoleAllows(to.meta.role, resolvedRole)) {
    next('/403')
    return
  }

  next()
})

export default router

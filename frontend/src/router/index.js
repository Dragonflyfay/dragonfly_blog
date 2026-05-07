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
      { path: 'user/info', component: UserInfo, meta: { title: '基本资料' } },
      { path: 'user/avatar', component: UserAvatarVue, meta: { title: '更换头像' } },
      { path: 'user/resetPassword', component: UserResetPasswordVue, meta: { title: '重置密码' } },
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
      { path: 'user/info', component: UserInfo, meta: { title: '基本资料' } },
      { path: 'user/avatar', component: UserAvatarVue, meta: { title: '更换头像' } },
      { path: 'user/resetPassword', component: UserResetPasswordVue, meta: { title: '重置密码' } },
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

  // 获取token（支持多种来源）
  let token = tokenStore.token
  if (!token) {
    token = localStorage.getItem('global_token')
    if (!token) {
      const tabId = sessionStorage.getItem('tab_id')
      if (tabId) {
        token = sessionStorage.getItem(`token_${tabId}`)
      }
    }
  }

  // 获取用户角色
  let resolvedRole = normalizeRole(
      userInfoStore.info?.role ?? userInfoStore.role,
  )

  // 如果store中没有，尝试从存储中恢复
  if (!resolvedRole) {
    // 先检查全局存储
    const globalInfo = localStorage.getItem('global_userInfo')
    if (globalInfo) {
      try {
        const info = JSON.parse(globalInfo)
        resolvedRole = normalizeRole(info?.role)
      } catch (e) {
        console.error('解析全局用户信息失败:', e)
      }
    } else {
      // 再检查标签页存储
      const tabId = sessionStorage.getItem('tab_id')
      if (tabId) {
        const tabInfo = sessionStorage.getItem(`userInfo_${tabId}`)
        if (tabInfo) {
          try {
            const info = JSON.parse(tabInfo)
            resolvedRole = normalizeRole(info?.role)
          } catch (e) {
            console.error('解析标签页用户信息失败:', e)
          }
        }
      }
    }
  }

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

  // 角色权限验证
  if (to.meta.role && !routeRoleAllows(to.meta.role, resolvedRole)) {
    next('/403')
    return
  }

  // 管理员访问 /user/* 路径时，重定向到 /admin/user/*
  if (canEnterAdmin(resolvedRole) && to.path.startsWith('/user/')) {
    const adminPath = to.path.replace(/^\/user\//, '/admin/user/')
    next(adminPath)
    return
  }

  next()
})

export default router

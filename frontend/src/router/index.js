import { createWebHistory, createRouter } from 'vue-router'
//导入组件
import LoginVue from '@/views/Login.vue'
import LayoutVue from '@/views/Layout.vue'
import ArticleCategoryVue from '@/views/article/ArticleCategory.vue'
import ArticleManageVue from '@/views/article/ArticleManage.vue'
import UserAvatarVue from '@/views/user/UserAvatar.vue'
import UserInfo from '@/views/user/UserInfo.vue'
import UserResetPasswordVue from '@/views/user/UserResetPassword.vue'
import { useTokenStore } from '@/stores/token'

//定义路由关系
const routes = [
  { path: '/login', component: LoginVue },
  {
    path: '/',
    component: LayoutVue,
    redirect: '/article/manage',
    children: [
      { path: '/article/category', component: ArticleCategoryVue },
      { path: '/article/manage', component: ArticleManageVue },
      { path: '/user/avatar', component: UserAvatarVue },
      { path: '/user/info', component: UserInfo },
      { path: '/user/resetPassword', component: UserResetPasswordVue },
    ],
  },
]

//创建路由器
const router = createRouter({
  history: createWebHistory(),
  routes: routes,
})
// // 添加路由守卫

// router.beforeEach((to, from, next) => {
//   const tokenStore = useTokenStore()
//   // 如果目标路径是登录页，直接允许访问
//   const rememberMe = localStorage.getItem('rememberMe') === true
//   const savedToken = localStorage.getItem('token')
//   if (to.path === '/login') {
//     // 如果用户已登录，重定向到主页
//     if (tokenStore.token && savedToken) {
//       next('/')
//     } else {
//       next()
//     }
//   } else {
//     // 如果不是登录页且用户未登录，重定向到登录页
//     if (!tokenStore.token && !savedToken) {
//       next('/login')
//     } else {
//       if (!tokenStore.token && savedToken) {
//         tokenStore.setToken(savedToken)
//       }
//       next()
//     }
//   }
// })

//导出路由
export default router

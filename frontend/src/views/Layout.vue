<script setup>
import {
  Management,
  Promotion,
  UserFilled,
  User,
  Crop,
  EditPen,
  SwitchButton,
  CaretBottom,
  Sunny,
  Moon,
} from '@element-plus/icons-vue'
import avatar from '@/assets/default.png'
import { ref } from 'vue'
import router from '@/router'
import { useRoute } from 'vue-router'
import { useTokenStore } from '@/stores/token.js'
import { ElMessage } from 'element-plus'

const route = useRoute()
const isDark = ref(false)
const tokenStore = useTokenStore()

const toggleTheme = () => {
  isDark.value = !isDark.value
}

const handleCommand = (command) => {
  switch(command) {
    case 'profile':
      router.push('/user/info')
      break
    case 'avatar':
      router.push('/user/avatar')
      break
    case 'password':
      router.push('/user/resetPassword')
      break
    case 'logout':
      tokenStore.removeToken()
      router.push('/login')
      ElMessage.success('已退出登录')
      break
  }
}
</script>

<template>
  <el-container class="layout-container" :class="{ 'dark-mode': isDark }">
    <!-- 左侧菜单 -->
    <el-aside width="260px">
      <div class="el-aside__logo">
        <div class="logo-wrapper">
          <img src="@/assets/logo.png" alt="logo" class="logo-img" />
          <span class="logo-text">dragonfly 笔记</span>
        </div>
        <div class="logo-slogan">记录生活 · 分享美好</div>
      </div>
      <el-menu
        :default-active="route.path"
        active-text-color="#c5a3ff"
        background-color="transparent"
        text-color="rgba(255,255,255,0.75)"
        router
        class="custom-menu"
      >
        <el-menu-item index="/article/category">
          <el-icon><Management /></el-icon>
          <span>文章分类</span>
        </el-menu-item>
        <el-menu-item index="/article/manage">
          <el-icon><Promotion /></el-icon>
          <span>笔记管理</span>
        </el-menu-item>
        <el-sub-menu index="profile">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>个人中心</span>
          </template>
          <el-menu-item index="/user/info">
            <el-icon><User /></el-icon>
            <span>基本资料</span>
          </el-menu-item>
          <el-menu-item index="/user/avatar">
            <el-icon><Crop /></el-icon>
            <span>更换头像</span>
          </el-menu-item>
          <el-menu-item index="/user/resetPassword">
            <el-icon><EditPen /></el-icon>
            <span>重置密码</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
      <div class="aside-footer">
        <div class="floating-emoji-mini">✨</div>
        <div class="floating-emoji-mini">🦋</div>
      </div>
    </el-aside>
    <!-- 右侧主区域 -->
    <el-container>
      <!-- 头部区域 -->
      <el-header>
        <div class="header-left">
          <div class="greeting">
            <span class="greeting-text">下午好，</span>
            <strong class="username-display">东哥</strong>
          </div>
          <div class="theme-toggle" @click="toggleTheme">
            <el-icon v-if="!isDark"><Sunny /></el-icon>
            <el-icon v-else><Moon /></el-icon>
          </div>
        </div>
        <el-dropdown placement="bottom-end" class="user-dropdown" @command="handleCommand">
          <span class="el-dropdown__box">
            <el-avatar :src="avatar" class="user-avatar" />
            <span class="user-name">东哥</span>
            <el-icon class="dropdown-icon"><CaretBottom /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu class="custom-dropdown">
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>基本资料
              </el-dropdown-item>
              <el-dropdown-item command="avatar">
                <el-icon><Crop /></el-icon>更换头像
              </el-dropdown-item>
              <el-dropdown-item command="password">
                <el-icon><EditPen /></el-icon>重置密码
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <!-- 中间区域 -->
      <el-main>
        <div class="content-container">
          <router-view></router-view>
        </div>
      </el-main>
      <!-- 底部区域 -->
      <el-footer>
        <span>dragonfly 笔记</span>
        <span class="footer-dot">•</span>
        <span>记录每一次灵感</span>
      </el-footer>
    </el-container>
  </el-container>
</template>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);

  .el-aside {
    background-color: #232323;

    &__logo {
      height: 120px;
      background: url('@/assets/logo.png') no-repeat center / 120px auto;
    }

    .el-menu {
      border-right: none;
      --el-menu-active-color: #c5a3ff;
    }
  }

  .el-header {
    background-color: rgba(255, 255, 255, 0.96);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    border-bottom: 1px solid #f0e5ff;

    .welcome-text {
      font-size: 18px;
      font-weight: 600;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }

    .el-dropdown__box {
      display: flex;
      align-items: center;

      .el-icon {
        color: #c5a3ff;
        margin-left: 10px;
      }

      &:active,
      &:focus {
        outline: none;
      }
    }
  }

  .el-main {
    padding: 20px;
    background: linear-gradient(145deg, #ffffff 0%, #fef9ff 100%);
  }

  .el-footer {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    color: #666;
    background-color: rgba(255, 255, 255, 0.96);
    border-top: 1px solid #f0e5ff;
  }
}
</style>

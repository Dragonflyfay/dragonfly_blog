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

const route = useRoute()
const isDark = ref(false)
const toggleTheme = () => {
  isDark.value = !isDark.value
}
</script>

<template>
  <!-- Element-plus中的容器 -->
  <el-container class="layout-container" :class="{ 'dark-mode': isDark }">
    <!-- 左侧菜单 -->
    <el-aside width="260px">
      <!-- el-aside 菜单标签 -->
      <div class="el-aside__logo">
        <div class="logo-wrapper">
          <img src="@/assets/logo.png" alt="logo" class="logo-img" />
          <span class="logo-text">dragonfly 笔记</span>
        </div>
        <div class="logo-slogan">记录生活 · 分享美好</div>
      </div>
      <!-- 菜单栏 -->
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

        <!-- 可折叠 菜单 -->
        <el-sub-menu index="profile">
          <!-- 每一个下拉框的元素 -->
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

      <!-- 左下角装饰 -->
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
            <strong class="username-display">username</strong>
          </div>
          <!-- 主题切换 -->
          <div class="theme-toggle" @click="toggleTheme">
            <el-icon v-if="!isDark"><Sunny /></el-icon>
            <el-icon v-else><Moon /></el-icon>
          </div>
        </div>
        <!--  下拉菜单-->
        <el-dropdown placement="bottom-end" class="user-dropdown">
          <span class="el-dropdown__box">
            <el-avatar :src="avatar" class="user-avatar" />
            <span class="user-name">username</span>
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
  background: linear-gradient(145deg, #faf5ff 0%, #f0eaf8 100%);
  transition: all 0.3s ease;

  // 左侧菜单
  .el-aside {
    width: 260px;
    background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
    position: relative;
    overflow: hidden;
    display: flex;
    flex-direction: column;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 200px;
      background: radial-gradient(circle at 30% 20%, rgba(197, 163, 255, 0.15), transparent);
      pointer-events: none;
    }

    &__logo {
      padding: 32px 20px 24px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      margin-bottom: 24px;

      .logo-wrapper {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 12px;

        .logo-img {
          width: 44px;
          height: 44px;
          filter: drop-shadow(0 4px 12px rgba(197, 163, 255, 0.3));
        }

        .logo-text {
          font-size: 20px;
          font-weight: 700;
          background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
          -webkit-background-clip: text;
          background-clip: text;
          color: transparent;
        }
      }

      .logo-slogan {
        font-size: 11px;
        color: rgba(255, 255, 255, 0.4);
        letter-spacing: 1px;
        padding-left: 56px;
      }
    }

    .custom-menu {
      flex: 1;
      border-right: none;
      background: transparent;
      padding: 0 12px;

      :deep(.el-menu-item),
      :deep(.el-sub-menu__title) {
        border-radius: 16px;
        margin: 4px 0;
        transition: all 0.3s ease;

        &:hover {
          background-color: rgba(197, 163, 255, 0.2);
          color: #c5a3ff;
        }
      }

      :deep(.el-menu-item.is-active) {
        background: linear-gradient(135deg, rgba(197, 163, 255, 0.25), rgba(248, 180, 217, 0.2));
        color: #c5a3ff;

        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 50%;
          transform: translateY(-50%);
          width: 3px;
          height: 20px;
          background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
          border-radius: 0 3px 3px 0;
        }
      }

      :deep(.el-sub-menu.is-opened .el-sub-menu__title) {
        color: #c5a3ff;
      }
    }

    .aside-footer {
      padding: 20px;
      text-align: center;

      .floating-emoji-mini {
        display: inline-block;
        font-size: 14px;
        margin: 0 4px;
        opacity: 0.4;
        animation: floatMini 4s infinite ease-in-out;
      }

      .floating-emoji-mini:nth-child(2) {
        animation-delay: 1.5s;
      }
    }
  }

  // 右侧主区域
  .el-header {
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(20px);
    border-bottom: 1px solid rgba(197, 163, 255, 0.15);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 32px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 20px;

      .greeting {
        .greeting-text {
          font-size: 14px;
          color: #8b7aa8;
        }

        .username-display {
          font-size: 18px;
          font-weight: 600;
          background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
          -webkit-background-clip: text;
          background-clip: text;
          color: transparent;
        }
      }

      .theme-toggle {
        width: 36px;
        height: 36px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        background: rgba(197, 163, 255, 0.1);
        cursor: pointer;
        transition: all 0.3s ease;
        color: #c5a3ff;

        &:hover {
          background: rgba(197, 163, 255, 0.25);
          transform: rotate(15deg);
        }
      }
    }

    .user-dropdown {
      .el-dropdown__box {
        display: flex;
        align-items: center;
        gap: 10px;
        cursor: pointer;
        padding: 6px 16px 6px 12px;
        border-radius: 60px;
        background: rgba(197, 163, 255, 0.08);
        transition: all 0.3s ease;

        &:hover {
          background: rgba(197, 163, 255, 0.18);
        }

        .user-avatar {
          width: 40px;
          height: 40px;
          border: 2px solid #c5a3ff;
          box-shadow: 0 0 0 2px rgba(197, 163, 255, 0.2);
        }

        .user-name {
          font-size: 14px;
          font-weight: 500;
          color: #4a3a6e;
        }

        .dropdown-icon {
          color: #c5a3ff;
          font-size: 14px;
        }
      }
    }
  }

  .el-main {
    padding: 24px 32px;
    background: transparent;

    .content-container {
      background: rgba(255, 255, 255, 0.6);
      backdrop-filter: blur(4px);
      border-radius: 32px;
      padding: 24px;
      min-height: 100%;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
      transition: all 0.3s ease;
    }
  }

  .el-footer {
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(12px);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    font-size: 12px;
    color: #a09ab5;
    border-top: 1px solid rgba(197, 163, 255, 0.1);

    .footer-dot {
      font-size: 16px;
      color: #c5a3ff;
    }
  }
}

// 深色模式
.layout-container.dark-mode {
  background: linear-gradient(145deg, #1a1a2e 0%, #16213e 100%);

  .el-header {
    background: rgba(26, 26, 46, 0.85);
    border-bottom-color: rgba(197, 163, 255, 0.2);

    .header-left {
      .greeting {
        .greeting-text {
          color: #8e82b0;
        }
      }
    }

    .user-dropdown .el-dropdown__box {
      .user-name {
        color: #c5a3ff;
      }
    }
  }

  .el-main .content-container {
    background: rgba(26, 26, 46, 0.5);
    backdrop-filter: blur(8px);
  }

  .el-footer {
    background: rgba(26, 26, 46, 0.7);
    color: #6b5f8e;
  }
}

@keyframes floatMini {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}
</style>

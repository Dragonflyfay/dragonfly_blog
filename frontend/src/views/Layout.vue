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
import { useRouter } from 'vue-router'

import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRouter()
const isDark = ref(false)

const toggleTheme = () => {
  isDark.value = !isDark.value
}
import { useTokenStore } from '@/stores/token.js'
const tokenStore = useTokenStore()

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('你确认要退出吗', '温馨提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
      .then(() => {
        // 退出登录
        // 清空pinia存储的token和用户信息
        tokenStore.removeToken()
        userInfoStore.removeInfo()
        // 跳转到登陆界面
        router.push('/login')

        ElMessage.success('退出登录成功')
      })
      .catch((error) => {
        // 只有当用户真正取消操作时才显示此消息
        if (error !== 'cancel' && error !== undefined) {
          console.error('退出失败:', error)
          ElMessage.error('退出失败')
        } else {
          ElMessage({
            type: 'info',
            message: '你取消了退出登录',
          })
        }
      })
  } else {
    router.push('/user/' + command)
  }
}

//调用函数，获取用户详细信息
import { userInfoService } from '@/api/user.js'
import useUserInfoStore from '@/stores/userInfo.js'
const userInfoStore = useUserInfoStore()
const getUserInfo = async () => {
  let reuslt = await userInfoService()

  //存到pinia
  userInfoStore.setInfo(reuslt.data)
}
getUserInfo()
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
        <div class="floating-emoji-mini"></div>
        <div class="floating-emoji-mini"></div>
      </div>
    </el-aside>
    <!-- 右侧主区域 -->
    <el-container>
      <!-- 头部区域 -->
      <el-header>
        <div class="header-left">
          <div class="greeting">
            <span class="greeting-text">你好，</span>
            <strong class="username-display">{{ userInfoStore.info.nickname }}</strong>
          </div>
          <div class="theme-toggle" @click="toggleTheme">
            <el-icon v-if="!isDark"><Sunny /></el-icon>
            <el-icon v-else><Moon /></el-icon>
          </div>
        </div>

        <!--下拉菜单-->
        <!--        command:条目被点击后触发，在事件函数上声明一个参数，接受条目对应的指令-->
        <el-dropdown placement="bottom-end" class="user-dropdown" @command="handleCommand">
          <span class="el-dropdown__box">
            <el-avatar
              :src="userInfoStore.info.userPic ? userInfoStore.info.userPic : avatar"
              class="user-avatar"
            />
            <span class="user-name">{{ userInfoStore.info.nickname }}</span>
            <el-icon class="dropdown-icon"><CaretBottom /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu class="custom-dropdown">
              <el-dropdown-item command="info">
                <el-icon><User /></el-icon>基本资料
              </el-dropdown-item>
              <el-dropdown-item command="avatar">
                <el-icon><Crop /></el-icon>更换头像
              </el-dropdown-item>
              <el-dropdown-item command="resetPassword">
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
  transition: all 0.3s ease;

  &.dark-mode {
    background: linear-gradient(145deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);

    .el-header {
      background-color: rgba(30, 30, 46, 0.96);
      border-bottom: 1px solid #2d2d44;

      .greeting-text {
        color: #e0e0e0;
      }

      .username-display {
        color: #c5a3ff;
      }
    }

    .el-main {
      background: linear-gradient(145deg, #1e1e2e 0%, #252538 100%);
    }

    .el-footer {
      background-color: rgba(30, 30, 46, 0.96);
      border-top: 1px solid #2d2d44;
      color: #a0a0b0;
    }
  }

  .el-aside {
    background: linear-gradient(180deg, #2d2d44 0%, #1e1e2e 100%);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: -50%;
      left: -50%;
      width: 200%;
      height: 200%;
      background: radial-gradient(circle, rgba(197, 163, 255, 0.1) 0%, transparent 70%);
      animation: rotate 20s linear infinite;
      pointer-events: none;
    }

    &__logo {
      height: 120px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 20px;
      background: transparent;

      .logo-wrapper {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .logo-img {
          width: 40px;
          height: 40px;
          object-fit: contain;
          filter: drop-shadow(0 2px 8px rgba(197, 163, 255, 0.3));
        }

        .logo-text {
          font-size: 20px;
          font-weight: 700;
          background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
          -webkit-background-clip: text;
          background-clip: text;
          color: transparent;
          letter-spacing: 1px;
        }
      }

      .logo-slogan {
        font-size: 12px;
        color: rgba(255, 255, 255, 0.5);
        letter-spacing: 2px;
      }
    }

    .custom-menu {
      border-right: none;
      background: transparent;
      --el-menu-active-color: #c5a3ff;

      :deep(.el-menu-item) {
        margin: 8px 12px;
        border-radius: 12px;
        transition: all 0.3s ease;

        &:hover {
          background: rgba(197, 163, 255, 0.15);
          transform: translateX(4px);
        }

        &.is-active {
          background: linear-gradient(135deg, rgba(197, 163, 255, 0.2), rgba(248, 180, 217, 0.2));
          box-shadow: 0 4px 12px rgba(197, 163, 255, 0.2);
        }
      }

      :deep(.el-sub-menu__title) {
        margin: 8px 12px;
        border-radius: 12px;
        transition: all 0.3s ease;

        &:hover {
          background: rgba(197, 163, 255, 0.15);
        }
      }
    }

    .aside-footer {
      position: absolute;
      bottom: 20px;
      left: 0;
      right: 0;
      display: flex;
      justify-content: center;
      gap: 8px;

      .floating-emoji-mini {
        width: 6px;
        height: 6px;
        background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
        border-radius: 50%;
        opacity: 0.4;
        animation: float 3s ease-in-out infinite;

        &:nth-child(2) {
          animation-delay: 1.5s;
        }
      }
    }
  }

  .el-header {
    background-color: rgba(255, 255, 255, 0.96);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    border-bottom: 1px solid #f0e5ff;
    backdrop-filter: blur(10px);

    .header-left {
      display: flex;
      align-items: center;
      gap: 20px;

      .greeting {
        .greeting-text {
          font-size: 16px;
          color: #666;
          transition: color 0.3s ease;
        }

        .username-display {
          font-size: 18px;
          font-weight: 600;
          background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
          -webkit-background-clip: text;
          background-clip: text;
          color: transparent;
        }
      }

      .theme-toggle {
        cursor: pointer;
        padding: 8px;
        border-radius: 50%;
        transition: all 0.3s ease;
        display: flex;
        align-items: center;
        justify-content: center;

        &:hover {
          background: rgba(197, 163, 255, 0.1);
          transform: rotate(180deg);
        }

        .el-icon {
          font-size: 20px;
          color: #c5a3ff;
        }
      }
    }

    .user-dropdown {
      .el-dropdown__box {
        display: flex;
        align-items: center;
        gap: 10px;
        cursor: pointer;
        padding: 6px 12px;
        border-radius: 48px;
        transition: all 0.3s ease;

        &:hover {
          background: rgba(197, 163, 255, 0.1);
        }

        .user-avatar {
          border: 2px solid #c5a3ff;
          box-shadow: 0 2px 8px rgba(197, 163, 255, 0.3);
        }

        .user-name {
          font-size: 14px;
          color: #333;
          font-weight: 500;
        }

        .dropdown-icon {
          color: #c5a3ff;
          font-size: 14px;
        }

        &:active,
        &:focus {
          outline: none;
        }
      }
    }
  }

  .el-main {
    padding: 24px;
    background: linear-gradient(145deg, #ffffff 0%, #fef9ff 100%);
    min-height: calc(100vh - 180px);

    .content-container {
      max-width: 1400px;
      margin: 0 auto;
    }
  }

  .el-footer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    font-size: 14px;
    color: #666;
    background-color: rgba(255, 255, 255, 0.96);
    border-top: 1px solid #f0e5ff;
    padding: 16px;
    backdrop-filter: blur(10px);

    .footer-dot {
      color: #c5a3ff;
      font-size: 12px;
    }
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  50% {
    transform: translateY(-10px);
    opacity: 0.7;
  }
}
</style>

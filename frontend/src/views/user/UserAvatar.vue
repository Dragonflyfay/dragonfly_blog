<!-- UserAvatar.vue -->
<script setup>
import { Plus, Upload } from '@element-plus/icons-vue'
import { ref } from 'vue'
import avatar from '@/assets/default.png'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const uploadRef = ref()

import { useTokenStore } from '@/stores/token'
const tokenStore = useTokenStore()

//导入用户头像信息
import useUserInfoStore from '@/stores/userInfo.js'
const userInfoStore = useUserInfoStore()

//用户头像地址
const imgUrl = ref(userInfoStore.info.userPic)

const uploadsuccess = (res) => {
  // console.log(res)
  imgUrl.value = res.data
  // console.log(imgUrl.value)
  // console.log(userInfoStore.info)
  //userInfoStore.info.userPic = imgUrl.value
  // console.log(userInfoStore.info)
}

//更新用户头像
import { userUpdateAvatarService } from '@/api/user.js'
const updateAvatar = async () => {
  //调用接口
  let res = await userUpdateAvatarService(imgUrl.value)
  if (res.code === 200) {
    ElMessage.success('修改成功')
  }
  //修改pinia中的用户信息
  userInfoStore.info.userPic = imgUrl.value
}
</script>

<template>
  <div class="avatar-container">
    <div class="avatar-card">
      <!-- 头部装饰区 -->
      <div class="avatar-header">
        <div class="header-decoration">
          <span class="deco-dot"></span>
          <span class="deco-dot"></span>
          <span class="deco-dot"></span>
        </div>
        <h1 class="avatar-title">📷 更换头像</h1>
        <p class="avatar-subtitle">上传属于你的专属头像，让朋友们记住你 ~</p>
      </div>

      <!-- 主要内容区 -->
      <div class="avatar-content">
        <div class="avatar-preview-area">
          <div class="preview-wrapper">
            <div class="preview-decoration left">✨</div>
            <el-upload
              ref="uploadRef"
              class="avatar-uploader"
              :show-file-list="false"
              :auto-upload="true"
              action="/api/upload"
              name="file"
              :headers="{ Authorization: tokenStore.token }"
              :on-success="uploadsuccess"
            >
              <img v-if="imgUrl" :src="imgUrl" class="avatar-img" />
              <div v-else class="avatar-placeholder">
                <el-icon class="placeholder-icon"><Plus /></el-icon>
                <span>点击上传</span>
              </div>
            </el-upload>
            <div class="preview-decoration right">🌸</div>
          </div>

          <div class="avatar-actions">
            <el-button
              type="primary"
              :icon="Plus"
              size="large"
              class="select-btn"
              @click="uploadRef.$el.querySelector('input').click()"
            >
              选择图片
            </el-button>
            <el-button
              type="success"
              :icon="Upload"
              size="large"
              class="upload-btn"
              @click="updateAvatar"
            >
              上传头像
            </el-button>
          </div>
        </div>

        <div class="avatar-tips">
          <div class="tips-icon">💡</div>
          <div class="tips-text">
            <p>支持 jpg、png 格式</p>
            <p>建议上传正方形图片，效果更佳</p>
          </div>
        </div>
      </div>

      <!-- 底部装饰 -->
      <div class="avatar-footer">
        <span>✨</span>
        <span>换个头像换个心情</span>
        <span>🦋</span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.avatar-container {
  min-height: calc(100vh - 60px);
  padding: 20px;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
}

.avatar-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  backdrop-filter: blur(10px);
}

.avatar-header {
  padding: 32px 40px;
  background: linear-gradient(135deg, rgba(248, 180, 217, 0.3) 0%, rgba(197, 163, 255, 0.3) 100%);
  border-bottom: 2px solid rgba(197, 163, 255, 0.2);
  position: relative;

  .header-decoration {
    position: absolute;
    top: 20px;
    left: 30px;
    display: flex;
    gap: 8px;

    .deco-dot {
      width: 6px;
      height: 6px;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      border-radius: 50%;

      &:nth-child(2) {
        width: 8px;
        height: 8px;
        opacity: 0.6;
      }

      &:nth-child(3) {
        width: 10px;
        height: 10px;
        opacity: 0.3;
      }
    }
  }

  .avatar-title {
    font-size: 26px;
    font-weight: 700;
    margin: 0 0 8px 0;
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }

  .avatar-subtitle {
    font-size: 13px;
    color: #a09abf;
    margin: 0;
    letter-spacing: 0.5px;
  }
}

.avatar-content {
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

.avatar-preview-area {
  text-align: center;
}

.preview-wrapper {
  position: relative;
  display: inline-block;

  .preview-decoration {
    position: absolute;
    font-size: 24px;
    opacity: 0.6;
    animation: float 3s ease-in-out infinite;

    &.left {
      left: -30px;
      top: 50%;
      transform: translateY(-50%);
      animation-delay: 0s;
    }

    &.right {
      right: -30px;
      top: 50%;
      transform: translateY(-50%);
      animation-delay: 1.5s;
    }
  }
}

.avatar-uploader {
  :deep() {
    .avatar-img {
      width: 200px;
      height: 200px;
      border-radius: 50%;
      display: block;
      object-fit: cover;
      box-shadow: 0 8px 24px rgba(197, 163, 255, 0.3);
      transition: all 0.3s ease;

      &:hover {
        transform: scale(1.02);
        box-shadow: 0 12px 32px rgba(197, 163, 255, 0.4);
      }
    }

    .avatar-placeholder {
      width: 200px;
      height: 200px;
      border-radius: 50%;
      background: linear-gradient(135deg, #faf7ff 0%, #f0e5ff 100%);
      border: 3px dashed #c5a3ff;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        border-color: #f8b4d9;
        background: linear-gradient(135deg, #fff 0%, #faf7ff 100%);
        transform: scale(1.02);
      }

      .placeholder-icon {
        font-size: 48px;
        color: #c5a3ff;
      }

      span {
        font-size: 14px;
        color: #c5a3ff;
      }
    }

    .el-upload {
      cursor: pointer;
    }
  }
}

.avatar-actions {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-top: 32px;

  .select-btn {
    background: linear-gradient(135deg, #a8e6cf 0%, #c2f5e9 100%);
    border: none;
    border-radius: 48px;
    padding: 12px 28px;
    font-weight: 500;
    color: #2c665a;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(168, 230, 207, 0.4);
    }
  }

  .upload-btn {
    background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
    border: none;
    border-radius: 48px;
    padding: 12px 28px;
    font-weight: 500;
    color: white;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
    }
  }
}

.avatar-tips {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  background: rgba(197, 163, 255, 0.08);
  border-radius: 20px;

  .tips-icon {
    font-size: 28px;
  }

  .tips-text {
    p {
      margin: 4px 0;
      font-size: 13px;
      color: #a09abf;
    }
  }
}

.avatar-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 20px;
  background: rgba(197, 163, 255, 0.05);
  font-size: 14px;
  color: #a09abf;
  border-top: 1px solid rgba(197, 163, 255, 0.1);
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-8px) rotate(3deg);
  }
}
</style>

<!-- UserInfo.vue -->
<script setup>
import { ref, reactive } from 'vue'
import useUserInfoStore from '@/stores/userInfo.js'
const userInfoStore = useUserInfoStore()
const userInfo = reactive({
  ...userInfoStore.info,
})
const rules = {
  nickname: [
    { required: true, message: '请输入用户昵称', trigger: 'blur' },
    {
      pattern: /^\S{2,10}$/,
      message: '昵称必须是2-10位的非空字符串',
      trigger: 'blur',
    },
  ],
  email: [
    { required: true, message: '请输入用户邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}
//修改用户信息
import { userInfoService } from '@/api/user.js'
import { ElMessage } from 'element-plus'
const updateUserInfo = async () => {
  //调用接口
  let result = await userInfoService(userInfo)
  ElMessage.success(result.message ? result.message : '修改成功')
  userInfoStore.setInfo(userInfo)
}
</script>

<template>
  <div class="userinfo-container">
    <div class="userinfo-card">
      <!-- 头部装饰区 -->
      <div class="userinfo-header">
        <div class="header-decoration">
          <span class="deco-dot"></span>
          <span class="deco-dot"></span>
          <span class="deco-dot"></span>
        </div>
        <h1 class="userinfo-title">📝 基本资料</h1>
        <p class="userinfo-subtitle">完善个人信息，让朋友们更好地认识你 ~</p>
      </div>

      <!-- 表单区域 -->
      <div class="userinfo-content">
        <div class="form-wrapper">
          <el-form
            :model="userInfo"
            :rules="rules"
            label-width="100px"
            size="large"
            class="info-form"
          >
            <el-form-item label="登录名称">
              <el-input v-model="userInfo.username" disabled class="disabled-input"></el-input>
            </el-form-item>

            <el-form-item label="用户昵称" prop="nickname">
              <el-input
                v-model="userInfo.nickname"
                placeholder="请输入昵称"
                class="custom-input"
              ></el-input>
            </el-form-item>

            <el-form-item label="用户邮箱" prop="email">
              <el-input
                v-model="userInfo.email"
                placeholder="请输入邮箱"
                class="custom-input"
              ></el-input>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" class="submit-btn" @click="updateUserInfo">
                ✨ 保存修改 ✨
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="info-tips">
          <div class="tips-card">
            <span class="tips-emoji">💡</span>
            <span>昵称会在笔记和评论中展示哦</span>
          </div>
          <div class="tips-card">
            <span class="tips-emoji">📧</span>
            <span>邮箱用于接收重要通知，请填写真实邮箱</span>
          </div>
        </div>
      </div>

      <!-- 底部装饰 -->
      <div class="userinfo-footer">
        <span>🦋</span>
        <span>让每一次记录都与众不同</span>
        <span>✨</span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.userinfo-container {
  min-height: calc(100vh - 60px);
  padding: 20px;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
}

.userinfo-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.userinfo-header {
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

  .userinfo-title {
    font-size: 26px;
    font-weight: 700;
    margin: 0 0 8px 0;
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }

  .userinfo-subtitle {
    font-size: 13px;
    color: #a09abf;
    margin: 0;
    letter-spacing: 0.5px;
  }
}

.userinfo-content {
  padding: 40px;
  display: flex;
  gap: 48px;
  flex-wrap: wrap;

  @media (max-width: 768px) {
    flex-direction: column;
  }
}

.form-wrapper {
  flex: 2;
  min-width: 300px;
}

.info-form {
  :deep(.el-form-item__label) {
    color: #6a4a9c;
    font-weight: 500;
  }

  .custom-input {
    :deep(.el-input__wrapper) {
      border-radius: 48px;
      padding: 8px 20px;
      background-color: #faf7ff;
      border: 1px solid #f0e5ff;
      transition: all 0.3s ease;
      box-shadow: none;

      &:hover {
        border-color: #d9b8ff;
        background-color: #fff;
      }

      &.is-focus {
        border-color: #c5a3ff;
        background-color: #fff;
        box-shadow: 0 0 0 4px rgba(197, 163, 255, 0.12);
      }
    }
  }

  .disabled-input {
    :deep(.el-input__wrapper) {
      border-radius: 48px;
      padding: 8px 20px;
      background-color: #f0e5ff;
      border: 1px solid #e8ddf8;
      box-shadow: none;

      .el-input__inner {
        color: #a09abf;
      }
    }
  }

  .submit-btn {
    background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
    border: none;
    border-radius: 48px;
    padding: 14px 32px;
    font-size: 16px;
    font-weight: 500;
    color: white;
    transition: all 0.3s ease;
    width: auto;
    min-width: 160px;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
    }
  }
}

.info-tips {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;

  .tips-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 20px;
    background: linear-gradient(135deg, #faf7ff 0%, #fef9ff 100%);
    border-radius: 20px;
    border: 1px solid rgba(197, 163, 255, 0.2);
    font-size: 13px;
    color: #a09abf;

    .tips-emoji {
      font-size: 24px;
    }
  }
}

.userinfo-footer {
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
</style>

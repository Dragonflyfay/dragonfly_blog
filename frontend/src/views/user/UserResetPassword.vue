<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { userUpdatePwdService } from '@/api/user.js'

// 表单数据
const pwdData = reactive({
  old_pwd: '',
  new_pwd: '',
  re_pwd: '',
})

// 表单验证规则
const rules = {
  old_pwd: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { min: 5, max: 16, message: '密码长度为5-16位非空字符', trigger: 'blur' },
  ],
  new_pwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 5, max: 16, message: '密码长度为5-16位非空字符', trigger: 'blur' },
  ],
  re_pwd: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { min: 5, max: 16, message: '密码长度为5-16位非空字符', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdData.new_pwd) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

// 表单引用
const formRef = ref()

// 重置密码
const resetPassword = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      let result = await userUpdatePwdService(pwdData)
      ElMessage.success(result.message || '密码修改成功')

      pwdData.old_pwd = ''
      pwdData.new_pwd = ''
      pwdData.re_pwd = ''
      formRef.value.clearValidate()
    } catch (error) {
      console.error('密码修改失败:', error)
    }
  })
}

// 重置表单
const resetForm = () => {
  pwdData.old_pwd = ''
  pwdData.new_pwd = ''
  pwdData.re_pwd = ''
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}
</script>

<template>
  <div class="reset-password-container">
    <div class="reset-card">
      <!-- 头部装饰 -->
      <div class="reset-header">
        <div class="header-decoration">
          <span class="deco-dot"></span>
          <span class="deco-dot"></span>
          <span class="deco-dot"></span>
        </div>
        <div class="header-content">
          <div class="title-section">
            <h1 class="reset-title">🔐 重置密码</h1>
            <p class="reset-subtitle">为了账号安全，请设置一个强密码吧 ~</p>
          </div>
          <div class="header-emoji">
            <span class="emoji-float">🛡️</span>
            <span class="emoji-float">💎</span>
            <span class="emoji-float">✨</span>
          </div>
        </div>
      </div>

      <!-- 表单区域 -->
      <div class="reset-body">
        <div class="form-wrapper">
          <el-form
            ref="formRef"
            :model="pwdData"
            :rules="rules"
            label-width="110px"
            size="large"
            class="reset-form"
          >
            <el-form-item label="原密码" prop="old_pwd">
              <el-input
                v-model="pwdData.old_pwd"
                type="password"
                placeholder="请输入原密码"
                show-password
                class="custom-input"
              >
                <template #prefix>
                  <span class="input-icon">🔑</span>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="新密码" prop="new_pwd">
              <el-input
                v-model="pwdData.new_pwd"
                type="password"
                placeholder="请输入新密码（5-16位）"
                show-password
                class="custom-input"
              >
                <template #prefix>
                  <span class="input-icon">✨</span>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="确认密码" prop="re_pwd">
              <el-input
                v-model="pwdData.re_pwd"
                type="password"
                placeholder="请再次输入新密码"
                show-password
                class="custom-input"
              >
                <template #prefix>
                  <span class="input-icon">✅</span>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <div class="button-group">
                <el-button type="primary" class="submit-btn" @click="resetPassword">
                  <span class="btn-emoji">✨</span> 确认修改 <span class="btn-emoji">✨</span>
                </el-button>
                <el-button class="reset-form-btn" @click="resetForm">
                  <span class="btn-emoji">🗑️</span> 清空表单
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <!-- 安全提示卡片 -->
        <div class="tips-wrapper">
          <div class="tips-card">
            <div class="tips-header">
              <span class="tips-icon">💡</span>
              <span class="tips-title">密码安全小贴士</span>
            </div>
            <ul class="tips-list">
              <li><span class="list-icon">✓</span> 密码长度 5-16 位</li>
              <li><span class="list-icon">✓</span> 建议包含字母 + 数字</li>
              <li><span class="list-icon">✓</span> 避免使用生日、手机号等个人信息</li>
              <li><span class="list-icon">✓</span> 不要与其他平台密码重复</li>
            </ul>
          </div>

          <div class="strength-tip">
            <div class="strength-header">
              <span class="strength-icon">🔒</span>
              <span>密码强度</span>
            </div>
            <div class="strength-bars">
              <span class="bar"></span>
              <span class="bar"></span>
              <span class="bar"></span>
              <span class="bar"></span>
            </div>
            <p class="strength-text">设置包含大小写字母+数字+符号的密码更安全哦</p>
          </div>
        </div>
      </div>

      <!-- 底部装饰 -->
      <div class="reset-footer">
        <div class="footer-decoration">
          <span>🦋</span>
          <span class="footer-text">保护你的账号安全，从设置一个好密码开始</span>
          <span>🌟</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.reset-password-container {
  min-height: calc(100vh - 60px);
  padding: 20px;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
}

.reset-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 32px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  backdrop-filter: blur(10px);
}

.reset-header {
  padding: 28px 36px;
  background: linear-gradient(135deg, rgba(248, 180, 217, 0.3) 0%, rgba(197, 163, 255, 0.3) 100%);
  border-bottom: 2px solid rgba(197, 163, 255, 0.15);
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
      transition: all 0.3s ease;

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

  .header-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 20px;
  }

  .title-section {
    .reset-title {
      font-size: 26px;
      font-weight: 700;
      margin: 0 0 8px 0;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }

    .reset-subtitle {
      font-size: 13px;
      color: #a09abf;
      margin: 0;
      letter-spacing: 0.3px;
    }
  }

  .header-emoji {
    display: flex;
    gap: 16px;

    .emoji-float {
      font-size: 28px;
      opacity: 0.6;
      animation: float 3s ease-in-out infinite;

      &:nth-child(2) {
        animation-delay: 0.8s;
      }

      &:nth-child(3) {
        animation-delay: 1.6s;
      }
    }
  }
}

.reset-body {
  display: flex;
  flex-wrap: wrap;
  gap: 40px;
  padding: 40px 48px;

  @media (max-width: 900px) {
    flex-direction: column;
    padding: 32px 36px;
  }
}

.form-wrapper {
  flex: 2;
  min-width: 300px;
}

.reset-form {
  :deep(.el-form-item__label) {
    color: #6a4a9c;
    font-weight: 500;
    font-size: 14px;
  }
}

.custom-input {
  width: 100%;

  .input-icon {
    font-size: 16px;
    margin-right: 6px;
  }

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
      transform: translateY(-1px);
    }

    &.is-focus {
      border-color: #c5a3ff;
      background-color: #fff;
      box-shadow: 0 0 0 4px rgba(197, 163, 255, 0.12);
    }
  }

  :deep(.el-input__inner) {
    &::placeholder {
      color: #c5c0d4;
      font-size: 13px;
    }
  }
}

.button-group {
  display: flex;
  gap: 20px;
  margin-top: 16px;
  flex-wrap: wrap;

  .submit-btn {
    flex: 1;
    background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
    border: none;
    border-radius: 48px;
    padding: 14px 24px;
    font-size: 15px;
    font-weight: 500;
    color: white;
    transition: all 0.3s ease;
    min-width: 140px;

    .btn-emoji {
      font-size: 14px;
    }

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
    }

    &:active {
      transform: translateY(0);
    }
  }

  .reset-form-btn {
    flex: 1;
    background: linear-gradient(135deg, #e0d4ff 0%, #f0e5ff 100%);
    border: 1px solid rgba(197, 163, 255, 0.3);
    border-radius: 48px;
    padding: 14px 24px;
    font-size: 15px;
    font-weight: 500;
    color: #7a5a9e;
    transition: all 0.3s ease;
    min-width: 140px;

    .btn-emoji {
      font-size: 14px;
    }

    &:hover {
      transform: translateY(-2px);
      border-color: #c5a3ff;
      background: #fff;
      box-shadow: 0 4px 12px rgba(197, 163, 255, 0.2);
    }
  }
}

.tips-wrapper {
  flex: 1.2;
  min-width: 260px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.tips-card {
  background: linear-gradient(135deg, #faf7ff 0%, #fef9ff 100%);
  border-radius: 24px;
  padding: 24px;
  border: 1px solid rgba(197, 163, 255, 0.2);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 12px 24px rgba(197, 163, 255, 0.12);
  }

  .tips-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 2px dashed rgba(197, 163, 255, 0.3);

    .tips-icon {
      font-size: 28px;
    }

    .tips-title {
      font-size: 16px;
      font-weight: 600;
      color: #6a4a9c;
    }
  }

  .tips-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 0;
      color: #a09abf;
      font-size: 13px;
      border-bottom: 1px solid rgba(197, 163, 255, 0.1);

      &:last-child {
        border-bottom: none;
      }

      .list-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 20px;
        height: 20px;
        background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
        border-radius: 50%;
        color: white;
        font-size: 11px;
        font-weight: bold;
      }
    }
  }
}

.strength-tip {
  background: linear-gradient(135deg, #fff5f8 0%, #fef5ff 100%);
  border-radius: 24px;
  padding: 20px;
  border: 1px solid rgba(248, 180, 217, 0.3);

  .strength-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    font-size: 14px;
    font-weight: 500;
    color: #d47fa6;
  }

  .strength-bars {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;

    .bar {
      flex: 1;
      height: 6px;
      background: linear-gradient(135deg, #e0d4ff, #f0e5ff);
      border-radius: 3px;
      transition: all 0.3s ease;

      &:nth-child(1) {
        background: linear-gradient(135deg, #ff6b6b, #ff9a9e);
      }
      &:nth-child(2) {
        background: linear-gradient(135deg, #ffb347, #ffcc80);
      }
      &:nth-child(3) {
        background: linear-gradient(135deg, #4ecdc4, #6dd5b1);
      }
      &:nth-child(4) {
        background: linear-gradient(135deg, #a8e6cf, #7ee0b5);
      }
    }
  }

  .strength-text {
    font-size: 11px;
    color: #b0a7c0;
    margin: 0;
    line-height: 1.5;
  }
}

.reset-footer {
  padding: 18px 36px;
  background: rgba(197, 163, 255, 0.05);
  border-top: 1px solid rgba(197, 163, 255, 0.1);

  .footer-decoration {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    font-size: 14px;
    color: #a09abf;

    .footer-text {
      font-size: 13px;
      letter-spacing: 0.5px;
    }

    span:first-child,
    span:last-child {
      font-size: 16px;
      animation: pulse 2s ease-in-out infinite;
    }
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.6;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}
</style>

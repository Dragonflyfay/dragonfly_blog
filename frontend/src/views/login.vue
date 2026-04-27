<script setup>
import { User, Lock } from '@element-plus/icons-vue'

import { ref } from 'vue'
//美化
import { ElMessage } from 'element-plus'

const isRegister = ref(false)

//定义响应式数据类型
const registerData = ref({
  username: '',
  password: '',
  rePassword: '',
})
//校验密码的函数
const checkrePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else if (value !== registerData.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

//定义表单规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 5, max: 16, message: '长度为5-16非空字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 16, message: '长度为5-16非空字符', trigger: 'blur' },
  ],
  rePassword: [{ validator: checkrePassword, trigger: 'blur' }],
}

//调用后台接口，实现注册
import { userRegisterService, userLoginService } from '@/api/user'
const register = async () => {
  try {
    let result = await userRegisterService(registerData.value)
    console.log('注册接口返回:', result) // 调试用，查看实际返回结构

    //拦截器已经处理了
    ElMessage.success('注册成功')
  } catch (error) {
    console.error('注册请求异常:', error)
  }
}
//绑定数据，复用注册表单的数据模型,v-model
//表单数据校验
//登录函数
import { useTokenStore } from '@/stores/token'

import { useRouter } from 'vue-router'
const router = useRouter()
const tokenStore = useTokenStore()
const login = async () => {
  //调用接口
  try {
    let result = await userLoginService(registerData.value)

    ElMessage.success('登录成功')
    //把得到的token存储到pinia
    tokenStore.setToken(result.data.token)
    //跳转到首页  路由完成跳转
    router.push('/')
  } catch (err) {
    console.error('登录请求异常：', err)
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 左侧装饰区 -->
      <div class="decorative-area">
        <div class="logo-wrapper">
          <img src="@/assets/logo.png" alt="logo" class="logo" />
          <span class="logo-text">dragonfly 笔记</span>
        </div>
        <div class="slogan">
          <p>记录生活</p>
          <p>分享美好</p>
        </div>
        <div class="floating-emoji">✨</div>
        <div class="floating-emoji">🦋</div>
        <div class="floating-emoji">🌸</div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-area">
        <!-- 注册表单 -->
        <el-form
          ref="form"
          size="large"
          autocomplete="off"
          v-if="isRegister"
          class="auth-form"
          :model="registerData"
          :rules="rules"
        >
          <div class="form-header">
            <h1 class="form-title">注册账号</h1>
            <p class="form-subtitle">加入dragonfly笔记,开始你的创作之旅</p>
          </div>

          <el-form-item prop="username">
            <el-input
              :prefix-icon="User"
              placeholder="请输入用户名"
              class="custom-input"
              v-model="registerData.username"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              :prefix-icon="Lock"
              type="password"
              placeholder="请输入密码"
              v-model="registerData.password"
              class="custom-input"
            />
          </el-form-item>

          <el-form-item prop="rePassword">
            <el-input
              :prefix-icon="Lock"
              type="password"
              placeholder="请再次输入密码"
              v-model="registerData.rePassword"
              class="custom-input"
            />
          </el-form-item>

          <el-form-item>
            <el-button class="auth-button" type="primary" size="large" @click="register"
              >注册新账号</el-button
            >
          </el-form-item>

          <div class="form-footer">
            <span>已有账号？</span>
            <el-link
              type="primary"
              underline="hover"
              @click="isRegister = false"
              class="switch-link"
            >
              去登录 →
            </el-link>
          </div>
        </el-form>

        <!-- 登录表单 -->
        <el-form
          ref="form"
          size="large"
          autocomplete="off"
          v-else
          class="auth-form"
          :model="registerData"
          :rules="rules"
        >
          <div class="form-header">
            <h1 class="form-title">欢迎回来</h1>
            <p class="form-subtitle">很高兴再次见到你</p>
          </div>

          <el-form-item prop="username">
            <el-input
              :prefix-icon="User"
              placeholder="请输入用户名"
              class="custom-input"
              v-model="registerData.username"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              name="password"
              :prefix-icon="Lock"
              v-model="registerData.password"
              type="password"
              placeholder="请输入密码"
              class="custom-input"
            />
          </el-form-item>

          <div class="login-options">
            <el-checkbox class="remember-checkbox">记住我</el-checkbox>
            <el-link type="primary" underline="hover" class="forgot-link">忘记密码？</el-link>
          </div>

          <el-form-item>
            <el-button class="auth-button" type="primary" size="large" @click="login"
              >登录</el-button
            >
          </el-form-item>

          <div class="form-footer">
            <span>还没有账号？</span>
            <el-link
              type="primary"
              underline="hover"
              @click="isRegister = true"
              class="switch-link"
            >
              立即注册 →
            </el-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
  padding: 20px;
}

.login-card {
  width: 1000px;
  max-width: 95%;
  min-height: 600px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 40px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 30px 50px rgba(0, 0, 0, 0.08);
}

/* 左侧装饰区 */
.decorative-area {
  flex: 1;
  background: linear-gradient(135deg, #f8b4d9 0%, #c5a3ff 50%, #a8e6cf 100%);
  padding: 48px 32px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 60px;
  z-index: 2;
}

.logo {
  width: 48px;
  height: 48px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.08));
}

.logo-text {
  font-size: 24px;
  font-weight: 700;
  color: white;
  letter-spacing: 2px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 12px;
  border-radius: 40px;
  backdrop-filter: blur(4px);
}

.slogan {
  margin-top: auto;
  margin-bottom: 60px;
  z-index: 2;

  p {
    font-size: 28px;
    font-weight: 600;
    color: white;
    margin: 8px 0;
    line-height: 1.3;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
    background: rgba(255, 255, 255, 0.15);
    display: inline-block;
    padding: 4px 16px;
    border-radius: 60px;
    backdrop-filter: blur(4px);
  }
}

.floating-emoji {
  position: absolute; //绝对定位
  font-size: 28px;
  opacity: 0.35;
  animation: float 6s infinite ease-in-out;
}

.floating-emoji:nth-child(3) {
  top: 15%;
  left: 10%;
  animation-delay: 0s;
}

.floating-emoji:nth-child(4) {
  bottom: 20%;
  right: 12%;
  animation-delay: 1s;
  font-size: 36px;
}

.floating-emoji:nth-child(5) {
  top: 55%;
  left: 15%;
  animation-delay: 2s;
  font-size: 32px;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-15px) rotate(5deg);
  }
}

/* 右侧表单区 */
.form-area {
  flex: 1;
  padding: 48px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #ffffff 0%, #fef9ff 100%);
}

.auth-form {
  width: 100%;
  max-width: 320px;
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.form-subtitle {
  font-size: 13px;
  color: #b0a7c0;
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

  :deep(.el-input__prefix) {
    margin-right: 12px;
    color: #c5a3ff;
  }
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.remember-checkbox {
  :deep(.el-checkbox__label) {
    color: #a09aaf;
    font-size: 13px;
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: #c5a3ff;
    border-color: #c5a3ff;
  }
}

.forgot-link {
  font-size: 13px;
  color: #c5a3ff;
}

.auth-button {
  width: 100%;
  background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
  border: none;
  border-radius: 48px;
  padding: 14px;
  font-size: 16px;
  font-weight: 500;
  color: white;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
  }

  &:active {
    transform: translateY(0);
  }
}

.form-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: #b0a7c0;

  .switch-link {
    margin-left: 8px;
    font-weight: 500;
    color: #c5a3ff;
  }
}
</style>

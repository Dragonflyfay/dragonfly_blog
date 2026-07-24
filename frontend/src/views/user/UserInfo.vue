<!-- UserInfo.vue -->
<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, Lock, Message, Plus, Refresh, Upload, User } from '@element-plus/icons-vue'
import avatar from '@/assets/default.png'
import { useTokenStore } from '@/stores/token'
import useUserInfoStore from '@/stores/userInfo.js'
import {
  userInfoService,
  userUpdateAvatarService,
  userUpdatePwdService,
  userUpdateService,
} from '@/api/user.js'

const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()

const goBack = () => router.back()

const profileFormRef = ref()
const passwordFormRef = ref()
const uploadRef = ref()
const profileSaving = ref(false)
const avatarSaving = ref(false)
const passwordSaving = ref(false)

const profileForm = reactive({
  id: userInfoStore.info.id,
  username: userInfoStore.info.username,
  nickname: userInfoStore.info.nickname || '',
  email: userInfoStore.info.email || '',
  userPic: userInfoStore.info.userPic || '',
})

const passwordForm = reactive({
  old_pwd: '',
  new_pwd: '',
  re_pwd: '',
})

const uploadHeaders = computed(() => ({ Authorization: tokenStore.token }))

const syncUserInfo = (patch) => {
  const nextInfo = {
    ...userInfoStore.info,
    ...patch,
  }
  userInfoStore.setInfo(nextInfo, userInfoStore.rememberMe)
  window.dispatchEvent(new CustomEvent('userInfoUpdated', { detail: nextInfo }))
}

const profileRules = {
  nickname: [
    { required: true, message: '请输入用户昵称', trigger: 'blur' },
    {
      pattern: /^\S{1,10}$/,
      message: '昵称必须是1-10位非空字符',
      trigger: 'blur',
    },
  ],
  email: [
    { required: true, message: '请输入用户邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}

const validateRepeatPassword = (rule, value, callback) => {
  if (value !== passwordForm.new_pwd) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const passwordRules = {
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
    { validator: validateRepeatPassword, trigger: 'blur' },
  ],
}

const refreshUserInfo = async () => {
  try {
    const result = await userInfoService()
    if (!result.data) return

    Object.assign(profileForm, {
      id: result.data.id,
      username: result.data.username,
      nickname: result.data.nickname || '',
      email: result.data.email || '',
      userPic: result.data.userPic || '',
    })
    syncUserInfo(result.data)
    ElMessage.success('资料已刷新')
  } catch (error) {
    console.error('刷新资料失败:', error)
  }
}

const updateProfile = async () => {
  if (!profileFormRef.value) return

  await profileFormRef.value.validate(async (valid) => {
    if (!valid) return

    profileSaving.value = true
    try {
      const payload = {
        id: profileForm.id,
        nickname: profileForm.nickname,
        email: profileForm.email,
      }
      const result = await userUpdateService(payload)
      syncUserInfo(payload)
      ElMessage.success(result.message || '资料保存成功')
    } catch (error) {
      console.error('资料保存失败:', error)
    } finally {
      profileSaving.value = false
    }
  })
}

const beforeAvatarUpload = (file) => {
  const isImage = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'].includes(
    file.type,
  )
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    ElMessage.error('头像仅支持 jpg、png、gif、webp 格式')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('头像大小不能超过 10MB')
    return false
  }
  return true
}

const uploadSuccess = (res) => {
  if (res.code !== 0) {
    ElMessage.error(res.message || '头像上传失败')
    return
  }
  profileForm.userPic = res.data
  ElMessage.success('头像已上传，请保存')
}

const uploadError = () => {
  ElMessage.error('头像上传失败')
}

const chooseAvatar = () => {
  const uploadInput = uploadRef.value?.$el?.querySelector('input')
  uploadInput?.click()
}

const updateAvatar = async () => {
  if (!profileForm.userPic) {
    ElMessage.warning('请先上传头像')
    return
  }

  avatarSaving.value = true
  try {
    const result = await userUpdateAvatarService(profileForm.userPic)
    syncUserInfo({ userPic: profileForm.userPic })
    ElMessage.success(result.message || '头像保存成功')
  } catch (error) {
    console.error('头像保存失败:', error)
  } finally {
    avatarSaving.value = false
  }
}

const updatePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    passwordSaving.value = true
    try {
      const result = await userUpdatePwdService(passwordForm)
      ElMessage.success(result.message || '密码修改成功')
      resetPasswordForm()
    } catch (error) {
      console.error('密码修改失败:', error)
    } finally {
      passwordSaving.value = false
    }
  })
}

const resetPasswordForm = () => {
  passwordForm.old_pwd = ''
  passwordForm.new_pwd = ''
  passwordForm.re_pwd = ''
  passwordFormRef.value?.clearValidate()
}
</script>

<template>
  <div class="edit-container">
    <!-- 返回按钮 -->
    <button class="back-btn" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回</span>
    </button>

    <!-- ===== 头像卡片 ===== -->
    <div class="info-card avatar-card">
      <div class="card-bg"></div>
      <div class="card-body">
        <div class="section-label">
          <el-icon><User /></el-icon>
          <span>头像</span>
        </div>
        <p class="section-desc">选择一个你喜欢的的图片作为头像</p>

        <div class="avatar-area">
          <el-upload
            ref="uploadRef"
            class="avatar-uploader"
            :show-file-list="false"
            :auto-upload="true"
            action="/api/upload"
            name="file"
            :headers="uploadHeaders"
            :before-upload="beforeAvatarUpload"
            :on-success="uploadSuccess"
            :on-error="uploadError"
          >
            <img :src="profileForm.userPic || avatar" class="avatar-img" alt="用户头像" />
            <div class="avatar-mask">
              <el-icon><Plus /></el-icon>
              <span>更换</span>
            </div>
          </el-upload>

          <div class="avatar-btns">
            <el-button class="btn-outline" :icon="Plus" @click="chooseAvatar">选择图片</el-button>
            <el-button
              class="btn-gradient"
              :icon="Upload"
              :loading="avatarSaving"
              @click="updateAvatar"
            >
              保存头像
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 基本资料卡片 ===== -->
    <div class="info-card profile-card">
      <div class="card-bg"></div>
      <div class="card-body">
        <div class="card-header-row">
          <div class="section-label">
            <el-icon><Message /></el-icon>
            <span>基本资料</span>
          </div>
          <el-button class="btn-outline btn-sm" :icon="Refresh" @click="refreshUserInfo"
            >刷新</el-button
          >
        </div>
        <p class="section-desc">设置你的昵称和邮箱，让朋友们更好地认识你</p>

        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="88px"
          size="large"
          class="info-form"
        >
          <el-form-item label="登录名称">
            <el-input v-model="profileForm.username" disabled class="input-disabled" />
          </el-form-item>

          <el-form-item label="用户昵称" prop="nickname">
            <el-input
              v-model="profileForm.nickname"
              placeholder="请输入昵称"
              maxlength="10"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="用户邮箱" prop="email">
            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item>
            <el-button
              class="btn-gradient"
              :icon="Check"
              :loading="profileSaving"
              @click="updateProfile"
            >
              保存资料
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- ===== 修改密码卡片 ===== -->
    <div class="info-card password-card">
      <div class="card-bg"></div>
      <div class="card-body">
        <div class="section-label">
          <el-icon><Lock /></el-icon>
          <span>修改密码</span>
        </div>
        <p class="section-desc">建议定期更换密码以保护账号安全</p>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="88px"
          size="large"
          class="info-form"
        >
          <el-form-item label="原密码" prop="old_pwd">
            <el-input
              v-model="passwordForm.old_pwd"
              type="password"
              placeholder="请输入原密码"
              show-password
            />
          </el-form-item>

          <el-form-item label="新密码" prop="new_pwd">
            <el-input
              v-model="passwordForm.new_pwd"
              type="password"
              placeholder="请输入新密码"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="re_pwd">
            <el-input
              v-model="passwordForm.re_pwd"
              type="password"
              placeholder="请再次输入新密码"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <div class="btn-row">
              <el-button
                class="btn-gradient"
                :icon="Check"
                :loading="passwordSaving"
                @click="updatePassword"
              >
                修改密码
              </el-button>
              <el-button class="btn-outline" @click="resetPasswordForm">清空</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
// ==================== 设计令牌（与 Me.vue 统一） ====================
$purple: #c5a3ff;
$pink: #f8b4d9;
$mint: #a8e6cf;
$dark: #2d2d44;
$text-secondary: #999;
$border-light: #f0e5ff;
$border-input: #e0d4ff;
$bg-card: #fff;
$gradient-primary: linear-gradient(135deg, $purple, $pink);
$gradient-bg: linear-gradient(135deg, $purple 0%, $pink 50%, $mint 100%);

// ==================== 容器 ====================
.edit-container {
  max-width: 680px;
  margin: 0 auto;
  padding: 20px 16px 40px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

// ==================== 返回按钮 ====================
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  padding: 8px 18px;
  border: 1px solid $border-input;
  border-radius: 24px;
  background: #fff;
  color: #7a5a9e;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;

  .el-icon { font-size: 16px; }

  &:hover {
    color: $purple;
    border-color: $purple;
    background: rgba(197, 163, 255, 0.05);
    transform: translateX(-2px);
  }
}

// ==================== 通用卡片 ====================
.info-card {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(197, 163, 255, 0.15);
  background: $bg-card;

  .card-bg {
    height: 80px;
    background: $gradient-bg;
    opacity: 0.75;
  }

  .card-body {
    padding: 0 28px 28px;
    position: relative;

    .section-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 17px;
      font-weight: 700;
      color: $dark;

      .el-icon {
        color: $purple;
        font-size: 18px;
      }
    }

    .section-desc {
      margin: 6px 0 20px;
      font-size: 13px;
      color: $text-secondary;
    }
  }

  .card-header-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }
}

// ==================== 头像卡片 ====================
.avatar-card {
  .avatar-area {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
  }
}

.avatar-uploader {
  display: block;
  width: 180px;
  height: 180px;

  :deep(.el-upload) {
    position: relative;
    width: 180px;
    height: 180px;
    border-radius: 50%;
    overflow: hidden;
    cursor: pointer;
    box-shadow: 0 12px 28px rgba(63, 53, 86, 0.14);
  }
}

.avatar-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #fff;
  background: rgba(45, 45, 68, 0.48);
  opacity: 0;
  transition: opacity 0.25s ease;

  .el-icon {
    font-size: 26px;
  }
  span {
    font-size: 13px;
    font-weight: 600;
  }
}

.avatar-uploader:hover .avatar-mask {
  opacity: 1;
}

.avatar-btns {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

// ==================== 表单 ====================
.info-form {
  max-width: 520px;

  :deep(.el-form-item__label) {
    color: #615974;
    font-weight: 500;
  }

  :deep(.el-input__wrapper) {
    border-radius: 22px;
    padding: 6px 16px;
    box-shadow: none;
    border: 1px solid $border-input;
    background-color: #fbfaff;
    transition: all 0.2s ease;

    &:hover {
      border-color: $purple;
      background-color: #fff;
    }

    &.is-focus {
      border-color: $purple;
      background-color: #fff;
      box-shadow: 0 0 0 4px rgba(197, 163, 255, 0.16);
    }
  }

  .input-disabled {
    :deep(.el-input__wrapper) {
      background-color: #f5f0ff;
    }
    :deep(.el-input__inner) {
      color: #8d879b;
    }
  }
}

// ==================== 按钮 ====================
.btn-gradient {
  min-width: 118px;
  border: none;
  border-radius: 24px;
  padding: 10px 24px;
  color: #fff;
  font-weight: 500;
  background: $gradient-primary;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(197, 163, 255, 0.35);
    color: #fff;
  }
}

.btn-outline {
  border-radius: 24px;
  padding: 10px 24px;
  border: 1px solid $border-input;
  color: #7a5a9e;
  background: #fff;
  font-weight: 500;
  transition: all 0.3s ease;

  &:hover {
    color: $purple;
    border-color: $purple;
    background: rgba(197, 163, 255, 0.04);
  }

  &.btn-sm {
    padding: 6px 16px;
    font-size: 13px;
  }
}

.btn-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

// ==================== 响应式 ====================
@media (max-width: 600px) {
  .edit-container {
    padding: 12px 10px 30px;
  }

  .info-card .card-body {
    padding: 0 18px 22px;
  }

  .info-form {
    :deep(.el-form-item) {
      display: block;
    }
    :deep(.el-form-item__label) {
      display: block;
      width: 100% !important;
      margin-bottom: 8px;
      text-align: left;
    }
  }

  .avatar-uploader,
  .avatar-uploader :deep(.el-upload) {
    width: 150px;
    height: 150px;
  }
}
</style>

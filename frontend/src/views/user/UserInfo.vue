<!-- UserInfo.vue -->
<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Lock, Message, Plus, Refresh, Upload, User } from '@element-plus/icons-vue'
import avatar from '@/assets/default.png'
import { useTokenStore } from '@/stores/token'
import useUserInfoStore from '@/stores/userInfo.js'
import {
  userInfoService,
  userUpdateAvatarService,
  userUpdatePwdService,
  userUpdateService,
} from '@/api/user.js'

const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()

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
  <div class="userinfo-container">
    <div class="userinfo-card">
      <div class="userinfo-header">
        <div>
          <h1 class="userinfo-title">编辑资料</h1>
          <p class="userinfo-subtitle">头像、昵称、邮箱和密码都可以在这里修改</p>
        </div>
        <el-button :icon="Refresh" class="refresh-btn" @click="refreshUserInfo">刷新</el-button>
      </div>

      <div class="userinfo-content">
        <section class="avatar-section">
          <div class="section-title">
            <el-icon><User /></el-icon>
            <span>头像</span>
          </div>

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

          <div class="avatar-actions">
            <el-button :icon="Plus" class="secondary-btn" @click="chooseAvatar">选择图片</el-button>
            <el-button
              type="primary"
              :icon="Upload"
              class="primary-btn"
              :loading="avatarSaving"
              @click="updateAvatar"
            >
              保存头像
            </el-button>
          </div>
        </section>

        <section class="profile-section">
          <div class="section-title">
            <el-icon><Message /></el-icon>
            <span>基本资料</span>
          </div>

          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-width="88px"
            size="large"
            class="edit-form"
          >
            <el-form-item label="登录名称">
              <el-input v-model="profileForm.username" disabled class="disabled-input" />
            </el-form-item>

            <el-form-item label="用户昵称" prop="nickname">
              <el-input
                v-model="profileForm.nickname"
                placeholder="请输入昵称"
                class="custom-input"
                maxlength="10"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="用户邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" class="custom-input" />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :icon="Check"
                class="primary-btn"
                :loading="profileSaving"
                @click="updateProfile"
              >
                保存资料
              </el-button>
            </el-form-item>
          </el-form>
        </section>

        <section class="password-section">
          <div class="section-title">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </div>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="88px"
            size="large"
            class="edit-form"
          >
            <el-form-item label="原密码" prop="old_pwd">
              <el-input
                v-model="passwordForm.old_pwd"
                type="password"
                placeholder="请输入原密码"
                show-password
                class="custom-input"
              />
            </el-form-item>

            <el-form-item label="新密码" prop="new_pwd">
              <el-input
                v-model="passwordForm.new_pwd"
                type="password"
                placeholder="请输入新密码"
                show-password
                class="custom-input"
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="re_pwd">
              <el-input
                v-model="passwordForm.re_pwd"
                type="password"
                placeholder="请再次输入新密码"
                show-password
                class="custom-input"
              />
            </el-form-item>

            <el-form-item>
              <div class="button-group">
                <el-button
                  type="primary"
                  :icon="Check"
                  class="primary-btn"
                  :loading="passwordSaving"
                  @click="updatePassword"
                >
                  修改密码
                </el-button>
                <el-button class="secondary-btn" @click="resetPasswordForm">清空</el-button>
              </div>
            </el-form-item>
          </el-form>
        </section>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.userinfo-container {
  min-height: calc(100vh - 60px);
  padding: 20px;
  background: linear-gradient(145deg, #f7f4ff 0%, #eef7f4 48%, #fff3f8 100%);
}

.userinfo-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 24px;
  box-shadow: 0 10px 30px rgba(63, 53, 86, 0.08);
  overflow: hidden;
}

.userinfo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 30px 40px;
  background: linear-gradient(135deg, rgba(197, 163, 255, 0.18), rgba(168, 230, 207, 0.22));
  border-bottom: 1px solid rgba(197, 163, 255, 0.18);
}

.userinfo-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  color: #2d2d44;
}

.userinfo-subtitle {
  margin: 0;
  font-size: 13px;
  color: #7d7891;
}

.refresh-btn,
.secondary-btn {
  border-radius: 20px;
  border-color: #e3d8f8;
  color: #6a4a9c;
  background: #fff;
}

.userinfo-content {
  display: grid;
  grid-template-columns: minmax(240px, 300px) 1fr;
  gap: 32px;
  padding: 36px 40px 42px;
}

.avatar-section,
.profile-section,
.password-section {
  min-width: 0;
}

.avatar-section {
  grid-row: span 2;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  font-size: 16px;
  font-weight: 700;
  color: #3b3650;

  .el-icon {
    color: #8d64d6;
  }
}

.avatar-uploader {
  display: block;
  width: 220px;
  height: 220px;
  margin: 0 auto;

  :deep(.el-upload) {
    position: relative;
    width: 220px;
    height: 220px;
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
  gap: 8px;
  color: #fff;
  background: rgba(45, 45, 68, 0.48);
  opacity: 0;
  transition: opacity 0.2s ease;

  .el-icon {
    font-size: 28px;
  }

  span {
    font-size: 14px;
    font-weight: 600;
  }
}

.avatar-uploader:hover .avatar-mask {
  opacity: 1;
}

.avatar-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.edit-form {
  max-width: 620px;

  :deep(.el-form-item__label) {
    color: #615974;
    font-weight: 500;
  }
}

.custom-input,
.disabled-input {
  :deep(.el-input__wrapper) {
    border-radius: 22px;
    padding: 6px 16px;
    box-shadow: none;
    border: 1px solid #ede6f8;
    background-color: #fbfaff;
    transition: all 0.2s ease;

    &:hover {
      border-color: #d7c4f3;
      background-color: #fff;
    }

    &.is-focus {
      border-color: #a987e2;
      background-color: #fff;
      box-shadow: 0 0 0 4px rgba(169, 135, 226, 0.14);
    }
  }
}

.disabled-input {
  :deep(.el-input__wrapper) {
    background-color: #f3effb;
  }

  :deep(.el-input__inner) {
    color: #8d879b;
  }
}

.primary-btn {
  min-width: 118px;
  border: none;
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(135deg, #8d64d6, #d97cad);
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 18px rgba(141, 100, 214, 0.28);
  }
}

.button-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.password-section {
  padding-top: 26px;
  border-top: 1px solid #f0e9fb;
}

@media (max-width: 900px) {
  .userinfo-header {
    align-items: flex-start;
    flex-direction: column;
    padding: 26px 24px;
  }

  .userinfo-content {
    grid-template-columns: 1fr;
    padding: 28px 24px 34px;
  }

  .avatar-section {
    grid-row: auto;
  }
}

@media (max-width: 520px) {
  .userinfo-container {
    padding: 12px;
  }

  .userinfo-title {
    font-size: 22px;
  }

  .avatar-uploader,
  .avatar-uploader :deep(.el-upload) {
    width: 180px;
    height: 180px;
  }

  .edit-form {
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
}
</style>

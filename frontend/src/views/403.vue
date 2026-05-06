<script setup>
import { useRouter } from 'vue-router'
import { useTokenStore } from '@/stores/token'
import useUserInfoStore from '@/stores/userInfo'
import { canEnterAdmin } from '@/utils/roles.js'
const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()
const goBack = () => {
  const role = userInfoStore.info?.role
  if (canEnterAdmin(role)) {
    router.push('/admin/home')
  } else {
    router.push('/home')
  }
}
const reLogin = () => {
  tokenStore.removeToken()
  userInfoStore.removeInfo()
  router.push('/login')
}
</script>
<template>
  <div class="err-page">
    <div class="err-card">
      <h1>403</h1>
      <p>当前账号无权访问该页面</p>
      <div class="actions">
        <el-button type="primary" round class="btn-primary" @click="goBack">返回工作台</el-button>
        <el-button round @click="reLogin">重新登录</el-button>
      </div>
    </div>
  </div>
</template>
<style lang="scss" scoped>
.err-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
  padding: 24px;
}
.err-card {
  text-align: center;
  padding: 48px 56px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0e5ff;
  h1 {
    font-size: 72px;
    margin: 0 0 12px;
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }
  p {
    color: #888;
    margin-bottom: 28px;
  }
  .actions {
    display: flex;
    gap: 12px;
    justify-content: center;
    flex-wrap: wrap;
  }
}
.btn-primary {
  background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
  border: none;
}
</style>



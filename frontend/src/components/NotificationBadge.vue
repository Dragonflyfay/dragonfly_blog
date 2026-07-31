<!-- components/NotificationBadge.vue -->
<template>
  <div class="notification-badge" @click="goToNotification">
    <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
      <el-icon :size="24"><Bell /></el-icon>
    </el-badge>
    <span class="badge-label">通知</span>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { getUnreadCountService } from '@/api/notification.js'

const router = useRouter()
const unreadCount = ref(0)
let pollTimer = null

const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCountService()
    unreadCount.value = res.data
  } catch (error) {
    console.warn('获取未读数量失败:', error)
  }
}

const goToNotification = () => {
  router.push('/notification')
}

onMounted(() => {
  fetchUnreadCount()
  // 每30秒轮询一次
  pollTimer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style lang="scss" scoped>
.notification-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(197, 163, 255, 0.1);
  }

  .badge-label {
    font-size: 14px;
    color: #6a4a9c;
  }

  :deep(.el-badge) {
    .el-badge__content {
      background: linear-gradient(135deg, #ff6b6b, #ff8e8e);
      border: none;
      font-weight: 600;
    }
  }
}
</style>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete } from '@element-plus/icons-vue'
import {
  getNotificationListService,
  getUnreadCountService,
  markAsReadService,
  markAllAsReadService,
  deleteNotificationService,
  deleteAllReadService,
} from '@/api/notification.js'

const router = useRouter() //路由
const loading = ref(false)
const notifications = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const unreadCount = ref(0)

// 计算未读和已读列表
const unreadList = computed(() => notifications.value.filter((n) => n.isRead === 0))
const readList = computed(() => notifications.value.filter((n) => n.isRead === 1))
const hasMore = computed(() => notifications.value.length < total.value)
const hasReadNotifications = computed(() => readList.value.length > 0)

// 获取通知类型文本
const getActionText = (type) => {
  const typeMap = {
    1: '👍 点赞了你的笔记',
    2: '👍 点赞了你的评论',
    3: '💬 评论了你的笔记',
    4: '👤 关注了你',
    5: '📢 系统通知',
  }
  return typeMap[type] || '📢 系统通知'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const now = new Date()
  const date = new Date(time)
  const diff = Math.floor((now - date) / 1000)

  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + '天前'

  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}月${day}日`
}

// 加载通知
const loadNotifications = async (reset = true) => {
  if (loading.value) return
  loading.value = true

  try {
    if (reset) {
      pageNum.value = 1
    }
    const res = await getNotificationListService(pageNum.value, pageSize.value)
    const data = res.data
    total.value = data.total

    if (reset) {
      notifications.value = data.items
    } else {
      notifications.value = [...notifications.value, ...data.items]
    }
  } catch (error) {
    if (error?.__handled) return
    console.error('加载通知失败:', error)
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  if (hasMore.value && !loading.value) {
    pageNum.value++
    loadNotifications(false)
  }
}

// 获取未读数量
const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCountService()
    unreadCount.value = res.data
  } catch (error) {
    console.error('获取未读数量失败:', error)
  }
}

// 标记为已读
const markAsRead = async (id) => {
  try {
    await markAsReadService(id)
    const item = notifications.value.find((n) => n.id === id)
    if (item) {
      item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  } catch (error) {
    console.error('标记已读失败:', error)
    ElMessage.error('操作失败')
  }
}

// 全部标记为已读
const markAllRead = async () => {
  try {
    await ElMessageBox.confirm('确定要将所有通知标记为已读吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    await markAllAsReadService()
    notifications.value.forEach((n) => (n.isRead = 1))
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('全部标记已读失败:', error)
    }
  }
}

// 删除通知
const deleteNotification = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条通知吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteNotificationService(id)
    const index = notifications.value.findIndex((n) => n.id === id)
    if (index > -1) {
      const item = notifications.value[index]
      if (item.isRead === 0) {
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      notifications.value.splice(index, 1)
    }
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除通知失败:', error)
    }
  }
}

// 删除所有已读通知
const deleteAllRead = async () => {
  try {
    await ElMessageBox.confirm('确定要删除所有已读通知吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteAllReadService()
    notifications.value = notifications.value.filter((n) => n.isRead === 0)
    ElMessage.success('已清空已读通知')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空已读通知失败:', error)
    }
  }
}

// 点击通知跳转
const handleNotificationClick = (notification) => {
  // 自动标记为已读
  if (notification.isRead === 0) {
    markAsRead(notification.id)
  }

  // 根据通知类型跳转
  if (notification.targetType === 1 && notification.targetId) {
    // 跳转到笔记详情
    router.push({ path: '/home', query: { noteId: notification.targetId } })
  } else if (notification.type === 4) {
    // 关注通知，跳转到用户主页
    router.push(`/user/${notification.fromUserId}`)
  }
}

// 轮询获取未读数量（每30秒）
let pollTimer = null

onMounted(() => {
  loadNotifications()
  fetchUnreadCount()

  // 启动轮询
  pollTimer = setInterval(() => {
    fetchUnreadCount()
  }, 30000)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>
<template>
  <div class="notification-container">
    <!-- 顶部标题栏 -->
    <div class="notification-header">
      <div class="header-left">
        <h1 class="page-title">🔔 通知</h1>
        <span class="unread-badge" v-if="unreadCount > 0"> {{ unreadCount }} 条未读 </span>
      </div>
      <div class="header-actions">
        <el-button v-if="unreadCount > 0" type="primary" size="small" round @click="markAllRead">
          <el-icon><Check /></el-icon>
          全部已读
        </el-button>
        <el-button
          v-if="hasReadNotifications"
          type="danger"
          size="small"
          round
          plain
          @click="deleteAllRead"
        >
          <el-icon><Delete /></el-icon>
          清空已读
        </el-button>
      </div>
    </div>

    <!-- 通知列表 -->
    <div class="notification-list" v-loading="loading">
      <!-- 未读通知分组 -->
      <div v-if="unreadList.length > 0" class="notification-group">
        <div class="group-header">
          <span class="group-title">📌 未读</span>
          <span class="group-count">{{ unreadList.length }}</span>
        </div>
        <div
          v-for="item in unreadList"
          :key="item.id"
          class="notification-item unread"
          @click="handleNotificationClick(item)"
        >
          <div class="item-avatar">
            <img v-if="item.fromUserPic" :src="item.fromUserPic" class="avatar-img" />
            <div v-else class="avatar-placeholder">
              {{ (item.fromUserName || '用户').charAt(0).toUpperCase() }}
            </div>
          </div>
          <div class="item-content">
            <div class="item-header">
              <span class="user-name">{{ item.fromUserName || '用户' }}</span>
              <span class="action-type">{{ getActionText(item.type) }}</span>
              <span class="item-time">{{ formatTime(item.createTime) }}</span>
            </div>
            <div class="item-body">
              <span class="content-text">{{ item.content }}</span>
            </div>
            <div v-if="item.targetTitle" class="item-target">
              <span class="target-text">📄 {{ item.targetTitle }}</span>
            </div>
          </div>
          <div class="item-actions">
            <el-button type="primary" link size="small" @click.stop="markAsRead(item.id)">
              标记已读
            </el-button>
            <el-button type="danger" link size="small" @click.stop="deleteNotification(item.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 已读通知分组 -->
      <div v-if="readList.length > 0" class="notification-group">
        <div class="group-header">
          <span class="group-title">📖 已读</span>
          <span class="group-count">{{ readList.length }}</span>
        </div>
        <div
          v-for="item in readList"
          :key="item.id"
          class="notification-item read"
          @click="handleNotificationClick(item)"
        >
          <div class="item-avatar">
            <img v-if="item.fromUserPic" :src="item.fromUserPic" class="avatar-img" />
            <div v-else class="avatar-placeholder">
              {{ (item.fromUserName || '用户').charAt(0).toUpperCase() }}
            </div>
          </div>
          <div class="item-content">
            <div class="item-header">
              <span class="user-name">{{ item.fromUserName || '用户' }}</span>
              <span class="action-type">{{ getActionText(item.type) }}</span>
              <span class="item-time">{{ formatTime(item.createTime) }}</span>
            </div>
            <div class="item-body">
              <span class="content-text">{{ item.content }}</span>
            </div>
            <div v-if="item.targetTitle" class="item-target">
              <span class="target-text">📄 {{ item.targetTitle }}</span>
            </div>
          </div>
          <div class="item-actions">
            <el-button type="danger" link size="small" @click.stop="deleteNotification(item.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && notifications.length === 0" class="empty-state">
        <div class="empty-icon">🔔</div>
        <p class="empty-title">暂无通知</p>
        <p class="empty-desc">当有人与你互动时，通知会出现在这里</p>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore && !loading" class="load-more">
        <el-button type="primary" link @click="loadMore">加载更多</el-button>
      </div>
    </div>
  </div>
</template>
<style lang="scss" scoped>
.notification-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, #f5f0ff, #fef9ff);
  border-radius: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(197, 163, 255, 0.12);

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .page-title {
      font-size: 24px;
      font-weight: 700;
      margin: 0;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }

    .unread-badge {
      padding: 4px 14px;
      background: linear-gradient(135deg, #ff6b6b, #ff8e8e);
      color: white;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 600;
    }
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.notification-list {
  .notification-group {
    margin-bottom: 24px;

    .group-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 4px 12px;
      border-bottom: 2px solid rgba(197, 163, 255, 0.15);

      .group-title {
        font-size: 16px;
        font-weight: 600;
        color: #6a4a9c;
      }

      .group-count {
        font-size: 12px;
        padding: 2px 10px;
        background: rgba(197, 163, 255, 0.15);
        border-radius: 12px;
        color: #c5a3ff;
      }
    }
  }
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  margin-top: 8px;
  border-radius: 16px;
  transition: all 0.3s ease;
  cursor: pointer;

  &:hover {
    background: rgba(197, 163, 255, 0.06);
  }

  &.unread {
    background: linear-gradient(135deg, rgba(197, 163, 255, 0.08), rgba(248, 180, 217, 0.08));
    border-left: 4px solid #c5a3ff;

    &:hover {
      background: linear-gradient(135deg, rgba(197, 163, 255, 0.14), rgba(248, 180, 217, 0.14));
    }
  }

  &.read {
    opacity: 0.8;
  }

  .item-avatar {
    flex-shrink: 0;

    .avatar-img {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      object-fit: cover;
    }

    .avatar-placeholder {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      font-weight: 600;
    }
  }

  .item-content {
    flex: 1;
    min-width: 0;

    .item-header {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .user-name {
        font-weight: 600;
        color: #2d2d44;
        font-size: 14px;
      }

      .action-type {
        font-size: 13px;
        color: #8a6fa8;
      }

      .item-time {
        font-size: 12px;
        color: #b0a7c0;
        margin-left: auto;
      }
    }

    .item-body {
      margin-top: 4px;

      .content-text {
        font-size: 14px;
        color: #4a4a6a;
        line-height: 1.6;
      }
    }

    .item-target {
      margin-top: 8px;
      padding: 8px 14px;
      background: rgba(197, 163, 255, 0.08);
      border-radius: 8px;

      .target-text {
        font-size: 13px;
        color: #8a6fa8;
        display: -webkit-box;
        -webkit-line-clamp: 1;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
    }
  }

  .item-actions {
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  &:hover .item-actions {
    opacity: 1;
  }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;

  .empty-icon {
    font-size: 64px;
    margin-bottom: 16px;
    opacity: 0.5;
  }

  .empty-title {
    font-size: 18px;
    color: #6a4a9c;
    margin: 0 0 8px;
    font-weight: 500;
  }

  .empty-desc {
    font-size: 14px;
    color: #b0a7c0;
    margin: 0;
  }
}

.load-more {
  text-align: center;
  padding: 20px 0;

  .el-button {
    color: #c5a3ff;
    font-size: 14px;
  }
}

@media (max-width: 768px) {
  .notification-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;

    .header-left {
      justify-content: center;
    }

    .header-actions {
      justify-content: center;
    }
  }

  .notification-item {
    flex-direction: column;
    align-items: stretch;

    .item-actions {
      flex-direction: row;
      opacity: 1;
      margin-top: 8px;
      justify-content: flex-end;
    }
  }
}
</style>

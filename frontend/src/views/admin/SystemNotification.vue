<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit, Promotion } from '@element-plus/icons-vue'
import request from '@/utils/request.js'

const notifications = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = ref({
  id: null,
  title: '',
  content: '',
  targetType: 'all', // all | user
  targetUserId: null,
  priority: 'normal', // normal | important | urgent
})

const rules = {
  title: [{ required: true, message: '请输入标题' }],
  content: [{ required: true, message: '请输入内容' }],
}

// 获取通知列表
const getList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/notifications')
    notifications.value = res.data || []
  } catch (error) {
    console.error('获取通知列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 发送通知
const sendNotification = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      if (isEdit.value) {
        await request.put('/admin/notifications', form.value)
        ElMessage.success('通知已更新')
      } else {
        await request.post('/admin/notifications', form.value)
        ElMessage.success('通知已发送')
      }
      dialogVisible.value = false
      getList()
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '操作失败')
    }
  })
}

// 删除通知
const deleteNotification = (row) => {
  ElMessageBox.confirm('确定删除此通知吗？', '提示', { type: 'warning' })
    .then(async () => {
      await request.delete(`/admin/notifications/${row.id}`)
      ElMessage.success('已删除')
      getList()
    })
    .catch(() => {})
}

// 标记通知状态
const toggleStatus = async (row) => {
  await request.put(`/admin/notifications/${row.id}/status`, {
    status: row.status === 'draft' ? 'published' : 'draft',
  })
  ElMessage.success('状态已更新')
  getList()
}

onMounted(getList)
</script>

<template>
  <div class="notification-manage">
    <div class="page-header">
      <div class="header-title-section">
        <h1 class="page-title">📢 系统通知</h1>
        <p class="page-subtitle">向用户发送系统公告和重要消息</p>
      </div>
      <el-button
        type="primary"
        :icon="Plus"
        @click="
          dialogVisible = true
          isEdit = false
          form = { targetType: 'all', priority: 'normal' }
        "
      >
        发送通知
      </el-button>
    </div>

    <div class="notification-list" v-loading="loading">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="item.priority"
      >
        <div class="noti-left">
          <span class="priority-badge" :class="item.priority">
            {{ item.priority === 'urgent' ? '🔴' : item.priority === 'important' ? '🟡' : '🔵' }}
          </span>
          <div class="noti-content">
            <h4>{{ item.title }}</h4>
            <p>{{ item.content }}</p>
            <div class="noti-meta">
              <span
                >📌
                {{
                  item.targetType === 'all' ? '全部用户' : `指定用户 ID:${item.targetUserId}`
                }}</span
              >
              <span>🕐 {{ item.createTime }}</span>
              <el-tag :type="item.status === 'published' ? 'success' : 'info'" size="small">
                {{ item.status === 'published' ? '已发布' : '草稿' }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="noti-actions">
          <el-button size="small" @click="toggleStatus(item)">
            {{ item.status === 'draft' ? '发布' : '撤回' }}
          </el-button>
          <el-button size="small" type="danger" @click="deleteNotification(item)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notification-manage {
  padding: 20px;
}
.notification-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 12px;
  border-left: 4px solid #c5a3ff;
}
.notification-item.urgent {
  border-left-color: #ff4757;
}
.notification-item.important {
  border-left-color: #ffa502;
}
.noti-left {
  display: flex;
  gap: 12px;
  flex: 1;
}
.noti-content h4 {
  margin: 0 0 6px;
  font-size: 15px;
}
.noti-content p {
  margin: 0 0 8px;
  color: #666;
  font-size: 13px;
}
.noti-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}
.noti-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.priority-badge {
  font-size: 18px;
}
</style>

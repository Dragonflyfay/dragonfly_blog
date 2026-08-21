<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit } from '@element-plus/icons-vue'
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
  priority: 'normal', // normal | important | urgent
  targetType: 'all', // all | user
  targetUserId: null,
  status: 'published', // draft | published
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  targetUserId: [
    {
      validator: (rule, value, callback) => {
        if (form.value.targetType === 'user' && !value) {
          callback(new Error('请输入目标用户ID'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
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

// 打开新建
const openCreate = () => {
  isEdit.value = false
  form.value = {
    id: null,
    title: '',
    content: '',
    priority: 'normal',
    targetType: 'all',
    targetUserId: null,
    status: 'published',
  }
  dialogVisible.value = true
}

// 打开编辑
const openEdit = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    title: row.title,
    content: row.content,
    priority: row.priority || 'normal',
    targetType: row.userId === 0 ? 'all' : 'user',
    targetUserId: row.userId === 0 ? null : row.userId,
    status: row.status || 'published',
  }
  dialogVisible.value = true
}

// 提交（发送/更新）
const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    const payload = {
      id: form.value.id,
      title: form.value.title,
      content: form.value.content,
      priority: form.value.priority,
      status: form.value.status,
      // 全部用户 → 0；指定用户 → 具体用户ID
      userId: form.value.targetType === 'user' ? form.value.targetUserId : 0,
    }

    try {
      if (isEdit.value) {
        await request.put('/admin/notifications', payload)
        ElMessage.success('通知已更新')
      } else {
        await request.post('/admin/notifications', payload)
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

// 切换通知状态（发布/撤回）
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
      <el-button type="primary" :icon="Plus" @click="openCreate">发送通知</el-button>
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
            <h4>{{ item.title || item.content }}</h4>
            <p>{{ item.content }}</p>
            <div class="noti-meta">
              <span>📌 {{ item.userId === 0 ? '全部用户' : `指定用户 ID:${item.userId}` }}</span>
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
          <el-button size="small" :icon="Edit" @click="openEdit(item)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteNotification(item)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 发送/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑通知' : '发送通知'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入通知标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="请输入通知内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority">
            <el-radio value="normal">普通</el-radio>
            <el-radio value="important">重要</el-radio>
            <el-radio value="urgent">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发送对象">
          <el-radio-group v-model="form.targetType">
            <el-radio value="all">全部用户</el-radio>
            <el-radio value="user">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.targetType === 'user'" label="用户ID" prop="targetUserId">
          <el-input v-model.number="form.targetUserId" placeholder="请输入目标用户ID" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="published">发布</el-radio>
            <el-radio value="draft">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.notification-manage {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #2d2d44;
}
.page-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #999;
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
  align-items: center;
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

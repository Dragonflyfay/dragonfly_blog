<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete, Edit } from '@element-plus/icons-vue'
import { userListService, userUpdateRoleService } from '@/api/user.js'
import { normalizeRole } from '@/utils/roles.js'

import useUserInfoStore from '@/stores/userInfo.js'
const users = ref([])
const loading = ref(false)
const searchNickname = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedUsers = ref([])
// 图片预览相关
const showImageViewer = ref(false)
const previewImageUrl = ref('')

// 获取当前登录用户信息
const userInfoStore = useUserInfoStore()
const currentUserRole = normalizeRole(userInfoStore.role)
// 获取管理员用户列表
const getUserList = async () => {
  loading.value = true
  try {
    const result = await userListService()
    const allUsers = result.data || []

    // 过滤管理员用户（包括 admin 和 super_admin），使用 normalizeRole 函数进行大小写不敏感的比较
    const adminUsers = allUsers.filter((user) => {
      const normalizedRole = normalizeRole(user.role)
      return normalizedRole === 'admin' || normalizedRole === 'super_admin'
    })

    // 搜索过滤
    let filteredUsers = adminUsers
    if (searchNickname.value) {
      filteredUsers = adminUsers.filter(
        (user) =>
          user.nickname && user.nickname.toLowerCase().includes(searchNickname.value.toLowerCase()),
      )
    }

    // 分页处理
    total.value = filteredUsers.length
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    users.value = filteredUsers.slice(start, end)
  } catch (error) {
    console.error('获取管理员用户列表失败:', error)
    ElMessage.error('获取管理员用户列表失败')
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  getUserList()
}

// 重置
const handleReset = () => {
  searchNickname.value = ''
  currentPage.value = 1
  getUserList()
}

// 分页变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  getUserList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  getUserList()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedUsers.value = selection
}

// 更新用户角色
const updateUserRole = async (user, newRole) => {
  try {
    await userUpdateRoleService({
      userId: user.id,
      role: newRole,
    })
    ElMessage.success('角色更新成功')
    await getUserList()
  } catch (error) {
    console.error('更新角色失败:', error)
    ElMessage.error(error.response?.data?.message || '更新角色失败')
  }
}

// 降级为普通用户
const demoteToAdmin = (user) => {
  ElMessageBox.confirm(`确定要将超级管理员 "${user.nickname}" 降级为普通管理员吗？`, '确认降级', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
    .then(() => {
      updateUserRole(user, 'admin')
    })
    .catch(() => {})
}

// 批量降级
const batchDemote = () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请先选择用户')
    return
  }

  ElMessageBox.confirm(
    `确定要将选中的 ${selectedUsers.value.length} 个管理员降级为普通用户吗？`,
    '批量降级',
    {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    },
  )
    .then(async () => {
      for (const user of selectedUsers.value) {
        await updateUserRole(user, 'user')
      }
      selectedUsers.value = []
    })
    .catch(() => {})
}
// 打开图片预览
const openImageViewer = (imageUrl) => {
  if (imageUrl) {
    previewImageUrl.value = imageUrl
    showImageViewer.value = true
  }
}
// 关闭图片预览
const closeImageViewer = () => {
  showImageViewer.value = false
  previewImageUrl.value = ''
}

// 在组件挂载时初始化数据
onMounted(() => {
  getUserList()
})
</script>

<template>
  <div class="admin-role-manage-container">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchNickname"
        placeholder="请输入昵称"
        prefix-icon="Search"
        clearable
        style="width: 200px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
    </div>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" :icon="Plus">新增</el-button>
      <el-button type="danger" :icon="Delete" @click="batchDemote">批量降级</el-button>
    </div>

    <!-- 管理员用户列表 -->
    <div class="table-card" v-loading="loading">
      <el-table :data="users" style="width: 100%" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="电话" width="150">
          <template #default="scope">
            {{ scope.row.phone || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="120">
          <template #default="scope">
            <el-tag :type="normalizeRole(scope.row.role) === 'super_admin' ? 'warning' : 'danger'">
              {{ normalizeRole(scope.row.role) === 'super_admin' ? '超级管理员' : '管理员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="头像" width="100">
          <template #default="scope">
            <el-avatar
              :size="50"
              :src="scope.row.userPic"
              @click="openImageViewer(scope.row.userPic)"
              style="cursor: pointer"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <!-- 超级管理员可以看到：将其他超级管理员降级为普通管理员 -->
            <el-button
              v-if="
                currentUserRole === 'super_admin' &&
                normalizeRole(scope.row.role) === 'super_admin' &&
                scope.row.id !== userInfoStore.info.id
              "
              type="warning"
              size="small"
              :icon="Edit"
              @click="demoteToAdmin(scope.row)"
            >
              降级为管理员
            </el-button>

            <!-- 所有管理员都可以将其他管理员降级为普通用户 -->
            <el-button
              v-if="
                normalizeRole(scope.row.role) !== 'user' && scope.row.id !== userInfoStore.info.id
              "
              type="info"
              size="small"
              :icon="Edit"
              @click="demoteToUser(scope.row)"
            >
              降级为用户
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
    <!-- 图片预览组件 -->
    <el-image-viewer
      v-if="showImageViewer"
      :url-list="[previewImageUrl]"
      @close="closeImageViewer"
    />
  </div>
</template>

<style lang="scss" scoped>
.admin-role-manage-container {
  padding: 20px;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
  min-height: calc(100vh - 60px);
}

.search-bar {
  background: rgba(255, 255, 255, 0.96);
  padding: 16px 20px;
  border-radius: 30px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(10px);

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

  :deep(.el-button) {
    border-radius: 48px;
    padding: 10px 20px;
    font-weight: 500;
    transition: all 0.3s ease;

    &.el-button--primary {
      background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
      border: none;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
      }
    }
  }
}

.action-bar {
  background: rgba(255, 255, 255, 0.96);
  padding: 16px 20px;
  border-radius: 30px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(10px);

  :deep(.el-button) {
    border-radius: 48px;
    padding: 10px 20px;
    font-weight: 500;
    transition: all 0.3s ease;

    &.el-button--primary {
      background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
      border: none;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
      }
    }

    &.el-button--danger {
      background: linear-gradient(135deg, #ff9e9e 0%, #ffbaba 100%);
      border: none;
      color: white;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(255, 158, 158, 0.4);
      }
    }
  }
}

.table-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 30px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(10px);

  :deep(.el-table) {
    border-radius: 20px;
    overflow: hidden;

    .el-table__header {
      th {
        background: linear-gradient(135deg, #f8f3ff 0%, #faf7ff 100%);
        color: #6a4a9c;
        font-weight: 600;
        font-size: 14px;
      }
    }

    .el-table__body {
      tr {
        transition: all 0.3s ease;

        &:hover {
          background: rgba(197, 163, 255, 0.08);
          transform: scale(1.005);
        }
      }

      td {
        padding: 14px 0;
      }
    }
  }

  :deep(.el-tag) {
    border-radius: 48px;
    padding: 4px 12px;
    font-weight: 500;
    border: none;

    &.el-tag--warning {
      background: linear-gradient(135deg, #ffa726, #ffb74d);
      color: white;
    }

    &.el-tag--danger {
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      color: white;
    }
  }

  :deep(.el-button) {
    border-radius: 48px;
    padding: 8px 16px;
    font-weight: 500;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
    }

    &.el-button--warning {
      background: linear-gradient(135deg, #ffa726, #ffb74d);
      border: none;
      color: white;

      &:hover {
        box-shadow: 0 4px 12px rgba(255, 167, 38, 0.3);
      }
    }

    &.el-button--info {
      background: linear-gradient(135deg, #a8e6cf, #7ee0b5);
      border: none;
      color: #2c665a;

      &:hover {
        box-shadow: 0 4px 12px rgba(168, 230, 207, 0.3);
      }
    }
  }
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;

  :deep(.el-pagination) {
    .btn-prev,
    .btn-next,
    .el-pager li {
      border-radius: 48px;
      transition: all 0.3s ease;

      &:hover {
        color: #c5a3ff;
      }

      &.is-active {
        background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
        color: white;
      }
    }
  }
}
</style>

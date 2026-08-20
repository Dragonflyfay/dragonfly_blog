<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, Search, Refresh, InfoFilled } from '@element-plus/icons-vue'
import { userListService, userUpdateRoleService } from '@/api/user.js'
import { normalizeRole } from '@/utils/roles.js'

import useUserInfoStore from '@/stores/userInfo.js'

// ==================== 获取当前用户信息 ====================
const userInfoStore = useUserInfoStore()
const currentUserRole = computed(() =>
  normalizeRole(userInfoStore.info?.role || userInfoStore.role),
)

// ==================== 权限判断 ====================
// 是否是超级管理员
const isSuperAdmin = computed(() => currentUserRole.value === 'super_admin')
// 是否是管理员（包含超级管理员和普通管理员）
const isAdmin = computed(
  () => currentUserRole.value === 'admin' || currentUserRole.value === 'super_admin',
)

// ==================== 数据 ====================
const users = ref([])
const loading = ref(false)
const searchNickname = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 图片预览相关
const showImageViewer = ref(false)
const previewImageUrl = ref('')

// ==================== 图片预览 ====================
const openImageViewer = (imageUrl) => {
  if (imageUrl) {
    previewImageUrl.value = imageUrl
    showImageViewer.value = true
  }
}

const closeImageViewer = () => {
  showImageViewer.value = false
  previewImageUrl.value = ''
}

// ==================== 获取用户列表 ====================
const getUserList = async () => {
  loading.value = true
  try {
    const result = await userListService()
    const allUsers = result.data || []

    // 过滤出普通用户（排除管理员和超级管理员）
    const normalUsers = allUsers.filter((user) => {
      const role = normalizeRole(user.role)
      return role === 'user'
    })

    // 搜索过滤
    let filteredUsers = normalUsers
    if (searchNickname.value) {
      filteredUsers = normalUsers.filter(
        (user) =>
          (user.nickname &&
            user.nickname.toLowerCase().includes(searchNickname.value.toLowerCase())) ||
          (user.username &&
            user.username.toLowerCase().includes(searchNickname.value.toLowerCase())) ||
          (user.email && user.email.toLowerCase().includes(searchNickname.value.toLowerCase())),
      )
    }

    // 分页处理
    total.value = filteredUsers.length
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    users.value = filteredUsers.slice(start, end)
  } catch (error) {
    if (error?.__handled) return
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// ==================== 搜索 ====================
const handleSearch = () => {
  currentPage.value = 1
  getUserList()
}

const handleReset = () => {
  searchNickname.value = ''
  currentPage.value = 1
  getUserList()
}

// ==================== 分页 ====================
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  getUserList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  getUserList()
}

// ==================== 更新用户角色（仅超级管理员） ====================
const updateUserRole = async (user, newRole) => {
  // 🔒 权限检查：只有超级管理员可以修改角色
  if (!isSuperAdmin.value) {
    ElMessage.warning('只有超级管理员可以修改用户角色')
    return
  }

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

// ==================== 确认更新角色 ====================
const confirmUpdateRole = (user, newRole) => {
  // 🔒 权限检查：只有超级管理员可以执行此操作
  if (!isSuperAdmin.value) {
    ElMessage.warning('只有超级管理员可以更改用户角色')
    return
  }

  const roleName =
    normalizeRole(newRole) === 'super_admin'
      ? '超级管理员'
      : normalizeRole(newRole) === 'admin'
        ? '管理员'
        : '普通用户'

  const confirmMsg =
    normalizeRole(newRole) === 'super_admin'
      ? `确定要将用户 "${user.nickname}" 设置为超级管理员吗？此操作授予最高权限！`
      : `确定要将用户 "${user.nickname}" 的角色更改为 ${roleName} 吗？`

  ElMessageBox.confirm(confirmMsg, '确认更改角色', {
    type: normalizeRole(newRole) === 'super_admin' ? 'warning' : 'info',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    distinguishCancelAndClose: true,
  })
    .then(() => {
      updateUserRole(user, newRole)
    })
    .catch(() => {})
}

// ==================== 初始化 ====================
onMounted(() => {
  getUserList()
})
</script>

<template>
  <div class="user-role-manage-container">
    <!-- ===== 页面头部 ===== -->
    <div class="page-header">
      <div class="header-title-section">
        <div class="title-decoration">
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
        </div>
        <h1 class="page-title">普通用户管理</h1>
        <p class="page-subtitle">管理普通用户账户和权限</p>
      </div>
      <!-- 显示当前角色 -->
      <div class="header-tag">
        <el-tag :type="isSuperAdmin ? 'warning' : 'info'" size="large">
          {{ isSuperAdmin ? '👑 超级管理员' : '🔒 普通管理员' }}
        </el-tag>
      </div>
    </div>

    <!-- ===== 权限提示 ===== -->
    <div v-if="!isSuperAdmin" class="permission-banner">
      <el-icon><InfoFilled /></el-icon>
      <span>你当前为普通管理员，仅有查看权限，无法修改用户角色</span>
    </div>

    <!-- ===== 搜索栏 ===== -->
    <div class="search-bar">
      <el-input
        v-model="searchNickname"
        placeholder="请输入用户名/昵称/邮箱"
        prefix-icon="Search"
        clearable
        style="width: 250px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
    </div>

    <!-- ===== 用户列表 ===== -->
    <div class="user-list-card" v-loading="loading">
      <el-table :data="users" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="头像" width="80">
          <template #default="scope">
            <el-avatar
              :size="40"
              :src="scope.row.userPic"
              @click="openImageViewer(scope.row.userPic)"
              style="cursor: pointer"
            />
          </template>
        </el-table-column>
        <el-table-column prop="role" label="当前角色" width="120">
          <template #default="scope">
            <el-tag
              :type="
                normalizeRole(scope.row.role) === 'super_admin'
                  ? 'warning'
                  : normalizeRole(scope.row.role) === 'admin'
                    ? 'danger'
                    : 'primary'
              "
            >
              {{
                normalizeRole(scope.row.role) === 'super_admin'
                  ? '超级管理员'
                  : normalizeRole(scope.row.role) === 'admin'
                    ? '管理员'
                    : '普通用户'
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="scope">
            {{ new Date(scope.row.createTime).toLocaleString('zh-CN') }}
          </template>
        </el-table-column>

        <!-- ===== 操作列 - 根据权限显示 ===== -->
        <el-table-column label="操作" width="280">
          <template #default="scope">
            <!-- ✅ 超级管理员：显示所有操作按钮 -->
            <template v-if="isSuperAdmin">
              <!-- 设为超级管理员（不能对自己操作） -->
              <el-button
                v-if="
                  normalizeRole(scope.row.role) !== 'super_admin' &&
                  scope.row.id !== userInfoStore.info.id
                "
                size="small"
                type="warning"
                @click="confirmUpdateRole(scope.row, 'super_admin')"
              >
                👑 设为超级管理员
              </el-button>

              <!-- 设为管理员（不能对自己操作） -->
              <el-button
                v-if="
                  normalizeRole(scope.row.role) !== 'admin' &&
                  normalizeRole(scope.row.role) !== 'super_admin' &&
                  scope.row.id !== userInfoStore.info.id
                "
                size="small"
                type="primary"
                @click="confirmUpdateRole(scope.row, 'admin')"
              >
                设为管理员
              </el-button>

              <!-- 降为普通用户（不能对自己操作） -->
              <el-button
                v-if="
                  normalizeRole(scope.row.role) !== 'user' && scope.row.id !== userInfoStore.info.id
                "
                size="small"
                type="info"
                @click="confirmUpdateRole(scope.row, 'user')"
              >
                降为普通用户
              </el-button>

              <!-- 自己不能操作自己 -->
              <span v-if="scope.row.id === userInfoStore.info.id" class="self-tip">
                ⚡ 当前账号
              </span>
            </template>

            <!-- ❌ 普通管理员：显示无权限提示 -->
            <template v-else>
              <span class="no-permission-tip">
                <el-icon><InfoFilled /></el-icon>
                无操作权限
              </span>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- ===== 分页 ===== -->
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
        <el-image-viewer
          v-if="showImageViewer"
          :url-list="[previewImageUrl]"
          @close="closeImageViewer"
        />
      </div>
    </div>

    <!-- ===== 底部统计 ===== -->
    <div class="page-footer">
      <span class="footer-emoji">👥</span>
      <span>共 {{ total }} 个普通用户</span>
      <span class="footer-emoji">✨</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.user-role-manage-container {
  min-height: calc(100vh - 120px);
  padding: 20px;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
}

// ===== 页面头部 =====
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 24px 28px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);

  .header-title-section {
    position: relative;

    .title-decoration {
      position: absolute;
      top: -12px;
      left: -16px;
      display: flex;
      gap: 6px;

      .decoration-dot {
        width: 6px;
        height: 6px;
        background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
        border-radius: 50%;
        opacity: 0.7;

        &:nth-child(2) {
          width: 10px;
          height: 10px;
          opacity: 0.4;
        }

        &:nth-child(3) {
          width: 14px;
          height: 14px;
          opacity: 0.2;
        }
      }
    }

    .page-title {
      font-size: 28px;
      font-weight: 700;
      margin: 0 0 6px 0;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }

    .page-subtitle {
      font-size: 13px;
      color: #a09abf;
      margin: 0;
      letter-spacing: 0.5px;
    }
  }

  .header-tag {
    flex-shrink: 0;
  }
}

// ===== 权限提示横幅 =====
.permission-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  margin-bottom: 16px;
  background: rgba(255, 158, 158, 0.1);
  border: 1px solid rgba(255, 158, 158, 0.2);
  border-radius: 16px;
  color: #d4727a;
  font-size: 14px;

  .el-icon {
    font-size: 20px;
    color: #d4727a;
  }
}

// ===== 搜索栏 =====
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

// ===== 用户列表 =====
.user-list-card {
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

    &.el-tag--primary {
      background: linear-gradient(135deg, #d4d0e8, #c5c0d8);
      color: #5a4a7a;
    }

    &.el-tag--success {
      background: linear-gradient(135deg, #a8e6cf, #7ee0b5);
      color: #2c665a;
    }

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
    padding: 6px 14px;
    font-weight: 500;
    font-size: 12px;
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

    &.el-button--primary {
      background: linear-gradient(135deg, #c5a3ff, #b583ff);
      border: none;
      color: white;

      &:hover {
        box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
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

  .self-tip {
    font-size: 12px;
    color: #c5a3ff;
    font-weight: 500;
  }

  .no-permission-tip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #b0a7c0;
    padding: 4px 12px;
    background: rgba(197, 163, 255, 0.06);
    border-radius: 48px;

    .el-icon {
      font-size: 14px;
    }
  }
}

// ===== 分页 =====
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

// ===== 底部 =====
.page-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding: 16px;
  font-size: 13px;
  color: #a09abf;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  backdrop-filter: blur(10px);

  .footer-emoji {
    font-size: 14px;
    animation: pulse 2s ease-in-out infinite;
  }
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.1);
  }
}

// ===== 响应式 =====
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .user-role-manage-container {
    padding: 12px;
  }

  .search-bar {
    flex-wrap: wrap;
  }
}
</style>

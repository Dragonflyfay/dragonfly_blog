<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, Search, Refresh } from '@element-plus/icons-vue'
import { userListService, userUpdateRoleService } from '@/api/user.js'
import { normalizeRole } from '@/utils/roles.js'

const users = ref([])
const loading = ref(false)
const searchNickname = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 获取用户列表
const getUserList = async () => {
  loading.value = true
  try {
    const result = await userListService()
    console.log('API返回的所有用户数据:', result.data) // 调试用，可以查看实际返回的数据
    const allUsers = result.data || []

    // 过滤出普通用户（排除管理员和超级管理员）
    const normalUsers = allUsers.filter((user) => {
      const role = normalizeRole(user.role)
      return role === 'user'
    })

    // 搜索过滤 - 对普通用户进行搜索过滤（用户名、昵称、邮箱）
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

    console.log('过滤后的普通用户数据:', users.value) // 调试用
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 计算普通用户统计信息
const userStats = computed(() => {
  return {
    total: users.value.length,
    users: users.value.length,
    admins: 0, // 因为只显示普通用户，所以这里始终为0
    superAdmins: 0,
  }
})

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

// 更新用户角色
const updateUserRole = async (user, newRole) => {
  try {
    await userUpdateRoleService({
      userId: user.id,
      role: newRole,
    })
    ElMessage.success('角色更新成功')
    // 重新获取用户列表以刷新数据
    await getUserList()
  } catch (error) {
    console.error('更新角色失败:', error)
    ElMessage.error(error.response?.data?.message || '更新角色失败')
  }
}

// 确认更新角色
const confirmUpdateRole = (user, newRole) => {
  const roleName =
    normalizeRole(newRole) === 'super_admin'
      ? '超级管理员'
      : normalizeRole(newRole) === 'admin'
        ? '管理员'
        : '普通用户'
  ElMessageBox.confirm(
    `确定要将用户 "${user.nickname}" 的角色更改为 ${roleName} 吗？`,
    '确认更改角色',
    {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    },
  )
    .then(() => {
      updateUserRole(user, newRole)
    })
    .catch(() => {
      // 取消操作
    })
}

// 在组件挂载时初始化数据
onMounted(() => {
  getUserList()
})
</script>

<template>
  <div class="user-role-manage-container">
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
    </div>

    <!-- 搜索栏 -->
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

    <div class="user-list-card" v-loading="loading">
      <el-table :data="users" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="头像" width="80">
          <template #default="scope">
            <el-avatar :size="40" :src="scope.row.userPic" />
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
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button
              v-if="normalizeRole(scope.row.role) !== 'super_admin'"
              size="small"
              type="warning"
              @click="confirmUpdateRole(scope.row, 'super_admin')"
            >
              设为超级管理员
            </el-button>
            <el-button
              v-if="
                normalizeRole(scope.row.role) !== 'admin' &&
                normalizeRole(scope.row.role) !== 'super_admin'
              "
              size="small"
              type="primary"
              @click="confirmUpdateRole(scope.row, 'admin')"
            >
              设为管理员
            </el-button>
            <el-button
              v-if="normalizeRole(scope.row.role) !== 'user'"
              size="small"
              type="info"
              @click="confirmUpdateRole(scope.row, 'user')"
            >
              降为普通用户
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

    <div class="page-footer">
      <span class="footer-emoji">👥</span>
      <span>共 {{ users.length }} 个普通用户（当前页）</span>
      <span class="footer-emoji">✨</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.user-role-manage-container {
  min-height: calc(100vh - 120px);
  padding: 20px;
}

.search-bar {
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stats-info {
  margin-bottom: 20px;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .stats-row {
    display: flex;
    justify-content: space-around;
  }

  .stat-item {
    text-align: center;
    padding: 10px;
    border-radius: 8px;

    .stat-number {
      font-size: 24px;
      font-weight: bold;
      margin-bottom: 5px;
    }

    .stat-label {
      font-size: 14px;
      color: #666;
    }

    &.total-users {
      background-color: #f5fff9;
      color: #67c23a;
    }
  }
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;
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
        background: linear-gradient(135deg, #ff2442, #ff6b8a);
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
      background: linear-gradient(135deg, #ff2442, #ff8fa3, #ffb8c8);
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
}

.user-list-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 24px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
}

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

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
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
</style>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete, Edit } from '@element-plus/icons-vue'
import { userListService, userUpdateRoleService } from '@/api/user.js'
import { normalizeRole } from '@/utils/roles.js'

const users = ref([])
const loading = ref(false)
const searchNickname = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedUsers = ref([])

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
const demoteToUser = (user) => {
  const normalizedRole = normalizeRole(user.role)
  const roleName = normalizedRole === 'super_admin' ? '超级管理员' : '管理员'
  ElMessageBox.confirm(`确定要将${roleName} "${user.nickname}" 降级为普通用户吗？`, '确认降级', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
    .then(() => {
      updateUserRole(user, 'user')
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
            <el-avatar :size="50" :src="scope.row.userPic" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button type="info" size="small" :icon="Edit" @click="demoteToUser(scope.row)">
              降级
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
  </div>
</template>

<style lang="scss" scoped>
.admin-role-manage-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
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

.action-bar {
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

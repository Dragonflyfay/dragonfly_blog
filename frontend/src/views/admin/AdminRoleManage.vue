<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Delete,
  Edit,
  User,
  Lock,
  Management,
  InfoFilled,
  Check,
} from '@element-plus/icons-vue'
import { userListService, userUpdateRoleService } from '@/api/user.js'
import { normalizeRole } from '@/utils/roles.js'
import { userRegisterService } from '@/api/user.js'

import useUserInfoStore from '@/stores/userInfo.js'

// ==================== 列表数据 ====================
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

// ==================== 权限判断 ====================
// 是否是超级管理员
const isSuperAdmin = computed(() => currentUserRole === 'super_admin')
// 是否是普通管理员（非超级管理员）
const isNormalAdmin = computed(() => currentUserRole === 'admin')

// ==================== 添加管理员弹窗 ====================
const showAddAdminDialog = ref(false)
const submitLoading = ref(false)
const addAdminFormRef = ref(null)

const addAdminForm = reactive({
  username: '',
  password: '',
  role: 'admin',
})

const addAdminRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 15, message: '用户名长度为1-15个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 5, max: 16, message: '密码长度为5-16个字符', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

// 添加管理员（仅超级管理员可操作）
const addAdmin = async () => {
  // 权限检查
  if (!isSuperAdmin.value) {
    ElMessage.warning('只有超级管理员可以添加管理员')
    return
  }

  if (!addAdminFormRef.value) return

  await addAdminFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      // 1. 注册用户
      await userRegisterService({
        username: addAdminForm.username,
        password: addAdminForm.password,
      })

      // 2. 查询用户ID并更新角色
      const userListRes = await userListService()
      const allUsers = userListRes.data || []
      const targetUser = allUsers.find((u) => u.username === addAdminForm.username)

      if (targetUser) {
        await userUpdateRoleService({
          userId: targetUser.id,
          role: addAdminForm.role,
        })
        ElMessage.success(`管理员 "${addAdminForm.username}" 添加成功`)
      } else {
        ElMessage.error('用户创建成功，但未找到该用户，请手动分配角色')
      }

      showAddAdminDialog.value = false
      resetAddAdminForm()
      getUserList()
    } catch (error) {
      console.error('添加管理员失败:', error)
      ElMessage.error(error.response?.data?.message || '添加失败，请重试')
    } finally {
      submitLoading.value = false
    }
  })
}

const resetAddAdminForm = () => {
  addAdminForm.username = ''
  addAdminForm.password = ''
  addAdminForm.role = 'admin'
  addAdminFormRef.value?.clearValidate()
}

// ==================== 获取用户列表 ====================
const getUserList = async () => {
  loading.value = true
  try {
    const result = await userListService()
    const allUsers = result.data || []

    const adminUsers = allUsers.filter((user) => {
      const normalizedRole = normalizeRole(user.role)
      return normalizedRole === 'admin' || normalizedRole === 'super_admin'
    })

    let filteredUsers = adminUsers
    if (searchNickname.value) {
      filteredUsers = adminUsers.filter(
        (user) =>
          user.nickname && user.nickname.toLowerCase().includes(searchNickname.value.toLowerCase()),
      )
    }

    total.value = filteredUsers.length
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    users.value = filteredUsers.slice(start, end)
  } catch (error) {
    if (error?.__handled) return
    console.error('获取管理员用户列表失败:', error)
    ElMessage.error('获取管理员用户列表失败')
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

// ==================== 选择变化 ====================
const handleSelectionChange = (selection) => {
  selectedUsers.value = selection
}

// ==================== 更新用户角色 ====================
const updateUserRole = async (user, newRole) => {
  // 权限检查：只有超级管理员可以修改角色
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

// ==================== 降级为普通管理员（超级管理员专属） ====================
const demoteToAdmin = (user) => {
  if (!isSuperAdmin.value) {
    ElMessage.warning('只有超级管理员可以执行此操作')
    return
  }

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

// ==================== 降级为普通用户（超级管理员专属） ====================
const demoteToUser = (user) => {
  if (!isSuperAdmin.value) {
    ElMessage.warning('只有超级管理员可以执行此操作')
    return
  }

  ElMessageBox.confirm(`确定要将管理员 "${user.nickname}" 降级为普通用户吗？`, '确认降级', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
    .then(() => {
      updateUserRole(user, 'user')
    })
    .catch(() => {})
}

// ==================== 批量降级（超级管理员专属） ====================
const batchDemote = () => {
  if (!isSuperAdmin.value) {
    ElMessage.warning('只有超级管理员可以批量降级')
    return
  }

  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请先选择用户')
    return
  }

  // 检查是否包含自己
  const hasSelf = selectedUsers.value.some((u) => u.id === userInfoStore.info.id)
  if (hasSelf) {
    ElMessage.warning('不能降级自己')
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
      let successCount = 0
      for (const user of selectedUsers.value) {
        try {
          await userUpdateRoleService({
            userId: user.id,
            role: 'user',
          })
          successCount++
        } catch (e) {
          console.error(`降级用户 ${user.username} 失败:`, e)
        }
      }
      selectedUsers.value = []
      ElMessage.success(`成功降级 ${successCount} 个用户`)
      getUserList()
    })
    .catch(() => {})
}

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

// ==================== 初始化 ====================
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

    <!-- 操作栏 -->
    <div class="action-bar">
      <!-- 只有超级管理员可以看到"添加管理员"按钮 -->
      <el-button
        v-if="isSuperAdmin"
        class="add-admin-btn"
        :icon="Plus"
        @click="showAddAdminDialog = true"
      >
        <span>添加管理员</span>
      </el-button>

      <!-- 只有超级管理员可以看到"批量降级"按钮 -->
      <el-button v-if="isSuperAdmin" type="danger" :icon="Delete" @click="batchDemote">
        批量降级
      </el-button>

      <!-- 普通管理员提示 -->
      <span v-if="isNormalAdmin" class="permission-tip">
        <el-icon><InfoFilled /></el-icon>
        普通管理员无权限管理其他管理员
      </span>
    </div>

    <!-- ==================== 添加管理员弹窗 ==================== -->
    <el-dialog
      v-model="showAddAdminDialog"
      title=""
      width="440px"
      top="25vh"
      class="add-admin-dialog"
      destroy-on-close
    >
      <template #header>
        <div class="dialog-header">
          <div class="header-icon-wrapper">
            <el-icon><User /></el-icon>
          </div>
          <div class="header-text">
            <span class="header-title">添加管理员</span>
            <span class="header-subtitle">为新管理员分配账号和权限</span>
          </div>
        </div>
      </template>

      <el-form
        ref="addAdminFormRef"
        :model="addAdminForm"
        :rules="addAdminRules"
        label-width="0"
        class="add-admin-form"
      >
        <el-form-item prop="username">
          <div class="form-field">
            <div class="field-icon">
              <el-icon><User /></el-icon>
            </div>
            <el-input
              v-model="addAdminForm.username"
              placeholder="请输入用户名"
              size="large"
              class="field-input"
              clearable
            >
              <template #prefix>
                <span class="input-prefix-label">用户名</span>
              </template>
            </el-input>
          </div>
        </el-form-item>

        <el-form-item prop="password">
          <div class="form-field">
            <div class="field-icon">
              <el-icon><Lock /></el-icon>
            </div>
            <el-input
              v-model="addAdminForm.password"
              type="password"
              placeholder="请输入初始密码"
              size="large"
              class="field-input"
              show-password
              clearable
            >
              <template #prefix>
                <span class="input-prefix-label">初始密码</span>
              </template>
            </el-input>
          </div>
        </el-form-item>

        <el-form-item prop="role">
          <div class="form-field">
            <div class="field-icon">
              <el-icon><Management /></el-icon>
            </div>
            <el-select
              v-model="addAdminForm.role"
              placeholder="选择角色"
              size="large"
              class="field-select"
            >
              <template #prefix>
                <span class="input-prefix-label">角色</span>
              </template>
              <el-option label="超级管理员" value="super_admin">
                <div class="role-option">
                  <span class="role-dot super"></span>
                  <span>超级管理员</span>
                  <span class="role-desc">所有权限</span>
                </div>
              </el-option>
              <el-option label="管理员" value="admin">
                <div class="role-option">
                  <span class="role-dot admin"></span>
                  <span>管理员</span>
                  <span class="role-desc">除系统设置外</span>
                </div>
              </el-option>
            </el-select>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button class="cancel-btn" @click="showAddAdminDialog = false">取消</el-button>
          <el-button class="confirm-btn" type="primary" @click="addAdmin" :loading="submitLoading">
            <el-icon><Check /></el-icon>
            确认添加
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ==================== 管理员用户列表 ==================== -->
    <div class="table-card" v-loading="loading">
      <el-table :data="users" style="width: 100%" stripe @selection-change="handleSelectionChange">
        <!-- 只有超级管理员可以看到多选框 -->
        <el-table-column v-if="isSuperAdmin" type="selection" width="55" />
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
            <!-- ===== 只有超级管理员才显示操作按钮 ===== -->
            <template v-if="isSuperAdmin">
              <!-- 超级管理员：将其他超级管理员降级为普通管理员 -->
              <el-button
                v-if="
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

              <!-- 超级管理员：将普通管理员降级为普通用户 -->
              <el-button
                v-if="
                  normalizeRole(scope.row.role) === 'admin' &&
                  scope.row.id !== userInfoStore.info.id
                "
                type="info"
                size="small"
                :icon="Edit"
                @click="demoteToUser(scope.row)"
              >
                降级为用户
              </el-button>
            </template>

            <!-- ===== 普通管理员：无操作权限 ===== -->
            <span v-else class="no-permission-text">无权限</span>
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

  .permission-tip {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #b0a7c0;
    padding: 8px 16px;
    background: rgba(197, 163, 255, 0.06);
    border-radius: 48px;

    .el-icon {
      font-size: 16px;
      color: #c5a3ff;
    }
  }

  :deep(.el-button) {
    border-radius: 48px;
    padding: 10px 20px;
    font-weight: 500;
    transition: all 0.3s ease;

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

  .no-permission-text {
    font-size: 12px;
    color: #b0a7c0;
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

// ============================================================
// 添加管理员弹窗样式
// ============================================================

.add-admin-btn {
  background: linear-gradient(135deg, #c5a3ff 0%, #b583ff 100%);
  border: none;
  border-radius: 48px;
  padding: 10px 24px;
  color: white;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);

  span {
    font-size: 14px;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
    color: white;
  }

  &:active {
    transform: translateY(0);
  }

  .el-icon {
    font-size: 16px;
    margin-right: 4px;
  }
}

.add-admin-dialog {
  :deep(.el-dialog) {
    border-radius: 24px;
    overflow: hidden;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
    background: transparent;
  }

  :deep(.el-dialog__header) {
    padding: 0;
    margin: 0;
    height: 0;
    display: none;
  }

  :deep(.el-dialog__body) {
    padding: 32px 32px 0;
    background: #ffffff;
  }

  :deep(.el-dialog__footer) {
    padding: 16px 32px 32px;
    background: #ffffff;
    border-top: 1px solid #f5f0ff;
  }
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 28px 32px 20px;
  background: linear-gradient(135deg, #f8f3ff 0%, #fef9ff 100%);
  border-bottom: 1px solid #f0e5ff;

  .header-icon-wrapper {
    width: 48px;
    height: 48px;
    border-radius: 16px;
    background: linear-gradient(135deg, #c5a3ff, #b583ff);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);

    .el-icon {
      font-size: 24px;
      color: white;
    }
  }

  .header-text {
    display: flex;
    flex-direction: column;

    .header-title {
      font-size: 20px;
      font-weight: 700;
      color: #2d2d44;
      line-height: 1.3;
    }

    .header-subtitle {
      font-size: 13px;
      color: #b0a7c0;
    }
  }
}

.add-admin-form {
  padding: 4px 0;

  .el-form-item {
    margin-bottom: 20px;

    &:last-of-type {
      margin-bottom: 4px;
    }
  }

  .form-field {
    display: flex;
    align-items: center;
    gap: 12px;
    position: relative;

    .field-icon {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      background: linear-gradient(135deg, #f5f0ff, #faf7ff);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      transition: all 0.3s ease;

      .el-icon {
        font-size: 18px;
        color: #c5a3ff;
      }
    }

    &:focus-within .field-icon {
      background: linear-gradient(135deg, #ede5ff, #f5f0ff);
      box-shadow: 0 0 0 3px rgba(197, 163, 255, 0.15);

      .el-icon {
        color: #b583ff;
      }
    }

    .field-input,
    .field-select {
      flex: 1;

      :deep(.el-input__wrapper) {
        border-radius: 12px;
        padding: 4px 16px;
        background: #faf7ff;
        border: 1.5px solid #f0e5ff;
        transition: all 0.3s ease;
        box-shadow: none;

        &:hover {
          border-color: #d9b8ff;
          background: #fff;
        }

        &.is-focus {
          border-color: #c5a3ff;
          background: #fff;
          box-shadow: 0 0 0 4px rgba(197, 163, 255, 0.1);
        }
      }

      :deep(.el-input__prefix) {
        .input-prefix-label {
          font-size: 12px;
          color: #b0a7c0;
          font-weight: 500;
          padding-right: 8px;
          border-right: 1.5px solid #f0e5ff;
          margin-right: 8px;
        }
      }

      :deep(.el-input__inner) {
        font-size: 14px;
        color: #2d2d44;

        &::placeholder {
          color: #c5c0d4;
        }
      }
    }

    .field-select {
      :deep(.el-input__wrapper) {
        cursor: pointer;
      }

      :deep(.el-select__caret) {
        color: #c5a3ff;
        font-size: 14px;
      }
    }
  }
}

.role-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 0;

  .role-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex-shrink: 0;

    &.super {
      background: linear-gradient(135deg, #ff6b6b, #ff8e8e);
    }

    &.admin {
      background: linear-gradient(135deg, #c5a3ff, #b583ff);
    }
  }

  .role-desc {
    font-size: 11px;
    color: #b0a7c0;
    margin-left: auto;
  }
}

:deep(.el-select-dropdown) {
  border-radius: 16px;
  border: 1px solid #f0e5ff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;

  .el-select-dropdown__item {
    padding: 10px 16px;
    transition: all 0.2s ease;

    &:hover {
      background: #f8f3ff;
    }

    &.is-selected {
      background: linear-gradient(135deg, #f5f0ff, #faf7ff);
      color: #c5a3ff;
      font-weight: 500;
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .cancel-btn {
    border-radius: 48px;
    padding: 10px 28px;
    border: 1.5px solid #f0e5ff;
    color: #8a7a9a;
    font-weight: 500;
    transition: all 0.3s ease;
    background: transparent;

    &:hover {
      border-color: #c5a3ff;
      color: #c5a3ff;
      background: rgba(197, 163, 255, 0.05);
      transform: translateY(-2px);
    }
  }

  .confirm-btn {
    border-radius: 48px;
    padding: 10px 28px;
    background: linear-gradient(135deg, #c5a3ff 0%, #b583ff 100%);
    border: none;
    color: white;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 6px;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);

    .el-icon {
      font-size: 16px;
    }

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
    }

    &:active {
      transform: translateY(0);
    }
  }
}

.add-admin-dialog {
  :deep(.el-dialog) {
    animation: dialogFadeIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  }
}

@keyframes dialogFadeIn {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@media (max-width: 480px) {
  .add-admin-dialog {
    :deep(.el-dialog) {
      width: 92% !important;
      border-radius: 20px;
    }
  }

  .dialog-header {
    padding: 20px 20px 16px;
    flex-wrap: wrap;

    .header-icon-wrapper {
      width: 40px;
      height: 40px;

      .el-icon {
        font-size: 20px;
      }
    }

    .header-text .header-title {
      font-size: 17px;
    }
  }

  .add-admin-form {
    .form-field {
      gap: 8px;

      .field-icon {
        width: 34px;
        height: 34px;

        .el-icon {
          font-size: 15px;
        }
      }
    }
  }

  .dialog-footer {
    flex-direction: column-reverse;

    .cancel-btn,
    .confirm-btn {
      width: 100%;
      justify-content: center;
    }
  }
}
</style>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Delete, View, Sort, SortUp, SortDown } from '@element-plus/icons-vue'
import { commentPageListService, commentDeleteService } from '@/api/comment.js'

const comments = ref([])
const loading = ref(false)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedComments = ref([])

// 排序相关
const sortOrder = ref('desc') // desc: 最新优先, asc: 最早优先
const statusFilter = ref('') // '', 'normal', 'deleted'

// 内容预览弹窗
const previewVisible = ref(false)
const previewComment = ref(null)

// 批量删除loading
const batchDeleting = ref(false)

// 获取评论列表
const getCommentList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sortOrder: sortOrder.value,
    }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value === 'normal' ? 1 : 0

    const result = await commentPageListService(params)
    total.value = result.data?.total || 0

    // 处理评论数据，添加层级显示信息
    const items = result.data?.items || []
    comments.value = items.map(comment => ({
      ...comment,
      // 构建回复预览文本
      replyPreview: comment.parentId && comment.parentId !== 0
          ? `回复 @${comment.replyToUserName || '用户'}：${comment.content?.slice(0, 30)}${comment.content?.length > 30 ? '...' : ''}`
          : null,
      // 是否有子评论
      hasChildren: false, // 后端可以返回 childCount 字段
    }))
  } catch (error) {
    console.error('获取评论列表失败:', error)
    ElMessage.error('获取评论列表失败')
    comments.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  getCommentList()
}

// 重置
const handleReset = () => {
  keyword.value = ''
  statusFilter.value = ''
  sortOrder.value = 'desc'
  pageNum.value = 1
  getCommentList()
}

// 分页变化
const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  getCommentList()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  getCommentList()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedComments.value = selection
}

// 删除评论
const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除这条评论吗？删除后无法恢复。', '删除评论', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
      .then(async () => {
        try {
          await commentDeleteService(row.id)
          ElMessage.success('已删除')
          // 如果当前页只有一条数据且不是第一页，回到上一页
          if (comments.value.length === 1 && pageNum.value > 1) {
            pageNum.value--
          }
          getCommentList()
        } catch (error) {
          console.error('删除评论失败:', error)
          ElMessage.error(error.response?.data?.message || '删除失败')
        }
      })
      .catch(() => {})
}

// 批量删除
const batchDelete = () => {
  if (selectedComments.value.length === 0) {
    ElMessage.warning('请先选择要删除的评论')
    return
  }

  ElMessageBox.confirm(
      `确定要删除选中的 ${selectedComments.value.length} 条评论吗？删除后无法恢复。`,
      '批量删除',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      }
  )
      .then(async () => {
        batchDeleting.value = true
        let successCount = 0
        let failCount = 0

        for (const comment of selectedComments.value) {
          try {
            await commentDeleteService(comment.id)
            successCount++
          } catch (error) {
            console.error(`删除评论 ${comment.id} 失败:`, error)
            failCount++
          }
        }

        batchDeleting.value = false

        if (failCount === 0) {
          ElMessage.success(`成功删除 ${successCount} 条评论`)
        } else {
          ElMessage.warning(`成功删除 ${successCount} 条，失败 ${failCount} 条`)
        }

        // 清空选中
        selectedComments.value = []
        // 刷新列表
        if (comments.value.length === selectedComments.value.length && pageNum.value > 1) {
          pageNum.value--
        }
        getCommentList()
      })
      .catch(() => {})
}

// 预览评论内容
const openPreview = (comment) => {
  previewComment.value = comment
  previewVisible.value = true
}

const closePreview = () => {
  previewVisible.value = false
  previewComment.value = null
}

// 截断内容显示
const truncateContent = (content, maxLen = 50) => {
  if (!content) return ''
  return content.length > maxLen ? content.slice(0, maxLen) + '…' : content
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 状态标签
const getStatusTag = (status) => {
  return status === 0 ? '已删除' : '正常'
}

const getStatusType = (status) => {
  return status === 0 ? 'danger' : 'success'
}

// 切换排序
const toggleSort = () => {
  sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
  pageNum.value = 1
  getCommentList()
}

// 获取排序图标
const getSortIcon = () => {
  return sortOrder.value === 'desc' ? SortDown : SortUp
}

// 统计信息
const stats = computed(() => {
  return {
    total: total.value,
    normal: comments.value.filter(c => c.status === 1).length,
    deleted: comments.value.filter(c => c.status === 0).length,
  }
})

onMounted(() => {
  getCommentList()
})
</script>

<template>
  <div class="comment-manage-container">
    <div class="page-header">
      <div class="header-title-section">
        <div class="title-decoration">
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
        </div>
        <h1 class="page-title">评论管理</h1>
        <p class="page-subtitle">查看和管理所有评论内容</p>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
          v-model="keyword"
          placeholder="搜索评论内容 / 用户昵称 / 笔记标题"
          :prefix-icon="Search"
          clearable
          style="width: 320px"
          @keyup.enter="handleSearch"
      />

      <el-select
          v-model="statusFilter"
          placeholder="评论状态"
          clearable
          style="width: 120px"
          @change="handleSearch"
      >
        <el-option label="全部" value="" />
        <el-option label="正常" value="normal" />
        <el-option label="已删除" value="deleted" />
      </el-select>

      <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="action-left">
        <el-button
            type="danger"
            :icon="Delete"
            @click="batchDelete"
            :loading="batchDeleting"
            :disabled="selectedComments.length === 0"
        >
          批量删除 ({{ selectedComments.length }})
        </el-button>
      </div>
      <div class="action-right">
        <span class="stats-info">
          <span class="stats-tag">共 {{ stats.total }} 条评论</span>
          <span class="stats-tag normal">正常 {{ stats.normal }}</span>
          <span class="stats-tag deleted">已删除 {{ stats.deleted }}</span>
        </span>
        <el-button :icon="getSortIcon()" @click="toggleSort" text>
          {{ sortOrder === 'desc' ? '最新优先' : '最早优先' }}
        </el-button>
      </div>
    </div>

    <div class="comment-list-card" v-loading="loading">
      <el-table
          :data="comments"
          style="width: 100%"
          stripe
          @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />

        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="用户" min-width="160">
          <template #default="scope">
            <div class="user-cell">
              <el-avatar :size="36" :src="scope.row.userPic" />
              <div class="user-info">
                <span class="user-name">{{ scope.row.nickname || scope.row.username }}</span>
                <span class="user-id">ID: {{ scope.row.userId }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="所属笔记" min-width="180">
          <template #default="scope">
            <div class="note-info">
              <span class="note-title-link">{{ scope.row.noteTitle || '-' }}</span>
              <span class="note-id">笔记ID: {{ scope.row.noteId }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="评论内容" min-width="280">
          <template #default="scope">
            <div class="content-wrapper">
              <!-- 回复标识 -->
              <div v-if="scope.row.parentId && scope.row.parentId !== 0" class="reply-indicator">
                <span class="reply-icon">↳</span>
                <span class="reply-text">回复 @{{ scope.row.replyToUserName || '用户' }}</span>
              </div>
              <div
                  class="content-cell"
                  :class="{ 'has-reply': scope.row.parentId && scope.row.parentId !== 0 }"
                  @click="openPreview(scope.row)"
              >
                {{ truncateContent(scope.row.content) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="likesCount" label="👍 点赞" width="90" align="center">
          <template #default="scope">
            <span class="likes-count">{{ scope.row.likesCount || 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="评论时间" width="160">
          <template #default="scope">
            <div class="time-info">
              <span>{{ formatTime(scope.row.createTime) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusTag(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="scope">
            <el-button
                size="small"
                :icon="View"
                type="primary"
                link
                @click="openPreview(scope.row)"
            >查看</el-button>
            <el-button
                size="small"
                :icon="Delete"
                type="danger"
                link
                @click="handleDelete(scope.row)"
                :disabled="scope.row.status === 0"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <div v-if="!loading && comments.length === 0" class="custom-empty">
        <span class="empty-emoji">💬</span>
        <p class="empty-text">暂无评论数据</p>
        <p class="empty-hint">用户发表的评论将在这里显示</p>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <div class="page-footer">
      <span class="footer-emoji">💜</span>
      <span>共 {{ total }} 条评论</span>
      <span class="footer-emoji">✨</span>
    </div>

    <!-- 评论内容预览弹窗 -->
    <el-dialog
        v-model="previewVisible"
        title="评论详情"
        width="560px"
        top="8vh"
        class="preview-dialog"
        destroy-on-close
    >
      <div v-if="previewComment" class="preview-body">
        <div class="preview-meta">
          <el-avatar :size="44" :src="previewComment.userPic" />
          <div class="preview-user-info">
            <span class="preview-username">{{ previewComment.nickname || previewComment.username }}</span>
            <span class="preview-note">📝 {{ previewComment.noteTitle || '未知笔记' }}</span>
          </div>
          <el-tag :type="getStatusType(previewComment.status)" size="small" class="preview-status">
            {{ getStatusTag(previewComment.status) }}
          </el-tag>
        </div>

        <!-- 回复信息 -->
        <div v-if="previewComment.parentId && previewComment.parentId !== 0" class="preview-reply-info">
          <span class="reply-badge">回复</span>
          <span class="reply-target">@{{ previewComment.replyToUserName || '用户' }}</span>
        </div>

        <div class="preview-content">{{ previewComment.content }}</div>

        <div class="preview-stats">
          <span class="stat-item">
            <span class="stat-icon">❤️</span>
            {{ previewComment.likesCount || 0 }} 点赞
          </span>
          <span class="stat-item">
            <span class="stat-icon">🕐</span>
            {{ formatTime(previewComment.createTime) }}
          </span>
          <span class="stat-item" v-if="previewComment.updateTime && previewComment.updateTime !== previewComment.createTime">
            <span class="stat-icon">✏️</span>
            编辑于 {{ formatTime(previewComment.updateTime) }}
          </span>
        </div>
      </div>
      <template #footer>
        <el-button @click="closePreview">关闭</el-button>
        <el-button
            v-if="previewComment?.status !== 0"
            type="danger"
            @click="handleDelete(previewComment); closePreview()"
        >
          删除评论
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.comment-manage-container {
  min-height: calc(100vh - 120px);
  padding: 20px;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
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
}

.search-bar {
  background: rgba(255, 255, 255, 0.96);
  padding: 16px 20px;
  border-radius: 30px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
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

  :deep(.el-select) {
    .el-input__wrapper {
      border-radius: 48px;
    }
  }
}

.action-bar {
  background: rgba(255, 255, 255, 0.96);
  padding: 12px 20px;
  border-radius: 30px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(10px);

  .action-left {
    display: flex;
    gap: 12px;
  }

  .action-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stats-info {
    display: flex;
    gap: 16px;
    font-size: 13px;

    .stats-tag {
      padding: 4px 12px;
      background: rgba(197, 163, 255, 0.1);
      border-radius: 48px;
      color: #6a4a9c;

      &.normal {
        color: #2c665a;
        background: rgba(168, 230, 207, 0.15);
      }

      &.deleted {
        color: #8a1c1c;
        background: rgba(255, 158, 158, 0.15);
      }
    }
  }

  :deep(.el-button) {
    border-radius: 48px;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
    }
  }
}

.comment-list-card {
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

    &.el-tag--success {
      background: linear-gradient(135deg, #a8e6cf, #7ee0b5);
      color: #2c665a;
    }

    &.el-tag--danger {
      background: linear-gradient(135deg, #ff9e9e, #ffbaba);
      color: #8a1c1c;
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
  }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  .user-info {
    display: flex;
    flex-direction: column;

    .user-name {
      font-size: 14px;
      font-weight: 500;
      color: #333;
    }

    .user-id {
      font-size: 11px;
      color: #a09abf;
    }
  }
}

.note-info {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .note-title-link {
    color: #c5a3ff;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }

  .note-id {
    font-size: 11px;
    color: #a09abf;
  }
}

.content-wrapper {
  .reply-indicator {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-bottom: 4px;
    font-size: 11px;
    color: #a09abf;

    .reply-icon {
      font-size: 14px;
    }

    .reply-text {
      color: #c5a3ff;
    }
  }

  .content-cell {
    cursor: pointer;
    color: #555;
    font-size: 13px;
    line-height: 1.5;

    &.has-reply {
      padding-left: 16px;
      border-left: 2px solid #c5a3ff;
    }

    &:hover {
      color: #c5a3ff;
    }
  }
}

.likes-count {
  font-weight: 500;
  color: #f8b4d9;
}

.time-info {
  font-size: 12px;
  color: #999;
}

.custom-empty {
  padding: 60px 20px;
  text-align: center;

  .empty-emoji {
    font-size: 48px;
    display: block;
    margin-bottom: 12px;
  }

  .empty-text {
    font-size: 16px;
    color: #666;
    margin: 0 0 8px 0;
    font-weight: 500;
  }

  .empty-hint {
    font-size: 13px;
    color: #999;
    margin: 0;
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

// 预览弹窗样式
:deep(.preview-dialog) {
  .el-dialog__header {
    border-bottom: 1px solid #f0e5ff;
    padding: 16px 20px;
    background: linear-gradient(135deg, #f8f3ff 0%, #faf7ff 100%);
  }

  .el-dialog__body {
    padding: 20px;
  }
}

.preview-body {
  .preview-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f5f0ff;

    .preview-user-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;

      .preview-username {
        font-size: 15px;
        font-weight: 600;
        color: #333;
      }

      .preview-note {
        font-size: 12px;
        color: #999;
      }
    }

    .preview-status {
      flex-shrink: 0;
    }
  }

  .preview-reply-info {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    padding: 6px 12px;
    background: rgba(197, 163, 255, 0.08);
    border-radius: 48px;
    font-size: 12px;

    .reply-badge {
      color: #c5a3ff;
      font-weight: 500;
    }

    .reply-target {
      color: #f8b4d9;
    }
  }

  .preview-content {
    font-size: 15px;
    color: #444;
    line-height: 1.8;
    padding: 12px 0;
    white-space: pre-wrap;
    word-break: break-word;
    min-height: 60px;
  }

  .preview-stats {
    display: flex;
    gap: 20px;
    margin-top: 16px;
    padding-top: 12px;
    border-top: 1px solid #f5f0ff;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
      color: #999;

      .stat-icon {
        font-size: 14px;
      }
    }
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
</style>
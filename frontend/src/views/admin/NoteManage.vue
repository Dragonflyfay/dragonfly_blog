<script setup>
import { computed, ref, onMounted } from 'vue'
import {
  Delete,
  Edit,
  LocationFilled,
  PictureFilled,
  View,
  ArrowLeft,
  ArrowRight,
  VideoPlay,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  noteDeleteService,
  notePageListService,
  topicListService,
  userListService,
} from '@/api/note.js'

const topics = ref([])
const topicId = ref('')
const state = ref('')
const userId = ref('')
const users = ref([])

const notes = ref([])
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(12)
const loading = ref(false)

// 笔记详情预览相关
const previewVisible = ref(false)
const previewNote = ref(null)
const currentImageIndex = ref(0)
const videoPlayerRef = ref(null)

const PLACEHOLDER_IMG =
  'data:image/svg+xml,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="533" viewBox="0 0 400 533"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#ffeef8"/><stop offset="100%" stop-color="#e8f4ff"/></linearGradient></defs><rect width="400" height="533" fill="url(#g)"/><text x="200" y="270" text-anchor="middle" fill="#b8b0c8" font-family="system-ui" font-size="14">暂无封面</text></svg>`,
  )

const cardCover = (row) => {
  if (row.coverImg) return row.coverImg
  if (Array.isArray(row.images) && row.images.length > 0) return row.images[0]
  if (row.video) return row.video
  return PLACEHOLDER_IMG
}

const onImgError = (e) => {
  const el = e.target
  if (el) el.src = PLACEHOLDER_IMG
}

const contentPreview = (html, maxLen = 72) => {
  if (!html) return ''
  const div = document.createElement('div')
  div.innerHTML = html
  const text = (div.textContent || '').replace(/\s+/g, ' ').trim()
  if (!text) return ''
  return text.length > maxLen ? `${text.slice(0, maxLen)}…` : text
}

// 打开笔记预览
const openPreview = (note) => {
  previewNote.value = note
  currentImageIndex.value = 0
  previewVisible.value = true
}

// 关闭预览
const closePreview = () => {
  previewVisible.value = false
  previewNote.value = null
  currentImageIndex.value = 0
  // 暂停视频播放
  if (videoPlayerRef.value) {
    videoPlayerRef.value.pause()
  }
}

// 上一张图片
const prevImage = () => {
  if (!previewNote.value?.images || previewNote.value.images.length <= 1) return
  currentImageIndex.value =
    (currentImageIndex.value - 1 + previewNote.value.images.length) %
    previewNote.value.images.length
}

// 下一张图片
const nextImage = () => {
  if (!previewNote.value?.images || previewNote.value.images.length <= 1) return
  currentImageIndex.value = (currentImageIndex.value + 1) % previewNote.value.images.length
}

// 获取媒体类型
const getMediaType = (note) => {
  if (note.noteCategory === 'video') return 'video'
  if (note.noteCategory === 'image') return 'image'
  // 如果没有明确类型，根据字段判断
  if (note.video) return 'video'
  if (note.images && note.images.length > 0) return 'image'
  return 'unknown'
}

// 创建映射表优化查找性能
const topicMap = computed(() => {
  const map = {}
  topics.value.forEach((t) => {
    map[t.id] = t.topicName
  })
  return map
})

const userMap = computed(() => {
  const map = {}
  users.value.forEach((u) => {
    map[u.id] = u.nickname
  })
  return map
})

const topicNameOf = (note) => {
  return topicMap.value[note.topicId] || '未分类'
}

const userNameOf = (note) => {
  return userMap.value[note.createUser] || '未知用户'
}

const getTopicList = async () => {
  try {
    const result = await topicListService()
    topics.value = result.data || []
  } catch (error) {
    console.error('获取话题列表失败:', error)
    ElMessage.error('获取话题列表失败')
    topics.value = []
  }
}

const getUserList = async () => {
  try {
    const result = await userListService()
    users.value = result.data || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
    users.value = []
  }
}

const noteList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }
    if (topicId.value !== '' && topicId.value != null) params.topicId = topicId.value
    if (state.value) params.state = state.value
    if (userId.value !== '' && userId.value != null) params.userId = userId.value

    const result = await notePageListService(params)
    total.value = result.data.total
    notes.value = (result.data.items || []).map((note) => ({
      ...note,
      topicName: topicNameOf(note),
      userName: userNameOf(note),
    }))
  } catch (error) {
    console.error('获取笔记列表失败:', error)
    ElMessage.error('获取笔记列表失败')
    notes.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const onSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  noteList()
}

const onCurrentChange = (num) => {
  pageNum.value = num
  noteList()
}

const resetFilters = () => {
  topicId.value = ''
  state.value = ''
  userId.value = ''
  pageNum.value = 1
  noteList()
}

const removeNote = (row) => {
  ElMessageBox.confirm('确定删除这篇笔记吗？删除后无法恢复。', '删除笔记', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
    .then(async () => {
      try {
        await noteDeleteService(row.id)
        ElMessage.success('已删除')
        // 如果当前页只有一条数据且不是第一页，回到上一页
        if (notes.value.length === 1 && pageNum.value > 1) {
          pageNum.value--
        }
        noteList()
      } catch (error) {
        console.error('删除笔记失败:', error)
        ElMessage.error(error.response?.data?.message || '删除失败')
      }
    })
    .catch(() => {})
}

// 视频错误处理
const handleVideoError = (e) => {
  console.error('视频加载失败:', e)
  ElMessage.error('视频加载失败，请检查视频链接是否有效')
}

// 视频元数据加载完成
const handleVideoLoaded = (e) => {
  const video = e.target
  // 确保视频第一帧被渲染
  if (video.readyState >= 2) {
    video.currentTime = 0
  }
}

// 在组件挂载时初始化数据
onMounted(() => {
  getTopicList()
  getUserList()
  noteList()
})
</script>

<template>
  <div class="article-manage-container">
    <div class="page-header">
      <div class="header-title-section">
        <div class="title-decoration">
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
        </div>
        <h1 class="page-title">笔记管理</h1>
        <p class="page-subtitle">瀑布流浏览 · 封面标题 · 互动数据</p>
      </div>
    </div>

    <div class="search-card">
      <div class="search-title">
        <span class="search-emoji">🔍</span>
        <span>筛选</span>
      </div>
      <el-form inline class="search-form">
        <el-form-item label="用户">
          <el-select
            v-model="userId"
            placeholder="全部用户"
            clearable
            class="custom-select"
            style="width: 200px"
            @clear="userId = ''"
          >
            <el-option v-for="u in users" :key="u.id" :label="u.nickname" :value="u.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="话题">
          <el-select
            v-model="topicId"
            placeholder="全部话题"
            clearable
            class="custom-select"
            style="width: 200px"
            @clear="topicId = ''"
          >
            <el-option v-for="t in topics" :key="t.id" :label="t.topicName" :value="t.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="state"
            placeholder="全部状态"
            clearable
            class="custom-select"
            style="width: 160px"
          >
            <el-option label="已发布" value="已发布" />
            <el-option label="草稿" value="草稿" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button
            class="search-btn"
            @click="
              () => {
                pageNum = 1
                noteList()
              }
            "
            >搜索</el-button
          >
          <el-button class="reset-btn" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="feed-card" v-loading="loading">
      <div v-if="notes.length" class="xhs-feed">
        <article v-for="row in notes" :key="row.id" class="xhs-card" @click="openPreview(row)">
          <div class="xhs-card-media">
            <img :src="cardCover(row)" :alt="row.title" loading="lazy" @error="onImgError" />
            <span
              class="xhs-state-pill"
              :class="{
                'is-draft': row.state === '草稿',
                'is-live': row.state === '已发布',
              }"
            >
              {{ row.state }}
            </span>
          </div>
          <div class="xhs-card-body">
            <h3 class="xhs-title">{{ row.title }}</h3>
            <p class="xhs-desc">{{ contentPreview(row.content) }}</p>
            <div class="xhs-meta">
              <span class="xhs-chip">{{ row.topicName }}</span>
              <span v-if="row.location" class="xhs-loc">
                <el-icon><LocationFilled /></el-icon>
                {{ row.location }}
              </span>
              <span class="xhs-author">👤 {{ row.userName }}</span>
            </div>
            <div class="xhs-stats">
              <span
                ><el-icon><View /></el-icon> {{ row.viewsCount ?? 0 }}</span
              >
              <span>❤️ {{ row.likesCount ?? 0 }}</span>
              <span>⭐ {{ row.favoritesCount ?? 0 }}</span>
            </div>
            <div class="xhs-actions">
              <el-button
                :icon="Delete"
                circle
                class="action-btn delete-btn"
                @click.stop="removeNote(row)"
              />
            </div>
          </div>
        </article>
      </div>

      <div v-else class="custom-empty">
        <el-icon class="empty-icon"><PictureFilled /></el-icon>
        <p class="empty-text">还没有笔记</p>
        <p class="empty-hint">点击「发笔记」上传封面与正文，会像小红书一样展示在瀑布流里</p>
      </div>

      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[12, 24, 48]"
          layout="jumper, total, sizes, prev, pager, next"
          background
          :total="total"
          @size-change="onSizeChange"
          @current-change="onCurrentChange"
        />
      </div>
    </div>

    <div class="page-footer">
      <span class="footer-emoji">💜</span>
      <span>共 {{ total }} 篇笔记</span>
      <span class="footer-emoji">✨</span>
    </div>

    <!-- 笔记详情预览弹窗 -->
    <el-dialog
      v-model="previewVisible"
      :title="previewNote?.title || '笔记详情'"
      width="85%"
      top="3vh"
      class="note-preview-dialog"
      @close="closePreview"
    >
      <div v-if="previewNote" class="note-preview-content">
        <!-- 视频类型 -->
        <div v-if="getMediaType(previewNote) === 'video'" class="note-video-section">
          <div class="video-container">
            <video
              ref="videoPlayerRef"
              :src="previewNote.video"
              :poster="previewNote.coverImg || ''"
              controls
              autoplay
              preload="metadata"
              crossorigin="anonymous"
              playsinline
              class="preview-video-player"
              @error="handleVideoError"
              @loadedmetadata="handleVideoLoaded"
            >
              您的浏览器不支持视频播放
            </video>
          </div>
        </div>

        <!-- 图片类型 -->
        <div v-else-if="getMediaType(previewNote) === 'image'" class="note-images-section">
          <div class="image-gallery">
            <img
              :src="previewNote.images?.[currentImageIndex] || previewNote.coverImg"
              :alt="`图片${currentImageIndex + 1}`"
              class="preview-image"
              @error="onImgError"
            />

            <!-- 图片导航按钮 -->
            <div v-if="previewNote.images && previewNote.images.length > 1" class="image-nav">
              <button class="nav-btn prev-btn" @click="prevImage">
                <el-icon><ArrowLeft /></el-icon>
              </button>
              <button class="nav-btn next-btn" @click="nextImage">
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>

            <!-- 图片指示器 -->
            <div v-if="previewNote.images && previewNote.images.length > 1" class="image-indicator">
              {{ currentImageIndex + 1 }} / {{ previewNote.images.length }}
            </div>
          </div>
        </div>

        <!-- 未知类型或无媒体 -->
        <div v-else class="note-cover">
          <img
            :src="cardCover(previewNote)"
            :alt="previewNote.title"
            class="preview-image"
            @error="onImgError"
          />
        </div>

        <!-- 笔记基本信息 -->
        <div class="note-info">
          <h2 class="note-title">{{ previewNote.title }}</h2>
          <div class="note-meta">
            <div class="meta-item">
              <span class="meta-label">作者：</span>
              <span class="meta-value">{{ userNameOf(previewNote) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">话题：</span>
              <span class="meta-value">{{ topicNameOf(previewNote) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">状态：</span>
              <el-tag :type="previewNote.state === '草稿' ? 'info' : 'success'" size="small">
                {{ previewNote.state }}
              </el-tag>
            </div>
            <div class="meta-item" v-if="previewNote.location">
              <span class="meta-label">位置：</span>
              <span class="meta-value">
                <el-icon><LocationFilled /></el-icon>
                {{ previewNote.location }}
              </span>
            </div>
            <div class="meta-item">
              <span class="meta-label">发布时间：</span>
              <span class="meta-value">{{
                new Date(previewNote.createTime).toLocaleString('zh-CN')
              }}</span>
            </div>
            <div
              class="meta-item"
              v-if="getMediaType(previewNote) === 'image' && previewNote.images"
            >
              <span class="meta-label">图片数：</span>
              <span class="meta-value">{{ previewNote.images.length }} 张</span>
            </div>
          </div>
        </div>

        <!-- 互动数据 -->
        <div class="note-stats">
          <div class="stat-item">
            <el-icon><View /></el-icon>
            <span>{{ previewNote.viewsCount ?? 0 }} 浏览</span>
          </div>
          <div class="stat-item">
            <span>❤️ {{ previewNote.likesCount ?? 0 }} 点赞</span>
          </div>
          <div class="stat-item">
            <span>⭐ {{ previewNote.favoritesCount ?? 0 }} 收藏</span>
          </div>
        </div>

        <!-- 笔记内容 -->
        <div class="note-content" v-html="previewNote.content"></div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closePreview">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.article-manage-container {
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

.search-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 24px;
  padding: 20px 24px;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);

  .search-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    font-size: 14px;
    font-weight: 500;
    color: #c5a3ff;

    .search-emoji {
      font-size: 18px;
    }
  }

  .search-form {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    align-items: flex-end;

    :deep(.el-form-item__label) {
      color: #6a4a9c;
      font-weight: 500;
    }

    .search-btn {
      background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
      border: none;
      border-radius: 48px;
      padding: 8px 24px;
      color: white;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
      }
    }

    .reset-btn {
      background: linear-gradient(135deg, #a8e6cf 0%, #90e0c0 100%);
      border: none;
      border-radius: 48px;
      padding: 8px 24px;
      color: white;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(168, 230, 207, 0.3);
      }
    }
  }
}

.feed-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 24px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
}

.xhs-feed {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

.xhs-card {
  border-radius: 20px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease;
  cursor: pointer;
  border: 1px solid rgba(197, 163, 255, 0.1);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 28px rgba(197, 163, 255, 0.15);
    border-color: rgba(197, 163, 255, 0.2);
  }
}

.xhs-card-media {
  position: relative;
  aspect-ratio: 3 / 4;
  background: linear-gradient(145deg, #fff5f7, #f0f7ff);
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.xhs-state-pill {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 48px;
  font-size: 11px;
  font-weight: 600;
  backdrop-filter: blur(8px);

  &.is-live {
    background: linear-gradient(135deg, #a8e6cf, #7ee0b5);
    color: #2c665a;
  }

  &.is-draft {
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
    color: white;
  }
}

.xhs-card-body {
  padding: 12px 14px 14px;
}

.xhs-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #222;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.xhs-desc {
  margin: 0 0 10px;
  font-size: 12px;
  color: #888;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: calc(1.45em * 2);
}

.xhs-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.xhs-chip {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 48px;
  font-size: 11px;
  font-weight: 500;
  background: linear-gradient(135deg, rgba(197, 163, 255, 0.15), rgba(248, 180, 217, 0.15));
  color: #c5a3ff;
}

.xhs-loc {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: #999;

  .el-icon {
    font-size: 13px;
  }
}

.xhs-author {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: #6a4a9c;
  font-weight: 500;
}

.xhs-stats {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #666;
  margin-bottom: 10px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .el-icon {
    font-size: 14px;
    color: #aaa;
  }
}

.xhs-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;

  .action-btn {
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px) scale(1.05);
    }
  }

  .delete-btn {
    background: linear-gradient(135deg, #ff9e9e 0%, #ffbaba 100%);
    border-color: #ff9e9e;
    color: #8a1c1c;
  }
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.custom-empty {
  padding: 60px 20px;
  text-align: center;

  .empty-icon {
    font-size: 56px;
    color: #f0c8d4;
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
    max-width: 360px;
    margin-left: auto;
    margin-right: auto;
    line-height: 1.5;
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

:deep(.el-pagination) {
  .btn-prev,
  .btn-next,
  .el-pager li {
    border-radius: 12px !important;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(255, 36, 66, 0.08) !important;
    }
  }

  .el-pager li.is-active {
    background: linear-gradient(135deg, #ff2442, #ff6b8a) !important;
    color: white !important;
  }
}

// 笔记预览弹窗样式
:deep(.note-preview-dialog) {
  .el-dialog__body {
    padding: 20px;
    max-height: 80vh;
    overflow-y: auto;
  }

  .note-preview-content {
    .note-video-section {
      margin-bottom: 20px;

      .video-container {
        position: relative;
        width: 100%;
        background: #000;
        border-radius: 8px;
        overflow: hidden;

        .preview-video-player {
          width: 100%;
          max-height: 600px;
          display: block;
          object-fit: contain;
        }
      }
    }

    .note-images-section {
      margin-bottom: 20px;

      .image-gallery {
        position: relative;
        width: 100%;
        background: linear-gradient(145deg, #f5f7fa, #e8eef5);
        border-radius: 8px;
        overflow: hidden;
        min-height: 400px;
        display: flex;
        align-items: center;
        justify-content: center;

        .preview-image {
          max-width: 100%;
          max-height: 600px;
          object-fit: contain;
          display: block;
        }

        .image-nav {
          position: absolute;
          top: 50%;
          left: 0;
          right: 0;
          transform: translateY(-50%);
          display: flex;
          justify-content: space-between;
          padding: 0 16px;
          pointer-events: none;

          .nav-btn {
            width: 44px;
            height: 44px;
            border-radius: 50%;
            background: rgba(0, 0, 0, 0.6);
            border: 2px solid rgba(255, 255, 255, 0.3);
            color: white;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s ease;
            pointer-events: auto;
            backdrop-filter: blur(8px);

            &:hover {
              background: rgba(0, 0, 0, 0.8);
              transform: scale(1.1);
              border-color: rgba(255, 255, 255, 0.6);
            }

            .el-icon {
              font-size: 20px;
            }
          }
        }

        .image-indicator {
          position: absolute;
          bottom: 16px;
          left: 50%;
          transform: translateX(-50%);
          background: rgba(0, 0, 0, 0.7);
          color: white;
          padding: 6px 16px;
          border-radius: 20px;
          font-size: 13px;
          font-weight: 500;
          backdrop-filter: blur(8px);
        }
      }
    }

    .note-cover {
      margin-bottom: 20px;
      text-align: center;

      .preview-image {
        max-width: 100%;
        max-height: 400px;
        object-fit: contain;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }
    }

    .note-info {
      margin-bottom: 20px;

      .note-title {
        font-size: 24px;
        font-weight: 600;
        margin: 0 0 15px;
        color: #333;
        line-height: 1.3;
      }

      .note-meta {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 10px;

        .meta-item {
          display: flex;
          font-size: 14px;
          color: #666;

          .meta-label {
            font-weight: 500;
            min-width: 60px;
          }

          .meta-value {
            flex: 1;
            color: #333;

            .el-icon {
              margin-right: 4px;
              vertical-align: middle;
            }
          }
        }
      }
    }

    .note-stats {
      display: flex;
      gap: 20px;
      margin-bottom: 20px;
      padding: 15px;
      background: #f8f9fa;
      border-radius: 8px;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 5px;
        font-weight: 500;
        color: #555;
      }
    }

    .note-content {
      padding: 20px;
      background: #fff;
      border-radius: 8px;
      box-shadow: inset 0 0 8px rgba(0, 0, 0, 0.05);
      line-height: 1.6;
      color: #444;

      :deep(*) {
        max-width: 100%;
      }

      :deep(h1),
      :deep(h2),
      :deep(h3),
      :deep(h4),
      :deep(h5),
      :deep(h6) {
        margin: 1.2em 0 0.8em;
        font-weight: 600;
        color: #222;
      }

      :deep(p) {
        margin: 0.8em 0;
      }

      :deep(img) {
        max-width: 100%;
        height: auto;
        border-radius: 4px;
        margin: 10px 0;
      }

      :deep(ul),
      :deep(ol) {
        padding-left: 20px;
        margin: 0.8em 0;
      }

      :deep(li) {
        margin: 0.4em 0;
      }

      :deep(blockquote) {
        margin: 1em 0;
        padding: 10px 15px;
        border-left: 4px solid #e0e0e0;
        background: #f9f9f9;
        color: #666;
      }

      :deep(code) {
        padding: 2px 6px;
        background: #f1f1f1;
        border-radius: 3px;
        font-family: monospace;
        font-size: 0.9em;
      }

      :deep(pre) {
        padding: 12px;
        background: #f8f8f8;
        border-radius: 4px;
        overflow-x: auto;
        margin: 1em 0;
      }
    }
  }
}
</style>

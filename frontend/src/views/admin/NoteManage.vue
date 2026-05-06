<script setup>
import { computed, ref } from 'vue'
import { Delete, Edit, LocationFilled, PictureFilled, Plus, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'

import {
  noteAddService,
  noteDeleteService,
  noteDetailService,
  notePageListService,
  noteUpdateService,
  topicListService,
} from '@/api/note.js'
import { useTokenStore } from '@/stores/token.js'

const topics = ref([])
const topicId = ref('')
const state = ref('')

const notes = ref([])
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(12)

const PLACEHOLDER_IMG =
  'data:image/svg+xml,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="533" viewBox="0 0 400 533"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#ffeef8"/><stop offset="100%" stop-color="#e8f4ff"/></linearGradient></defs><rect width="400" height="533" fill="url(#g)"/><text x="200" y="270" text-anchor="middle" fill="#b8b0c8" font-family="system-ui" font-size="14">暂无封面</text></svg>`,
  )

const tokenStore = useTokenStore()

const visibleDrawer = ref(false)
const isEdit = ref(false)
const editingId = ref(null)

const defaultNote = () => ({
  title: '',
  topicId: '',
  coverImg: '',
  content: '',
  location: '',
  state: '',
  images: [],
})

const noteModel = ref(defaultNote())

const drawerKey = computed(() => (editingId.value != null ? `edit-${editingId.value}` : 'create'))

const cardCover = (row) => {
  if (row.coverImg) return row.coverImg
  if (Array.isArray(row.images) && row.images.length > 0) return row.images[0]
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

const topicNameOf = (note) => {
  const t = topics.value.find((x) => x.id === note.topicId)
  return t ? t.topicName : '未分类'
}

const getTopicList = async () => {
  const result = await topicListService()
  topics.value = result.data || []
}

const noteList = async () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  }
  if (topicId.value !== '' && topicId.value != null) params.topicId = topicId.value
  if (state.value) params.state = state.value

  const result = await notePageListService(params)
  total.value = result.data.total
  notes.value = result.data.items || []
  for (const note of notes.value) {
    note.topicName = topicNameOf(note)
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
  pageNum.value = 1
  noteList()
}

const uploadSuccess = (result) => {
  noteModel.value.coverImg = result.data
}

const openCreateDrawer = () => {
  isEdit.value = false
  editingId.value = null
  noteModel.value = defaultNote()
  visibleDrawer.value = true
}

const openEditDrawer = async (row) => {
  try {
    const result = await noteDetailService(row.id)
    isEdit.value = true
    editingId.value = row.id
    const data = result.data || {}
    noteModel.value = {
      title: data.title ?? '',
      topicId: data.topicId ?? '',
      coverImg: data.coverImg ?? '',
      content: data.content ?? '',
      location: data.location ?? '',
      state: data.state ?? '',
      images: Array.isArray(data.images) ? [...data.images] : [],
      id: data.id,
      publishTime: data.publishTime,
    }
    visibleDrawer.value = true
  } catch {
    ElMessage.error('加载笔记失败')
  }
}

const submitNote = async (clickState) => {
  noteModel.value.state = clickState
  try {
    if (isEdit.value) {
      await noteUpdateService(noteModel.value)
      ElMessage.success('保存成功')
    } else {
      await noteAddService(noteModel.value)
      ElMessage.success('添加成功')
    }
    visibleDrawer.value = false
    noteList()
  } catch {
    ElMessage.error(isEdit.value ? '保存失败' : '添加失败')
  }
}

const removeNote = (row) => {
  ElMessageBox.confirm('确定删除这篇笔记吗？删除后无法恢复。', '删除笔记', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
    .then(async () => {
      await noteDeleteService(row.id)
      ElMessage.success('已删除')
      noteList()
    })
    .catch(() => {})
}

getTopicList()
noteList()
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
        <p class="page-subtitle">瀑布流浏览 · 封面标题 · 互动数据，风格类似「小红书」笔记流</p>
      </div>
      <div class="header-extra">
        <el-button class="add-btn" type="primary" size="large" @click="openCreateDrawer">
          <span class="btn-text">+ 发笔记</span>
        </el-button>
      </div>
    </div>

    <div class="search-card">
      <div class="search-title">
        <span class="search-emoji">🔍</span>
        <span>筛选</span>
      </div>
      <el-form inline class="search-form">
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
          <el-select v-model="state" placeholder="全部状态" clearable class="custom-select" style="width: 160px">
            <el-option label="已发布" value="已发布" />
            <el-option label="草稿" value="草稿" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button class="search-btn" @click="() => { pageNum = 1; noteList() }">搜索</el-button>
          <el-button class="reset-btn" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="feed-card">
      <div v-if="notes.length" class="xhs-feed">
        <article v-for="row in notes" :key="row.id" class="xhs-card">
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
            </div>
            <div class="xhs-stats">
              <span><el-icon><View /></el-icon> {{ row.viewsCount ?? 0 }}</span>
              <span>❤️ {{ row.likesCount ?? 0 }}</span>
              <span>⭐ {{ row.favoritesCount ?? 0 }}</span>
            </div>
            <div class="xhs-actions">
              <el-button :icon="Edit" circle class="action-btn edit-btn" @click="openEditDrawer(row)" />
              <el-button :icon="Delete" circle class="action-btn delete-btn" @click="removeNote(row)" />
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

    <el-drawer v-model="visibleDrawer" :title="isEdit ? '编辑笔记' : '发笔记'" direction="rtl" size="50%" class="custom-drawer">
      <div class="drawer-content">
        <el-form :key="drawerKey" :model="noteModel" label-width="100px" class="drawer-form">
          <el-form-item label="标题">
            <el-input v-model="noteModel.title" placeholder="简短标题，会显示在卡片上" class="custom-input" />
          </el-form-item>

          <el-form-item label="话题">
            <el-select v-model="noteModel.topicId" placeholder="选择话题" class="custom-select">
              <el-option v-for="t in topics" :key="t.id" :label="t.topicName" :value="t.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="位置">
            <el-input v-model="noteModel.location" placeholder="可选，显示为地点标签" class="custom-input" />
          </el-form-item>

          <el-form-item label="封面图">
            <el-upload
              class="avatar-uploader"
              :auto-upload="true"
              :show-file-list="false"
              action="/api/upload"
              name="file"
              :headers="{ Authorization: tokenStore.token }"
              :on-success="uploadSuccess"
            >
              <img v-if="noteModel.coverImg" :src="noteModel.coverImg" class="cover-preview" />
              <div v-else class="upload-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <span>上传封面（竖图更接近小红书）</span>
              </div>
            </el-upload>
          </el-form-item>

          <el-form-item label="正文">
            <div class="editor-wrapper">
              <quill-editor theme="snow" v-model:content="noteModel.content" contentType="html" />
            </div>
          </el-form-item>

          <el-form-item>
            <div class="form-actions">
              <el-button class="publish-btn" type="primary" @click="submitNote('已发布')">发布</el-button>
              <el-button class="draft-btn" @click="submitNote('草稿')">存草稿</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </el-drawer>
  </div>
</template>

<style lang="scss" scoped>
.article-manage-container {
  min-height: calc(100vh - 120px);
  padding: 20px;
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

  .add-btn {
    background: linear-gradient(135deg, #ff2442 0%, #ff6b8a 100%);
    border: none;
    border-radius: 48px;
    padding: 12px 32px;
    font-weight: 500;
    color: white;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(255, 36, 66, 0.25);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(255, 36, 66, 0.35);
    }

    .btn-text {
      font-size: 14px;
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
    color: #ff6b8a;

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
      background: linear-gradient(135deg, #ff2442 0%, #ff6b8a 100%);
      border: none;
      border-radius: 48px;
      padding: 8px 24px;
      color: white;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(255, 36, 66, 0.3);
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
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.06);
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 28px rgba(255, 36, 66, 0.12);
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
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  backdrop-filter: blur(8px);

  &.is-live {
    background: rgba(255, 255, 255, 0.92);
    color: #ff2442;
  }

  &.is-draft {
    background: rgba(0, 0, 0, 0.45);
    color: #fff;
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
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
  background: linear-gradient(135deg, #ffeef1, #fff0f3);
  color: #ff2442;
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

  .edit-btn {
    background: linear-gradient(135deg, #a8e6cf 0%, #c2f5e9 100%);
    border-color: #a8e6cf;
    color: #2c665a;
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

.custom-drawer {
  :deep(.el-drawer__header) {
    background: linear-gradient(135deg, #ff6b8a 0%, #ff2442 100%);
    color: white;
    margin-bottom: 0;
    padding: 20px 24px;

    .el-drawer__title {
      font-weight: 600;
    }

    .el-drawer__close-btn {
      color: white;
    }
  }

  :deep(.el-drawer__body) {
    padding: 24px;
    background: linear-gradient(145deg, #ffffff 0%, #fffafb 100%);
  }
}

.drawer-content {
  .drawer-form {
    .custom-input {
      :deep(.el-input__wrapper) {
        border-radius: 48px;
        padding: 8px 20px;
        background-color: #fff8fa;
        border: 1px solid #ffe0e8;
        transition: all 0.3s ease;
        box-shadow: none;

        &:hover {
          border-color: #ffb8c8;
          background-color: #fff;
        }

        &.is-focus {
          border-color: #ff2442;
          background-color: #fff;
          box-shadow: 0 0 0 4px rgba(255, 36, 66, 0.1);
        }
      }
    }

    .custom-select {
      width: 100%;
    }

    .avatar-uploader {
      .cover-preview {
        width: 200px;
        height: 267px;
        object-fit: cover;
        border-radius: 16px;
        box-shadow: 0 4px 12px rgba(255, 36, 66, 0.15);
      }

      .upload-placeholder {
        width: 200px;
        height: 267px;
        background: linear-gradient(135deg, #fff8fa 0%, #ffeef2 100%);
        border: 2px dashed #ffb8c8;
        border-radius: 16px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 8px;
        cursor: pointer;
        transition: all 0.3s ease;

        &:hover {
          border-color: #ff2442;
          background: linear-gradient(135deg, #fff 0%, #fff5f7 100%);
        }

        .upload-icon {
          font-size: 24px;
          color: #ff6b8a;
        }

        span {
          font-size: 12px;
          color: #999;
          text-align: center;
          padding: 0 8px;
        }
      }
    }

    .editor-wrapper {
      width: 100%;

      :deep(.ql-editor) {
        min-height: 200px;
        border-radius: 12px;
      }

      :deep(.ql-toolbar) {
        border-radius: 12px 12px 0 0;
        border-color: #ffe0e8;
      }

      :deep(.ql-container) {
        border-radius: 0 0 12px 12px;
        border-color: #ffe0e8;
      }
    }

    .form-actions {
      display: flex;
      gap: 16px;

      .publish-btn {
        background: linear-gradient(135deg, #ff2442 0%, #ff6b8a 100%);
        border: none;
        border-radius: 48px;
        padding: 10px 32px;
        transition: all 0.3s ease;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(255, 36, 66, 0.35);
        }
      }

      .draft-btn {
        background: linear-gradient(135deg, #a8e6cf 0%, #c2f5e9 100%);
        border: none;
        border-radius: 48px;
        padding: 10px 32px;
        transition: all 0.3s ease;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(168, 230, 207, 0.4);
        }
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
</style>

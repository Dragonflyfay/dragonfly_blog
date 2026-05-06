<template>
  <div class="publish-container">
    <div class="publish-card">
      <!-- 头部 -->
      <div class="publish-header">
        <div class="header-decoration">
          <span class="deco-dot"></span>
          <span class="deco-dot"></span>
          <span class="deco-dot"></span>
        </div>
        <h1 class="publish-title">📝 发布新笔记</h1>
        <p class="publish-subtitle">分享你的精彩瞬间，让更多人看见 ✨</p>
      </div>

      <!-- 主要内容区 -->
      <div class="publish-content">
        <!-- 媒体类型选择 -->
        <div class="media-type-selector">
          <div
            class="media-type-item"
            :class="{ active: mediaType === 'image' }"
            @click="mediaType = 'image'"
          >
            <div class="type-icon">🖼️</div>
            <span>图文笔记</span>
          </div>
          <div
            class="media-type-item"
            :class="{ active: mediaType === 'video' }"
            @click="mediaType = 'video'"
          >
            <div class="type-icon">🎬</div>
            <span>视频笔记</span>
          </div>
        </div>

        <!-- 图文笔记上传区域 -->
        <div v-if="mediaType === 'image'" class="image-upload-section">
          <div class="section-title">
            <span class="title-icon">📸</span>
            <span>精选图片</span>
            <span class="title-count">{{ images.length }}/9</span>
          </div>

          <div class="image-grid">
            <div
              v-for="(img, index) in images"
              :key="index"
              class="image-item"
              :style="{ backgroundImage: `url(${img})` }"
            >
              <div class="image-overlay">
                <el-button circle size="small" class="delete-img-btn" @click="removeImage(index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>

            <div v-if="images.length < 9" class="upload-trigger" @click="triggerImageUpload">
              <div class="upload-icon">+</div>
              <span>上传图片</span>
              <input
                ref="imageInput"
                type="file"
                accept="image/*"
                multiple
                style="display: none"
                @change="handleImageUpload"
              />
            </div>
          </div>
          <p class="upload-hint">最多可上传9张图片，支持jpg/png格式</p>
        </div>

        <!-- 视频笔记上传区域 -->
        <div v-if="mediaType === 'video'" class="video-upload-section">
          <div class="section-title">
            <span class="title-icon">🎥</span>
            <span>上传视频</span>
          </div>

          <div v-if="!videoUrl" class="video-upload-trigger" @click="triggerVideoUpload">
            <div class="video-upload-icon">
              <el-icon :size="48"><VideoCamera /></el-icon>
            </div>
            <span>点击上传视频</span>
            <span class="video-hint">支持 mp4、mov 格式，大小不超过 500MB</span>
            <input
              ref="videoInput"
              type="file"
              accept="video/*"
              style="display: none"
              @change="handleVideoUpload"
            />
          </div>

          <div v-else class="video-preview">
            <video ref="videoPreview" :src="videoUrl" controls class="preview-video"></video>
            <div class="video-actions">
              <el-button type="danger" size="small" @click="removeVideo">
                <el-icon><Delete /></el-icon> 重新上传
              </el-button>
              <div class="video-cover-section">
                <span>视频封面：</span>
                <div class="cover-upload" @click="triggerCoverUpload">
                  <img v-if="videoCover" :src="videoCover" class="cover-preview" />
                  <div v-else class="cover-placeholder">
                    <el-icon><Plus /></el-icon>
                    <span>上传封面</span>
                  </div>
                </div>
                <input
                  ref="coverInput"
                  type="file"
                  accept="image/*"
                  style="display: none"
                  @change="handleCoverUpload"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 笔记信息表单 -->
        <el-form :model="noteForm" :rules="rules" ref="formRef" class="publish-form">
          <el-form-item prop="title">
            <el-input
              v-model="noteForm.title"
              placeholder="标题：一句话概括你的笔记内容..."
              maxlength="50"
              show-word-limit
              class="title-input"
            />
          </el-form-item>

          <el-form-item prop="content">
            <div class="editor-wrapper">
              <quill-editor
                theme="snow"
                v-model:content="noteForm.content"
                contentType="html"
                placeholder="正文：详细描述你的笔记内容..."
              />
            </div>
          </el-form-item>

          <div class="form-row">
            <el-form-item prop="topicId" class="half-item">
              <el-select v-model="noteForm.topicId" placeholder="选择话题" class="topic-select">
                <el-option v-for="t in topics" :key="t.id" :label="t.topicName" :value="t.id" />
              </el-select>
            </el-form-item>

            <el-form-item class="half-item">
              <el-input v-model="noteForm.location" placeholder="添加地点">
                <template #prefix>
                  <el-icon><LocationFilled /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>
        </el-form>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button class="draft-btn" size="large" @click="saveAsDraft">
            <el-icon><Document /></el-icon> 存草稿
          </el-button>
          <el-button class="publish-btn" type="primary" size="large" @click="publishNote">
            <el-icon><Promotion /></el-icon> 发布笔记
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Delete,
  LocationFilled,
  VideoCamera,
  Plus,
  Document,
  Promotion,
} from '@element-plus/icons-vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { useRouter } from 'vue-router'
import { useTokenStore } from '@/stores/token.js'
import { topicListService, noteAddService } from '@/api/note.js'

const router = useRouter()
const tokenStore = useTokenStore()
const formRef = ref()

const mediaType = ref('image') // image 或 video

// 图片相关
const images = ref([])
const imageInput = ref()

// 视频相关
const videoUrl = ref('')
const videoCover = ref('')
const videoInput = ref()
const coverInput = ref()
const videoPreview = ref()

// 话题列表
const topics = ref([])

// 表单数据
const noteForm = reactive({
  title: '',
  content: '',
  topicId: '',
  location: '',
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 1, max: 50, message: '标题长度1-50字符', trigger: 'blur' },
  ],
  content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }],
  topicId: [{ required: true, message: '请选择话题', trigger: 'change' }],
}

// 获取话题列表
const getTopicList = async () => {
  try {
    const result = await topicListService()
    topics.value = result.data || []
  } catch (error) {
    console.error('获取话题失败', error)
  }
}

// 触发图片上传
const triggerImageUpload = () => {
  imageInput.value?.click()
}

// 处理图片上传
const handleImageUpload = async (event) => {
  const files = Array.from(event.target.files)
  const remainingSlots = 9 - images.value.length

  if (files.length > remainingSlots) {
    ElMessage.warning(`最多只能上传${remainingSlots}张图片`)
    return
  }

  for (const file of files) {
    if (!file.type.startsWith('image/')) {
      ElMessage.warning('请上传图片文件')
      continue
    }

    const formData = new FormData()
    formData.append('file', file)

    try {
      const response = await fetch('/api/upload', {
        method: 'POST',
        headers: {
          Authorization: tokenStore.token,
        },
        body: formData,
      })
      const result = await response.json()
      if (result.code === 0) {
        images.value.push(result.data)
      }
    } catch (error) {
      ElMessage.error('上传失败')
    }
  }
  event.target.value = ''
}

// 删除图片
const removeImage = (index) => {
  images.value.splice(index, 1)
}

// 触发视频上传
const triggerVideoUpload = () => {
  videoInput.value?.click()
}

// 处理视频上传
const handleVideoUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('video/')) {
    ElMessage.warning('请上传视频文件')
    return
  }

  if (file.size > 500 * 1024 * 1024) {
    ElMessage.warning('视频大小不能超过500MB')
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await fetch('/api/upload', {
      method: 'POST',
      headers: {
        Authorization: tokenStore.token,
      },
      body: formData,
    })
    const result = await response.json()
    if (result.code === 0) {
      videoUrl.value = result.data
      ElMessage.success('视频上传成功')
    }
  } catch (error) {
    ElMessage.error('视频上传失败')
  }
  event.target.value = ''
}

// 移除视频
const removeVideo = () => {
  videoUrl.value = ''
  videoCover.value = ''
}

// 触发封面上传
const triggerCoverUpload = () => {
  coverInput.value?.click()
}

// 处理封面上传
const handleCoverUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await fetch('/api/upload', {
      method: 'POST',
      headers: {
        Authorization: tokenStore.token,
      },
      body: formData,
    })
    const result = await response.json()
    if (result.code === 0) {
      videoCover.value = result.data
      ElMessage.success('封面上传成功')
    }
  } catch (error) {
    ElMessage.error('封面上传失败')
  }
  event.target.value = ''
}

// 保存草稿
const saveAsDraft = async () => {
  if (!noteForm.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  await submitNote('草稿')
}

// 发布笔记
const publishNote = async () => {
  await formRef.value?.validate()
  await submitNote('已发布')
}

// 提交笔记
const submitNote = async (state) => {
  const requestData = {
    title: noteForm.title,
    content: noteForm.content,
    topicId: noteForm.topicId,
    location: noteForm.location,
    state: state,
    mediaType: mediaType.value,
  }

  if (mediaType.value === 'image') {
    if (images.value.length === 0) {
      ElMessage.warning('请至少上传一张图片')
      return
    }
    requestData.images = images.value
    requestData.coverImg = images.value[0]
  } else {
    if (!videoUrl.value) {
      ElMessage.warning('请上传视频')
      return
    }
    requestData.videoUrl = videoUrl.value
    requestData.videoCover = videoCover.value
    requestData.coverImg = videoCover.value
  }

  try {
    const result = await noteAddService(requestData)
    ElMessage.success(state === '已发布' ? '发布成功！' : '已保存到草稿箱')
    // 触发自定义事件，通知其他组件刷新话题列表
    window.dispatchEvent(new CustomEvent('notesChanged'))
    router.push('/home')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  if (!tokenStore.token) {
    router.push('/login')
    return
  }
  getTopicList()
})
</script>

<style lang="scss" scoped>
.publish-container {
  min-height: calc(100vh - 120px);
  padding: 20px;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
}

.publish-card {
  max-width: 900px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 32px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.publish-header {
  padding: 32px 40px;
  background: linear-gradient(135deg, rgba(248, 180, 217, 0.3) 0%, rgba(197, 163, 255, 0.3) 100%);
  border-bottom: 2px solid rgba(197, 163, 255, 0.15);
  position: relative;

  .header-decoration {
    position: absolute;
    top: 20px;
    left: 30px;
    display: flex;
    gap: 8px;

    .deco-dot {
      width: 6px;
      height: 6px;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      border-radius: 50%;

      &:nth-child(2) {
        width: 8px;
        height: 8px;
        opacity: 0.6;
      }
      &:nth-child(3) {
        width: 10px;
        height: 10px;
        opacity: 0.3;
      }
    }
  }

  .publish-title {
    font-size: 28px;
    font-weight: 700;
    margin: 0 0 8px;
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }

  .publish-subtitle {
    font-size: 13px;
    color: #a09abf;
    margin: 0;
  }
}

.publish-content {
  padding: 32px 40px;
}

.media-type-selector {
  display: flex;
  gap: 16px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(197, 163, 255, 0.15);

  .media-type-item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 16px;
    background: linear-gradient(135deg, #faf7ff 0%, #fff 100%);
    border-radius: 24px;
    cursor: pointer;
    transition: all 0.3s ease;
    border: 2px solid transparent;

    .type-icon {
      font-size: 28px;
    }

    span {
      font-size: 16px;
      font-weight: 500;
      color: #a09abf;
    }

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(197, 163, 255, 0.2);
    }

    &.active {
      border-color: #c5a3ff;
      background: linear-gradient(135deg, #fff 0%, #f8f3ff 100%);

      span {
        color: #c5a3ff;
      }
    }
  }
}

.image-upload-section,
.video-upload-section {
  margin-bottom: 32px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    font-size: 16px;
    font-weight: 600;
    color: #6a4a9c;

    .title-icon {
      font-size: 20px;
    }
    .title-count {
      margin-left: auto;
      font-size: 13px;
      color: #b0a7c0;
    }
  }
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;

  .image-item {
    aspect-ratio: 1;
    background-size: cover;
    background-position: center;
    border-radius: 16px;
    position: relative;
    overflow: hidden;

    .image-overlay {
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.3s ease;

      .delete-img-btn {
        background: rgba(255, 255, 255, 0.9);
        color: #ff4757;

        &:hover {
          background: #ff4757;
          color: white;
        }
      }
    }

    &:hover .image-overlay {
      opacity: 1;
    }
  }

  .upload-trigger {
    aspect-ratio: 1;
    background: linear-gradient(135deg, #faf7ff 0%, #f0e5ff 100%);
    border: 2px dashed #c5a3ff;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    cursor: pointer;
    transition: all 0.3s ease;

    .upload-icon {
      font-size: 32px;
      color: #c5a3ff;
    }

    span {
      font-size: 12px;
      color: #a09abf;
    }

    &:hover {
      background: linear-gradient(135deg, #fff 0%, #f8f3ff 100%);
      transform: translateY(-2px);
    }
  }
}

.upload-hint {
  font-size: 12px;
  color: #b0a7c0;
  margin-top: 12px;
  text-align: center;
}

.video-upload-trigger {
  background: linear-gradient(135deg, #faf7ff 0%, #f0e5ff 100%);
  border: 2px dashed #c5a3ff;
  border-radius: 24px;
  padding: 48px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;

  .video-upload-icon {
    margin-bottom: 16px;
    color: #c5a3ff;
  }
  span {
    display: block;
    font-size: 16px;
    color: #6a4a9c;
    margin-bottom: 8px;
  }
  .video-hint {
    font-size: 12px;
    color: #b0a7c0;
  }

  &:hover {
    background: linear-gradient(135deg, #fff 0%, #f8f3ff 100%);
    transform: translateY(-2px);
  }
}

.video-preview {
  .preview-video {
    width: 100%;
    border-radius: 16px;
    max-height: 400px;
  }

  .video-actions {
    margin-top: 16px;
    display: flex;
    gap: 16px;
    align-items: center;
    flex-wrap: wrap;
  }

  .video-cover-section {
    display: flex;
    align-items: center;
    gap: 12px;

    .cover-upload {
      cursor: pointer;

      .cover-preview {
        width: 80px;
        height: 80px;
        object-fit: cover;
        border-radius: 12px;
      }

      .cover-placeholder {
        width: 80px;
        height: 80px;
        background: linear-gradient(135deg, #faf7ff 0%, #f0e5ff 100%);
        border: 2px dashed #c5a3ff;
        border-radius: 12px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 4px;

        .el-icon {
          font-size: 24px;
          color: #c5a3ff;
        }
        span {
          font-size: 10px;
          color: #a09abf;
        }
      }
    }
  }
}

.publish-form {
  .title-input {
    :deep(.el-input__wrapper) {
      border-radius: 48px;
      padding: 12px 20px;
      background: #faf7ff;
      border: 1px solid #f0e5ff;

      &:hover {
        border-color: #d9b8ff;
      }
      &.is-focus {
        border-color: #c5a3ff;
        box-shadow: 0 0 0 4px rgba(197, 163, 255, 0.12);
      }
    }
  }

  .editor-wrapper {
    :deep(.ql-editor) {
      min-height: 200px;
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

  .form-row {
    display: flex;
    gap: 16px;

    .half-item {
      flex: 1;
    }
  }

  .topic-select {
    width: 100%;

    :deep(.el-input__wrapper) {
      border-radius: 48px;
      background: #faf7ff;
      border-color: #f0e5ff;
    }
  }
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(197, 163, 255, 0.15);

  .draft-btn {
    flex: 1;
    background: linear-gradient(135deg, #e0d4ff 0%, #f0e5ff 100%);
    border: none;
    border-radius: 48px;
    padding: 14px;
    color: #7a5a9e;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(197, 163, 255, 0.2);
    }
  }

  .publish-btn {
    flex: 1;
    background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
    border: none;
    border-radius: 48px;
    padding: 14px;
    color: white;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
    }
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}
//富文本框的格式美化
</style>

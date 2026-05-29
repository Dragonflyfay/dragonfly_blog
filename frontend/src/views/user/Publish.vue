<script setup>
import { ref, reactive, onMounted, shallowRef, onBeforeUnmount,nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Delete,
  LocationFilled,
  VideoCamera,
  Plus,
  Document,
  Promotion,
  Check,
} from '@element-plus/icons-vue'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { useRouter } from 'vue-router'
import { useTokenStore } from '@/stores/token.js'
import { topicListService, noteAddService } from '@/api/note.js'
import request from '@/utils/request.js'
import axios from 'axios'

const router = useRouter()
const tokenStore = useTokenStore()
const formRef = ref()
const submitting = ref(false)

const mediaType = ref('image') // image 或 video

// 图片相关
const images = ref([])
const imageInput = ref()
const selectedCoverIndex = ref(0)

// 视频相关
const videoUrl = ref('')
const videoCover = ref('')
const videoInput = ref()
const coverInput = ref()
const videoPreview = ref()
// 新增：用于从视频中提取封面
const canvasRef = ref(null)
// 新增：用于保存视频封面
const isVideoTranscoding = ref(false)
// 视频上传进度
const videoUploadProgress = ref(0)
const isUploadingVideo = ref(false)

// 话题列表
const topics = ref([])

// 表单数据
const noteForm = reactive({
  title: '',
  content: '',
  topicId: '',
  location: '',
})

// 富文本编辑器相关
const editorRef = shallowRef()
const toolbarConfig = {}
const editorConfig = {
  placeholder: '正文：详细描述你的笔记内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/upload',
      fieldName: 'file',
      customInsert(res, insertFn) {
        if (res.code === 0) {
          insertFn(res.data, '', res.data)
        }
      },
    },
  },
}

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 1, max: 50, message: '标题长度1-50字符', trigger: 'blur' },
  ],
  content: [
    {
      validator: (rule, value, callback) => {
        if (!value || value === '<p><br></p>' || value.trim() === '') {
          callback(new Error('请输入正文内容'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  topicId: [{ required: true, message: '请选择话题', trigger: 'change' }],
}

// 获取话题列表
const getTopicList = async () => {
  try {
    const result = await topicListService()
    topics.value = result.data || []
  } catch (error) {
    console.error('获取话题失败', error)
    ElMessage.error('获取话题列表失败')
  }
}

// 切换媒体类型
const switchMediaType = (type) => {
  if (mediaType.value === type) return

  if (type === 'video' && images.value.length > 0) {
    ElMessage.warning('切换到视频模式将清空已上传的图片')
    images.value = []
  } else if (type === 'image' && videoUrl.value) {
    ElMessage.warning('切换到图文模式将清空已上传的视频')
    videoUrl.value = ''
    videoCover.value = ''
  }

  mediaType.value = type
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

  // 过滤有效的图片文件
  const validFiles = []
  for (const file of files) {
    if (!file.type.startsWith('image/')) {
      ElMessage.warning('请上传图片文件')
      continue
    }

    if (file.size > 10 * 1024 * 1024) {
      ElMessage.warning(`图片 ${file.name} 超过10MB限制`)
      continue
    }

    validFiles.push(file)
  }

  if (validFiles.length === 0) {
    event.target.value = ''
    return
  }

  // 显示上传进度信息
  ElMessage.info(`正在上传 ${validFiles.length} 张图片...`)

  const uploadPromises = validFiles.map((file) => {
    const formData = new FormData()
    formData.append('file', file)

    return request.post('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  })

  try {
    const results = await Promise.all(uploadPromises.map((p) => p.catch((e) => e)))

    let successCount = 0
    for (let i = 0; i < results.length; i++) {
      const result = results[i]

      if (result instanceof Error) {
        console.error('上传失败:', result)
        continue
      }

      if (result.code === 0) {
        images.value.push(result.data)
        successCount++
      } else {
        console.error(`图片 ${validFiles[i].name} 上传失败:`, result.message)
      }
    }

    if (successCount > 0) {
      ElMessage.success(`成功上传了 ${successCount} 张图片`)
    } else {
      ElMessage.warning('所有图片上传都失败了')
    }
  } catch (error) {
    console.error('批量上传失败:', error)
    ElMessage.error('批量上传失败')
  }

  event.target.value = ''
}

// 删除图片
const removeImage = (index) => {
  images.value.splice(index, 1)
  // 如果删除的是封面，重置封面索引
  if (index === selectedCoverIndex.value) {
    selectedCoverIndex.value = 0
  } else if (index < selectedCoverIndex.value) {
    selectedCoverIndex.value--
  }
}

// 选择封面
const selectCover = (index) => {
  selectedCoverIndex.value = index
}

// 触发视频上传
const triggerVideoUpload = () => {
  videoInput.value?.click()
}

// 处理视频上传
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

  videoUploadProgress.value = 0
  isUploadingVideo.value = true

  try {
    const uploadInstance = axios.create({
      baseURL: '/api',
      timeout: 300000,
    })

    uploadInstance.interceptors.request.use(
        (config) => {
          const tokenStore = useTokenStore()
          let token = tokenStore.token
          if (!token) {
            token = localStorage.getItem('global_token')
            if (!token) {
              const tabId = sessionStorage.getItem('tab_id')
              if (tabId) {
                token = sessionStorage.getItem(`token_${tabId}`)
              }
            }
          }
          if (token) {
            config.headers.Authorization = token
          }
          return config
        },
        (err) => Promise.reject(err),
    )

    uploadInstance.interceptors.response.use(
        (result) => {
          if (result.data.code === 0) {
            return result.data
          }
          ElMessage.error(result.data.message || '服务异常')
          return Promise.reject(result.data)
        },
        (err) => {
          if (err.code === 'ECONNABORTED') {
            ElMessage.error('上传超时，请检查网络连接或尝试上传较小的视频')
          } else if (!err.response) {
            ElMessage.error('网络异常，请检查网络连接')
          } else {
            ElMessage.error(err.response?.data?.message || '上传失败')
          }
          return Promise.reject(err)
        },
    )

    const result = await uploadInstance.post('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        videoUploadProgress.value = percentCompleted
        console.log(`上传进度: ${percentCompleted}%`)
      },
    })

    if (result.code === 0) {
      videoUrl.value = result.data
      isVideoTranscoding.value = true
      ElMessage.info('视频上传成功，正在转码中（约1-2分钟）...')

      // 轮询检查视频是否可播放
      let retryCount = 0
      const maxRetries = 40
      const checkInterval = setInterval(() => {
        if (!videoUrl.value) {
          clearInterval(checkInterval)
          return
        }

        const testVideo = document.createElement('video')
        testVideo.src = videoUrl.value + '?t=' + Date.now()

        testVideo.onloadedmetadata = () => {
          clearInterval(checkInterval)
          isVideoTranscoding.value = false
          ElMessage.success('视频转码完成，可以预览了')
          if (videoPreview.value) {
            videoPreview.value.load()
          }
        }

        testVideo.onerror = () => {
          retryCount++
          console.log(`等待转码... 第${retryCount}次检查`)
          if (retryCount >= maxRetries) {
            clearInterval(checkInterval)
            isVideoTranscoding.value = false
            ElMessage.warning('视频转码超时，请稍后刷新页面重试')
          }
        }
      }, 3000)
    }
  } catch (error) {
    console.error('视频上传失败:', error)
    if (error.code !== 'ECONNABORTED' && error.response) {
      ElMessage.error(error.response?.data?.message || '视频上传失败')
    }
  } finally {
    isUploadingVideo.value = false
    videoUploadProgress.value = 0
    event.target.value = ''
  }
}

// 移除视频
const removeVideo = () => {
  videoUrl.value = ''
  videoCover.value = ''
  videoUploadProgress.value = 0
  isUploadingVideo.value = false
}

// 触发封面上传
const triggerCoverUpload = () => {
  coverInput.value?.click()
}

// 处理视频封面上传
const handleCoverUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('封面图片不能超过10MB')
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    const result = await request.post('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    if (result.code === 0) {
      videoCover.value = result.data
      ElMessage.success('封面上传成功')
    }
  } catch (error) {
    console.error('封面上传失败:', error)
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('上传超时，请检查网络连接')
    } else {
      ElMessage.error(error.response?.data?.message || '封面上传失败')
    }
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
  try {
    await formRef.value?.validate()
    await submitNote('已发布')
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

// 提交笔记
const submitNote = async (state) => {
  submitting.value = true

  try {
    const requestData = {
      title: noteForm.title,
      content: noteForm.content,
      topicId: noteForm.topicId,
      location: noteForm.location,
      state: state,
      noteCategory: mediaType.value,
    }

    if (mediaType.value === 'image') {
      if (images.value.length === 0) {
        ElMessage.warning('请至少上传一张图片')
        submitting.value = false
        return
      }
      requestData.images = images.value
      // 使用用户选择的封面
      requestData.coverImg = images.value[selectedCoverIndex.value] || images.value[0]
    } else {
      if (!videoUrl.value) {
        ElMessage.warning('请上传视频')
        submitting.value = false
        return
      }
      requestData.video = videoUrl.value
      requestData.coverImg = videoCover.value || videoUrl.value
    }

    console.log('提交数据:', JSON.stringify(requestData, null, 2))

    const result = await noteAddService(requestData)
    ElMessage.success(state === '已发布' ? '发布成功！' : '已保存到草稿箱')

    window.dispatchEvent(new CustomEvent('notesChanged'))
    router.push('/home')
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 编辑器创建回调
const handleCreated = (editor) => {
  editorRef.value = editor
}

// 组件销毁时销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

onMounted(() => {
  if (!tokenStore.token) {
    router.push('/login')
    return
  }
  getTopicList()
})
</script>

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
            @click="switchMediaType('image')"
          >
            <div class="type-icon">🖼️</div>
            <span>图文笔记</span>
          </div>
          <div
            class="media-type-item"
            :class="{ active: mediaType === 'video' }"
            @click="switchMediaType('video')"
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
              :class="{ 'cover-selected': selectedCoverIndex === index }"
              :style="{ backgroundImage: `url(${img})` }"
              @click="selectCover(index)"
            >
              <div class="image-overlay">
                <el-button
                  circle
                  size="small"
                  class="delete-img-btn"
                  @click.stop="removeImage(index)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
                <div v-if="selectedCoverIndex === index" class="cover-badge">
                  <el-icon><Check /></el-icon> 封面
                </div>
              </div>
            </div>

            <div v-if="images.length < 9" class="upload-trigger" @click="triggerImageUpload">
              <div class="upload-icon">+</div>
              <span>上传图片</span>
              <input
                ref="imageInput"
                type="file"
                accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
                multiple
                style="display: none"
                @change="handleImageUpload"
              />
            </div>
          </div>
          <p class="upload-hint">
            最多可上传9张图片，支持jpg/png/gif/webp格式，单张不超过10MB • 点击任意图片设为封面
          </p>
        </div>

        <!-- 视频笔记上传区域 -->
        <div v-if="mediaType === 'video'" class="video-upload-section">
          <div class="section-title">
            <span class="title-icon">🎥</span>
            <span>上传视频</span>
          </div>

          <div v-if="!videoUrl" class="video-upload-wrapper">
            <div
              class="video-upload-trigger"
              :class="{ disabled: isUploadingVideo }"
              @click="!isUploadingVideo && triggerVideoUpload()"
            >
              <div class="video-upload-icon">
                <el-icon :size="48"><VideoCamera /></el-icon>
              </div>
              <span class="video-upload-text">点击上传视频</span>
              <span class="video-hint">支持 mp4/mov/avi/flv/3gp 格式，大小不超过 500MB</span>
              <input
                ref="videoInput"
                type="file"
                accept="video/mp4,video/quicktime,video/x-msvideo,video/x-flv,video/3gpp"
                style="display: none"
                @change="handleVideoUpload"
              />
            </div>

            <!-- 上传进度条 -->
            <div v-if="isUploadingVideo" class="upload-progress-container">
              <div class="progress-info">
                <span class="progress-text">视频上传中...</span>
                <span class="progress-percent">{{ videoUploadProgress }}%</span>
              </div>
              <el-progress
                :percentage="videoUploadProgress"
                :stroke-width="12"
                :show-text="false"
                color="linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%)"
              />
            </div>
          </div>

          <div v-else class="video-preview">
            <video
                ref="videoPreview"
                :src="videoUrl"
                controls
                class="preview-video"
                preload="auto"
                @error="(e) => console.error('视频加载错误:', e)"
            ></video>

            <!-- 转码中的提示 -->
            <div class="video-tip" v-if="isVideoTranscoding">
              <el-alert
                  type="info"
                  :closable="false"
                  title="视频正在转码中"
                  description="视频上传后需要1-2分钟转码，转码完成后会自动显示预览。您可以先发布笔记，稍后查看。"
                  show-icon
              />
            </div>

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
                    accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
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
              <Toolbar
                class="editor-toolbar"
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                mode="simple"
              />
              <Editor
                class="editor-content"
                v-model="noteForm.content"
                :defaultConfig="editorConfig"
                mode="simple"
                @onCreated="handleCreated"
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
          <el-button class="draft-btn" size="large" @click="saveAsDraft" :loading="submitting">
            <el-icon><Document /></el-icon> 存草稿
          </el-button>
          <el-button
            class="publish-btn"
            type="primary"
            size="large"
            @click="publishNote"
            :loading="submitting"
          >
            <el-icon><Promotion /></el-icon> 发布笔记
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

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
    cursor: pointer;
    transition: all 0.3s ease;
    border: 3px solid transparent;

    &.cover-selected {
      border-color: #c5a3ff;
      box-shadow: 0 0 0 4px rgba(197, 163, 255, 0.3);
    }

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

      .cover-badge {
        position: absolute;
        top: 8px;
        left: 8px;
        background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
        color: white;
        padding: 4px 10px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 4px;
        box-shadow: 0 2px 8px rgba(197, 163, 255, 0.4);
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

.video-upload-wrapper {
  position: relative;
}

.upload-progress-container {
  margin-top: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #faf7ff 0%, #f0e5ff 100%);
  border-radius: 16px;
  border: 2px solid rgba(197, 163, 255, 0.2);

  .progress-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .progress-text {
      font-size: 14px;
      color: #6a4a9c;
      font-weight: 500;
    }

    .progress-percent {
      font-size: 14px;
      color: #c5a3ff;
      font-weight: 600;
    }
  }

  :deep(.el-progress-bar__outer) {
    background: rgba(197, 163, 255, 0.15);
    border-radius: 6px;
  }

  :deep(.el-progress-bar__inner) {
    border-radius: 6px;
    transition: width 0.3s ease;
  }
}

.video-upload-trigger {
  background: linear-gradient(135deg, #faf7ff 0%, #f0e5ff 100%);
  border: 2px dashed #c5a3ff;
  border-radius: 24px;
  padding: 48px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;

  &.disabled {
    opacity: 0.6;
    cursor: not-allowed;
    pointer-events: none;
  }

  .video-upload-icon {
    margin-bottom: 16px;
    color: #c5a3ff;
  }
  .video-upload-text {
    display: block;
    font-size: 16px;
    color: #6a4a9c;
    margin-bottom: 8px;
  }
  .video-hint {
    font-size: 12px;
    color: #b0a7c0;
  }

  &:hover:not(.disabled) {
    background: linear-gradient(135deg, #fff 0%, #f8f3ff 100%);
    transform: translateY(-2px);
  }
}

.video-preview {
  .preview-video {
    width: 100%;
    border-radius: 16px;
    max-height: 400px;
    background: #000;
  }
  .video-tip {
    margin-top: 12px;

    :deep(.el-alert) {
      background: linear-gradient(135deg, #f0f7ff 0%, #f0e5ff 100%);
      border: none;
      border-radius: 12px;

      .el-alert__title {
        color: #6a4a9c;
        font-weight: 600;
      }

      .el-alert__description {
        color: #a09abf;
        font-size: 12px;
      }
    }
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
    border: 1px solid #f0e5ff;
    border-radius: 12px;
    overflow: hidden;
    background: #faf7ff;

    .editor-toolbar {
      border-bottom: 1px solid #f0e5ff;
    }

    .editor-content {
      height: 300px;

      :deep(.w-e-text-container) {
        background: #faf7ff;
      }
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
</style>

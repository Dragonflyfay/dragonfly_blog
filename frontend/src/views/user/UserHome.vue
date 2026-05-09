NEW_FILE_CODE
<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import {
  VideoPlay,
  VideoPause,
  Top,
  Loading,
  PictureFilled,
  View,
  ChatDotRound,
  Star,
  CaretRight,
  Clock,
  Collection,
  ArrowLeft,
  ArrowRight
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { 
  userNotePageListService, 
  topicListService,
  likeNoteService,
  unlikeNoteService,
  checkLikeNoteService,
  recordViewService,
  getCommentsByNoteIdService,
  addCommentService,
  favoriteNoteService,
  unfavoriteNoteService,
  checkFavoriteNoteService,
  likeCommentService,
  unlikeCommentService,
  checkLikeCommentService,
  noteDetailService
} from '@/api/note'

const router = useRouter()

// 搜索表单
const searchForm = reactive({
  topicId: 0,
  keyword: '',
})

// 话题列表
const topics = ref([])

// 笔记数据
const notes = ref([])
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(12)
const loading = ref(false)
const hasMore = ref(true)
const isLoadingMore = ref(false)

// 瀑布流列数
const columnCount = ref(4)

// 视频播放器状态
const videoPlayers = reactive({})

// 弹窗相关
const showDetailDialog = ref(false)
const currentNote = ref(null)
const dialogVideoPlayer = ref(null)

// 回到顶部按钮显示状态
const showBackToTop = ref(false)

// 点赞状态管理
const likedNotes = reactive({})
const likedComments = reactive({})
const favoritedNotes = reactive({})

// 评论相关
const comments = ref([])
const commentContent = ref('')
const showCommentInput = ref(false)
const replyToComment = ref(null) // 回复的评论

// 占位图
const PLACEHOLDER_IMG =
    'data:image/svg+xml,' +
    encodeURIComponent(
        `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="533" viewBox="0 0 400 533"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#ffeef8"/><stop offset="100%" stop-color="#e8f4ff"/></linearGradient></defs><rect width="400" height="533" fill="url(#g)"/><text x="200" y="270" text-anchor="middle" fill="#b8b0c8" font-family="system-ui" font-size="14">暂无封面</text></svg>`,
    )

// 切换话题
const changeTopicId = (id) => {
  searchForm.topicId = id
  resetAndLoad()
}

// 搜索
const handleSearch = () => {
  resetAndLoad()
}

// 重置并重新加载
const resetAndLoad = () => {
  pageNum.value = 1
  notes.value = []
  hasMore.value = true
  loadNotes()
}

// 加载话题列表
const loadTopics = async () => {
  try {
    const res = await topicListService()
    topics.value = res.data || []
    topics.value.unshift({
      id: 0,
      topicName: '全部',
    })
  } catch (error) {
    console.error('获取话题列表失败:', error)
    ElMessage.error('获取话题列表失败')
  }
}

// 加载笔记数据
const loadNotes = async () => {
  if (loading.value || !hasMore.value) return

  if (pageNum.value === 1) {
    loading.value = true
  } else {
    isLoadingMore.value = true
  }

  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }
    if (searchForm.topicId && searchForm.topicId !== 0) {
      params.topicId = searchForm.topicId
    }
    if (searchForm.keyword) {
      params.keyword = searchForm.keyword
    }

    const result = await userNotePageListService(params)

    if (result.data && result.data.items) {
      const newNotes = result.data.items

      if (pageNum.value === 1) {
        notes.value = newNotes
      } else {
        notes.value = [...notes.value, ...newNotes]
      }

      total.value = result.data.total || 0

      // 判断是否还有更多数据
      if (notes.value.length >= total.value) {
        hasMore.value = false
      }
    }
  } catch (error) {
    console.error('加载笔记失败:', error)
    ElMessage.error('加载笔记失败')
  } finally {
    loading.value = false
    isLoadingMore.value = false
  }
}

// 图片加载错误处理
const onImgError = (e) => {
  const el = e.target
  if (el) el.src = PLACEHOLDER_IMG
}

// 获取封面图
const getCoverImage = (note) => {
  // 优先使用封面图
  if (note.coverImg) return note.coverImg
  // 如果是图文笔记，使用第一张图片作为封面
  if (note.noteCategory === 'image' && Array.isArray(note.images) && note.images.length > 0) {
    return note.images[0]
  }
  // 如果是视频笔记，使用视频URL作为封面（浏览器会自动生成第一帧）
  if (note.noteCategory === 'video' && note.video) {
    return note.video
  }
  return PLACEHOLDER_IMG
}

// 生成视频缩略图（使用视频第一帧）
const generateVideoThumbnail = (videoUrl) => {
  // 直接返回视频URL，浏览器会自动显示第一帧
  return videoUrl
}

// 图片轮播相关
const currentImageIndex = ref(0)
const showImageGallery = ref(false)

// 打开图片画廊
const openImageGallery = (note, index = 0) => {
  currentNote.value = note
  currentImageIndex.value = index
  showImageGallery.value = true
}

// 关闭图片画廊
const closeImageGallery = () => {
  showImageGallery.value = false
  currentImageIndex.value = 0
}

// 上一张图片
const prevImage = () => {
  if (currentNote.value && currentNote.value.images) {
    currentImageIndex.value = (currentImageIndex.value - 1 + currentNote.value.images.length) % currentNote.value.images.length
  }
}

// 下一张图片
const nextImage = () => {
  if (currentNote.value && currentNote.value.images) {
    currentImageIndex.value = (currentImageIndex.value + 1) % currentNote.value.images.length
  }
}

// 计算内容预览文本
const getContentPreview = (content, maxLength = 60) => {
  if (!content) return ''
  const div = document.createElement('div')
  div.innerHTML = content
  const text = (div.textContent || '').replace(/\s+/g, ' ').trim()
  if (!text) return ''
  return text.length > maxLength ? `${text.slice(0, maxLength)}…` : text
}

// 格式化数字
const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num
}

// 格式化时间为多久以前
const formatTimeAgo = (time) => {
  if (!time) return ''
  
  const now = new Date()
  const past = new Date(time)
  const diffMs = now - past
  const diffSecs = Math.floor(diffMs / 1000)
  const diffMins = Math.floor(diffSecs / 60)
  const diffHours = Math.floor(diffMins / 60)
  const diffDays = Math.floor(diffHours / 24)
  
  if (diffSecs < 60) {
    return '刚刚'
  } else if (diffMins < 60) {
    return `${diffMins}分钟前`
  } else if (diffHours < 24) {
    return `${diffHours}小时前`
  } else if (diffDays < 30) {
    return `${diffDays}天前`
  } else {
    return time.split(' ')[0] // 返回日期部分
  }
}

// 查看详情
const viewDetail = async (note) => {
  try {
    // 获取完整的笔记详情
    const res = await noteDetailService(note.id)
    currentNote.value = res.data
    showDetailDialog.value = true
    
    // 重置图片索引
    currentImageIndex.value = 0
    
    // 记录浏览
    await recordView(note.id)
    
    // 检查点赞状态
    await checkLiked(note.id)
    
    // 检查收藏状态
    await checkFavorited(note.id)
    
    // 加载评论
    await loadComments(note.id)
    
    nextTick(() => {
      initDialogVideoPlayer()
    })
  } catch (error) {
    console.error('获取笔记详情失败:', error)
    ElMessage.error('获取笔记详情失败')
  }
}

// 视频错误处理
const handleVideoError = (e) => {
  console.error('视频加载失败:', e)
  ElMessage.error('视频加载失败，请检查网络连接或视频链接')
}

// 初始化弹窗视频播放器
const initDialogVideoPlayer = () => {
  if (dialogVideoPlayer.value && currentNote.value?.noteCategory === 'video') {
    const video = dialogVideoPlayer.value
    video.addEventListener('timeupdate', handleVideoTimeUpdate)
    video.addEventListener('loadedmetadata', () => {
      // 自动设置封面为视频第一帧
      if (!currentNote.value.coverImg) {
        video.currentTime = 0.1
      }
    })
  }
}

// 视频时间更新
const handleVideoTimeUpdate = (e) => {
  const video = e.target
  if (!currentNote.value.coverImg && video.currentTime > 0) {
    // 可以在这里捕获视频帧作为封面
  }
}

// 播放/暂停视频
const toggleVideoPlay = (noteId, event) => {
  const video = event.target.closest('.video-container').querySelector('video')
  if (video) {
    if (video.paused) {
      video.play()
      videoPlayers[noteId].isPlaying = true
    } else {
      video.pause()
      videoPlayers[noteId].isPlaying = false
    }
  }
}

// 视频进度条拖拽
const onVideoSeek = (noteId, event) => {
  const videoContainer = event.target.closest('.video-container')
  const video = videoContainer.querySelector('video')
  const progressBar = videoContainer.querySelector('.progress-bar')

  if (video && progressBar) {
    const rect = progressBar.getBoundingClientRect()
    const percent = Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width))
    video.currentTime = percent * video.duration

    // 更新进度条显示
    const progressFill = progressBar.querySelector('.progress-fill')
    if (progressFill) {
      progressFill.style.width = `${percent * 100}%`
    }
  }
}

// 初始化视频播放器状态
const initVideoPlayer = (noteId) => {
  if (!videoPlayers[noteId]) {
    videoPlayers[noteId] = {
      isPlaying: false,
      currentTime: 0,
      duration: 0,
      progress: 0
    }
  }
  return videoPlayers[noteId]
}

// 视频元数据加载完成
const onVideoLoaded = (noteId, event) => {
  const video = event.target
  videoPlayers[noteId].duration = video.duration
}

// 视频时间更新
const onVideoTimeUpdate = (noteId, event) => {
  const video = event.target
  const player = videoPlayers[noteId]
  player.currentTime = video.currentTime
  player.progress = (video.currentTime / video.duration) * 100
}

// 视频播放结束
const onVideoEnded = (noteId) => {
  videoPlayers[noteId].isPlaying = false
  videoPlayers[noteId].progress = 0
}

// 滚动事件处理
const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight

  // 显示/隐藏回到顶部按钮
  showBackToTop.value = scrollTop > 300

  // 触底加载更多
  if (scrollTop + windowHeight >= documentHeight - 100 && hasMore.value && !isLoadingMore.value) {
    pageNum.value++
    loadNotes()
  }
}

// 回到顶部
const backToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

// 关闭弹窗
const closeDialog = () => {
  showDetailDialog.value = false
  currentNote.value = null
  currentImageIndex.value = 0
  if (dialogVideoPlayer.value) {
    dialogVideoPlayer.value.pause()
  }
}

// 点赞笔记
const toggleLike = async (note, event) => {
  event.stopPropagation() // 阻止事件冒泡，避免触发卡片点击
  
  try {
    if (likedNotes[note.id]) {
      // 取消点赞
      await unlikeNoteService(note.id)
      likedNotes[note.id] = false
      note.likesCount = Math.max(0, (note.likesCount || 0) - 1)
      ElMessage.success('已取消点赞')
    } else {
      // 点赞
      await likeNoteService(note.id)
      likedNotes[note.id] = true
      note.likesCount = (note.likesCount || 0) + 1
      
      // 添加点赞动画效果
      const likeElement = document.querySelector(`.like-badge[data-note-id="${note.id}"]`)
      if (likeElement) {
        likeElement.classList.add('like-animation')
        setTimeout(() => {
          likeElement.classList.remove('like-animation')
        }, 500)
      }
      
      ElMessage.success('点赞成功')
    }
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 检查是否已点赞
const checkLiked = async (noteId) => {
  try {
    const res = await checkLikeNoteService(noteId)
    likedNotes[noteId] = res.data
  } catch (error) {
    // 静默失败，不影响用户体验
    console.warn('检查点赞状态失败:', error.message || error)
  }
}

// 收藏笔记
const toggleFavorite = async (note, event) => {
  event.stopPropagation()
  
  try {
    if (favoritedNotes[note.id]) {
      // 取消收藏
      await unfavoriteNoteService(note.id)
      favoritedNotes[note.id] = false
      note.favoritesCount = Math.max(0, (note.favoritesCount || 0) - 1)
      ElMessage.success('已取消收藏')
    } else {
      // 收藏
      await favoriteNoteService(note.id)
      favoritedNotes[note.id] = true
      note.favoritesCount = (note.favoritesCount || 0) + 1
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error('收藏失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 检查是否已收藏
const checkFavorited = async (noteId) => {
  try {
    const res = await checkFavoriteNoteService(noteId)
    favoritedNotes[noteId] = res.data
  } catch (error) {
    // 静默失败，不影响用户体验
    console.warn('检查收藏状态失败:', error.message || error)
  }
}

// 记录浏览
const recordView = async (noteId) => {
  try {
    await recordViewService(noteId)
  } catch (error) {
    // 静默失败，不影响用户体验
    console.warn('记录浏览失败:', error.message || error)
  }
}

// 加载评论
const loadComments = async (noteId) => {
  try {
    const res = await getCommentsByNoteIdService(noteId)
    comments.value = res.data || []
    
    // 检查每个评论的点赞状态
    for (const comment of comments.value) {
      await checkCommentLiked(comment.id)
    }
  } catch (error) {
    console.error('加载评论失败:', error)
    ElMessage.error('加载评论失败')
  }
}

// 提交评论
const submitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  
  try {
    const commentData = {
      noteId: currentNote.value.id,
      content: commentContent.value,
      parentId: replyToComment.value ? replyToComment.value.id : 0, // 顶级评论或回复
      replyToUserId: replyToComment.value ? replyToComment.value.userId : null
    }
    
    await addCommentService(commentData)
    ElMessage.success('评论成功')
    commentContent.value = ''
    replyToComment.value = null
    
    // 重新加载评论列表
    await loadComments(currentNote.value.id)
    
    // 更新笔记的评论数
    currentNote.value.commentsCount = (currentNote.value.commentsCount || 0) + 1
  } catch (error) {
    console.error('评论失败:', error)
    ElMessage.error(error.response?.data?.message || '评论失败')
  }
}

// 回复评论
const replyComment = (comment) => {
  replyToComment.value = comment
  commentContent.value = `@${comment.nickname || comment.username || '匿名用户'} `
  // 聚焦到评论输入框
  nextTick(() => {
    const textarea = document.querySelector('.comment-input-wrapper textarea')
    if (textarea) {
      textarea.focus()
    }
  })
}

// 点赞评论
const toggleCommentLike = async (comment, event) => {
  event.stopPropagation()
  
  try {
    if (likedComments[comment.id]) {
      // 取消点赞
      await unlikeCommentService(comment.id)
      likedComments[comment.id] = false
      comment.likesCount = Math.max(0, (comment.likesCount || 0) - 1)
      ElMessage.success('已取消点赞')
    } else {
      // 点赞
      await likeCommentService(comment.id)
      likedComments[comment.id] = true
      comment.likesCount = (comment.likesCount || 0) + 1
      
      // 添加点赞动画效果
      const likeElement = document.querySelector(`.comment-like-btn[data-comment-id="${comment.id}"]`)
      if (likeElement) {
        likeElement.classList.add('like-animation')
        setTimeout(() => {
          likeElement.classList.remove('like-animation')
        }, 500)
      }
      
      ElMessage.success('点赞成功')
    }
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 检查评论是否已点赞
const checkCommentLiked = async (commentId) => {
  try {
    const res = await checkLikeCommentService(commentId)
    likedComments[commentId] = res.data
  } catch (error) {
    // 静默失败，不影响用户体验
    console.warn('检查评论点赞状态失败:', error.message || error)
  }
}

// 响应式列数调整
const updateColumnCount = () => {
  const width = window.innerWidth
  if (width < 768) {
    columnCount.value = 2
  } else if (width < 1024) {
    columnCount.value = 3
  } else if (width < 1440) {
    columnCount.value = 4
  } else {
    columnCount.value = 5
  }
}

// 生命周期
onMounted(() => {
  loadTopics()
  loadNotes()
  updateColumnCount()
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', updateColumnCount)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', updateColumnCount)
})
</script>

<template>
  <div class="user-home-container">
    <!-- 顶部导航栏 -->
    <div class="top-navbar">
      <div class="navbar-content">
        <div class="logo-section">
          <span class="logo-text">📝 蜻蜓博客</span>
        </div>

        <div class="search-section">
          <el-input
              v-model="searchForm.keyword"
              placeholder="搜索感兴趣的内容..."
              prefix-icon="Search"
              clearable
              class="search-input"
              @keyup.enter="handleSearch"
          />
        </div>

        <div class="user-section">
          <el-button type="primary" round class="publish-btn" @click="router.push('/publish')">
            <el-icon><CaretRight /></el-icon>
            <span>发笔记</span>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 话题标签栏 -->
    <div class="topic-bar">
      <div class="topic-scroll-wrapper">
        <div
            v-for="topic in topics"
            :key="topic.id"
            class="topic-tag"
            :class="{ active: searchForm.topicId === topic.id }"
            @click="changeTopicId(topic.id)"
        >
          {{ topic.topicName }}
        </div>
      </div>
    </div>

    <!-- 瀑布流内容区 -->
    <div class="waterfall-container" v-loading="loading && pageNum === 1">
      <div v-if="notes.length === 0 && !loading" class="empty-state">
        <el-icon class="empty-icon"><PictureFilled /></el-icon>
        <p class="empty-text">暂无内容</p>
        <p class="empty-hint">快来发布第一篇笔记吧~</p>
      </div>

      <div v-else class="waterfall-grid" :style="{ '--column-count': columnCount }">
        <div
            v-for="note in notes"
            :key="note.id"
            class="waterfall-item"
            @click="viewDetail(note)"
        >
          <div class="note-card">
            <!-- 媒体区域 -->
            <div class="card-media">
              <!-- 视频类型 -->
              <div v-if="note.noteCategory === 'video'" class="video-container">
                <video
                    :src="note.video"
                    preload="metadata"
                    muted
                    loop
                    @loadedmetadata="initVideoPlayer(note.id)"
                    @timeupdate="onVideoTimeUpdate(note.id, $event)"
                    @ended="onVideoEnded(note.id)"
                    class="video-player"
                ></video>

                <!-- 视频控制层 -->
                <div class="video-overlay" @click.stop="toggleVideoPlay(note.id, $event)">
                  <el-icon v-if="!videoPlayers[note.id]?.isPlaying" class="play-icon">
                    <VideoPlay />
                  </el-icon>
                </div>

                <!-- 视频进度条 -->
                <div class="video-progress" @click.stop="onVideoSeek(note.id, $event)">
                  <div class="progress-bar">
                    <div
                        class="progress-fill"
                        :style="{ width: `${videoPlayers[note.id]?.progress || 0}%` }"
                    ></div>
                  </div>
                </div>

                <!-- 视频标识 -->
                <div class="video-badge">
                  <el-icon><VideoPlay /></el-icon>
                  <span>视频</span>
                </div>
              </div>

              <!-- 图片类型 -->
              <img
                  v-else
                  :src="getCoverImage(note)"
                  :alt="note.title"
                  loading="lazy"
                  @error="onImgError"
                  class="card-image"
              />
            </div>

            <!-- 卡片内容 -->
            <div class="card-body">
              <h3 class="card-title">{{ note.title }}</h3>
              <p class="card-desc">{{ getContentPreview(note.content) }}</p>

              <div class="card-footer">
                <div class="author-info">
                  <span class="author-avatar">👤</span>
                  <span class="author-name">作者</span>
                </div>

                <div class="stats-info">
                  <span class="stat-item">
                    <el-icon><View /></el-icon>
                    {{ formatNumber(note.viewsCount || 0) }}
                  </span>
                  <span class="stat-item">
                    <el-icon><ChatDotRound /></el-icon>
                    {{ formatNumber(note.commentsCount || 0) }}
                  </span>
                  <span 
                    class="stat-item like-stat" 
                    :class="{ liked: likedNotes[note.id] }"
                    @click="toggleLike(note, $event)"
                  >
                    <el-icon><Star /></el-icon>
                    {{ formatNumber(note.likesCount || 0) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多提示 -->
      <div v-if="isLoadingMore" class="loading-more">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-if="!hasMore && notes.length > 0" class="no-more">
        <span>没有更多了~</span>
      </div>
    </div>

    <!-- 回到顶部按钮 -->
    <transition name="fade-slide">
      <div v-show="showBackToTop" class="back-to-top" @click="backToTop">
        <el-icon><Top /></el-icon>
        <span>顶部</span>
      </div>
    </transition>

    <!-- 详情弹窗 -->
    <el-dialog
        v-model="showDetailDialog"
        :show-close="true"
        width="80%"
        class="detail-dialog"
        @close="closeDialog"
    >
      <div v-if="currentNote" class="detail-content">
        <div class="detail-left">
          <!-- 视频详情 -->
          <div v-if="currentNote.noteCategory === 'video'" class="detail-video-wrapper">
            <video
                ref="dialogVideoPlayer"
                :src="currentNote.video"
                controls
                autoplay
                preload="metadata"
                crossorigin="anonymous"
                class="detail-video"
                @error="handleVideoError"
            ></video>
          </div>

          <!-- 图片详情 -->
          <div v-else class="detail-images">
            <div class="image-gallery">
              <img
                :src="currentNote.images[currentImageIndex] || currentNote.coverImg"
                :alt="`图片${currentImageIndex + 1}`"
                @error="onImgError"
                class="detail-image"
              />
              
              <!-- 图片导航按钮 -->
              <div v-if="currentNote.images && currentNote.images.length > 1" class="image-nav">
                <button class="nav-btn prev-btn" @click="prevImage">
                  <el-icon><ArrowLeft /></el-icon>
                </button>
                <button class="nav-btn next-btn" @click="nextImage">
                  <el-icon><ArrowRight /></el-icon>
                </button>
              </div>
              
              <!-- 图片指示器 -->
              <div v-if="currentNote.images && currentNote.images.length > 1" class="image-indicator">
                {{ currentImageIndex + 1 }} / {{ currentNote.images.length }}
              </div>
            </div>
          </div>
        </div>

        <div class="detail-right">
          <h2 class="detail-title">{{ currentNote.title }}</h2>
          <div class="detail-meta">
            <span class="meta-item">👤 作者</span>
            <span class="meta-item">📍 {{ currentNote.location || '未知地点' }}</span>
            <span class="meta-item">🕒 {{ currentNote.publishTime || currentNote.createTime }}</span>
          </div>
          <div class="detail-stats">
            <span class="stat-badge">
              <el-icon><View /></el-icon>
              {{ currentNote.viewsCount || 0 }} 浏览
            </span>
            <span class="stat-badge">
              <el-icon><ChatDotRound /></el-icon>
              {{ currentNote.commentsCount || 0 }} 评论
            </span>
            <span 
              class="stat-badge like-badge" 
              :class="{ liked: likedNotes[currentNote.id] }"
              :data-note-id="currentNote.id"
              @click="toggleLike(currentNote, $event)"
            >
              <el-icon><Star /></el-icon>
              {{ currentNote.likesCount || 0 }} 点赞
            </span>
            <span 
              class="stat-badge favorite-badge" 
              :class="{ favorited: favoritedNotes[currentNote.id] }"
              :data-note-id="currentNote.id"
              @click="toggleFavorite(currentNote, $event)"
            >
              <el-icon><Collection /></el-icon>
              {{ currentNote.favoritesCount || 0 }} 收藏
            </span>
            <span class="stat-badge">
              <el-icon><Clock /></el-icon>
              {{ formatTimeAgo(currentNote.createTime) }}
            </span>
          </div>
          <div class="detail-body" v-html="currentNote.content"></div>
          
          <!-- 评论区 -->
          <div class="comment-section">
            <h3 class="comment-title">评论 ({{ comments.length }})</h3>
            
            <!-- 评论输入框 -->
            <div class="comment-input-wrapper">
              <el-input
                v-model="commentContent"
                type="textarea"
                :rows="3"
                :placeholder="replyToComment ? `回复 @${replyToComment.nickname || replyToComment.username || '匿名用户'}...` : '写下你的评论...'"
                maxlength="500"
                show-word-limit
              />
              <div class="comment-actions-bar">
                <el-button 
                  v-if="replyToComment"
                  size="small"
                  @click="replyToComment = null; commentContent = ''"
                >
                  取消回复
                </el-button>
                <el-button 
                  type="primary" 
                  round 
                  class="submit-comment-btn"
                  @click="submitComment"
                >
                  {{ replyToComment ? '发送回复' : '发表评论' }}
                </el-button>
              </div>
            </div>
            
            <!-- 评论列表 -->
            <div class="comment-list">
              <div 
                v-for="comment in comments" 
                :key="comment.id" 
                class="comment-item"
              >
                <div class="comment-avatar">👤</div>
                <div class="comment-content">
                  <div class="comment-header">
                    <span class="comment-author">{{ comment.nickname || comment.username || '匿名用户' }}</span>
                    <span class="comment-time">{{ comment.createTime }}</span>
                  </div>
                  <div class="comment-text">{{ comment.content }}</div>
                  <div class="comment-actions">
                    <span 
                      class="comment-like-btn" 
                      :class="{ liked: likedComments[comment.id] }"
                      :data-comment-id="comment.id"
                      @click="toggleCommentLike(comment, $event)"
                    >
                      <el-icon><Star /></el-icon>
                      {{ comment.likesCount || 0 }}
                    </span>
                    <span 
                      class="comment-reply-btn"
                      @click="replyComment(comment)"
                    >
                      回复
                    </span>
                  </div>
                </div>
              </div>
              
              <div v-if="comments.length === 0" class="no-comments">
                暂无评论，快来抢沙发~
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.user-home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
}

// 顶部导航栏
.top-navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 12px 0;

  .navbar-content {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
  }

  .logo-section {
    .logo-text {
      font-size: 24px;
      font-weight: bold;
      background: linear-gradient(135deg, #ff2442, #ff6b8a);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }
  }

  .search-section {
    flex: 1;
    max-width: 500px;

    .search-input {
      :deep(.el-input__wrapper) {
        border-radius: 20px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      }
    }
  }

  .publish-btn {
    background: linear-gradient(135deg, #ff2442, #ff6b8a);
    border: none;
    display: flex;
    align-items: center;
    gap: 6px;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(255, 36, 66, 0.3);
    }
  }
}

// 话题标签栏
.topic-bar {
  background: white;
  padding: 16px 0;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  .topic-scroll-wrapper {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    gap: 12px;
    overflow-x: auto;

    &::-webkit-scrollbar {
      height: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: #ddd;
      border-radius: 2px;
    }
  }

  .topic-tag {
    padding: 8px 20px;
    border-radius: 20px;
    background: #f5f5f5;
    color: #666;
    cursor: pointer;
    transition: all 0.3s ease;
    white-space: nowrap;
    font-size: 14px;

    &:hover {
      background: #ffeef1;
      color: #ff2442;
    }

    &.active {
      background: linear-gradient(135deg, #ff2442, #ff6b8a);
      color: white;
      font-weight: 500;
      box-shadow: 0 4px 12px rgba(255, 36, 66, 0.25);
    }
  }
}

// 瀑布流容器
.waterfall-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px 40px;
  min-height: calc(100vh - 200px);
}

.empty-state {
  text-align: center;
  padding: 80px 20px;

  .empty-icon {
    font-size: 64px;
    color: #ddd;
    margin-bottom: 16px;
  }

  .empty-text {
    font-size: 18px;
    color: #999;
    margin: 0 0 8px;
  }

  .empty-hint {
    font-size: 14px;
    color: #bbb;
  }
}

.waterfall-grid {
  display: grid;
  grid-template-columns: repeat(var(--column-count), 1fr);
  gap: 16px;
}

.waterfall-item {
  break-inside: avoid;
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.3s ease;

  &:hover {
    transform: translateY(-4px);
  }
}

.note-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 24px rgba(255, 36, 66, 0.15);
  }
}

.card-media {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  background: linear-gradient(135deg, #ffeef8, #e8f4ff);
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

// 视频容器
.video-container {
  position: relative;
  width: 100%;
  height: 100%;

  .video-player {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .video-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.2);
    opacity: 0;
    transition: opacity 0.3s ease;

    &:hover {
      opacity: 1;
    }

    .play-icon {
      font-size: 48px;
      color: white;
      filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.3));
    }
  }

  .video-progress {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 8px 12px;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.5), transparent);

    .progress-bar {
      height: 4px;
      background: rgba(255, 255, 255, 0.3);
      border-radius: 2px;
      cursor: pointer;
      transition: height 0.2s ease;

      &:hover {
        height: 6px;
      }

      .progress-fill {
        height: 100%;
        background: linear-gradient(90deg, #ff2442, #ff6b8a);
        border-radius: 2px;
        transition: width 0.1s linear;
      }
    }
  }

  .video-badge {
    position: absolute;
    top: 12px;
    right: 12px;
    padding: 4px 10px;
    background: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(8px);
    border-radius: 12px;
    color: white;
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.card-body {
  padding: 12px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-desc {
  font-size: 12px;
  color: #999;
  margin: 0 0 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;

  .author-avatar {
    font-size: 14px;
  }
}

.stats-info {
  display: flex;
  gap: 8px;

  .stat-item {
    display: flex;
    align-items: center;
    gap: 3px;
    font-size: 11px;
    color: #999;
    cursor: pointer;
    transition: all 0.3s ease;
    padding: 2px 6px;
    border-radius: 10px;

    .el-icon {
      font-size: 13px;
    }
    
    &.like-stat {
      &:hover {
        background: #ffeef1;
        color: #ff2442;
      }
      
      &.liked {
        color: #ff2442;
        background: #ffeef1;
      }
    }
  }
}

// 加载更多
.loading-more,
.no-more {
  text-align: center;
  padding: 24px;
  color: #999;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

// 回到顶部按钮
.back-to-top {
  position: fixed;
  right: 32px;
  bottom: 80px;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #ff2442, #ff6b8a);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.3);
  transition: all 0.3s ease;
  z-index: 99;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 6px 16px rgba(255, 36, 66, 0.4);
  }

  .el-icon {
    font-size: 20px;
  }

  span {
    font-size: 10px;
    margin-top: 2px;
  }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

// 详情弹窗
.detail-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.detail-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  max-height: 80vh;
  overflow: hidden;
}

.detail-left {
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
}

.detail-video-wrapper {
  width: 100%;

  .detail-video {
    width: 100%;
    max-height: 80vh;
    object-fit: contain;
  }
}

.detail-images {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;

  .image-gallery {
    position: relative;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }

  .detail-image {
    width: 100%;
    max-height: 70vh;
    object-fit: contain;
    border-radius: 8px;
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
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: rgba(0, 0, 0, 0.5);
      border: none;
      color: white;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s ease;
      pointer-events: auto;

      &:hover {
        background: rgba(0, 0, 0, 0.7);
        transform: scale(1.1);
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
    background: rgba(0, 0, 0, 0.6);
    color: white;
    padding: 4px 12px;
    border-radius: 12px;
    font-size: 12px;
  }
}

.detail-right {
  padding: 32px;
  overflow-y: auto;
  background: white;
}

.detail-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0 0 16px;
  line-height: 1.4;
}

.detail-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;

  .meta-item {
    font-size: 14px;
    color: #666;
  }
}

.detail-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;

  .stat-badge {
    padding: 6px 12px;
    background: #f5f5f5;
    border-radius: 16px;
    font-size: 13px;
    color: #666;
    display: flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    transition: all 0.3s ease;

    .el-icon {
      font-size: 14px;
    }
    
    &.like-badge {
      &.liked {
        background: linear-gradient(135deg, #ff2442, #ff6b8a);
        color: white;
        
        .el-icon {
          animation: heartBeat 0.5s ease;
        }
      }
      
      &:hover {
        transform: scale(1.05);
        box-shadow: 0 4px 12px rgba(255, 36, 66, 0.2);
      }
    }
    
    &.favorite-badge {
      &.favorited {
        background: linear-gradient(135deg, #ffa726, #ffb74d);
        color: white;
        
        .el-icon {
          animation: heartBeat 0.5s ease;
        }
      }
      
      &:hover {
        transform: scale(1.05);
        box-shadow: 0 4px 12px rgba(255, 167, 38, 0.2);
      }
    }
  }
}

@keyframes heartBeat {
  0% { transform: scale(1); }
  25% { transform: scale(1.2); }
  50% { transform: scale(0.95); }
  75% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.like-animation {
  animation: heartBeat 0.5s ease;
}

.detail-body {
  font-size: 15px;
  line-height: 1.8;
  color: #333;

  :deep(img) {
    max-width: 100%;
    border-radius: 8px;
    margin: 12px 0;
  }
}

// 评论区样式
.comment-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.comment-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px;
}

.comment-input-wrapper {
  margin-bottom: 24px;
  
  .submit-comment-btn {
    margin-top: 12px;
    background: linear-gradient(135deg, #ff2442, #ff6b8a);
    border: none;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(255, 36, 66, 0.3);
    }
  }
}

.comment-list {
  max-height: 400px;
  overflow-y: auto;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffeef8, #e8f4ff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  color: #666;
  margin-bottom: 8px;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.comment-like-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  color: #999;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    background: #f5f5f5;
    color: #ff2442;
  }
  
  &.liked {
    color: #ff2442;
    background: #ffeef1;
    
    .el-icon {
      animation: heartBeat 0.5s ease;
    }
  }
  
  .el-icon {
    font-size: 14px;
  }
}

.comment-reply-btn {
  font-size: 12px;
  color: #999;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 4px 8px;
  border-radius: 12px;
  
  &:hover {
    background: #f5f5f5;
    color: #ff2442;
  }
}

.comment-actions-bar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.no-comments {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

// 响应式设计
@media (max-width: 1440px) {
  .waterfall-grid {
    --column-count: 4;
  }
}

@media (max-width: 1024px) {
  .waterfall-grid {
    --column-count: 3;
  }

  .detail-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .waterfall-grid {
    --column-count: 2;
  }

  .navbar-content {
    padding: 0 16px;
  }

  .logo-text {
    font-size: 20px !important;
  }

  .search-section {
    display: none;
  }

  .waterfall-container {
    padding: 0 12px 40px;
  }

  .back-to-top {
    right: 16px;
    bottom: 60px;
    width: 40px;
    height: 40px;
  }
}
</style>

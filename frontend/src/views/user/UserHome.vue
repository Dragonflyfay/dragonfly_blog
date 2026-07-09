<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  VideoPlay,
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
  ArrowRight,
  Search,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  userNotePageListService,
  topicListService,
  likeNoteService,
  unlikeNoteService,
  checkLikeNoteService,
  batchCheckNoteLikedService,
  recordViewService,
  getCommentsByNoteIdService,
  addCommentService,
  favoriteNoteService,
  unfavoriteNoteService,
  checkFavoriteNoteService,
  likeCommentService,
  unlikeCommentService,
  checkLikeCommentService,
  batchCheckCommentLikedService,
  noteDetailService,
  batchCheckNoteFavoritedService,
  batchCheckFollowService,
  followUserService,
  unfollowUserService,
} from '@/api/note'
import { userInfoService, userListService } from '@/api/user.js'
import useUserInfoStore from '@/stores/userInfo.js'
import logoImg from '@/assets/logo.png'
// 导入评论子组件
import CommentItem from '@/components/CommentItem.vue'
// 添加评论提交状态
const submittingComment = ref(false)
// 用户信息 store
const userInfoStore = useUserInfoStore()

//用户信息缓存 - 使用 Map 提高查找效率
const userInfoCache = new Map()
const router = useRouter()
const route = useRoute()

// 搜索表单
const searchForm = reactive({
  topicId: 0,
  keyword: '',
})

// 搜索状态
const isSearching = computed(() => {
  return !!(searchForm.keyword && searchForm.keyword.trim())
})

// 搜索历史
const searchHistory = ref([])
const showSearchHistory = ref(false)

// 热门搜索（基于话题）
const hotSearches = computed(() => {
  // 取前5个话题作为热门搜索
  return topics.value.slice(1, 6).map((topic) => topic.topicName)
})

// 搜索建议（基于输入的关键字匹配话题）
const searchSuggestions = computed(() => {
  if (!searchForm.keyword || !searchForm.keyword.trim()) {
    return []
  }

  const keyword = searchForm.keyword.trim().toLowerCase()
  return topics.value.filter((topic) => topic.topicName.toLowerCase().includes(keyword)).slice(0, 5) // 最多显示5个建议
})

// 从 localStorage 加载搜索历史
const loadSearchHistory = () => {
  try {
    const history = localStorage.getItem('search_history')
    if (history) {
      searchHistory.value = JSON.parse(history)
    }
  } catch (e) {
    console.warn('加载搜索历史失败:', e)
  }
}

// 保存搜索历史
const saveSearchHistory = (keyword) => {
  if (!keyword || !keyword.trim()) return

  const trimmedKeyword = keyword.trim()

  // 避免重复
  const index = searchHistory.value.indexOf(trimmedKeyword)
  if (index > -1) {
    searchHistory.value.splice(index, 1)
  }

  // 添加到开头
  searchHistory.value.unshift(trimmedKeyword)

  // 最多保留10条
  if (searchHistory.value.length > 10) {
    searchHistory.value = searchHistory.value.slice(0, 10)
  }

  // 保存到 localStorage
  try {
    localStorage.setItem('search_history', JSON.stringify(searchHistory.value))
  } catch (e) {
    console.warn('保存搜索历史失败:', e)
  }
}

// 清除搜索历史
const clearSearchHistory = () => {
  searchHistory.value = []
  try {
    localStorage.removeItem('search_history')
  } catch (e) {
    console.warn('清除搜索历史失败:', e)
  }
  ElMessage.success('已清除搜索历史')
}

// 使用搜索历史
const useSearchHistory = (keyword) => {
  searchForm.keyword = keyword
  handleSearch()
  showSearchHistory.value = false
}

// 使用搜索建议
const useSearchSuggestion = (suggestion) => {
  searchForm.keyword = suggestion
  handleSearch()
  showSearchHistory.value = false
}

// 高亮关键字（增强版）
const highlightKeyword = (text) => {
  // 类型检查和容错
  if (text === null || text === undefined) {
    return ''
  }

  // 确保是字符串
  let textStr = text
  if (typeof text !== 'string') {
    // 如果是数字或其他类型，转换为字符串
    textStr = String(text)
  }

  // 如果没有搜索关键词，直接返回原文本
  if (!searchForm.keyword || !searchForm.keyword.trim()) {
    return textStr
  }

  const keyword = searchForm.keyword.trim()
  // 转义正则表达式特殊字符
  const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escapedKeyword})`, 'gi')

  return textStr.replace(
    regex,
    '<mark style="background: linear-gradient(135deg, #ffe4f0, #ffe0f5); color: #c5a3ff; padding: 2px 8px; border-radius: 12px; font-weight: 600;">$1</mark>',
  )
}

// 防抖定时器
let searchDebounceTimer = null

// 防抖搜索（用于输入时实时搜索）
const debounceSearch = () => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }

  searchDebounceTimer = setTimeout(() => {
    if (searchForm.keyword && searchForm.keyword.trim()) {
      handleSearch()
    }
  }, 500) // 500ms 防抖
}

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
const detailDialogRef = ref(null)

// 点赞状态管理
const likedNotes = reactive({}) //笔记点赞状态
const likedComments = reactive({}) //评论点赞状态
const favoritedNotes = reactive({}) //收藏状态

// 评论相关
const comments = ref([])
const commentContent = ref('')
const showCommentInput = ref(false)
const replyToComment = ref(null) // 回复的评论

// 递归计算总评论数（含子回复）
const countAllComments = (list) => {
  if (!list || !list.length) return 0
  let count = 0
  for (const c of list) {
    count += 1 + countAllComments(c.children || [])
  }
  return count
}
// ========== 回复评论相关状态 ==========
const replyingCommentId = ref(null) // 当前正在回复的评论ID
// 使用 Map 存储每个评论的回复内容，而不是单一变量
const replyInputContentMap = ref(new Map())

// 处理回复评论（点击回复按钮时调用）
// 处理回复评论（点击回复按钮时调用）
const handleReplyComment = (comment) => {
  // 如果点击的是同一个评论，关闭回复框
  if (replyingCommentId.value === comment.id) {
    replyingCommentId.value = null
    return
  }

  // 关闭其他回复框，设置当前回复的评论ID
  replyingCommentId.value = comment.id

  // 如果该评论还没有回复内容，初始化一个
  if (!replyInputContentMap.value.has(comment.id)) {
    replyInputContentMap.value.set(
      comment.id,
      `@${comment.nickname || comment.username || '匿名用户'} `,
    )
  }

  // 聚焦到回复输入框
  nextTick(() => {
    // 使用更精确的选择器，找到对应评论的回复输入框
    const replyInput = document.querySelector(
      `.comment-item-wrapper[data-comment-id="${comment.id}"] .reply-input-container .el-textarea__inner`,
    )
    if (replyInput) {
      replyInput.focus()
    }
  })
}

// 取消回复
const cancelReplyInline = (commentId) => {
  if (replyingCommentId.value === commentId) {
    replyingCommentId.value = null
    // 不清空内容，保留以便再次回复时使用
  }
}

// 更新回复内容
// 更新回复内容
const updateReplyContent = (commentId, content) => {
  replyInputContentMap.value.set(commentId, content)
}

// 获取指定评论的回复内容
const getReplyContent = (commentId) => {
  return replyInputContentMap.value.get(commentId) || ''
}

// 提交回复
const submitReply = async (parentCommentId) => {
  const replyContent = replyInputContentMap.value.get(parentCommentId) || ''
  if (!replyContent.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  if (!currentNote.value) return

  // 递归查找被回复的评论
  const findCommentById = (list, id) => {
    for (const comment of list) {
      if (comment.id === id) return comment
      if (comment.children && comment.children.length) {
        const found = findCommentById(comment.children, id)
        if (found) return found
      }
    }
    return null
  }

  const parentComment = findCommentById(comments.value, parentCommentId)
  if (!parentComment) return

  submittingComment.value = true
  try {
    const commentData = {
      noteId: currentNote.value.id,
      content: replyContent,
      parentId: parentCommentId,
      replyToUserId: parentComment.userId,
    }

    await addCommentService(commentData)
    ElMessage.success('回复成功')

    // 重置回复状态
    replyingCommentId.value = null
    replyInputContentMap.value.delete(parentCommentId)

    // 重新加载评论列表
    await loadComments(currentNote.value.id)

    // 更新笔记的评论数
    currentNote.value.commentsCount = (currentNote.value.commentsCount || 0) + 1
  } catch (error) {
    console.error('回复失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submittingComment.value = false
  }
}

// 扁平化评论树用于渲染（带深度层级）
const flatComments = computed(() => {
  const result = []
  const walk = (list, depth) => {
    if (!list) return
    for (const c of list) {
      result.push({ ...c, _depth: depth, _hasChildren: !!(c.children && c.children.length) })
      if (c.children && c.children.length) {
        walk(c.children, depth + 1)
      }
    }
  }
  walk(comments.value, 0)
  return result
})

// 弹窗动画状态
const dialogAnimation = ref('')
const isDialogVisible = ref(false)

// 切换话题
const changeTopicId = (id) => {
  searchForm.topicId = id
  resetAndLoad()
}

// 搜索
const handleSearch = () => {
  // 如果关键词为空，不执行搜索
  if (!searchForm.keyword || !searchForm.keyword.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  // 保存搜索历史
  saveSearchHistory(searchForm.keyword)

  resetAndLoad()
  showSearchHistory.value = false
}

// 清除搜索
const clearSearch = () => {
  searchForm.keyword = ''
  resetAndLoad()
  showSearchHistory.value = false
  ElMessage.success('已清除搜索')
}

// 显示搜索历史
const focusSearchInput = () => {
  if (!searchForm.keyword) {
    showSearchHistory.value = true
  }
}

// 隐藏搜索历史
const blurSearchInput = () => {
  // 延迟隐藏，以便点击历史记录
  setTimeout(() => {
    showSearchHistory.value = false
  }, 200)
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
// 获取用户信息（从缓存中获取）
const getUserInfo = (userId) => {
  if (!userId) return null
  return userInfoCache.get(userId) || null
}

// 批量设置用户信息到缓存
const setUsersToCache = (users) => {
  if (!users || !Array.isArray(users)) return
  users.forEach((user) => {
    if (user && user.id) {
      userInfoCache.set(user.id, user)
    }
  })
  // 同时保存到 sessionStorage（页面刷新后仍可用）
  try {
    const cacheObj = {}
    userInfoCache.forEach((value, key) => {
      cacheObj[key] = value
    })
    sessionStorage.setItem('user_info_cache', JSON.stringify(cacheObj))
  } catch (e) {
    console.warn('保存用户缓存失败:', e)
  }
}

// 从 sessionStorage 恢复用户缓存
const restoreUserCache = () => {
  try {
    const cached = sessionStorage.getItem('user_info_cache')
    if (cached) {
      const cacheObj = JSON.parse(cached)
      Object.keys(cacheObj).forEach((key) => {
        userInfoCache.set(parseInt(key), cacheObj[key])
      })
    }
  } catch (e) {
    console.warn('恢复用户缓存失败:', e)
  }
}

// 是否已从服务端加载过完整用户列表
let fullUserListLoaded = false

// 加载所有用户信息到缓存
const loadAllUsers = async () => {
  // 尝试从 sessionStorage 恢复
  restoreUserCache()

  // 优先缓存当前登录用户的信息（从 store 中获取）
  if (userInfoStore.info && userInfoStore.info.id) {
    const currentUser = {
      id: userInfoStore.info.id,
      username: userInfoStore.info.username,
      nickname: userInfoStore.info.nickname,
      userPic: userInfoStore.info.userPic || userInfoStore.info.avatar,
      role: userInfoStore.info.role,
    }
    userInfoCache.set(currentUser.id, currentUser)
    console.log('已缓存当前用户信息:', currentUser)
  }

  // 始终从服务端加载完整用户列表（首次加载后使用标记避免重复请求）
  if (fullUserListLoaded) return

  try {
    const res = await userListService()
    if (res.data && Array.isArray(res.data)) {
      setUsersToCache(res.data)
      fullUserListLoaded = true
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
}
// 动态获取单个用户信息（当缓存中没有时）
const fetchUserInfo = async (userId) => {
  if (!userId) return null

  // 如果缓存中已有，直接返回
  if (userInfoCache.has(userId)) {
    return userInfoCache.get(userId)
  }

  try {
    // 这里可以调用获取单个用户信息的接口
    // 如果没有单独接口，可以重新加载用户列表
    const res = await userListService()
    if (res.data && Array.isArray(res.data)) {
      setUsersToCache(res.data)
      return userInfoCache.get(userId) || null
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
  return null
}

// 加载笔记数据
const loadNotes = async () => {
  if (loading.value || !hasMore.value) return

  // 提前设置 loading 状态，避免页面闪烁
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

      // 为每个笔记填充真实的用户信息和确保统计数据有效
      notes.value.forEach((note) => {
        // 初始化图片加载状态
        note.imageLoaded = false

        // 填充用户信息...
        if (note.createUser) {
          const userInfo = getUserInfo(note.createUser)
          if (userInfo) {
            note.userName = userInfo.nickname || userInfo.username || '匿名用户'
            note.userPic = userInfo.userPic || ''
          } else {
            note.userName = note.createUserName || note.userName || note.authorName || '匿名用户'
            note.userPic = note.createUserAvatar || note.userPic || note.authorAvatar || ''
          }
        } else {
          note.userName = note.userName || note.authorName || '匿名用户'
          note.userPic = note.userPic || note.authorAvatar || ''
        }

        // 确保统计数据有默认值
        note.likesCount = note.likesCount || 0
        note.viewsCount = note.viewsCount || 0
        note.commentsCount = note.commentsCount || 0
        note.favoritesCount = note.favoritesCount || 0
      })

      // 批量检查点赞状态（一次性请求）
      const noteIds = newNotes.map((note) => note.id).filter((id) => id)
      if (noteIds.length > 0) {
        try {
          const res = await batchCheckNoteLikedService(noteIds)
          // 将结果合并到 likedNotes
          Object.assign(likedNotes, res.data)
        } catch (error) {
          console.warn('批量检查点赞状态失败:', error)
        }
      }
      //批量检查收藏状态
      if (noteIds.length > 0) {
        try {
          const res = await batchCheckNoteFavoritedService(noteIds)
          Object.assign(favoritedNotes, res.data)
        } catch (error) {
          console.warn('批量检查收藏状态失败:', error)
        }
      }
      // 收集作者ID并检查关注状态
      const authorIds = [
        ...new Set(
          newNotes
            .map((note) => note.createUser)
            .filter((id) => id && id !== userInfoStore.info.id),
        ),
      ]
      if (authorIds.length > 0) {
        await checkFollowingStatus(authorIds)
      }

      // 异步加载缺失的用户信息（不阻塞主流程）
      const missingUserIds = [
        ...new Set(
          notes.value
            .filter((note) => note.createUser && !userInfoCache.has(note.createUser))
            .map((note) => note.createUser),
        ),
      ]

      if (missingUserIds.length > 0) {
        // 使用 Promise.all 并行加载，而不是串行
        Promise.all(missingUserIds.map((userId) => fetchUserInfo(userId))).catch((err) => {
          console.warn('加载用户信息失败:', err)
        })
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
  if (el && el.src !== logoImg) {
    el.src = logoImg
  }
}

// 图片加载完成处理
const onCardImageLoad = (e) => {
  const img = e.target
  if (img) {
    // 确保图片正确显示，添加淡入效果
    requestAnimationFrame(() => {
      img.style.opacity = '1'
    })
  }
}

// 获取封面图
const getCoverImage = (note) => {
  if (note.coverImg) return { url: note.coverImg, isPlaceholder: false }
  if (note.noteCategory === 'image' && Array.isArray(note.images) && note.images.length > 0) {
    return { url: note.images[0], isPlaceholder: false }
  }
  if (note.noteCategory === 'video' && note.video) {
    return { url: note.video, isPlaceholder: false }
  }
  return { url: '', isPlaceholder: true }
}

const hasRealCover = (note) => {
  if (note.coverImg) return true
  if (note.noteCategory === 'image' && Array.isArray(note.images) && note.images.length > 0)
    return true
  if (note.noteCategory === 'video' && note.video) return true
  return false
}

// 生成视频缩略图（使用视频第一帧）
const generateVideoThumbnail = (videoUrl) => {
  // 直接返回视频URL，浏览器会自动显示第一帧
  return videoUrl
}

// 图片轮播相关
const currentImageIndex = ref(0)
const showImageGallery = ref(false)
const imageLoaded = ref(true)
const galleryImageLoading = ref(false)

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

// 图片加载完成
const onImageLoad = () => {
  imageLoaded.value = true
  galleryImageLoading.value = false
}

// 图片加载开始
const onImageLoadStart = () => {
  galleryImageLoading.value = true
  imageLoaded.value = false
}

// 上一张图片
const prevImage = () => {
  if (currentNote.value && currentNote.value.images) {
    galleryImageLoading.value = true
    imageLoaded.value = false
    currentImageIndex.value =
      (currentImageIndex.value - 1 + currentNote.value.images.length) %
      currentNote.value.images.length
  }
}

// 下一张图片
const nextImage = () => {
  if (currentNote.value && currentNote.value.images) {
    galleryImageLoading.value = true
    imageLoaded.value = false
    currentImageIndex.value = (currentImageIndex.value + 1) % currentNote.value.images.length
  }
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

  // 将时间字符串转换为Date对象
  const date = new Date(time)

  // 格式化为 YYYY-MM-DD HH:mm:ss 格式
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 查看详情
const viewDetail = async (note) => {
  try {
    // 获取完整的笔记详情
    const res = await noteDetailService(note.id)
    currentNote.value = res.data

    // 补充用户信息（从缓存中获取）
    if (currentNote.value.createUser) {
      const userInfo = getUserInfo(currentNote.value.createUser)
      if (userInfo) {
        currentNote.value.userName = userInfo.nickname || userInfo.username || '匿名用户'
        currentNote.value.userPic = userInfo.userPic || ''
      } else {
        currentNote.value.userName = currentNote.value.userName || '匿名用户'
        currentNote.value.userPic = currentNote.value.userPic || ''
      }
    } else {
      currentNote.value.userName = currentNote.value.userName || '匿名用户'
      currentNote.value.userPic = currentNote.value.userPic || ''
    }

    // 显示弹窗并播放动画
    isDialogVisible.value = true
    showDetailDialog.value = true
    dialogAnimation.value = 'dialog-enter-active'

    // 重置图片索引和加载状态
    currentImageIndex.value = 0
    imageLoaded.value = false
    galleryImageLoading.value = false

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
  const video = e.target
  const errorCode = video.error?.code || 'unknown'
  const retryCount = parseInt(video.dataset.retryCount) || 0

  console.warn('视频加载出错:', { errorCode, retryCount, src: video.src?.substring(0, 100) })

  // 网络错误和格式/CORS错误先重试，避免误报
  if (
    (errorCode === MediaError.MEDIA_ERR_NETWORK ||
     errorCode === MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED) &&
    retryCount < 2
  ) {
    console.warn(`视频错误(code=${errorCode})，第${retryCount + 1}次重试...`)
    video.dataset.retryCount = String(retryCount + 1)
    const originalSrc = video.src.split('?')[0]
    video.src = `${originalSrc}?t=${Date.now()}`
    video.load()
    return
  }

  // 重试耗尽才显示错误
  if (retryCount >= 2) {
    ElMessage.error({ message: '视频加载失败，已自动重试仍无法播放', duration: 4000 })
  }
}

// 弹窗视频元数据加载完成
const handleDialogVideoLoaded = (e) => {
  const video = e.target
  // 确保视频第一帧被渲染
  if (video.readyState >= 2) {
    video.currentTime = 0
  }
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
      progress: 0,
    }
  }
  return videoPlayers[noteId]
}

// 视频元数据加载完成
const onVideoLoaded = (noteId, event) => {
  const video = event.target
  if (!videoPlayers[noteId]) {
    initVideoPlayer(noteId)
  }
  videoPlayers[noteId].duration = video.duration
}

// 视频时间更新
const onVideoTimeUpdate = (noteId, event) => {
  const video = event.target
  if (!videoPlayers[noteId]) {
    initVideoPlayer(noteId)
  }
  const player = videoPlayers[noteId]
  player.currentTime = video.currentTime
  player.progress = video.duration ? (video.currentTime / video.duration) * 100 : 0
}

// 视频播放结束
const onVideoEnded = (noteId) => {
  if (!videoPlayers[noteId]) {
    initVideoPlayer(noteId)
  }
  videoPlayers[noteId].isPlaying = false
  videoPlayers[noteId].progress = 0
}

// 关闭弹窗 - 带动画
const closeDialog = () => {
  dialogAnimation.value = 'dialog-leave-active'

  setTimeout(() => {
    showDetailDialog.value = false
    isDialogVisible.value = false
    currentNote.value = null
    currentImageIndex.value = 0
    imageLoaded.value = false
    galleryImageLoading.value = false
    if (dialogVideoPlayer.value) {
      dialogVideoPlayer.value.pause()
    }
    dialogAnimation.value = ''
  }, 300)
}

// 点赞笔记
// 点赞笔记（带乐观更新和错误回滚）
const toggleLike = async (note, event) => {
  event.stopPropagation()

  const currentLiked = likedNotes[note.id] || false
  const originalLikesCount = note.likesCount || 0

  // 乐观更新UI
  likedNotes[note.id] = !currentLiked
  note.likesCount = currentLiked ? Math.max(0, originalLikesCount - 1) : originalLikesCount + 1

  try {
    if (currentLiked) {
      // 取消点赞
      await unlikeNoteService(note.id)
      console.log(`取消点赞笔记 ${note.id} 成功`)
    } else {
      // 点赞
      await likeNoteService(note.id)
      console.log(`点赞笔记 ${note.id} 成功`)
    }
  } catch (error) {
    // 请求失败，回滚状态
    console.error('点赞操作失败:', error)
    likedNotes[note.id] = currentLiked
    note.likesCount = originalLikesCount
    ElMessage.error(error.response?.data?.message || '操作失败，请重试')
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
      await unfavoriteNoteService(note.id)
      favoritedNotes[note.id] = false
      note.favoritesCount = Math.max(0, (note.favoritesCount || 0) - 1)
      // 移除 ElMessage.success('已取消收藏')
    } else {
      await favoriteNoteService(note.id)
      favoritedNotes[note.id] = true
      note.favoritesCount = (note.favoritesCount || 0) + 1
      // 移除 ElMessage.success('收藏成功')
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
    console.log('收藏状态检查结果:', noteId, res.data) // 添加日志调试
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

// 加载评论 - 确保子评论的点赞状态也被检查
const loadComments = async (noteId) => {
  try {
    const res = await getCommentsByNoteIdService(noteId)
    // 后端已经返回树形结构，直接使用，不要再调用 buildCommentTree
    comments.value = res.data || []

    // 收集所有评论ID
    const allCommentIds = []
    const collectCommentIds = (commentList) => {
      if (!commentList || !commentList.length) return
      for (const comment of commentList) {
        allCommentIds.push(comment.id)
        if (comment.children && comment.children.length) {
          collectCommentIds(comment.children)
        }
      }
    }
    collectCommentIds(comments.value)

    // 批量检查所有评论的点赞状态（一次性请求）
    if (allCommentIds.length > 0) {
      try {
        const res = await batchCheckCommentLikedService(allCommentIds)
        // 将结果合并到 likedComments
        Object.assign(likedComments, res.data)
      } catch (error) {
        console.warn('批量检查评论点赞状态失败:', error)
      }
    }

    console.log('评论点赞状态已加载:', likedComments)
  } catch (error) {
    console.error('加载评论失败:', error)
    ElMessage.error('加载评论失败')
  }
}

// 修改提交评论函数
const submitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  submittingComment.value = true
  try {
    const commentData = {
      noteId: currentNote.value.id,
      content: commentContent.value,
      parentId: replyToComment.value ? replyToComment.value.id : 0,
      replyToUserId: replyToComment.value ? replyToComment.value.userId : null,
    }

    await addCommentService(commentData)
    ElMessage.success(replyToComment.value ? '回复成功' : '评论成功')

    // 重置表单
    commentContent.value = ''
    replyToComment.value = null

    // 重新加载评论列表
    await loadComments(currentNote.value.id)

    // 更新笔记的评论数
    currentNote.value.commentsCount = (currentNote.value.commentsCount || 0) + 1
  } catch (error) {
    console.error('评论失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submittingComment.value = false
  }
}

// 取消回复
const cancelReply = () => {
  replyToComment.value = null
  commentContent.value = ''
}

// 修改回复评论函数（支持更深层级）
const replyComment = (comment) => {
  replyToComment.value = comment
  commentContent.value = `@${comment.nickname || comment.username || '匿名用户'} `
  // 聚焦到评论输入框
  nextTick(() => {
    const textarea = document.querySelector('.comment-textarea textarea')
    if (textarea) {
      textarea.focus()
    }
  })
}

// 修改评论点赞函数 - 支持乐观更新和状态同步
const toggleCommentLike = async (comment, event) => {
  event?.stopPropagation()

  const commentId = comment.id
  const currentLiked = likedComments[commentId] || false
  const originalLikesCount = comment.likesCount || 0

  // 乐观更新 UI
  likedComments[commentId] = !currentLiked
  comment.likesCount = currentLiked ? Math.max(0, originalLikesCount - 1) : originalLikesCount + 1

  try {
    if (currentLiked) {
      // 取消点赞
      await unlikeCommentService(commentId)
      console.log(`取消点赞评论 ${commentId} 成功`)
    } else {
      // 点赞
      await likeCommentService(commentId)
      console.log(`点赞评论 ${commentId} 成功`)
    }
  } catch (error) {
    // 请求失败，回滚状态
    console.error('评论点赞操作失败:', error)
    likedComments[commentId] = currentLiked
    comment.likesCount = originalLikesCount
    ElMessage.error(error.response?.data?.message || '操作失败，请重试')
  }
}

// 计算总评论数（树形结构）
const totalCommentCount = computed(() => {
  const countRecursive = (list) => {
    if (!list || !list.length) return 0
    let count = 0
    for (const item of list) {
      count += 1 + countRecursive(item.children)
    }
    return count
  }
  return countRecursive(comments.value)
})

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

// 滚动处理 - 实现无限加载
const showBackToTop = ref(false)

const handleScroll = () => {
  // 控制回到顶部按钮显示
  //showBackToTop.value = window.scrollY > 300

  // 无限滚动加载
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight

  // 当滚动到距离底部200px时加载更多
  if (scrollTop + windowHeight >= documentHeight - 200 && hasMore.value && !isLoadingMore.value) {
    pageNum.value++
    loadNotes()
  }
}

// 回到顶部
// const backToTop = () => {
//   window.scrollTo({
//     top: 0,
//     behavior: 'smooth'
//   })
// }

/** 封面区域宽高比  */
const getMediaAspectClass = (note) => {
  if (note.noteCategory === 'video') return 'media-ar-video'
  const v = (note.id || 0) % 5
  return ['media-ar-1-1', 'media-ar-4-5', 'media-ar-3-4', 'media-ar-5-7', 'media-ar-4-5'][v]
}

/** 估算卡片相对高度，用于最短列瀑布流 */
const estimateNoteHeight = (note) => {
  let media = 1.12
  if (note.noteCategory === 'video') {
    media = 1.5
  } else {
    const ratios = [1.0, 1.12, 1.28, 0.95, 1.18]
    media = ratios[(note.id || 0) % 5]
  }
  const titleLen = String(note.title || '').length
  const titleBonus = Math.min(titleLen / 120, 0.12)
  return media + titleBonus + 0.26
}

/** 瀑布流：按估算高度依次放入当前最短的列 */
const waterfallColumns = computed(() => {
  const n = Math.max(1, columnCount.value)
  const cols = Array.from({ length: n }, () => [])
  const heights = Array(n).fill(0)
  for (const note of notes.value) {
    let minIdx = 0
    for (let i = 1; i < n; i++) {
      if (heights[i] < heights[minIdx]) minIdx = i
    }
    cols[minIdx].push(note)
    heights[minIdx] += estimateNoteHeight(note)
  }
  return cols
})

// 监听用户信息更新事件，实时更新页面显示
const handleUserInfoUpdate = (event) => {
  const updatedUser = event.detail
  if (updatedUser && updatedUser.id) {
    console.log('收到用户信息更新事件:', updatedUser)

    // 更新缓存中的用户信息
    if (userInfoCache.has(updatedUser.id)) {
      const cachedUser = userInfoCache.get(updatedUser.id)
      userInfoCache.set(updatedUser.id, { ...cachedUser, ...updatedUser })
      console.log('已更新缓存中的用户信息')
    } else {
      // 如果缓存中没有，添加新用户信息
      userInfoCache.set(updatedUser.id, updatedUser)
      console.log('已添加新用户信息到缓存')
    }

    // 更新当前页面中所有该用户的笔记显示
    let updatedCount = 0
    notes.value.forEach((note) => {
      if (note.createUser === updatedUser.id) {
        note.userName = updatedUser.nickname || updatedUser.username || '匿名用户'
        note.userPic = updatedUser.userPic || updatedUser.avatar || ''
        updatedCount++
      }
    })
    console.log(`已更新 ${updatedCount} 条笔记的用户信息显示`)

    // 如果当前正在查看笔记详情，也更新详情中的用户信息
    if (currentNote.value && currentNote.value.createUser === updatedUser.id) {
      currentNote.value.userName = updatedUser.nickname || updatedUser.username || '匿名用户'
      currentNote.value.userPic = updatedUser.userPic || updatedUser.avatar || ''
      console.log('已更新笔记详情中的用户信息')
    }

    // 强制触发响应式更新
    notes.value = [...notes.value]
  }
}
// 构建评论树形结构
const buildCommentTree = (commentsList) => {
  if (!commentsList || !commentsList.length) return []

  const commentMap = new Map()
  const rootComments = []

  // 先建立所有评论的映射
  commentsList.forEach((comment) => {
    comment.children = []
    commentMap.set(comment.id, comment)
  })

  // 构建树形结构
  commentsList.forEach((comment) => {
    if (comment.parentId && comment.parentId !== 0 && commentMap.has(comment.parentId)) {
      const parent = commentMap.get(comment.parentId)
      parent.children = parent.children || []
      parent.children.push(comment)
    } else {
      rootComments.push(comment)
    }
  })

  // 对每一层按时间排序
  const sortByTime = (items) => {
    items.sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
    items.forEach((item) => {
      if (item.children && item.children.length) {
        sortByTime(item.children)
      }
    })
  }
  sortByTime(rootComments)

  return rootComments
}

// 关注状态
const followingStatus = reactive({})

// 切换关注
const toggleFollow = async (userId) => {
  try {
    if (followingStatus[userId]) {
      await unfollowUserService(userId)
      followingStatus[userId] = false
      if (currentNote.value && currentNote.value.createUser === userId) {
        currentNote.value.followersCount = Math.max(0, (currentNote.value.followersCount || 0) - 1)
      }
      ElMessage.success('已取消关注')
    } else {
      await followUserService(userId)
      followingStatus[userId] = true
      if (currentNote.value && currentNote.value.createUser === userId) {
        currentNote.value.followersCount = (currentNote.value.followersCount || 0) + 1
      }
      ElMessage.success('关注成功')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
    console.log(error)
  }
}

// 批量检查关注状态
const checkFollowingStatus = async (userIds) => {
  if (!userIds || userIds.length === 0) return
  try {
    const res = await batchCheckFollowService(userIds)
    Object.assign(followingStatus, res.data)
  } catch (error) {
    console.warn('批量检查关注状态失败:', error)
  }
}
// 生命周期
onMounted(async () => {
  // 先加载用户信息和话题列表
  await Promise.all([loadTopics(), loadAllUsers()])
  // 然后加载笔记（此时用户缓存已有数据）
  await loadNotes()
  // 如果从"我"页面跳转过来，自动打开对应笔记详情
  const noteId = route.query.noteId
  if (noteId) {
    const targetNote = notes.value.find(n => n.id === Number(noteId))
    if (targetNote) {
      nextTick(() => viewDetail(targetNote))
    }
  }
  updateColumnCount()
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', updateColumnCount)

  // 监听用户信息更新事件
  window.addEventListener('userInfoUpdated', handleUserInfoUpdate)

  // 加载搜索历史
  loadSearchHistory()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', updateColumnCount)
  // 移除用户信息更新事件监听
  window.removeEventListener('userInfoUpdated', handleUserInfoUpdate)
})
</script>

<template>
  <div class="user-home-container">
    <!-- 顶部导航栏 -->
    <div class="top-navbar">
      <div class="navbar-content">
        <div class="logo-section">
          <span class="logo-text"> dragonfly 笔记</span>
        </div>

        <div class="search-section">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索感兴趣的内容..."
            prefix-icon="Search"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
            @clear="clearSearch"
            @focus="focusSearchInput"
            @blur="blurSearchInput"
            @input="debounceSearch"
          >
            <template #append>
              <el-button
                :icon="Search"
                @click="handleSearch"
                :class="{ 'is-searching': isSearching }"
              />
            </template>
          </el-input>

          <!-- 搜索历史下拉框 -->
          <transition name="slide-down">
            <div v-if="showSearchHistory" class="search-history-dropdown">
              <!-- 搜索建议（优先显示） -->
              <div v-if="searchSuggestions.length > 0" class="suggestions-section">
                <div class="section-header">
                  <el-icon><Search /></el-icon>
                  <span class="section-title">搜索建议</span>
                </div>
                <div class="suggestions-list">
                  <div
                    v-for="suggestion in searchSuggestions"
                    :key="'suggestion-' + suggestion"
                    class="suggestion-item"
                    @click="useSearchSuggestion(suggestion)"
                  >
                    <el-icon><Search /></el-icon>
                    <span v-html="highlightKeyword(suggestion)"></span>
                  </div>
                </div>
              </div>

              <!-- 热门搜索 -->
              <div v-else-if="hotSearches.length > 0" class="hot-search-section">
                <div class="section-header">
                  <el-icon><Top /></el-icon>
                  <span class="section-title">热门搜索</span>
                </div>
                <div class="hot-search-list">
                  <div
                    v-for="(keyword, index) in hotSearches"
                    :key="'hot-' + index"
                    class="hot-search-item"
                    @click="useSearchHistory(keyword)"
                  >
                    <span class="hot-index" :class="{ 'top-3': index < 3 }">{{ index + 1 }}</span>
                    <span>{{ keyword }}</span>
                  </div>
                </div>
              </div>

              <!-- 搜索历史 -->
              <div v-if="searchHistory.length > 0" class="history-section">
                <div class="section-header">
                  <div class="header-left">
                    <el-icon><Clock /></el-icon>
                    <span class="section-title">搜索历史</span>
                  </div>
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click="clearSearchHistory"
                    class="clear-history-btn"
                  >
                    清空
                  </el-button>
                </div>
                <div class="history-list">
                  <div
                    v-for="(keyword, index) in searchHistory"
                    :key="index"
                    class="history-item"
                    @click="useSearchHistory(keyword)"
                  >
                    <el-icon><Clock /></el-icon>
                    <span>{{ keyword }}</span>
                  </div>
                </div>
              </div>

              <!-- 空状态 -->
              <div
                v-if="
                  searchHistory.length === 0 &&
                  hotSearches.length === 0 &&
                  searchSuggestions.length === 0
                "
                class="empty-history"
              >
                <el-icon><Search /></el-icon>
                <p>暂无搜索记录</p>
              </div>
            </div>
          </transition>
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

      <!-- 搜索结果提示 -->
      <div v-if="isSearching" class="search-result-info">
        <span class="search-keyword">"{{ searchForm.keyword }}"</span>
        <span class="search-count">共找到 {{ total }} 条结果</span>
        <el-button type="primary" link size="small" @click="clearSearch" class="clear-search-btn">
          清除搜索
        </el-button>
      </div>
    </div>

    <!-- 瀑布流内容区 -->
    <div class="waterfall-container" v-loading="loading && pageNum === 1">
      <!-- 无笔记，空状态 -->
      <div v-if="notes.length === 0 && !loading" class="empty-state">
        <el-icon class="empty-icon"><PictureFilled /></el-icon>
        <p class="empty-text">{{ isSearching ? '未找到相关内容' : '暂无内容' }}</p>
        <p class="empty-hint">{{ isSearching ? '换个关键词试试吧~' : '快来发布第一篇笔记吧~' }}</p>
      </div>

      <!-- 使用 JS 瀑布流，按最短列优先放置 -->
      <div v-else class="waterfall-masonry">
        <div
          v-for="(column, colIndex) in waterfallColumns"
          :key="colIndex"
          class="waterfall-column"
        >
          <div
            v-for="note in column"
            :key="note.id"
            class="waterfall-item"
            @click="viewDetail(note)"
          >
            <article class="note-card xhs-note-card">
              <!-- 媒体区域 -->
              <div class="card-media" :class="getMediaAspectClass(note)">
                <!-- 视频内容 -->
                <div v-if="note.noteCategory === 'video'" class="video-container">
                  <video
                    :key="note.video"
                    :src="note.video"
                    :poster="note.coverImg || ''"
                    preload="metadata"
                    crossorigin="anonymous"
                    playsinline
                    muted
                    class="card-video"
                    @error="handleVideoError"
                    @loadedmetadata="(e) => onVideoLoaded(note.id, e)"
                    @timeupdate="(e) => onVideoTimeUpdate(note.id, e)"
                    @ended="() => onVideoEnded(note.id)"
                  ></video>

                  <!-- 视频播放遮罩 -->
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

                <!-- 图片内容 -->
                <template v-else>
                  <!-- 无封面：logo 占位 -->
                  <div v-if="!hasRealCover(note)" class="card-placeholder">
                    <img :src="logoImg" class="placeholder-logo" alt="logo" />
                  </div>
                  <!-- 有封面：正常图片 -->
                  <template v-else>
                    <div class="image-skeleton" v-if="!note.imageLoaded"></div>
                    <img
                      :src="getCoverImage(note).url"
                      :alt="note.title"
                      loading="lazy"
                      @error="onImgError"
                      @load="
                        (e) => {
                          onCardImageLoad(e)
                          note.imageLoaded = true
                        }
                      "
                      class="card-image"
                      :class="{ 'image-loaded': note.imageLoaded }"
                    />
                  </template>
                </template>

                <!-- 悬停遮罩层 -->
                <div class="card-hover-overlay">
                  <div class="hover-actions">
                    <button class="action-btn view-btn" @click.stop="viewDetail(note)">
                      <el-icon><View /></el-icon>
                      <span>查看</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- 卡片内容区 -->
              <div class="xhs-card-body">
                <h3 class="xhs-title">{{ note.title }}</h3>
                <div class="xhs-meta-row">
                  <div class="xhs-author">
                    <img
                      v-if="note.userPic"
                      :src="note.userPic"
                      class="author-avatar-img"
                      alt="author"
                    />
                    <span class="xhs-name">{{ note.userName }}</span>
                  </div>
                  <button
                    type="button"
                    class="xhs-like"
                    :class="{ liked: likedNotes[note.id] }"
                    @click="toggleLike(note, $event)"
                  >
                    <svg
                      v-if="!likedNotes[note.id]"
                      class="like-icon"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <path
                        d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                      />
                    </svg>
                    <svg
                      v-else
                      class="like-icon liked-icon"
                      viewBox="0 0 24 24"
                      fill="currentColor"
                    >
                      <path
                        d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                      />
                    </svg>
                    <span>{{ formatNumber(note.likesCount || 0) }}</span>
                  </button>

                  <!-- 收藏按钮 -->
                  <button
                    type="button"
                    class="xhs-favorite"
                    :class="{ favorited: favoritedNotes[note.id] }"
                    @click="toggleFavorite(note, $event)"
                  >
                    <svg
                      v-if="!favoritedNotes[note.id]"
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <path
                        d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
                      />
                    </svg>
                    <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                      <path
                        d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
                      />
                    </svg>
                    <span>{{ formatNumber(note.favoritesCount || 0) }}</span>
                  </button>
                </div>
              </div>
            </article>
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
    <!--    <transition name="fade-slide">-->
    <!--      <div v-show="showBackToTop" class="back-to-top" @click="backToTop">-->
    <!--        <el-icon><Top /></el-icon>-->
    <!--        <span>顶部</span>-->
    <!--      </div>-->
    <!--    </transition>-->

    <!-- 详情弹窗 - 带动画效果 -->
    <Teleport to="body">
      <div
        v-if="showDetailDialog"
        class="detail-dialog-overlay"
        :class="dialogAnimation"
        @click.self="closeDialog"
      >
        <div class="detail-dialog-container" :class="dialogAnimation">
          <div class="detail-dialog-header">
            <button class="dialog-close-btn" @click="closeDialog">
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M18 6L6 18M6 6L18 18"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                />
              </svg>
            </button>
          </div>

          <div v-if="currentNote" class="detail-content">
            <div class="detail-left">
              <!-- 视频详情 -->
              <div v-if="currentNote.noteCategory === 'video'" class="detail-video-wrapper">
                <video
                  ref="dialogVideoPlayer"
                  :key="currentNote.video"
                  :src="currentNote.video"
                  controls
                  autoplay
                  preload="metadata"
                  crossorigin="anonymous"
                  class="detail-video"
                  @error="handleVideoError"
                  @loadedmetadata="handleDialogVideoLoaded"
                ></video>
              </div>

              <!-- 图片详情 -->
              <div v-else class="detail-images">
                <div class="image-gallery">
                  <div class="gallery-image-container">
                    <!-- 图片加载骨架屏 -->
                    <div class="gallery-skeleton" v-if="galleryImageLoading"></div>
                    <img
                      :src="currentNote.images[currentImageIndex] || currentNote.coverImg"
                      :alt="`图片${currentImageIndex + 1}`"
                      @error="onImgError"
                      @load="onImageLoad"
                      @loadstart="onImageLoadStart"
                      class="detail-image"
                      :class="{ 'image-loaded': imageLoaded, 'image-loading': !imageLoaded }"
                    />
                  </div>

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
                  <div
                    v-if="currentNote.images && currentNote.images.length > 1"
                    class="image-indicator"
                  >
                    {{ currentImageIndex + 1 }} / {{ currentNote.images.length }}
                  </div>
                </div>
              </div>
            </div>

            <div class="detail-right">
              <h2 class="detail-title">{{ currentNote.title }}</h2>
              <div class="detail-author-section">
                <img
                  v-if="currentNote.userPic"
                  :src="currentNote.userPic"
                  class="author-avatar-detail"
                />
                <div v-else class="author-avatar-placeholder">
                  {{ (currentNote.userName || '匿名用户').charAt(0).toUpperCase() }}
                </div>
                <div class="author-info">
                  <span class="author-name-detail">{{ currentNote.userName || '匿名用户' }}</span>
                  <div class="author-stats">
                    <span>粉丝 {{ currentNote.followersCount || 0 }}</span>
                    <span>关注 {{ currentNote.followingCount || 0 }}</span>
                  </div>
                </div>
                <button
                  v-if="currentNote.createUser !== userInfoStore.info.id"
                  class="follow-btn"
                  :class="{ following: followingStatus[currentNote.createUser] }"
                  @click="toggleFollow(currentNote.createUser)"
                >
                  {{ followingStatus[currentNote.createUser] ? '已关注' : '关注' }}
                </button>
              </div>
              <div class="detail-meta">
                <span class="meta-item">📍 {{ currentNote.location || '未知地点' }}</span>
                <span class="meta-item"
                  >🕒 {{ formatTimeAgo(currentNote.publishTime || currentNote.createTime) }}</span
                >
              </div>
              <div class="detail-stats">
                <!-- 浏览量 -->
                <!--                <div class="stat-item">-->
                <!--                  <el-icon><View /></el-icon>-->
                <!--                  <span>{{ currentNote.viewsCount || 0 }}</span>-->
                <!--                  <span class="stat-label">浏览</span>-->
                <!--                </div>-->

                <!-- 评论数 -->
                <div class="stat-item">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>{{ currentNote.commentsCount || 0 }}</span>
                  <span class="stat-label">评论</span>
                </div>

                <!-- 点赞 -->
                <div
                  class="stat-item like-item"
                  :class="{ liked: likedNotes[currentNote.id] }"
                  @click="toggleLike(currentNote, $event)"
                >
                  <svg
                    v-if="!likedNotes[currentNote.id]"
                    class="stat-icon"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path
                      d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                    />
                  </svg>
                  <svg v-else class="stat-icon liked-icon" viewBox="0 0 24 24" fill="currentColor">
                    <path
                      d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                    />
                  </svg>
                  <span>{{ currentNote.likesCount || 0 }}</span>
                  <span class="stat-label">点赞</span>
                </div>

                <!-- 收藏 -->
                <div
                  class="stat-item favorite-item"
                  :class="{ favorited: favoritedNotes[currentNote.id] }"
                  @click="toggleFavorite(currentNote, $event)"
                >
                  <el-icon><Collection /></el-icon>
                  <span>{{ currentNote.favoritesCount || 0 }}</span>
                  <span class="stat-label">收藏</span>
                </div>
              </div>
              <div class="detail-body" v-html="currentNote.content"></div>

              <!-- 评论区 - 完整版 -->
              <div class="comment-section">
                <h3 class="comment-title">
                  评论 ({{ totalCommentCount }})
                  <span class="comment-subtitle">共 {{ comments.length }} 条主评论</span>
                </h3>

                <!-- 顶部评论输入框 -->
                <div class="top-comment-input-wrapper">
                  <div class="input-avatar">
                    <img
                      v-if="userInfoStore.info.userPic"
                      :src="userInfoStore.info.userPic"
                      class="input-avatar-img"
                    />
                    <div v-else class="input-avatar-placeholder">
                      {{ (userInfoStore.info.nickname || '我').charAt(0).toUpperCase() }}
                    </div>
                  </div>
                  <div class="input-content">
                    <el-input
                      v-model="commentContent"
                      type="textarea"
                      :rows="3"
                      placeholder="写下你的评论..."
                      maxlength="500"
                      show-word-limit
                      class="comment-textarea"
                    />
                    <div class="comment-actions-bar">
                      <span class="comment-tip">✨ 友善发言，分享美好</span>
                      <el-button
                        type="primary"
                        round
                        class="submit-comment-btn"
                        @click="submitComment"
                        :loading="submittingComment"
                      >
                        发表评论
                      </el-button>
                    </div>
                  </div>
                </div>

                <!-- 评论列表 - 树形结构展示 -->
                <div class="comment-list">
                  <template v-for="comment in comments" :key="comment.id">
                    <CommentItem
                      :comment="comment"
                      :depth="0"
                      :liked-comments="likedComments"
                      :replying-comment-id="replyingCommentId"
                      :reply-content-map="replyInputContentMap"
                      :submitting="submittingComment"
                      @reply="handleReplyComment"
                      @toggle-like="toggleCommentLike"
                      @cancel-reply="cancelReplyInline"
                      @submit-reply="submitReply"
                      @update-reply-content="updateReplyContent"
                    />
                  </template>

                  <div v-if="comments.length === 0" class="no-comments">
                    <span class="no-comments-emoji">💬</span>
                    <p>暂无评论，快来抢沙发~</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style lang="scss" scoped>
.user-home-container {
  min-height: 100vh;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
}

// 在 UserHome.vue 的 style 中添加/替换以下样式

// 顶部导航栏样式（与发布页风格统一）
.top-navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  padding: 16px 0;

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
      font-size: 26px;
      font-weight: 700;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
      letter-spacing: 1px;
    }
  }

  .search-section {
    flex: 1;
    max-width: 500px;
    position: relative;

    .search-input {
      :deep(.el-input__wrapper) {
        border-radius: 48px;
        padding: 8px 20px;
        background: linear-gradient(135deg, #faf7ff 0%, #fff 100%);
        border: 1px solid #f0e5ff;
        transition: all 0.3s ease;
        box-shadow: 0 2px 8px rgba(197, 163, 255, 0.08);

        &:hover {
          border-color: #c5a3ff;
          background-color: #fff;
          box-shadow: 0 4px 12px rgba(197, 163, 255, 0.12);
        }

        &.is-focus {
          border-color: #c5a3ff;
          background-color: #fff;
          box-shadow: 0 0 0 4px rgba(197, 163, 255, 0.12);
        }
      }

      :deep(.el-input__prefix) {
        color: #c5a3ff;
        font-size: 16px;
      }

      :deep(.el-input__inner) {
        color: #6a4a9c;

        &::placeholder {
          color: #b0a7c0;
        }
      }

      :deep(.el-input-group__append) {
        background: transparent;
        border: none;
        padding: 0;
        margin: 0;

        .el-button {
          border-radius: 0 48px 48px 0;
          background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
          border: none;
          color: white;
          padding: 12px 20px;
          transition: all 0.3s ease;

          &:hover {
            background: linear-gradient(135deg, #b090e0 0%, #e0a0c0 100%);
            transform: scale(1.02);
          }

          &.is-searching {
            background: linear-gradient(135deg, #ff6b6b 0%, #ff8e8e 100%);
          }
        }
      }
    }

    // 搜索历史下拉框（新样式）
    .search-history-dropdown {
      position: absolute;
      top: calc(100% + 8px);
      left: 0;
      right: 0;
      background: rgba(255, 255, 255, 0.98);
      backdrop-filter: blur(10px);
      border-radius: 20px;
      box-shadow:
        0 20px 40px rgba(197, 163, 255, 0.15),
        0 4px 12px rgba(0, 0, 0, 0.05);
      z-index: 1000;
      overflow: hidden;
      animation: slideDown 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      max-height: 400px;
      overflow-y: auto;
      border: 1px solid rgba(197, 163, 255, 0.2);

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-track {
        background: #f0e5ff;
        border-radius: 3px;
        margin: 8px 0;
      }

      &::-webkit-scrollbar-thumb {
        background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
        border-radius: 3px;
      }

      .suggestions-section,
      .hot-search-section,
      .history-section {
        padding: 12px 0;

        &:not(:last-child) {
          border-bottom: 1px solid rgba(197, 163, 255, 0.12);
        }

        .section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 8px 16px;
          margin-bottom: 4px;

          .header-left {
            display: flex;
            align-items: center;
            gap: 8px;

            .el-icon {
              color: #c5a3ff;
              font-size: 16px;
            }
          }

          .section-title {
            font-size: 12px;
            color: #8a6fa8;
            font-weight: 600;
            letter-spacing: 0.5px;
          }

          .clear-history-btn {
            font-size: 12px;
            color: #c5a3ff;

            &:hover {
              color: #f8b4d9;
            }
          }
        }
      }

      .hot-search-list {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        padding: 4px 16px;

        .hot-search-item {
          display: flex;
          align-items: center;
          gap: 6px;
          padding: 6px 14px;
          background: linear-gradient(135deg, rgba(197, 163, 255, 0.08), rgba(248, 180, 217, 0.08));
          border-radius: 24px;
          cursor: pointer;
          transition: all 0.25s ease;
          color: #6a4a9c;
          font-size: 13px;
          font-weight: 500;

          &:hover {
            background: linear-gradient(
              135deg,
              rgba(197, 163, 255, 0.18),
              rgba(248, 180, 217, 0.18)
            );
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(197, 163, 255, 0.15);
            color: #c5a3ff;
          }

          .hot-index {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 20px;
            height: 20px;
            border-radius: 50%;
            background: rgba(197, 163, 255, 0.15);
            color: #c5a3ff;
            font-size: 11px;
            font-weight: 700;

            &.top-3 {
              background: linear-gradient(135deg, #ff6b6b, #ff8e8e);
              color: white;
              box-shadow: 0 2px 6px rgba(255, 107, 107, 0.3);
            }
          }
        }
      }

      .suggestions-list {
        padding: 0 8px;

        .suggestion-item {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 10px 16px;
          cursor: pointer;
          transition: all 0.2s ease;
          color: #6a4a9c;
          font-size: 14px;
          border-radius: 12px;
          margin: 2px 0;

          &:hover {
            background: linear-gradient(135deg, rgba(197, 163, 255, 0.1), rgba(248, 180, 217, 0.1));
            transform: translateX(4px);

            .el-icon {
              color: #f8b4d9;
            }
          }

          .el-icon {
            font-size: 14px;
            color: #c5a3ff;
            transition: color 0.2s ease;
          }

          :deep(mark) {
            background: linear-gradient(135deg, #ffe4f0, #ffe0f5);
            color: #c5a3ff;
            padding: 2px 4px;
            border-radius: 4px;
            font-weight: 600;
          }
        }
      }

      .history-list {
        padding: 0 8px;

        .history-item {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 10px 16px;
          cursor: pointer;
          transition: all 0.2s ease;
          color: #8a6fa8;
          font-size: 14px;
          border-radius: 12px;
          margin: 2px 0;

          &:hover {
            background: linear-gradient(135deg, rgba(197, 163, 255, 0.1), rgba(248, 180, 217, 0.1));
            transform: translateX(4px);
            color: #c5a3ff;

            .el-icon {
              color: #f8b4d9;
            }
          }

          .el-icon {
            font-size: 14px;
            color: #c5a3ff;
            transition: color 0.2s ease;
          }
        }
      }

      .empty-history {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 40px 20px;
        color: #b0a7c0;

        .el-icon {
          font-size: 48px;
          margin-bottom: 12px;
          opacity: 0.4;
          color: #c5a3ff;
        }

        p {
          font-size: 13px;
          margin: 0;
          color: #a09abf;
        }
      }
    }
  }

  .publish-btn {
    background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
    border: none;
    border-radius: 48px;
    padding: 10px 24px;
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    color: white;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(197, 163, 255, 0.25);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
    }

    .el-icon {
      font-size: 16px;
    }
  }
}

// 搜索历史下拉动画
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-down-enter-from {
  opacity: 0;
  transform: translateY(-12px);
}

.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

// 话题标签栏样式
.topic-bar {
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8px);
  padding: 16px 0;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  border-bottom: 1px solid rgba(197, 163, 255, 0.1);

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
      background: linear-gradient(90deg, #e0c3ff, #c5a3ff);
      border-radius: 2px;
    }
  }

  .topic-tag {
    padding: 8px 22px;
    border-radius: 40px;
    background: linear-gradient(135deg, #faf7ff 0%, #fff 100%);
    color: #8a6fa8;
    cursor: pointer;
    transition: all 0.3s ease;
    white-space: nowrap;
    font-size: 14px;
    font-weight: 500;
    border: 1px solid rgba(197, 163, 255, 0.2);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);

    &:hover {
      background: linear-gradient(135deg, #fff 0%, #f8f3ff 100%);
      color: #c5a3ff;
      border-color: #c5a3ff;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(197, 163, 255, 0.15);
    }

    &.active {
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      color: white;
      font-weight: 600;
      box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
      border-color: transparent;
    }
  }

  .search-result-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 24px;
    margin: 12px 24px 0;
    background: linear-gradient(135deg, rgba(197, 163, 255, 0.1), rgba(248, 180, 217, 0.1));
    border-radius: 60px;
    font-size: 14px;
    animation: slideDown 0.3s ease-out;
    backdrop-filter: blur(4px);

    .search-keyword {
      color: #6a4a9c;
      font-weight: 700;
      padding: 4px 12px;
      background: rgba(197, 163, 255, 0.2);
      border-radius: 20px;
      font-size: 13px;
    }

    .search-count {
      color: #8a6fa8;
      font-size: 13px;
    }

    .clear-search-btn {
      margin-left: auto;
      color: #c5a3ff;
      font-weight: 500;

      &:hover {
        color: #f8b4d9;
      }
    }
  }
}

// 动画
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.publish-btn {
  background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
  border: none;
  border-radius: 48px;
  padding: 12px 28px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: white;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(197, 163, 255, 0.2);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
  }

  .el-icon {
    font-size: 16px;
  }
}

// 话题标签栏
.topic-bar {
  background: rgba(255, 255, 255, 0.96);
  padding: 18px 0;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  backdrop-filter: blur(10px);

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
      background: linear-gradient(90deg, #e0c3ff, #c5a3ff);
      border-radius: 2px;
    }
  }

  .topic-tag {
    padding: 10px 24px;
    border-radius: 48px;
    background: rgba(197, 163, 255, 0.1);
    color: #8a6fa8;
    cursor: pointer;
    transition: all 0.3s ease;
    white-space: nowrap;
    font-size: 14px;
    font-weight: 500;
    border: 1px solid transparent;

    &:hover {
      background: rgba(197, 163, 255, 0.18);
      color: #c5a3ff;
      border-color: rgba(197, 163, 255, 0.3);
      transform: translateY(-2px);
    }

    &.active {
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      color: white;
      font-weight: 600;
      box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
      border-color: transparent;
    }
  }

  .search-result-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 24px;
    margin-top: 12px;
    background: linear-gradient(135deg, rgba(197, 163, 255, 0.1), rgba(248, 180, 217, 0.1));
    border-radius: 12px;
    font-size: 14px;
    animation: slideDown 0.3s ease-out;

    .search-keyword {
      color: #6a4a9c;
      font-weight: 600;
      padding: 2px 8px;
      background: rgba(197, 163, 255, 0.15);
      border-radius: 6px;
    }

    .search-count {
      color: #8a6fa8;
    }

    .clear-search-btn {
      margin-left: auto;
      color: #c5a3ff;

      &:hover {
        color: #f8b4d9;
      }
    }
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
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
    color: #c5a3ff;
    margin-bottom: 16px;
    animation: float 3s ease-in-out infinite;
  }

  .empty-text {
    font-size: 18px;
    color: #6a4a9c;
    margin: 0 0 8px;
    font-weight: 500;
  }

  .empty-hint {
    font-size: 14px;
    color: #b0a7c0;
  }
}

// 瀑布流容器 - 使用 JS 瀑布流 + Flexbox 布局
.waterfall-masonry {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.waterfall-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.waterfall-item {
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.5s ease-out;

  &:hover {
    transform: translateY(-4px);
  }
}

// 卡片媒体区域 - 让图片按原始比例显示
.card-media {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #f0f0f0;
  // 不设置固定宽高比，让图片自适应
  aspect-ratio: 3 / 4;

  .card-image {
    width: 100%;
    height: auto; // 高度自适应，保持原始比例
    display: block;
    opacity: 0;
    transition:
      opacity 0.3s ease,
      transform 0.3s ease;

    &.image-loaded {
      opacity: 1;
    }
  }

  // 视频保持固定宽高比
  &.media-ar-video {
    aspect-ratio: 9 / 16;

    .card-video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  // 图片骨架屏
  .image-skeleton {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s ease-in-out infinite;
    z-index: 1;
  }

  // logo 占位卡片
  .card-placeholder {
    width: 100%;
    height: 100%;
    // 宽高比 3:4
    position: absolute;
    top: 0;
    left: 0;

    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 40%, #fce4ec 100%);

    .placeholder-logo {
      width: 40%;
      max-width: 80px;
      min-width: 28px;
      aspect-ratio: 1;
      object-fit: contain;
      opacity: 0.35;
    }
  }
}

// .xhs-avatar（如果没有其他地方使用）

.waterfall-item {
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.5s ease-out;

  &:hover {
    transform: translateY(-4px);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.xhs-note-card {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(197, 163, 255, 0.1);

  &:hover {
    box-shadow: 0 12px 32px rgba(197, 163, 255, 0.15);
    transform: translateY(-4px);
    border-color: rgba(197, 163, 255, 0.2);
  }
}

.card-media {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #f0f0f0;

  // 统一所有卡片的宽高比，确保瀑布流整齐
  aspect-ratio: 3 / 4;

  .card-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    opacity: 0;
    transition:
      opacity 0.3s ease,
      transform 0.3s ease;

    &.image-loaded {
      opacity: 1;
    }
  }

  // 图片骨架屏
  .image-skeleton {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s ease-in-out infinite;
    z-index: 1;
  }

  &.media-ar-video {
    aspect-ratio: 9 / 16;
  }

  &.media-ar-1-1 {
    aspect-ratio: 1 / 1;
  }

  &.media-ar-4-5 {
    aspect-ratio: 4 / 5;
  }

  &.media-ar-3-4 {
    aspect-ratio: 3 / 4;
  }

  &.media-ar-5-7 {
    aspect-ratio: 5 / 7;
  }

  // 悬停遮罩层（小红书风格）
  .card-hover-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(197, 163, 255, 0.4), rgba(248, 180, 217, 0.4));
    backdrop-filter: blur(3px);
    opacity: 0;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 10;

    .hover-actions {
      display: flex;
      gap: 12px;
      transform: scale(0.8);
      transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);

      .action-btn {
        padding: 10px 20px;
        border: none;
        border-radius: 48px;
        background: rgba(255, 255, 255, 0.95);
        color: #c5a3ff;
        font-size: 14px;
        font-weight: 500;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 6px;
        transition: all 0.2s ease;
        box-shadow: 0 4px 12px rgba(197, 163, 255, 0.2);

        .el-icon {
          font-size: 16px;
        }

        &:hover {
          background: #fff;
          transform: scale(1.05);
          box-shadow: 0 6px 16px rgba(197, 163, 255, 0.3);
          color: #f8b4d9;
        }

        &:active {
          transform: scale(0.95);
        }
      }
    }
  }

  &:hover .card-hover-overlay {
    opacity: 1;

    .hover-actions {
      transform: scale(1);
    }
  }
}

.card-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  vertical-align: top;
  background: #000;
  transition: transform 0.3s ease;

  .video-container:hover & {
    transform: scale(1.02);
  }
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  opacity: 0;
  transition:
    opacity 0.3s ease,
    transform 0.3s ease;

  .card-media:hover & {
    transform: scale(1.05);
  }
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
    background: linear-gradient(135deg, rgba(197, 163, 255, 0.3), rgba(248, 180, 217, 0.3));
    opacity: 0;
    transition: opacity 0.3s ease;

    &:hover {
      opacity: 1;
    }

    .play-icon {
      font-size: 56px;
      color: white;
      filter: drop-shadow(0 4px 12px rgba(197, 163, 255, 0.4));
      transform: scale(0.9);
      transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }

    &:hover .play-icon {
      transform: scale(1);
    }
  }

  .video-progress {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 8px 12px;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.6), transparent);
    opacity: 0;
    transition: opacity 0.3s ease;

    .card-media:hover & {
      opacity: 1;
    }

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
        background: linear-gradient(90deg, #c5a3ff, #f8b4d9);
        border-radius: 2px;
        transition: width 0.1s linear;
      }
    }
  }

  .video-badge {
    position: absolute;
    top: 12px;
    right: 12px;
    padding: 6px 12px;
    background: linear-gradient(135deg, rgba(197, 163, 255, 0.8), rgba(248, 180, 217, 0.8));
    backdrop-filter: blur(8px);
    border-radius: 48px;
    color: white;
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 4px;
    box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
    transition: all 0.3s ease;

    .el-icon {
      font-size: 14px;
    }

    .card-media:hover & {
      transform: scale(1.05);
      background: linear-gradient(135deg, rgba(197, 163, 255, 0.95), rgba(248, 180, 217, 0.95));
    }
  }
}

.xhs-card-body {
  padding: 12px 12px 14px;
  transition: background-color 0.2s ease;

  .xhs-note-card:hover & {
    background-color: #fafafa;
  }
}

.xhs-title {
  font-size: 14px;
  font-weight: 500;
  color: #4a4a6a;
  margin: 0 0 10px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: calc(1.5em * 2);
  transition: color 0.2s ease;

  .xhs-note-card:hover & {
    color: #c5a3ff;
  }
}

.xhs-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.xhs-author {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  transition: opacity 0.2s ease;

  .xhs-note-card:hover & {
    opacity: 0.8;
  }
}

.author-avatar-img {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  transition: transform 0.2s ease;

  .xhs-author:hover & {
    transform: scale(1.1);
  }
}

.xhs-name {
  font-size: 12px;
  color: #8a7a9a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s ease;

  .xhs-note-card:hover & {
    color: #6a5a7a;
  }
}

.xhs-like {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 6px 12px;
  margin: 0;
  border: none;
  border-radius: 48px;
  background: transparent;
  font-size: 12px;
  color: #999;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  .el-icon {
    font-size: 14px;
    transition: transform 0.2s ease;
  }

  &:hover {
    color: #c5a3ff;
    background: rgba(197, 163, 255, 0.1);
    transform: scale(1.05);

    .el-icon {
      transform: scale(1.1);
    }
  }

  &:active {
    transform: scale(0.95);
  }

  &.liked {
    color: #c5a3ff;
    background: rgba(197, 163, 255, 0.15);

    .el-icon {
      animation: heartBeat 0.5s ease;
    }
  }
}
.xhs-favorite {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: none;
  border-radius: 48px;
  background: transparent;
  font-size: 12px;
  color: #999;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(197, 163, 255, 0.1);
    transform: scale(1.05);
  }

  &.favorited {
    color: #f5a623;
    background: rgba(245, 166, 35, 0.1);
  }
}
// 加载更多
.loading-more,
.no-more {
  text-align: center;
  padding: 24px;
  color: #b0a7c0;
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
  width: 56px;
  height: 56px;
  // 使用更醒目的颜色
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: 2px solid rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.25);
  transition: all 0.3s ease;
  z-index: 9999; // 提高 z-index 确保在最上层

  // 添加脉冲动画吸引注意
  animation: pulse-ring 2s infinite;

  // 添加一个外发光效果
  &::before {
    content: '';
    position: absolute;
    top: -4px;
    left: -4px;
    right: -4px;
    bottom: -4px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 50%;
    opacity: 0.3;
    z-index: -1;
    transition: opacity 0.3s ease;
  }

  // 内圈光晕
  &::after {
    content: '';
    position: absolute;
    top: 2px;
    left: 2px;
    right: 2px;
    bottom: 2px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.2), rgba(118, 75, 162, 0.2));
    border-radius: 50%;
    z-index: -1;
  }

  &:hover {
    transform: translateY(-4px) scale(1.1);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.35);
    background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);

    &::before {
      opacity: 0.6;
    }
  }

  &:active {
    transform: translateY(0px) scale(0.95);
  }

  .el-icon {
    font-size: 24px;
    margin-bottom: 2px;
  }

  span {
    font-size: 11px;
    margin-top: 2px;
    font-weight: 600;
    letter-spacing: 1px;
  }
}

// 脉冲动画效果
@keyframes pulse-ring {
  0% {
    box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(102, 126, 234, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(102, 126, 234, 0);
  }
}

git config user.email git config user.name .fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

// 详情弹窗 - 现代化动画效果
.detail-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &.dialog-enter-active {
    animation: overlayFadeIn 0.3s ease-out forwards;
  }

  &.dialog-leave-active {
    animation: overlayFadeOut 0.3s ease-in forwards;
  }
}

.detail-dialog-container {
  position: relative;
  width: 90%;
  max-width: 1400px;
  height: 85vh;
  background: linear-gradient(145deg, #ffffff 0%, #fef9ff 100%);
  border-radius: 32px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &.dialog-enter-active {
    animation: modalZoomIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  }

  &.dialog-leave-active {
    animation: modalZoomOut 0.3s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  }
}

@keyframes overlayFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes overlayFadeOut {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

@keyframes modalZoomIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes modalZoomOut {
  from {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
  to {
    opacity: 0;
    transform: scale(0.95) translateY(20px);
  }
}

.detail-dialog-header {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10;

  .dialog-close-btn {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(4px);
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    color: #4a4a6a;

    &:hover {
      background: white;
      transform: scale(1.1);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      color: #c5a3ff;
    }

    &:active {
      transform: scale(0.95);
    }

    svg {
      width: 18px;
      height: 18px;
    }
  }
}

// 弹窗内容样式优化
.detail-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  height: 100%;
  overflow: hidden;
}

.detail-left {
  background: linear-gradient(135deg, #f0e5ff, #fce4ec);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: all 0.3s ease;
}

.detail-video-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;

  .detail-video {
    width: 100%;
    height: 100%;
    object-fit: contain;
    background: transparent;
  }
}

.detail-images {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;

  .image-gallery {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .gallery-image-container {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }

  .gallery-skeleton {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 60px;
    height: 60px;
    border: 3px solid rgba(197, 163, 255, 0.2);
    border-top-color: #c5a3ff;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to {
      transform: translate(-50%, -50%) rotate(360deg);
    }
  }

  .detail-image {
    max-width: 100%;
    max-height: 100%;
    width: auto;
    height: auto;
    object-fit: contain;
    transition:
      opacity 0.3s ease,
      transform 0.3s ease;

    &.image-loading {
      opacity: 0;
    }

    &.image-loaded {
      opacity: 1;
      animation: imageFadeIn 0.5s ease-out;
    }
  }

  @keyframes imageFadeIn {
    from {
      opacity: 0;
      transform: scale(0.98);
    }
    to {
      opacity: 1;
      transform: scale(1);
    }
  }

  .image-nav {
    position: absolute;
    top: 50%;
    left: 0;
    right: 0;
    transform: translateY(-50%);
    display: flex;
    justify-content: space-between;
    padding: 0 20px;
    pointer-events: none;
    z-index: 10;

    .nav-btn {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      border: 2px solid rgba(255, 255, 255, 0.8);
      color: white;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s ease;
      pointer-events: auto;
      backdrop-filter: blur(4px);
      box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);

      &:hover {
        background: linear-gradient(135deg, #f8b4d9, #c5a3ff);
        border-color: white;
        transform: scale(1.1);
        box-shadow: 0 6px 16px rgba(197, 163, 255, 0.4);
      }

      &:active {
        transform: scale(0.95);
      }

      .el-icon {
        font-size: 20px;
      }
    }
  }

  .image-indicator {
    position: absolute;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
    color: white;
    padding: 6px 16px;
    border-radius: 48px;
    font-size: 13px;
    font-weight: 500;
    backdrop-filter: blur(4px);
    z-index: 10;
    box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
    animation: indicatorFadeIn 0.3s ease;
  }

  @keyframes indicatorFadeIn {
    from {
      opacity: 0;
      transform: translateX(-50%) translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateX(-50%) translateY(0);
    }
  }
}

.detail-right {
  padding: 32px;
  overflow-y: auto;
  background: linear-gradient(145deg, #ffffff 0%, #fef9ff 100%);
  scrollbar-width: thin;
  scrollbar-color: #c5a3ff #f0e5ff;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #f0e5ff;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: #c5a3ff;
    border-radius: 3px;

    &:hover {
      background: #f8b4d9;
    }
  }
}

.detail-title {
  font-size: 24px;
  font-weight: bold;
  background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin: 0 0 16px;
  line-height: 1.4;
  animation: slideInFromLeft 0.4s ease-out;
}

@keyframes slideInFromLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.detail-author-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  animation: fadeInUp 0.4s ease-out 0.05s both;
}

.author-avatar-detail {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #f0f0f0;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.1);
  }
}

.author-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e0c3ff, #c5a3ff);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 18px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(197, 163, 255, 0.3);
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.1);
  }
}

.author-name-detail {
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.detail-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  animation: fadeInUp 0.4s ease-out 0.1s both;

  .meta-item {
    font-size: 14px;
    color: #8a7a9a;
  }
}

.detail-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
  flex-wrap: wrap;
  padding: 12px 0;
  border-top: 1px solid rgba(197, 163, 255, 0.12);
  border-bottom: 1px solid rgba(197, 163, 255, 0.12);
  animation: fadeInUp 0.4s ease-out 0.15s both;

  .stat-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    background: rgba(197, 163, 255, 0.08);
    border-radius: 48px;
    font-size: 14px;
    color: #6a4a9c;
    transition: all 0.3s ease;
    cursor: default;

    .el-icon,
    .stat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .stat-label {
      font-size: 12px;
      color: #a09abf;
      margin-left: 2px;
    }

    span:not(.stat-label) {
      font-weight: 600;
      color: #6a4a9c;
    }

    // 点赞按钮样式
    &.like-item {
      cursor: pointer;
      background: rgba(197, 163, 255, 0.1);

      &:hover {
        background: rgba(197, 163, 255, 0.2);
        transform: translateY(-2px);
      }

      &:active {
        transform: translateY(0);
      }

      &.liked {
        background: linear-gradient(135deg, rgba(197, 163, 255, 0.2), rgba(248, 180, 217, 0.2));

        span:not(.stat-label) {
          color: #c5a3ff;
        }

        .liked-icon {
          color: #c5a3ff;
          animation: heartBeat 0.4s ease;
        }
      }
    }

    // 收藏按钮样式
    &.favorite-item {
      cursor: pointer;
      background: rgba(168, 230, 207, 0.1);

      &:hover {
        background: rgba(168, 230, 207, 0.2);
        transform: translateY(-2px);
      }

      &:active {
        transform: translateY(0);
      }

      &.favorited {
        background: linear-gradient(135deg, rgba(168, 230, 207, 0.2), rgba(126, 224, 181, 0.2));

        span:not(.stat-label) {
          color: #2c665a;
        }

        .el-icon {
          color: #2c665a;
          animation: heartBeat 0.4s ease;
        }
      }
    }
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

.detail-body {
  font-size: 15px;
  line-height: 1.8;
  color: #4a4a6a;
  animation: fadeInUp 0.4s ease-out 0.2s both;
  transition: all 0.3s ease;

  :deep(img) {
    max-width: 100%;
    border-radius: 12px;
    margin: 12px 0;
    transition: transform 0.3s ease;

    &:hover {
      transform: scale(1.02);
    }
  }

  :deep(p) {
    margin-bottom: 12px;
  }
}

// 评论区样式优化
.comment-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(197, 163, 255, 0.15);
  animation: fadeInUp 0.4s ease-out 0.25s both;
}

// 在 UserHome.vue 样式末尾添加以下代码

// 评论区样式增强（保留原有渐变文字效果）

// 保留原有的渐变文字样式，只添加子标题样式
.comment-title {
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin: 0 0 20px;
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;

  .comment-subtitle {
    font-size: 13px;
    font-weight: normal;
    background: none;
    -webkit-background-clip: unset;
    background-clip: unset;
    color: #a09abf;
  }
}

// 评论列表容器
.comment-list {
  max-height: 500px;
  overflow-y: auto;
  padding-right: 8px;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: #f0e5ff;
    border-radius: 2px;
  }

  &::-webkit-scrollbar-thumb {
    background: #c5a3ff;
    border-radius: 2px;
  }
}

// 顶部评论输入框样式
.top-comment-input-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;
  padding: 16px;
  background: rgba(197, 163, 255, 0.03);
  border-radius: 20px;
  transition: all 0.3s ease;

  .input-avatar {
    flex-shrink: 0;
  }

  .input-avatar-img {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    object-fit: cover;
    border: 2px solid #c5a3ff;
  }

  .input-avatar-placeholder {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    font-weight: 600;
    color: white;
  }

  .input-content {
    flex: 1;
  }

  .comment-textarea {
    :deep(.el-textarea__inner) {
      border-radius: 16px;
      background: #faf7ff;
      border: 1px solid #f0e5ff;
      transition: all 0.3s ease;

      &:focus {
        border-color: #c5a3ff;
        box-shadow: 0 0 0 3px rgba(197, 163, 255, 0.1);
      }
    }
  }

  .comment-actions-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;

    .comment-tip {
      font-size: 11px;
      color: #b0a7c0;
    }

    .submit-comment-btn {
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      border: none;
      border-radius: 48px;
      padding: 8px 24px;
      font-weight: 500;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
      }
    }
  }
}

// 无评论状态
.no-comments {
  text-align: center;
  padding: 48px 20px;

  .no-comments-emoji {
    font-size: 48px;
    display: block;
    margin-bottom: 12px;
    opacity: 0.5;
  }

  p {
    font-size: 14px;
    color: #b0a7c0;
    margin: 0;
  }
}

//爱心
// 爱心图标通用样式
.like-icon {
  width: 16px;
  height: 16px;
  transition: all 0.2s ease;
}

.liked-icon {
  color: #c5a3ff;
  animation: heartBeat 0.4s ease;
}

// 笔记卡片点赞按钮样式增强
.xhs-like {
  .like-icon {
    width: 14px;
    height: 14px;
  }

  &.liked {
    .liked-icon {
      color: #c5a3ff;
    }
  }
}

// 弹窗点赞统计样式增强
.stat-badge.like-badge {
  .like-icon {
    width: 14px;
    height: 14px;
    margin-right: 4px;
  }

  &.liked {
    .liked-icon {
      color: white;
    }
  }
}

// 心跳动画
@keyframes heartBeat {
  0% {
    transform: scale(1);
  }
  30% {
    transform: scale(1.3);
  }
  60% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}
.follow-btn {
  padding: 6px 20px;
  border-radius: 48px;
  border: none;
  background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
  color: white;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-left: auto;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
  }

  &.following {
    background: transparent;
    border: 1px solid #c5a3ff;
    color: #c5a3ff;

    &:hover {
      background: rgba(197, 163, 255, 0.1);
    }
  }
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #a09abf;

  span {
    cursor: default;
  }
}
</style>

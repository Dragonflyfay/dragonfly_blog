<script setup>
import { ref, computed, reactive, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User,
  EditPen,
  Plus,
  Star,
  ChatDotRound,
  View,
  Clock,
  Collection,
  ArrowLeft,
  ArrowRight,
} from '@element-plus/icons-vue'
import useUserInfoStore from '@/stores/userInfo.js' //引入用户信息store：获取用户信息

import avatar from '@/assets/default.png'
import logoImg from '@/assets/logo.png'
import {
  notePageListService, //获取笔记列表
  batchCheckNoteLikedService, //批量获取笔记是否被当前用户点赞
  batchCheckNoteFavoritedService, //批量获取笔记是否被当前用户收藏
  likeNoteService, //点赞笔记
  unlikeNoteService, //取消点赞笔记
  favoriteNoteService, //收藏笔记
  unfavoriteNoteService, //取消收藏笔记
  noteDetailService, //获取笔记详情
  recordViewService, //记录笔记浏览量
  getCommentsByNoteIdService, //获取笔记下的评论
  addCommentService, //添加评论
  likeCommentService, //点赞评论
  unlikeCommentService, //取消点赞评论
  batchCheckCommentLikedService, //批量获取评论是否被当前用户点赞
  batchCheckFollowService, //批量获取用户是否关注
  followUserService, //关注用户
  unfollowUserService, //取消关注用户
} from '@/api/note.js'
import { getFollowersService, getFollowingService } from '@/api/note.js' //获取用户关注/粉丝
import request from '@/utils/request.js' //请求封装
import CommentItem from '@/components/CommentItem.vue' //评论组件

const router = useRouter() //路由
const userInfoStore = useUserInfoStore() //用户信息store：获取用户信息

// 当前选中的标签页
const activeTab = ref('mynotes') //默认我的笔记

// 我的笔记
const myNotes = ref([]) //我的笔记
const myNotesLoading = ref(false) //是否正在加载笔记
const myNotesPage = ref({ pageNum: 1, pageSize: 12, total: 0 })

// 我的收藏
const favoriteNotes = ref([])
const favoriteNotesLoading = ref(false)

// 我的喜欢
const likedNotes = ref([])
const likedNotesLoading = ref(false)

// 用户统计
const stats = ref({
  notesCount: 0,
  favoritesCount: 0,
  likesCount: 0,
  followersCount: 0,
  followingCount: 0,
})

// 点赞/收藏状态
const likedNoteIds = ref(new Set())
const favoritedNoteIds = ref(new Set())

// 标记哪些tab已加载过
const tabLoaded = ref({ mynotes: false, favorites: false, likes: false })

const showDetailDialog = ref(false)
const currentNote = ref(null)
const currentImageIndex = ref(0)
const detailLoading = ref(false)
const dialogAnimation = ref('')
const imageLoaded = ref(true)
const galleryImageLoading = ref(false)
const dialogVideoPlayer = ref(null)

const comments = ref([])
const commentContent = ref('')
const submittingComment = ref(false)
const likedComments = reactive({})
const replyToComment = ref(null)
const replyingCommentId = ref(null)
const replyInputContentMap = ref(new Map())
const followingStatus = reactive({})

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

// ==================== 数据加载 ====================

// 格式化笔记数据（剥离大字段，只保留列表展示需要的内容）
const formatNoteForList = (note) => {
  // 截取纯文本摘要，不保留完整 HTML
  const plainText = note.content?.replace(/<[^>]+>/g, '').substring(0, 80) || ''
  return {
    id: note.id,
    title: note.title,
    excerpt: plainText,
    coverImg: note.coverImg,
    noteCategory: note.noteCategory,
    userName: note.createUserName || note.userName || '匿名用户',
    userPic: note.createUserAvatar || '',
    viewsCount: note.viewsCount || 0,
    commentsCount: note.commentsCount || 0,
    likesCount: note.likesCount || 0,
    favoritesCount: note.favoritesCount || 0,
    createTime: note.createTime,
    imageLoaded: false,
    // 保留原始数据引用（用于后续可能的详情查看）
    _raw: null,
  }
}

// 加载我的笔记
const loadMyNotes = async (reset = true) => {
  if (myNotesLoading.value) return
  myNotesLoading.value = true
  try {
    if (reset) myNotesPage.value.pageNum = 1
    const userId = userInfoStore.info.id
    const res = await notePageListService({
      userId,
      pageNum: myNotesPage.value.pageNum,
      pageSize: myNotesPage.value.pageSize,
    })
    const items = (res.data?.items || []).map(formatNoteForList)
    if (items.length > 0) {
      console.log('[DEBUG] loadMyNotes 第一条原始数据:', JSON.stringify(res.data?.items[0], null, 2))
      console.log('[DEBUG] loadMyNotes 第一条格式化后:', JSON.stringify(items[0], null, 2))
    }

    if (reset) {
      myNotes.value = items
    } else {
      myNotes.value.push(...items)
    }

    myNotesPage.value.total = res.data?.total || 0
    stats.value.notesCount = myNotesPage.value.total

    // 异步加载状态（不阻塞渲染）
    refreshLikeAndFavStatus(items)
  } catch (e) {
    console.error('加载我的笔记失败:', e)
  } finally {
    myNotesLoading.value = false
  }
}

// 加载更多笔记
const loadMoreMyNotes = () => {
  if (myNotes.value.length >= myNotesPage.value.total) return
  myNotesPage.value.pageNum++
  loadMyNotes(false)
}

// 加载我的收藏
const loadFavoriteNotes = async () => {
  if (favoriteNotesLoading.value) return
  favoriteNotesLoading.value = true
  try {
    const favRes = await request.get('/favorite/notes')
    const noteIds = favRes.data || []
    stats.value.favoritesCount = noteIds.length

    if (noteIds.length === 0) {
      favoriteNotes.value = []
      return
    }

    // 只取最近12条
    const recentIds = noteIds.slice(0, 12)
    const params = recentIds.map((id) => `ids=${id}`).join('&')
    const notesRes = await request.get(`/note/byIds?${params}`)
    favoriteNotes.value = (notesRes.data || []).map(formatNoteForList)

    refreshLikeAndFavStatus(favoriteNotes.value)
  } catch (e) {
    console.error('加载收藏笔记失败:', e)
  } finally {
    favoriteNotesLoading.value = false
  }
}

// 加载我的喜欢
const loadLikedNotes = async () => {
  if (likedNotesLoading.value) return
  likedNotesLoading.value = true
  try {
    const likeRes = await request.get('/like/notes')
    const noteIds = likeRes.data || []
    stats.value.likesCount = noteIds.length

    if (noteIds.length === 0) {
      likedNotes.value = []
      return
    }

    // 喜欢的笔记默认已点赞
    noteIds.forEach((id) => likedNoteIds.value.add(id))

    // 只取最近12条
    const recentIds = noteIds.slice(0, 12)
    const params = recentIds.map((id) => `ids=${id}`).join('&')
    const notesRes = await request.get(`/note/byIds?${params}`)
    likedNotes.value = (notesRes.data || []).map(formatNoteForList)

    refreshLikeAndFavStatus(likedNotes.value)
  } catch (e) {
    console.error('加载喜欢的笔记失败:', e)
  } finally {
    likedNotesLoading.value = false
  }
}

// 刷新点赞和收藏状态（后台执行，不阻塞）
const refreshLikeAndFavStatus = async (notes) => {
  const ids = notes.map((n) => n.id).filter(Boolean)
  if (ids.length === 0) return
  try {
    const [likeRes, favRes] = await Promise.all([
      batchCheckNoteLikedService(ids),
      batchCheckNoteFavoritedService(ids),
    ])
    if (likeRes.data) {
      Object.entries(likeRes.data).forEach(([id, liked]) => {
        if (liked) likedNoteIds.value.add(Number(id))
        else likedNoteIds.value.delete(Number(id))
      })
    }
    if (favRes.data) {
      Object.entries(favRes.data).forEach(([id, fav]) => {
        if (fav) favoritedNoteIds.value.add(Number(id))
        else favoritedNoteIds.value.delete(Number(id))
      })
    }
  } catch (e) {
    // 静默失败，不影响主流程
  }
}

const getLoadedNoteLists = () => [myNotes.value, favoriteNotes.value, likedNotes.value]

const syncNoteById = (noteId, patch) => {
  getLoadedNoteLists().forEach((list) => {
    list.forEach((item) => {
      if (item.id === noteId) Object.assign(item, patch)
    })
  })
  if (currentNote.value?.id === noteId) {
    Object.assign(currentNote.value, patch)
  }
}

const normalizeImages = (images) => {
  if (!images) return []
  if (Array.isArray(images)) return images.filter(Boolean)
  if (typeof images === 'string') {
    try {
      const parsed = JSON.parse(images)
      if (Array.isArray(parsed)) return parsed.filter(Boolean)
    } catch (e) {
      // Some APIs return comma separated image urls.
    }
    return images
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }
  return []
}

const formatNoteForDetail = (note, fallback = {}) => {
  const listNote = formatNoteForList({ ...fallback, ...note })
  const images = normalizeImages(note.images || fallback.images)
  return {
    ...listNote,
    ...note,
    content: note.content || fallback.content || '',
    images,
    video: note.video || fallback.video || '',
    location: note.location || fallback.location || '',
    publishTime: note.publishTime || fallback.publishTime || '',
    coverImg: note.coverImg || fallback.coverImg || images[0] || '',
    userName: note.createUserName || note.userName || fallback.userName || '匿名用户',
    userPic:
      note.createUserAvatar || note.userPic || note.createUser?.userPic || fallback.userPic || '',
  }
}

const onImgError = (e) => {
  const el = e.target
  if (el && el.src !== logoImg) {
    el.src = logoImg
  }
}

const onImageLoad = () => {
  imageLoaded.value = true
  galleryImageLoading.value = false
}

const onImageLoadStart = () => {
  galleryImageLoading.value = true
  imageLoaded.value = false
}

const prevImage = () => {
  if (!currentNote.value?.images?.length) return
  galleryImageLoading.value = true
  imageLoaded.value = false
  currentImageIndex.value =
    (currentImageIndex.value - 1 + currentNote.value.images.length) %
    currentNote.value.images.length
}

const nextImage = () => {
  if (!currentNote.value?.images?.length) return
  galleryImageLoading.value = true
  imageLoaded.value = false
  currentImageIndex.value = (currentImageIndex.value + 1) % currentNote.value.images.length
}

const handleVideoError = (e) => {
  const video = e.target
  const errorCode = video.error?.code || 'unknown'
  const retryCount = parseInt(video.dataset.retryCount) || 0

  if (
    (errorCode === MediaError.MEDIA_ERR_NETWORK ||
      errorCode === MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED) &&
    retryCount < 2
  ) {
    video.dataset.retryCount = String(retryCount + 1)
    const originalSrc = video.src.split('?')[0]
    video.src = `${originalSrc}?t=${Date.now()}`
    video.load()
    return
  }

  if (retryCount >= 2) {
    ElMessage.error({ message: '视频加载失败，已自动重试仍无法播放', duration: 4000 })
  }
}

const handleDialogVideoLoaded = (e) => {
  const video = e.target
  if (video.readyState >= 2) {
    video.currentTime = 0
  }
}

const loadComments = async (noteId) => {
  try {
    const res = await getCommentsByNoteIdService(noteId)
    comments.value = res.data || []

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

    if (allCommentIds.length > 0) {
      const likeRes = await batchCheckCommentLikedService(allCommentIds)
      Object.assign(likedComments, likeRes.data)
    }
  } catch (error) {
    console.error('加载评论失败:', error)
    ElMessage.error('加载评论失败')
  }
}

const submitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  if (!currentNote.value) return

  submittingComment.value = true
  try {
    await addCommentService({
      noteId: currentNote.value.id,
      content: commentContent.value,
      parentId: replyToComment.value ? replyToComment.value.id : 0,
      replyToUserId: replyToComment.value ? replyToComment.value.userId : null,
    })
    commentContent.value = ''
    replyToComment.value = null
    await loadComments(currentNote.value.id)
    const commentsCount = (currentNote.value.commentsCount || 0) + 1
    syncNoteById(currentNote.value.id, { commentsCount })
    ElMessage.success('评论成功')
  } catch (error) {
    console.error('评论失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submittingComment.value = false
  }
}

const handleReplyComment = (comment) => {
  if (replyingCommentId.value === comment.id) {
    replyingCommentId.value = null
    return
  }

  replyingCommentId.value = comment.id
  if (!replyInputContentMap.value.has(comment.id)) {
    replyInputContentMap.value.set(
      comment.id,
      `@${comment.nickname || comment.username || '匿名用户'} `,
    )
  }

  nextTick(() => {
    const replyInput = document.querySelector(
      `.comment-item-wrapper[data-comment-id="${comment.id}"] .reply-input-container .el-textarea__inner`,
    )
    replyInput?.focus()
  })
}

const cancelReplyInline = (commentId) => {
  if (replyingCommentId.value === commentId) {
    replyingCommentId.value = null
  }
}

const updateReplyContent = (commentId, content) => {
  replyInputContentMap.value.set(commentId, content)
}

const submitReply = async (parentCommentId) => {
  const replyContent = replyInputContentMap.value.get(parentCommentId) || ''
  if (!replyContent.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  if (!currentNote.value) return

  const findCommentById = (list, id) => {
    for (const comment of list) {
      if (comment.id === id) return comment
      if (comment.children?.length) {
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
    await addCommentService({
      noteId: currentNote.value.id,
      content: replyContent,
      parentId: parentCommentId,
      replyToUserId: parentComment.userId,
    })
    replyingCommentId.value = null
    replyInputContentMap.value.delete(parentCommentId)
    await loadComments(currentNote.value.id)
    const commentsCount = (currentNote.value.commentsCount || 0) + 1
    syncNoteById(currentNote.value.id, { commentsCount })
    ElMessage.success('回复成功')
  } catch (error) {
    console.error('回复失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submittingComment.value = false
  }
}

const toggleCommentLike = async (comment, event) => {
  event?.stopPropagation()
  const commentId = comment.id
  const currentLiked = likedComments[commentId] || false
  const originalLikesCount = comment.likesCount || 0

  likedComments[commentId] = !currentLiked
  comment.likesCount = currentLiked ? Math.max(0, originalLikesCount - 1) : originalLikesCount + 1

  try {
    if (currentLiked) {
      await unlikeCommentService(commentId)
    } else {
      await likeCommentService(commentId)
    }
  } catch (error) {
    likedComments[commentId] = currentLiked
    comment.likesCount = originalLikesCount
    ElMessage.error(error.response?.data?.message || '操作失败，请重试')
  }
}

const checkFollowingStatus = async (userIds) => {
  if (!userIds || userIds.length === 0) return
  try {
    const res = await batchCheckFollowService(userIds)
    Object.assign(followingStatus, res.data)
  } catch (error) {
    console.warn('批量检查关注状态失败:', error)
  }
}

const toggleFollow = async (userId) => {
  try {
    if (followingStatus[userId]) {
      await unfollowUserService(userId)
      followingStatus[userId] = false
      if (currentNote.value?.createUser === userId) {
        currentNote.value.followersCount = Math.max(0, (currentNote.value.followersCount || 0) - 1)
      }
      ElMessage.success('已取消关注')
    } else {
      await followUserService(userId)
      followingStatus[userId] = true
      if (currentNote.value?.createUser === userId) {
        currentNote.value.followersCount = (currentNote.value.followersCount || 0) + 1
      }
      ElMessage.success('关注成功')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 加载用户统计（后台执行）
const loadStats = async () => {
  try {
    const userId = userInfoStore.info.id
    const [followersRes, followingRes] = await Promise.all([
      getFollowersService(userId),
      getFollowingService(userId),
    ])
    stats.value.followersCount = (followersRes.data || []).length
    stats.value.followingCount = (followingRes.data || []).length
  } catch (e) {
    // 静默失败
  }
}

// 切换标签页
const onTabChange = (tab) => {
  if (tab === 'mynotes' && !tabLoaded.value.mynotes) {
    tabLoaded.value.mynotes = true
    loadMyNotes()
  } else if (tab === 'favorites') {
    // 每次都重新加载，保证收藏列表与最新操作同步
    loadFavoriteNotes()
  } else if (tab === 'likes') {
    // 每次都重新加载，保证喜欢列表与最新操作同步
    loadLikedNotes()
  }
}

// ==================== 交互操作 ====================

// 点赞/取消点赞
const toggleLike = async (note, event) => {
  event?.stopPropagation()
  try {
    const wasLiked = likedNoteIds.value.has(note.id)
    if (likedNoteIds.value.has(note.id)) {
      await unlikeNoteService(note.id)
      likedNoteIds.value.delete(note.id)
      const likesCount = Math.max(0, (note.likesCount || 1) - 1)
      syncNoteById(note.id, { likesCount })
      // 从喜欢列表中移除（无论当前在哪个tab，保持数据一致性）
      likedNotes.value = likedNotes.value.filter((n) => n.id !== note.id)
      if (wasLiked) stats.value.likesCount = Math.max(0, stats.value.likesCount - 1)
      ElMessage.success('已取消点赞')
    } else {
      await likeNoteService(note.id)
      likedNoteIds.value.add(note.id)
      const likesCount = (note.likesCount || 0) + 1
      syncNoteById(note.id, { likesCount })
      stats.value.likesCount += 1
      ElMessage.success('点赞成功')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 收藏/取消收藏
const toggleFavorite = async (note, event) => {
  event?.stopPropagation()
  try {
    const wasFavorited = favoritedNoteIds.value.has(note.id)
    if (favoritedNoteIds.value.has(note.id)) {
      await unfavoriteNoteService(note.id)
      favoritedNoteIds.value.delete(note.id)
      const favoritesCount = Math.max(0, (note.favoritesCount || 1) - 1)
      syncNoteById(note.id, { favoritesCount })
      // 从收藏列表中移除（无论当前在哪个tab，保持数据一致性）
      favoriteNotes.value = favoriteNotes.value.filter((n) => n.id !== note.id)
      if (wasFavorited) stats.value.favoritesCount = Math.max(0, stats.value.favoritesCount - 1)
      ElMessage.success('已取消收藏')
    } else {
      await favoriteNoteService(note.id)
      favoritedNoteIds.value.add(note.id)
      const favoritesCount = (note.favoritesCount || 0) + 1
      syncNoteById(note.id, { favoritesCount })
      stats.value.favoritesCount += 1
      ElMessage.success('收藏成功')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 查看笔记详情
const viewNote = async (note) => {
  currentNote.value = formatNoteForDetail(note)
  currentImageIndex.value = 0
  showDetailDialog.value = true
  dialogAnimation.value = 'dialog-enter-active'
  imageLoaded.value = false
  galleryImageLoading.value = false
  comments.value = []
  commentContent.value = ''
  replyingCommentId.value = null
  replyInputContentMap.value = new Map()
  detailLoading.value = true
  try {
    const res = await noteDetailService(note.id)
    console.log('[DEBUG] noteDetailService 返回:', JSON.stringify(res, null, 2))
    if (res.data) {
      console.log('[DEBUG] res.data.createUserName:', res.data.createUserName)
      console.log('[DEBUG] res.data.createUser:', res.data.createUser)
      currentNote.value = formatNoteForDetail(res.data, note)
      syncNoteById(note.id, {
        likesCount: currentNote.value.likesCount || 0,
        favoritesCount: currentNote.value.favoritesCount || 0,
        commentsCount: currentNote.value.commentsCount || 0,
        viewsCount: currentNote.value.viewsCount || 0,
      })
      refreshLikeAndFavStatus([currentNote.value])
      if (currentNote.value.createUser && currentNote.value.createUser !== userInfoStore.info.id) {
        checkFollowingStatus([currentNote.value.createUser])
      }
    }
    try {
      await recordViewService(note.id)
      const viewsCount = (currentNote.value.viewsCount || 0) + 1
      syncNoteById(note.id, { viewsCount })
    } catch (e) {
      // Viewing should still work even if view counting fails.
    }
    await loadComments(note.id)
  } catch (e) {
    ElMessage.error('加载笔记详情失败')
  } finally {
    detailLoading.value = false
  }
}

const closeDialog = () => {
  dialogAnimation.value = 'dialog-leave-active'
  setTimeout(() => {
    showDetailDialog.value = false
    currentNote.value = null
    currentImageIndex.value = 0
    imageLoaded.value = false
    galleryImageLoading.value = false
    comments.value = []
    commentContent.value = ''
    replyingCommentId.value = null
    replyInputContentMap.value = new Map()
    dialogVideoPlayer.value?.pause()
    dialogAnimation.value = ''
  }, 300)
}

// 去发布页
const goPublish = () => {
  router.push('/publish')
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// 预加载数量（后台静默执行，不阻塞渲染）
const preloadCounts = async () => {
  try {
    const [favRes, likeRes] = await Promise.all([
      request.get('/favorite/notes'),
      request.get('/like/notes'),
    ])
    stats.value.favoritesCount = (favRes.data || []).length
    stats.value.likesCount = (likeRes.data || []).length
  } catch (e) {
    // 静默失败
  }
}

// ==================== 初始化 ====================
onMounted(async () => {
  tabLoaded.value.mynotes = true
  // 先加载笔记列表（主要数据），其他后台异步加载
  loadMyNotes()
  // 统计和数量不阻塞渲染
  loadStats()
  preloadCounts()
})
</script>

<template>
  <div class="me-container">
    <!-- 用户信息卡片 -->
    <div class="profile-card">
      <div class="profile-bg"></div>
      <div class="profile-content">
        <div class="profile-avatar-wrap">
          <el-avatar
            :src="userInfoStore.info.userPic || avatar"
            :size="80"
            class="profile-avatar"
          />
          <div class="avatar-ring"></div>
        </div>
        <div class="profile-info">
          <h2 class="profile-nickname">
            {{ userInfoStore.info.nickname || userInfoStore.info.username }}
          </h2>
          <p class="profile-bio">{{ userInfoStore.info.bio || '这个人很懒，什么都没写~' }}</p>
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-num">{{ stats.notesCount }}</span>
              <span class="stat-label">笔记</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-num">{{ stats.favoritesCount }}</span>
              <span class="stat-label">收藏</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-num">{{ stats.followersCount }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-num">{{ stats.followingCount }}</span>
              <span class="stat-label">关注</span>
            </div>
          </div>
        </div>
        <div class="profile-actions">
          <el-button class="edit-btn" @click="router.push('/user/info')">
            <el-icon><EditPen /></el-icon>编辑资料
          </el-button>
        </div>
      </div>
    </div>

    <!-- 标签页切换 -->
    <div class="tabs-bar">
      <div
        class="tab-item"
        :class="{ active: activeTab === 'mynotes' }"
        @click="((activeTab = 'mynotes'), onTabChange('mynotes'))"
      >
        <el-icon><EditPen /></el-icon>
        <span>我的笔记</span>
        <span class="tab-count">{{ stats.notesCount }}</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'favorites' }"
        @click="((activeTab = 'favorites'), onTabChange('favorites'))"
      >
        <el-icon><Star /></el-icon>
        <span>我的收藏</span>
        <span class="tab-count">{{ stats.favoritesCount }}</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'likes' }"
        @click="((activeTab = 'likes'), onTabChange('likes'))"
      >
        <span>❤️</span>
        <span>我的喜欢</span>
        <span class="tab-count">{{ stats.likesCount }}</span>
      </div>
    </div>

    <!-- 笔记列表 -->
    <div class="notes-section">
      <!-- 我的笔记 -->
      <div v-if="activeTab === 'mynotes'" v-loading="myNotesLoading" class="notes-grid">
        <div v-if="myNotes.length === 0 && !myNotesLoading" class="empty-state">
          <div class="empty-icon">📝</div>
          <p class="empty-title">还没有发布过笔记</p>
          <p class="empty-desc">记录生活中的美好瞬间，分享你的独特视角</p>
          <el-button class="publish-btn-empty" @click="goPublish">
            <el-icon><Plus /></el-icon>发布第一篇笔记
          </el-button>
        </div>
        <div v-for="note in myNotes" :key="note.id" class="note-card" @click="viewNote(note)">
          <div class="note-cover">
            <img
              v-if="note.coverImg"
              :src="note.coverImg"
              :alt="note.title"
              class="cover-img"
              @load="note.imageLoaded = true"
            />
            <div v-else class="cover-placeholder">
              <img :src="logoImg" class="cover-logo" alt="logo" />
            </div>
            <div class="note-category-tag" v-if="note.noteCategory">
              {{ note.noteCategory === 'video' ? '视频' : '图文' }}
            </div>
          </div>
          <div class="note-body">
            <h3 class="note-title">{{ note.title }}</h3>
            <p class="note-excerpt">{{ note.excerpt || '暂无内容' }}</p>
            <div class="note-meta">
              <span class="meta-item"
                ><el-icon><View /></el-icon>{{ note.viewsCount || 0 }}</span
              >
              <span class="meta-item"
                ><el-icon><ChatDotRound /></el-icon>{{ note.commentsCount || 0 }}</span
              >
              <span class="meta-item"
                ><el-icon><Clock /></el-icon>{{ formatDate(note.createTime) }}</span
              >
            </div>
            <div class="note-actions" @click.stop>
              <span
                class="action-btn"
                :class="{ active: likedNoteIds.has(note.id) }"
                @click="toggleLike(note)"
              >
                {{ likedNoteIds.has(note.id) ? '❤️' : '🤍' }} {{ note.likesCount || 0 }}
              </span>
              <span
                class="action-btn"
                :class="{ active: favoritedNoteIds.has(note.id) }"
                @click="toggleFavorite(note)"
              >
                {{ favoritedNoteIds.has(note.id) ? '⭐' : '☆' }} {{ note.favoritesCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div
        v-if="activeTab === 'mynotes' && myNotes.length < myNotesPage.total"
        class="load-more-wrap"
      >
        <el-button class="load-more-btn" :loading="myNotesLoading" @click="loadMoreMyNotes">
          加载更多 ({{ myNotes.length }}/{{ myNotesPage.total }})
        </el-button>
      </div>

      <!-- 我的收藏 -->
      <div v-if="activeTab === 'favorites'" v-loading="favoriteNotesLoading" class="notes-grid">
        <div v-if="favoriteNotes.length === 0 && !favoriteNotesLoading" class="empty-state">
          <div class="empty-icon">⭐</div>
          <p class="empty-title">还没有收藏过笔记</p>
          <p class="empty-desc">去发现页逛逛，收藏你喜欢的精彩内容</p>
          <el-button class="publish-btn-empty" @click="router.push('/home')">
            <el-icon><View /></el-icon>去发现
          </el-button>
        </div>
        <div v-for="note in favoriteNotes" :key="note.id" class="note-card" @click="viewNote(note)">
          <div class="note-cover">
            <img
              v-if="note.coverImg"
              :src="note.coverImg"
              :alt="note.title"
              class="cover-img"
              @load="note.imageLoaded = true"
            />
            <div v-else class="cover-placeholder">
              <img :src="logoImg" class="cover-logo" alt="logo" />
            </div>
            <div class="note-category-tag" v-if="note.noteCategory">
              {{ note.noteCategory === 'video' ? '视频' : '图文' }}
            </div>
          </div>
          <div class="note-body">
            <h3 class="note-title">{{ note.title }}</h3>
            <p class="note-excerpt">{{ note.excerpt || '暂无内容' }}</p>
            <div class="note-author-row">
              <span class="author-name-small">{{ note.userName }}</span>
            </div>
            <div class="note-meta">
              <span class="meta-item"
                ><el-icon><View /></el-icon>{{ note.viewsCount || 0 }}</span
              >
              <span class="meta-item"
                ><el-icon><ChatDotRound /></el-icon>{{ note.commentsCount || 0 }}</span
              >
              <span class="meta-item"
                ><el-icon><Clock /></el-icon>{{ formatDate(note.createTime) }}</span
              >
            </div>
            <div class="note-actions" @click.stop>
              <span
                class="action-btn"
                :class="{ active: likedNoteIds.has(note.id) }"
                @click="toggleLike(note)"
              >
                {{ likedNoteIds.has(note.id) ? '❤️' : '🤍' }} {{ note.likesCount || 0 }}
              </span>
              <span class="action-btn active" @click="toggleFavorite(note)">
                ⭐ {{ note.favoritesCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 我的喜欢 -->
      <div v-if="activeTab === 'likes'" v-loading="likedNotesLoading" class="notes-grid">
        <div v-if="likedNotes.length === 0 && !likedNotesLoading" class="empty-state">
          <div class="empty-icon">❤️</div>
          <p class="empty-title">还没有点赞过笔记</p>
          <p class="empty-desc">去发现页逛逛，为你喜欢的内容点个赞吧</p>
          <el-button class="publish-btn-empty" @click="router.push('/home')">
            <el-icon><View /></el-icon>去发现
          </el-button>
        </div>
        <div v-for="note in likedNotes" :key="note.id" class="note-card" @click="viewNote(note)">
          <div class="note-cover">
            <img
              v-if="note.coverImg"
              :src="note.coverImg"
              :alt="note.title"
              class="cover-img"
              @load="note.imageLoaded = true"
            />
            <div v-else class="cover-placeholder">
              <img :src="logoImg" class="cover-logo" alt="logo" />
            </div>
            <div class="note-category-tag" v-if="note.noteCategory">
              {{ note.noteCategory === 'video' ? '视频' : '图文' }}
            </div>
          </div>
          <div class="note-body">
            <h3 class="note-title">{{ note.title }}</h3>
            <p class="note-excerpt">{{ note.excerpt || '暂无内容' }}</p>
            <div class="note-author-row">
              <span class="author-name-small">{{ note.userName }}</span>
            </div>
            <div class="note-meta">
              <span class="meta-item"
                ><el-icon><View /></el-icon>{{ note.viewsCount || 0 }}</span
              >
              <span class="meta-item"
                ><el-icon><ChatDotRound /></el-icon>{{ note.commentsCount || 0 }}</span
              >
              <span class="meta-item"
                ><el-icon><Clock /></el-icon>{{ formatDate(note.createTime) }}</span
              >
            </div>
            <div class="note-actions" @click.stop>
              <span class="action-btn active" @click="toggleLike(note)">
                ❤️ {{ note.likesCount || 0 }}
              </span>
              <span
                class="action-btn"
                :class="{ active: favoritedNoteIds.has(note.id) }"
                @click="toggleFavorite(note)"
              >
                {{ favoritedNoteIds.has(note.id) ? '⭐' : '☆' }} {{ note.favoritesCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 详情弹窗 -->

    <Teleport to="body">
      <div
        v-if="showDetailDialog"
        class="detail-dialog-overlay"
        :class="dialogAnimation"
        @click.self="closeDialog"
      >
        <div class="detail-dialog-container" :class="dialogAnimation" v-loading="detailLoading">
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

              <div v-else class="detail-images">
                <div class="image-gallery">
                  <div class="gallery-image-container">
                    <div class="gallery-skeleton" v-if="galleryImageLoading"></div>
                    <img
                      :src="
                        currentNote.images[currentImageIndex] || currentNote.coverImg || logoImg
                      "
                      :alt="`图片${currentImageIndex + 1}`"
                      @error="onImgError"
                      @load="onImageLoad"
                      @loadstart="onImageLoadStart"
                      class="detail-image"
                      :class="{ 'image-loaded': imageLoaded, 'image-loading': !imageLoaded }"
                    />
                  </div>

                  <div v-if="currentNote.images && currentNote.images.length > 1" class="image-nav">
                    <button class="nav-btn prev-btn" @click="prevImage">
                      <el-icon><ArrowLeft /></el-icon>
                    </button>
                    <button class="nav-btn next-btn" @click="nextImage">
                      <el-icon><ArrowRight /></el-icon>
                    </button>
                  </div>

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
                  >🕒 {{ formatDate(currentNote.publishTime || currentNote.createTime) }}</span
                >
              </div>
              <div class="detail-stats">
                <div class="stat-item">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>{{ currentNote.commentsCount || 0 }}</span>
                  <span class="stat-label">评论</span>
                </div>

                <div
                  class="stat-item like-item"
                  :class="{ liked: likedNoteIds.has(currentNote.id) }"
                  @click="toggleLike(currentNote, $event)"
                >
                  <svg
                    v-if="!likedNoteIds.has(currentNote.id)"
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

                <div
                  class="stat-item favorite-item"
                  :class="{ favorited: favoritedNoteIds.has(currentNote.id) }"
                  @click="toggleFavorite(currentNote, $event)"
                >
                  <el-icon><Collection /></el-icon>
                  <span>{{ currentNote.favoritesCount || 0 }}</span>
                  <span class="stat-label">收藏</span>
                </div>
              </div>
              <div class="detail-body" v-html="currentNote.content"></div>

              <div class="comment-section">
                <h3 class="comment-title">
                  评论 ({{ totalCommentCount }})
                  <span class="comment-subtitle">共 {{ comments.length }} 条主评论</span>
                </h3>

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
                      <span class="comment-tip">友善发言，分享美好</span>
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

    <el-dialog
      v-if="false"
      v-model="showDetailDialog"
      class="me-note-detail-dialog"
      width="86%"
      :show-close="true"
      destroy-on-close
      @closed="closeDetail"
    >
      <div v-if="currentNote" v-loading="detailLoading" class="detail-viewer">
        <div class="detail-media">
          <video
            v-if="currentNote.noteCategory === 'video' && currentNote.video"
            :src="currentNote.video"
            :poster="currentNote.coverImg"
            class="detail-video"
            controls
          ></video>
          <div v-else-if="currentNote.images && currentNote.images.length" class="detail-images">
            <img
              :src="currentNote.images[currentImageIndex]"
              :alt="currentNote.title"
              class="detail-image"
            />
            <div v-if="currentNote.images.length > 1" class="detail-image-nav">
              <el-button
                circle
                size="small"
                @click="
                  currentImageIndex =
                    (currentImageIndex - 1 + currentNote.images.length) % currentNote.images.length
                "
              >
                ‹
              </el-button>
              <span>{{ currentImageIndex + 1 }} / {{ currentNote.images.length }}</span>
              <el-button
                circle
                size="small"
                @click="currentImageIndex = (currentImageIndex + 1) % currentNote.images.length"
              >
                ›
              </el-button>
            </div>
          </div>
          <img
            v-else-if="currentNote.coverImg"
            :src="currentNote.coverImg"
            :alt="currentNote.title"
            class="detail-image"
          />
          <div v-else class="detail-placeholder">
            <img :src="logoImg" alt="logo" />
          </div>
        </div>

        <div class="detail-info">
          <h2 class="detail-title">{{ currentNote.title }}</h2>
          <div class="detail-author">
            <el-avatar :src="currentNote.userPic || avatar" :size="36" />
            <span>{{ currentNote.userName }}</span>
          </div>
          <div class="detail-meta">
            <span
              ><el-icon><View /></el-icon>{{ currentNote.viewsCount || 0 }}</span
            >
            <span
              ><el-icon><ChatDotRound /></el-icon>{{ currentNote.commentsCount || 0 }}</span
            >
            <span
              ><el-icon><Clock /></el-icon>{{ formatDate(currentNote.createTime) }}</span
            >
          </div>
          <div class="detail-actions" @click.stop>
            <button
              class="detail-action-btn"
              :class="{ active: likedNoteIds.has(currentNote.id) }"
              type="button"
              @click="toggleLike(currentNote)"
            >
              {{ likedNoteIds.has(currentNote.id) ? '♥' : '♡' }} {{ currentNote.likesCount || 0 }}
            </button>
            <button
              class="detail-action-btn"
              :class="{ active: favoritedNoteIds.has(currentNote.id) }"
              type="button"
              @click="toggleFavorite(currentNote)"
            >
              {{ favoritedNoteIds.has(currentNote.id) ? '★' : '☆' }}
              {{ currentNote.favoritesCount || 0 }}
            </button>
          </div>
          <div class="detail-content" v-html="currentNote.content || currentNote.excerpt"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.me-container {
  max-width: 1000px;
  margin: 0 auto;
}

// ==================== 用户信息卡片 ====================
.profile-card {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 24px;
  box-shadow: 0 8px 32px rgba(197, 163, 255, 0.15);

  .profile-bg {
    height: 100px;
    background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 50%, #a8e6cf 100%);
    opacity: 0.85;
  }

  .profile-content {
    display: flex;
    align-items: center;
    gap: 24px;
    padding: 0 32px 28px;
    background: #fff;
    position: relative;
  }

  .profile-avatar-wrap {
    position: relative;
    margin-top: -40px;
    flex-shrink: 0;

    .profile-avatar {
      border: 4px solid #fff;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    }

    .avatar-ring {
      position: absolute;
      inset: -6px;
      border-radius: 50%;
      border: 2px solid transparent;
      border-top-color: #c5a3ff;
      border-right-color: #f8b4d9;
      animation: spin 3s linear infinite;
    }
  }

  .profile-info {
    flex: 1;
    min-width: 0;

    .profile-nickname {
      margin: 0 0 4px;
      font-size: 22px;
      font-weight: 700;
      color: #2d2d44;
    }

    .profile-bio {
      margin: 0 0 16px;
      font-size: 13px;
      color: #999;
    }

    .profile-stats {
      display: flex;
      align-items: center;
      gap: 0;

      .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 0 20px;

        .stat-num {
          font-size: 20px;
          font-weight: 700;
          color: #2d2d44;
        }

        .stat-label {
          font-size: 12px;
          color: #999;
          margin-top: 2px;
        }
      }

      .stat-divider {
        width: 1px;
        height: 28px;
        background: #f0e5ff;
      }
    }
  }

  .profile-actions {
    flex-shrink: 0;

    .edit-btn {
      border-radius: 24px;
      padding: 10px 24px;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      border: none;
      color: #fff;
      font-weight: 500;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(197, 163, 255, 0.4);
      }
    }
  }
}

// ==================== 标签栏 ====================
.tabs-bar {
  display: flex;
  gap: 4px;
  background: #fff;
  border-radius: 16px;
  padding: 6px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

  .tab-item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 20px;
    border-radius: 12px;
    cursor: pointer;
    font-size: 15px;
    font-weight: 500;
    color: #888;
    transition: all 0.3s ease;

    .tab-count {
      font-size: 12px;
      padding: 2px 8px;
      border-radius: 10px;
      background: #f5f0ff;
      color: #c5a3ff;
    }

    &:hover {
      color: #c5a3ff;
      background: rgba(197, 163, 255, 0.06);
    }

    &.active {
      color: #fff;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);

      .tab-count {
        background: rgba(255, 255, 255, 0.3);
        color: #fff;
      }
    }
  }
}

// ==================== 笔记网格 ====================
.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.note-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 32px rgba(197, 163, 255, 0.18);
  }

  .note-cover {
    position: relative;
    aspect-ratio: 3 / 4;
    overflow: hidden;
    background: linear-gradient(135deg, #f5f0ff, #fce4ec);

    .cover-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.4s ease;
    }

    &:hover .cover-img {
      transform: scale(1.05);
    }

    .cover-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 40%, #fce4ec 100%);

      .cover-logo {
        width: 40%;
        max-width: 80px;
        min-width: 28px;
        aspect-ratio: 1;
        object-fit: contain;
        opacity: 0.35;
      }
    }

    .note-category-tag {
      position: absolute;
      top: 10px;
      right: 10px;
      padding: 3px 10px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 500;
      background: rgba(0, 0, 0, 0.5);
      color: #fff;
    }
  }

  .note-body {
    padding: 16px;

    .note-title {
      margin: 0 0 8px;
      font-size: 16px;
      font-weight: 600;
      color: #2d2d44;
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .note-excerpt {
      margin: 0 0 12px;
      font-size: 13px;
      color: #999;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .note-author-row {
      margin-bottom: 8px;

      .author-name-small {
        font-size: 12px;
        color: #c5a3ff;
      }
    }

    .note-meta {
      display: flex;
      gap: 14px;
      margin-bottom: 12px;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 3px;
        font-size: 12px;
        color: #bbb;

        .el-icon {
          font-size: 13px;
        }
      }
    }

    .note-actions {
      display: flex;
      gap: 16px;
      padding-top: 10px;
      border-top: 1px solid #f5f0ff;

      .action-btn {
        font-size: 13px;
        color: #bbb;
        cursor: pointer;
        transition: all 0.2s;
        user-select: none;

        &:hover {
          color: #c5a3ff;
          transform: scale(1.1);
        }

        &.active {
          color: #c5a3ff;
        }
      }
    }
  }
}

// ==================== 空状态 ====================
.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 18px;
    font-weight: 600;
    color: #666;
    margin: 0 0 8px;
  }

  .empty-desc {
    font-size: 14px;
    color: #aaa;
    margin: 0 0 24px;
  }

  .publish-btn-empty {
    border-radius: 24px;
    padding: 12px 28px;
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
    border: none;
    color: #fff;
    font-weight: 500;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(197, 163, 255, 0.4);
    }
  }
}

// 加载更多按钮
.load-more-wrap {
  display: flex;
  justify-content: center;
  padding: 24px 0;

  .load-more-btn {
    border-radius: 24px;
    padding: 10px 32px;
    background: linear-gradient(135deg, #f5f0ff, #fff);
    border: 1px solid #e0d4ff;
    color: #7a5a9e;
    font-size: 14px;
    transition: all 0.3s ease;

    &:hover {
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      color: #fff;
      border-color: transparent;
      transform: translateY(-2px);
    }
  }
}

:deep(.me-note-detail-dialog) {
  border-radius: 18px;
  overflow: hidden;

  .el-dialog__body {
    padding: 0;
  }
}

.detail-viewer {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
  min-height: 70vh;
  max-height: 82vh;
  background: #fff;
}

.detail-media {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  background: linear-gradient(135deg, #f5f0ff, #fce4ec);
  overflow: hidden;
}

.detail-video,
.detail-image {
  max-width: 100%;
  max-height: 82vh;
  object-fit: contain;
}

.detail-images {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-image-nav {
  position: absolute;
  left: 50%;
  bottom: 18px;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  color: #6a4a9c;
  font-size: 13px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}

.detail-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;

  img {
    width: 96px;
    opacity: 0.35;
  }
}

.detail-info {
  padding: 34px;
  overflow-y: auto;
}

.detail-title {
  margin: 0 0 18px;
  color: #2d2d44;
  font-size: 24px;
  line-height: 1.35;
}

.detail-author {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  color: #6a5a7a;
  font-size: 14px;
}

.detail-meta,
.detail-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  padding: 12px 0;
  border-top: 1px solid #f5f0ff;
}

.detail-meta {
  color: #aaa;
  font-size: 13px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

.detail-actions {
  border-bottom: 1px solid #f5f0ff;
}

.detail-action-btn {
  border: 0;
  border-radius: 18px;
  padding: 8px 14px;
  background: #f7f3ff;
  color: #8a7a9a;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;

  &:hover,
  &.active {
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
    color: #fff;
  }
}

.detail-content {
  padding-top: 20px;
  color: #4a4a6a;
  font-size: 15px;
  line-height: 1.8;

  :deep(img) {
    max-width: 100%;
    border-radius: 10px;
  }
}

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
  }
}

.detail-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  height: 100%;
  overflow: hidden;
  padding-top: 0;
}

.detail-left {
  background: linear-gradient(135deg, #f0e5ff, #fce4ec);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
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
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;

  .image-gallery,
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
      box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);

      &:hover {
        background: linear-gradient(135deg, #f8b4d9, #c5a3ff);
        border-color: white;
        transform: scale(1.1);
        box-shadow: 0 6px 16px rgba(197, 163, 255, 0.4);
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
    z-index: 10;
    box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
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
}

.detail-author-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.author-avatar-detail,
.author-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
}

.author-avatar-detail {
  object-fit: cover;
  border: 1px solid #f0f0f0;
}

.author-avatar-placeholder {
  background: linear-gradient(135deg, #e0c3ff, #c5a3ff);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 18px;
  box-shadow: 0 2px 8px rgba(197, 163, 255, 0.3);
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name-detail {
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.author-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #a09abf;
}

.detail-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  padding: 0;
  border-top: 0;

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

    &.like-item,
    &.favorite-item {
      cursor: pointer;

      &:hover {
        transform: translateY(-2px);
      }
    }

    &.like-item.liked {
      background: linear-gradient(135deg, rgba(197, 163, 255, 0.2), rgba(248, 180, 217, 0.2));

      .liked-icon,
      span:not(.stat-label) {
        color: #c5a3ff;
      }
    }

    &.favorite-item.favorited {
      background: linear-gradient(135deg, rgba(168, 230, 207, 0.2), rgba(126, 224, 181, 0.2));

      .el-icon,
      span:not(.stat-label) {
        color: #2c665a;
      }
    }
  }
}

.detail-body {
  font-size: 15px;
  line-height: 1.8;
  color: #4a4a6a;

  :deep(img) {
    max-width: 100%;
    border-radius: 12px;
    margin: 12px 0;
  }

  :deep(p) {
    margin-bottom: 12px;
  }
}

.comment-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(197, 163, 255, 0.15);
}

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

.top-comment-input-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;
  padding: 16px;
  background: rgba(197, 163, 255, 0.03);
  border-radius: 20px;

  .input-avatar {
    flex-shrink: 0;
  }

  .input-avatar-img,
  .input-avatar-placeholder {
    width: 44px;
    height: 44px;
    border-radius: 50%;
  }

  .input-avatar-img {
    object-fit: cover;
    border: 2px solid #c5a3ff;
  }

  .input-avatar-placeholder {
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
    }
  }
}

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

.liked-icon {
  color: #c5a3ff;
  animation: heartBeat 0.4s ease;
}

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
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// ==================== 响应式 ====================
@media (max-width: 768px) {
  .profile-content {
    flex-direction: column;
    text-align: center;
    padding: 0 20px 24px;

    .profile-stats {
      justify-content: center;
    }

    .profile-actions {
      margin-top: 12px;
    }
  }

  .notes-grid {
    grid-template-columns: 1fr;
  }

  .detail-viewer {
    grid-template-columns: 1fr;
    max-height: none;
  }

  .detail-media {
    min-height: 360px;
  }

  .detail-info {
    padding: 24px;
  }
}
</style>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, EditPen, Plus, Star, ChatDotRound, View, Clock } from '@element-plus/icons-vue'
import useUserInfoStore from '@/stores/userInfo.js'
import avatar from '@/assets/default.png'
import logoImg from '@/assets/logo.png'
import {
  notePageListService,
  batchCheckNoteLikedService,
  batchCheckNoteFavoritedService,
  likeNoteService,
  unlikeNoteService,
  favoriteNoteService,
  unfavoriteNoteService,
} from '@/api/note.js'
import { getFollowersService, getFollowingService } from '@/api/note.js'
import request from '@/utils/request.js'

const router = useRouter()
const userInfoStore = useUserInfoStore()

// 当前标签页
const activeTab = ref('mynotes')

// 我的笔记
const myNotes = ref([])
const myNotesLoading = ref(false)
const myNotesPage = ref({ pageNum: 1, pageSize: 12, total: 0 })

// 我的收藏
const favoriteNotes = ref([])
const favoriteNotesLoading = ref(false)

// 我的喜欢
const likedNotes = ref([])
const likedNotesLoading = ref(false)

// 用户统计 — 各字段独立维护，stats 为 computed 聚合（单一数据源）
const notesCount = ref(0)
const favoritesCount = ref(0)
const likesCount = ref(0)
const followersCount = ref(0)
const followingCount = ref(0)

const stats = computed(() => ({
  notesCount: notesCount.value,
  favoritesCount: favoritesCount.value,
  likesCount: likesCount.value,
  followersCount: followersCount.value,
  followingCount: followingCount.value,
}))

// 点赞/收藏状态
const likedNoteIds = ref(new Set())
const favoritedNoteIds = ref(new Set())

// 标记哪些tab已加载过
const tabLoaded = ref({ mynotes: false, favorites: false, likes: false })

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
    userName: note.createUserName || note.createUser?.nickname || '匿名用户',
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

    if (reset) {
      myNotes.value = items
    } else {
      myNotes.value.push(...items)
    }

    myNotesPage.value.total = res.data?.total || 0
    notesCount.value = myNotesPage.value.total

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
    favoritesCount.value = noteIds.length

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
    likesCount.value = noteIds.length

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

// 加载用户统计（后台执行）
const loadStats = async () => {
  try {
    const userId = userInfoStore.info.id
    const [followersRes, followingRes] = await Promise.all([
      getFollowersService(userId),
      getFollowingService(userId),
    ])
    followersCount.value = (followersRes.data || []).length
    followingCount.value = (followingRes.data || []).length
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
const toggleLike = async (note) => {
  try {
    if (likedNoteIds.value.has(note.id)) {
      await unlikeNoteService(note.id)
      likedNoteIds.value.delete(note.id)
      note.likesCount = Math.max(0, (note.likesCount || 1) - 1)
      // 从喜欢列表中移除（无论当前在哪个tab，保持数据一致性）
      likedNotes.value = likedNotes.value.filter((n) => n.id !== note.id)
      likesCount.value = Math.max(0, likesCount.value - 1)
      ElMessage.success('已取消点赞')
    } else {
      await likeNoteService(note.id)
      likedNoteIds.value.add(note.id)
      note.likesCount = (note.likesCount || 0) + 1
      ElMessage.success('点赞成功')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 收藏/取消收藏
const toggleFavorite = async (note) => {
  try {
    if (favoritedNoteIds.value.has(note.id)) {
      await unfavoriteNoteService(note.id)
      favoritedNoteIds.value.delete(note.id)
      note.favoritesCount = Math.max(0, (note.favoritesCount || 1) - 1)
      // 从收藏列表中移除（无论当前在哪个tab，保持数据一致性）
      favoriteNotes.value = favoriteNotes.value.filter((n) => n.id !== note.id)
      favoritesCount.value = Math.max(0, favoritesCount.value - 1)
      ElMessage.success('已取消收藏')
    } else {
      await favoriteNoteService(note.id)
      favoritedNoteIds.value.add(note.id)
      note.favoritesCount = (note.favoritesCount || 0) + 1
      ElMessage.success('收藏成功')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 查看笔记详情 → 跳转到首页并打开详情弹窗
const viewNote = (note) => {
  router.push({ path: '/home', query: { noteId: note.id } })
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
    favoritesCount.value = (favRes.data || []).length
    likesCount.value = (likeRes.data || []).length
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
}
</style>

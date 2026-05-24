<!-- CommentItem.vue - 完整修复版 -->
<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { likeCommentService, unlikeCommentService } from '@/api/note.js'

const props = defineProps({
  comment: {
    type: Object,
    required: true
  },
  depth: {
    type: Number,
    default: 0
  },
  likedComments: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['reply', 'toggle-like'])

// 最大缩进深度
const MAX_DEPTH = 5

// 计算缩进样式
const indentStyle = computed(() => {
  const indent = Math.min(props.depth, MAX_DEPTH) * 20
  return props.depth > 0 ? { marginLeft: `${indent}px` } : {}
})

// 是否有子评论
const hasChildren = computed(() => {
  return props.comment.children && props.comment.children.length > 0
})

// 格式化时间
const formatTimeAgo = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return `${date.getMonth() + 1}-${date.getDate()}`
}

// 处理回复
const handleReply = () => {
  emit('reply', props.comment)
}

// 处理点赞
const handleToggleLike = async (event) => {
  event.stopPropagation()

  try {
    if (props.likedComments[props.comment.id]) {
      await unlikeCommentService(props.comment.id)
      emit('toggle-like', props.comment)
    } else {
      await likeCommentService(props.comment.id)
      emit('toggle-like', props.comment)
    }
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 检查是否已点赞
const isLiked = computed(() => {
  return props.likedComments[props.comment.id] || false
})
</script>

<template>
  <div class="comment-item-wrapper" :style="indentStyle">
    <div class="comment-item" :class="{ 'has-children': hasChildren }">
      <!-- 用户头像 -->
      <div class="comment-avatar">
        <img
            v-if="comment.userPic"
            :src="comment.userPic"
            class="comment-avatar-img"
            @error="(e) => (e.target.style.display = 'none')"
        />
        <div v-else class="comment-avatar-placeholder">
          {{ (comment.nickname || comment.username || '匿').charAt(0).toUpperCase() }}
        </div>
      </div>

      <!-- 评论内容 -->
      <div class="comment-content">
        <div class="comment-header">
          <div class="comment-author-info">
            <span class="comment-author">{{ comment.nickname || comment.username || '匿名用户' }}</span>
            <span v-if="comment.replyToUserName" class="reply-indicator">
              <span class="reply-arrow">→</span>
              <span class="reply-target">@{{ comment.replyToUserName }}</span>
            </span>
          </div>
          <span class="comment-time">{{ formatTimeAgo(comment.createTime) }}</span>
        </div>

        <div class="comment-text">{{ comment.content }}</div>

        <div class="comment-footer">
          <button
              class="comment-like-btn"
              :class="{ liked: isLiked }"
              @click="handleToggleLike"
          >
            <!-- 未点赞：空心红心 -->
            <svg v-if="!isLiked" class="like-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
            <!-- 已点赞：实心红心 -->
            <svg v-else class="like-icon liked-icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
            <span>{{ comment.likesCount || 0 }}</span>
          </button>
          <button class="comment-reply-btn" @click="handleReply">
            <svg class="reply-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            回复
          </button>
        </div>
      </div>
    </div>

    <!-- 子评论递归渲染 -->
    <div v-if="hasChildren" class="comment-children">
      <CommentItem
          v-for="child in comment.children"
          :key="child.id"
          :comment="child"
          :depth="depth + 1"
          :liked-comments="likedComments"
          @reply="emit('reply', $event)"
          @toggle-like="emit('toggle-like', $event)"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.comment-item-wrapper {
  transition: all 0.2s ease;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid rgba(197, 163, 255, 0.1);
  transition: background 0.2s ease;

  &:hover {
    background: rgba(197, 163, 255, 0.02);
  }

  &.has-children {
    border-bottom: none;
  }
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.05);
  }
}

.comment-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e0c3ff, #c5a3ff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: white;
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  flex-wrap: wrap;
  gap: 8px;
}

.comment-author-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.comment-author {
  font-weight: 600;
  font-size: 14px;
  background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.reply-indicator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #a09abf;

  .reply-arrow {
    font-size: 12px;
  }

  .reply-target {
    color: #c5a3ff;
  }
}

.comment-time {
  font-size: 11px;
  color: #b0a7c0;
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  color: #5a5a7a;
  margin-bottom: 8px;
  word-break: break-word;
}

.comment-footer {
  display: flex;
  align-items: center;
  gap: 16px;
}

.comment-like-btn,
.comment-reply-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 48px;
  font-size: 12px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #a09abf;

  .like-icon {
    width: 16px;
    height: 16px;
  }

  &:hover {
    background: rgba(197, 163, 255, 0.1);
    color: #c5a3ff;
  }

  &.liked {
    color: #ff5a7e;
    background: rgba(255, 90, 126, 0.1);

    .liked-icon {
      fill: #ff5a7e;
      animation: heartBeat 0.3s ease;
    }
  }
}

.comment-reply-btn {
  .reply-icon {
    width: 14px;
    height: 14px;
  }
}

.comment-children {
  margin-left: 20px;
  border-left: 2px dashed rgba(197, 163, 255, 0.2);
  padding-left: 12px;
}

@keyframes heartBeat {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.3);
  }
  100% {
    transform: scale(1);
  }
}
</style>
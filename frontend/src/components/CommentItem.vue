<!-- components/CommentItem.vue -->
<template>
  <div class="comment-item-wrapper" :data-comment-id="comment.id">
    <div class="comment-item" :style="{ marginLeft: depth * 24 + 'px' }">
      <div class="comment-avatar">
        <img
            v-if="comment.userPic"
            :src="comment.userPic"
            class="comment-avatar-img"
            @error="(e) => (e.target.style.display = 'none')"
        />
        <div v-else class="comment-avatar-placeholder">
          {{ (comment.nickname || comment.username || '匿名').charAt(0).toUpperCase() }}
        </div>
      </div>

      <div class="comment-content">
        <div class="comment-header">
          <span class="comment-author">{{ comment.nickname || comment.username || '匿名用户' }}</span>
          <span class="comment-time">{{ formatTimeAgo(comment.createTime) }}</span>
        </div>

        <!-- 回复目标提示 -->
        <div v-if="comment.replyToUserName" class="comment-reply-target">
          <span class="reply-arrow">↳</span>
          <span class="reply-target-name">@{{ comment.replyToUserName }}</span>
        </div>

        <div class="comment-body">{{ comment.content }}</div>

        <div class="comment-actions">
          <button class="action-btn reply-btn" @click="$emit('reply', comment)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <span>回复</span>
          </button>

          <button
              class="action-btn like-btn"
              :class="{ liked: likedComments[comment.id] }"
              @click="$emit('toggle-like', comment)"
          >
            <svg v-if="!likedComments[comment.id]" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
            <svg v-else class="liked-icon" width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
            <span>{{ comment.likesCount || 0 }}</span>
          </button>
        </div>

        <!-- 内联回复输入框 -->
        <div v-if="replyingCommentId === comment.id" class="reply-input-container">
          <el-input
              v-model="replyContent"
              type="textarea"
              :rows="2"
              :placeholder="`回复 ${comment.nickname || comment.username || '匿名用户'}...`"
              maxlength="300"
              class="reply-textarea"
          />
          <div class="reply-actions">
            <el-button size="small" @click="$emit('cancel-reply', comment.id)">取消</el-button>
            <el-button
                type="primary"
                size="small"
                :loading="submitting"
                @click="$emit('submit-reply', comment.id)"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 递归渲染子评论 -->
    <div v-if="comment.children && comment.children.length" class="comment-children">
      <CommentItem
          v-for="child in comment.children"
          :key="child.id"
          :comment="child"
          :depth="depth + 1"
          :liked-comments="likedComments"
          :replying-comment-id="replyingCommentId"
          :reply-content-map="replyContentMap"
          :submitting="submitting"
          @reply="$emit('reply', $event)"
          @toggle-like="$emit('toggle-like', $event)"
          @cancel-reply="$emit('cancel-reply', $event)"
          @submit-reply="$emit('submit-reply', $event)"
          @update-reply-content="$emit('update-reply-content', $event.commentId, $event.content)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

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
  },
  replyingCommentId: {
    type: Number,
    default: null
  },
  replyContentMap: {
    type: Map,
    default: () => new Map()
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'reply',
  'toggle-like',
  'cancel-reply',
  'submit-reply',
  'update-reply-content'
])

// 使用 computed 双向绑定回复内容 ✅ 解决打字不显示的问题
const replyContent = computed({
  get: () => props.replyContentMap.get(props.comment.id) || '',
  set: (val) => emit('update-reply-content', props.comment.id, val)
})

// 格式化时间
const formatTimeAgo = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = Math.floor((now - date) / 1000)

  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)}天前`

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
</script>

<style lang="scss" scoped>
/* 样式保持不变 */
.comment-item-wrapper {
  margin-bottom: 16px;
  transition: all 0.2s ease;

  &:last-child {
    margin-bottom: 0;
  }
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  transition: background-color 0.2s ease;
  border-radius: 12px;

  &:hover {
    background-color: rgba(197, 163, 255, 0.04);
  }
}

.comment-avatar {
  flex-shrink: 0;

  .comment-avatar-img {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    object-fit: cover;
    border: 1px solid #f0e5ff;
    transition: transform 0.2s ease;

    &:hover {
      transform: scale(1.05);
    }
  }

  .comment-avatar-placeholder {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    color: white;
  }
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 4px;

  .comment-author {
    font-size: 14px;
    font-weight: 600;
    color: #c5a3ff;
    transition: color 0.2s ease;

    &:hover {
      color: #f8b4d9;
    }
  }

  .comment-time {
    font-size: 11px;
    color: #b0a7c0;
  }
}

.comment-reply-target {
  margin-bottom: 6px;
  font-size: 12px;
  color: #a09abf;
  display: flex;
  align-items: center;
  gap: 4px;

  .reply-arrow {
    font-size: 14px;
  }

  .reply-target-name {
    font-weight: 500;
    color: #c5a3ff;
  }
}

.comment-body {
  font-size: 14px;
  line-height: 1.55;
  color: #4a4a6a;
  word-break: break-word;
  white-space: pre-wrap;
  margin-bottom: 8px;
  transition: color 0.2s ease;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 16px;

  .action-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    border: none;
    background: transparent;
    font-size: 12px;
    color: #a09abf;
    cursor: pointer;
    border-radius: 20px;
    transition: all 0.2s ease;

    svg {
      transition: transform 0.2s ease;
    }

    &:hover {
      color: #c5a3ff;
      background-color: rgba(197, 163, 255, 0.08);

      svg {
        transform: scale(1.05);
      }
    }

    &:active {
      transform: scale(0.95);
    }
  }

  .like-btn {
    &.liked {
      color: #c5a3ff;

      svg {
        animation: heartBeat 0.4s ease;
      }
    }
  }
}

.reply-input-container {
  margin-top: 12px;
  padding: 12px;
  background: rgba(197, 163, 255, 0.04);
  border-radius: 16px;
  transition: all 0.3s ease;

  .reply-textarea {
    :deep(.el-textarea__inner) {
      border-radius: 16px;
      background: #faf7ff;
      border: 1px solid #f0e5ff;
      font-size: 13px;
      transition: all 0.3s ease;
      box-shadow: none;

      &:hover {
        border-color: #d9b8ff;
        background-color: #fff;
      }

      &:focus {
        border-color: #c5a3ff;
        background-color: #fff;
        box-shadow: 0 0 0 3px rgba(197, 163, 255, 0.12);
      }
    }
  }

  .reply-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 12px;

    .el-button {
      border-radius: 48px;
      padding: 8px 20px;
      font-size: 12px;
      font-weight: 500;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
      }

      &:active {
        transform: translateY(0);
      }
    }

    .el-button--primary {
      background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
      border: none;
      color: white;

      &:hover {
        box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
      }
    }

    .el-button--default {
      background: transparent;
      border: 1px solid #f0e5ff;
      color: #a09abf;

      &:hover {
        border-color: #c5a3ff;
        color: #c5a3ff;
        background: rgba(197, 163, 255, 0.05);
      }
    }
  }
}

.comment-children {
  margin-left: 20px;
  padding-left: 16px;
  border-left: 2px dashed rgba(197, 163, 255, 0.25);
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
</style>
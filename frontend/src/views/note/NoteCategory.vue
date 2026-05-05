NEW_FILE_CODE
<script setup>
import {
  topicListService,
  topicAddService,
  topicUpdateService,
  topicDeleteService,
} from '@/api/note.js'
import { Edit, Delete } from '@element-plus/icons-vue'
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useTokenStore } from '@/stores/token.js'

const router = useRouter()
const tokenStore = useTokenStore()

const topics = ref([])

const getTopicGradient = (name) => {
  const gradients = {
    美食: 'linear-gradient(135deg, #F8B4D9, #FF9A9E)',
    娱乐: 'linear-gradient(135deg, #A8E6CF, #7EE0B5)',
    军事: 'linear-gradient(135deg, #C5A3FF, #B583FF)',
  }
  return gradients[name] || 'linear-gradient(135deg, #E0C3FF, #C5A3FF)'
}

const topicList = async () => {
  try {
    let result = await topicListService()
    topics.value = result.data
  } catch (error) {
    console.error('获取话题列表失败:', error)
  }
}

onMounted(() => {
  if (!tokenStore.token) {
    router.push('/login')
    return
  }
  topicList()
})

const dialogVisible = ref(false)
const title = ref('')

const topicModel = ref({
  topicName: '',
  description: '',
  coverImg: '',
})

const rules = {
  topicName: [{ required: true, message: '请输入话题名称', trigger: 'blur' }],
  description: [{ required: false, message: '请输入话题描述', trigger: 'blur' }],
}

const addTopic = async () => {
  let result = await topicAddService(topicModel.value)
  ElMessage.success(result.message ? result.message : '添加成功')
  topicList()
  dialogVisible.value = false
}

const showDialog = (row) => {
  dialogVisible.value = true
  title.value = '编辑话题'
  topicModel.value.topicName = row.topicName
  topicModel.value.description = row.description
  topicModel.value.coverImg = row.coverImg
  topicModel.value.id = row.id
}

const updateTopic = async () => {
  let result = await topicUpdateService(topicModel.value)
  ElMessage.success(result.message ? result.message : '修改成功')
  topicList()
  dialogVisible.value = false
}

const deleteTopic = (row) => {
  ElMessageBox.confirm('你确认要删除该话题吗', '温馨提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  })
      .then(async () => {
        let result = await topicDeleteService(row.id)
        ElMessage.success(result.message ? result.message : '删除成功')
        topicList()
      })
      .catch(() => {
        ElMessage({
          type: 'info',
          message: '你取消了删除',
        })
      })
}
</script>

<template>
  <div class="category-container">
    <div class="category-header">
      <div class="header-title-section">
        <div class="title-decoration">
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
        </div>
        <h1 class="category-title">话题分类</h1>
        <p class="category-subtitle">用标签记录生活，让灵感有序安放 📝</p>
      </div>
      <div class="header-extra">
        <el-button
            class="add-category-btn"
            type="primary"
            size="large"
            @click="
            () => {
              dialogVisible = true
              title = '添加话题'
            }
          "
        >
          <span class="btn-text">✨ 添加话题</span>
        </el-button>
      </div>
    </div>

    <div class="category-table-wrapper">
      <el-table :data="topics" style="width: 100%" class="custom-table">
        <el-table-column label="序号" width="80" type="index">
          <template #default="{ $index }">
            <div class="index-badge">
              <span class="index-number">{{ $index + 1 }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="话题名称" prop="topicName" align="left">
          <template #default="{ row }">
            <div class="category-name-cell">
              <span
                  class="category-icon"
                  :style="{ backgroundImage: getTopicGradient(row.topicName) }"
              ></span>
              <span class="category-name-text">{{ row.topicName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="描述" prop="description" align="left">
          <template #default="{ row }">
            <div class="category-alias">
              <span class="alias-label">{{ row.description || '暂无描述' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="笔记数量" prop="notesCount" align="center">
          <template #default="{ row }">
            <span class="count-badge">{{ row.notesCount || 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="编辑话题" placement="top">
                <el-button
                    :icon="Edit"
                    circle
                    class="action-btn edit-btn"
                    @click="showDialog(row)"
                ></el-button>
              </el-tooltip>
              <el-tooltip content="删除话题" placement="top">
                <el-button
                    :icon="Delete"
                    circle
                    class="action-btn delete-btn"
                    @click="deleteTopic(row)"
                ></el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div class="custom-empty">
            <div class="empty-emoji">📝</div>
            <p class="empty-text">还没有话题噢</p>
            <p class="empty-hint">点击「添加话题」开始整理你的笔记世界吧 ✨</p>
          </div>
        </template>
      </el-table>
    </div>

    <div class="category-footer">
      <span class="footer-emoji">💜</span>
      <span>共 {{ topics.length }} 个话题，陪伴你的每一次记录</span>
      <span class="footer-emoji">✨</span>
    </div>

    <el-dialog v-model="dialogVisible" :title="title" width="400px" center class="dialog">
      <el-form
          :model="topicModel"
          :rules="rules"
          label-width="100px"
          style="padding-right: 30px"
          class="form"
      >
        <el-form-item label="话题名称" prop="topicName">
          <el-input
              v-model="topicModel.topicName"
              minlength="1"
              maxlength="20"
              class="custom-input"
              placeholder="请输入话题名称"
          ></el-input>
        </el-form-item>
        <el-form-item label="话题描述" prop="description">
          <el-input
              v-model="topicModel.description"
              type="textarea"
              :rows="3"
              maxlength="100"
              class="custom-input"
              placeholder="请输入话题描述（可选）"
          ></el-input>
        </el-form-item>
        <el-form-item label="封面图片" prop="coverImg">
          <el-input
              v-model="topicModel.coverImg"
              class="custom-input"
              placeholder="请输入封面图片URL（可选）"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false" class="cancel-btn">取消</el-button>
          <el-button
              type="primary"
              @click="title == '添加话题' ? addTopic() : updateTopic()"
              class="confirm-btn"
          >
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.category-container {
  min-height: calc(100vh - 60px);
  padding: 20px;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
}

.category-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;
  padding: 20px 28px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(10px);

  .header-title-section {
    position: relative;

    .title-decoration {
      position: absolute;
      top: -8px;
      left: -12px;
      display: flex;
      gap: 6px;

      .decoration-dot {
        width: 6px;
        height: 6px;
        background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
        border-radius: 50%;
        opacity: 0.7;
      }
      .decoration-dot:nth-child(2) {
        width: 10px;
        height: 10px;
        opacity: 0.4;
      }
    }

    .category-title {
      font-size: 28px;
      font-weight: 700;
      margin: 0 0 6px 0;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
      display: inline-block;
    }

    .category-subtitle {
      font-size: 13px;
      color: #b0a7c0;
      margin: 0;
      letter-spacing: 0.5px;
    }
  }

  .header-extra {
    .add-category-btn {
      background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
      border: none;
      border-radius: 48px;
      padding: 10px 28px;
      font-weight: 500;
      color: white;
      transition: all 0.3s ease;
      box-shadow: 0 4px 12px rgba(197, 163, 255, 0.2);

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
      }

      .btn-text {
        font-size: 14px;
      }
    }
  }
}

.category-table-wrapper {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 30px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(10px);

  .custom-table {
    border-radius: 20px;
    overflow: hidden;

    :deep(.el-table__header) {
      th {
        background: linear-gradient(135deg, #f8f3ff 0%, #faf7ff 100%);
        color: #6a4a9c;
        font-weight: 600;
        font-size: 14px;
      }
    }

    :deep(.el-table__body) {
      tr {
        transition: all 0.3s ease;

        &:hover {
          background: rgba(197, 163, 255, 0.08);
          transform: scale(1.005);
        }
      }

      td {
        padding: 14px 0;
      }
    }

    .index-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      background: linear-gradient(135deg, #e0c3ff, #c5a3ff);
      border-radius: 50%;
      color: white;
      font-weight: 600;
      font-size: 13px;
      box-shadow: 0 2px 8px rgba(197, 163, 255, 0.3);
    }

    .category-name-cell {
      display: flex;
      align-items: center;
      gap: 12px;

      .category-icon {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        display: inline-block;
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
      }

      .category-name-text {
        font-size: 15px;
        font-weight: 500;
        color: #4a4a6a;
      }
    }

    .category-alias {
      .alias-label {
        display: inline-block;
        padding: 4px 14px;
        background: rgba(197, 163, 255, 0.12);
        border-radius: 20px;
        color: #c5a3ff;
        font-size: 13px;
        font-weight: 500;
      }
    }

    .count-badge {
      display: inline-block;
      padding: 4px 12px;
      background: linear-gradient(135deg, #a8e6cf, #7ee0b5);
      border-radius: 20px;
      color: #2c665a;
      font-size: 13px;
      font-weight: 600;
    }

    .action-buttons {
      display: flex;
      gap: 8px;
      justify-content: center;

      .action-btn {
        transition: all 0.3s ease;

        &:hover {
          transform: translateY(-2px) scale(1.1);
        }
      }

      .edit-btn {
        background: linear-gradient(135deg, #a8e6cf 0%, #c2f5e9 100%);
        border-color: #a8e6cf;
        color: #2c665a;

        &:hover {
          box-shadow: 0 4px 12px rgba(168, 230, 207, 0.4);
        }
      }

      .delete-btn {
        background: linear-gradient(135deg, #ff9e9e 0%, #ffbaba 100%);
        border-color: #ff9e9e;
        color: #8a1c1c;

        &:hover {
          box-shadow: 0 4px 12px rgba(255, 158, 158, 0.4);
        }
      }
    }

    .custom-empty {
      padding: 60px 20px;
      text-align: center;

      .empty-emoji {
        font-size: 48px;
        margin-bottom: 16px;
        animation: float 3s ease-in-out infinite;
      }

      .empty-text {
        font-size: 16px;
        color: #6a4a9c;
        margin: 0 0 8px 0;
        font-weight: 500;
      }

      .empty-hint {
        font-size: 13px;
        color: #b0a7c0;
        margin: 0;
      }
    }
  }
}

.category-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding: 16px;
  font-size: 14px;
  color: #b0a7c0;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 48px;
  backdrop-filter: blur(10px);

  .footer-emoji {
    font-size: 16px;
    animation: pulse 2s ease-in-out infinite;
  }
}

.dialog {
  border-radius: 24px;
  overflow: hidden;

  :deep(.el-dialog__header) {
    background: linear-gradient(135deg, #f8b4d9 0%, #c5a3ff 100%);
    padding: 20px;

    .el-dialog__title {
      color: white;
      font-weight: 600;
    }

    .el-dialog__headerbtn {
      .el-dialog__close {
        color: white;
        font-size: 18px;

        &:hover {
          color: rgba(255, 255, 255, 0.8);
        }
      }
    }
  }

  :deep(.el-dialog__body) {
    padding: 24px;
    background: linear-gradient(145deg, #ffffff 0%, #fef9ff 100%);
  }

  .form {
    .custom-input {
      :deep(.el-input__wrapper) {
        border-radius: 48px;
        padding: 6px 20px;
        background-color: #faf7ff;
        border: 1px solid #f0e5ff;
        transition: all 0.3s ease;
        box-shadow: none;

        &:hover {
          border-color: #d9b8ff;
          background-color: #fff;
        }

        &.is-focus {
          border-color: #c5a3ff;
          background-color: #fff;
          box-shadow: 0 0 0 4px rgba(197, 163, 255, 0.12);
        }
      }

      :deep(textarea) {
        border-radius: 12px;
      }
    }
  }

  .dialog-footer {
    .cancel-btn {
      border-radius: 48px;
      padding: 10px 24px;
      font-weight: 500;
      color: #c5a3ff;
      border-color: #c5a3ff;
      transition: all 0.3s ease;

      &:hover {
        background-color: #f8f3ff;
        transform: translateY(-2px);
      }
    }

    .confirm-btn {
      background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
      border: none;
      border-radius: 48px;
      padding: 10px 24px;
      font-size: 14px;
      font-weight: 500;
      color: white;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
      }
    }
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
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
</style>

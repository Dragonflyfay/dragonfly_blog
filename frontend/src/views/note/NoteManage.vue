<script setup>
import { Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ref } from 'vue'

//话题数据模型
const topics = ref([
  {
    id: 3,
    topicName: '美食',
    notesCount: 10,
    coverImg: '',
    description: '分享美食心得',
    createTime: '2023-09-02 12:06:59',
    updateTime: '2023-09-02 12:06:59',
  },
  {
    id: 4,
    topicName: '娱乐',
    notesCount: 5,
    coverImg: '',
    description: '娱乐八卦资讯',
    createTime: '2023-09-02 12:08:16',
    updateTime: '2023-09-02 12:08:16',
  },
  {
    id: 5,
    topicName: '军事',
    notesCount: 8,
    coverImg: '',
    description: '军事新闻讨论',
    createTime: '2023-09-02 12:08:33',
    updateTime: '2023-09-02 12:08:33',
  },
])

//用户搜索时选中的话题id
const topicId = ref('')

//用户搜索时选中的发布状态
const state = ref('')

//笔记列表数据模型
const notes = ref([
  {
    id: 5,
    title: '陕西旅游攻略',
    content: '兵马俑,华清池,法门寺,华山...爱去哪去哪...',
    coverImg:
        'https://big-event-gwd.oss-cn-beijing.aliyuncs.com/9bf1cf5b-1420-4c1b-91ad-e0f4631cbed4.png',
    state: '草稿',
    topicId: 2,
    location: '西安',
    createTime: '2023-09-03 11:55:30',
    updateTime: '2023-09-03 11:55:30',
  },
  {
    id: 6,
    title: '北京美食探店',
    content: '今天去了一家超棒的餐厅...',
    coverImg:
        'https://big-event-gwd.oss-cn-beijing.aliyuncs.com/9bf1cf5b-1420-4c1b-91ad-e0f4631cbed4.png',
    state: '已发布',
    topicId: 3,
    location: '北京',
    createTime: '2023-09-04 10:20:15',
    updateTime: '2023-09-04 10:20:15',
  },
  {
    id: 7,
    title: '电影观后感',
    content: '刚看完一部好电影...',
    coverImg:
        'https://big-event-gwd.oss-cn-beijing.aliyuncs.com/9bf1cf5b-1420-4c1b-91ad-e0f4631cbed4.png',
    state: '草稿',
    topicId: 4,
    location: '',
    createTime: '2023-09-05 15:30:45',
    updateTime: '2023-09-05 15:30:45',
  },
])

//分页条数据模型
const pageNum = ref(1) //当前页
const total = ref(20) //总条数
const pageSize = ref(3) //每页条数

//当每页条数发生了变化，调用此函数
const onSizeChange = (size) => {
  pageSize.value = size
  noteList()
}
//当前页码发生变化，调用此函数
const onCurrentChange = (num) => {
  pageNum.value = num
  noteList()
}

import { topicListService, noteListService, noteAddService } from '@/api/note.js'

//获取话题列表
const getTopicList = async () => {
  let result = await topicListService()
  topics.value = result.data
}

//获取笔记列表数据
const noteList = async () => {
  let params = {
    topicId: topicId.value ? topicId.value : null,
    state: state.value ? state.value : null,
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  }
  let result = await noteListService(params)

  //渲染视图
  total.value = result.data.total
  notes.value = result.data.items

  //处理数据，把话题名称回显到笔记列表中
  for (let i = 0; i < notes.value.length; i++) {
    let note = notes.value[i]
    let found = false
    for (let j = 0; j < topics.value.length; j++) {
      if (note.topicId == topics.value[j].id) {
        note.topicName = topics.value[j].topicName
        found = true
        break
      }
    }
    if (!found) {
      note.topicName = '未分类'
    }
  }
}

getTopicList()
noteList()

import { Plus } from '@element-plus/icons-vue'
//控制抽屉是否显示
const visibleDrawer = ref(false)
//添加表单数据模型
const noteModel = ref({
  title: '',
  topicId: '',
  coverImg: '',
  content: '',
  location: '',
  state: '',
})

//富文本编辑器
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'

//导入token
import { useTokenStore } from '@/stores/token.js'
const tokenStore = useTokenStore()

//上传成功的回调
const uploadSuccess = (result) => {
  noteModel.value.coverImg = result.data
  console.log(result.data)
}

//添加笔记
const addNote = async (clickState) => {
  //把发布状态赋值到笔记模型
  noteModel.value.state = clickState
  //调用接口
  let result = await noteAddService(noteModel.value)
  if (result.code == 200) {
    ElMessage.success('添加成功')
    visibleDrawer.value = false
    noteList()
  } else {
    ElMessage.error('添加失败')
  }
}
</script>

<template>
  <div class="article-manage-container">
    <!-- 顶部装饰区 -->
    <div class="page-header">
      <div class="header-title-section">
        <div class="title-decoration">
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
        </div>
        <h1 class="page-title">笔记管理</h1>
        <p class="page-subtitle">记录灵感，整理思绪，让每一篇笔记都闪闪发光 ✨</p>
      </div>
      <div class="header-extra">
        <el-button class="add-btn" type="primary" size="large" @click="visibleDrawer = true">
          <span class="btn-text">+ 写笔记</span>
        </el-button>
      </div>
    </div>

    <!-- 搜索卡片 -->
    <div class="search-card">
      <div class="search-title">
        <span class="search-emoji">🔍</span>
        <span>筛选笔记</span>
      </div>
      <el-form inline class="search-form">
        <el-form-item label="话题分类">
          <el-select placeholder="请选择话题" v-model="topicId" class="custom-select">
            <el-option v-for="t in topics" :key="t.id" :label="t.topicName" :value="t.id">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="发布状态">
          <el-select placeholder="请选择状态" v-model="state" class="custom-select">
            <el-option label="已发布" value="已发布"></el-option>
            <el-option label="草稿" value="草稿"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button class="search-btn" @click="noteList">搜索</el-button>
          <el-button
              class="reset-btn"
              @click="
              () => {
                topicId = ''
                state = ''
              }
            "
          >重置</el-button
          >
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格卡片 -->
    <div class="table-card">
      <el-table :data="notes" style="width: 100%" class="custom-table">
        <el-table-column label="笔记标题" width="400" prop="title">
          <template #default="{ row }">
            <div class="title-cell">
              <span class="title-icon">📄</span>
              <span class="title-text">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="话题" prop="topicName">
          <template #default="{ row }">
            <span class="category-tag">{{ row.topicName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发表时间" prop="createTime">
          <template #default="{ row }">
            <div class="time-cell">
              <span>📅</span>
              <span>{{ row.createTime }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="state">
          <template #default="{ row }">
            <span
                class="status-badge"
                :class="{
                'status-draft': row.state === '草稿',
                'status-published': row.state === '已发布',
              }"
            >
              {{ row.state }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="编辑" placement="top" effect="customized">
                <el-button :icon="Edit" circle class="action-btn edit-btn"></el-button>
              </el-tooltip>
              <el-tooltip content="删除" placement="top" effect="customized">
                <el-button :icon="Delete" circle class="action-btn delete-btn"></el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="custom-empty">
            <div class="empty-emoji">📭</div>
            <p class="empty-text">还没有笔记哦</p>
            <p class="empty-hint">点击「写笔记」开始记录你的美好生活吧 ✨</p>
          </div>
        </template>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[3, 5, 10, 15]"
            layout="jumper, total, sizes, prev, pager, next"
            background
            :total="total"
            @size-change="onSizeChange"
            @current-change="onCurrentChange"
        />
      </div>
    </div>

    <!-- 底部分隔装饰 -->
    <div class="page-footer">
      <span class="footer-emoji">💜</span>
      <span>共 {{ total }} 篇笔记，记录你的每一次灵感</span>
      <span class="footer-emoji">✨</span>
    </div>

    <!-- 抽屉 - 添加笔记 -->
    <el-drawer
        v-model="visibleDrawer"
        title="写笔记"
        direction="rtl"
        size="50%"
        class="custom-drawer"
    >
      <div class="drawer-content">
        <el-form :model="noteModel" label-width="100px" class="drawer-form">
          <el-form-item label="笔记标题">
            <el-input
                v-model="noteModel.title"
                placeholder="给笔记起个名字吧"
                class="custom-input"
            ></el-input>
          </el-form-item>

          <el-form-item label="话题分类">
            <el-select
                placeholder="选择话题"
                v-model="noteModel.topicId"
                class="custom-select"
            >
              <el-option v-for="t in topics" :key="t.id" :label="t.topicName" :value="t.id">
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="地理位置">
            <el-input
                v-model="noteModel.location"
                placeholder="添加位置信息（可选）"
                class="custom-input"
            ></el-input>
          </el-form-item>

          <el-form-item label="笔记封面">
            <el-upload
                class="avatar-uploader"
                :auto-upload="true"
                :show-file-list="false"
                action="/api/upload"
                name="file"
                :headers="{ Authorization: tokenStore.token }"
                :on-success="uploadSuccess"
            >
              <img
                  v-if="noteModel.coverImg"
                  :src="noteModel.coverImg"
                  class="cover-preview"
              />
              <div v-else class="upload-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <span>点击上传封面</span>
              </div>
            </el-upload>
          </el-form-item>

          <el-form-item label="笔记内容">
            <div class="editor-wrapper">
              <quill-editor theme="snow" v-model:content="noteModel.content" contentType="html">
              </quill-editor>
            </div>
          </el-form-item>

          <el-form-item>
            <div class="form-actions">
              <el-button class="publish-btn" type="primary" @click="addNote('已发布')"
              >发布</el-button
              >
              <el-button class="draft-btn" @click="addNote('草稿')">存草稿</el-button>
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

// 页面头部
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
        background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
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
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
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
    background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
    border: none;
    border-radius: 48px;
    padding: 12px 32px;
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

// 搜索卡片
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
    color: #c5a3ff;

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

    .custom-select {
      width: 180px;
    }

    .search-btn {
      background: linear-gradient(135deg, #c5a3ff 0%, #b583ff 100%);
      border: none;
      border-radius: 48px;
      padding: 8px 24px;
      color: white;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
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

// 表格卡片
.table-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 24px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);

  .custom-table {
    border-radius: 16px;
    overflow: hidden;

    :deep(.el-table__header) {
      th {
        background: linear-gradient(135deg, #f8f3ff 0%, #faf7ff 100%);
        color: #6a4a9c;
        font-weight: 600;
      }
    }

    :deep(.el-table__body) {
      tr {
        transition: all 0.3s ease;

        &:hover {
          background: rgba(197, 163, 255, 0.08);
        }
      }
    }

    .title-cell {
      display: flex;
      align-items: center;
      gap: 10px;

      .title-icon {
        font-size: 18px;
      }

      .title-text {
        font-weight: 500;
        color: #333;
      }
    }

    .category-tag {
      display: inline-block;
      padding: 4px 12px;
      background: linear-gradient(135deg, #e0c3ff, #c5a3ff);
      border-radius: 20px;
      color: white;
      font-size: 12px;
      font-weight: 500;
    }

    .time-cell {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: #999;
    }

    .status-badge {
      display: inline-block;
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 500;

      &.status-published {
        background: linear-gradient(135deg, #a8e6cf, #7ee0b5);
        color: #2c665a;
      }

      &.status-draft {
        background: linear-gradient(135deg, #ffd6a8, #ffc88a);
        color: #8a5a1c;
      }
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
  }

  .pagination-wrapper {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
  }
}

// 空状态
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
    color: #666;
    margin: 0 0 8px 0;
    font-weight: 500;
  }

  .empty-hint {
    font-size: 13px;
    color: #999;
    margin: 0;
  }
}

// 底部装饰
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

// 抽屉样式
.custom-drawer {
  :deep(.el-drawer__header) {
    background: linear-gradient(135deg, #f8b4d9 0%, #c5a3ff 100%);
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
    background: linear-gradient(145deg, #ffffff 0%, #fef9ff 100%);
  }
}

.drawer-content {
  .drawer-form {
    .custom-input {
      :deep(.el-input__wrapper) {
        border-radius: 48px;
        padding: 8px 20px;
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
    }

    .custom-select {
      width: 100%;
    }

    .avatar-uploader {
      .cover-preview {
        width: 200px;
        height: 120px;
        object-fit: cover;
        border-radius: 16px;
        box-shadow: 0 4px 12px rgba(197, 163, 255, 0.2);
      }

      .upload-placeholder {
        width: 200px;
        height: 120px;
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

        &:hover {
          border-color: #f8b4d9;
          background: linear-gradient(135deg, #fff 0%, #faf7ff 100%);
        }

        .upload-icon {
          font-size: 24px;
          color: #c5a3ff;
        }

        span {
          font-size: 12px;
          color: #999;
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
        border-color: #f0e5ff;
      }

      :deep(.ql-container) {
        border-radius: 0 0 12px 12px;
        border-color: #f0e5ff;
      }
    }

    .form-actions {
      display: flex;
      gap: 16px;

      .publish-btn {
        background: linear-gradient(135deg, #c5a3ff 0%, #f8b4d9 100%);
        border: none;
        border-radius: 48px;
        padding: 10px 32px;
        transition: all 0.3s ease;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(197, 163, 255, 0.4);
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

// 动画
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

// 全局样式覆盖
:deep(.el-pagination) {
  .btn-prev,
  .btn-next,
  .el-pager li {
    border-radius: 12px !important;
    transition: all 0.3s ease;

    &:hover {
      background: linear-gradient(135deg, #c5a3ff20, #f8b4d920) !important;
    }
  }

  .el-pager li.is-active {
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9) !important;
    color: white !important;
  }
}
</style>

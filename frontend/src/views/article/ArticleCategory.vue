<script setup>
import {
  articleCategoryListService,
  articleCategoryAddService,
  articleCategoryUpdateService,
  articleCategoryDeleteService,
} from '@/api/article'
import { Edit, Delete } from '@element-plus/icons-vue'
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useTokenStore } from '@/stores/token.js'

const router = useRouter()
const tokenStore = useTokenStore()

// 原始数据（不动）
const categorys = ref([])

// 可选：为不同分类设置渐变色小圆点（装饰用途）
const getCategoryGradient = (name) => {
  const gradients = {
    美食: 'linear-gradient(135deg, #F8B4D9, #FF9A9E)',
    娱乐: 'linear-gradient(135deg, #A8E6CF, #7EE0B5)',
    军事: 'linear-gradient(135deg, #C5A3FF, #B583FF)',
  }
  return gradients[name] || 'linear-gradient(135deg, #E0C3FF, #C5A3FF)'
}

// 纯粹的美化动画状态（hover效果）
const onIconHover = (row, type, isHover) => {
  // 不添加任何功能，仅用于样式控制
}

// 表格行的悬浮样式（class）
const tableRowClassName = ({ row, rowIndex }) => {
  return 'custom-table-row'
}

// 鼠标进入/离开表格单元格（无实际功能，仅用于可能的事件预留）
const handleCellMouseEnter = (row, column, cell, event) => {}
const handleCellMouseLeave = (row, column, cell, event) => {}

//声明一个异步函数
const articleCategoryList = async () => {
  try {
    let result = await articleCategoryListService()
    categorys.value = result.data
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

// 使用onMounted确保在组件挂载后执行数据加载
onMounted(() => {
  // 检查是否有token，如果没有则重定向到登录页
  if (!tokenStore.token) {
    router.push('/login')
    return
  }
  articleCategoryList()
})

//控制添加分类弹窗
const dialogVisible = ref(false)
const title = ref('')

//添加分类数据模型
const categoryModel = ref({
  categoryName: '',
  categoryAlias: '',
})
//添加分类表单校验
const rules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  categoryAlias: [{ required: true, message: '请输入分类别名', trigger: 'blur' }],
}
//访问后台，添加文章分类
const addCategory = async () => {
  //调用接口
  let result = await articleCategoryAddService(categoryModel.value)
  ElMessage.success(result.message ? result.message : '添加成功')

  //再次访问后台接口，查询所有分类
  articleCategoryList()
  //隐藏弹窗
  dialogVisible.value = false
}
//展示编辑弹窗
const showDialog = (row) => {
  dialogVisible.value = true
  title.value = '编辑分类'
  //数据拷贝、
  categoryModel.value.categoryName = row.categoryName
  categoryModel.value.categoryAlias = row.categoryAlias
  //扩展id属性，将来需要传递给后台，完成分类的修改
  categoryModel.value.id = row.id
}

//编辑分类
const updateCategory = async () => {
  //调用接口
  let result = await articleCategoryUpdateService(categoryModel.value)

  ElMessage.success(result.message ? result.message : '修改成功')
  articleCategoryList()
  dialogVisible.value = false
}

//删除分类
import { ElMessageBox } from 'element-plus'
const deleteCategory = (row) => {
  try {
    ElMessageBox.confirm('你确认要删除该分类信息吗', '温馨提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
      .then(async () => {
        // 调用接口
        let result = await articleCategoryDeleteService(row.id)
        ElMessage.success(result.message ? result.message : '删除成功')
        //刷新列表
        articleCategoryList()
      })
      .catch(() => {
        ElMessage({
          type: 'info',
          message: '你取消了删除',
        })
      })
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除失败:', err)
      ElMessage.error('删除失败')
    }
  }
}
</script>
<template>
  <div class="category-container">
    <!-- 顶部装饰与标题区 -->
    <div class="category-header">
      <div class="header-title-section">
        <div class="title-decoration">
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
        </div>
        <h1 class="category-title">文章分类</h1>
        <p class="category-subtitle">用标签记录生活，让灵感有序安放</p>
      </div>
      <div class="header-extra">
        <el-button
          class="add-category-btn"
          type="primary"
          size="large"
          @click="
            () => {
              dialogVisible = true
              title = '添加分类'
            }
          "
        >
          <span class="btn-text">+ 添加分类</span>
        </el-button>
      </div>
    </div>

    <!-- 表格卡片区 -->
    <div class="category-table-wrapper">
      <el-table
        :data="categorys"
        style="width: 100%"
        class="custom-table"
        :row-class-name="tableRowClassName"
        @cell-mouse-enter="handleCellMouseEnter"
        @cell-mouse-leave="handleCellMouseLeave"
      >
        <!-- 序号列（美化后置为标签样式） -->
        <el-table-column label="序号" width="80" type="index">
          <template #default="{ $index }">
            <div class="index-badge">
              <span class="index-number">{{ $index + 1 }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 分类名称 -->
        <el-table-column label="分类名称" prop="categoryName" align="left">
          <template #default="{ row }">
            <div class="category-name-cell">
              <span
                class="category-icon"
                :style="{ backgroundImage: getCategoryGradient(row.categoryName) }"
              ></span>
              <span class="category-name-text">{{ row.categoryName }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 分类别名 -->
        <el-table-column label="分类别名" prop="categoryAlias" align="left">
          <template #default="{ row }">
            <div class="category-alias">
              <span class="alias-label">{{ row.categoryAlias }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 操作按钮（美化后悬浮动效） -->
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="编辑分类" placement="top" effect="customized">
                <el-button
                  :icon="Edit"
                  circle
                  class="action-btn edit-btn"
                  @mouseenter="onIconHover(row, 'edit', true)"
                  @mouseleave="onIconHover(row, 'edit', false)"
                  @click="showDialog(row)"
                ></el-button>
              </el-tooltip>
              <el-tooltip content="删除分类" placement="top" effect="customized">
                <el-button
                  :icon="Delete"
                  circle
                  class="action-btn delete-btn"
                  @mouseenter="onIconHover(row, 'delete', true)"
                  @mouseleave="onIconHover(row, 'delete', false)"
                  @click="deleteCategory(row)"
                ></el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <!-- 空状态自定义（变得更可爱了） -->
        <template #empty>
          <div class="custom-empty">
            <div class="empty-emoji">📝</div>
            <p class="empty-text">还没有分类噢</p>
            <p class="empty-hint">点击「添加分类」开始整理你的笔记世界吧 ✨</p>
          </div>
        </template>
      </el-table>
    </div>

    <!-- 底部分类小提示（装饰） -->
    <div class="category-footer">
      <span class="footer-emoji">💜</span>
      <span>共 {{ categorys.length }} 个分类，陪伴你的每一次记录</span>
      <span class="footer-emoji">✨</span>
    </div>

    <!-- 添加分类弹窗 -->
    <el-dialog v-model="dialogVisible" :title="title" width="400px" center class="dialog">
      <el-form
        :model="categoryModel"
        :rules="rules"
        label-width="100px"
        style="padding-right: 30px"
        class="form"
      >
        <el-form-item label="分类名称" prop="categoryName">
          <el-input
            v-model="categoryModel.categoryName"
            minlength="1"
            maxlength="10"
            class="custom-input"
          ></el-input>
        </el-form-item>
        <el-form-item label="分类别名" prop="categoryAlias">
          <el-input
            v-model="categoryModel.categoryAlias"
            minlength="1"
            maxlength="15"
            class="custom-input"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false" class="cancel-btn">取消</el-button>
          <el-button
            type="primary"
            @click="title == '添加分类' ? addCategory() : updateCategory()"
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
// 整体容器背景延续之前的浪漫通透感
.category-container {
  padding: 8px 0;
  background: transparent;
  min-height: calc(100vh - 180px);
}

// 头部区域设计（类似之前 header 部的拉风感）
.category-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;
  padding: 0 8px;

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
      color: #a09abf;
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

      &:active {
        transform: translateY(0);
      }

      .btn-text {
        font-size: 14px;
      }
    }
  }
}

// 表格卡片区
.category-table-wrapper {
  background: linear-gradient(145deg, #ffffff 0%, #fef9ff 100%);
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  overflow: hidden;

  .custom-table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__header) {
      th {
        background-color: #f8f3ff;
        color: #6a4a9c;
        font-weight: 600;
        font-size: 14px;
      }
    }

    :deep(.el-table__body) {
      tr.custom-table-row {
        transition: all 0.3s ease;

        &:hover {
          background: rgba(197, 163, 255, 0.08);
          transform: scale(1.005);
        }
      }

      td {
        padding: 12px 0;
      }
    }

    // 序号徽章
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

      .index-number {
        line-height: 1;
      }
    }

    // 分类名称单元格
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
        color: #333;
      }
    }

    // 分类别名
    .category-alias {
      .alias-label {
        display: inline-block;
        padding: 4px 12px;
        background: rgba(197, 163, 255, 0.1);
        border-radius: 12px;
        color: #c5a3ff;
        font-size: 13px;
        font-weight: 500;
      }
    }

    // 操作按钮
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
  }
}

// 底部分类提示
.category-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding: 16px;
  font-size: 14px;
  color: #666;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  backdrop-filter: blur(10px);

  .footer-emoji {
    font-size: 16px;
    animation: pulse 2s ease-in-out infinite;
  }
}

// 弹窗样式
.dialog {
  border-radius: 20px;
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
    padding: 20px;
  }

  .form {
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

      :deep(.el-input__prefix) {
        margin-right: 12px;
        color: #c5a3ff;
      }
    }
  }

  .dialog-footer {
    .cancel-btn {
      border-radius: 48px;
      padding: 10px 20px;
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
      padding: 10px 20px;
      font-size: 14px;
      font-weight: 500;
      color: white;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(197, 163, 255, 0.4);
      }

      &:active {
        transform: translateY(0);
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
</style>

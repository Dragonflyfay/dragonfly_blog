<script setup>
import { Edit, Delete } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

// 原始数据（不动）
const categorys = ref([
  //   {
  //     id: 3,
  //     categoryName: '美食',
  //     categoryAlias: 'my',
  //     createTime: '2023-09-02 12:06:59',
  //     updateTime: '2023-09-02 12:06:59',
  //   },
  //   {
  //     id: 4,
  //     categoryName: '娱乐',
  //     categoryAlias: 'yl',
  //     createTime: '2023-09-02 12:08:16',
  //     updateTime: '2023-09-02 12:08:16',
  //   },
  //   {
  //     id: 5,
  //     categoryName: '军事',
  //     categoryAlias: 'js',
  //     createTime: '2023-09-02 12:08:33',
  //     updateTime: '2023-09-02 12:08:33',
  //   },
])
const loading = ref(true) //加载状态

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
  // 不添加任何功能，仅用于样式控制，此处不做逻辑处理，但可以通过类名变化（已在样式内处理悬浮伪类）
  // 留空满足无功能添加要求，但模板tooltip已存在视觉增强
}

// 表格行的悬浮样式（class）
const tableRowClassName = ({ row, rowIndex }) => {
  return 'custom-table-row'
}

// 鼠标进入/离开表格单元格（无实际功能，仅用于可能的事件预留）
const handleCellMouseEnter = (row, column, cell, event) => {}
const handleCellMouseLeave = (row, column, cell, event) => {}

//声明一个异步函数
import { articleCategoryListService } from '@/api/article'
const router = useRouter()
const articleCategoryList = async () => {
  let result = await articleCategoryListService()
  categorys.value = result.data
}

// 使用onMounted确保在组件挂载后执行数据加载

articleCategoryList()
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
        <el-button class="add-category-btn" type="primary" size="large">
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
                ></el-button>
              </el-tooltip>
              <el-tooltip content="删除分类" placement="top" effect="customized">
                <el-button
                  :icon="Delete"
                  circle
                  class="action-btn delete-btn"
                  @mouseenter="onIconHover(row, 'delete', true)"
                  @mouseleave="onIconHover(row, 'delete', false)"
                ></el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <!-- 空状态自定义（变得更可爱了） -->
        <template #empty>
          <div class="custom-empty">
            <div class="empty-emoji">📭</div>
            <p class="empty-text">还没有分类噢～</p>
            <p class="empty-hint">点击「添加分类」开始整理你的笔记世界吧 ✨</p>
          </div>
        </template>
      </el-table>
    </div>

    <!-- 底部分类小提示（装饰） -->
    <div class="category-footer">
      <span class="footer-emoji">🌸</span>
      <span>共 {{ categorys.length }} 个分类，陪伴你的每一次记录</span>
      <span class="footer-emoji">🦋</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
// 整体容器背景延续之前的浪漫通透感
.category-container {
  padding: 8px 0;
  background: transparent;
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

      .btn-text {
        font-size: 14px;
        letter-spacing: 0.5px;
      }

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(197, 163, 255, 0.35);
      }
    }
  }
}

// 表格容器（毛玻璃透明卡）
.category-table-wrapper {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(12px);
  border-radius: 32px;
  padding: 4px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.8);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.06);
  }
}

// 自定义表格样式
.custom-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(197, 163, 255, 0.08);
  --el-table-border-color: rgba(197, 163, 255, 0.2);
  --el-table-text-color: #4a3a6e;
  --el-table-row-hover-bg-color: rgba(197, 163, 255, 0.12);
  border-radius: 28px;
  overflow: hidden;

  :deep(.el-table__header-wrapper) {
    th {
      font-size: 15px;
      font-weight: 600;
      color: #b18ed8;
      background: rgba(197, 163, 255, 0.08);
      border-bottom: 1px solid rgba(197, 163, 255, 0.2);
      padding: 16px 0;
    }
  }

  :deep(.el-table__body-wrapper) {
    tr {
      transition: all 0.2s ease;
    }
    td {
      border-bottom: 1px solid rgba(197, 163, 255, 0.08);
      padding: 18px 0;
    }
  }
}

// 序号小标识
.index-badge {
  display: flex;
  justify-content: center;
  align-items: center;

  .index-number {
    width: 28px;
    height: 28px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: rgba(197, 163, 255, 0.2);
    border-radius: 20px;
    font-size: 13px;
    font-weight: 500;
    color: #c5a3ff;
    transition: all 0.2s;
  }
}

// 分类名称装饰球 + 文字
.category-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  .category-icon {
    width: 32px;
    height: 32px;
    border-radius: 16px;
    background-size: cover;
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.02);
    transition: transform 0.2s;
  }

  .category-name-text {
    font-size: 16px;
    font-weight: 500;
    color: #3d2f5e;
    letter-spacing: 0.3px;
  }
}

// 分类别名的小标签
.category-alias {
  .alias-label {
    background: rgba(168, 230, 207, 0.25);
    padding: 4px 12px;
    border-radius: 40px;
    font-size: 13px;
    color: #77aa8b;
    font-weight: 500;
    backdrop-filter: blur(2px);
    transition: all 0.2s;
  }
}

// 操作按钮圆润可爱
.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;

  .action-btn {
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(4px);
    border: 1px solid rgba(197, 163, 255, 0.3);
    transition: all 0.2s ease;

    &.edit-btn {
      --el-button-icon-color: #c5a3ff;
      color: #c5a3ff;

      &:hover {
        background: #c5a3ff;
        border-color: #c5a3ff;
        transform: scale(1.08);
        box-shadow: 0 4px 12px rgba(197, 163, 255, 0.3);
        --el-button-icon-color: white;
        color: white;
      }
    }

    &.delete-btn {
      --el-button-icon-color: #f8b4d9;
      color: #f8b4d9;

      &:hover {
        background: #f8b4d9;
        border-color: #f8b4d9;
        transform: scale(1.08);
        box-shadow: 0 4px 12px rgba(248, 180, 217, 0.3);
        --el-button-icon-color: white;
        color: white;
      }
    }
  }
}

// 空状态（可爱风）
.custom-empty {
  text-align: center;
  padding: 48px 0;

  .empty-emoji {
    font-size: 64px;
    margin-bottom: 16px;
    opacity: 0.7;
  }

  .empty-text {
    font-size: 18px;
    font-weight: 500;
    color: #c5a3ff;
    margin: 8px 0;
  }

  .empty-hint {
    font-size: 13px;
    color: #b0a8cd;
    margin: 4px 0;
  }
}

// 底部小装饰
.category-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 12px;
  color: #b7acd6;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px;

  .footer-emoji {
    font-size: 14px;
    opacity: 0.6;
    animation: gentleFloat 3s infinite ease;
  }
}

@keyframes gentleFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-4px);
  }
}

// 深色模式自适应（不主动添加深色切换功能，但兼容父组件 dark-mode 类）
:deep(.layout-container.dark-mode) .category-container {
  .category-header .category-title {
    background: linear-gradient(135deg, #e0c3ff, #ffc8e6, #b5e6d0);
  }

  .category-table-wrapper {
    background: rgba(30, 30, 50, 0.7);
    backdrop-filter: blur(14px);
  }

  .custom-table {
    --el-table-text-color: #d9ceff;
    .category-name-text {
      color: #e2d3ff;
    }
    .alias-label {
      background: rgba(168, 230, 207, 0.3);
      color: #aee0c8;
    }
  }

  .custom-empty .empty-text {
    color: #e0c3ff;
  }

  .category-footer {
    color: #9b8abb;
  }
}
</style>

<script setup>
import {
  articleCategoryListService,
  articleCategoryAddService,
  articleCategoryUpdateService,
  articleCategoryDeleteService,
} from '@/api/article'
import { Edit, Delete } from '@element-plus/icons-vue'
import { ref } from 'vue'
const categorys = ref([
  {
    id: 3,
    categoryName: '美食',
    categoryAlias: 'my',
    createTime: '2023-09-02 12:06:59',
    updateTime: '2023-09-02 12:06:59',
  },
  {
    id: 4,
    categoryName: '娱乐',
    categoryAlias: 'yl',
    createTime: '2023-09-02 12:08:16',
    updateTime: '2023-09-02 12:08:16',
  },
  {
    id: 5,
    categoryName: '军事',
    categoryAlias: 'js',
    createTime: '2023-09-02 12:08:33',
    updateTime: '2023-09-02 12:08:33',
  },
])
//声明一个异步的函数
const articleCategoryList = async () => {
  let res = await articleCategoryListService()
  categorys.value = res.data
}
articleCategoryList()

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
import { ElMessage } from 'element-plus'
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
        ElMessage({
          type: 'success',
          message: '删除成功',
        })
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
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}
</script>
<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span class="title">文章分类</span>
        <div class="extra">
          <el-button
            type="primary"
            @click="
              () => {
                dialogVisible = true
                title = '添加分类'
              }
            "
            class="add-btn"
            >添加分类</el-button
          >
        </div>
      </div>
    </template>
    <el-table :data="categorys" style="width: 100%" stripe class="table">
      <el-table-column label="序号" width="100" type="index"> </el-table-column>
      <el-table-column label="分类名称" prop="categoryName"></el-table-column>
      <el-table-column label="分类别名" prop="categoryAlias"></el-table-column>
      <el-table-column label="创建时间" prop="createTime"></el-table-column>
      <el-table-column label="更新时间" prop="updateTime"></el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button
            :icon="Edit"
            circle
            plain
            type="primary"
            @click="showDialog(row)"
            class="edit-btn"
          ></el-button>
          <el-button
            :icon="Delete"
            circle
            plain
            type="danger"
            @click="deleteCategory(row)"
            class="delete-btn"
          ></el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="没有数据" />
      </template>
    </el-table>
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
  </el-card>
</template>

<style lang="scss" scoped>
.page-container {
  min-height: 100%;
  box-sizing: border-box;
  border: none;
  border-radius: 32px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(4px);

  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .title {
      font-size: 22px;
      font-weight: 700;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }
  }

  .add-btn {
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

  .table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__header) {
      th {
        background-color: #f8f3ff;
        color: #6a4a9c;
        font-weight: 600;
      }
    }
  }

  .edit-btn,
  .delete-btn {
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
    }
  }

  .edit-btn {
    background: linear-gradient(135deg, #a8e6cf 0%, #c2f5e9 100%);
    border-color: #a8e6cf;
    color: #2c665a;
  }

  .delete-btn {
    background: linear-gradient(135deg, #ff9e9e 0%, #ffbaba 100%);
    border-color: #ff9e9e;
    color: #8a1c1c;
  }

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
    }

    :deep(.el-dialog__body) {
      padding: 20px;
    }
  }

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
</style>

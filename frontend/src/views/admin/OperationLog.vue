<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request.js'

const logs = ref([])
const loading = ref(false)
const filters = reactive({ operator: '', module: '', startDate: '', endDate: '' })
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const modules = ['用户管理', '笔记管理', '评论管理', '话题管理', '角色管理', '系统设置']

const getLogs = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/logs', {
      params: { ...filters, pageNum: pageNum.value, pageSize: pageSize.value },
    })
    logs.value = res.data.items || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const getActionColor = (action) => {
  const map = {
    新增: 'success',
    删除: 'danger',
    修改: 'warning',
    查询: 'info',
    导出: 'primary',
    登录: 'success',
    退出: 'info',
  }
  return map[action] || ''
}
const resetFilters = () => {
  filters.operator = ''
  filters.module = ''
  filters.startDate = ''
  filters.endDate = ''
  pageNum.value = 1
  getLogs()
}

onMounted(getLogs)
</script>

<template>
  <div class="operation-log">
    <div class="page-header">
      <h1 class="page-title">📋 操作日志</h1>
      <p class="page-subtitle">记录管理员的所有操作行为</p>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-input v-model="filters.operator" placeholder="操作人" style="width: 150px" clearable />
      <el-select v-model="filters.module" placeholder="模块" style="width: 140px" clearable>
        <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
      </el-select>
      <el-date-picker v-model="filters.startDate" type="date" placeholder="开始日期" />
      <el-date-picker v-model="filters.endDate" type="date" placeholder="结束日期" />
      <el-button
        type="primary"
        :icon="Search"
        @click="
          () => {
            pageNum = 1
            getLogs()
          }
        "
        >搜索</el-button
      >
      <el-button :icon="Refresh" @click="resetFilters()">重置</el-button>
    </div>

    <!-- 日志列表 -->
    <el-table :data="logs" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="operator" label="操作人" width="120" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-tag :type="getActionColor(row.action)" size="small">{{ row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="操作详情" min-width="200" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="createTime" label="操作时间" width="180" />
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next"
      @size-change="getLogs"
      @current-change="getLogs"
    />
  </div>
</template>

<style scoped>
.operation-log {
  padding: 20px;
}
.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
</style>

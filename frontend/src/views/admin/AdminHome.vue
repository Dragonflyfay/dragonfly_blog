<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document,
  User,
  View,
  ChatDotRound,
  Star,
  Collection,
  Top,
  DataLine,
  Plus,
  TrendCharts,
  Clock,
  Loading,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getDashboardStatsService,
  getWeeklyTrendService,
  getTopicStatsService,
} from '@/api/admin.js'

const router = useRouter()
const loading = ref(true)

// ==================== 统计数据 ====================
const stats = ref({
  totalNotes: 0,
  publishedNotes: 0,
  draftNotes: 0,
  totalViews: 0,
  totalLikes: 0,
  totalFavorites: 0,
  totalComments: 0,
  totalUsers: 0,
  todayNewUsers: 0,
  activeUsers: 0,
  totalTopics: 0,
  todayNewNotes: 0,
  todayComments: 0,
})

// ==================== 图表数据 ====================
const weeklyData = ref({
  dates: [],
  notes: [],
  users: [],
  comments: [],
})

const topicData = ref({
  names: [],
  counts: [],
})

// ==================== 图表实例 ====================
let trendChart = null
let topicChart = null

// ==================== 加载数据 ====================
const loadDashboardData = async () => {
  loading.value = true
  try {
    const [statsRes, trendRes, topicRes] = await Promise.all([
      getDashboardStatsService(),
      getWeeklyTrendService(),
      getTopicStatsService(),
    ])

    if (statsRes.code === 0) {
      stats.value = statsRes.data
    }

    if (trendRes.code === 0) {
      weeklyData.value = trendRes.data
    }

    if (topicRes.code === 0) {
      topicData.value = topicRes.data
    }

    // 等待 DOM 渲染完成后再初始化图表
    await nextTick()
    initCharts()
  } catch (error) {
    if (error?.__handled) return
    console.error('加载仪表盘数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ==================== 初始化图表 ====================
const initCharts = () => {
  initTrendChart()
  initTopicChart()
}

// 趋势图
const initTrendChart = () => {
  const chartDom = document.getElementById('trendChart')
  if (!chartDom) return

  if (trendChart) {
    trendChart.dispose()
  }

  trendChart = echarts.init(chartDom)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#f0e5ff',
      borderWidth: 1,
      textStyle: { color: '#4a4a6a' },
    },
    legend: {
      data: ['新增笔记', '新增用户', '新增评论'],
      right: 0,
      top: 0,
      textStyle: { color: '#8a7a9a', fontSize: 12 },
      itemWidth: 16,
      itemHeight: 8,
    },
    grid: {
      left: 40,
      right: 20,
      bottom: 20,
      top: 40,
    },
    xAxis: {
      type: 'category',
      data: weeklyData.value.dates,
      axisLine: { lineStyle: { color: '#f0e5ff' } },
      axisLabel: { color: '#b0a7c0', fontSize: 11 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f5f0ff', type: 'dashed' } },
      axisLabel: { color: '#b0a7c0', fontSize: 11 },
    },
    series: [
      {
        name: '新增笔记',
        type: 'line',
        data: weeklyData.value.notes,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#c5a3ff',
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(197, 163, 255, 0.3)' },
            { offset: 1, color: 'rgba(197, 163, 255, 0.02)' },
          ]),
        },
        itemStyle: {
          color: '#c5a3ff',
        },
      },
      {
        name: '新增用户',
        type: 'line',
        data: weeklyData.value.users,
        smooth: true,
        symbol: 'diamond',
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#f8b4d9',
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(248, 180, 217, 0.3)' },
            { offset: 1, color: 'rgba(248, 180, 217, 0.02)' },
          ]),
        },
        itemStyle: {
          color: '#f8b4d9',
        },
      },
      {
        name: '新增评论',
        type: 'line',
        data: weeklyData.value.comments,
        smooth: true,
        symbol: 'roundRect',
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#a8e6cf',
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(168, 230, 207, 0.3)' },
            { offset: 1, color: 'rgba(168, 230, 207, 0.02)' },
          ]),
        },
        itemStyle: {
          color: '#a8e6cf',
        },
      },
    ],
  }

  trendChart.setOption(option)
  window.addEventListener('resize', () => trendChart?.resize())
}

// 话题统计图（饼图）
const initTopicChart = () => {
  const chartDom = document.getElementById('topicChart')
  if (!chartDom) return

  if (topicChart) {
    topicChart.dispose()
  }

  topicChart = echarts.init(chartDom)

  const colors = [
    '#c5a3ff',
    '#f8b4d9',
    '#a8e6cf',
    '#ffd93d',
    '#ff9a9e',
    '#6bcbff',
    '#b583ff',
    '#ff6b6b',
    '#4ecdc4',
    '#ffe66d',
  ]

  const data = topicData.value.names.map((name, index) => ({
    name: name || '未分类',
    value: topicData.value.counts[index] || 0,
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#f0e5ff',
      borderWidth: 1,
      textStyle: { color: '#4a4a6a' },
      formatter: '{b}<br/>笔记数: {c} 篇 ({d}%)',
    },
    legend: {
      orient: 'vertical',
      right: 20,
      top: 'center',
      textStyle: { color: '#8a7a9a', fontSize: 12 },
      itemWidth: 14,
      itemHeight: 14,
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '75%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 13,
            fontWeight: 'bold',
          },
          scale: true,
        },
        data: data.map((item, index) => ({
          ...item,
          itemStyle: { color: colors[index % colors.length] },
        })),
      },
    ],
  }

  topicChart.setOption(option)
  window.addEventListener('resize', () => topicChart?.resize())
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadDashboardData()
})

// 组件卸载时销毁图表
import { onBeforeUnmount } from 'vue'
onBeforeUnmount(() => {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
  if (topicChart) {
    topicChart.dispose()
    topicChart = null
  }
})
</script>

<template>
  <div class="admin-home-container">
    <!-- 顶部标题 -->
    <div class="page-header">
      <div class="header-title-section">
        <div class="title-decoration">
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
          <span class="decoration-dot"></span>
        </div>
        <h1 class="page-title">📊 数据概览</h1>
        <p class="page-subtitle">实时掌握平台运营数据</p>
      </div>
      <div class="header-time">
        <el-icon><Clock /></el-icon>
        <span>{{ new Date().toLocaleString('zh-CN') }}</span>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid" v-loading="loading">
      <!-- 总笔记 -->
      <div class="stat-card" @click="router.push('/admin/note')">
        <div
          class="stat-icon-wrapper"
          style="background: linear-gradient(135deg, #c5a3ff, #b583ff)"
        >
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.totalNotes }}</span>
          <span class="stat-label">总笔记</span>
        </div>
        <div class="stat-sub">
          <span>已发布 {{ stats.publishedNotes }}</span>
          <span>草稿 {{ stats.draftNotes }}</span>
        </div>
      </div>

      <!-- 总用户 -->
      <div class="stat-card" @click="router.push('/admin/userRole')">
        <div
          class="stat-icon-wrapper"
          style="background: linear-gradient(135deg, #f8b4d9, #ff9a9e)"
        >
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.totalUsers }}</span>
          <span class="stat-label">总用户</span>
        </div>
        <div class="stat-sub">
          <span>今日新增 +{{ stats.todayNewUsers }}</span>
          <span>活跃用户 {{ stats.activeUsers }}</span>
        </div>
      </div>

      <!-- 总评论 -->
      <div class="stat-card" @click="router.push('/admin/comment')">
        <div
          class="stat-icon-wrapper"
          style="background: linear-gradient(135deg, #a8e6cf, #7ee0b5)"
        >
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.totalComments }}</span>
          <span class="stat-label">总评论</span>
        </div>
        <div class="stat-sub">
          <span>今日 +{{ stats.todayComments }}</span>
        </div>
      </div>

      <!-- 总话题 -->
      <div class="stat-card" @click="router.push('/admin/topic')">
        <div
          class="stat-icon-wrapper"
          style="background: linear-gradient(135deg, #ffd93d, #f6b93b)"
        >
          <el-icon><Collection /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.totalTopics }}</span>
          <span class="stat-label">总话题</span>
        </div>
        <div class="stat-sub">
          <span>今日新增 +{{ stats.todayNewNotes }}</span>
        </div>
      </div>

      <!-- 互动数据 -->
      <div class="stat-card stat-card-wide">
        <div
          class="stat-icon-wrapper"
          style="background: linear-gradient(135deg, #ff6b6b, #ff8e8e)"
        >
          <el-icon><Star /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.totalLikes }}</span>
          <span class="stat-label">总点赞</span>
        </div>
        <div class="stat-sub">
          <span>❤️ {{ stats.totalLikes }}</span>
          <span>⭐ {{ stats.totalFavorites }}</span>
          <span>👁️ {{ stats.totalViews }}</span>
        </div>
      </div>

      <!-- 今日数据 -->
      <div class="stat-card stat-card-wide">
        <div
          class="stat-icon-wrapper"
          style="background: linear-gradient(135deg, #4ecdc4, #44b39d)"
        >
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{
            stats.todayNewNotes + stats.todayComments + stats.todayNewUsers
          }}</span>
          <span class="stat-label">今日新增</span>
        </div>
        <div class="stat-sub">
          <span>📝 笔记 {{ stats.todayNewNotes }}</span>
          <span>💬 评论 {{ stats.todayComments }}</span>
          <span>👤 用户 {{ stats.todayNewUsers }}</span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 趋势图 -->
      <div class="chart-card chart-card-large">
        <div class="chart-header">
          <span class="chart-title">📈 近7天趋势</span>
          <span class="chart-subtitle">笔记 · 用户 · 评论 每日新增</span>
        </div>
        <div id="trendChart" class="chart-container"></div>
      </div>

      <!-- 话题分布 -->
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">📊 话题分布</span>
          <span class="chart-subtitle">各话题笔记数量</span>
        </div>
        <div id="topicChart" class="chart-container"></div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.admin-home-container {
  padding: 20px;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 50%, #fce4ec 100%);
  min-height: calc(100vh - 120px);
}

// ==================== 页面头部 ====================
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding: 20px 28px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(10px);

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
      font-size: 26px;
      font-weight: 700;
      margin: 0 0 4px 0;
      background: linear-gradient(135deg, #c5a3ff, #f8b4d9, #a8e6cf);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }

    .page-subtitle {
      font-size: 13px;
      color: #a09abf;
      margin: 0;
    }
  }

  .header-time {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 18px;
    background: rgba(197, 163, 255, 0.08);
    border-radius: 48px;
    font-size: 13px;
    color: #8a7a9a;

    .el-icon {
      font-size: 16px;
      color: #c5a3ff;
    }
  }
}

// ==================== 统计卡片 ====================
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 20px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(197, 163, 255, 0.15);
  }

  &.stat-card-wide {
    grid-column: span 2;
  }

  .stat-icon-wrapper {
    width: 52px;
    height: 52px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .el-icon {
      font-size: 26px;
      color: white;
    }
  }

  .stat-info {
    flex: 1;
    min-width: 0;

    .stat-number {
      display: block;
      font-size: 28px;
      font-weight: 700;
      color: #2d2d44;
      line-height: 1.2;
    }

    .stat-label {
      font-size: 13px;
      color: #a09abf;
    }
  }

  .stat-sub {
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: #b0a7c0;

    span {
      padding: 2px 10px;
      background: rgba(197, 163, 255, 0.06);
      border-radius: 12px;
    }
  }
}

// ==================== 图表区域 ====================
.charts-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.chart-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 20px;
  padding: 20px 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 24px rgba(197, 163, 255, 0.1);
  }

  .chart-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-bottom: 12px;

    .chart-title {
      font-size: 16px;
      font-weight: 600;
      color: #2d2d44;
    }

    .chart-subtitle {
      font-size: 12px;
      color: #b0a7c0;
    }
  }

  .chart-container {
    width: 100%;
    height: 280px;
  }

  &.chart-card-large .chart-container {
    height: 300px;
  }
}

// ==================== 加载状态 ====================
:deep(.el-loading-mask) {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(4px);
}

// ==================== 响应式 ====================
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-card.stat-card-wide {
    grid-column: span 1;
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .stat-card .stat-number {
    font-size: 22px;
  }
}
</style>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document,
  User,
  ChatDotRound,
  Collection,
  View,
  Star,
  ArrowUp,
  ArrowDown,
  Refresh,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getDashboardStatsService,
  getWeeklyTrendService,
  getTopicStatsService,
} from '@/api/admin.js'

const router = useRouter()
const loading = ref(true)
const refreshing = ref(false)

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
let refreshTimer = null
let isPageVisible = true

// ==================== 数字变化追踪（用于动画） ====================
const previousStats = ref({})

// ==================== 计算变化率 ====================
const getChangeRate = (current, prev) => {
  if (!prev || prev === 0) return { value: 0, up: true }
  const rate = ((current - prev) / prev) * 100
  return {
    value: Math.abs(rate).toFixed(1),
    up: rate >= 0,
  }
}

const noteChange = computed(() => {
  const prev = previousStats.value.totalNotes || stats.value.totalNotes
  return getChangeRate(stats.value.totalNotes, prev)
})

const userChange = computed(() => {
  const prev = previousStats.value.totalUsers || stats.value.totalUsers
  return getChangeRate(stats.value.totalUsers, prev)
})

const commentChange = computed(() => {
  const prev = previousStats.value.totalComments || stats.value.totalComments
  return getChangeRate(stats.value.totalComments, prev)
})

// ==================== 数字跳动动画 ====================
const animateNumber = (el, target, duration = 600) => {
  if (!el) return
  const current = parseInt(el.textContent.replace(/,/g, '')) || 0
  const start = current
  const diff = target - start
  if (diff === 0) return

  const startTime = performance.now()

  const update = (currentTime) => {
    const progress = Math.min((currentTime - startTime) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    const value = Math.floor(start + diff * eased)
    el.textContent = value.toLocaleString()
    if (progress < 1) {
      requestAnimationFrame(update)
    } else {
      el.textContent = target.toLocaleString()
    }
  }
  requestAnimationFrame(update)
}

// ==================== 更新所有数字动画 ====================
const triggerNumberAnimations = () => {
  nextTick(() => {
    document.querySelectorAll('.stat-number').forEach((el) => {
      const target = parseInt(el.dataset.target) || 0
      animateNumber(el, target)
    })
  })
}

// ==================== 加载数据（核心） ====================
const loadDashboardData = async (showLoading = false) => {
  if (showLoading) loading.value = true
  if (!showLoading) refreshing.value = true

  try {
    const [statsRes, trendRes, topicRes] = await Promise.all([
      getDashboardStatsService(),
      getWeeklyTrendService(),
      getTopicStatsService(),
    ])

    // 保存旧数据用于计算变化
    previousStats.value = { ...stats.value }

    // 更新统计数据
    if (statsRes.code === 0) {
      stats.value = statsRes.data
      triggerNumberAnimations()
    }

    // 更新趋势数据
    if (trendRes.code === 0) {
      weeklyData.value = trendRes.data
    }

    // 更新话题数据
    if (topicRes.code === 0) {
      topicData.value = topicRes.data
    }

    // 更新图表
    await nextTick()
    updateCharts()
  } catch (error) {
    if (error?.__handled) return
    console.error('加载仪表盘数据失败:', error)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

// ==================== 手动刷新（用户点击） ====================
const manualRefresh = () => {
  if (refreshing.value) return
  loadDashboardData(false)
  ElMessage.success('数据已刷新')
}

// ==================== 更新图表（不重建） ====================
const updateCharts = () => {
  updateTrendChart()
  updateTopicChart()
}

// 更新趋势图
const updateTrendChart = () => {
  if (!trendChart) return

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.92)',
      borderColor: '#f0e5ff',
      borderWidth: 1,
      textStyle: { color: '#4a4a6a', fontSize: 12 },
    },
    legend: {
      data: ['笔记', '用户', '评论'],
      right: 0,
      top: 0,
      textStyle: { color: '#8a7a9a', fontSize: 11 },
      itemWidth: 14,
      itemHeight: 8,
    },
    grid: {
      left: 36,
      right: 16,
      bottom: 16,
      top: 36,
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
        name: '笔记',
        type: 'line',
        data: weeklyData.value.notes,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2.5, color: '#c5a3ff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(197, 163, 255, 0.25)' },
            { offset: 1, color: 'rgba(197, 163, 255, 0.02)' },
          ]),
        },
        itemStyle: { color: '#c5a3ff' },
      },
      {
        name: '用户',
        type: 'line',
        data: weeklyData.value.users,
        smooth: true,
        symbol: 'diamond',
        symbolSize: 6,
        lineStyle: { width: 2.5, color: '#f8b4d9' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(248, 180, 217, 0.25)' },
            { offset: 1, color: 'rgba(248, 180, 217, 0.02)' },
          ]),
        },
        itemStyle: { color: '#f8b4d9' },
      },
      {
        name: '评论',
        type: 'line',
        data: weeklyData.value.comments,
        smooth: true,
        symbol: 'roundRect',
        symbolSize: 6,
        lineStyle: { width: 2.5, color: '#a8e6cf' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(168, 230, 207, 0.25)' },
            { offset: 1, color: 'rgba(168, 230, 207, 0.02)' },
          ]),
        },
        itemStyle: { color: '#a8e6cf' },
      },
    ],
  }

  trendChart.setOption(option, true)
}

// 更新话题分布图
const updateTopicChart = () => {
  if (!topicChart) return

  const names = topicData.value.names || []
  const counts = topicData.value.counts || []

  const sorted = names
    .map((name, i) => ({ name, count: counts[i] || 0 }))
    .sort((a, b) => b.count - a.count)

  const topNames = sorted.map((item) => item.name)
  const topCounts = sorted.map((item) => item.count)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.92)',
      borderColor: '#f0e5ff',
      borderWidth: 1,
      textStyle: { color: '#4a4a6a', fontSize: 12 },
      formatter: (params) => {
        const p = params[0]
        return `${p.name}<br/>笔记数: <strong>${p.value}</strong> 篇`
      },
    },
    grid: {
      left: 16,
      right: 30,
      bottom: 16,
      top: 8,
    },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f5f0ff', type: 'dashed' } },
      axisLabel: { color: '#b0a7c0', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'category',
      data: topNames.length > 8 ? topNames.slice(0, 8) : topNames,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#6a4a9c',
        fontSize: 12,
        fontWeight: 500,
        width: 60,
        overflow: 'truncate',
      },
    },
    series: [
      {
        type: 'bar',
        data: topCounts.length > 8 ? topCounts.slice(0, 8) : topCounts,
        barWidth: 14,
        borderRadius: [0, 6, 6, 0],
        itemStyle: {
          color: (params) => {
            const colors = [
              '#c5a3ff',
              '#f8b4d9',
              '#a8e6cf',
              '#ffd93d',
              '#ff9a9e',
              '#6bcbff',
              '#b583ff',
              '#4ecdc4',
            ]
            return colors[params.dataIndex % colors.length]
          },
        },
        label: {
          show: true,
          position: 'right',
          color: '#b0a7c0',
          fontSize: 11,
          formatter: (params) => (params.value > 0 ? params.value : ''),
        },
      },
    ],
  }

  topicChart.setOption(option, true)
}

// ==================== 初始化图表 ====================
const initCharts = () => {
  // 趋势图
  const trendDom = document.getElementById('trendChart')
  if (trendDom) {
    if (trendChart) trendChart.dispose()
    trendChart = echarts.init(trendDom)
    updateTrendChart()
  }

  // 话题图
  const topicDom = document.getElementById('topicChart')
  if (topicDom) {
    if (topicChart) topicChart.dispose()
    topicChart = echarts.init(topicDom)
    updateTopicChart()
  }
}

// ==================== 窗口resize ====================
const handleResize = () => {
  trendChart?.resize()
  topicChart?.resize()
}

// ==================== 页面可见性检测 ====================
const handleVisibilityChange = () => {
  isPageVisible = !document.hidden
  if (isPageVisible) {
    // 页面重新可见时立即刷新一次
    loadDashboardData(false)
  }
}

// ==================== 轮询控制 ====================
const startAutoRefresh = () => {
  if (refreshTimer) clearInterval(refreshTimer)
  refreshTimer = setInterval(() => {
    if (isPageVisible) {
      loadDashboardData(false)
    }
  }, 30000) // 30秒刷新一次
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadDashboardData(true)
  setTimeout(() => {
    initCharts()
  }, 100)

  startAutoRefresh()
  window.addEventListener('resize', handleResize)
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onBeforeUnmount(() => {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
  if (topicChart) {
    topicChart.dispose()
    topicChart = null
  }
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<template>
  <div class="admin-home">
    <!-- ===== 顶部 ===== -->
    <div class="home-header">
      <div>
        <h1 class="home-title">📊 数据概览</h1>
        <p class="home-subtitle">
          实时掌握平台运营动态
          <span class="refresh-status" :class="{ refreshing }">
            <el-icon class="is-loading" v-if="refreshing"><Refresh /></el-icon>
            <span v-else>●</span>
            {{ refreshing ? '更新中...' : '已同步' }}
          </span>
        </p>
      </div>
      <div class="header-right">
        <span class="live-badge">
          <span class="live-dot"></span>
          实时
        </span>
        <span class="header-time">{{ new Date().toLocaleString('zh-CN') }}</span>
        <el-button
          class="refresh-btn"
          :icon="Refresh"
          circle
          size="small"
          @click="manualRefresh"
          :loading="refreshing"
        />
      </div>
    </div>

    <!-- ===== 统计卡片 ===== -->
    <div class="stats-grid" v-loading="loading">
      <!-- 笔记 -->
      <div class="stat-card" @click="router.push('/admin/note')">
        <div class="stat-card-inner">
          <div class="stat-icon purple">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-body">
            <span class="stat-number" :data-target="stats.totalNotes">0</span>
            <span class="stat-label">总笔记</span>
          </div>
          <div class="stat-change" :class="noteChange.up ? 'up' : 'down'">
            <el-icon><ArrowUp v-if="noteChange.up" /><ArrowDown v-else /></el-icon>
            {{ noteChange.value }}%
          </div>
        </div>
        <div class="stat-footer">
          <span>已发布 {{ stats.publishedNotes }}</span>
          <span class="dot">·</span>
          <span>草稿 {{ stats.draftNotes }}</span>
        </div>
      </div>

      <!-- 用户 -->
      <div class="stat-card" @click="router.push('/admin/userRole')">
        <div class="stat-card-inner">
          <div class="stat-icon pink">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-body">
            <span class="stat-number" :data-target="stats.totalUsers">0</span>
            <span class="stat-label">总用户</span>
          </div>
          <div class="stat-change" :class="userChange.up ? 'up' : 'down'">
            <el-icon><ArrowUp v-if="userChange.up" /><ArrowDown v-else /></el-icon>
            {{ userChange.value }}%
          </div>
        </div>
        <div class="stat-footer">
          <span>今日 +{{ stats.todayNewUsers }}</span>
          <span class="dot">·</span>
          <span>活跃 {{ stats.activeUsers }}</span>
        </div>
      </div>

      <!-- 评论 -->
      <div class="stat-card" @click="router.push('/admin/comment')">
        <div class="stat-card-inner">
          <div class="stat-icon green">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="stat-body">
            <span class="stat-number" :data-target="stats.totalComments">0</span>
            <span class="stat-label">总评论</span>
          </div>
          <div class="stat-change" :class="commentChange.up ? 'up' : 'down'">
            <el-icon><ArrowUp v-if="commentChange.up" /><ArrowDown v-else /></el-icon>
            {{ commentChange.value }}%
          </div>
        </div>
        <div class="stat-footer">
          <span>今日 +{{ stats.todayComments }}</span>
        </div>
      </div>

      <!-- 话题 -->
      <div class="stat-card" @click="router.push('/admin/topic')">
        <div class="stat-card-inner">
          <div class="stat-icon gold">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="stat-body">
            <span class="stat-number" :data-target="stats.totalTopics">0</span>
            <span class="stat-label">总话题</span>
          </div>
          <div class="stat-change up">
            <el-icon><ArrowUp /></el-icon>
            {{ stats.todayNewNotes > 0 ? `+${stats.todayNewNotes}` : '0' }}
          </div>
        </div>
        <div class="stat-footer">
          <span>今日新增 {{ stats.todayNewNotes }} 篇笔记</span>
        </div>
      </div>
    </div>

    <!-- ===== 互动数据行 ===== -->
    <div class="interaction-row">
      <div class="interaction-item">
        <el-icon><View /></el-icon>
        <span class="interaction-number">{{ stats.totalViews.toLocaleString() }}</span>
        <span class="interaction-label">总浏览</span>
      </div>
      <div class="interaction-divider"></div>
      <div class="interaction-item">
        <span class="interaction-emoji">❤️</span>
        <span class="interaction-number">{{ stats.totalLikes.toLocaleString() }}</span>
        <span class="interaction-label">总点赞</span>
      </div>
      <div class="interaction-divider"></div>
      <div class="interaction-item">
        <span class="interaction-emoji">⭐</span>
        <span class="interaction-number">{{ stats.totalFavorites.toLocaleString() }}</span>
        <span class="interaction-label">总收藏</span>
      </div>
    </div>

    <!-- ===== 图表双栏 ===== -->
    <div class="charts-row">
      <div class="chart-card">
        <div class="chart-card-header">
          <span class="chart-title">📈 近7天趋势</span>
          <span class="chart-subtitle">笔记 · 用户 · 评论</span>
        </div>
        <div id="trendChart" class="chart-container"></div>
      </div>

      <div class="chart-card chart-card-topic">
        <div class="chart-card-header">
          <span class="chart-title">🏷️ 话题分布</span>
          <span class="chart-subtitle">各话题笔记数量</span>
        </div>
        <div id="topicChart" class="chart-container chart-topic"></div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.admin-home {
  padding: 20px 24px;
  background: linear-gradient(145deg, #f5f0ff 0%, #e8ddf8 40%, #fce4ec 100%);
  min-height: calc(100vh - 120px);
}

// ===== 顶部 =====
.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding: 20px 28px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(197, 163, 255, 0.08);

  .home-title {
    font-size: 24px;
    font-weight: 700;
    margin: 0 0 2px;
    background: linear-gradient(135deg, #c5a3ff, #f8b4d9);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }

  .home-subtitle {
    font-size: 13px;
    color: #b0a7c0;
    margin: 0;
    display: flex;
    align-items: center;
    gap: 12px;

    .refresh-status {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 11px;
      color: #a8e6cf;
      transition: all 0.3s ease;

      .el-icon {
        font-size: 12px;
      }

      &.refreshing {
        color: #c5a3ff;
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .live-badge {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 4px 14px;
      background: rgba(168, 230, 207, 0.2);
      border-radius: 48px;
      font-size: 12px;
      color: #2c665a;
      font-weight: 500;

      .live-dot {
        width: 6px;
        height: 6px;
        background: #4ecdc4;
        border-radius: 50%;
        animation: pulse-dot 1.5s ease-in-out infinite;
      }
    }

    .header-time {
      font-size: 13px;
      color: #b0a7c0;
    }

    .refresh-btn {
      border: 1px solid #f0e5ff;
      color: #c5a3ff;
      transition: all 0.3s ease;

      &:hover {
        background: rgba(197, 163, 255, 0.1);
        border-color: #c5a3ff;
        transform: rotate(180deg);
      }
    }
  }
}

@keyframes pulse-dot {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(0.8);
  }
}

// ===== 统计卡片 =====
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  padding: 18px 20px 14px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(197, 163, 255, 0.15);
    border-color: rgba(197, 163, 255, 0.2);
  }

  .stat-card-inner {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  .stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .el-icon {
      font-size: 22px;
      color: white;
    }

    &.purple {
      background: linear-gradient(135deg, #c5a3ff, #b583ff);
    }
    &.pink {
      background: linear-gradient(135deg, #f8b4d9, #ff9a9e);
    }
    &.green {
      background: linear-gradient(135deg, #a8e6cf, #7ee0b5);
    }
    &.gold {
      background: linear-gradient(135deg, #ffd93d, #f6b93b);
    }
  }

  .stat-body {
    flex: 1;
    min-width: 0;

    .stat-number {
      display: block;
      font-size: 26px;
      font-weight: 700;
      color: #2d2d44;
      line-height: 1.2;
      font-variant-numeric: tabular-nums;
      transition: color 0.3s ease;
    }

    .stat-label {
      font-size: 12px;
      color: #b0a7c0;
    }
  }

  .stat-change {
    display: flex;
    align-items: center;
    gap: 2px;
    padding: 2px 10px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 600;
    flex-shrink: 0;
    transition: all 0.3s ease;

    &.up {
      color: #2c665a;
      background: rgba(168, 230, 207, 0.2);
    }
    &.down {
      color: #d4727a;
      background: rgba(255, 154, 158, 0.2);
    }

    .el-icon {
      font-size: 12px;
    }
  }

  .stat-footer {
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px solid rgba(197, 163, 255, 0.08);
    font-size: 12px;
    color: #b0a7c0;
    display: flex;
    align-items: center;
    gap: 6px;

    .dot {
      color: #e0d4ff;
    }
  }
}

// ===== 互动数据行 =====
.interaction-row {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 16px 24px;
  margin-bottom: 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);

  .interaction-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 20px;

    .el-icon,
    .interaction-emoji {
      font-size: 18px;
      color: #c5a3ff;
    }

    .interaction-emoji {
      font-size: 16px;
    }

    .interaction-number {
      font-size: 18px;
      font-weight: 700;
      color: #2d2d44;
      font-variant-numeric: tabular-nums;
    }

    .interaction-label {
      font-size: 12px;
      color: #b0a7c0;
    }
  }

  .interaction-divider {
    width: 1px;
    height: 28px;
    background: #f0e5ff;
  }
}

// ===== 图表双栏 =====
.charts-row {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 16px;
}

.chart-card {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  padding: 18px 20px 12px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 24px rgba(197, 163, 255, 0.08);
  }

  .chart-card-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-bottom: 8px;

    .chart-title {
      font-size: 15px;
      font-weight: 600;
      color: #2d2d44;
    }

    .chart-subtitle {
      font-size: 11px;
      color: #b0a7c0;
    }
  }

  .chart-container {
    width: 100%;
    height: 260px;
  }

  .chart-topic {
    height: 250px;
  }
}

// ===== 加载状态 =====
:deep(.el-loading-mask) {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(4px);
}

// ===== 响应式 =====
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row {
    grid-template-columns: 1fr;
  }
  .chart-card .chart-container {
    height: 220px;
  }
}

@media (max-width: 768px) {
  .admin-home {
    padding: 12px;
  }
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .home-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .interaction-row {
    flex-wrap: wrap;
    gap: 8px;
    padding: 12px 16px;
  }
  .interaction-item {
    padding: 0 10px;
  }
  .interaction-divider {
    display: none;
  }
  .header-right {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>

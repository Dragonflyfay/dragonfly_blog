import request from '@/utils/request.js'

/**
 * 获取仪表盘统计数据
 */
export const getDashboardStatsService = () => {
  return request.get('/admin/dashboard/stats')
}

/**
 * 获取最近7天趋势数据
 */
export const getWeeklyTrendService = () => {
  return request.get('/admin/dashboard/weekly-trend')
}

/**
 * 获取话题统计
 */
export const getTopicStatsService = () => {
  return request.get('/admin/dashboard/topic-stats')
}

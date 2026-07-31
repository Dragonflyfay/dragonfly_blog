// api/notification.js
import request from '@/utils/request.js'

// 获取通知列表
export const getNotificationListService = (pageNum, pageSize) => {
  return request.get('/notification/list', { params: { pageNum, pageSize } })
}

// 获取未读通知数
export const getUnreadCountService = () => {
  return request.get('/notification/unread/count')
}

// 获取最近未读通知
export const getRecentUnreadService = () => {
  return request.get('/notification/unread/recent')
}

// 标记为已读
export const markAsReadService = (id) => {
  return request.put(`/notification/read/${id}`)
}

// 全部标记为已读
export const markAllAsReadService = () => {
  return request.put('/notification/read/all')
}

// 删除通知
export const deleteNotificationService = (id) => {
  return request.delete(`/notification/${id}`)
}

// 删除所有已读通知
export const deleteAllReadService = () => {
  return request.delete('/notification/read/all')
}

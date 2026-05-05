
import request from '@/utils/request.js'

// 话题列表查询
export const topicListService = () => {
  return request.get('/topic')
}

// 话题添加
export const topicAddService = (topicData) => {
  return request.post('/topic', topicData)
}

// 话题删除
export const topicDeleteService = (id) => {
  return request.delete('/topic', { params: { id } })
}

// 话题修改
export const topicUpdateService = (topicData) => {
  return request.put('/topic', topicData)
}

// 笔记列表查询
export const noteListService = (params) => {
  return request.get('/note', { params: params })
}

// 笔记分页查询
export const notePageListService = (pageNum, pageSize) => {
  return request.get('/note/page', { params: { pageNum, pageSize } })
}

// 笔记添加
export const noteAddService = (noteData) => {
  return request.post('/note', noteData)
}

// 笔记详情查询
export const noteDetailService = (id) => {
  return request.get('/note/detail', { params: { id } })
}

// 笔记更新
export const noteUpdateService = (noteData) => {
  return request.put('/note', noteData)
}

// 笔记删除
export const noteDeleteService = (id) => {
  return request.delete('/note', { params: { id } })
}

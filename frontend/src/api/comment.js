import request from '@/utils/request.js'

// 管理员分页查询评论
export const commentPageListService = (params) => {
  return request.get('/comment/page', { params })
}

// 删除评论
export const commentDeleteService = (id) => {
  return request.delete(`/comment/${id}`)
}

// 获取笔记的评论列表
export const getCommentsByNoteIdService = (noteId) => {
  return request.get(`/comment/note/${noteId}`)
}

// 添加评论
export const addCommentService = (commentData) => {
  return request.post('/comment', commentData)
}

// 更新评论
export const updateCommentService = (commentData) => {
  return request.put('/comment', commentData)
}

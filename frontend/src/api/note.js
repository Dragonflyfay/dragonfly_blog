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

// 笔记分页查询（支持 topicId、state 筛选）
export const notePageListService = (params) => {
  return request.get('/note/page', { params })
}

//用户首页分页查询（支持话题，关键词筛选）
export const userNotePageListService = (params) => {
  return request.get('/note/user/page', { params })
}
// 获取笔记详情（包含完整信息）
export const noteDetailFullService = (id) => {
  return request.get('/note/detail', { params: { id } })
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

// 用户列表查询
export const userListService = () => {
  return request.get('/user/list')
}

// 点赞相关接口
// 点赞笔记
export const likeNoteService = (noteId) => {
  return request.post(`/like/note/${noteId}`)
}

// 取消点赞笔记
export const unlikeNoteService = (noteId) => {
  return request.delete(`/like/note/${noteId}`)
}

// 检查是否点赞笔记
export const checkLikeNoteService = (noteId) => {
  return request.get(`/like/note/${noteId}/check`)
}

// 批量检查笔记点赞状态
export const batchCheckNoteLikedService = (noteIds) => {
  return request.post('/like/note/batch-check', noteIds)
}

// 点赞评论
export const likeCommentService = (commentId) => {
  return request.post(`/like/comment/${commentId}`)
}

// 取消点赞评论
export const unlikeCommentService = (commentId) => {
  return request.delete(`/like/comment/${commentId}`)
}

// 检查是否点赞评论
export const checkLikeCommentService = (commentId) => {
  return request.get(`/like/comment/${commentId}/check`)
}

// 批量检查评论点赞状态
export const batchCheckCommentLikedService = (commentIds) => {
  return request.post('/like/comment/batch-check', commentIds)
}

// 评论相关接口
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

// 删除评论
export const deleteCommentService = (id) => {
  return request.delete(`/comment/${id}`)
}

// 浏览记录相关接口
// 记录浏览
export const recordViewService = (noteId) => {
  return request.post(`/view/note/${noteId}`)
}

// 获取笔记浏览次数
export const getViewCountService = (noteId) => {
  return request.get(`/view/note/${noteId}/count`)
}

// 收藏相关接口
// 收藏笔记
export const favoriteNoteService = (noteId) => {
  return request.post(`/favorite/note/${noteId}`)
}

// 取消收藏笔记
export const unfavoriteNoteService = (noteId) => {
  return request.delete(`/favorite/note/${noteId}`)
}

// 检查是否收藏笔记
export const checkFavoriteNoteService = (noteId) => {
  return request.get(`/favorite/note/${noteId}/check`)
}
// 批量检查笔记收藏状态
export const batchCheckNoteFavoritedService = (noteIds) => {
  return request.post('/favorite/notes/batch-check', noteIds)
}

// 关注相关API
export const followUserService = (userId) => {
  return request.post(`/follow/${userId}`)
}

export const unfollowUserService = (userId) => {
  return request.delete(`/follow/${userId}`)
}

export const checkFollowService = (userId) => {
  return request.get(`/follow/${userId}/check`)
}

export const batchCheckFollowService = (userIds) => {
  return request.post('/follow/batch-check', userIds)
}

export const getFollowersService = (userId) => {
  return request.get(`/follow/${userId}/followers`)
}

export const getFollowingService = (userId) => {
  return request.get(`/follow/${userId}/following`)
}

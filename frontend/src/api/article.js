import request from '@/utils/request.js'
//文章分类列表查询
export const articleCategoryListService = () => {
  // const tokenStore = useTokenStore()
  //pinia中获取的响应式数据都不需要.value 获取token
  //return request.get('/category',{header:{'Authorization:tokenStore.token'}})
  return request.get('/category')
}

//文章分类添加
export const articleCategoryAddService = (categoryData) => {
  return request.post('/category', categoryData)
}

//文章分类删除
export const articleCategoryDeleteService = (id) => {
  return request.delete(`/category`, { params: { id } })
}
//文章分类修改
export const articleCategoryUpdateService = (categoryData) => {
  return request.put(`/category`, categoryData)
}

//文章列表查询
export const articleListService = (params) => {
  return request.get('/article', { params: params })
}
//文章添加
export const articleAddService = (articleData) => {
  return request.post('/article', articleData)
}

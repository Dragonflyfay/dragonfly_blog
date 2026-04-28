import request from '@/utils/request.js'
import { useTokenStore } from '@/stores/token'
//文章列表查询
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
export const articleCategoryDeleteService = (id) => {
  return request.delete(`/category`,{params:{id}})
}
export const articleCategoryUpdateService = (categoryData) => {
  return request.put(`/category`, categoryData)
}

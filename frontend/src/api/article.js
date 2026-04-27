import request from '@/utils/request.js'
import { useTokenStore } from '@/stores/token'
export const articleCategoryListService = () => {
  const tokenStore = useTokenStore()
  //再pinia中定义的响应式数据，都不需要.value
  return request.get('/category', { headers: { Authorization: tokenStore.token } })
}

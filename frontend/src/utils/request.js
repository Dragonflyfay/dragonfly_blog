//定制请求的实例
//拦截器
//导入axios  npm install axios
import axios from 'axios'
import { ElMessage } from 'element-plus'
//定义一个变量,记录公共的前缀  ,  baseURL
//const baseURL = 'http://localhost:8080';
//解决跨域问题
const baseURL = '/api'
const instance = axios.create({ baseURL })

// import { useTokenStore } from '@/stores/token.js'
// //添加请求拦截器
// instance.interceptors.request.use(
//   (config) => {
//     //请求前的回调
//     //添加token
//     const tokenStore = useTokenStore()
//     //判断有没有token
//     if (tokenStore.token) {
//       config.headers.Authorization = tokenStore.token
//     }
//     return config
//   },
//   (error) => {
//     //请求错误的回调
//     Promise.reject(error)
//   },
// )

import { useTokenStore } from '@/stores/token.js'
//添加请求拦截器
//use里可以调用两个回调函数
instance.interceptors.request.use(
  (config) => {
    //请求前的回调
    //添加token
    const tokenStore = useTokenStore()
    //判断是否有
    if (tokenStore.token) {
      config.headers.Authorization = tokenStore.token
    }
    return config
  },
  (err) => {
    //请求错误的回调
    Promise.reject(err)
  },
)

import router from '@/router'
//添加响应拦截器
instance.interceptors.response.use(
  (result) => {
    //判断业务状态吗
    if (result.data.code === 0) {
      return result.data
    }
    //操作失败
    ElMessage.error(result.data.message ? result.data.message : '服务异常')
    //异步的状态转化成失败的状态
    return Promise.reject(result.data)
  },
  (err) => {
    const status = err.response?.status

    if (status === 401) {
      ElMessage.error('请先登录')
      router.push('/login')
    } else if (status === 502 || status === 503 || status === 504) {
      ElMessage.error(
        '无法连接后端服务，请确认 Spring Boot 已在运行（开发环境默认 http://localhost:8080）',
      )
    } else if (!err.response) {
      ElMessage.error('网络异常，请检查网络或后端是否可达')
    } else {
      ElMessage.error(err.message || '服务异常')
    }

    return Promise.reject(err)
  },
)

export default instance

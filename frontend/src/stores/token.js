// export const useTokenStore = defineStore(
//   'token',
//   () => {
//     //定义状态的内容

//     //1.响应式变量
//     const token = ref('')

//     //2.定义一个函数，修改token的值
//     const setToken = (newToken, rememberMe = true) => {
//       token.value = newToken
//       // 根据rememberMe参数决定是否持久化存储
//       if (rememberMe) {
//         // 使用localStorage进行持久化存储
//         localStorage.setItem('token', newToken)
//       } else {
//         // 不记住时，只在sessionStorage中存储，关闭浏览器时会自动清除
//         sessionStorage.setItem('token', newToken)
//         // 同时清除localStorage中的token（如果有）
//         localStorage.removeItem('token')
//       }
//     }

//     //移除token的值
//     const removeToken = () => {
//       token.value = ''
//       localStorage.removeItem('token')
//       sessionStorage.removeItem('token')
//     }
//     const restoreToken = () => {
//       const storedToken = localStorage.getItem('token') || sessionStorage.getItem('token')
//       if (storedToken) {
//         token.value = storedToken
//       }
//     }
//     restoreToken()

//     return {
//       token,
//       setToken,
//       removeToken,
//       restoreToken,
//     }
//   },
//   { persist: false },
// )

//用来管理用户的token
//定义store
import { defineStore } from 'pinia'
import { ref } from 'vue'
/**
 * 第一个参数：名字，唯一性
 * 第二个参数：函数，内部可以定义状态的所有内容
 */
export const useTokenStore = defineStore(
  'token',
  () => {
    //定义状态的内容

    //1.响应式变量
    const token = ref('')
    //定义函数，修改token
    const setToken = (newToken) => {
      token.value = newToken
    }
    const removeToken = () => {
      token.value = ''
    }
    //   const restoreToken = () => {
    //     const storedToken = localStorage.getItem('token')
    //     if (storedToken) {
    //     }
    //   }
    return { token, setToken, removeToken }
  },
  {
    persist: true,
  },
)

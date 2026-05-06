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
//用来管理用户的token
import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 获取当前标签页的唯一标识
 * 如果不存在则生成一个新的
 */
const getTabId = () => {
    let tabId = sessionStorage.getItem('tab_id')
    if (!tabId) {
        // 生成唯一ID：时间戳 + 随机数
        tabId = `tab_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
        sessionStorage.setItem('tab_id', tabId)
    }
    return tabId
}

/**
 * 获取当前标签页的 storage key
 * @param {string} baseKey - 基础key名称
 * @returns {string} - 带标签页标识的key
 */
const getTabStorageKey = (baseKey) => {
    const tabId = getTabId()
    return `${baseKey}_${tabId}`
}
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
      const rememberMe = ref(false)

    //定义函数，修改token
    const setToken = (newToken,isRememberMe=false) => {
      token.value = newToken
        rememberMe.value = isRememberMe
        // 根据rememberMe参数决定存储方式
        if (isRememberMe) {
            // 记住我：使用localStorage（全局共享，所有标签页共用）
            localStorage.setItem('global_token', newToken)
            // 清除当前标签页的sessionStorage
            sessionStorage.removeItem(getTabStorageKey('token'))
        } else {
            // 不记住：使用sessionStorage（仅当前标签页）
            sessionStorage.setItem(getTabStorageKey('token'), newToken)
            // 注意：不清除global_token，因为其他标签页可能在使用
        }
    }
    const removeToken = () => {
      token.value = ''
        localStorage.removeItem('global_token')
        sessionStorage.removeItem(getTabStorageKey('token'))
    }
      // 初始化时从sessionStorage恢复token
      const initToken = () => {
          // 优先检查是否有"记住我"的全局token
          const globalToken = localStorage.getItem('global_token')
          if (globalToken) {
              token.value = globalToken
              rememberMe.value = true
              return
          }

          // 否则检查当前标签页的token
          const tabToken = sessionStorage.getItem(getTabStorageKey('token'))
          if (tabToken) {
              token.value = tabToken
              rememberMe.value = false
          }
      }
      // 在store创建时立即执行
      initToken()
    //   const restoreToken = () => {
    //     const storedToken = localStorage.getItem('token')
    //     if (storedToken) {
    //     }
    //   }
    return { token, setToken, removeToken,rememberMe }
  },
  {
    persist: false,
  },
)

import { defineStore } from 'pinia'
import { ref ,computed} from 'vue'
import { normalizeRole, canEnterAdmin } from '@/utils/roles'

/**
 * 获取当前标签页的唯一标识
 */
const getTabId = () => {
    let tabId = sessionStorage.getItem('tab_id')
    if (!tabId) {
        tabId = `tab_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
        sessionStorage.setItem('tab_id', tabId)
    }
    return tabId
}

/**
 * 获取当前标签页的 storage key
 */
const getTabStorageKey = (baseKey) => {
    const tabId = getTabId()
    return `${baseKey}_${tabId}`
}



const useUserInfoStore = defineStore(
  'userInfo',
  () => {
      //定义状态相关的内容
      const info = ref({})
      const role = ref('user')
      const rememberMe = ref(false)

    const setInfo = (newInfo,isRememberMe = false) => {
        info.value = newInfo
        role.value = normalizeRole(newInfo?.role)
        rememberMe.value=isRememberMe

        // 根据rememberMe参数决定存储方式

        if (isRememberMe) {
            // 记住我：使用localStorage（全局共享）
            localStorage.setItem('global_userInfo', JSON.stringify(newInfo))
            localStorage.setItem('global_userRole', role.value)
            // 清除当前标签页的数据
            sessionStorage.removeItem(getTabStorageKey('userInfo'))
            sessionStorage.removeItem(getTabStorageKey('userRole'))
        } else {
            // 不记住：使用sessionStorage（仅当前标签页）
            sessionStorage.setItem(getTabStorageKey('userInfo'), JSON.stringify(newInfo))
            sessionStorage.setItem(getTabStorageKey('userRole'), role.value)
        }
    }
    const removeInfo = () => {
        info.value = {}
        role.value='user'
        rememberMe.value=false
        // 清除全局数据
        localStorage.removeItem('global_userInfo')
        localStorage.removeItem('global_userRole')
        // 清除当前标签页的数据
        sessionStorage.removeItem(getTabStorageKey('userInfo'))
        sessionStorage.removeItem(getTabStorageKey('userRole'))

    }
      // 初始化时从存储中恢复用户信息
      const initInfo = () => {
          // 优先检查是否有"记住我"的全局数据
          const globalInfo = localStorage.getItem('global_userInfo')
          const globalRole = localStorage.getItem('global_userRole')
          if (globalInfo) {
              try {
                  info.value = JSON.parse(globalInfo)
                  role.value = globalRole || 'user'
                  rememberMe.value = true
                  return
              } catch (e) {
                  console.error('解析全局用户信息失败:', e)
              }
          }

          // 否则检查当前标签页的数据
          const tabInfo = sessionStorage.getItem(getTabStorageKey('userInfo'))
          const tabRole = sessionStorage.getItem(getTabStorageKey('userRole'))
          if (tabInfo) {
              try {
                  info.value = JSON.parse(tabInfo)
                  role.value = tabRole || 'user'
                  rememberMe.value = false
              } catch (e) {
                  console.error('解析标签页用户信息失败:', e)
              }
          }
      }

      // 在store创建时立即执行
      initInfo()
      // 👇 新增：判断是否为管理员
      const isAdmin = computed(() => canEnterAdmin(role.value))

      // 👇 新增：判断是否为普通用户
      const isUser = computed(() => role.value === 'user' )

      return {
          info,
          role,
          rememberMe,
          setInfo,
          removeInfo,
          isAdmin,
          isUser
      }
  },
  { persist: false },
)
export default useUserInfoStore

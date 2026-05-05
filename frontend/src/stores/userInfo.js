import { defineStore } from 'pinia'
import { ref ,computed} from 'vue'
import { normalizeRole, canEnterAdmin } from '@/utils/roles'
const useUserInfoStore = defineStore(
  'userInfo',
  () => {
    //定义状态相关的内容
    const info = ref({})
      const role = ref('user')

    const setInfo = (newInfo) => {
      info.value = newInfo
        role.value = normalizeRole(newInfo?.role)
    }
    const removeInfo = () => {
      info.value = {}
        role.value='user'
    }
      // 👇 新增：判断是否为管理员
      const isAdmin = computed(() => canEnterAdmin(role.value))

      // 👇 新增：判断是否为普通用户
      const isUser = computed(() => role.value === 'user' )

      return {
          info,
          role,
          setInfo,
          removeInfo,
          isAdmin,
          isUser
      }
  },
  { persist: true },
)
export default useUserInfoStore

//导入request
import request from '@/utils/request'

//提供调用注册接口的函数
export const userRegisterService = (registerData) => {
  //借助URLSearchParams完成传递数据
  const params = new URLSearchParams()
  for (let key in registerData) {
    params.append(key, registerData[key])
  }
  return request.post('/user/register', params)
}

//提供调用登录接口的函数
export const userLoginService = (loginData) => {
  const params = new URLSearchParams()
  for (let key in loginData) {
    params.append(key, loginData[key])
  }
  return request.post('/user/login', params)
}
//获取用户详细信息
export const userInfoService = () => {
  return request.get('/user/userInfo')
}

//修改用户信息
export const userUpdateService = (userInfoData) => {
  return request.put('/user/update', userInfoData)
}

//更新用户头像
export const userUpdateAvatarService = (avatarUrl) => {
  const params = new URLSearchParams()
  params.append('avatarUrl', avatarUrl)
  return request.put('/user/updateAvatar', params)
}

//更新密码
export const userUpdatePwdService = (pwdData) => {
  return request.patch('/user/updatePwd', pwdData)
}

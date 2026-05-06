/** 与后端约定一致：统一小写比较（兼容库中 ADMIN、ADMIn 等写法） */
export function normalizeRole(role) {
  if (role == null || role === '') return 'user'
  return String(role).toLowerCase()
}

/** 可进入管理后台的角色（与后端用户表 role 字段一致，大小写不敏感） */
export function canEnterAdmin(role) {
  const r = normalizeRole(role)
  return r === 'admin' || r === 'super_admin'
}

export function routeRoleAllows(routeRole, userRole) {
  const u = normalizeRole(userRole)
  if (!routeRole) return true
  if (routeRole === 'admin') return canEnterAdmin(u)
  // 用户端路由：普通用户与管理员均可访问
  if (routeRole === 'user') return u === 'user' || canEnterAdmin(u)
  return u === normalizeRole(routeRole)
}


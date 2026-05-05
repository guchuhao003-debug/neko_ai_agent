import http from './http'

/**
 * 用户注册
 * @param {Object} data - 注册数据
 * @param {string} data.userName - 用户名
 * @param {string} data.userAccount - 账号
 * @param {string} data.userPassword - 密码
 * @param {string} data.checkPassword - 确认密码
 * @returns {Promise<{code: number, data: number, message: string}>}
 */
export const userRegister = (data) => {
  return http.post('/user/register', data)
}

/**
 * 用户登录
 * @param {Object} data - 登录数据
 * @param {string} data.userAccount - 账号
 * @param {string} data.userPassword - 密码
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export const userLogin = (data) => {
  return http.post('/user/login', data)
}

/**
 * 获取当前登录用户
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export const getCurrentUser = () => {
  return http.get('/user/get/current')
}

/**
 * 用户注销登录
 * @returns {Promise<{code: number, data: boolean, message: string}>}
 */
export const userLogout = () => {
  return http.post('/user/logout')
}

/**
 * 发送邮箱验证码
 * @param {string} userEmail - 邮箱地址
 * @returns {Promise<{code: number, data: string, message: string}>}
 */
export const sendCode = (userEmail) => {
  return http.post('/user/send_code', null, { params: { userEmail } })
}

/**
 * 邮箱验证码登录
 * @param {string} userEmail - 邮箱地址
 * @param {string} inputCode - 验证码
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export const userLoginByEmail = (userEmail, inputCode) => {
  return http.post('/user/login/email', null, { params: { userEmail, inputCode } })
}

/**
 * 用户修改个人信息
 * @param {Object} data - 更新数据
 * @param {number} data.id - 用户 ID
 * @param {string} [data.userName] - 用户名
 * @param {string} [data.userEmail] - 邮箱
 * @param {string} [data.userProfile] - 简介
 * @returns {Promise<{code: number, data: boolean, message: string}>}
 */
export const GlobalUpdateUser = (data) => {
  return http.post('/user/global/update', data)
}

/**
 * 用户上传头像
 * @param {File} file - 头像图片文件
 * @returns {Promise<{code: number, data: string, message: string}>}
 */
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/user/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ==================== 管理员接口 ====================

/**
 * 创建用户（管理员）
 * @param {Object} data - { userName, userAccount, userAvatar, userProfile, userRole }
 * @returns {Promise}
 */
export const addUser = (data) => {
  return http.post('/user/add', data)
}

/**
 * 根据 id 获取用户（管理员）
 * @param {string|number} id
 * @returns {Promise}
 */
export const getUserById = (id) => {
  return http.get('/user/get', { params: { id } })
}

/**
 * 删除用户（管理员）
 * @param {string|number} id
 * @returns {Promise}
 */
export const deleteUser = (id) => {
  return http.post('/user/delete', { id })
}

/**
 * 更新用户信息（管理员）
 * @param {Object} data - { id, userName, userEmail, userAvatar, userProfile, userRole }
 * @returns {Promise}
 */
export const updateUser = (data) => {
  return http.post('/user/update', data)
}

/**
 * 分页查询用户列表（管理员）
 * @param {Object} data - { current, pageSize, userName, userAccount, userRole, sortField, sortOrder }
 * @returns {Promise}
 */
export const listUserByPage = (data) => {
  return http.post('/user/list/page/vo', data)
}

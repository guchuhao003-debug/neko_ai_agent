import http from './http'

/**
 * 创建智能体。
 * @param {Object} data - 智能体配置
 * @returns {Promise}
 */
export const createAgent = (data) => {
  return http.post('/agent/create', data)
}

/**
 * 更新智能体。
 * @param {Object} data - 智能体配置
 * @returns {Promise}
 */
export const updateAgent = (data) => {
  return http.post('/agent/update', data)
}

/**
 * 删除智能体。
 * @param {string|number} id - 智能体 ID
 * @returns {Promise}
 */
export const deleteAgent = (id) => {
  return http.post('/agent/delete', { id })
}

/**
 * 获取我的智能体列表。
 * @param {Object} params - 分页参数
 * @returns {Promise}
 */
export const listMyAgents = (params = {}) => {
  return http.get('/agent/list/my', { params })
}

/**
 * 获取公开智能体列表。
 * @param {Object} params - 分页参数
 * @returns {Promise}
 */
export const listPublicAgents = (params = {}) => {
  return http.get('/agent/list/public', { params })
}

/**
 * 获取智能体详情。
 * @param {string|number} id - 智能体 ID
 * @returns {Promise}
 */
export const getAgent = (id) => {
  return http.get('/agent/get', { params: { id } })
}

/**
 * 上传智能体头像。
 * @param {File} file - 图片文件
 * @returns {Promise}
 */
export const uploadAgentAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/agent/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 获取所有智能体列表（管理员）。
 * @param {Object} params - 分页参数
 * @returns {Promise}
 */
export const listAllAgents = (params = {}) => {
  return http.get('/agent/list/all', { params })
}

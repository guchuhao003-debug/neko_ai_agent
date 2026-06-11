import http from './http'

/**
 * 获取当前用户积分。
 * @returns {Promise}
 */
export const getMyQuota = () => {
  return http.get('/quota/my')
}

/**
 * 用户兑换积分。
 * @param {string} code 兑换码
 * @returns {Promise}
 */
export const redeemQuotaCode = (code) => {
  return http.post('/quota/redeem', { code })
}

/**
 * 管理员批量生成兑换码。
 * @param {Object} data 生成参数
 * @returns {Promise}
 */
export const generateQuotaCodes = (data) => {
  return http.post('/quota/admin/redeem-code/generate', data)
}

/**
 * 管理员查询兑换码列表。
 * @param {Object} data 查询参数
 * @returns {Promise}
 */
export const listQuotaCodes = (data) => {
  return http.post('/quota/admin/redeem-code/list', data)
}

/**
 * 管理员删除兑换码。
 * @param {string|number} id 兑换码 ID
 * @returns {Promise}
 */
export const deleteQuotaCode = (id) => {
  return http.post('/quota/admin/redeem-code/delete', { id })
}

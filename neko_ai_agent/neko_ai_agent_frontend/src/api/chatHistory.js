import http from './http'

/**
 * 获取对话历史列表
 * @param {string} appType - 应用类型: love / pet / manus
 * @returns {Promise}
 */
export const getChatHistoryList = (appType) => {
  return http.get('/chat/history/list', { params: { appType } })
}

/**
 * 获取单个对话的完整消息详情
 * @param {string} chatId - 对话ID
 * @param {string} appType - 应用类型: love / pet / manus
 * @returns {Promise}
 */
export const getChatHistoryDetail = (chatId, appType) => {
  return http.get('/chat/history/detail', { params: { chatId, appType } })
}

/**
 * 保存对话消息
 * @param {Object} data - { chatId, appType, messages: [{role, content, time}] }
 * @returns {Promise}
 */
export const saveChatMessages = (data) => {
  return http.post('/chat/history/save', data)
}

/**
 * 删除对话记录
 * @param {string} chatId - 对话ID
 * @param {string} appType - 应用类型: love / pet / manus
 * @returns {Promise}
 */
export const deleteChatHistory = (chatId, appType) => {
  return http.delete('/chat/history/delete', { params: { chatId, appType } })
}

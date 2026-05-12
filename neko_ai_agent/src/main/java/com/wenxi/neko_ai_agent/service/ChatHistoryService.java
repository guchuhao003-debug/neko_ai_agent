package com.wenxi.neko_ai_agent.service;

import com.wenxi.neko_ai_agent.model.dto.ChatHistoryDetailDTO;
import com.wenxi.neko_ai_agent.model.dto.ChatHistoryListDTO;

import java.util.List;

/**
 * 对话历史记录 Service
 */
public interface ChatHistoryService {

    /**
     * 获取用户某应用的对话历史列表
     *
     * @param userId  用户ID
     * @param appType 应用类型: love / pet / manus
     * @return 对话历史列表
     */
    List<ChatHistoryListDTO> getChatHistoryList(Long userId, String appType);

    /**
     * 获取单个对话的完整消息详情
     *
     * @param userId  用户ID
     * @param chatId  对话ID
     * @param appType 应用类型: love / pet / manus
     * @return 对话详情
     */
    ChatHistoryDetailDTO getChatHistoryDetail(Long userId, String chatId, String appType);

    /**
     * 保存对话消息（每次AI回复后调用）
     *
     * @param userId   用户ID
     * @param chatId   对话ID
     * @param appType  应用类型: love / pet / manus
     * @param messages 完整消息列表
     */
    void saveChatMessages(Long userId, String chatId, String appType, List<ChatHistoryDetailDTO.ChatMessage> messages);

    /**
     * 删除对话记录
     *
     * @param userId  用户ID
     * @param chatId  对话ID
     * @param appType 应用类型: love / pet / manus
     * @return 是否删除成功
     */
    boolean deleteChatHistory(Long userId, String chatId, String appType);
}

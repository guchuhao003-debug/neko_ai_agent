package com.wenxi.neko_ai_agent.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 保存对话消息请求
 */
@Data
public class SaveChatMessageRequest implements Serializable {

    /**
     * 对话ID
     */
    private String chatId;

    /**
     * 应用类型: love / pet / manus
     */
    private String appType;

    /**
     * 消息列表
     */
    private List<ChatHistoryDetailDTO.ChatMessage> messages;

    private static final long serialVersionUID = 1L;
}

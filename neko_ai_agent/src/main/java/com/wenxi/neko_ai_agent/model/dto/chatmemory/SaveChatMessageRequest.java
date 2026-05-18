package com.wenxi.neko_ai_agent.model.dto.chatmemory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
    @NotBlank(message = "对话ID不能为空")
    private String chatId;

    /**
     * 应用类型: love / pet / manus
     */
    @NotBlank(message = "应用类型不能为空")
    private String appType;

    /**
     * 消息列表
     */
    @NotEmpty(message = "消息列表不能为空")
    private List<ChatHistoryDetailDTO.ChatMessage> messages;

    private static final long serialVersionUID = 1L;
}

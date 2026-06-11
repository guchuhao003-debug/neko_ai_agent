package com.wenxi.neko_ai_agent.model.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 自定义智能体聊天请求。
 */
@Data
public class AgentChatRequest implements Serializable {

    /**
     * 智能体 ID。
     */
    @NotNull(message = "智能体 ID 不能为空")
    private Long agentId;

    /**
     * 会话 ID。
     */
    @NotBlank(message = "会话 ID 不能为空")
    private String chatId;

    /**
     * 用户消息。
     */
    @NotBlank(message = "用户消息不能为空")
    private String message;

    /**
     * 临时指定模型 ID。
     */
    private String modelId;

    private static final long serialVersionUID = 1L;
}

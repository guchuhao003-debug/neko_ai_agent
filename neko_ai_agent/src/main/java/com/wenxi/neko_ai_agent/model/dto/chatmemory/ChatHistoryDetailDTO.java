package com.wenxi.neko_ai_agent.model.dto.chatmemory;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 对话历史详情 DTO（包含完整消息列表）
 */
@Data
public class ChatHistoryDetailDTO implements Serializable {

    /**
     * 对话ID
     */
    private String chatId;

    /**
     * 消息列表
     */
    private List<ChatMessage> messages;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 单条消息
     */
    @Data
    public static class ChatMessage implements Serializable {
        /**
         * 角色: user / ai
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 发送时间
         */
        private String time;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}

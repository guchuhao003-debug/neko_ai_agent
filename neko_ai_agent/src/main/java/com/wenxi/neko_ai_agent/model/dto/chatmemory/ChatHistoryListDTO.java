package com.wenxi.neko_ai_agent.model.dto.chatmemory;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 对话历史列表 DTO（仅展示摘要信息）
 */
@Data
public class ChatHistoryListDTO implements Serializable {

    /**
     * 对话ID
     */
    private String chatId;

    /**
     * 最后一条消息内容（用于列表展示）
     */
    private String lastMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}

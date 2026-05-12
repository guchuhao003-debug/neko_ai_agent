package com.wenxi.neko_ai_agent.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 对话历史记录
 */
@Data
public class ChatHistory implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 对话id
     */
    private String chatId;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 对话记录（JSON格式存储）
     */
    private String messages;

    /**
     * 最后一条消息内容（用于列表展示）
     */
    private String lastMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

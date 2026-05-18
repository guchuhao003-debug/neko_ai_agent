package com.wenxi.neko_ai_agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenxi.neko_ai_agent.model.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天历史记录 Mapper
 */
@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

}

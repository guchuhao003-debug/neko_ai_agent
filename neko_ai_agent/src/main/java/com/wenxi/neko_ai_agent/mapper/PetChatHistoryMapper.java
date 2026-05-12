package com.wenxi.neko_ai_agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenxi.neko_ai_agent.model.entity.PetChatHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 宠物专家对话历史 Mapper
 */
@Mapper
public interface PetChatHistoryMapper extends BaseMapper<PetChatHistory> {
}

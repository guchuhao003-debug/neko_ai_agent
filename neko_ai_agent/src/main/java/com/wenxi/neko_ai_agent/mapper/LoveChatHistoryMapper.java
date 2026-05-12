package com.wenxi.neko_ai_agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenxi.neko_ai_agent.model.entity.LoveChatHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 恋爱大师对话历史 Mapper
 */
@Mapper
public interface LoveChatHistoryMapper extends BaseMapper<LoveChatHistory> {
}

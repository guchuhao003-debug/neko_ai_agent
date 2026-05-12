package com.wenxi.neko_ai_agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenxi.neko_ai_agent.model.entity.ManusChatHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 超级智能体对话历史 Mapper
 */
@Mapper
public interface ManusChatHistoryMapper extends BaseMapper<ManusChatHistory> {
}

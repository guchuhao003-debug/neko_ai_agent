package com.wenxi.neko_ai_agent.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenxi.neko_ai_agent.model.entity.Agent;
import org.apache.ibatis.annotations.Update;

/**
* @author kk
* @description 针对表【agent】的数据库操作Mapper
* @createDate 2026-05-13 22:26:51
* @Entity generator.domain.Agent
*/
public interface AgentMapper extends BaseMapper<Agent> {

    @Update("UPDATE agent SET useCount = useCount + 1 WHERE id = #{agentId}")
    void incrementUserCount(Long agentId);
}





package com.wenxi.neko_ai_agent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wenxi.neko_ai_agent.model.dto.agent.AgentCreateRequest;
import com.wenxi.neko_ai_agent.model.dto.agent.AgentUpdateRequest;
import com.wenxi.neko_ai_agent.model.entity.Agent;
import com.wenxi.neko_ai_agent.model.entity.User;
import reactor.core.publisher.Flux;

/**
 * 智能体服务。
 */
public interface AgentService extends IService<Agent> {

    /**
     * 创建智能体。
     *
     * @param userId 创建者用户 ID
     * @param agentCreateRequest 创建请求
     * @return 创建后的智能体
     */
    Agent createAgent(String userId, AgentCreateRequest agentCreateRequest);

    /**
     * 更新智能体。
     *
     * @param userId 当前用户 ID
     * @param agentUpdateRequest 更新请求
     * @return 更新后的智能体
     */
    Agent updateAgent(String userId, AgentUpdateRequest agentUpdateRequest);

    /**
     * 删除智能体。
     *
     * @param agentId 智能体 ID
     * @param loginUser 当前登录用户
     */
    void deleteAgent(Long agentId, User loginUser);

    /**
     * 获取用户的智能体列表。
     *
     * @param userId 用户 ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页智能体
     */
    Page<Agent> listUserAgents(String userId, int page, int size);

    /**
     * 获取公开智能体列表。
     *
     * @param page 页码
     * @param size 每页大小
     * @return 公开智能体分页
     */
    Page<Agent> listPublicAgents(int page, int size);

    /**
     * 获取所有智能体列表（管理员）。
     *
     * @param page 页码
     * @param size 每页大小
     * @return 所有智能体分页
     */
    Page<Agent> listAllAgents(int page, int size);

    /**
     * 获取单个智能体详情。
     *
     * @param userId 当前用户 ID，可为空
     * @param agentId 智能体 ID
     * @return 智能体详情
     */
    Agent getAgent(String userId, Long agentId);

    /**
     * 流式调用自定义智能体。
     *
     * @param userId 当前用户 ID
     * @param agentId 智能体 ID
     * @param chatId 会话 ID
     * @param modelId 临时指定模型 ID
     * @param userMessage 用户消息
     * @return SSE 文本流
     */
    Flux<String> streamChat(String userId, Long agentId, String chatId, String modelId,
                            String userMessage);
}

package com.wenxi.neko_ai_agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wenxi.neko_ai_agent.advisor.MyLoggerAdvisor;
import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import com.wenxi.neko_ai_agent.exception.ThrowUtils;
import com.wenxi.neko_ai_agent.mapper.AgentMapper;
import com.wenxi.neko_ai_agent.model.dto.agent.AgentCreateRequest;
import com.wenxi.neko_ai_agent.model.dto.agent.AgentUpdateRequest;
import com.wenxi.neko_ai_agent.model.entity.Agent;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.service.AgentService;
import com.wenxi.neko_ai_agent.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 智能体服务实现。
 */
@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    /**
     * 默认系统提示词。
     */
    public static final String SYSTEM_PROMPT = "你是一个智能 AI 助手，请根据用户问题给出"
            + "准确、清晰、结构化的回答。";

    private static final int DEFAULT_MAX_TOKENS = 2048;

    private static final BigDecimal DEFAULT_TEMPERATURE = BigDecimal.valueOf(0.7);

    private static final int CHAT_MEMORY_CONVERSATION_ID_MAX_LENGTH = 36;

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private Map<String, ChatModel> chatModelMap;

    @Resource
    private ChatMemory chatMemory;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 创建智能体。
     *
     * @param userId 创建者用户 ID
     * @param agentCreateRequest 创建请求
     * @return 创建后的智能体
     */
    @Override
    public Agent createAgent(String userId, AgentCreateRequest agentCreateRequest) {
        ThrowUtils.throwIf(StrUtil.isBlank(userId), ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        ThrowUtils.throwIf(agentCreateRequest == null, ErrorCode.PARAMS_ERROR);
        validateAgentCreateRequest(agentCreateRequest);

        Agent agent = new Agent();
        BeanUtil.copyProperties(agentCreateRequest, agent);
        agent.setUserId(userId);
        Integer maxTokens = resolveMaxTokens(agentCreateRequest.getMaxTokens(),
                agentCreateRequest.getMaxToken());
        agent.setMaxTokens(maxTokens == null ? DEFAULT_MAX_TOKENS : maxTokens);
        agent.setTemperature(resolveTemperature(agentCreateRequest.getTemperature()));
        agent.setIsPublic(Boolean.TRUE.equals(agentCreateRequest.getIsPublic()));
        agent.setStatus(true);
        agent.setUseCount(0);
        this.save(agent);
        return agent;
    }

    /**
     * 更新智能体。
     *
     * @param userId 当前用户 ID
     * @param agentUpdateRequest 更新请求
     * @return 更新后的智能体
     */
    @Override
    public Agent updateAgent(String userId, AgentUpdateRequest agentUpdateRequest) {
        ThrowUtils.throwIf(StrUtil.isBlank(userId), ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        ThrowUtils.throwIf(agentUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(agentUpdateRequest.getId() == null || agentUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "智能体 ID 不能为空");

        Agent oldAgent = this.getById(agentUpdateRequest.getId());
        ThrowUtils.throwIf(oldAgent == null, ErrorCode.NOT_FOUND_ERROR, "智能体不存在");
        checkManageAuth(oldAgent, userId);
        validateAgentUpdateRequest(agentUpdateRequest);

        Agent updateAgent = new Agent();
        BeanUtil.copyProperties(agentUpdateRequest, updateAgent);
        updateAgent.setId(agentUpdateRequest.getId());
        Integer maxTokens = resolveMaxTokens(agentUpdateRequest.getMaxTokens(),
                agentUpdateRequest.getMaxToken());
        if (maxTokens != null) {
            updateAgent.setMaxTokens(maxTokens);
        }
        boolean updated = this.updateById(updateAgent);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新智能体失败");
        return this.getById(agentUpdateRequest.getId());
    }

    /**
     * 删除智能体。
     *
     * @param agentId 智能体 ID
     * @param loginUser 当前登录用户
     */
    @Override
    public void deleteAgent(Long agentId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(agentId == null || agentId <= 0, ErrorCode.PARAMS_ERROR);

        Agent agent = this.getById(agentId);
        ThrowUtils.throwIf(agent == null, ErrorCode.NOT_FOUND_ERROR, "当前智能体不存在");
        checkManageAuth(agent, String.valueOf(loginUser.getId()));

        transactionTemplate.execute(status -> {
            boolean result = this.removeById(agentId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除失败");
            return true;
        });
    }

    /**
     * 获取用户智能体列表。
     *
     * @param userId 用户 ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页智能体
     */
    @Override
    public Page<Agent> listUserAgents(String userId, int page, int size) {
        ThrowUtils.throwIf(StrUtil.isBlank(userId), ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        Page<Agent> pageRequest = new Page<>(normalizePage(page), normalizePageSize(size));
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getUserId, userId)
                .orderByDesc(Agent::getUpdateTime);
        return this.page(pageRequest, queryWrapper);
    }

    /**
     * 获取公开智能体列表。
     *
     * @param page 页码
     * @param size 每页大小
     * @return 公开智能体分页
     */
    @Override
    public Page<Agent> listPublicAgents(int page, int size) {
        Page<Agent> pageRequest = new Page<>(normalizePage(page), normalizePageSize(size));
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getIsPublic, true)
                .eq(Agent::getStatus, true)
                .orderByDesc(Agent::getUpdateTime);
        return this.page(pageRequest, queryWrapper);
    }

    /**
     * 获取所有智能体列表（管理员）。
     *
     * @param page 页码
     * @param size 每页大小
     * @return 所有智能体分页
     */
    @Override
    public Page<Agent> listAllAgents(int page, int size) {
        Page<Agent> pageRequest = new Page<>(normalizePage(page), normalizePageSize(size));
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Agent::getUpdateTime);
        return this.page(pageRequest, queryWrapper);
    }

    /**
     * 获取单个智能体详情。
     *
     * @param userId 当前用户 ID，可为空
     * @param agentId 智能体 ID
     * @return 智能体详情
     */
    @Override
    public Agent getAgent(String userId, Long agentId) {
        ThrowUtils.throwIf(agentId == null || agentId <= 0, ErrorCode.PARAMS_ERROR);
        Agent agent = this.getById(agentId);
        ThrowUtils.throwIf(agent == null, ErrorCode.NOT_FOUND_ERROR, "智能体不存在");

        boolean owner = StrUtil.isNotBlank(userId) && agent.getUserId().equals(userId);
        boolean admin = isAdmin(userId);
        if ((!Boolean.TRUE.equals(agent.getStatus()) || !Boolean.TRUE.equals(agent.getIsPublic()))
                && !owner && !admin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该智能体");
        }
        return agent;
    }

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
    @Override
    public Flux<String> streamChat(String userId, Long agentId, String chatId, String modelId,
                                   String userMessage) {
        ThrowUtils.throwIf(StrUtil.isBlank(userId), ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(chatId), ErrorCode.PARAMS_ERROR, "会话 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(userMessage), ErrorCode.PARAMS_ERROR, "用户消息不能为空");

        Agent agent = this.getAgent(userId, agentId);
        ThrowUtils.throwIf(!Boolean.TRUE.equals(agent.getStatus()), ErrorCode.FORBIDDEN_ERROR,
                "智能体已禁用");

        ChatModel chatModel = resolveChatModel(StrUtil.blankToDefault(modelId, agent.getModelId()));
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem(StrUtil.blankToDefault(agent.getSystemPrompt(), SYSTEM_PROMPT))
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
        String conversationId = buildConversationId(userId, agentId, chatId);

        return chatClient.prompt()
                .user(userMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .doOnSubscribe(subscription -> this.baseMapper.incrementUserCount(agentId));
    }

    /**
     * 构造聊天记忆会话 ID。
     *
     * @param userId 当前用户 ID
     * @param agentId 智能体 ID
     * @param chatId 前端会话 ID
     * @return 可安全写入聊天记忆表的会话 ID
     */
    private String buildConversationId(String userId, Long agentId, String chatId) {
        String rawConversationId = "agent:" + agentId + ":" + userId + ":" + chatId;
        String conversationId = SecureUtil.md5(rawConversationId);
        ThrowUtils.throwIf(conversationId.length() > CHAT_MEMORY_CONVERSATION_ID_MAX_LENGTH,
                ErrorCode.OPERATION_ERROR, "聊天记忆会话 ID 超出限制");
        return conversationId;
    }

    private ChatOptions buildChatOptions(ChatModel chatModel, Agent agent) {
        double temperature = agent.getTemperature() != null ? agent.getTemperature().doubleValue() : 0.7;
        int maxTokens = agent.getMaxTokens() != null ? agent.getMaxTokens() : 2048;

        if (chatModel instanceof DashScopeChatModel) {
            return DashScopeChatOptions.builder()
                    .withTemperature(temperature)
                    .withMaxToken(maxTokens)
                    .build();
        } else if (chatModel instanceof OpenAiChatModel) {
            return OpenAiChatOptions.builder()
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();
        }
        return ChatOptions.builder()
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    /**
     * 校验新增智能体请求。
     *
     * @param request 创建请求
     */
    private void validateAgentCreateRequest(AgentCreateRequest request) {
        ThrowUtils.throwIf(StrUtil.isBlank(request.getName()), ErrorCode.PARAMS_ERROR,
                "智能体名称不能为空");
        ThrowUtils.throwIf(request.getName().length() > 50, ErrorCode.PARAMS_ERROR,
                "智能体名称不能超过 50 个字符");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getSystemPrompt()), ErrorCode.PARAMS_ERROR,
                "系统提示词不能为空");
        ThrowUtils.throwIf(request.getSystemPrompt().length() > 4000, ErrorCode.PARAMS_ERROR,
                "系统提示词不能超过 4000 个字符");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getModelId()), ErrorCode.PARAMS_ERROR,
                "模型 ID 不能为空");
        validateModelId(request.getModelId());
        validateGenerationOptions(request.getTemperature(),
                resolveMaxTokens(request.getMaxTokens(), request.getMaxToken()));
    }

    /**
     * 校验更新智能体请求。
     *
     * @param request 更新请求
     */
    private void validateAgentUpdateRequest(AgentUpdateRequest request) {
        if (request.getName() != null) {
            ThrowUtils.throwIf(StrUtil.isBlank(request.getName()), ErrorCode.PARAMS_ERROR,
                    "智能体名称不能为空");
            ThrowUtils.throwIf(request.getName().length() > 50, ErrorCode.PARAMS_ERROR,
                    "智能体名称不能超过 50 个字符");
        }
        if (request.getSystemPrompt() != null) {
            ThrowUtils.throwIf(StrUtil.isBlank(request.getSystemPrompt()), ErrorCode.PARAMS_ERROR,
                    "系统提示词不能为空");
            ThrowUtils.throwIf(request.getSystemPrompt().length() > 4000, ErrorCode.PARAMS_ERROR,
                    "系统提示词不能超过 4000 个字符");
        }
        if (StrUtil.isNotBlank(request.getModelId())) {
            validateModelId(request.getModelId());
        }
        validateGenerationOptions(request.getTemperature(),
                resolveMaxTokens(request.getMaxTokens(), request.getMaxToken()));
    }

    /**
     * 校验模型生成参数。
     *
     * @param temperature 温度
     * @param maxTokens 最大 token 数
     */
    private void validateGenerationOptions(BigDecimal temperature, Integer maxTokens) {
        if (temperature != null) {
            BigDecimal min = BigDecimal.ZERO;
            BigDecimal max = BigDecimal.valueOf(2);
            ThrowUtils.throwIf(temperature.compareTo(min) < 0 || temperature.compareTo(max) > 0,
                    ErrorCode.PARAMS_ERROR, "温度参数必须在 0 到 2 之间");
        }
        if (maxTokens != null) {
            ThrowUtils.throwIf(maxTokens < 256 || maxTokens > 8192, ErrorCode.PARAMS_ERROR,
                    "最大 Token 数必须在 256 到 8192 之间");
        }
    }

    /**
     * 校验模型 ID 是否可用。
     *
     * @param modelId 模型 ID
     */
    private void validateModelId(String modelId) {
        ThrowUtils.throwIf(!chatModelMap.containsKey(modelId), ErrorCode.PARAMS_ERROR,
                "模型不存在或未配置");
    }

    /**
     * 校验管理权限。
     *
     * @param agent 智能体
     * @param userId 当前用户 ID
     */
    private void checkManageAuth(Agent agent, String userId) {
        boolean owner = agent.getUserId().equals(userId);
        boolean admin = isAdmin(userId);
        if (!owner && !admin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该智能体");
        }
    }

    /**
     * 判断用户是否管理员。
     *
     * @param userId 用户 ID
     * @return 是否管理员
     */
    private boolean isAdmin(String userId) {
        if (StrUtil.isBlank(userId)) {
            return false;
        }
        User user = userService.getById(userId);
        return userService.isAdmin(user);
    }

    /**
     * 获取可用模型。
     *
     * @param modelId 模型 ID
     * @return 模型
     */
    private static final String DEFAULT_AGENT_MODEL_ID = "deepseek-chat";

    private ChatModel resolveChatModel(String modelId) {
        if (StrUtil.isBlank(modelId)) {
            modelId = DEFAULT_AGENT_MODEL_ID;
        }
        return chatModelMap.getOrDefault(modelId, chatModelMap.getOrDefault(DEFAULT_AGENT_MODEL_ID, dashscopeChatModel));
    }

    /**
     * 获取温度默认值。
     *
     * @param temperature 请求温度
     * @return 温度
     */
    private BigDecimal resolveTemperature(BigDecimal temperature) {
        return temperature == null ? DEFAULT_TEMPERATURE : temperature;
    }

    /**
     * 兼容 maxTokens 与历史 maxToken 字段。
     *
     * @param maxTokens 新字段值
     * @param maxToken 旧字段值
     * @return 最大 token 数
     */
    private Integer resolveMaxTokens(Integer maxTokens, Integer maxToken) {
        if (maxTokens != null) {
            return maxTokens;
        }
        if (maxToken != null) {
            return maxToken;
        }
        return null;
    }

    /**
     * 规范页码。
     *
     * @param page 页码
     * @return 页码
     */
    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    /**
     * 规范分页大小。
     *
     * @param size 分页大小
     * @return 分页大小
     */
    private int normalizePageSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }
}

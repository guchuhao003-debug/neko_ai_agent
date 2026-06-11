package com.wenxi.neko_ai_agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wenxi.neko_ai_agent.app.BaseApp;
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
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * 鏅鸿兘浣撴湇鍔″疄鐜般€? */
@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    /**
     * 榛樿绯荤粺鎻愮ず璇嶃€?     */
    public static final String SYSTEM_PROMPT = "你是一个智能 AI 助手，请根据用户问题给出"
            + "准确、清晰、结构化的回答。";

    private static final int DEFAULT_MAX_TOKENS = 2048;

    private static final BigDecimal DEFAULT_TEMPERATURE = BigDecimal.valueOf(0.7);

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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${neko.cache.agent-config.ttl-minutes:360}")
    private long agentCacheTtlMinutes;

    private static final String AGENT_CACHE_KEY_PREFIX = "agent:config:";

    // ==================== Agent 缂撳瓨 ====================

    /**
     * 浠庣紦瀛樻垨鏁版嵁搴撹幏鍙?Agent 瀹炰綋锛堜笉鍖呭惈鏉冮檺鏍￠獙锛夈€?     */
    private Agent getAgentById(Long agentId) {
        String cacheKey = AGENT_CACHE_KEY_PREFIX + agentId;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return JSON.parseObject(cached, Agent.class);
            }
        } catch (Exception e) {
            log.warn("Redis 璇诲彇 Agent 缂撳瓨澶辫触锛岄檷绾ф煡璇?MySQL: " + e.getMessage());
        }

        Agent agent = this.baseMapper.selectById(agentId);
        if (agent != null) {
            try {
                stringRedisTemplate.opsForValue().set(AGENT_CACHE_KEY_PREFIX + agent.getId(),
                        JSON.toJSONString(agent), Duration.ofMinutes(agentCacheTtlMinutes));
            } catch (Exception e) {
                log.warn("Redis 鍥炲～ Agent 缂撳瓨澶辫触: " + e.getMessage());
            }
        }
        return agent;
    }

    /**
     * 鍐欏叆 Agent 閰嶇疆缂撳瓨锛岀紦瀛樺け璐ヤ笉褰卞搷涓绘祦绋嬨€?     */
    private void cacheAgent(Agent agent) {
        if (agent == null || agent.getId() == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(AGENT_CACHE_KEY_PREFIX + agent.getId(),
                    JSON.toJSONString(agent), Duration.ofMinutes(agentCacheTtlMinutes));
        } catch (Exception e) {
            log.warn("Redis 鍐欏叆 Agent 缂撳瓨澶辫触: " + e.getMessage());
        }
    }

    /**
     * 浣?Agent 缂撳瓨澶辨晥銆?     */
    private void evictAgentCache(Long agentId) {
        try {
            stringRedisTemplate.delete(AGENT_CACHE_KEY_PREFIX + agentId);
        } catch (Exception e) {
            log.warn("Redis 鍒犻櫎 Agent 缂撳瓨澶辫触: " + e.getMessage());
        }
    }

    // ==================== CRUD ====================

    /**
     * 鍒涘缓鏅鸿兘浣撱€?     *
     * @param userId 鍒涘缓鑰呯敤鎴?ID
     * @param agentCreateRequest 鍒涘缓璇锋眰
     * @return 鍒涘缓鍚庣殑鏅鸿兘浣?     */
    @Override
    public Agent createAgent(String userId, AgentCreateRequest agentCreateRequest) {
        ThrowUtils.throwIf(StrUtil.isBlank(userId), ErrorCode.PARAMS_ERROR, "鐢ㄦ埛 ID 涓嶈兘涓虹┖");
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
        boolean saved = this.save(agent);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建智能体失败");
        cacheAgent(agent);
        return agent;
    }

    /**
     * 鏇存柊鏅鸿兘浣撱€?     *
     * @param userId 褰撳墠鐢ㄦ埛 ID
     * @param agentUpdateRequest 鏇存柊璇锋眰
     * @return 鏇存柊鍚庣殑鏅鸿兘浣?     */
    @Override
    public Agent updateAgent(String userId, AgentUpdateRequest agentUpdateRequest) {
        ThrowUtils.throwIf(StrUtil.isBlank(userId), ErrorCode.PARAMS_ERROR, "鐢ㄦ埛 ID 涓嶈兘涓虹┖");
        ThrowUtils.throwIf(agentUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(agentUpdateRequest.getId() == null || agentUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "鏅鸿兘浣?ID 涓嶈兘涓虹┖");

        Agent oldAgent = this.getById(agentUpdateRequest.getId());
        ThrowUtils.throwIf(oldAgent == null, ErrorCode.NOT_FOUND_ERROR, "鏅鸿兘浣撲笉瀛樺湪");
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
        evictAgentCache(agentUpdateRequest.getId());
        return this.getById(agentUpdateRequest.getId());
    }

    /**
     * 鍒犻櫎鏅鸿兘浣撱€?     *
     * @param agentId 鏅鸿兘浣?ID
     * @param loginUser 褰撳墠鐧诲綍鐢ㄦ埛
     */
    @Override
    public void deleteAgent(Long agentId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(agentId == null || agentId <= 0, ErrorCode.PARAMS_ERROR);

        Agent agent = this.getById(agentId);
        ThrowUtils.throwIf(agent == null, ErrorCode.NOT_FOUND_ERROR, "褰撳墠鏅鸿兘浣撲笉瀛樺湪");
        checkManageAuth(agent, String.valueOf(loginUser.getId()));

        transactionTemplate.execute(status -> {
            boolean result = this.removeById(agentId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "鍒犻櫎澶辫触");
            evictAgentCache(agentId);
            return true;
        });
    }

    /**
     * 鑾峰彇鐢ㄦ埛鏅鸿兘浣撳垪琛ㄣ€?     *
     * @param userId 鐢ㄦ埛 ID
     * @param page 椤电爜
     * @param size 姣忛〉澶у皬
     * @return 鍒嗛〉鏅鸿兘浣?     */
    @Override
    public Page<Agent> listUserAgents(String userId, int page, int size) {
        ThrowUtils.throwIf(StrUtil.isBlank(userId), ErrorCode.PARAMS_ERROR, "鐢ㄦ埛 ID 涓嶈兘涓虹┖");
        Page<Agent> pageRequest = new Page<>(normalizePage(page), normalizePageSize(size));
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getUserId, userId)
                .orderByDesc(Agent::getUpdateTime);
        return this.page(pageRequest, queryWrapper);
    }

    /**
     * 鑾峰彇鍏紑鏅鸿兘浣撳垪琛ㄣ€?     *
     * @param page 椤电爜
     * @param size 姣忛〉澶у皬
     * @return 鍏紑鏅鸿兘浣撳垎椤?     */
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
     * 鑾峰彇鎵€鏈夋櫤鑳戒綋鍒楄〃锛堢鐞嗗憳锛夈€?     *
     * @param page 椤电爜
     * @param size 姣忛〉澶у皬
     * @return 鎵€鏈夋櫤鑳戒綋鍒嗛〉
     */
    @Override
    public Page<Agent> listAllAgents(int page, int size) {
        Page<Agent> pageRequest = new Page<>(normalizePage(page), normalizePageSize(size));
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Agent::getUpdateTime);
        return this.page(pageRequest, queryWrapper);
    }

    /**
     * 鑾峰彇鍗曚釜鏅鸿兘浣撹鎯呫€?     *
     * @param userId 褰撳墠鐢ㄦ埛 ID锛屽彲涓虹┖
     * @param agentId 鏅鸿兘浣?ID
     * @return 鏅鸿兘浣撹鎯?     */
    @Override
    public Agent getAgent(String userId, Long agentId) {
        ThrowUtils.throwIf(agentId == null || agentId <= 0, ErrorCode.PARAMS_ERROR);
        Agent agent = getAgentById(agentId);
        ThrowUtils.throwIf(agent == null, ErrorCode.NOT_FOUND_ERROR, "鏅鸿兘浣撲笉瀛樺湪");

        boolean owner = StrUtil.isNotBlank(userId) && StrUtil.equals(agent.getUserId(), userId);
        boolean admin = isAdmin(userId);
        if ((!Boolean.TRUE.equals(agent.getStatus()) || !Boolean.TRUE.equals(agent.getIsPublic()))
                && !owner && !admin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "鏃犳潈璁块棶璇ユ櫤鑳戒綋");
        }
        return agent;
    }

    /**
     * 娴佸紡璋冪敤鑷畾涔夋櫤鑳戒綋銆?     *
     * @param userId 褰撳墠鐢ㄦ埛 ID
     * @param agentId 鏅鸿兘浣?ID
     * @param chatId 浼氳瘽 ID
     * @param modelId 涓存椂鎸囧畾妯″瀷 ID
     * @param userMessage 鐢ㄦ埛娑堟伅
     * @return SSE 鏂囨湰娴?     */
    @Override
    public Flux<String> streamChat(String userId, Long agentId, String chatId, String modelId,
                                   String userMessage) {
        ThrowUtils.throwIf(StrUtil.isBlank(userId), ErrorCode.PARAMS_ERROR, "鐢ㄦ埛 ID 涓嶈兘涓虹┖");
        ThrowUtils.throwIf(StrUtil.isBlank(chatId), ErrorCode.PARAMS_ERROR, "浼氳瘽 ID 涓嶈兘涓虹┖");
        ThrowUtils.throwIf(StrUtil.isBlank(userMessage), ErrorCode.PARAMS_ERROR, "鐢ㄦ埛娑堟伅涓嶈兘涓虹┖");

        Agent agent = this.getAgent(userId, agentId);
        ThrowUtils.throwIf(!Boolean.TRUE.equals(agent.getStatus()), ErrorCode.FORBIDDEN_ERROR,
                "鏅鸿兘浣撳凡绂佺敤");

        ChatModel chatModel = resolveChatModel(StrUtil.blankToDefault(modelId, agent.getModelId()));
        String systemPrompt = StrUtil.blankToDefault(agent.getSystemPrompt(), SYSTEM_PROMPT);
        ChatClient chatClient = BaseApp.buildChatClient(chatModel, systemPrompt, chatMemory);
        String conversationId = buildConversationId(userId, agentId, chatId);
        ChatOptions chatOptions = buildChatOptions(chatModel, agent);

        return chatClient.prompt()
                .options(chatOptions)
                .user(userMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .doOnSubscribe(subscription -> this.baseMapper.incrementUserCount(agentId));
    }

    /**
     * 鏋勯€犺亰澶╄蹇嗕細璇?ID銆?     *
     * @param userId 褰撳墠鐢ㄦ埛 ID
     * @param agentId 鏅鸿兘浣?ID
     * @param chatId 鍓嶇浼氳瘽 ID
     * @return 鍙畨鍏ㄥ啓鍏ヨ亰澶╄蹇嗚〃鐨勪細璇?ID
     */
    private String buildConversationId(String userId, Long agentId, String chatId) {
        String rawConversationId = "agent:" + agentId + ":" + userId + ":" + chatId;
        return SecureUtil.md5(rawConversationId);
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
     * 鏍￠獙鏂板鏅鸿兘浣撹姹傘€?     *
     * @param request 鍒涘缓璇锋眰
     */
    private void validateAgentCreateRequest(AgentCreateRequest request) {
        // 缁撴瀯鏍￠獙涓?@Valid 娉ㄨВ鍙岄噸淇濋殰
        ThrowUtils.throwIf(StrUtil.isBlank(request.getName()), ErrorCode.PARAMS_ERROR,
                "智能体名称不能为空");
        ThrowUtils.throwIf(request.getName().length() > 50, ErrorCode.PARAMS_ERROR,
                "智能体名称不能超过 50 个字符");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getSystemPrompt()), ErrorCode.PARAMS_ERROR,
                "系统提示词不能为空");
        ThrowUtils.throwIf(request.getSystemPrompt().length() > 4000, ErrorCode.PARAMS_ERROR,
                "系统提示词不能超过 4000 个字符");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getModelId()), ErrorCode.PARAMS_ERROR,
                "妯″瀷 ID 涓嶈兘涓虹┖");
        validateModelId(request.getModelId());
        validateGenerationOptions(request.getTemperature(),
                resolveMaxTokens(request.getMaxTokens(), request.getMaxToken()));
    }

    /**
     * 鏍￠獙鏇存柊鏅鸿兘浣撹姹傘€?     *
     * @param request 鏇存柊璇锋眰
     */
    private void validateAgentUpdateRequest(AgentUpdateRequest request) {
        // 缁撴瀯鏍￠獙涓?@Valid 娉ㄨВ鍙岄噸淇濋殰
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
     * 鏍￠獙妯″瀷鐢熸垚鍙傛暟銆?     */
    private void validateGenerationOptions(BigDecimal temperature, Integer maxTokens) {
        if (temperature != null) {
            BigDecimal min = BigDecimal.ZERO;
            BigDecimal max = BigDecimal.valueOf(2);
            ThrowUtils.throwIf(temperature.compareTo(min) < 0 || temperature.compareTo(max) > 0,
                    ErrorCode.PARAMS_ERROR, "娓╁害鍙傛暟蹇呴』鍦?0 鍒?2 涔嬮棿");
        }
        if (maxTokens != null) {
            ThrowUtils.throwIf(maxTokens < 256 || maxTokens > 8192, ErrorCode.PARAMS_ERROR,
                    "鏈€澶?Token 鏁板繀椤诲湪 256 鍒?8192 涔嬮棿");
        }
    }

    /**
     * 鏍￠獙妯″瀷 ID 鏄惁鍙敤銆?     *
     * @param modelId 妯″瀷 ID
     */
    private void validateModelId(String modelId) {
        ThrowUtils.throwIf(!chatModelMap.containsKey(modelId), ErrorCode.PARAMS_ERROR,
                "模型不存在或未配置");
    }

    /**
     * 鏍￠獙绠＄悊鏉冮檺銆?     *
     * @param agent 鏅鸿兘浣?     * @param userId 褰撳墠鐢ㄦ埛 ID
     */
    private void checkManageAuth(Agent agent, String userId) {
        boolean owner = StrUtil.equals(agent.getUserId(), userId);
        boolean admin = isAdmin(userId);
        if (!owner && !admin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "鏃犳潈鎿嶄綔璇ユ櫤鑳戒綋");
        }
    }

    /**
     * 鍒ゆ柇鐢ㄦ埛鏄惁绠＄悊鍛樸€?     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏄惁绠＄悊鍛?     */
    private boolean isAdmin(String userId) {
        if (StrUtil.isBlank(userId)) {
            return false;
        }
        User user = userService.getById(userId);
        return userService.isAdmin(user);
    }

    /**
     * 鑾峰彇鍙敤妯″瀷銆?     *
     * @param modelId 妯″瀷 ID
     * @return 妯″瀷
     */
    private static final String DEFAULT_AGENT_MODEL_ID = "deepseek-chat";

    private ChatModel resolveChatModel(String modelId) {
        if (StrUtil.isBlank(modelId)) {
            modelId = DEFAULT_AGENT_MODEL_ID;
        }
        return chatModelMap.getOrDefault(modelId, chatModelMap.getOrDefault(DEFAULT_AGENT_MODEL_ID, dashscopeChatModel));
    }

    /**
     * 鑾峰彇娓╁害榛樿鍊笺€?     *
     * @param temperature 璇锋眰娓╁害
     * @return 娓╁害
     */
    private BigDecimal resolveTemperature(BigDecimal temperature) {
        return temperature == null ? DEFAULT_TEMPERATURE : temperature;
    }

    /**
     * 鍏煎 maxTokens 涓庡巻鍙?maxToken 瀛楁銆?     *
     * @param maxTokens 鏂板瓧娈靛€?     * @param maxToken 鏃у瓧娈靛€?     * @return 鏈€澶?token 鏁?     */
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
     * 瑙勮寖椤电爜銆?     *
     * @param page 椤电爜
     * @return 椤电爜
     */
    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    /**
     * 瑙勮寖鍒嗛〉澶у皬銆?     *
     * @param size 鍒嗛〉澶у皬
     * @return 鍒嗛〉澶у皬
     */
    private int normalizePageSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }
}

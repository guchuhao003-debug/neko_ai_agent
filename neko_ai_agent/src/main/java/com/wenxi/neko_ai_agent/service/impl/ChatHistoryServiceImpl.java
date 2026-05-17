package com.wenxi.neko_ai_agent.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenxi.neko_ai_agent.mapper.LoveChatHistoryMapper;
import com.wenxi.neko_ai_agent.mapper.ManusChatHistoryMapper;
import com.wenxi.neko_ai_agent.mapper.PetChatHistoryMapper;
import com.wenxi.neko_ai_agent.model.dto.chatmemory.ChatHistoryDetailDTO;
import com.wenxi.neko_ai_agent.model.dto.chatmemory.ChatHistoryListDTO;
import com.wenxi.neko_ai_agent.model.entity.ChatHistory;
import com.wenxi.neko_ai_agent.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 对话历史记录 Service 实现类
 * MySQL + Redis 双重存储
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl implements ChatHistoryService {

    @Resource
    private LoveChatHistoryMapper loveChatHistoryMapper;

    @Resource
    private PetChatHistoryMapper petChatHistoryMapper;

    @Resource
    private ManusChatHistoryMapper manusChatHistoryMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis 缓存过期时间（30分钟）
     */
    private static final long CACHE_EXPIRE_MINUTES = 30;

    /**
     * Redis Key 前缀
     */
    private static final String CACHE_LIST_PREFIX = "chat:history:%s:%s:list";
    private static final String CACHE_DETAIL_PREFIX = "chat:history:%s:%s:messages";

    @Override
    public List<ChatHistoryListDTO> getChatHistoryList(Long userId, String appType) {
        // 1. 先查 Redis
        String cacheKey = String.format(CACHE_LIST_PREFIX, appType, userId);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Redis 命中对话列表缓存: {}", cacheKey);
                String json = JSON.toJSONString(cached);
                return JSON.parseArray(json, ChatHistoryListDTO.class);
            }
        } catch (Exception e) {
            log.warn("Redis 读取对话列表失败，降级查询 MySQL: {}", e.getMessage());
        }

        // 2. Redis 未命中，查询 MySQL
        BaseMapper<? extends ChatHistory> mapper = getMapper(appType);
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("isDelete", 0)
                .orderByDesc("updateTime");

        @SuppressWarnings("unchecked")
        List<ChatHistory> records = ((BaseMapper<ChatHistory>) mapper).selectList(queryWrapper);

        // 3. 转换为 DTO
        List<ChatHistoryListDTO> result = new ArrayList<>();
        for (ChatHistory record : records) {
            ChatHistoryListDTO dto = new ChatHistoryListDTO();
            dto.setChatId(record.getChatId());
            dto.setLastMessage(record.getLastMessage());
            dto.setCreateTime(record.getCreateTime());
            dto.setUpdateTime(record.getUpdateTime());
            result.add(dto);
        }

        // 4. 回填 Redis
        try {
            redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(CACHE_EXPIRE_MINUTES));
        } catch (Exception e) {
            log.warn("Redis 回填对话列表缓存失败: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public ChatHistoryDetailDTO getChatHistoryDetail(Long userId, String chatId, String appType) {
        // 1. 先查 Redis
        String cacheKey = String.format(CACHE_DETAIL_PREFIX, appType, chatId);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Redis 命中对话详情缓存: {}", cacheKey);
                String json = JSON.toJSONString(cached);
                return JSON.parseObject(json, ChatHistoryDetailDTO.class);
            }
        } catch (Exception e) {
            log.warn("Redis 读取对话详情失败，降级查询 MySQL: {}", e.getMessage());
        }

        // 2. Redis 未命中，查询 MySQL
        BaseMapper<? extends ChatHistory> mapper = getMapper(appType);
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chatId", chatId)
                .eq("userId", userId)
                .eq("isDelete", 0);

        @SuppressWarnings("unchecked")
        ChatHistory record = ((BaseMapper<ChatHistory>) mapper).selectOne(queryWrapper);
        if (record == null) {
            return null;
        }

        // 3. 转换为 DTO
        ChatHistoryDetailDTO dto = new ChatHistoryDetailDTO();
        dto.setChatId(record.getChatId());
        dto.setCreateTime(record.getCreateTime());
        dto.setUpdateTime(record.getUpdateTime());

        // 解析 JSON 消息列表
        List<ChatHistoryDetailDTO.ChatMessage> messages = JSON.parseArray(record.getMessages(), ChatHistoryDetailDTO.ChatMessage.class);
        dto.setMessages(messages != null ? messages : new ArrayList<>());

        // 4. 回填 Redis
        try {
            redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofMinutes(CACHE_EXPIRE_MINUTES));
        } catch (Exception e) {
            log.warn("Redis 回填对话详情缓存失败: {}", e.getMessage());
        }

        return dto;
    }

    @Override
    public void saveChatMessages(Long userId, String chatId, String appType, List<ChatHistoryDetailDTO.ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        String messagesJson = JSON.toJSONString(messages);
        // 取第一条用户消息作为对话标题摘要（截断到50字符）
        String lastMessage = messages.stream()
                .filter(m -> "user".equals(m.getRole()))
                .map(ChatHistoryDetailDTO.ChatMessage::getContent)
                .findFirst()
                .orElse("新对话");
        if (lastMessage.length() > 50) {
            lastMessage = lastMessage.substring(0, 50) + "...";
        }

        BaseMapper<? extends ChatHistory> mapper = getMapper(appType);

        // 查询是否已存在该 chatId 的记录
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chatId", chatId)
                .eq("userId", userId)
                .eq("isDelete", 0);

        @SuppressWarnings("unchecked")
        ChatHistory existing = ((BaseMapper<ChatHistory>) mapper).selectOne(queryWrapper);

        if (existing != null) {
            // 更新已有记录（仅更新消息内容，不覆盖标题）
            UpdateWrapper<ChatHistory> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("chatId", chatId)
                    .eq("userId", userId)
                    .eq("isDelete", 0)
                    .set("messages", messagesJson)
                    .set("editTime", new Date());

            @SuppressWarnings("unchecked")
            BaseMapper<ChatHistory> castedMapper = (BaseMapper<ChatHistory>) mapper;
            castedMapper.update(null, updateWrapper);
        } else {
            // 新增记录
            ChatHistory chatHistory = createEntityByAppType(appType);
            chatHistory.setChatId(chatId);
            chatHistory.setUserId(userId);
            chatHistory.setMessages(messagesJson);
            chatHistory.setLastMessage(lastMessage);
            chatHistory.setEditTime(new Date());

            @SuppressWarnings("unchecked")
            BaseMapper<ChatHistory> castedMapper = (BaseMapper<ChatHistory>) mapper;
            castedMapper.insert(chatHistory);
        }

        // 更新 Redis 缓存
        try {
            // 更新对话详情缓存
            String detailCacheKey = String.format(CACHE_DETAIL_PREFIX, appType, chatId);
            ChatHistoryDetailDTO detailDTO = new ChatHistoryDetailDTO();
            detailDTO.setChatId(chatId);
            detailDTO.setMessages(messages);
            detailDTO.setUpdateTime(new Date());
            redisTemplate.opsForValue().set(detailCacheKey, detailDTO, Duration.ofMinutes(CACHE_EXPIRE_MINUTES));

            // 删除列表缓存（使其下次查询时刷新）
            String listCacheKey = String.format(CACHE_LIST_PREFIX, appType, userId);
            redisTemplate.delete(listCacheKey);
        } catch (Exception e) {
            log.warn("Redis 更新对话缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean deleteChatHistory(Long userId, String chatId, String appType) {
        BaseMapper<? extends ChatHistory> mapper = getMapper(appType);

        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chatId", chatId)
                .eq("userId", userId)
                .eq("isDelete", 0);

        @SuppressWarnings("unchecked")
        int deleted = ((BaseMapper<ChatHistory>) mapper).delete(queryWrapper);

        if (deleted > 0) {
            // 清除 Redis 缓存
            try {
                String detailCacheKey = String.format(CACHE_DETAIL_PREFIX, appType, chatId);
                String listCacheKey = String.format(CACHE_LIST_PREFIX, appType, userId);
                redisTemplate.delete(detailCacheKey);
                redisTemplate.delete(listCacheKey);
            } catch (Exception e) {
                log.warn("Redis 删除缓存失败: {}", e.getMessage());
            }
            return true;
        }
        return false;
    }

    /**
     * 根据 appType 获取对应的 Mapper
     */
    private BaseMapper<? extends ChatHistory> getMapper(String appType) {
        return switch (appType) {
            case "love" -> loveChatHistoryMapper;
            case "pet" -> petChatHistoryMapper;
            case "manus" -> manusChatHistoryMapper;
            default -> throw new IllegalArgumentException("不支持的应用类型: " + appType);
        };
    }

    /**
     * 根据 appType 创建对应的实体对象
     */
    private ChatHistory createEntityByAppType(String appType) {
        return switch (appType) {
            case "love" -> new com.wenxi.neko_ai_agent.model.entity.LoveChatHistory();
            case "pet" -> new com.wenxi.neko_ai_agent.model.entity.PetChatHistory();
            case "manus" -> new com.wenxi.neko_ai_agent.model.entity.ManusChatHistory();
            default -> throw new IllegalArgumentException("不支持的应用类型: " + appType);
        };
    }
}

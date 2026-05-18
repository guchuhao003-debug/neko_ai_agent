package com.wenxi.neko_ai_agent.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wenxi.neko_ai_agent.mapper.ChatHistoryMapper;
import com.wenxi.neko_ai_agent.model.dto.chatmemory.ChatHistoryDetailDTO;
import com.wenxi.neko_ai_agent.model.dto.chatmemory.ChatHistoryListDTO;
import com.wenxi.neko_ai_agent.model.entity.ChatHistory;
import com.wenxi.neko_ai_agent.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ChatHistoryMapper chatHistoryMapper;

    /**
     * Redis 缓存最大 TTL（兜底值，正常通过 save/delete 主动失效缓存）。
     */
    @Value("${neko.cache.chat-history.ttl-minutes:120}")
    private long cacheTtlMinutes;

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
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Redis 命中对话列表缓存: {}", cacheKey);
                return JSON.parseArray(cached, ChatHistoryListDTO.class);
            }
        } catch (Exception e) {
            log.warn("Redis 读取对话列表失败，降级查询 MySQL: {}", e.getMessage());
        }

        // 2. Redis 未命中，查询 MySQL
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                    .eq("appType", appType)
                    .eq("isDelete", 0)
                    .orderByDesc("updateTime");

        List<ChatHistory> records = chatHistoryMapper.selectList(queryWrapper);

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
            stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(result), Duration.ofMinutes(cacheTtlMinutes));
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
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Redis 命中对话详情缓存: {}", cacheKey);
                return JSON.parseObject(cached, ChatHistoryDetailDTO.class);
            }
        } catch (Exception e) {
            log.warn("Redis 读取对话详情失败，降级查询 MySQL: {}", e.getMessage());
        }

        // 2. Redis 未命中，查询 MySQL
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chatId", chatId)
                .eq("userId", userId)
                .eq("appType", appType)
                .eq("isDelete", 0);

        ChatHistory record = chatHistoryMapper.selectOne(queryWrapper);
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
            stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(dto), Duration.ofMinutes(cacheTtlMinutes));
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

        // 查询是否已存在该 chatId 的记录
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chatId", chatId)
                .eq("userId", userId)
                .eq("appType",appType)
                .eq("isDelete", 0);

        ChatHistory existing = chatHistoryMapper.selectOne(queryWrapper);

        if (existing != null) {
            // 更新已有记录（仅更新消息内容，不覆盖标题）
            UpdateWrapper<ChatHistory> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("chatId", chatId)
                    .eq("userId", userId)
                    .eq("appType",appType)
                    .eq("isDelete", 0)
                    .set("messages", messagesJson)
                    .set("editTime", new Date());
            chatHistoryMapper.update(null, updateWrapper);
        } else {
            // 新增记录
            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setChatId(chatId);
            chatHistory.setAppType(appType);
            chatHistory.setUserId(userId);
            chatHistory.setMessages(messagesJson);
            chatHistory.setLastMessage(lastMessage);
            chatHistory.setEditTime(new Date());
            chatHistoryMapper.insert(chatHistory);
        }

        // 更新 Redis 缓存
        try {
            // 更新对话详情缓存
            String detailCacheKey = String.format(CACHE_DETAIL_PREFIX, appType, chatId);
            ChatHistoryDetailDTO detailDTO = new ChatHistoryDetailDTO();
            detailDTO.setChatId(chatId);
            detailDTO.setMessages(messages);
            detailDTO.setUpdateTime(new Date());
            stringRedisTemplate.opsForValue().set(detailCacheKey, JSON.toJSONString(detailDTO), Duration.ofMinutes(cacheTtlMinutes));

            // 删除列表缓存（使其下次查询时刷新）
            String listCacheKey = String.format(CACHE_LIST_PREFIX, appType, userId);
            stringRedisTemplate.delete(listCacheKey);
        } catch (Exception e) {
            log.warn("Redis 更新对话缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean deleteChatHistory(Long userId, String chatId, String appType) {
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chatId", chatId)
                .eq("userId", userId)
                .eq("appType", appType)
                .eq("isDelete", 0);

        int deleted = chatHistoryMapper.delete(queryWrapper);

        if (deleted > 0) {
            String detailCacheKey = String.format(CACHE_DETAIL_PREFIX, appType, chatId);
            String listCacheKey = String.format(CACHE_LIST_PREFIX, appType, userId);
            stringRedisTemplate.delete(detailCacheKey);
            stringRedisTemplate.delete(listCacheKey);
            return true;
        }
        return false;
    }


}

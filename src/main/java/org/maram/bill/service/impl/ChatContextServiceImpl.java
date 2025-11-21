package org.maram.bill.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.service.ChatContextService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于 Redis 的轻量级聊天上下文管理
 * 
 * 存储结构：
 * Key: chat:context:{conversationId}
 * Value: LinkedList<MessagePair> (最近N轮对话)
 * TTL: 1小时（避免内存泄漏）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatContextServiceImpl implements ChatContextService {

    private static final String CONTEXT_KEY_PREFIX = "chat:context:";
    private static final int CONTEXT_TTL_HOURS = 1;  // 上下文过期时间：1小时
    private static final int MAX_STORED_ROUNDS = 10; // Redis中最多存储10轮对话
    
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void addUserMessage(String conversationId, String message) {
        if (conversationId == null || message == null) {
            return;
        }
        
        String key = buildKey(conversationId);
        
        // 获取当前上下文
        @SuppressWarnings("unchecked")
        LinkedList<MessagePair> context = (LinkedList<MessagePair>) redisTemplate.opsForValue().get(key);
        if (context == null) {
            context = new LinkedList<>();
        }
        
        // 添加用户消息（创建新的对话轮）
        context.add(new MessagePair(message, null));
        
        // 限制存储轮数
        while (context.size() > MAX_STORED_ROUNDS) {
            context.removeFirst();
        }
        
        // 保存到 Redis
        redisTemplate.opsForValue().set(key, context, CONTEXT_TTL_HOURS, TimeUnit.HOURS);
        
        log.debug("添加用户消息到上下文 [{}], 当前轮数: {}", conversationId, context.size());
    }

    @Override
    public void addAssistantMessage(String conversationId, String message) {
        if (conversationId == null || message == null) {
            return;
        }
        
        String key = buildKey(conversationId);
        
        // 获取当前上下文
        @SuppressWarnings("unchecked")
        LinkedList<MessagePair> context = (LinkedList<MessagePair>) redisTemplate.opsForValue().get(key);
        if (context == null || context.isEmpty()) {
            log.warn("尝试添加AI响应，但没有对应的用户消息 [{}]", conversationId);
            return;
        }
        
        // 更新最后一轮的AI响应
        MessagePair lastRound = context.getLast();
        lastRound.setAssistantMessage(message);
        
        // 保存到 Redis
        redisTemplate.opsForValue().set(key, context, CONTEXT_TTL_HOURS, TimeUnit.HOURS);
        
        log.debug("添加AI响应到上下文 [{}], 当前轮数: {}", conversationId, context.size());
    }

    @Override
    public List<Message> getRecentMessages(String conversationId, int maxRounds) {
        if (conversationId == null || maxRounds <= 0) {
            return new ArrayList<>();
        }
        
        String key = buildKey(conversationId);
        
        // 获取上下文
        @SuppressWarnings("unchecked")
        LinkedList<MessagePair> context = (LinkedList<MessagePair>) redisTemplate.opsForValue().get(key);
        if (context == null || context.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 只取最近的 maxRounds 轮
        int startIndex = Math.max(0, context.size() - maxRounds);
        List<MessagePair> recentRounds = context.subList(startIndex, context.size());
        
        // 转换为 Spring AI 的 Message 对象
        List<Message> messages = new ArrayList<>();
        for (MessagePair pair : recentRounds) {
            if (pair.getUserMessage() != null) {
                messages.add(new UserMessage(pair.getUserMessage()));
            }
            if (pair.getAssistantMessage() != null) {
                messages.add(new AssistantMessage(pair.getAssistantMessage()));
            }
        }
        
        log.debug("获取上下文 [{}], 返回 {} 轮对话, 共 {} 条消息", 
                conversationId, recentRounds.size(), messages.size());
        
        return messages;
    }

    @Override
    public void clearContext(String conversationId) {
        if (conversationId == null) {
            return;
        }
        
        String key = buildKey(conversationId);
        redisTemplate.delete(key);
        
        log.info("清空对话上下文 [{}]", conversationId);
    }

    private String buildKey(String conversationId) {
        return CONTEXT_KEY_PREFIX + conversationId;
    }

    /**
     * 消息对：一轮对话包含用户消息和AI响应
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class MessagePair implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        private String userMessage;
        private String assistantMessage;
    }
}

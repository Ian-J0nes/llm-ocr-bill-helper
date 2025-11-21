package org.maram.bill.service;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

/**
 * 聊天上下文管理服务
 */
public interface ChatContextService {
    
    /**
     * 添加用户消息到上下文
     */
    void addUserMessage(String conversationId, String message);
    
    /**
     * 添加AI响应到上下文
     */
    void addAssistantMessage(String conversationId, String message);
    
    /**
     * 获取最近的N轮对话（包括用户消息和AI响应）
     * @param conversationId 对话ID（通常是openid）
     * @param maxRounds 最大轮数（默认5轮，即最近5组 user+assistant）
     * @return 消息列表
     */
    List<Message> getRecentMessages(String conversationId, int maxRounds);
    
    /**
     * 清空对话上下文
     */
    void clearContext(String conversationId);
}

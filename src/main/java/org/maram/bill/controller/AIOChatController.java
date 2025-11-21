package org.maram.bill.controller;

import lombok.RequiredArgsConstructor;
import org.maram.bill.common.security.UserContext;
import org.maram.bill.service.AIOrchestrationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/**
 * AI总控制器，负责接收聊天请求并委派给AI编排服务处理。
 *
 * @author Claude
 * @date 2025-07-19
 */
@RestController
@RequestMapping("/aio")
@RequiredArgsConstructor
public class AIOChatController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AIOChatController.class);
    
    private final AIOrchestrationService aiOrchestrationService;
    private final UserContext userContext;

    /**
     * 处理聊天请求，支持文本和文件上传。
     *
     * @param userTextMessage 用户的文本消息 (可选)
     * @param files           用户上传的文件数组 (可选)
     * @return AI模型的响应流
     */
    @PostMapping(value = "/messages", produces = "application/json;charset=UTF-8")
    public Flux<String> chat(
            @RequestParam(value = "message", required = false) String userTextMessage,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        String openid = userContext.currentOpenid().orElse(null);
        return aiOrchestrationService.chatWithAi(userTextMessage, files, openid);
    }
}

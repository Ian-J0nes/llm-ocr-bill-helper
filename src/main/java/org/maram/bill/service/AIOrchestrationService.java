package org.maram.bill.service;

import org.maram.bill.entity.Bill;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/**
 * 编排AI聊天交互的服务。
 * 该服务负责处理来自控制器的请求，准备数据，并与AI模型进行通信。
 *
 * @author Claude
 * @date 2025-07-19
 */
public interface AIOrchestrationService {

    /**
     * 处理用户聊天请求，可能包含文本和/或文件。
     *
     * @param userTextMessage 用户的文本消息 (可选)
     * @param files           用户上传的文件数组 (可选)
     * @param openid          用户的OpenID，用于身份验证和获取上下文信息
     * @return 一个包含AI模型响应的Flux流
     */
    Flux<String> chatWithAi(String userTextMessage, MultipartFile[] files, String openid);

    /**
     * 从已上传的发票文件中提取账单信息。
     *
     * @param fileId 发票文件的数据库ID
     * @return 提取出的账单对象，如果失败则返回null
     */
    Bill extractBillFromInvoice(String fileId);

}

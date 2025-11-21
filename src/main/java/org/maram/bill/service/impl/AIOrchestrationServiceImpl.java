package org.maram.bill.service.impl;

import org.maram.bill.entity.Bill;
import org.maram.bill.entity.InvoiceFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.common.service.PromptService;
import org.maram.bill.config.ai.ChatConfig;
import org.maram.bill.service.AIOrchestrationService;
import org.maram.bill.service.BillCategoryMatchingService;
import org.maram.bill.service.BillProcessingService;
import org.maram.bill.service.ChatContextService;
import org.maram.bill.service.InvoiceFileService;
import org.maram.bill.service.UserService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AI交互编排服务的实现类。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIOrchestrationServiceImpl implements AIOrchestrationService {

    private static final String ERROR_MSG_AI_BUSY = "咩～服务暂时有点忙，小咩连接不上大脑啦！请稍后再试吧！😥";
    private static final String ERROR_MSG_SYSTEM_ERROR = "哎呀，系统好像开小差了，小咩正在紧急处理！🛠️";
    private static final String ERROR_MSG_NO_INPUT = "哎呀，好像什么都没发送呢，小咩该做什么好呢？🤔 请说点什么或上传账单图片吧";

    private final ChatModel chatModel;
    private final InvoiceFileService invoiceFileService;
    private final BillProcessingService billProcessingService;
    private final ChatConfig chatConfig;
    private final ObjectMapper objectMapper;
    private final BillCategoryMatchingService categoryMatchingService;
    private final UserService userService;
    private final PromptService promptService;
    private final ChatContextService chatContextService;  // 使用我们自己的轻量级上下文管理

    private static final int MAX_CONTEXT_ROUNDS = 5;  // 最多保留最近5轮对话

    @Override
    public Flux<String> chatWithAi(String userTextMessage, MultipartFile[] files, String openid) {
        if (openid == null || openid.isEmpty()) {
            log.error("无法从请求中获取OpenID");
            return Flux.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法获取用户信息，请重新登录。"));
        }

        log.info("用户 [{}]: 开始处理聊天请求。文本: '{}', 文件数: {}",
                openid, userTextMessage == null ? "<无>" : userTextMessage, (files != null ? files.length : 0));

        try {
            ChatConfig.UserAiConfigInfo userAiConfig = chatConfig.getUserAiConfig(openid);
            log.info("用户 [{}]: 使用AI配置 - 模型: {}, 温度: {}, 配置详情: {}",
                    openid, userAiConfig.getModel(), userAiConfig.getTemperature(), userAiConfig.getModelConfig());

            ChatClient chatClient = buildChatClient(userAiConfig);
            String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            Long userId = userService.getUserIdByOpenid(openid);
            List<String> availableCategories = categoryMatchingService.getAvailableCategoryNames(userId);
            String systemPrompt = promptService.getSystemPrompt(availableCategories);

            log.debug("用户 [{}]: 系统提示词长度: {} 字符, 可用分类数: {}",
                    openid, systemPrompt != null ? systemPrompt.length() : 0, availableCategories.size());

            boolean hasText = userTextMessage != null && !userTextMessage.trim().isEmpty();
            FileProcessingResult fileResult = processUploadedFiles(files, openid);

            if (fileResult.hasFiles() && fileResult.firstFileId() != null) {
                String userPrompt = hasText
                    ? promptService.formatUserPromptForImageAndText(currentDate, availableCategories, userTextMessage, fileResult.firstFileId())
                    : promptService.formatUserPromptForImageOnly(currentDate, availableCategories, fileResult.firstFileId());

                log.info("用户 [{}]: 构建 ChatClient 请求 (带媒体)。媒体数: {}, FileID: '{}', Prompt长度: {}",
                        openid, fileResult.mediaResources().size(), fileResult.firstFileId(), userPrompt.length());
                log.debug("用户 [{}]: 用户提示词内容: {}", openid, userPrompt);

                return chatClient.prompt()
                        // .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, openid))  // 临时禁用
                        .system(systemPrompt)
                        .user(u -> {
                            u.text(userPrompt);
                            for (int i = 0; i < fileResult.mediaResources().size(); i++) {
                                u.media(fileResult.mimeTypes().get(i), fileResult.mediaResources().get(i).getURL());
                            }
                        })
                        .stream()
                        .content()
                        .doOnSubscribe(subscription -> log.info("用户 [{}]: LLM流已订阅", openid))
                        .doOnError(error -> log.error("用户 [{}]: LLM流处理错误", openid, error))
                        .doOnComplete(() -> log.info("用户 [{}]: LLM流处理完成", openid))
                        .onErrorResume(e -> {
                            log.error("用户 [{}]: LLM流错误", openid, e);
                            return Flux.just("{\"error\":\"" + ERROR_MSG_AI_BUSY + " (" + e.getClass().getSimpleName() + ")\"}");
                        });

            } else if (hasText) {
                String userPrompt = promptService.formatUserPromptWithCategories(currentDate, availableCategories, userTextMessage);

                // 获取历史对话上下文
                List<Message> historyMessages = chatContextService.getRecentMessages(openid, MAX_CONTEXT_ROUNDS);

                log.info("用户 [{}]: 构建 ChatClient 请求 (纯文本)。Prompt长度: {}, 历史消息数: {}",
                        openid, userPrompt.length(), historyMessages.size());
                log.debug("用户 [{}]: 用户提示词内容: {}", openid, userPrompt);

                // 先添加用户消息到上下文（响应还没收到）
                chatContextService.addUserMessage(openid, userTextMessage);

                // 用于收集完整的AI响应
                StringBuilder fullResponse = new StringBuilder();

                return chatClient.prompt()
                        .system(systemPrompt)
                        .messages(historyMessages)  // 添加历史对话
                        .user(userPrompt)
                        .stream()
                        .content()
                        .doOnSubscribe(subscription -> log.info("用户 [{}]: LLM流已订阅", openid))
                        .doOnNext(chunk -> fullResponse.append(chunk))  // 收集响应片段
                        .doOnComplete(() -> {
                            log.info("用户 [{}]: LLM流处理完成", openid);
                            // 保存完整的AI响应到上下文
                            if (fullResponse.length() > 0) {
                                chatContextService.addAssistantMessage(openid, fullResponse.toString());
                            }
                        })
                        .doOnError(error -> log.error("用户 [{}]: LLM流处理错误", openid, error))
                        .onErrorResume(e -> {
                            log.error("用户 [{}]: LLM流错误", openid, e);
                            return Flux.just(ERROR_MSG_AI_BUSY);
                        });

            } else {
                log.warn("用户 [{}]: 未提供任何有效输入", openid);
                return Flux.just(ERROR_MSG_NO_INPUT);
            }

        } catch (Exception e) {
            log.error("用户 [{}]: 处理聊天请求时发生错误", openid, e);
            return Flux.just(ERROR_MSG_SYSTEM_ERROR);
        }
    }

    @Override
    public Bill extractBillFromInvoice(String fileId) {
        InvoiceFile invoiceFile = invoiceFileService.getInvoiceFileEntity(fileId);
        if (invoiceFile == null) {
            log.error("无法找到文件ID为 {} 的发票文件记录", fileId);
            return null;
        }

        Long userId = invoiceFile.getUserId();
        String openid = userService.getOpenidByUserId(userId);
        if (openid == null) {
            log.error("无法找到用户ID {} 对应的OpenID", userId);
            return null;
        }
        
        log.info("开始从文件ID {} (用户ID: {}) 中提取账单信息", fileId, userId);

        try {
            MimeType mimeType = MimeTypeUtils.parseMimeType(invoiceFile.getFileType());
            UrlResource mediaResource = new UrlResource(invoiceFile.getFileUrl());

            ChatConfig.UserAiConfigInfo userAiConfig = chatConfig.getUserAiConfig(openid);
            List<String> availableCategories = categoryMatchingService.getAvailableCategoryNames(userId);
            String systemPrompt = promptService.getSystemPrompt(availableCategories);
            String userPrompt = promptService.formatUserPromptForImageOnly(
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                availableCategories,
                fileId
            );

            ChatClient chatClient = buildChatClientForExtraction(userAiConfig);

            log.debug("向AI发送请求以提取账单, FileID: {}", fileId);
            
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> u.text(userPrompt).media(mimeType, mediaResource.getURL()))
                    .call()
                    .entity(Bill.class);

        } catch (MalformedURLException e) {
            log.error("文件URL格式错误: {} (FileID: {})", invoiceFile.getFileUrl(), fileId, e);
        } catch (Exception e) {
            log.error("从发票文件 (FileID: {}) 提取信息时发生异常", fileId, e);
        }

        return null;
    }

    /**
     * 构建聊天客户端（不使用内置的 MessageChatMemoryAdvisor）
     * 我们使用自己的轻量级 ChatContextService 来管理上下文
     */
    private ChatClient buildChatClient(ChatConfig.UserAiConfigInfo userAiConfig) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(userAiConfig.getModel())
                        .temperature(userAiConfig.getTemperature())
                        .build())
                .build();
    }

    private ChatClient buildChatClientForExtraction(ChatConfig.UserAiConfigInfo userAiConfig) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(userAiConfig.getModel())
                        .temperature(userAiConfig.getTemperature())
                        .build())
                .build();
    }

    private FileProcessingResult processUploadedFiles(MultipartFile[] files, String openid) {
        List<UrlResource> mediaResources = new ArrayList<>();
        List<MimeType> mimeTypes = new ArrayList<>();
        String firstFileId = null;

        if (files == null || files.length == 0) {
            return new FileProcessingResult(mediaResources, mimeTypes, firstFileId);
        }

        log.debug("用户 [{}]: 检测到 {} 个文件，开始处理", openid, files.length);
        
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                log.warn("用户 [{}]: 跳过空文件", openid);
                continue;
            }
            
            log.info("用户 [{}]: 正在处理文件: {}", openid, file.getOriginalFilename());
            
            try {
                String fileId = invoiceFileService.uploadInvoiceFileByOpenid(
                        file.getBytes(), file.getOriginalFilename(), openid);

                if (fileId == null) {
                    log.error("用户 [{}]: 文件上传失败: {}", openid, file.getOriginalFilename());
                    continue;
                }

                if (firstFileId == null) {
                    firstFileId = fileId;
                }

                String fileInfoJson = invoiceFileService.getInvoiceFileInfo(fileId);
                if (fileInfoJson == null) {
                    log.error("用户 [{}]: 未能获取文件ID {} 的详细信息", openid, fileId);
                    continue;
                }

                JsonNode fileNode = objectMapper.readTree(fileInfoJson);
                String fileUrl = fileNode.path("fileUrl").asText();
                String fileType = fileNode.path("fileType").asText();

                if (fileUrl.isEmpty() || fileType.isEmpty()) {
                    log.error("用户 [{}]: 文件信息不完整, FileID: {}", openid, fileId);
                    continue;
                }

                MimeType parsedMimeType;
                try {
                    parsedMimeType = MimeTypeUtils.parseMimeType(fileType);
                } catch (Exception e) {
                    log.error("用户 [{}]: 无效的MIME类型 '{}', FileID: {}", openid, fileType, fileId, e);
                    continue;
                }

                UrlResource imageResource = new UrlResource(fileUrl);
                mediaResources.add(imageResource);
                mimeTypes.add(parsedMimeType);

                log.info("用户 [{}]: 成功添加图片, FileID: {}", openid, fileId);

                // 异步处理发票文件，提取并保存账单
                processInvoiceFileAsync(fileId);

            } catch (IOException e) {
                log.error("用户 [{}]: 处理文件 {} 时发生IO错误", openid, file.getOriginalFilename(), e);
            } catch (Exception e) {
                log.error("用户 [{}]: 处理文件 {} 时发生未知错误", openid, file.getOriginalFilename(), e);
            }
        }

        return new FileProcessingResult(mediaResources, mimeTypes, firstFileId);
    }

    /**
     * 异步处理发票文件：提取账单信息并保存
     *
     * @param fileId 发票文件ID
     */
    private void processInvoiceFileAsync(String fileId) {
        try {
            log.info("开始通过AI处理发票文件: {}", fileId);

            Bill extractedBill = extractBillFromInvoice(fileId);

            if (extractedBill == null) {
                log.error("AI未能从文件 {} 提取账单信息", fileId);
                return;
            }

            if (extractedBill.getUserId() == null) {
                log.error("从文件 {} 提取的账单缺少用户ID", fileId);
                return;
            }

            billProcessingService.processAndSaveBill(extractedBill, extractedBill.getUserId());
            log.info("成功处理并保存从文件 {} 提取的账单", fileId);

        } catch (Exception e) {
            log.error("处理文件 {} 提取的账单时发生错误", fileId, e);
        }
    }

    private record FileProcessingResult(
        List<UrlResource> mediaResources,
        List<MimeType> mimeTypes,
        String firstFileId
    ) {
        boolean hasFiles() {
            return !mediaResources.isEmpty();
        }
    }
}

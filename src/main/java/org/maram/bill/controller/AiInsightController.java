package org.maram.bill.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.common.security.UserContext;
import org.maram.bill.config.ai.ChatConfig;
import org.maram.bill.service.AiInsightService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

/**
 * AI智能洞察控制器
 * 提供基于用户财务数据的AI分析和建议
 */
@RestController
@RequestMapping("/ai-insight")
@Slf4j
@RequiredArgsConstructor
public class AiInsightController {

    private final ChatModel chatModel;
    private final ChatConfig chatConfig;
    private final AiInsightService aiInsightService;
    private final UserContext userContext;

    // AI洞察系统提示词
    private static final String INSIGHT_SYSTEM_PROMPT = """
            你是一位专业的财务顾问AI助手，名字叫'小咩'。你的任务是基于用户提供的财务数据，提供专业、实用、个性化的财务建议和洞察分析。

            请遵循以下原则：
            1. 语言风格要友好、专业，适当使用emoji让内容更生动
            2. 重点关注预算管理、支出优化、财务健康度
            3. 如果发现超预算风险，要及时提醒并给出具体建议
            4. 分析支出结构，指出可能的优化空间
            5. 根据历史数据给出未来的财务规划建议
            6. 保持积极正面的态度，即使财务状况不佳也要给出建设性建议
            7. 回答要结构清晰，分点说明，便于阅读

            请基于用户提供的财务数据，给出详细的分析和建议。
            """;

    /**
     * 获取月度财务洞察
     */
    @GetMapping(value = "/monthly", produces = "application/json;charset=UTF-8")
    public Flux<String> getMonthlyInsight(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate) {

        return generateInsight("monthly", targetDate);
    }

    /**
     * 获取季度财务洞察
     */
    @GetMapping(value = "/quarterly", produces = "application/json;charset=UTF-8")
    public Flux<String> getQuarterlyInsight(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate) {

        return generateInsight("quarterly", targetDate);
    }

    /**
     * 获取年度财务洞察
     */
    @GetMapping(value = "/yearly", produces = "application/json;charset=UTF-8")
    public Flux<String> getYearlyInsight(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate) {

        return generateInsight("yearly", targetDate);
    }

    /**
     * 通用洞察生成方法
     */
    private Flux<String> generateInsight(String period, LocalDate targetDate) {
        final UserIdentity identity;
        try {
            identity = requireUserIdentity();
        } catch (ResponseStatusException e) {
            return Flux.error(e);
        }

        try {
            log.info("用户 [{}] 请求{}洞察分析, 目标日期: {}", identity.openid(), period, targetDate);

            // 获取用户的AI配置
            ChatConfig.UserAiConfigInfo userAiConfig = chatConfig.getUserAiConfig(identity.openid());

            // 创建ChatClient
            ChatClient chatClient = buildChatClient(userAiConfig);

            // 生成财务数据摘要
            String financialSummary;
            switch (period.toLowerCase()) {
                case "monthly":
                    financialSummary = aiInsightService.generateMonthlyInsight(identity.userId(), targetDate);
                    break;
                case "quarterly":
                    financialSummary = aiInsightService.generateQuarterlyInsight(identity.userId(), targetDate);
                    break;
                case "yearly":
                    financialSummary = aiInsightService.generateYearlyInsight(identity.userId(), targetDate);
                    break;
                default:
                    return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的分析周期"));
            }

            log.debug("用户 [{}] 财务数据摘要生成完成，开始AI分析", identity.openid());

            // 调用AI进行分析
            return chatClient.prompt()
                    .system(INSIGHT_SYSTEM_PROMPT)
                    .user(financialSummary)
                    .stream()
                    .content()
                    .doOnSubscribe(subscription -> log.info("用户 [{}]: AI洞察分析开始", identity.openid()))
                    .doOnError(error -> log.error("用户 [{}]: AI洞察分析发生错误", identity.openid(), error))
                    .doOnComplete(() -> log.info("用户 [{}]: AI洞察分析完成", identity.openid()))
                    .onErrorResume(e -> {
                        log.error("用户 [{}]: AI洞察分析失败", identity.openid(), e);
                        return Flux.just("{\"error\":\"咩～小咩的大脑暂时转不动了，请稍后再试试吧！😅\"}");
                    });

        } catch (Exception e) {
            log.error("用户 [{}]: 生成AI洞察时发生未知错误", identity.openid(), e);
            return Flux.just("{\"error\":\"哎呀，系统出了点小问题，小咩正在努力修复中！🔧\"}");
        }
    }

    /**
     * 获取财务数据摘要（用于调试）
     */
    @GetMapping("/summary/{period}")
    public String getFinancialSummary(
            @PathVariable String period,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate) {

        final UserIdentity identity;
        try {
            identity = requireUserIdentity();
        } catch (ResponseStatusException e) {
            return "错误: " + e.getReason();
        }

        try {
            switch (period.toLowerCase()) {
                case "monthly":
                    return aiInsightService.generateMonthlyInsight(identity.userId(), targetDate);
                case "quarterly":
                    return aiInsightService.generateQuarterlyInsight(identity.userId(), targetDate);
                case "yearly":
                    return aiInsightService.generateYearlyInsight(identity.userId(), targetDate);
                default:
                    return "错误: 不支持的分析周期";
            }
        } catch (Exception e) {
            log.error("获取财务摘要失败", e);
            return "错误: " + e.getMessage();
        }
    }

    private UserIdentity requireUserIdentity() {
        String openid = userContext.currentOpenid().orElse(null);
        if (openid == null || openid.isEmpty()) {
            log.error("AI洞察请求失败: 无法获取OpenID");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法获取用户信息，请重新登录");
        }

        Long userId = userContext.currentUserId().orElse(null);
        if (userId == null) {
            log.error("AI洞察请求失败: 无法通过OpenID {} 找到用户ID", openid);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "用户信息不完整");
        }

        return new UserIdentity(openid, userId);
    }

    private ChatClient buildChatClient(ChatConfig.UserAiConfigInfo userAiConfig) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(userAiConfig.getModel())
                        .temperature(userAiConfig.getTemperature())
                        .build())
                .build();
    }

    private record UserIdentity(String openid, Long userId) {
    }
}

package org.maram.bill.service;

import java.time.LocalDate;

/**
 * AI智能洞察服务接口
 * 基于用户的预算和账单数据提供智能财务建议
 */
public interface AiInsightService {

    /**
     * 生成月度财务洞察
     * @param userId 用户ID
     * @param targetDate 目标月份（如果为null则使用当前月）
     * @return AI洞察结果（流式响应字符串）
     */
    String generateMonthlyInsight(Long userId, LocalDate targetDate);

    /**
     * 生成季度财务洞察
     * @param userId 用户ID
     * @param targetDate 目标季度的任意日期（如果为null则使用当前季度）
     * @return AI洞察结果（流式响应字符串）
     */
    String generateQuarterlyInsight(Long userId, LocalDate targetDate);

    /**
     * 生成年度财务洞察
     * @param userId 用户ID
     * @param targetDate 目标年份的任意日期（如果为null则使用当前年）
     * @return AI洞察结果（流式响应字符串）
     */
    String generateYearlyInsight(Long userId, LocalDate targetDate);

    /**
     * 构建用户财务数据摘要
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param period 时间周期类型（MONTHLY/QUARTERLY/YEARLY）
     * @return 财务数据摘要字符串
     */
    String buildFinancialSummary(Long userId, LocalDate startDate, LocalDate endDate, String period);
}

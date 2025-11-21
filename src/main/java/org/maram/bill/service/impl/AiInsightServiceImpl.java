package org.maram.bill.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.entity.Bill;
import org.maram.bill.entity.BillCategory;
import org.maram.bill.entity.UserBudget;
import org.maram.bill.service.AiInsightService;
import org.maram.bill.service.BillCategoryService;
import org.maram.bill.service.BillService;
import org.maram.bill.service.UserBudgetService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI智能洞察服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiInsightServiceImpl implements AiInsightService {

    private static final int TOP_CATEGORIES_LIMIT = 5;
    private static final int PERCENTAGE_SCALE = 1;
    private static final int CALCULATION_SCALE = 4;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final BillService billService;
    private final UserBudgetService userBudgetService;
    private final BillCategoryService billCategoryService;

    @Override
    public String generateMonthlyInsight(Long userId, LocalDate targetDate) {
        DateRange range = calculateMonthlyDateRange(targetDate);
        return buildFinancialSummary(userId, range.start(), range.end(), "MONTHLY");
    }

    @Override
    public String generateQuarterlyInsight(Long userId, LocalDate targetDate) {
        DateRange range = calculateQuarterlyDateRange(targetDate);
        return buildFinancialSummary(userId, range.start(), range.end(), "QUARTERLY");
    }

    @Override
    public String generateYearlyInsight(Long userId, LocalDate targetDate) {
        DateRange range = calculateYearlyDateRange(targetDate);
        return buildFinancialSummary(userId, range.start(), range.end(), "YEARLY");
    }

    @Override
    public String buildFinancialSummary(Long userId, LocalDate startDate, LocalDate endDate, String period) {
        log.debug("构建用户{}的财务摘要: {} 到 {}, 周期: {}", userId, startDate, endDate, period);

        StringBuilder summary = new StringBuilder();
        
        summary.append("📊 财务数据分析报告\n");
        summary.append("分析周期: ").append(startDate.format(DATE_FORMATTER))
               .append(" 至 ").append(endDate.format(DATE_FORMATTER)).append("\n\n");

        List<UserBudget> budgets = userBudgetService.getActiveBudgets(userId, startDate);
        UserBudget currentBudget = budgets.stream()
                .filter(budget -> period.equals(budget.getBudgetType()))
                .findFirst()
                .orElse(null);

        List<Bill> bills = getBillsInPeriod(userId, startDate, endDate);
        BigDecimal totalIncome = calculateTotalIncome(bills);
        BigDecimal totalExpense = calculateTotalExpense(bills);

        appendBudgetInfo(summary, currentBudget, totalExpense, period);
        appendIncomeExpenseSummary(summary, totalIncome, totalExpense);
        appendCategoryAnalysis(summary, bills, totalExpense);
        appendBillStatistics(summary, bills);
        appendAverageExpense(summary, bills, totalExpense);
        appendTrendPrediction(summary, totalExpense, startDate, endDate, currentBudget);

        summary.append("🤖 请小咩根据以上数据为我提供专业的财务建议和洞察分析。");

        return summary.toString();
    }

    private List<Bill> getBillsInPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        return billService.listByUserIdAndDateRange(userId, startDate, endDate);
    }

    private String getCategoryName(Long categoryId) {
        try {
            BillCategory category = billCategoryService.getById(categoryId);
            return category != null ? category.getCategoryName() : "未分类";
        } catch (Exception e) {
            log.warn("获取分类名称失败: {}", categoryId);
            return "未分类";
        }
    }

    private BigDecimal calculateTotalIncome(List<Bill> bills) {
        return bills.stream()
                .filter(bill -> "income".equalsIgnoreCase(bill.getTransactionType()))
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalExpense(List<Bill> bills) {
        return bills.stream()
                .filter(bill -> "expense".equalsIgnoreCase(bill.getTransactionType()))
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void appendBudgetInfo(StringBuilder summary, UserBudget budget, BigDecimal totalExpense, String period) {
        if (budget != null) {
            summary.append("💰 预算情况:\n");
            summary.append("预算金额: ¥").append(budget.getBudgetAmount()).append("\n");
            summary.append("已支出: ¥").append(totalExpense).append("\n");
            
            BigDecimal remaining = budget.getBudgetAmount().subtract(totalExpense);
            summary.append("剩余预算: ¥").append(remaining).append("\n");
            
            BigDecimal usagePercentage = totalExpense
                    .divide(budget.getBudgetAmount(), CALCULATION_SCALE, RoundingMode.HALF_UP)
                    .multiply(HUNDRED);
            summary.append("预算使用率: ").append(usagePercentage.setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP))
                   .append("%\n\n");
        } else {
            summary.append("💰 预算情况: 未设置").append(period.toLowerCase()).append("预算\n\n");
        }
    }

    private void appendIncomeExpenseSummary(StringBuilder summary, BigDecimal totalIncome, BigDecimal totalExpense) {
        summary.append("📈 收支概况:\n");
        summary.append("总收入: ¥").append(totalIncome).append("\n");
        summary.append("总支出: ¥").append(totalExpense).append("\n");
        BigDecimal balance = totalIncome.subtract(totalExpense);
        summary.append("净收支: ¥").append(balance).append("\n\n");
    }

    private void appendCategoryAnalysis(StringBuilder summary, List<Bill> bills, BigDecimal totalExpense) {
        Map<String, BigDecimal> categoryExpenses = bills.stream()
                .filter(bill -> "expense".equalsIgnoreCase(bill.getTransactionType()))
                .filter(bill -> bill.getCategoryId() != null)
                .collect(Collectors.groupingBy(
                    bill -> getCategoryName(bill.getCategoryId()),
                    Collectors.reducing(BigDecimal.ZERO, Bill::getTotalAmount, BigDecimal::add)
                ));

        if (!categoryExpenses.isEmpty()) {
            summary.append("🏷️ 支出分类分析:\n");
            categoryExpenses.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .limit(TOP_CATEGORIES_LIMIT)
                    .forEach(entry -> {
                        BigDecimal percentage = totalExpense.compareTo(BigDecimal.ZERO) > 0
                            ? entry.getValue().divide(totalExpense, CALCULATION_SCALE, RoundingMode.HALF_UP).multiply(HUNDRED)
                            : BigDecimal.ZERO;
                        summary.append("- ").append(entry.getKey()).append(": ¥")
                               .append(entry.getValue()).append(" (")
                               .append(percentage.setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP)).append("%)\n");
                    });
            summary.append("\n");
        }
    }

    private void appendBillStatistics(StringBuilder summary, List<Bill> bills) {
        long incomeCount = bills.stream()
                .filter(bill -> "income".equalsIgnoreCase(bill.getTransactionType()))
                .count();
        long expenseCount = bills.stream()
                .filter(bill -> "expense".equalsIgnoreCase(bill.getTransactionType()))
                .count();

        summary.append("📋 账单统计:\n");
        summary.append("总账单数: ").append(bills.size()).append("笔\n");
        summary.append("收入账单: ").append(incomeCount).append("笔\n");
        summary.append("支出账单: ").append(expenseCount).append("笔\n\n");
    }

    private void appendAverageExpense(StringBuilder summary, List<Bill> bills, BigDecimal totalExpense) {
        if (totalExpense.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        long expenseBillCount = bills.stream()
                .filter(bill -> "expense".equalsIgnoreCase(bill.getTransactionType()))
                .count();

        if (expenseBillCount > 0) {
            BigDecimal avgExpense = totalExpense.divide(new BigDecimal(expenseBillCount), 2, RoundingMode.HALF_UP);
            summary.append("💳 平均单笔支出: ¥").append(avgExpense).append("\n\n");
        }
    }

    private void appendTrendPrediction(StringBuilder summary, BigDecimal totalExpense, 
                                      LocalDate startDate, LocalDate endDate, UserBudget budget) {
        LocalDate now = LocalDate.now();
        if (!endDate.isAfter(now)) {
            return;
        }

        long passedDays = startDate.until(now).getDays() + 1;
        long remainingDays = now.until(endDate).getDays();

        if (remainingDays <= 0 || passedDays <= 0) {
            return;
        }

        BigDecimal dailyAvgExpense = totalExpense.divide(new BigDecimal(passedDays), 2, RoundingMode.HALF_UP);
        BigDecimal projectedExpense = dailyAvgExpense.multiply(new BigDecimal(remainingDays));

        summary.append("🔮 趋势预测:\n");
        summary.append("已过天数: ").append(passedDays).append("天\n");
        summary.append("剩余天数: ").append(remainingDays).append("天\n");
        summary.append("日均支出: ¥").append(dailyAvgExpense).append("\n");
        summary.append("预计剩余支出: ¥").append(projectedExpense).append("\n");

        if (budget != null) {
            BigDecimal projectedTotal = totalExpense.add(projectedExpense);
            summary.append("预计总支出: ¥").append(projectedTotal).append("\n");
            if (projectedTotal.compareTo(budget.getBudgetAmount()) > 0) {
                BigDecimal overBudget = projectedTotal.subtract(budget.getBudgetAmount());
                summary.append("⚠️ 预计超预算: ¥").append(overBudget).append("\n");
            }
        }
        summary.append("\n");
    }

    private DateRange calculateMonthlyDateRange(LocalDate targetDate) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        LocalDate start = date.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = date.with(TemporalAdjusters.lastDayOfMonth());
        return new DateRange(start, end);
    }

    private DateRange calculateQuarterlyDateRange(LocalDate targetDate) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        int startMonth = (quarter - 1) * 3 + 1;
        LocalDate start = LocalDate.of(date.getYear(), startMonth, 1);
        LocalDate end = start.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
        return new DateRange(start, end);
    }

    private DateRange calculateYearlyDateRange(LocalDate targetDate) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        LocalDate start = LocalDate.of(date.getYear(), 1, 1);
        LocalDate end = LocalDate.of(date.getYear(), 12, 31);
        return new DateRange(start, end);
    }

    private record DateRange(LocalDate start, LocalDate end) {}
}

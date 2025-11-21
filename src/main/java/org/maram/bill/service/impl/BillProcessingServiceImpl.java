package org.maram.bill.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.common.enums.TransactionType;
import org.maram.bill.entity.Bill;
import org.maram.bill.service.BillCategoryMatchingService;
import org.maram.bill.service.BillProcessingService;
import org.maram.bill.service.BillService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 账单处理服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BillProcessingServiceImpl implements BillProcessingService {

    private static final String DEFAULT_CATEGORY_NAME = "其他";
    private static final String ERROR_USER_ID_REQUIRED = "用户ID不能为空";
    private static final String ERROR_AMOUNT_REQUIRED = "总金额必须大于0";
    private static final String ERROR_INVALID_TRANSACTION_TYPE = "交易类型只能是 '%s' 或 '%s'";

    private final BillService billService;
    private final BillCategoryMatchingService categoryMatchingService;

    @Override
    public Long processAndSaveBill(Bill bill, Long userId) {
        log.debug("开始智能处理账单: userId={}, bill={}", userId, bill);

        bill.setUserId(userId);

        String validationError = validateBillData(bill);
        if (validationError != null) {
            log.warn("账单数据验证失败: {}", validationError);
            throw new IllegalArgumentException(validationError);
        }

        processBillCategory(bill, userId);

        Long billId = billService.saveAndReturnId(bill);
        log.info("账单处理完成: billId={}, categoryId={}", billId, bill.getCategoryId());
        return billId;
    }

    private void processBillCategory(Bill bill, Long userId) {
        if (bill.getCategoryId() == null) {
            matchAndSetCategory(bill, userId);
        } else if (!billService.isValidCategory(bill.getCategoryId(), userId)) {
            log.warn("用户提供的分类ID无效: {}", bill.getCategoryId());
            bill.setCategoryId(null);
            matchAndSetCategory(bill, userId);
        }
    }

    private void matchAndSetCategory(Bill bill, Long userId) {
        Long matchedCategoryId = matchBillCategory(bill, userId);
        if (matchedCategoryId != null) {
            bill.setCategoryId(matchedCategoryId);
            log.debug("智能匹配到分类: {}", matchedCategoryId);
        }
    }

    @Override
    public Long matchBillCategory(Bill bill, Long userId) {
        String billType = inferBillType(bill);
        return categoryMatchingService.matchCategory(billType, bill.getTransactionType(), userId);
    }

    private String inferBillType(Bill bill) {
        if (StringUtils.hasText(bill.getBillType())) {
            return bill.getBillType();
        }
        if (StringUtils.hasText(bill.getName())) {
            return bill.getName();
        }
        if (StringUtils.hasText(bill.getSupplierName())) {
            return bill.getSupplierName();
        }
        return DEFAULT_CATEGORY_NAME;
    }

    @Override
    public String validateBillData(Bill bill) {
        if (bill.getUserId() == null || bill.getUserId() <= 0) {
            return ERROR_USER_ID_REQUIRED;
        }

        if (bill.getTotalAmount() == null || bill.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ERROR_AMOUNT_REQUIRED;
        }

        if (StringUtils.hasText(bill.getTransactionType()) && !TransactionType.isValid(bill.getTransactionType())) {
            return String.format(ERROR_INVALID_TRANSACTION_TYPE, 
                    TransactionType.INCOME.getValue(), TransactionType.EXPENSE.getValue());
        }

        validateAmountConsistency(bill);

        return null;
    }

    private void validateAmountConsistency(Bill bill) {
        if (bill.getTaxAmount() != null && bill.getNetAmount() != null) {
            BigDecimal calculatedTotal = bill.getNetAmount().add(bill.getTaxAmount());
            if (calculatedTotal.compareTo(bill.getTotalAmount()) != 0) {
                log.warn("金额计算不一致: 净额({}) + 税额({}) != 总额({})", 
                        bill.getNetAmount(), bill.getTaxAmount(), bill.getTotalAmount());
            }
        }
    }

    @Override
    public boolean initializeDefaultCategoriesForUser(Long userId) {
        log.info("为用户{}初始化默认分类", userId);
        try {
            int createdCount = categoryMatchingService.createDefaultCategoriesForUser(userId);
            log.info("为用户{}创建了{}个默认分类", userId, createdCount);
            return createdCount > 0;
        } catch (Exception e) {
            log.error("为用户{}初始化默认分类失败", userId, e);
            return false;
        }
    }
}

package org.maram.bill.service;

import org.maram.bill.entity.Bill;

/**
 * 账单处理服务接口
 * 负责账单保存时的智能处理逻辑
 */
public interface BillProcessingService {

    /**
     * 智能处理并保存账单
     * 包括分类匹配、数据验证等
     * @param bill 账单信息
     * @param userId 用户ID
     * @return 保存后的账单ID
     */
    Long processAndSaveBill(Bill bill, Long userId);

    /**
     * 智能匹配账单分类
     * @param bill 账单信息
     * @param userId 用户ID
     * @return 匹配的分类ID
     */
    Long matchBillCategory(Bill bill, Long userId);

    /**
     * 验证账单数据完整性
     * @param bill 账单信息
     * @return 验证结果消息，null表示验证通过
     */
    String validateBillData(Bill bill);

    /**
     * 为新用户初始化默认分类
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean initializeDefaultCategoriesForUser(Long userId);
}

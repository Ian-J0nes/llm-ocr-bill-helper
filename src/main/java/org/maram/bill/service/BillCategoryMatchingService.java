package org.maram.bill.service;

import org.maram.bill.entity.BillCategory;

import java.util.List;

/**
 * 账单分类智能匹配服务接口
 * 负责LLM识别后的分类匹配逻辑
 */
public interface BillCategoryMatchingService {

    /**
     * 根据LLM识别的账单类型智能匹配用户分类
     * @param billType LLM识别的账单类型
     * @param transactionType 交易类型（收入/支出/转账）
     * @param userId 用户ID
     * @return 匹配的分类ID，如果没有匹配则返回null
     */
    Long matchCategory(String billType, String transactionType, Long userId);

    /**
     * 根据关键词模糊匹配分类
     * @param keywords 关键词列表
     * @param userId 用户ID
     * @return 匹配的分类列表，按匹配度排序
     */
    List<BillCategory> fuzzyMatchCategories(List<String> keywords, Long userId);

    /**
     * 获取用户可用的分类列表（用于LLM提示）
     * @param userId 用户ID
     * @return 分类名称列表
     */
    List<String> getAvailableCategoryNames(Long userId);

    /**
     * 根据分类名称精确匹配分类ID
     * @param categoryName 分类名称
     * @param userId 用户ID
     * @return 分类ID，如果没有匹配则返回null
     */
    Long findCategoryIdByName(String categoryName, Long userId);

    /**
     * 为用户创建默认分类（如果不存在）
     * @param userId 用户ID
     * @return 创建的分类数量
     */
    int createDefaultCategoriesForUser(Long userId);

    /**
     * 获取或创建默认分类
     * @param categoryName 分类名称
     * @param userId 用户ID
     * @return 分类ID
     */
    Long getOrCreateDefaultCategory(String categoryName, Long userId);
}

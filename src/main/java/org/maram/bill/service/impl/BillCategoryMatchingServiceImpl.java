package org.maram.bill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.entity.BillCategory;
import org.maram.bill.mapper.BillCategoryMapper;
import org.maram.bill.service.BillCategoryMatchingService;
import org.maram.bill.service.BillCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 账单分类智能匹配服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BillCategoryMatchingServiceImpl implements BillCategoryMatchingService {

    private static final int CATEGORY_STATUS_ENABLED = 1;
    private static final int SCORE_EXACT_MATCH = 100;
    private static final int SCORE_CONTAINS_MATCH = 50;
    private static final int SCORE_DESCRIPTION_MATCH = 20;
    private static final int SCORE_KEYWORD_MATCH = 30;

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.ofEntries(
        Map.entry("工资收入", List.of("工资", "薪资", "薪水", "月薪", "年薪", "salary")),
        Map.entry("奖金", List.of("奖金", "年终奖", "绩效奖", "提成", "bonus")),
        Map.entry("投资收益", List.of("投资", "股票", "基金", "理财", "分红", "利息", "收益")),
        Map.entry("餐饮", List.of("餐饮", "美食", "吃饭", "外卖", "餐厅", "咖啡", "奶茶", "食物", "饭店")),
        Map.entry("交通", List.of("交通", "出行", "打车", "地铁", "公交", "火车", "飞机", "汽车", "加油", "停车")),
        Map.entry("购物", List.of("购物", "商场", "超市", "淘宝", "京东", "服装", "化妆品", "电子产品")),
        Map.entry("娱乐", List.of("娱乐", "电影", "游戏", "KTV", "旅游", "健身", "运动")),
        Map.entry("医疗", List.of("医疗", "医院", "药店", "看病", "体检", "药品", "保健")),
        Map.entry("教育", List.of("教育", "培训", "学费", "书籍", "课程", "学习")),
        Map.entry("住房", List.of("住房", "房租", "物业", "装修", "家具", "水电费", "燃气费"))
    );

    private final BillCategoryMapper billCategoryMapper;
    private final BillCategoryService billCategoryService;

    @Override
    public Long matchCategory(String billType, String transactionType, Long userId) {
        log.debug("开始匹配分类: billType={}, transactionType={}, userId={}", billType, transactionType, userId);
        
        if (!StringUtils.hasText(billType)) {
            return null;
        }

        // 1. 首先尝试精确匹配分类名称
        Long exactMatch = findCategoryIdByName(billType, userId);
        if (exactMatch != null) {
            log.debug("精确匹配到分类: {}", exactMatch);
            return exactMatch;
        }

        // 2. 尝试关键词模糊匹配
        List<String> keywords = Arrays.asList(billType.split("[\\s,，、]"));
        List<BillCategory> fuzzyMatches = fuzzyMatchCategories(keywords, userId);
        if (!fuzzyMatches.isEmpty()) {
            log.debug("模糊匹配到分类: {}", fuzzyMatches.get(0).getId());
            return fuzzyMatches.get(0).getId();
        }

        // 3. 如果没有匹配到，尝试创建默认分类
        Long defaultCategoryId = getOrCreateDefaultCategory(billType, userId);
        log.debug("创建或获取默认分类: {}", defaultCategoryId);
        return defaultCategoryId;
    }

    @Override
    public List<BillCategory> fuzzyMatchCategories(List<String> keywords, Long userId) {
        List<BillCategory> userCategories = billCategoryService.listByUserId(userId);
        Map<BillCategory, Integer> scoreMap = new HashMap<>();

        for (BillCategory category : userCategories) {
            int score = calculateMatchScore(category, keywords);
            if (score > 0) {
                scoreMap.put(category, score);
            }
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<BillCategory, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAvailableCategoryNames(Long userId) {
        List<BillCategory> categories = billCategoryService.listByUserId(userId);
        return categories.stream()
                .filter(category -> CATEGORY_STATUS_ENABLED == category.getStatus())
                .map(BillCategory::getCategoryName)
                .collect(Collectors.toList());
    }

    @Override
    public Long findCategoryIdByName(String categoryName, Long userId) {
        if (!StringUtils.hasText(categoryName)) {
            return null;
        }

        LambdaQueryWrapper<BillCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> 
            wrapper.eq(BillCategory::getUserId, userId)
            .or()
            .isNull(BillCategory::getUserId)
        );
        queryWrapper.eq(BillCategory::getCategoryName, categoryName.trim())
                   .eq(BillCategory::getStatus, CATEGORY_STATUS_ENABLED);

        BillCategory category = billCategoryMapper.selectOne(queryWrapper);
        return category != null ? category.getId() : null;
    }

    @Override
    public int createDefaultCategoriesForUser(Long userId) {
        // TODO: 前端自定义分类功能尚未实现
        // 当前系统已有预设分类，用户可以直接使用系统分类
        // 未来需要支持用户创建和管理自定义分类
        log.info("用户{}使用系统预设分类", userId);
        return 0;
    }

    @Override
    public Long getOrCreateDefaultCategory(String categoryName, Long userId) {
        Long existingId = findCategoryIdByName(categoryName, userId);
        if (existingId != null) {
            return existingId;
        }

        log.warn("未找到匹配的分类: {}, 用户ID: {}", categoryName, userId);
        return null;
    }

    private int calculateMatchScore(BillCategory category, List<String> keywords) {
        int score = 0;
        String categoryName = category.getCategoryName().toLowerCase();
        String description = category.getDescription() != null ? category.getDescription().toLowerCase() : "";

        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase().trim();
            if (lowerKeyword.isEmpty()) {
                continue;
            }

            if (categoryName.equals(lowerKeyword)) {
                score += SCORE_EXACT_MATCH;
            } else if (categoryName.contains(lowerKeyword)) {
                score += SCORE_CONTAINS_MATCH;
            } else if (description.contains(lowerKeyword)) {
                score += SCORE_DESCRIPTION_MATCH;
            } else {
                List<String> categoryKeywords = CATEGORY_KEYWORDS.get(category.getCategoryName());
                if (categoryKeywords != null && categoryKeywords.contains(lowerKeyword)) {
                    score += SCORE_KEYWORD_MATCH;
                }
            }
        }

        return score;
    }
}

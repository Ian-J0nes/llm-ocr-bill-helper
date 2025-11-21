package org.maram.bill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.config.properties.AppConfigProperties;
import org.maram.bill.entity.BillCategory;
import org.maram.bill.mapper.BillCategoryMapper;
import org.maram.bill.service.BillCategoryService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 账单分类服务实现类
 */
@Service
@Slf4j
public class BillCategoryServiceImpl extends ServiceImpl<BillCategoryMapper, BillCategory> implements BillCategoryService {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;
    private static final int IS_SYSTEM_CATEGORY = 1;
    private static final int IS_USER_CATEGORY = 0;
    private static final int DEFAULT_SORT_ORDER = 0;

    private final AppConfigProperties appConfigProperties;

    public BillCategoryServiceImpl(AppConfigProperties appConfigProperties) {
        this.appConfigProperties = appConfigProperties;
    }

    @Override
    public BillCategory getById(Long id) {
        log.debug("根据ID获取账单分类: {}", id);
        return baseMapper.selectById(id);
    }

    @Override
    @Cacheable(value = "categories", key = "'user:' + #userId", unless = "#result == null or #result.isEmpty()")
    public List<BillCategory> listByUserId(Long userId) {
        log.debug("获取用户{}的账单分类列表", userId);
        return baseMapper.selectByUserId(userId);
    }

    @Override
    @Cacheable(value = "categories", key = "'system'", unless = "#result == null or #result.isEmpty()")
    public List<BillCategory> listSystemCategories() {
        log.debug("获取系统预设分类列表");
        return baseMapper.selectSystemCategories();
    }



    @Override
    public boolean save(BillCategory billCategory) {
        log.debug("保存账单分类: {}", billCategory);
        if (billCategory.getSortOrder() == null) {
            billCategory.setSortOrder(DEFAULT_SORT_ORDER);
        }
        if (billCategory.getStatus() == null) {
            billCategory.setStatus(STATUS_ENABLED);
        }
        if (billCategory.getIsSystem() == null) {
            billCategory.setIsSystem(IS_USER_CATEGORY);
        }
        
        return baseMapper.insert(billCategory) > 0;
    }

    @Override
    public boolean updateById(BillCategory billCategory) {
        log.debug("更新账单分类: {}", billCategory);
        BillCategory existingCategory = getById(billCategory.getId());
        if (isSystemCategory(existingCategory)) {
            log.warn("系统预设分类不允许被修改: {}", billCategory.getId());
            return false;
        }
        
        return baseMapper.updateById(billCategory) > 0;
    }

    @Override
    public boolean removeById(Long id) {
        log.debug("删除账单分类: {}", id);
        BillCategory category = getById(id);
        if (category == null) {
            log.warn("账单分类不存在: {}", id);
            return false;
        }
        
        if (isSystemCategory(category)) {
            log.warn("系统预设分类不允许被删除: {}", id);
            return false;
        }
        
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        log.debug("更新账单分类状态: {}, 状态: {}", id, status);
        BillCategory category = new BillCategory();
        category.setId(id);
        category.setStatus(status);
        
        BillCategory existingCategory = getById(id);
        if (isSystemCategory(existingCategory)) {
            log.warn("系统预设分类不允许修改状态: {}", id);
            return false;
        }
        
        return baseMapper.updateById(category) > 0;
    }

    @Override
    public Page<BillCategory> page(Page<BillCategory> page, Long userId) {
        log.debug("分页查询用户{}的账单分类: 页码 {}, 每页 {} 条", userId, page.getCurrent(), page.getSize());

        LambdaQueryWrapper<BillCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper ->
            wrapper.eq(BillCategory::getUserId, userId)
            .or()
            .isNull(BillCategory::getUserId)
        );
        queryWrapper.orderByAsc(BillCategory::getSortOrder);

        return baseMapper.selectPage(page, queryWrapper);
    }

    // ========== 验证相关方法实现 ==========

    @Override
    public boolean hasAccessPermission(Long categoryId, Long userId) {
        log.debug("验证用户{}对分类{}的访问权限", userId, categoryId);
        if (categoryId == null || userId == null) {
            return false;
        }

        BillCategory category = getById(categoryId);
        if (category == null) {
            return false;
        }

        // 系统分类所有用户都可以访问，用户分类只有创建者可以访问
        return category.getUserId() == null || userId.equals(category.getUserId());
    }

    @Override
    public boolean isDuplicateName(String categoryName, Long userId, Long excludeId) {
        log.debug("验证分类名称是否重复: name={}, userId={}, excludeId={}", categoryName, userId, excludeId);
        return isDuplicateField(BillCategory::getCategoryName, categoryName, userId, excludeId);
    }

    @Override
    public boolean isDuplicateCode(String categoryCode, Long userId, Long excludeId) {
        log.debug("验证分类编码是否重复: code={}, userId={}, excludeId={}", categoryCode, userId, excludeId);
        return isDuplicateField(BillCategory::getCategoryCode, categoryCode, userId, excludeId);
    }

    /**
     * 通用的字段重复检查方法
     */
    private boolean isDuplicateField(SFunction<BillCategory, String> fieldGetter, String fieldValue, Long userId, Long excludeId) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return false;
        }

        LambdaQueryWrapper<BillCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(fieldGetter, fieldValue.trim())
                   .eq(BillCategory::getUserId, userId);

        if (excludeId != null) {
            queryWrapper.ne(BillCategory::getId, excludeId);
        }

        return baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean canDelete(Long categoryId) {
        log.debug("验证分类{}是否可以删除", categoryId);
        if (categoryId == null) {
            return false;
        }

        BillCategory category = getById(categoryId);
        if (category == null) {
            return false;
        }

        if (isSystemCategory(category)) {
            log.warn("系统分类不允许删除: {}", categoryId);
            return false;
        }

        // TODO: 检查是否有关联的账单，如果有则不允许删除
        // 这里可以调用BillService来检查
        return true;
    }

    private boolean isSystemCategory(BillCategory category) {
        return category != null && IS_SYSTEM_CATEGORY == category.getIsSystem();
    }
}
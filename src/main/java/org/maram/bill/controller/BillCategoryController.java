package org.maram.bill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.entity.BillCategory;
import org.maram.bill.service.BillCategoryMatchingService;
import org.maram.bill.service.BillCategoryService;
import org.maram.bill.common.security.UserContext;
import org.maram.bill.common.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 账单分类控制器
 */
@RestController
@RequestMapping("/bill-category")
@Slf4j
@RequiredArgsConstructor
public class BillCategoryController {

    private final BillCategoryService billCategoryService;
    private final BillCategoryMatchingService categoryMatchingService;
    private final UserContext userContext;

    /**
     * 查询当前用户的账单分类
     */
    @GetMapping
    public Result<?> listUserCategories(
            @RequestParam(value = "current", required = false) Long current,
            @RequestParam(value = "size", required = false) Long size) {
        return withUser(userId -> {
            if (current == null && size == null) {
                return Result.success(billCategoryService.listByUserId(userId));
            }
            long pageNum = (current == null || current <= 0) ? 1 : current;
            long pageSize = (size == null || size <= 0) ? 10 : size;
            log.info("分页查询用户ID: {} 的账单分类列表, 页码: {}, 每页: {} 条", userId, pageNum, pageSize);
            Page<BillCategory> page = new Page<>(pageNum, pageSize);
            Page<BillCategory> categoryPage = billCategoryService.page(page, userId);
            return Result.success(categoryPage);
        });
    }



    /**
     * 根据ID获取账单分类
     */
    @GetMapping("/{id}")
    public Result<BillCategory> getById(@PathVariable Long id) {
        return withCategory(id, (userId, category) -> {
            log.info("用户ID: {} 获取账单分类ID: {}", userId, id);
            return Result.success(category);
        });
    }

    /**
     * 新增账单分类
     */
    @PostMapping
    public Result<BillCategory> add(@Valid @RequestBody BillCategory billCategory) {
        return withUser(userId -> {
            if (billCategoryService.isDuplicateName(billCategory.getCategoryName(), userId, null)) {
                return Result.error("分类名称已存在");
            }

            billCategory.setUserId(userId);
            billCategory.setIsSystem(0);
            log.info("用户ID: {} 新增账单分类: {}", userId, billCategory.getCategoryName());

            boolean success = billCategoryService.save(billCategory);
            if (success) {
                log.info("用户ID: {} 新增账单分类成功: {}", userId, billCategory.getId());
                return Result.success("新增成功", billCategory);
            }
            log.warn("用户ID: {} 新增账单分类失败", userId);
            return Result.error("新增账单分类失败");
        });
    }

    /**
     * 更新账单分类
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody BillCategory billCategory) {
        billCategory.setId(id);
        return withCategory(id, (userId, existing) -> {
            log.info("用户ID: {} 更新账单分类ID: {}", userId, id);

            boolean success = billCategoryService.updateById(billCategory);
            if (success) {
                log.info("用户ID: {} 更新账单分类成功: {}", userId, id);
                return Result.success("更新成功");
            }
            log.warn("用户ID: {} 更新账单分类失败: {}", userId, id);
            return Result.error("更新账单分类失败");
        });
    }

    /**
     * 删除账单分类
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> removeById(@PathVariable Long id) {
        return withCategory(id, (userId, category) -> {
            log.info("用户ID: {} 删除账单分类ID: {}", userId, id);

            if (!billCategoryService.canDelete(id)) {
                log.warn("账单分类ID: {} 不允许删除", id);
                return Result.error("该分类不允许删除");
            }

            boolean success = billCategoryService.removeById(id);
            if (success) {
                log.info("用户ID: {} 删除账单分类成功: {}", userId, id);
                return Result.success("删除成功");
            }
            log.warn("用户ID: {} 删除账单分类失败: {}", userId, id);
            return Result.error("删除账单分类失败");
        });
    }

    /**
     * 更新账单分类状态
     */
    @PutMapping("/{id}/status/{status}")
    public Result<Boolean> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("状态值无效，只能是0或1");
        }

        return withCategory(id, (userId, category) -> {
            log.info("用户ID: {} 更新账单分类ID: {} 的状态为: {}", userId, id, status);

            boolean success = billCategoryService.updateStatus(id, status);
            if (success) {
                log.info("用户ID: {} 更新账单分类状态成功: {}", userId, id);
                return Result.success("状态更新成功");
            }
            log.warn("用户ID: {} 更新账单分类状态失败: {}", userId, id);
            return Result.error("更新账单分类状态失败");
        });
    }

    /**
     * 获取系统预设分类
     */
    @GetMapping("/system")
    public Result<List<BillCategory>> getSystemCategories() {
        log.info("获取系统预设分类列表");
        List<BillCategory> systemCategories = billCategoryService.listSystemCategories();
        return Result.success(systemCategories);
    }

    /**
     * 获取用户可用分类名称列表（用于LLM提示）
     */
    @GetMapping("/names")
    public Result<List<String>> getCategoryNames() {
        return withUser(userId -> Result.success(categoryMatchingService.getAvailableCategoryNames(userId)));
    }

    private <T> Result<T> withUser(Function<Long, Result<T>> action) {
        return userContext.currentUserId()
                .map(action)
                .orElseGet(() -> Result.unauthorized("无法获取用户信息，请重新登录"));
    }

    private <T> Result<T> withCategory(Long categoryId, BiFunction<Long, BillCategory, Result<T>> action) {
        return withUser(userId -> {
            BillCategory category = billCategoryService.getById(categoryId);
            if (category == null) {
                log.warn("账单分类ID: {} 不存在", categoryId);
                return Result.error("分类不存在");
            }
            if (!billCategoryService.hasAccessPermission(categoryId, userId)) {
                log.warn("用户ID: {} 无权限操作账单分类ID: {}", userId, categoryId);
                return Result.error("无权操作此分类");
            }
            return action.apply(userId, category);
        });
    }
}

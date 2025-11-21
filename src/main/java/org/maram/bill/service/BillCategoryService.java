package org.maram.bill.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.maram.bill.entity.BillCategory;

import java.util.List;

/**
 * 账单分类服务接口，定义账单分类相关的业务操作
 */
public interface BillCategoryService {

    /**
     * 根据ID获取账单分类
     * @param id 分类ID
     * @return 账单分类信息
     */
    BillCategory getById(Long id);

    /**
     * 获取指定用户的所有账单分类（包含系统预设分类）
     * @param userId 用户ID
     * @return 账单分类列表
     */
    List<BillCategory> listByUserId(Long userId);

    /**
     * 获取系统预设分类
     * @return 系统预设分类列表
     */
    List<BillCategory> listSystemCategories();

    /**
     * 新增账单分类
     * @param billCategory 账单分类信息
     * @return 是否成功
     */
    boolean save(BillCategory billCategory);

    /**
     * 更新账单分类
     * @param billCategory 账单分类信息
     * @return 是否成功
     */
    boolean updateById(BillCategory billCategory);

    /**
     * 删除账单分类
     * @param id 分类ID
     * @return 是否成功
     */
    boolean removeById(Long id);

    /**
     * 启用/禁用账单分类
     * @param id 分类ID
     * @param status 状态（0-禁用，1-启用）
     * @return 是否成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 分页查询账单分类
     * @param page 分页参数
     * @param userId 用户ID
     * @return 分页结果
     */
    Page<BillCategory> page(Page<BillCategory> page, Long userId);

    // ========== 验证相关方法 ==========

    /**
     * 验证分类是否属于指定用户或系统分类
     * @param categoryId 分类ID
     * @param userId 用户ID
     * @return 是否有权限访问
     */
    boolean hasAccessPermission(Long categoryId, Long userId);

    /**
     * 验证分类名称是否重复
     * @param categoryName 分类名称
     * @param userId 用户ID
     * @param excludeId 排除的分类ID（用于更新时排除自身）
     * @return 是否重复
     */
    boolean isDuplicateName(String categoryName, Long userId, Long excludeId);

    /**
     * 验证分类编码是否重复
     * @param categoryCode 分类编码
     * @param userId 用户ID
     * @param excludeId 排除的分类ID（用于更新时排除自身）
     * @return 是否重复
     */
    boolean isDuplicateCode(String categoryCode, Long userId, Long excludeId);

    /**
     * 验证是否可以删除分类
     * @param categoryId 分类ID
     * @return 是否可以删除
     */
    boolean canDelete(Long categoryId);
}
package org.maram.bill.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.maram.bill.entity.Bill;

import java.time.LocalDate;
import java.util.List;

public interface BillService {
    Bill getById(Long id);
    List<Bill> list(); // 考虑是否保留或修改
    List<Bill> listByUserId(Long userId); // 新增或修改

    Long saveAndReturnId(Bill bill);
    boolean updateById(Bill bill);
    boolean removeById(Long id);

    Page<Bill> pageUserBills(Page<Bill> pageRequest, Long userId, String transactionType, String billType, Long categoryId, String startDate, String endDate);

    boolean existsByFileId(Long fileId);
    Bill getByFileId(Long fileId);

    /**
     * 验证账单分类是否有效
     * @param categoryId 分类ID
     * @param userId 用户ID
     * @return 是否有效
     */
    boolean isValidCategory(Long categoryId, Long userId);

    /**
     * 获取指定分类下的账单数量
     * @param categoryId 分类ID
     * @param userId 用户ID
     * @return 账单数量
     */
    long countBillsByCategory(Long categoryId, Long userId);

    /**
     * 根据日期范围查询用户账单
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 账单列表
     */
    List<Bill> listByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
}
package org.maram.bill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.common.security.UserContext;
import org.maram.bill.config.properties.AppConfigProperties;
import org.maram.bill.entity.Bill;
import org.maram.bill.service.BillService;
import org.maram.bill.common.utils.Result;
import org.maram.bill.common.utils.ResultCode;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bill")
@Slf4j
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;
    private final AppConfigProperties appConfigProperties;
    private final UserContext userContext;

    /**
     * 根据ID获取账单
     *
     * @param id 账单ID
     * @return 账单信息
     */
    @GetMapping("/{id}")
    public Result<Bill> getById(@PathVariable Long id) {
        return withBill(id, (userId, bill) -> {
            log.info("用户 {} 查询账单 {}", userId, id);
            return Result.success(bill);
        });
    }

    /**
     * 查询当前用户的账单
     */
    @GetMapping
    public Result<?> query(
            @RequestParam(value = "current", required = false) Long current,
            @RequestParam(value = "size", required = false) Long size,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String billType,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        return withUser(userId -> {
            if (current == null && size == null) {
                // 不分页查询
                List<Bill> bills;

                // 检查是否有日期参数
                if (StringUtils.hasText(startDate) || StringUtils.hasText(endDate)) {
                    try {
                        LocalDate start = StringUtils.hasText(startDate) ? LocalDate.parse(startDate) : null;
                        LocalDate end = StringUtils.hasText(endDate) ? LocalDate.parse(endDate) : null;
                        log.info("查询用户 {} 的账单，日期范围: {} 到 {}", userId, start, end);
                        bills = billService.listByUserIdAndDateRange(userId, start, end);
                    } catch (DateTimeParseException e) {
                        log.error("日期格式解析错误: startDate={}, endDate={}", startDate, endDate, e);
                        return Result.error(ResultCode.BAD_REQUEST, "日期格式错误，请使用 yyyy-MM-dd 格式");
                    }
                } else {
                    log.info("查询用户 {} 的全部账单", userId);
                    bills = billService.listByUserId(userId);
                }

                return Result.success(bills);
            }

            long pageNum = (current == null || current <= 0) ? 1 : current;
            long pageSize = (size == null || size <= 0)
                    ? appConfigProperties.getPagination().getDefaultSize()
                    : size;
            if (pageSize > appConfigProperties.getPagination().getMaxSize()) {
                pageSize = appConfigProperties.getPagination().getMaxSize();
            }

            log.info("分页查询用户 {} 的账单: 第{}页, 每页{}条, transactionType: {}, billType: {}, categoryId: {}, startDate: {}, endDate: {}",
                    userId, pageNum, pageSize, transactionType, billType, categoryId, startDate, endDate);

            Page<Bill> pageRequest = new Page<>(pageNum, pageSize);
            Page<Bill> billPage = billService.pageUserBills(pageRequest, userId, transactionType, billType, categoryId, startDate, endDate);
            return Result.success(billPage);
        });
    }

    /**
     * 保存账单
     *
     * @param bill 账单对象
     * @return 操作结果，包含新账单的ID和fileId (如果存在)
     */
    @PostMapping
    public Result<Map<String, String>> save(@Valid @RequestBody Bill bill) {
        return withUser(userId -> {
            bill.setUserId(userId);

            if (bill.getFileId() != null) {
                Bill existingBillWithFileId = billService.getByFileId(bill.getFileId());
                if (existingBillWithFileId != null) {
                    if (!existingBillWithFileId.getUserId().equals(userId)) {
                        return Result.error(ResultCode.BILL_FILE_ASSOCIATED);
                    }
                    return Result.error(ResultCode.BILL_ALREADY_EXISTS);
                }
            }

            Long billId = billService.saveAndReturnId(bill);
            if (billId != null) {
                return Result.success("账单保存成功", buildBillResponse(billId, bill.getFileId(), bill.getTransactionType()));
            }
            return Result.error(ResultCode.BILL_CREATION_FAILED);
        });
    }

    /**
     * 更新账单
     *
     * @param bill 账单对象
     * @return 操作结果，包含账单的ID和fileId (如果存在)
     */
    @PutMapping("/{id}")
    public Result<Map<String, String>> updateById(@PathVariable Long id, @Valid @RequestBody Bill bill) {
        bill.setId(id);
        return withBill(id, (userId, currentBill) -> {
            bill.setUserId(userId);

            if (bill.getFileId() != null) {
                if (!bill.getFileId().equals(currentBill.getFileId())) {
                    Bill existingBillWithFileId = billService.getByFileId(bill.getFileId());
                    if (existingBillWithFileId != null && !existingBillWithFileId.getId().equals(bill.getId())) {
                        if (!existingBillWithFileId.getUserId().equals(userId)) {
                            return Result.error(ResultCode.BILL_FILE_ASSOCIATED);
                        }
                        return Result.error(ResultCode.BILL_ALREADY_EXISTS);
                    }
                }
            }

            boolean result = billService.updateById(bill);
            if (result) {
                return Result.success("账单更新成功", buildBillResponse(bill.getId(), bill.getFileId(), bill.getTransactionType()));
            }
            return Result.error(ResultCode.BILL_UPDATE_FAILED);
        });
    }

    /**
     * 删除账单
     *
     * @param id 账单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> removeById(@PathVariable Long id) {
        return withBill(id, (userId, bill) -> {
            log.info("用户 {} 准备删除账单 {}", userId, id);

            boolean result = billService.removeById(id);
            if (result) {
                log.info("账单删除成功: ID {}", id);
                return Result.success("操作成功");
            }
            log.error("账单删除失败: ID {}", id);
            return Result.error(ResultCode.BILL_DELETION_FAILED);
        });
    }

    private <T> Result<T> withUser(Function<Long, Result<T>> action) {
        return userContext.currentUserId()
                .map(action)
                .orElseGet(() -> Result.unauthorized("无法获取用户信息，请重新登录"));
    }

    private <T> Result<T> withBill(Long billId, BiFunction<Long, Bill, Result<T>> action) {
        return withUser(userId -> {
            Bill bill = billService.getById(billId);
            if (bill == null) {
                log.warn("账单未找到: {}", billId);
                return Result.error(ResultCode.BILL_NOT_FOUND);
            }
            if (!bill.getUserId().equals(userId)) {
                log.warn("用户 {} 无权访问账单 {}", userId, billId);
                return Result.error(ResultCode.BILL_PERMISSION_DENIED);
            }
            return action.apply(userId, bill);
        });
    }

    private Map<String, String> buildBillResponse(Long billId, Long fileId, String transactionType) {
        Map<String, String> response = new HashMap<>();
        response.put("id", billId.toString());
        if (fileId != null) {
            response.put("fileId", fileId.toString());
        }
        if (transactionType != null) {
            response.put("transactionType", transactionType);
        }
        return response;
    }
}

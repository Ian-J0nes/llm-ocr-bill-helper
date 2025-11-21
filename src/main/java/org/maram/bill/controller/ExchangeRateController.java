package org.maram.bill.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.common.utils.Result;
import org.maram.bill.common.utils.ResultCode;
import org.maram.bill.entity.Currency;
import org.maram.bill.service.ExchangeRateService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/currencies")
    public Result<List<Currency>> getSupportedCurrencies() {
        log.info("正在获取货币列表");
        try {
            return Result.success(exchangeRateService.getSupportedCurrencies());
        } catch (Exception e) {
            log.error("获取支持货币列表失败", e);
            return Result.error(ResultCode.EXCHANGE_RATE_SERVICE_ERROR);
        }
    }

    /**
     * 使用已存储的汇率进行转换
     */
    @PostMapping("/conversions")
    public Result<Map<String, Object>> convertCurrency(
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("from") String fromCurrencyCode,
            @RequestParam("to") String toCurrencyCode) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error(ResultCode.BAD_REQUEST, Map.of("error", "金额必须为正数"));
        }
        if (!StringUtils.hasText(fromCurrencyCode) || !StringUtils.hasText(toCurrencyCode)) {
            return Result.error(ResultCode.BAD_REQUEST, Map.of("error", "货币代码不能为空"));
        }

        try {
            log.info("收到基于存储汇率的转换请求: amount={}, from={}, to={}", amount, fromCurrencyCode, toCurrencyCode);
            Map<String, Object> conversionResult = exchangeRateService.convertCurrencyUsingStoredRates(amount, fromCurrencyCode, toCurrencyCode);
            return Result.success(conversionResult);
        } catch (IllegalArgumentException | ArithmeticException e) {
            log.warn("货币转换请求参数无效或计算错误: amount={}, from={}, to={}, error={}", amount, fromCurrencyCode, toCurrencyCode, e.getMessage());
            return Result.error(ResultCode.BAD_REQUEST, Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("货币转换时发生错误: amount={}, from={}, to={}", amount, fromCurrencyCode, toCurrencyCode, e);
            if (e.getMessage() != null && e.getMessage().startsWith("无法找到存储的汇率")) {
                return Result.error(ResultCode.EXCHANGE_RATE_NOT_FOUND, Map.of("error", e.getMessage()));
            }
            return Result.error(ResultCode.EXCHANGE_RATE_SERVICE_ERROR, Map.of("error", "货币转换失败，请稍后重试"));
        }
    }
}

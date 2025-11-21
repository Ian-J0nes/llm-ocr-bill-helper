package org.maram.bill.service;

import org.maram.bill.entity.Currency;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ExchangeRateService {

    /**
     * 获取所有激活的支持的货币列表
     * @return 货币实体列表
     */
    List<Currency> getSupportedCurrencies();

    /**
     * 根据数据库中存储的最新汇率执行货币转换
     * @param amount 金额
     * @param fromCurrencyCode 源货币代码 (必须是配置的base-currency或者target-currencies中的一个)
     * @param toCurrencyCode 目标货币代码 (必须是配置的base-currency或者target-currencies中的一个)
     * @return 一个包含转换结果的Map。如果无法找到汇率，则抛出异常或返回错误提示。
     */
    Map<String, Object> convertCurrencyUsingStoredRates(BigDecimal amount, String fromCurrencyCode, String toCurrencyCode);

    /**
     * (供定时任务调用)
     * 从外部API获取并更新配置中指定的货币对的汇率到数据库。
     */
    void fetchAndUpdateExchangeRates();
}
package org.maram.bill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.config.properties.ExchangeRateApiProperties;
import org.maram.bill.entity.Currency;
import org.maram.bill.entity.ExchangeRate;
import org.maram.bill.mapper.CurrencyMapper;
import org.maram.bill.mapper.ExchangeRateMapper;
import org.maram.bill.service.ExchangeRateService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 汇率服务实现类
 */
@Service
@Slf4j
public class ExchangeRateServiceImpl extends ServiceImpl<ExchangeRateMapper, ExchangeRate> implements ExchangeRateService {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
    
    private static final int RATE_CALCULATION_SCALE = 10;
    private static final int RATE_DISPLAY_SCALE = 8;
    private static final int AMOUNT_CALCULATION_SCALE = 8;
    private static final int AMOUNT_DISPLAY_SCALE = 2;
    
    private static final int HTTP_SUCCESS_CODE = 200;
    private static final String SUCCESS_CODE_STRING = "200";
    private static final String DUPLICATE_ENTRY_ERROR = "duplicate entry";
    private static final String API_SOURCE_PREFIX = "ScheduledTaskAPI:";

    private final RestTemplate restTemplate;
    private final ExchangeRateApiProperties apiProperties;
    private final CurrencyMapper currencyMapper;

    public ExchangeRateServiceImpl(RestTemplateBuilder restTemplateBuilder,
                                   ExchangeRateApiProperties apiProperties,
                                   CurrencyMapper currencyMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiProperties = apiProperties;
        this.currencyMapper = currencyMapper;
    }

    @Override
    public List<Currency> getSupportedCurrencies() {
        LambdaQueryWrapper<Currency> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Currency::isActive, true);
        return currencyMapper.selectList(queryWrapper);
    }

    @Override
    public Map<String, Object> convertCurrencyUsingStoredRates(BigDecimal amount, String fromCurrencyCode, String toCurrencyCode) {
        String fromCode = fromCurrencyCode.toUpperCase();
        String toCode = toCurrencyCode.toUpperCase();
        String baseCode = apiProperties.getBaseCurrency().toUpperCase();

        if (fromCode.equals(toCode)) {
            return createConversionResult(amount, fromCode, amount, toCode, BigDecimal.ONE, LocalDateTime.now());
        }

        BigDecimal finalRate;
        LocalDateTime rateTimestamp;

        if (fromCode.equals(baseCode)) {
            ConversionRate conversionRate = calculateRateFromBase(baseCode, toCode);
            finalRate = conversionRate.rate();
            rateTimestamp = conversionRate.timestamp();
        } else if (toCode.equals(baseCode)) {
            ConversionRate conversionRate = calculateRateToBase(baseCode, fromCode);
            finalRate = conversionRate.rate();
            rateTimestamp = conversionRate.timestamp();
        } else {
            ConversionRate conversionRate = calculateRateThroughBase(baseCode, fromCode, toCode);
            finalRate = conversionRate.rate();
            rateTimestamp = conversionRate.timestamp();
        }

        BigDecimal convertedAmount = amount.multiply(finalRate).setScale(AMOUNT_CALCULATION_SCALE, RoundingMode.HALF_UP);
        return createConversionResult(amount, fromCode, convertedAmount, toCode, finalRate, rateTimestamp);
    }

    private ConversionRate calculateRateFromBase(String baseCode, String toCode) {
        ExchangeRate rateEntity = getLatestStoredRate(baseCode, toCode);
        if (rateEntity == null) {
            throw new RuntimeException("无法找到存储的汇率: " + baseCode + " -> " + toCode);
        }
        return new ConversionRate(rateEntity.getRate(), rateEntity.getLastUpdatedFromApi());
    }

    private ConversionRate calculateRateToBase(String baseCode, String fromCode) {
        ExchangeRate rateEntity = getLatestStoredRate(baseCode, fromCode);
        if (rateEntity == null) {
            throw new RuntimeException("无法找到存储的汇率: " + baseCode + " -> " + fromCode + " (用于计算 " + fromCode + " -> " + baseCode + ")");
        }
        if (rateEntity.getRate().compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("存储的汇率 " + baseCode + " -> " + fromCode + " 为0，无法计算倒数");
        }
        BigDecimal rate = BigDecimal.ONE.divide(rateEntity.getRate(), RATE_CALCULATION_SCALE, RoundingMode.HALF_UP);
        return new ConversionRate(rate, rateEntity.getLastUpdatedFromApi());
    }

    private ConversionRate calculateRateThroughBase(String baseCode, String fromCode, String toCode) {
        ExchangeRate rateBaseToTarget = getLatestStoredRate(baseCode, toCode);
        ExchangeRate rateBaseToSource = getLatestStoredRate(baseCode, fromCode);

        if (rateBaseToTarget == null) {
            throw new RuntimeException("无法找到存储的汇率: " + baseCode + " -> " + toCode);
        }
        if (rateBaseToSource == null) {
            throw new RuntimeException("无法找到存储的汇率: " + baseCode + " -> " + fromCode);
        }
        if (rateBaseToSource.getRate().compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("存储的汇率 " + baseCode + " -> " + fromCode + " 为0，无法进行中转计算");
        }

        BigDecimal rate = rateBaseToTarget.getRate().divide(rateBaseToSource.getRate(), RATE_CALCULATION_SCALE, RoundingMode.HALF_UP);
        LocalDateTime timestamp = rateBaseToTarget.getLastUpdatedFromApi().isAfter(rateBaseToSource.getLastUpdatedFromApi()) ?
                rateBaseToTarget.getLastUpdatedFromApi() : rateBaseToSource.getLastUpdatedFromApi();
        
        return new ConversionRate(rate, timestamp);
    }

    private Map<String, Object> createConversionResult(BigDecimal originalAmount, String fromCurrency,
                                                       BigDecimal convertedAmount, String toCurrency,
                                                       BigDecimal rateUsed, LocalDateTime rateTimestamp) {
        Map<String, Object> result = new HashMap<>();
        result.put("originalAmount", originalAmount);
        result.put("fromCurrency", fromCurrency);
        result.put("convertedAmount", convertedAmount.setScale(AMOUNT_DISPLAY_SCALE, RoundingMode.HALF_UP));
        result.put("toCurrency", toCurrency);
        result.put("rateUsed", rateUsed.setScale(RATE_DISPLAY_SCALE, RoundingMode.HALF_UP));
        result.put("rateTimestamp", rateTimestamp);
        return result;
    }

    private ExchangeRate getLatestStoredRate(String baseCurrencyCode, String targetCurrencyCode) {
        LambdaQueryWrapper<ExchangeRate> query = new LambdaQueryWrapper<>();
        query.eq(ExchangeRate::getBaseCurrencyCode, baseCurrencyCode)
                .eq(ExchangeRate::getTargetCurrencyCode, targetCurrencyCode)
                .orderByDesc(ExchangeRate::getLastUpdatedFromApi)
                .last("LIMIT 1");
        return baseMapper.selectOne(query);
    }

    @Override
    @Transactional
    public void fetchAndUpdateExchangeRates() {
        String baseCurrency = apiProperties.getBaseCurrency().toUpperCase();
        List<String> targetCurrencies = apiProperties.getTargetCurrencies();

        if (targetCurrencies == null || targetCurrencies.isEmpty()) {
            log.warn("目标货币列表未配置，定时更新任务跳过");
            return;
        }

        log.info("开始定时更新汇率任务，基准货币: {}, 目标货币: {}", baseCurrency, targetCurrencies);

        for (String targetCurrency : targetCurrencies) {
            targetCurrency = targetCurrency.toUpperCase();
            if (baseCurrency.equals(targetCurrency)) {
                continue;
            }

            try {
                Map<String, Object> apiRateData = fetchRateFromExternalAPI(baseCurrency, targetCurrency, BigDecimal.ONE);

                if (apiRateData != null && apiRateData.get("rate") instanceof BigDecimal fetchedRate) {
                    long timestamp = ((Number) apiRateData.getOrDefault("timestamp", System.currentTimeMillis())).longValue();
                    LocalDateTime rateTimestamp = Instant.ofEpochMilli(timestamp).atZone(DEFAULT_ZONE_ID).toLocalDateTime();

                    String apiFromCode = (String) apiRateData.get("from");
                    String apiToCode = (String) apiRateData.get("to");

                    if (apiFromCode != null && apiToCode != null &&
                            apiFromCode.equals(baseCurrency) && apiToCode.equals(targetCurrency)) {
                        saveExchangeRate(apiFromCode, apiToCode, fetchedRate, rateTimestamp, API_SOURCE_PREFIX + apiProperties.getUrl());
                    } else {
                        log.warn("API返回的货币对 ({}-{}) 与请求的 ({}-{}) 不符，跳过保存", apiFromCode, apiToCode, baseCurrency, targetCurrency);
                    }
                } else {
                    log.error("无法从API获取汇率: {} -> {}", baseCurrency, targetCurrency);
                }
            } catch (Exception e) {
                log.error("获取或保存汇率 {} -> {} 时发生错误", baseCurrency, targetCurrency, e);
            }
        }
        log.info("定时更新汇率任务完成");
    }

    private Map<String, Object> fetchRateFromExternalAPI(String fromCurrency, String toCurrency, BigDecimal amountToQuery) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiProperties.getUrl())
                .queryParam("from", fromCurrency)
                .queryParam("to", toCurrency)
                .queryParam("amount", amountToQuery.doubleValue());

        String url = builder.toUriString();
        log.info("请求外部汇率API: {}", url);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseApiResponse(response.getBody(), fromCurrency, toCurrency);
            }
            
            log.error("请求外部API {}->{} 失败: HTTP Status {}", fromCurrency, toCurrency, response.getStatusCode());
            return null;
            
        } catch (HttpClientErrorException e) {
            log.error("请求外部API {}->{} 时发生客户端错误: {} - {}", fromCurrency, toCurrency, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("请求外部API {}->{} 时发生未知错误", fromCurrency, toCurrency, e);
            return null;
        }
    }

    private Map<String, Object> parseApiResponse(Map<?, ?> body, String fromCurrency, String toCurrency) {
        Object code = body.get("code");
        if (!isSuccessCode(code)) {
            log.error("外部API为 {}->{} 返回错误码: {}", fromCurrency, toCurrency, code);
            return null;
        }

        Object dataNode = body.get("data");
        if (!(dataNode instanceof Map<?, ?> data)) {
            log.error("外部API {}->{} 返回的 data 结构异常: {}", fromCurrency, toCurrency, dataNode);
            return null;
        }

        Map<String, Object> parsed = new HashMap<>();
        parsed.put("from", toUpperString(data.get("from")));
        parsed.put("to", toUpperString(data.get("to")));
        parsed.put("rate", toBigDecimal(data.get("rate")));
        parsed.put("timestamp", extractTimestamp(data.get("updateAtTimestamp")));
        return parsed;
    }

    private void saveExchangeRate(String baseCode, String targetCode, BigDecimal rate, LocalDateTime lastUpdateTs, String source) {
        validateCurrencyExists(baseCode, targetCode);

        ExchangeRate newRate = new ExchangeRate();
        newRate.setBaseCurrencyCode(baseCode);
        newRate.setTargetCurrencyCode(targetCode);
        newRate.setRate(rate);
        newRate.setLastUpdatedFromApi(lastUpdateTs);
        newRate.setApiSource(source);

        try {
            int insertedRows = baseMapper.insert(newRate);
            if (insertedRows > 0) {
                log.info("已保存新的汇率记录: {} -> {} = {}, API更新时间: {}", baseCode, targetCode, rate, lastUpdateTs);
            } else {
                log.error("保存新的汇率记录失败 (insert返回0): {} -> {}", baseCode, targetCode);
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains(DUPLICATE_ENTRY_ERROR)) {
                log.debug("尝试插入重复汇率记录，已跳过: {}->{} @{}", baseCode, targetCode, lastUpdateTs);
            } else {
                log.error("保存新的汇率记录时发生数据库错误: {} -> {}", baseCode, targetCode, e);
            }
        }
    }

    private void validateCurrencyExists(String baseCode, String targetCode) {
        if (currencyMapper.selectCount(new LambdaQueryWrapper<Currency>().eq(Currency::getCode, baseCode)) == 0) {
            log.warn("基准货币 {} 不在currencies表中，汇率 {}->{} 可能无法正确关联或使用", baseCode, baseCode, targetCode);
        }
        if (currencyMapper.selectCount(new LambdaQueryWrapper<Currency>().eq(Currency::getCode, targetCode)) == 0) {
            log.warn("目标货币 {} 不在currencies表中，汇率 {}->{} 可能无法正确关联或使用", targetCode, baseCode, targetCode);
        }
    }

    private boolean isSuccessCode(Object code) {
        if (code instanceof Number number) {
            return number.intValue() == HTTP_SUCCESS_CODE;
        }
        if (code instanceof String text) {
            return SUCCESS_CODE_STRING.equals(text);
        }
        return false;
    }

    private String toUpperString(Object value) {
        return value == null ? null : value.toString().trim().toUpperCase();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            log.error("无法将值 {} 转换为 BigDecimal", value, ex);
            return null;
        }
    }

    private long extractTimestamp(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
            }
        }
        return System.currentTimeMillis();
    }

    private record ConversionRate(BigDecimal rate, LocalDateTime timestamp) {}
}

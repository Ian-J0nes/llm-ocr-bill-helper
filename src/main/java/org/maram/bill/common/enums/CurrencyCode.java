package org.maram.bill.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 货币代码枚举
 */
@Getter
public enum CurrencyCode {
    CNY("CNY"),
    USD("USD"),
    EUR("EUR"),
    HKD("HKD"),
    JPY("JPY");

    @JsonValue
    private final String value;

    CurrencyCode(String value) {
        this.value = value;
    }
}

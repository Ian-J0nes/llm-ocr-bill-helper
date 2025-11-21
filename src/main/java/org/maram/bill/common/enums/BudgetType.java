package org.maram.bill.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 预算类型枚举
 */
@Getter
public enum BudgetType {
    MONTHLY("MONTHLY"),
    QUARTERLY("QUARTERLY"),
    YEARLY("YEARLY");

    @JsonValue
    private final String value;

    BudgetType(String value) {
        this.value = value;
    }
}

package org.maram.bill.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 交易类型枚举
 */
@Getter
public enum TransactionType {
    INCOME("income"),
    EXPENSE("expense");

    @JsonValue
    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (TransactionType type : TransactionType.values()) {
            if (type.getValue().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }
}

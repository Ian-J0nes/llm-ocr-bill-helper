package org.maram.bill.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 启用/禁用状态枚举
 */
@Getter
public enum EnableStatus {
    DISABLED(0),
    ENABLED(1);

    @JsonValue
    private final int value;

    EnableStatus(int value) {
        this.value = value;
    }
}

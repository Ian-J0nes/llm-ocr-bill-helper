package org.maram.bill.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 系统分类标识枚举
 */
@Getter
public enum SystemCategory {
    NO(0),
    YES(1);

    @JsonValue
    private final int value;

    SystemCategory(int value) {
        this.value = value;
    }
}

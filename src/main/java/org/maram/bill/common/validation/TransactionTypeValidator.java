package org.maram.bill.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.maram.bill.common.enums.TransactionType;
import java.util.Arrays;

public class TransactionTypeValidator implements ConstraintValidator<ValidTransactionType, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; 
        }
        return Arrays.stream(TransactionType.values())
                .anyMatch(e -> e.getValue().equalsIgnoreCase(value.trim()));
    }
}

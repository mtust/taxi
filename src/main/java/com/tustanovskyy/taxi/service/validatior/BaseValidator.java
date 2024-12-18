package com.tustanovskyy.taxi.service.validatior;

import com.tustanovskyy.taxi.exception.ValidationException;
import java.util.function.Supplier;

public abstract class BaseValidator {

    protected void validate(Supplier<Boolean> condition, String errorMessage) {
        if (!condition.get()) {
            throw new ValidationException(errorMessage);
        }
    }
}

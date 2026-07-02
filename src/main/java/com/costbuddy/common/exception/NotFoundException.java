package com.costbuddy.common.exception;

public class NotFoundException extends BusinessException {

    public NotFoundException(String resourceName, Long id) {
        super("NOT_FOUND", resourceName + " not found: " + id);
    }
}

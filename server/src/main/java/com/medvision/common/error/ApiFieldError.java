package com.medvision.common.error;

public record ApiFieldError(
        String field,
        String message
) {
}
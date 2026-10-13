package com.medvision.common.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);

        if (!status.is4xxClientError()) {
            throw new IllegalArgumentException(
                    "ApiException requires a 4xx status."
            );
        }

        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
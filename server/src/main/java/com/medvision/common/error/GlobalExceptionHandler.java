package com.medvision.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        if (request instanceof ServletWebRequest servletRequest
                && servletRequest.getResponse() != null
                && servletRequest.getResponse().isCommitted()) {
            return null;
        }

        int status = statusCode.value();

        if (status >= 500) {
            log.error("Spring MVC request failed", ex);
        }

        List<ApiFieldError> fieldErrors = List.of();

        if (ex instanceof MethodArgumentNotValidException validationException) {
            fieldErrors = validationException.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(error -> new ApiFieldError(
                            error.getField(),
                            error.getDefaultMessage() == null
                                    ? "Qiymat noto‘g‘ri."
                                    : error.getDefaultMessage()
                    ))
                    .toList();
        }

        String code = ex instanceof MethodArgumentNotValidException
                ? "VALIDATION_ERROR"
                : codeFor(status);

        ApiError error = new ApiError(
                Instant.now(),
                status,
                code,
                messageFor(status),
                pathOf(request),
                fieldErrors
        );

        return new ResponseEntity<>(error, headers, statusCode);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(
            ApiException ex,
            WebRequest request
    ) {
        ApiError error = new ApiError(
                Instant.now(),
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage(),
                pathOf(request),
                List.of()
        );

        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(
            Exception ex,
            WebRequest request
    ) {
        log.error("Unexpected request failure", ex);

        ApiError error = new ApiError(
                Instant.now(),
                500,
                "INTERNAL_ERROR",
                "Serverda kutilmagan xato yuz berdi.",
                pathOf(request),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }

    private String pathOf(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return "";
    }

    private String codeFor(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 409 -> "CONFLICT";
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> status >= 500 ? "INTERNAL_ERROR" : "REQUEST_ERROR";
        };
    }

    private String messageFor(int status) {
        return switch (status) {
            case 400 -> "So‘rov ma’lumotlari noto‘g‘ri.";
            case 401 -> "Tizimga kirish talab qilinadi.";
            case 403 -> "Bu amalga ruxsat berilmagan.";
            case 404 -> "So‘ralgan resurs topilmadi.";
            case 405 -> "Bu endpoint ushbu HTTP metodini qabul qilmaydi.";
            case 406 -> "So‘ralgan javob formati qo‘llab-quvvatlanmaydi.";
            case 409 -> "So‘rov mavjud ma’lumot bilan ziddiyatga tushdi.";
            case 413 -> "Yuklangan fayl ruxsat etilgan hajmdan katta.";
            case 415 -> "So‘rov formati qo‘llab-quvvatlanmaydi.";
            default -> status >= 500
                    ? "Serverda kutilmagan xato yuz berdi."
                    : "So‘rovni bajarib bo‘lmadi.";
        };
    }
}
package com.costbuddy.common.web;

import com.costbuddy.common.api.ApiResponse;
import com.costbuddy.common.exception.BusinessException;
import com.costbuddy.motherboard.MotherboardFailureType;
import com.costbuddy.motherboard.MotherboardGatewayException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        LOGGER.warn("business exception: code={}, message={}", exception.getCode(), exception.getMessage(), exception);
        HttpStatus status = "NOT_FOUND".equals(exception.getCode()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiResponse.failed(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream().map(this::formatFieldError).collect(Collectors.joining("; "));
        LOGGER.warn("validation exception: {}", message, exception);
        return ResponseEntity.badRequest().body(ApiResponse.failed("VALIDATION_FAILED", message));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKeyException(DuplicateKeyException exception) {
        LOGGER.warn("duplicate key exception", exception);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failed("DUPLICATE_KEY", "duplicated unique field"));
    }

    @ExceptionHandler(MotherboardGatewayException.class)
    public ResponseEntity<ApiResponse<Void>> handleMotherboardException(MotherboardGatewayException exception) {
        LOGGER.error("motherboard exception: type={}, upstreamStatus={}, upstreamCode={}", exception.getFailureType(), exception.getUpstreamStatusCode(), exception
            .getUpstreamCode(), exception);
        if (exception.getFailureType() == MotherboardFailureType.TRANSPORT) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.failed("MOTHERBOARD_UNAVAILABLE", "Motherboard service is unavailable"));
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.failed("MOTHERBOARD_INTEGRATION_ERROR", "Motherboard integration request failed"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        LOGGER.error("unexpected exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failed("INTERNAL_ERROR", "internal server error"));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }
}

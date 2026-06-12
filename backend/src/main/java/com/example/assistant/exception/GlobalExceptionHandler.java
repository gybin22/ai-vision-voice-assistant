package com.example.assistant.exception;

import com.example.assistant.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CostLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleCostLimit(CostLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ModelCallException.class)
    public ResponseEntity<ErrorResponse> handleModelCall(ModelCallException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(error("MODEL_CALL_FAILED", e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(error("UPLOAD_TOO_LARGE", "上传文件过大。"));
    }

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestParameterException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "系统内部错误，请稍后再试。"));
    }

    private ErrorResponse error(String code, String message) {
        String requestId = "err_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return new ErrorResponse(requestId, code, message);
    }
}

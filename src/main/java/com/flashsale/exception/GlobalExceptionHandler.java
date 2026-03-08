package com.flashsale.exception;

import com.flashsale.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SoldOutException.class)
    public ResponseEntity<ErrorResponseDTO> handleSoldOut(SoldOutException ex) {
        return buildResponse("SOLD_OUT", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateOrder(DuplicateOrderException ex) {
        return buildResponse("DUPLICATE_ORDER", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FlashSaleNotActiveException.class)
    public ResponseEntity<ErrorResponseDTO> handleFlashSaleNotActive(FlashSaleNotActiveException ex) {
        return buildResponse("FLASH_SALE_NOT_ACTIVE", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        ErrorResponseDTO response = ErrorResponseDTO.builder()
                .error("VALIDATION_ERROR")
                .message("Request validation failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneral(Exception ex) {
        return buildResponse("INTERNAL_ERROR", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(String error, String message, HttpStatus status) {
        ErrorResponseDTO response = ErrorResponseDTO.builder()
                .error(error)
                .message(message)
                .status(status.value())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(response);
    }
}

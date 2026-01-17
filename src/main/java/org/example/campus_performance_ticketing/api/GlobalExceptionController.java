// java
package org.example.campus_performance_ticketing.api;

import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionController {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath().toString() + " " + cv.getMessage())
                .findFirst()
                .orElse(ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(msg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .findFirst()
                .orElse(ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(msg));
    }
}

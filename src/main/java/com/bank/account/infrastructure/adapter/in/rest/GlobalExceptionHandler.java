package com.bank.account.infrastructure.adapter.in.rest;

import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.exception.BusinessRuleViolationException;
import com.bank.account.domain.exception.ConditionsNotFoundException;
import com.bank.account.domain.exception.CustomerNotFoundException;
import com.bank.account.domain.exception.DownstreamServiceUnavailableException;
import com.bank.account.domain.exception.OperationNotFoundException;
import com.bank.account.infrastructure.adapter.in.rest.dto.ErrorResponse;
import com.bank.account.infrastructure.adapter.in.rest.dto.FieldError;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Set<String> CONFLICT_CODES = Set.of("CONCURRENT_MODIFICATION", "OPERATION_ID_REUSED");

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex,
                                                                 ServerWebExchange exchange) {
        return build(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.getMessage(), exchange, null);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException ex,
                                                                  ServerWebExchange exchange) {
        return build(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", ex.getMessage(), exchange, null);
    }

    @ExceptionHandler(ConditionsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConditionsNotFound(ConditionsNotFoundException ex,
                                                                    ServerWebExchange exchange) {
        return build(HttpStatus.NOT_FOUND, "CONDITIONS_NOT_FOUND", ex.getMessage(), exchange, null);
    }

    @ExceptionHandler(OperationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOperationNotFound(OperationNotFoundException ex,
                                                                   ServerWebExchange exchange) {
        return build(HttpStatus.NOT_FOUND, "OPERATION_NOT_FOUND", ex.getMessage(), exchange, null);
    }

    @ExceptionHandler(DownstreamServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamUnavailable(DownstreamServiceUnavailableException ex,
                                                                       ServerWebExchange exchange) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", ex.getMessage(), exchange, null);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleViolationException ex,
                                                              ServerWebExchange exchange) {
        HttpStatus status = CONFLICT_CODES.contains(ex.getErrorCode())
                ? HttpStatus.CONFLICT
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return build(status, ex.getErrorCode(), ex.getMessage(), exchange, null);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(WebExchangeBindException ex,
                                                                ServerWebExchange exchange) {
        List<FieldError> details = ex.getFieldErrors().stream()
                .map(fieldError -> {
                    FieldError detail = new FieldError();
                    detail.setField(fieldError.getField());
                    detail.setMessage(fieldError.getDefaultMessage());
                    return detail;
                })
                .collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", exchange, details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(HandlerMethodValidationException ex,
                                                                 ServerWebExchange exchange) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getReason(), exchange, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handlePathValidation(ConstraintViolationException ex,
                                                                ServerWebExchange exchange) {
        List<FieldError> details = ex.getConstraintViolations().stream()
                .map(violation -> {
                    FieldError detail = new FieldError();
                    String path = violation.getPropertyPath().toString();
                    detail.setField(path.substring(path.lastIndexOf('.') + 1));
                    detail.setMessage(violation.getMessage());
                    return detail;
                })
                .collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", exchange, details);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                  ServerWebExchange exchange, List<FieldError> details) {
        ErrorResponse body = new ErrorResponse();
        body.setTimestamp(OffsetDateTime.now());
        body.setStatus(status.value());
        body.setCode(code);
        body.setMessage(message);
        body.setPath(exchange.getRequest().getPath().value());
        body.setDetails(details);
        return ResponseEntity.status(status).body(body);
    }
}

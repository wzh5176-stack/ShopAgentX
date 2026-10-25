package com.wwzyhy.shopagent.exception;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.RestClientException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.MediaType;
@RestControllerAdvice(basePackages = "com.wwzyhy.shopagent.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(InventoryConflictException.class)
    public ResponseEntity<Map<String, Object>> handleInventoryConflict(InventoryConflictException ex) {
        return errorResponse(409, "INVENTORY_CONFLICTED", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound() {
        return errorResponse(404, "NOT_FOUND", "请求的资源不存在");
    }

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch() {
        return errorResponse(
                400, "INVALID_ARGUMENT", "请求参数格式或范围不正确"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidArgument() {
        return errorResponse(
                400, "INVALID_ARGUMENT", "请求参数不符合要求"
        );
    }

    @ExceptionHandler({
            CannotCreateTransactionException.class,
            DataAccessResourceFailureException.class,
            TransientDataAccessResourceException.class,
            PermissionDeniedDataAccessException.class
    })
    public ResponseEntity<Map<String, Object>> handleDatabaseUnavailable(
            Exception ex) {
        log.error("数据库访问不可用", ex);

        return errorResponse(
                503, "SERVICE_UNAVAILABLE", "数据库服务暂时不可用，请稍后再试"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = "请求参数不符合要求";
        FieldError fieldError = ex.getBindingResult().getFieldError();

        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        }

        return errorResponse(400, "INVALID_ARGUMENT", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody() {
        return errorResponse(
                400, "INVALID_ARGUMENT", "请求体格式不正确，请检查JSON和字段类型"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception ex) {

        Throwable cause = ex;

        while (cause != null) {
            if (cause instanceof DataAccessResourceFailureException
                    || cause instanceof TransientDataAccessResourceException
                    || cause instanceof PermissionDeniedDataAccessException) {

                return handleDatabaseUnavailable(ex);
            }

            cause = cause.getCause();
        }

        log.error("处理接口请求时发生未预期错误", ex);

        return errorResponse(
                500, "INTERNAL_ERROR", "服务内部错误，请稍后再试"
        );
    }

    private ResponseEntity<Map<String, Object>> errorResponse(
            int status, String code, String message) {

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "error", Map.of(
                                "code", code,
                                "message", message
                        )
                ));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, Object>> handleAiUnavailable() {
        return errorResponse(
                503, "AI_SERVICE_UNAVAILABLE", "AI服务暂时不可用，请稍后再试"
        );
    }


    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameAlreadyExists() {
        return errorResponse(
                409, "USERNAME_ALREADY_EXISTS", "用户名已被使用"
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationRequired() {
        return errorResponse(
                401, "UNAUTHORIZED", "登录状态无效，请重新登录"
        );
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleConversationNotFound() {
        return errorResponse(
                404, "CONVERSATION_NOT_FOUND", "会话不存在"
        );
    }

    @ExceptionHandler(ConversationChangedException.class)
    public ResponseEntity<Map<String, Object>> handleConversationChanged() {
        return errorResponse(
                409,
                "CONVERSATION_CHANGED",
                "会话已有新消息，请刷新后重新发送"
        );
    }

    @ExceptionHandler(CartProductUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleCartProductUnavailable() {
        return errorResponse(
                409,
                "PRODUCT_UNAVAILABLE",
                "商品不存在、已下架或已删除，请刷新商品列表"
        );
    }
    @ExceptionHandler(RecommendationSourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRecommendationSourceNotFound() {
        return errorResponse(
                404,
                "RECOMMENDATION_SOURCE_NOT_FOUND",
                "推荐消息不存在");
    }

    @ExceptionHandler(CartActionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCartActionNotFound() {
        return errorResponse(
                404,
                "CART_ACTION_NOT_FOUND",
                "购物车操作不存在");
    }
}

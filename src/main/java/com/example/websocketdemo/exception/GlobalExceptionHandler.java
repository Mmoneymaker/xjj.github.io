package com.example.websocketdemo.exception;

import com.example.websocketdemo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

//没有全局异常处理器需要在controller层去catch业务service层的异常，影响业务观感
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNoResourceFoundException(NoResourceFoundException e) {
        // 忽略Chrome DevTools的特定请求，不记录错误日志
        if (e.getMessage() != null && e.getMessage().contains(".well-known/appspecific/com.chrome.devtools.json")) {
            logger.debug("忽略Chrome DevTools资源请求: {}", e.getMessage());
            return;
        }
        // 其他资源未找到异常正常记录
        logger.error("资源未找到异常: {}", e.getMessage());
    }

    // 1. 先处理具体的业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    // 2.  处理数据库唯一约束异常（用户已存在）
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<String> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("用户重复注册", e);
        return Result.error(403, "用户已存在");
    }
    // 3. 处理运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        return Result.error(500, "系统执行异常，请稍后再试");
    }

    // 4. 处理其他所有异常（兜底）
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统内部异常: ", e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}
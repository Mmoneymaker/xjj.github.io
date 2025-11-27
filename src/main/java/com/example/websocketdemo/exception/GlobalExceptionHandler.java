package com.example.websocketdemo.exception;

import com.example.websocketdemo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
//没有全局异常处理器需要在controller层去catch业务service层的异常，影响业务观感
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 先处理具体的业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // 2. 处理运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        return Result.error(500, "系统执行异常，请稍后再试");
    }

    // 3. 处理其他所有异常（兜底）
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统内部异常: ", e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}
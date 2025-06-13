package org.river.file_server.handler;

import org.river.file_server.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalHandler {

    public static final Logger logger = LoggerFactory.getLogger(GlobalHandler.class);
    @ExceptionHandler(RuntimeException.class)
    public Result<?> runtimeExceptionHandler(RuntimeException e) {
        logger.error(e.getLocalizedMessage());
        return Result.error(e.getMessage());
    }
}

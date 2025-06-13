package org.river.file_server.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    Integer code;
    String message;
    T data;

    public static final Integer CODE_SUCCESS = 200;
    public static final Integer CODE_FAILED = 500;


    public static Result<?> success(String message) {
        return success(message, null);
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(CODE_SUCCESS);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(CODE_FAILED);
        result.setMessage(message);
        return result;
    }

}

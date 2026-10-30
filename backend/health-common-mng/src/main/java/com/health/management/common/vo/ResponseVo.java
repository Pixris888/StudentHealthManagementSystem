package com.health.management.common.vo;

import com.health.management.common.enums.ResultCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一接口返回结构
 *
 * @param <T> 业务数据类型
 */
@Data
public class ResponseVo<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;

    public static <T> ResponseVo<T> success() {
        return success(null);
    }

    public static <T> ResponseVo<T> success(T data) {
        return success(ResultCode.SUCCESS.getMsg(), data);
    }

    public static <T> ResponseVo<T> success(String msg, T data) {
        ResponseVo<T> vo = new ResponseVo<>();
        vo.setCode(ResultCode.SUCCESS.getCode());
        vo.setMsg(msg);
        vo.setData(data);
        return vo;
    }

    public static <T> ResponseVo<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMsg());
    }

    public static <T> ResponseVo<T> fail(ResultCode resultCode, String msg) {
        return fail(resultCode.getCode(), msg);
    }

    public static <T> ResponseVo<T> fail(int code, String msg) {
        ResponseVo<T> vo = new ResponseVo<>();
        vo.setCode(code);
        vo.setMsg(msg);
        return vo;
    }
}

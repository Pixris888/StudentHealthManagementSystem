package com.health.management.common.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.management.common.vo.ResponseVo;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应体处理：Controller 直接返回业务数据，由该 Advice 自动包装为 ResponseVo。
 * 已经返回 ResponseVo 的接口（如全局异常处理）不会重复包装。
 */
@RestControllerAdvice(basePackages = "com.health.management")
public class RestResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public RestResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> parameterType = returnType.getParameterType();
        // ResponseVo 已包装过；ResponseEntity、Resource、byte[] 等特殊返回值不处理
        return !ResponseVo.class.isAssignableFrom(parameterType)
                && !ResponseEntity.class.isAssignableFrom(parameterType)
                && !Resource.class.isAssignableFrom(parameterType)
                && parameterType != byte[].class;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ResponseVo) {
            return body;
        }

        ResponseVo<Object> result = ResponseVo.success(body);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 返回 String 时使用的是 StringHttpMessageConverter，不能直接返回对象，否则会类型转换失败
        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("统一响应序列化失败", e);
            }
        }
        return result;
    }
}

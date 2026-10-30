package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 发送关怀留言时选择学生接收人。
 */
@Data
public class MessageReceiverQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 20;

    /** 姓名、学号或登录账号模糊搜索 */
    @Size(max = 50, message = "搜索关键词长度不能超过50个字符")
    private String keyword;
}

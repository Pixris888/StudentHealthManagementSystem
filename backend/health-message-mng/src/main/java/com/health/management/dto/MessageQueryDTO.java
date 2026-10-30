package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 消息分页查询条件。
 */
@Data
public class MessageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 10;

    @Min(value = 1, message = "消息类型只能是1-5")
    @Max(value = 5, message = "消息类型只能是1-5")
    private Integer messageType;

    /** 0-未读，1-已读；不传表示全部 */
    @Min(value = 0, message = "已读状态只能是0-未读、1-已读")
    @Max(value = 1, message = "已读状态只能是0-未读、1-已读")
    private Integer readStatus;
}

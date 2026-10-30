package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 校医/超管发送公告或关怀留言。
 * 1、2 为系统预警类型，不允许手工发送。
 */
@Data
public class MessageSendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择消息类型")
    @Min(value = 3, message = "手工消息类型只能是3-公告、4-关怀留言、5-其他")
    @Max(value = 5, message = "手工消息类型只能是3-公告、4-关怀留言、5-其他")
    private Integer messageType;

    @NotBlank(message = "消息标题不能为空")
    @Size(max = 200, message = "消息标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容长度不能超过5000个字符")
    private String content;

    /** 个人消息接收人；公告必须为空 */
    @Size(max = 100, message = "单次最多发送给100名学生")
    private List<Long> receiverIds;
}

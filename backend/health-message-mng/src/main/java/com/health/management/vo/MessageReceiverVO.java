package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 关怀留言接收人展示对象（只返回选择学生所需的最小信息）。
 */
@Data
public class MessageReceiverVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String realName;
    private String studentNo;
    private String deptName;
}

package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 未读消息数。
 */
@Data
public class UnreadCountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long count;

    public UnreadCountVO() {
    }

    public UnreadCountVO(Long count) {
        this.count = count == null ? 0L : count;
    }
}

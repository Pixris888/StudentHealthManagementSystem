package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 校医/超级管理员新增或编辑科普文章。
 * 状态、审核信息、浏览量与点赞数均由服务端维护。
 */
@Data
public class HealthArticleSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择文章分类")
    private Long categoryId;

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题长度不能超过200个字符")
    private String title;

    @Size(max = 255, message = "封面地址长度不能超过255个字符")
    private String coverUrl;

    @Size(max = 50, message = "作者长度不能超过50个字符")
    private String author;

    @Size(max = 500, message = "文章摘要长度不能超过500个字符")
    private String summary;

    @NotBlank(message = "文章正文不能为空")
    private String content;

    @Size(max = 100, message = "文章来源长度不能超过100个字符")
    private String source;

    @Size(max = 255, message = "附件地址长度不能超过255个字符")
    private String docUrl;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}

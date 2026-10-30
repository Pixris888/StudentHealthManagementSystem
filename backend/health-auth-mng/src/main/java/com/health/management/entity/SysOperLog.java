package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.health.management.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("title")
    private String title; // 模块标题
    @TableField("business_type")
    private Integer businessType; // 业务类型 0-其他 1-新增 2-修改 3-删除 4-授权 5-导出 6-导入 7-强退 8-重置密码
    @TableField("method")
    private String method; // 方法名称
    @TableField("request_method")
    private String requestMethod; // 请求方式
    @TableField("operator_type")
    private Integer operatorType; // 操作类别 0-其他 1-后台用户 2-手机端
    @TableField("oper_user_id")
    private Long operUserId; // 操作人员ID
    @TableField("oper_user_name")
    private String operUserName; // 操作人员账号
    @TableField("oper_ip")
    private String operIp; // 操作IP
    @TableField("oper_location")
    private String operLocation; // 操作地点
    @TableField("oper_param")
    private String operParam; // 请求参数
    @TableField("json_result")
    private String jsonResult; // 返回参数
    @TableField("status")
    private Integer status; // 操作状态 0-成功 1-失败
    @TableField("error_msg")
    private String errorMsg; // 错误消息
    @TableField("oper_time")
    private LocalDateTime operTime; // 操作时间
}

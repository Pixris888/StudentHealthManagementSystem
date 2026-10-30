-- 大学生健康管理平台 · 完整表结构（23 张表）
-- 适用于 MySQL 5.7 / 8.0
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `ai_consultation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_consultation` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID(逻辑关联 sys_user.id)',
  `session_id` varchar(64) DEFAULT NULL COMMENT '会话ID(多轮问答分组)',
  `question` varchar(1000) NOT NULL COMMENT '用户提问',
  `answer` text COMMENT 'AI回答内容',
  `answer_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '回答状态 0-处理中 1-成功 2-失败 3-降级',
  `emergency` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否紧急问题 0-否 1-是',
  `error_msg` varchar(500) DEFAULT '' COMMENT '失败/降级原因',
  `knowledge_source` varchar(255) DEFAULT '' COMMENT '知识来源(权威知识库)',
  `retrieved_ids` varchar(255) DEFAULT '' COMMENT '命中的知识素材ID(逗号分隔)',
  `model_name` varchar(50) DEFAULT '' COMMENT '使用模型',
  `provider` varchar(20) DEFAULT '' COMMENT '模型提供方 bailian/ollama/rule/mock',
  `prompt_tokens` int(11) DEFAULT '0' COMMENT '输入token数',
  `completion_tokens` int(11) DEFAULT '0' COMMENT '输出token数',
  `latency_ms` int(11) DEFAULT '0' COMMENT '响应耗时(毫秒)',
  `feedback` tinyint(4) NOT NULL DEFAULT '0' COMMENT '反馈 0-未评价 1-有帮助 2-没帮助',
  `feedback_remark` varchar(255) DEFAULT '' COMMENT '反馈备注',
  `consult_time` datetime DEFAULT NULL COMMENT '咨询时间',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0' COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_consult_time` (`consult_time`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_user_time` (`user_id`,`consult_time`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_answer_status` (`answer_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI健康科普问答记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_health_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ai_health_plan` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID(逻辑关联 sys_user.id)',
  `plan_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '计划类型 1-作息计划 2-运动计划 3-综合计划',
  `plan_title` varchar(100) DEFAULT '' COMMENT '计划标题',
  `plan_content` text COMMENT '计划内容',
  `plan_data` longtext COMMENT '结构化计划数据(JSON，可选)',
  `model_name` varchar(50) DEFAULT '' COMMENT '生成模型',
  `provider` varchar(20) DEFAULT '' COMMENT '模型提供方 bailian/ollama/rule/mock',
  `base_checkin_date` date DEFAULT NULL COMMENT '依据打卡日期',
  `start_date` date DEFAULT NULL COMMENT '计划开始日期',
  `end_date` date DEFAULT NULL COMMENT '计划结束日期',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态 0-未采纳 1-已采纳',
  `adopted_time` datetime DEFAULT NULL COMMENT '采纳时间',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_plan_type` (`plan_type`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='AI个性化健康计划表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_article` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `category_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '分类ID(逻辑关联 health_article_category.id)',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `cover_url` varchar(255) DEFAULT '' COMMENT '封面图',
  `author` varchar(50) DEFAULT '' COMMENT '作者/发布人',
  `summary` varchar(500) DEFAULT '' COMMENT '摘要',
  `content` longtext COMMENT '正文内容',
  `source` varchar(100) DEFAULT '' COMMENT '来源',
  `doc_url` varchar(255) DEFAULT '' COMMENT '官方文档附件地址',
  `view_count` int(11) NOT NULL DEFAULT '0' COMMENT '浏览量',
  `like_count` int(11) NOT NULL DEFAULT '0' COMMENT '点赞数',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态 0-草稿 1-已发布 2-已下架',
  `audit_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '审核状态 0-待审核 1-通过 2-不通过(超级管理员审核)',
  `audit_by` bigint(20) unsigned DEFAULT '0' COMMENT '审核人(逻辑关联 sys_user.id)',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) DEFAULT '' COMMENT '审核意见',
  `publish_by` bigint(20) unsigned DEFAULT '0' COMMENT '发布人',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='健康科普文章表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_article_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_article_category` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `parent_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '父分类ID',
  `order_num` int(11) NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-正常',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='健康科普文章分类表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_checkin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_checkin` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID(逻辑关联 sys_user.id)',
  `student_no` varchar(30) DEFAULT '' COMMENT '学号',
  `checkin_date` date NOT NULL COMMENT '打卡日期',
  `sleep_time` time DEFAULT NULL COMMENT '入睡时间',
  `wake_time` time DEFAULT NULL COMMENT '起床时间',
  `sleep_duration` decimal(4,1) DEFAULT NULL COMMENT '睡眠时长(小时)',
  `stay_up_late` tinyint(4) DEFAULT NULL COMMENT '是否熬夜 0-否 1-是 NULL-未填写睡眠',
  `sleep_source` tinyint(4) NOT NULL DEFAULT '0' COMMENT '睡眠来源 0-未填写 1-计时打卡 2-手动填写',
  `exercise_type` varchar(50) DEFAULT '' COMMENT '运动类型',
  `exercise_duration` int(11) DEFAULT NULL COMMENT '运动时长(分钟)',
  `exercise_intensity` tinyint(4) DEFAULT NULL COMMENT '运动强度 1-低 2-中 3-高',
  `exercise_sufficient` tinyint(4) DEFAULT NULL COMMENT '运动是否达标 0-否 1-是 NULL-未填写运动',
  `exercise_source` tinyint(4) NOT NULL DEFAULT '0' COMMENT '运动来源 0-未填写 1-计时打卡 2-手动填写',
  `diet_breakfast` tinyint(4) DEFAULT NULL COMMENT '是否吃早餐 0-否 1-是 NULL-未填写饮食',
  `diet_balance` tinyint(4) DEFAULT NULL COMMENT '饮食均衡度 1-不均衡 2-一般 3-均衡',
  `water_intake` int(11) DEFAULT NULL COMMENT '饮水量(ml)',
  `emotion_type` tinyint(4) DEFAULT NULL COMMENT '情绪类型 1-愉悦 2-平静 3-焦虑 4-低落 5-愤怒',
  `mood_score` tinyint(4) DEFAULT NULL COMMENT '心情评分 1-5',
  `content` varchar(500) DEFAULT '' COMMENT '打卡备注',
  `data_quality` tinyint(4) NOT NULL DEFAULT '0' COMMENT '数据质量 0-正常 1-异常待复核',
  `abnormal_reason` varchar(500) DEFAULT '' COMMENT '异常原因',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`checkin_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_checkin_date` (`checkin_date`),
  KEY `idx_stay_up_late` (`stay_up_late`),
  KEY `idx_exercise_sufficient` (`exercise_sufficient`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_data_quality` (`data_quality`),
  KEY `idx_date_quality` (`checkin_date`,`data_quality`)
) ENGINE=InnoDB AUTO_INCREMENT=667 DEFAULT CHARSET=utf8mb4 COMMENT='每日健康打卡记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_checkin_photo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_checkin_photo` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `checkin_id` bigint(20) unsigned NOT NULL COMMENT '打卡记录ID(逻辑关联 health_checkin.id)',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID(逻辑关联 sys_user.id)',
  `photo_url` varchar(255) DEFAULT '' COMMENT '照片 objectKey（本地路径或 OSS objectKey）',
  `photo_type` tinyint(4) DEFAULT '1' COMMENT '照片类型 1-睡眠 2-运动 3-饮食 4-其他',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_checkin_id` (`checkin_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COMMENT='健康打卡生活照片表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_checkin_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_checkin_session` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID(逻辑关联 sys_user.id)',
  `student_no` varchar(30) DEFAULT '' COMMENT '学号',
  `checkin_date` date DEFAULT NULL COMMENT '归属日期：睡眠=起床日期，运动=开始日期',
  `session_type` tinyint(4) NOT NULL COMMENT '会话类型 1-睡眠 2-运动',
  `start_time` datetime NOT NULL COMMENT '服务端开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '服务端结束时间',
  `duration_minutes` int(11) DEFAULT NULL COMMENT '时长(分钟)',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态 0-进行中 1-已结束 2-已过期',
  `exercise_type` varchar(50) DEFAULT NULL COMMENT '运动类型',
  `exercise_intensity` tinyint(4) DEFAULT NULL COMMENT '运动强度 1-低 2-中 3-高',
  `data_quality` tinyint(4) NOT NULL DEFAULT '0' COMMENT '数据质量 0-正常 1-异常待复核',
  `abnormal_reason` varchar(500) DEFAULT '' COMMENT '异常原因',
  `ongoing_flag` tinyint(4) GENERATED ALWAYS AS ((case when (`status` = 0) then 1 else NULL end)) STORED COMMENT '进行中标记，用于唯一约束',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_type_ongoing` (`user_id`,`session_type`,`ongoing_flag`),
  KEY `idx_user_date` (`user_id`,`checkin_date`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_checkin_date` (`checkin_date`),
  KEY `idx_data_quality` (`data_quality`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COMMENT='打卡计时会话表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_knowledge_material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_knowledge_material` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `category_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '分类ID',
  `title` varchar(200) NOT NULL COMMENT '素材标题',
  `content` text COMMENT '素材内容',
  `keyword` varchar(255) DEFAULT '' COMMENT '关键词(逗号分隔)',
  `source` varchar(100) DEFAULT '' COMMENT '来源',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-启用',
  `audit_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '审核状态 0-待审核 1-通过 2-不通过',
  `audit_by` bigint(20) unsigned DEFAULT '0' COMMENT '审核人(逻辑关联 sys_user.id)',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) DEFAULT '' COMMENT '审核意见',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='AI问答知识库素材表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_physical_test`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_physical_test` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID(逻辑关联 sys_user.id)',
  `student_no` varchar(30) DEFAULT '' COMMENT '学号',
  `test_year` int(11) NOT NULL COMMENT '测试年份',
  `test_term` varchar(20) DEFAULT '' COMMENT '学期 如 上学期/下学期',
  `height` decimal(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `bmi` decimal(5,2) DEFAULT NULL COMMENT 'BMI',
  `vital_capacity` int(11) DEFAULT NULL COMMENT '肺活量(ml)',
  `run_50m` decimal(5,2) DEFAULT NULL COMMENT '50米跑(秒)',
  `sit_and_reach` decimal(5,2) DEFAULT NULL COMMENT '坐位体前屈(cm)',
  `standing_jump` decimal(5,2) DEFAULT NULL COMMENT '立定跳远(cm)',
  `endurance_run` decimal(6,2) DEFAULT NULL COMMENT '耐力跑 1000/800米(秒)',
  `strength_item` int(11) DEFAULT NULL COMMENT '力量项目 引体向上/仰卧起坐(个)',
  `total_score` decimal(6,2) DEFAULT NULL COMMENT '总分',
  `grade_level` varchar(10) DEFAULT '' COMMENT '等级 优秀/良好/及格/不及格',
  `test_date` date DEFAULT NULL COMMENT '测试日期',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_test` (`user_id`,`test_year`,`test_term`),
  KEY `idx_student_no` (`student_no`),
  KEY `idx_test_year` (`test_year`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COMMENT='历年体测成绩表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_profile` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID(逻辑关联 sys_user.id)',
  `student_no` varchar(30) DEFAULT '' COMMENT '学号',
  `real_name` varchar(50) DEFAULT '' COMMENT '姓名',
  `gender` tinyint(4) NOT NULL DEFAULT '0' COMMENT '性别 0-未知 1-男 2-女',
  `age` tinyint(4) DEFAULT NULL COMMENT '年龄',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `blood_type` varchar(10) DEFAULT '' COMMENT '血型',
  `height` decimal(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `bmi` decimal(5,2) DEFAULT NULL COMMENT 'BMI指数',
  `college` varchar(100) DEFAULT '' COMMENT '学院',
  `major` varchar(100) DEFAULT '' COMMENT '专业',
  `grade` varchar(20) DEFAULT '' COMMENT '年级',
  `class_name` varchar(50) DEFAULT '' COMMENT '班级',
  `chronic_disease` varchar(500) DEFAULT '' COMMENT '慢性病史',
  `allergy_history` varchar(500) DEFAULT '' COMMENT '过敏史',
  `emergency_contact` varchar(50) DEFAULT '' COMMENT '紧急联系人',
  `emergency_phone` varchar(20) DEFAULT '' COMMENT '紧急联系电话',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_student_no` (`student_no`),
  KEY `idx_college` (`college`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COMMENT='学生健康档案基础信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `health_report` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID(逻辑关联 sys_user.id)',
  `student_no` varchar(30) DEFAULT '' COMMENT '学号',
  `report_year` int(11) NOT NULL COMMENT '报告年份',
  `report_month` int(11) NOT NULL COMMENT '报告月份',
  `report_title` varchar(100) DEFAULT '' COMMENT '报告标题',
  `checkin_days` int(11) DEFAULT '0' COMMENT '打卡天数',
  `avg_sleep_duration` decimal(4,1) DEFAULT NULL COMMENT '平均睡眠时长(小时)',
  `stay_up_late_count` int(11) DEFAULT '0' COMMENT '熬夜次数',
  `avg_exercise_duration` int(11) DEFAULT '0' COMMENT '平均运动时长(分钟)',
  `exercise_insufficient_count` int(11) DEFAULT '0' COMMENT '运动不足次数',
  `avg_mood_score` decimal(3,1) DEFAULT NULL COMMENT '平均心情评分',
  `report_content` text COMMENT '报告内容/总结',
  `report_data` longtext COMMENT '报告结构化指标JSON快照',
  `file_url` varchar(255) DEFAULT '' COMMENT '报告文件(PDF等)地址',
  `generate_time` datetime DEFAULT NULL COMMENT '生成时间',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_month` (`user_id`,`report_year`,`report_month`),
  KEY `idx_student_no` (`student_no`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_report_month` (`report_year`,`report_month`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='个人月度健康统计报告表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `message_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `message_notification` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `receiver_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '接收人ID(0 表示全体广播)',
  `message_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '消息类型 1-熬夜预警 2-运动不足预警 3-校园公告 4-关怀留言 5-其他',
  `title` varchar(200) NOT NULL COMMENT '消息标题',
  `content` text COMMENT '消息内容',
  `sender_id` bigint(20) unsigned DEFAULT '0' COMMENT '发送人ID(0=系统自动推送)',
  `send_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '发送方式 1-个人 2-全体 3-按条件',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `dedup_key` varchar(128) DEFAULT NULL COMMENT '幂等键：warning:{type}:{userId}:{date}',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dedup_key` (`dedup_key`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_message_type` (`message_type`),
  KEY `idx_send_type` (`send_type`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_receiver_publish` (`receiver_id`,`publish_time`),
  KEY `idx_sender_publish` (`sender_id`,`publish_time`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `message_read`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `message_read` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `message_id` bigint(20) unsigned NOT NULL COMMENT '消息ID(逻辑关联 message_notification.id)',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID(逻辑关联 sys_user.id)',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_user` (`message_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COMMENT='消息已读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_backup_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_backup_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) NOT NULL COMMENT '备份文件名',
  `file_path` varchar(500) NOT NULL DEFAULT '' COMMENT '备份文件绝对路径',
  `file_size` bigint(20) NOT NULL DEFAULT '0' COMMENT '文件大小(字节)',
  `table_count` int(11) NOT NULL DEFAULT '0' COMMENT '备份表数量',
  `row_count` bigint(20) NOT NULL DEFAULT '0' COMMENT '备份总行数',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 0-失败 1-成功',
  `error_msg` varchar(1000) DEFAULT '' COMMENT '失败原因',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='数据库备份记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `config_name` varchar(100) NOT NULL COMMENT '参数名称',
  `config_key` varchar(100) NOT NULL COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数值',
  `config_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '参数类型 0-系统内置 1-自定义',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COMMENT='系统参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dict_data` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签',
  `dict_value` varchar(100) NOT NULL COMMENT '字典键值',
  `dict_sort` int(11) NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-正常',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dict_type` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `dict_name` varchar(100) NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型(唯一)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-正常',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_menu` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '父菜单ID',
  `menu_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '菜单类型 1-目录 2-菜单 3-按钮',
  `order_num` int(11) NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(100) DEFAULT '' COMMENT '路由地址',
  `component` varchar(100) DEFAULT '' COMMENT '组件路径',
  `perms` varchar(100) DEFAULT NULL,
  `icon` varchar(50) DEFAULT '' COMMENT '菜单图标',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-正常',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perms` (`perms`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_menu_type` (`menu_type`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=280 DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_oper_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(50) DEFAULT '' COMMENT '模块标题',
  `business_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '业务类型 0-其他 1-新增 2-修改 3-删除 4-授权 5-导出 6-导入 7-强退 8-重置密码',
  `method` varchar(100) DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `operator_type` tinyint(4) DEFAULT '0' COMMENT '操作类别 0-其他 1-后台用户 2-手机端',
  `oper_user_id` bigint(20) unsigned DEFAULT '0' COMMENT '操作人员ID',
  `oper_user_name` varchar(50) DEFAULT '' COMMENT '操作人员账号',
  `oper_ip` varchar(50) DEFAULT '' COMMENT '操作IP',
  `oper_location` varchar(255) DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) DEFAULT '' COMMENT '返回参数',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '操作状态 0-成功 1-失败',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_oper_user_id` (`oper_user_id`),
  KEY `idx_business_type` (`business_type`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=319 DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码(唯一)',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `user_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '用户类型 1-学生 2-校医科普管理员 3-超级管理员',
  `role_sort` int(11) NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `data_scope` tinyint(4) NOT NULL DEFAULT '3' COMMENT '数据权限范围 1-全部 2-自定义 3-仅本人 4-部门',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-正常',
  `create_by` bigint(20) unsigned DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint(20) unsigned DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) unsigned NOT NULL COMMENT '角色ID(逻辑关联 sys_role.id)',
  `menu_id` bigint(20) unsigned NOT NULL COMMENT '菜单ID(逻辑关联 sys_menu.id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=113 DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL DEFAULT '' COMMENT '登录密码(BCrypt加密)',
  `real_name` varchar(50) NOT NULL DEFAULT '' COMMENT '真实姓名',
  `nick_name` varchar(50) DEFAULT '' COMMENT '昵称',
  `user_type` tinyint(4) NOT NULL DEFAULT '3' COMMENT '用户类型 1-学生 2-校医科普管理员 3-超级管理员',
  `student_no` varchar(30) DEFAULT NULL,
  `gender` tinyint(4) NOT NULL DEFAULT '0' COMMENT '性别 0-未知 1-男 2-女',
  `avatar` varchar(255) DEFAULT '' COMMENT '头像地址',
  `phone` varchar(20) DEFAULT '' COMMENT '联系电话',
  `email` varchar(100) DEFAULT '' COMMENT '邮箱',
  `dept_name` varchar(100) DEFAULT '' COMMENT '所属院系/部门',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '账号状态 0-禁用 1-启用',
  `token_version` int(11) NOT NULL DEFAULT '0' COMMENT 'token版本号，改密/禁用/改角色后+1',
  `login_ip` varchar(50) DEFAULT '' COMMENT '最后登录IP',
  `login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` bigint(20) unsigned DEFAULT '0' COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) unsigned DEFAULT '0' COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删 1-已删',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_student_no` (`student_no`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_role` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID(逻辑关联 sys_user.id)',
  `role_id` bigint(20) unsigned NOT NULL COMMENT '角色ID(逻辑关联 sys_role.id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

SET FOREIGN_KEY_CHECKS=1;

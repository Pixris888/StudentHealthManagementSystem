-- 大学生健康管理平台 · 最小演示数据
-- 仅包含内置角色、菜单、字典、系统参数和 1 个超级管理员。
-- 演示账号：admin / 123456（BCrypt 哈希），部署后请立即修改密码。
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

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `user_type`, `role_sort`, `data_scope`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `remark`) VALUES (1,'SUPER_ADMIN','超级系统管理员',3,1,1,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:41:29',0,'系统最高权限，可管理全站用户与审核内容'),(2,'DOCTOR_ADMIN','校医科普管理员',2,2,2,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:41:29',0,'维护健康知识库，发布通知与关怀留言'),(3,'STUDENT','学生用户',1,3,3,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,'普通学生，维护个人健康数据');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `order_num`, `path`, `component`, `perms`, `icon`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `remark`) VALUES (1,'系统管理',0,1,1,'system','Layout',NULL,'system',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:41:30',0,'系统管理目录'),(2,'健康管理',0,1,2,'health','Layout',NULL,'health',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:41:30',0,'健康管理目录'),(11,'用户管理',1,2,1,'user','system/user/index','system:user:list','user',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(12,'角色管理',1,2,2,'role','system/role/index','system:role:list','peoples',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(13,'菜单管理',1,2,3,'menu','system/menu/index','system:menu:list','tree-table',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(14,'参数设置',1,2,4,'config','system/config/index','system:config:list','edit',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(15,'字典管理',1,2,5,'dict','system/dict/index','system:dict:list','dict',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(16,'操作日志',1,2,6,'operlog','monitor/operlog/index','system:log:list','log',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(21,'健康档案',2,2,1,'profile','health/profile/index','health:profile:list','profile',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(22,'体测成绩',2,2,2,'physical','health/physical/index','health:physical:list','tice',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(23,'健康打卡',2,2,3,'checkin','health/checkin/index','health:checkin:list','clock',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(24,'健康知识库',2,2,4,'article','health/article/index','health:article:list','article',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(25,'消息通知',2,2,5,'message','health/message/index','message:list','message',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(26,'统计报表',2,2,6,'report','health/report/index','report:list','report',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(27,'AI健康助手',2,2,7,'ai','health/ai/index','ai:consult:list','ai',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(111,'用户新增',11,3,1,'','','system:user:add','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(112,'用户修改',11,3,2,'','','system:user:edit','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(113,'用户删除',11,3,3,'','','system:user:remove','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(114,'重置密码',11,3,4,'','','system:user:resetpwd','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(115,'账号启停',11,3,5,'','','system:user:status','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(121,'角色新增',12,3,1,'','','system:role:add','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(122,'角色修改',12,3,2,'','','system:role:edit','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(123,'角色删除',12,3,3,'','','system:role:remove','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(124,'分配权限',12,3,4,'','','system:role:auth','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(131,'菜单新增',13,3,1,'','','system:menu:add','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(132,'菜单修改',13,3,2,'','','system:menu:edit','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(133,'菜单删除',13,3,3,'','','system:menu:remove','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(231,'打卡提交',23,3,1,'','','health:checkin:add','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(241,'文章新增',24,3,1,'','','health:article:add','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(242,'文章修改',24,3,2,'','','health:article:edit','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(243,'文章删除',24,3,3,'','','health:article:remove','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(244,'内容审核',24,3,4,'','','health:article:audit','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(251,'发送通知',25,3,1,'','','message:send','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(261,'报表导出',26,3,1,'','','report:export','',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(262,'分配角色',11,3,6,NULL,NULL,'system:user:assignRole',NULL,1,0,'2026-10-17 13:41:30',0,'2026-10-17 13:41:30',0,'用户管理-分配角色'),(263,'AI问答提问',27,3,1,'','','ai:consult:ask','',1,0,'2026-10-26 11:07:02',0,'2026-10-26 11:07:02',0,'AI健康助手'),(264,'生成健康计划',27,3,2,'','','ai:plan:generate','',1,0,'2026-10-26 11:07:02',0,'2026-10-26 11:07:02',0,'AI健康助手'),(265,'AI问答管理',27,3,3,'','','ai:consult:manage','',1,0,'2026-10-26 11:07:02',0,'2026-10-26 11:07:02',0,'AI健康助手'),(266,'AI使用统计',27,3,4,'','','ai:consult:stats','',1,0,'2026-10-26 11:07:02',0,'2026-10-26 11:07:02',0,'AI健康助手'),(267,'测试菜单',0,1,1,'test','test',NULL,'profile',1,1,'2026-10-26 15:38:13',1,'2026-10-26 16:49:32',1,'测试菜单的新增'),(268,'参数新增',14,3,1,'','','system:config:add','',1,0,'2026-10-26 15:39:29',0,'2026-10-26 15:39:29',0,'系统参数'),(269,'参数修改',14,3,2,'','','system:config:edit','',1,0,'2026-10-26 15:39:29',0,'2026-10-26 15:39:29',0,'系统参数'),(270,'参数删除',14,3,3,'','','system:config:remove','',1,0,'2026-10-26 15:39:29',0,'2026-10-26 15:39:29',0,'系统参数'),(271,'字典新增',15,3,1,'','','system:dict:add','',1,0,'2026-10-26 15:39:29',0,'2026-10-26 15:39:29',0,'字典管理'),(272,'字典修改',15,3,2,'','','system:dict:edit','',1,0,'2026-10-26 15:39:29',0,'2026-10-26 15:39:29',0,'字典管理'),(273,'字典删除',15,3,3,'','','system:dict:remove','',1,0,'2026-10-26 15:39:29',0,'2026-10-26 15:39:29',0,'字典管理'),(274,'数据备份',1,2,7,'backup','system/backup/index','system:backup:list','',1,0,'2026-10-26 15:39:29',0,'2026-10-26 15:39:29',0,'数据库备份'),(275,'创建备份',274,3,1,'','','system:backup:create','',1,0,'2026-10-26 15:39:29',0,'2026-10-26 15:39:29',0,'数据备份'),(276,'下载备份',274,3,2,'','','system:backup:download','',1,0,'2026-10-26 15:39:29',0,'2026-10-26 15:39:29',0,'数据备份'),(277,'删除备份',274,3,3,'','','system:backup:remove','',1,0,'2026-10-26 15:39:29',0,'2026-10-26 15:39:29',0,'数据备份'),(278,'测试菜单',267,2,1,'test/testone','test/testone/index','test:testone:index','monitor',1,1,'2026-10-26 15:41:47',1,'2026-10-26 16:49:30',1,''),(279,'新增菜单',1,2,1,'/menu/add','menu/add/index','system:add','dict',1,1,'2026-10-26 16:52:32',1,'2026-10-29 09:25:26',1,'');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `create_time`, `deleted`) VALUES (1,1,1,'2026-10-17 13:34:24',0),(2,1,2,'2026-10-17 13:34:24',0),(3,1,11,'2026-10-17 13:34:24',0),(4,1,12,'2026-10-17 13:34:24',0),(5,1,13,'2026-10-17 13:34:24',0),(6,1,14,'2026-10-17 13:34:24',0),(7,1,15,'2026-10-17 13:34:24',0),(8,1,16,'2026-10-17 13:34:24',0),(9,1,21,'2026-10-17 13:34:24',0),(10,1,22,'2026-10-17 13:34:24',0),(11,1,23,'2026-10-17 13:34:24',0),(12,1,24,'2026-10-17 13:34:24',0),(13,1,25,'2026-10-17 13:34:24',0),(14,1,26,'2026-10-17 13:34:24',0),(15,1,27,'2026-10-17 13:34:24',0),(16,1,111,'2026-10-17 13:34:24',0),(17,1,112,'2026-10-17 13:34:24',0),(18,1,113,'2026-10-17 13:34:24',0),(19,1,114,'2026-10-17 13:34:24',0),(20,1,115,'2026-10-17 13:34:24',0),(21,1,121,'2026-10-17 13:34:24',0),(22,1,122,'2026-10-17 13:34:24',0),(23,1,123,'2026-10-17 13:34:24',0),(24,1,124,'2026-10-17 13:34:24',0),(25,1,131,'2026-10-17 13:34:24',0),(26,1,132,'2026-10-17 13:34:24',0),(27,1,133,'2026-10-17 13:34:24',0),(28,1,231,'2026-10-17 13:34:24',0),(29,1,241,'2026-10-17 13:34:24',0),(30,1,242,'2026-10-17 13:34:24',0),(31,1,243,'2026-10-17 13:34:24',0),(32,1,244,'2026-10-17 13:34:24',0),(33,1,251,'2026-10-17 13:34:24',0),(34,1,261,'2026-10-17 13:34:24',0),(64,2,2,'2026-10-17 13:34:24',0),(65,2,21,'2026-10-17 13:34:24',0),(66,2,22,'2026-10-17 13:34:24',0),(67,2,23,'2026-10-17 13:34:24',0),(68,2,24,'2026-10-17 13:34:24',0),(69,2,25,'2026-10-17 13:34:24',0),(70,2,26,'2026-10-17 13:34:24',0),(71,2,27,'2026-10-17 13:34:24',0),(72,2,231,'2026-10-17 13:34:24',0),(73,2,241,'2026-10-17 13:34:24',0),(74,2,242,'2026-10-17 13:34:24',0),(75,2,243,'2026-10-17 13:34:24',0),(76,2,251,'2026-10-17 13:34:24',0),(77,2,261,'2026-10-17 13:34:24',0),(79,3,2,'2026-10-17 13:34:24',0),(80,3,21,'2026-10-17 13:34:24',0),(81,3,22,'2026-10-17 13:34:24',0),(82,3,23,'2026-10-17 13:34:24',0),(83,3,24,'2026-10-17 13:34:24',0),(84,3,25,'2026-10-17 13:34:24',0),(85,3,26,'2026-10-17 13:34:24',0),(86,3,27,'2026-10-17 13:34:24',0),(87,3,231,'2026-10-17 13:34:24',0),(94,1,262,'2026-10-17 13:41:30',0),(95,3,263,'2026-10-26 11:07:02',0),(96,3,264,'2026-10-26 11:07:02',0),(97,1,265,'2026-10-26 11:07:02',0),(98,2,265,'2026-10-26 11:07:02',0),(100,1,266,'2026-10-26 11:07:02',0),(101,2,266,'2026-10-26 11:07:02',0),(103,1,268,'2026-10-26 15:39:29',0),(104,1,269,'2026-10-26 15:39:29',0),(105,1,270,'2026-10-26 15:39:29',0),(106,1,271,'2026-10-26 15:39:29',0),(107,1,272,'2026-10-26 15:39:29',0),(108,1,273,'2026-10-26 15:39:29',0),(109,1,274,'2026-10-26 15:39:29',0),(110,1,275,'2026-10-26 15:39:29',0),(111,1,276,'2026-10-26 15:39:29',0),(112,1,277,'2026-10-26 15:39:29',0);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `remark`) VALUES (1,'性别','sys_gender',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,'用户性别'),(2,'情绪类型','health_emotion',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,'打卡情绪'),(3,'消息类型','msg_type',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,'消息通知类型'),(4,'文章状态','article_status',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,'科普文章状态'),(5,'运动强度','exercise_intensity',1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,'打卡运动强度'),(6,'test dict','test_dict',1,1,'2026-10-26 15:43:19',1,'2026-10-26 15:43:20',1,'');
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `remark`) VALUES (1,'sys_gender','未知','0',0,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(2,'sys_gender','男','1',1,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(3,'sys_gender','女','2',2,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(4,'health_emotion','愉悦','1',1,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(5,'health_emotion','平静','2',2,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(6,'health_emotion','焦虑','3',3,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(7,'health_emotion','低落','4',4,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(8,'health_emotion','愤怒','5',5,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(9,'msg_type','熬夜预警','1',1,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(10,'msg_type','运动不足预警','2',2,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(11,'msg_type','校园公告','3',3,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(12,'msg_type','关怀留言','4',4,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(13,'msg_type','其他','5',5,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(14,'article_status','草稿','0',0,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(15,'article_status','已发布','1',1,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(16,'article_status','已下架','2',2,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(17,'exercise_intensity','低','1',1,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(18,'exercise_intensity','中','2',2,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(19,'exercise_intensity','高','3',3,1,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,''),(20,'test_dict','Option A','A',1,1,1,'2026-10-26 15:43:19',1,'2026-10-26 15:43:20',1,'');
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


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

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `remark`) VALUES (1,'健康预警-熬夜判定时间','health.warn.stayuplate.time','23:30',0,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,'晚于该时间入睡计为熬夜'),(2,'健康预警-运动达标时长(分钟)','health.warn.exercise.minutes','30',0,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,'每日运动达到该时长计为达标'),(3,'系统名称','sys.name','大学生健康管理平台',0,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,'平台展示名称'),(4,'文件上传根路径','sys.upload.path','/uploads',0,0,'2026-10-17 13:34:24',0,'2026-10-17 13:34:24',0,'本地/云存储上传根路径');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


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

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `nick_name`, `user_type`, `student_no`, `gender`, `avatar`, `phone`, `email`, `dept_name`, `status`, `token_version`, `login_ip`, `login_time`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `remark`) VALUES (1,'admin','$2a$10$YnbNR6cXHsceZTz7MtvEV.RBXZ/F8U5bOvL03ZAXx.nJY7ks63MQC','系统管理员','',3,NULL,0,'','','','',1,0,'',NULL,0,'2026-10-17 13:34:24',1,'2026-10-17 13:34:24',0,'超级管理员初始账号');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


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

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_time`, `deleted`) VALUES (1,1,1,'2026-10-17 13:34:24',0);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

SET FOREIGN_KEY_CHECKS=1;

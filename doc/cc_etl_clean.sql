/*
 Navicat Premium Dump SQL

 Source Server         : 本地mysql
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : cc_etl

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 07/03/2026 11:20:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for job_approval_pending
-- ----------------------------
DROP TABLE IF EXISTS `job_approval_pending`;
CREATE TABLE `job_approval_pending` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_log_id` bigint NOT NULL COMMENT '任务日志ID',
  `job_id` bigint NOT NULL COMMENT '任务组ID',
  `node_id` bigint NOT NULL COMMENT '节点ID',
  `batch_id` varchar(128) DEFAULT NULL COMMENT '执行批次ID',
  `approver_user_ids` varchar(500) DEFAULT NULL COMMENT '审批人用户ID JSON数组',
  `wait_deadline` datetime DEFAULT NULL COMMENT '等待审批截止时间',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_approval_status` (`status`),
  KEY `idx_approval_job_log` (`job_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_approval_pending
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for job_approval_satisfied
-- ----------------------------
DROP TABLE IF EXISTS `job_approval_satisfied`;
CREATE TABLE `job_approval_satisfied` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_parent_id` bigint NOT NULL COMMENT '任务组ID',
  `node_id` bigint NOT NULL COMMENT '节点ID(JobNode.id)',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_parent_node` (`job_parent_id`,`node_id`),
  KEY `idx_job_parent_id` (`job_parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务审批节点仅审批一次：记录(任务组,节点)已审批通过';

-- ----------------------------
-- Records of job_approval_satisfied
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for job_canvas_bookmark
-- ----------------------------
DROP TABLE IF EXISTS `job_canvas_bookmark`;
CREATE TABLE `job_canvas_bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_group_id` bigint NOT NULL COMMENT '任务组ID',
  `bookmark_name` varchar(255) NOT NULL COMMENT '书签名称',
  `bookmark_type` varchar(50) DEFAULT 'position' COMMENT '书签类型：position-位置书签，node-节点书签',
  `target_node_id` bigint DEFAULT NULL COMMENT '目标节点ID（节点书签专用）',
  `canvas_position_x` double DEFAULT NULL COMMENT '画布X坐标（位置书签专用）',
  `canvas_position_y` double DEFAULT NULL COMMENT '画布Y坐标（位置书签专用）',
  `zoom_level` double DEFAULT '1' COMMENT '缩放级别',
  `description` varchar(500) DEFAULT NULL COMMENT '书签描述',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_task_group_id` (`task_group_id`),
  KEY `idx_target_node_id` (`target_node_id`),
  KEY `idx_create_user_id` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='画布书签表';

-- ----------------------------
-- Records of job_canvas_bookmark
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for job_compose
-- ----------------------------
DROP TABLE IF EXISTS `job_compose`;
CREATE TABLE `job_compose` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_name` varchar(50) DEFAULT NULL COMMENT 'appName',
  `executor_address` varchar(100) DEFAULT NULL,
  `executor_server_address` varchar(100) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务组';

-- ----------------------------
-- Records of job_compose
-- ----------------------------
BEGIN;

COMMIT;

-- ----------------------------
-- Table structure for job_edge
-- ----------------------------
DROP TABLE IF EXISTS `job_edge`;
CREATE TABLE `job_edge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `from_node_id` bigint DEFAULT NULL COMMENT 'from节点',
  `end_node_id` bigint DEFAULT NULL COMMENT 'end节点',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `job_parent_id` bigint DEFAULT NULL,
  `points_list` varchar(1000) DEFAULT NULL,
  `properties` varchar(1000) DEFAULT NULL,
  `start_point` varchar(100) DEFAULT NULL,
  `end_point` varchar(1000) DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_job_edge_job_parent_id` (`job_parent_id`),
  KEY `idx_job_edge_from_node_id` (`from_node_id`),
  KEY `idx_job_edge_end_node_id` (`end_node_id`),
  KEY `idx_job_edge_parent_from` (`from_node_id`,`job_parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=26236 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务边';

-- ----------------------------
-- Records of job_edge
-- ----------------------------
BEGIN;

COMMIT;

-- ----------------------------
-- Table structure for job_group
-- ----------------------------
DROP TABLE IF EXISTS `job_group`;
CREATE TABLE `job_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_name` varchar(64) NOT NULL COMMENT '执行器AppName',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行器名称',
  `address_type` tinyint NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
  `address_list` text COMMENT '执行器地址列表，多地址逗号分隔',
  `update_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_group
-- ----------------------------
BEGIN;

COMMIT;

-- ----------------------------
-- Table structure for job_group_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `job_group_snapshot`;
CREATE TABLE `job_group_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_id` bigint NOT NULL COMMENT '任务组ID',
  `random_id` varchar(64) NOT NULL COMMENT '执行批次ID',
  `nodes_json` longtext COMMENT '节点快照JSON',
  `edges_json` longtext COMMENT '边快照JSON',
  `trigger_user_id` varchar(64) DEFAULT NULL COMMENT '触发用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_random` (`job_id`,`random_id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_random_id` (`random_id`)
) ENGINE=InnoDB AUTO_INCREMENT=623 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务组快照表';

-- ----------------------------
-- Records of job_group_snapshot
-- ----------------------------
BEGIN;

COMMIT;

-- ----------------------------
-- Table structure for job_info
-- ----------------------------
DROP TABLE IF EXISTS `job_info`;
CREATE TABLE `job_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_group` bigint NOT NULL COMMENT '执行器主键ID',
  `job_desc` varchar(255) NOT NULL,
  `author` varchar(64) DEFAULT NULL COMMENT '作者',
  `alarm_email` varchar(255) DEFAULT NULL COMMENT '报警邮件',
  `schedule_type` varchar(50) NOT NULL DEFAULT 'NONE' COMMENT '调度类型',
  `schedule_conf` varchar(128) DEFAULT NULL COMMENT '调度配置，值含义取决于调度类型',
  `misfire_strategy` varchar(50) NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略',
  `executor_route_strategy` varchar(50) DEFAULT NULL COMMENT '执行器路由策略',
  `fail_strategy` varchar(50) DEFAULT NULL COMMENT '执行失败策略（DO_NOTHING，JOB_FAIL）',
  `executor_handler` varchar(255) DEFAULT NULL COMMENT '执行器任务handler',
  `executor_param` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行器任务参数',
  `executor_block_strategy` varchar(50) DEFAULT NULL COMMENT '阻塞处理策略',
  `executor_timeout` int NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
  `executor_fail_retry_count` int NOT NULL DEFAULT '0' COMMENT '失败重试次数',
  `run_time` bigint DEFAULT '0' COMMENT '最近一次运行耗时（毫秒）',
  `glue_type` varchar(50) NOT NULL COMMENT 'GLUE类型',
  `glue_source` mediumtext COMMENT 'GLUE源代码',
  `glue_remark` varchar(128) DEFAULT NULL COMMENT 'GLUE备注',
  `glue_update_time` datetime DEFAULT NULL COMMENT 'GLUE更新时间',
  `child_job_id` varchar(255) DEFAULT NULL COMMENT '子任务ID，多个逗号分隔',
  `trigger_status` tinyint NOT NULL DEFAULT '0' COMMENT '调度状态：0-停止，1-运行',
  `trigger_one_status` tinyint NOT NULL DEFAULT '0' COMMENT '任务组一次运行调度状态：0-停止，1-运行',
  `trigger_last_time` bigint NOT NULL DEFAULT '0' COMMENT '上次调度时间',
  `trigger_next_time` bigint NOT NULL DEFAULT '0' COMMENT '下次调度时间',
  `job_type` int DEFAULT '0' COMMENT '任务类型：0-普通任务，2-任务组',
  `parent_id` bigint DEFAULT '0' COMMENT '父任务ID',
  `req_type` varchar(100) DEFAULT NULL COMMENT '请求类型（API任务专用）',
  `req_header` varchar(1000) DEFAULT NULL COMMENT '请求头（API任务专用）',
  `req_body` varchar(3000) DEFAULT NULL COMMENT '请求体（API任务专用）',
  `req_url` varchar(1000) DEFAULT NULL COMMENT '请求URL（API任务专用）',
  `node_flag` varchar(10) DEFAULT NULL COMMENT '节点标识：Y-是节点，N-不是节点',
  `jdbc_datasource_id` bigint DEFAULT NULL COMMENT 'JDBC数据源ID',
  `increment_type` tinyint DEFAULT '0' COMMENT '增量类型：0-全量，1-增量',
  `increment_content` varchar(2000) DEFAULT NULL COMMENT '增量字段配置（JSON格式）',
  `increment_param_template` varchar(500) DEFAULT NULL COMMENT '自定义增量参数模板，如 -DstartId=%s -DendId=%s，%s 按顺序替换为 increment_content 中的值',
  `pause_status` tinyint DEFAULT '0' COMMENT '暂停状态：0-运行，1-暂停',
  `job_part_id` int DEFAULT NULL COMMENT '任务分区ID',
  `trigger_user_id` bigint DEFAULT NULL COMMENT '触发用户ID',
  `approval_wait_minutes` int DEFAULT '1440' COMMENT '审批节点待办最长等待时间(分钟)，默认24小时，超时未审批则本批次失败',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_job_info_job_type` (`job_type`),
  KEY `idx_job_info_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=26021 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_info
-- ----------------------------
BEGIN;

COMMIT;

-- ----------------------------
-- Table structure for job_jdbc_datasource
-- ----------------------------
DROP TABLE IF EXISTS `job_jdbc_datasource`;
CREATE TABLE `job_jdbc_datasource` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `datasource_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源名称',
  `datasource_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'Default' COMMENT '数据源分组',
  `jdbc_username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `jdbc_password` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `jdbc_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'jdbc url',
  `jdbc_driver_class` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'jdbc驱动类',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0删除 1启用 2禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `comments` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `datasource` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `database_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `schema_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='jdbc数据源配置';

-- ----------------------------
-- Records of job_jdbc_datasource
-- ----------------------------
BEGIN;
INSERT INTO `job_jdbc_datasource` (`id`, `datasource_name`, `datasource_group`, `jdbc_username`, `jdbc_password`, `jdbc_url`, `jdbc_driver_class`, `status`, `create_time`, `update_time`, `comments`, `datasource`, `database_name`, `schema_name`, `is_deleted`) VALUES (19, '测试数据源', 'Default', 'root', 'root', 'jdbc:mysql://localhost:3306/yanhuo-test', 'com.mysql.cj.jdbc.Driver', 1, '2025-05-26 19:47:51', '2025-05-26 19:47:51', '测试数据源', 'MYSQL', 'yanhuo-test', NULL, 0);
INSERT INTO `job_jdbc_datasource` (`id`, `datasource_name`, `datasource_group`, `jdbc_username`, `jdbc_password`, `jdbc_url`, `jdbc_driver_class`, `status`, `create_time`, `update_time`, `comments`, `datasource`, `database_name`, `schema_name`, `is_deleted`) VALUES (20, 'cc_etl', 'Default', 'root', '123456', 'jdbc:mysql://localhost:3306/cc_etl', 'com.mysql.cj.jdbc.Driver', 1, '2025-08-23 13:50:17', '2025-08-23 13:50:17', 'cc_etl', 'MYSQL', 'cc_etl', NULL, 0);
INSERT INTO `job_jdbc_datasource` (`id`, `datasource_name`, `datasource_group`, `jdbc_username`, `jdbc_password`, `jdbc_url`, `jdbc_driver_class`, `status`, `create_time`, `update_time`, `comments`, `datasource`, `database_name`, `schema_name`, `is_deleted`) VALUES (21, 'test1', 'Default', 'root', '123456', 'jdbc:mysql://127.0.0.1:3306/test1', 'com.mysql.cj.jdbc.Driver', 1, '2026-02-10 18:12:15', '2026-02-10 18:12:15', '备注', 'MYSQL', 'test1', NULL, 0);
INSERT INTO `job_jdbc_datasource` (`id`, `datasource_name`, `datasource_group`, `jdbc_username`, `jdbc_password`, `jdbc_url`, `jdbc_driver_class`, `status`, `create_time`, `update_time`, `comments`, `datasource`, `database_name`, `schema_name`, `is_deleted`) VALUES (22, 'test2', 'Default', 'root', '123456', 'jdbc:mysql://127.0.0.1:3306/test2', 'com.mysql.cj.jdbc.Driver', 1, '2026-02-10 18:12:48', '2026-03-07 10:49:26', '备注', 'MYSQL', 'test2', NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for job_lock
-- ----------------------------
DROP TABLE IF EXISTS `job_lock`;
CREATE TABLE `job_lock` (
  `lock_name` varchar(50) NOT NULL COMMENT '锁名称',
  PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_lock
-- ----------------------------
BEGIN;
INSERT INTO `job_lock` (`lock_name`) VALUES ('schedule_lock');
COMMIT;

-- ----------------------------
-- Table structure for job_log
-- ----------------------------
DROP TABLE IF EXISTS `job_log`;
CREATE TABLE `job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_group` bigint NOT NULL COMMENT '执行器主键ID',
  `job_id` bigint NOT NULL COMMENT '任务，主键ID',
  `executor_address` varchar(255) DEFAULT NULL COMMENT '执行器地址，本次执行的地址',
  `executor_handler` varchar(255) DEFAULT NULL COMMENT '执行器任务handler',
  `executor_param` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行器任务参数',
  `executor_sharding_param` varchar(20) DEFAULT NULL COMMENT '执行器任务分片参数，格式如 1/2',
  `executor_fail_retry_count` int NOT NULL DEFAULT '0' COMMENT '失败重试次数',
  `trigger_time` datetime DEFAULT NULL COMMENT '调度-时间',
  `trigger_code` int NOT NULL COMMENT '调度-结果',
  `trigger_msg` text COMMENT '调度-日志',
  `handle_time` datetime DEFAULT NULL COMMENT '执行-时间',
  `handle_code` int NOT NULL COMMENT '执行-状态',
  `handle_msg` text COMMENT '执行-日志',
  `alarm_status` tinyint NOT NULL DEFAULT '0' COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
  `node_status` text COMMENT '节点执行状态（JSON格式）',
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `I_trigger_time` (`trigger_time`),
  KEY `I_handle_code` (`handle_code`)
) ENGINE=InnoDB AUTO_INCREMENT=112506 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_log
-- ----------------------------
BEGIN;
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112463, 6, 24733, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807253175374581760', NULL, 0, '2026-02-05 22:15:55', 500, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：500<br>msg：xxl-job remoting error(Connection refused), for url : http://127.0.0.1:15000/run', NULL, 0, NULL, 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112464, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807807490124288000', NULL, 0, '2026-02-07 10:58:34', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 10:58:53', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13409\":{\"jobId\":25837,\"jobDesc\":\"任务节点1\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112465, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807807700330221568', NULL, 0, '2026-02-07 10:59:24', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 10:59:30', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112466, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807809360658042880', NULL, 0, '2026-02-07 11:06:00', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 11:06:06', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112467, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807809629760393216', NULL, 0, '2026-02-07 11:07:04', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 11:07:19', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112468, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807809843204329472', NULL, 0, '2026-02-07 11:07:55', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 11:08:32', 500, 'web container destroy and kill the job. [job running, killed]', 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112469, 6, 25836, NULL, NULL, '807810873614798848', NULL, 0, '2026-02-07 11:12:01', 0, NULL, NULL, 0, NULL, 0, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112470, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807811320547250176', NULL, 0, '2026-02-07 11:13:47', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 11:14:04', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13409\":{\"jobId\":25837,\"jobDesc\":\"任务节点1\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112471, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807811702958723072', NULL, 0, '2026-02-07 11:15:18', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 11:17:22', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112472, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807812937937981440', NULL, 0, '2026-02-07 11:20:13', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 11:20:36', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112473, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807814099886018560', NULL, 0, '2026-02-07 11:24:50', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 11:25:03', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112474, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807814268111163392', NULL, 0, '2026-02-07 11:25:30', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 11:25:44', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13409\":{\"jobId\":25837,\"jobDesc\":\"任务节点1\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112475, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807814382133317632', NULL, 0, '2026-02-07 11:25:57', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 11:26:22', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112476, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807814610102128640', NULL, 0, '2026-02-07 11:26:52', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 11:27:05', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13409\":{\"jobId\":25837,\"jobDesc\":\"任务节点1\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112477, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807814725688758272', NULL, 0, '2026-02-07 11:27:19', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 11:28:50', 200, '任务组执行完成', 0, '{\"13419\":{\"jobId\":25848,\"jobDesc\":\"任务节点7_copy\",\"status\":1},\"13411\":{\"jobId\":25839,\"jobDesc\":\"任务节点3\",\"status\":1},\"13412\":{\"jobId\":25840,\"jobDesc\":\"任务节点5\",\"status\":1},\"13445\":{\"jobId\":25874,\"jobDesc\":\"任务节点1_copy\",\"status\":1},\"13410\":{\"jobId\":25838,\"jobDesc\":\"任务节点2\",\"status\":1},\"13415\":{\"jobId\":25843,\"jobDesc\":\"任务节点7\",\"status\":1},\"13413\":{\"jobId\":25841,\"jobDesc\":\"任务节点6\",\"status\":1},\"13414\":{\"jobId\":25842,\"jobDesc\":\"任务节点4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112478, 6, 25836, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807832651355525120', NULL, 0, '2026-02-07 12:38:33', 500, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：800<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：500<br>msg：xxl-job remoting error(Connection refused), for url : http://127.0.0.1:15000/run', NULL, 0, NULL, 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112479, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807847349496975360', NULL, 0, '2026-02-07 13:36:57', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 13:37:36', 200, '任务组执行完成', 0, '{\"13367\":{\"jobId\":25788,\"jobDesc\":\"demoJobHandler2\",\"status\":1},\"13368\":{\"jobId\":25789,\"jobDesc\":\"demoJobHandler3\",\"status\":1},\"13365\":{\"jobId\":25786,\"jobDesc\":\"demoJobHandler1\",\"status\":1},\"13366\":{\"jobId\":25787,\"jobDesc\":\"demoJobHandler5\",\"status\":1},\"13369\":{\"jobId\":25790,\"jobDesc\":\"demoJobHandler4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112480, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807847566162137088', NULL, 0, '2026-02-07 13:37:49', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-02-07 13:38:05', 200, '任务组执行完成', 0, '{\"13367\":{\"jobId\":25788,\"jobDesc\":\"demoJobHandler2\",\"status\":1},\"13368\":{\"jobId\":25789,\"jobDesc\":\"demoJobHandler3\",\"status\":1},\"13365\":{\"jobId\":25786,\"jobDesc\":\"demoJobHandler1\",\"status\":1},\"13366\":{\"jobId\":25787,\"jobDesc\":\"demoJobHandler5\",\"status\":1},\"13369\":{\"jobId\":25790,\"jobDesc\":\"demoJobHandler4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112481, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807848411134038016', NULL, 0, '2026-02-07 13:41:10', 500, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：500<br>msg：xxl-job remoting error(Connection refused), for url : http://127.0.0.1:15000/run', NULL, 0, NULL, 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112482, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807848611084898304', NULL, 0, '2026-02-07 13:41:58', 500, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：500<br>msg：xxl-job remoting error(Connection refused), for url : http://127.0.0.1:15000/run', NULL, 0, NULL, 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112483, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '807848989885075456', NULL, 0, '2026-02-07 13:43:28', 500, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：500<br>msg：xxl-job remoting error(Connection refused), for url : http://127.0.0.1:15000/run', NULL, 0, NULL, 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112484, 8, 25951, 'http://127.0.0.1:12000/', 'runDataxHandler', '{\n  \"job\": {\n    \"content\": [\n      {\n        \"reader\": {\n          \"name\": \"mysqlreader\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"connection\": [\n              {\n                \"jdbcUrl\": [\n                  \"jdbc:mysql://127.0.0.1:3306/test1\"\n                ],\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ]\n          }\n        },\n        \"writer\": {\n          \"name\": \"mysqlwriter\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"connection\": [\n              {\n                \"jdbcUrl\": \"jdbc:mysql://127.0.0.1:3306/test2\",\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ],\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"writeMode\": \"insert\"\n          }\n        }\n      }\n    ],\n    \"setting\": {\n      \"speed\": {\n        \"channel\": 3.0,\n        \"byte\": -1.0\n      },\n      \"errorLimit\": {\n        \"record\": 0.0,\n        \"percentage\": 0.02\n      }\n    }\n  }\n}', NULL, 0, '2026-02-10 18:13:28', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:12000/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:12000/<br>code：200<br>msg：null', '2026-02-10 18:13:28', 500, 'java.lang.reflect.InvocationTargetException\n	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)\n	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n	at java.base/java.lang.reflect.Method.invoke(Method.java:569)\n	at com.xxl.job.core.handler.impl.MethodJobHandler.execute(MethodJobHandler.java:31)\n	at com.xxl.job.core.thread.JobThread.run(JobThread.java:176)\nCaused by: java.lang.RuntimeException: DataX任务执行失败\n	at com.cc.job.executor.handler.DataxHandler.runDataxHandler(DataxHandler.java:47)\n	... 6 more\nCaused by: java.lang.RuntimeException: DataX任务执行失败\n	at com.cc.job.executor.core.service.DataxTaskExecutor.execute(DataxTaskExecutor.java:78)\n	at com.cc.job.executor.handler.DataxHandler.runDataxHandler(DataxHandler.java:41)\n	... 6 more\nCaused by: java.lang.NullPointerException: Cannot invoke \"java.lang.Integer.intValue()\" because the return value of \"com.cc.job.xo.model.entity.JobInfo.getIncrementType()\" is null\n	at com.cc.job.executor.core.service.datax.DataxCommandBuilder.buildCommand(DataxCommandBuilder.java:47)\n	at com.cc.job.executor.core.service.DataxTaskExecutor.execute(DataxTaskExecutor.java:67)\n	... 7 more\n', 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112485, 8, 25951, 'http://127.0.0.1:12000/', 'runDataxHandler', '{\n  \"job\": {\n    \"content\": [\n      {\n        \"reader\": {\n          \"name\": \"mysqlreader\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"connection\": [\n              {\n                \"jdbcUrl\": [\n                  \"jdbc:mysql://127.0.0.1:3306/test1\"\n                ],\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ]\n          }\n        },\n        \"writer\": {\n          \"name\": \"mysqlwriter\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"connection\": [\n              {\n                \"jdbcUrl\": \"jdbc:mysql://127.0.0.1:3306/test2\",\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ],\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"writeMode\": \"insert\"\n          }\n        }\n      }\n    ],\n    \"setting\": {\n      \"speed\": {\n        \"channel\": 3.0,\n        \"byte\": -1.0\n      },\n      \"errorLimit\": {\n        \"record\": 0.0,\n        \"percentage\": 0.02\n      }\n    }\n  }\n}', NULL, 0, '2026-02-10 18:14:42', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:12000/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:12000/<br>code：200<br>msg：null', '2026-02-10 18:14:42', 500, 'java.lang.reflect.InvocationTargetException\n	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)\n	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n	at java.base/java.lang.reflect.Method.invoke(Method.java:569)\n	at com.xxl.job.core.handler.impl.MethodJobHandler.execute(MethodJobHandler.java:31)\n	at com.xxl.job.core.thread.JobThread.run(JobThread.java:176)\nCaused by: java.lang.RuntimeException: DataX任务执行失败\n	at com.cc.job.executor.handler.DataxHandler.runDataxHandler(DataxHandler.java:47)\n	... 6 more\nCaused by: java.lang.RuntimeException: DataX任务执行失败\n	at com.cc.job.executor.core.service.DataxTaskExecutor.execute(DataxTaskExecutor.java:78)\n	at com.cc.job.executor.handler.DataxHandler.runDataxHandler(DataxHandler.java:41)\n	... 6 more\nCaused by: java.lang.RuntimeException: DataX任务执行失败，退出码: 2\n	at com.cc.job.executor.core.service.DataxTaskExecutor.handleExecutionResult(DataxTaskExecutor.java:114)\n	at com.cc.job.executor.core.service.DataxTaskExecutor.execute(DataxTaskExecutor.java:73)\n	... 7 more\n', 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112486, 8, 25951, 'http://127.0.0.1:12000/', 'runDataxHandler', '{\n  \"job\": {\n    \"content\": [\n      {\n        \"reader\": {\n          \"name\": \"mysqlreader\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"connection\": [\n              {\n                \"jdbcUrl\": [\n                  \"jdbc:mysql://127.0.0.1:3306/test1\"\n                ],\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ]\n          }\n        },\n        \"writer\": {\n          \"name\": \"mysqlwriter\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"connection\": [\n              {\n                \"jdbcUrl\": \"jdbc:mysql://127.0.0.1:3306/test2\",\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ],\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"writeMode\": \"insert\"\n          }\n        }\n      }\n    ],\n    \"setting\": {\n      \"speed\": {\n        \"channel\": 3.0,\n        \"byte\": -1.0\n      },\n      \"errorLimit\": {\n        \"record\": 0.0,\n        \"percentage\": 0.02\n      }\n    }\n  }\n}', NULL, 0, '2026-02-10 18:15:04', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:12000/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:12000/<br>code：200<br>msg：null', '2026-02-10 18:15:04', 500, 'java.lang.reflect.InvocationTargetException\n	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)\n	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n	at java.base/java.lang.reflect.Method.invoke(Method.java:569)\n	at com.xxl.job.core.handler.impl.MethodJobHandler.execute(MethodJobHandler.java:31)\n	at com.xxl.job.core.thread.JobThread.run(JobThread.java:176)\nCaused by: java.lang.RuntimeException: DataX任务执行失败\n	at com.cc.job.executor.handler.DataxHandler.runDataxHandler(DataxHandler.java:47)\n	... 6 more\nCaused by: java.lang.RuntimeException: DataX任务执行失败\n	at com.cc.job.executor.core.service.DataxTaskExecutor.execute(DataxTaskExecutor.java:78)\n	at com.cc.job.executor.handler.DataxHandler.runDataxHandler(DataxHandler.java:41)\n	... 6 more\nCaused by: java.lang.RuntimeException: DataX任务执行失败，退出码: 2\n	at com.cc.job.executor.core.service.DataxTaskExecutor.handleExecutionResult(DataxTaskExecutor.java:114)\n	at com.cc.job.executor.core.service.DataxTaskExecutor.execute(DataxTaskExecutor.java:73)\n	... 7 more\n', 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112487, 8, 25951, 'http://127.0.0.1:12000/', 'runDataxHandler', '{\n  \"job\": {\n    \"content\": [\n      {\n        \"reader\": {\n          \"name\": \"mysqlreader\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"connection\": [\n              {\n                \"jdbcUrl\": [\n                  \"jdbc:mysql://127.0.0.1:3306/test1\"\n                ],\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ]\n          }\n        },\n        \"writer\": {\n          \"name\": \"mysqlwriter\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"connection\": [\n              {\n                \"jdbcUrl\": \"jdbc:mysql://127.0.0.1:3306/test2\",\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ],\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"writeMode\": \"insert\"\n          }\n        }\n      }\n    ],\n    \"setting\": {\n      \"speed\": {\n        \"channel\": 3.0,\n        \"byte\": -1.0\n      },\n      \"errorLimit\": {\n        \"record\": 0.0,\n        \"percentage\": 0.02\n      }\n    }\n  }\n}', NULL, 0, '2026-02-10 18:23:07', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:12000/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:12000/<br>code：200<br>msg：null', '2026-02-10 18:23:09', 500, 'java.lang.reflect.InvocationTargetException\n	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)\n	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n	at java.base/java.lang.reflect.Method.invoke(Method.java:569)\n	at com.xxl.job.core.handler.impl.MethodJobHandler.execute(MethodJobHandler.java:31)\n	at com.xxl.job.core.thread.JobThread.run(JobThread.java:176)\nCaused by: java.lang.RuntimeException: DataX任务执行失败\n	at com.cc.job.executor.handler.DataxHandler.runDataxHandler(DataxHandler.java:47)\n	... 6 more\nCaused by: java.lang.RuntimeException: DataX任务执行失败\n	at com.cc.job.executor.core.service.DataxTaskExecutor.execute(DataxTaskExecutor.java:78)\n	at com.cc.job.executor.handler.DataxHandler.runDataxHandler(DataxHandler.java:41)\n	... 6 more\nCaused by: java.lang.RuntimeException: DataX任务执行失败，退出码: 1\n	at com.cc.job.executor.core.service.DataxTaskExecutor.handleExecutionResult(DataxTaskExecutor.java:114)\n	at com.cc.job.executor.core.service.DataxTaskExecutor.execute(DataxTaskExecutor.java:73)\n	... 7 more\n', 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112488, 8, 25951, 'http://127.0.0.1:12000/', 'runDataxHandler', '{\n  \"job\": {\n    \"content\": [\n      {\n        \"reader\": {\n          \"name\": \"mysqlreader\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"connection\": [\n              {\n                \"jdbcUrl\": [\n                  \"jdbc:mysql://127.0.0.1:3306/test1\"\n                ],\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ]\n          }\n        },\n        \"writer\": {\n          \"name\": \"mysqlwriter\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"connection\": [\n              {\n                \"jdbcUrl\": \"jdbc:mysql://127.0.0.1:3306/test2\",\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ],\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"writeMode\": \"insert\"\n          }\n        }\n      }\n    ],\n    \"setting\": {\n      \"speed\": {\n        \"channel\": 3.0,\n        \"byte\": -1.0\n      },\n      \"errorLimit\": {\n        \"record\": 0.0,\n        \"percentage\": 0.02\n      }\n    }\n  }\n}', NULL, 0, '2026-02-10 18:25:30', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:12000/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:12000/<br>code：200<br>msg：null', '2026-02-10 18:25:41', 200, '', 0, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112489, 8, 25979, 'http://127.0.0.1:12000/', 'runDataxHandler', '{\n  \"job\": {\n    \"content\": [\n      {\n        \"reader\": {\n          \"name\": \"mysqlreader\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"where\": \"id \\u003e ${id} \",\n            \"connection\": [\n              {\n                \"jdbcUrl\": [\n                  \"jdbc:mysql://127.0.0.1:3306/test1\"\n                ],\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ]\n          }\n        },\n        \"writer\": {\n          \"name\": \"mysqlwriter\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"connection\": [\n              {\n                \"jdbcUrl\": \"jdbc:mysql://127.0.0.1:3306/test2\",\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ],\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"writeMode\": \"insert\"\n          }\n        }\n      }\n    ],\n    \"setting\": {\n      \"speed\": {\n        \"channel\": 3.0,\n        \"byte\": -1.0\n      },\n      \"errorLimit\": {\n        \"record\": 0.0,\n        \"percentage\": 0.02\n      }\n    }\n  }\n}', NULL, 0, '2026-02-28 18:45:12', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.9<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:12000/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:12000/<br>code：200<br>msg：null', '2026-02-28 18:45:24', 200, '', 0, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112490, 8, 25979, 'http://127.0.0.1:12000/', 'runDataxHandler', '{\n  \"job\": {\n    \"content\": [\n      {\n        \"reader\": {\n          \"name\": \"mysqlreader\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"where\": \"id > 3\",\n            \"connection\": [\n              {\n                \"jdbcUrl\": [\n                  \"jdbc:mysql://127.0.0.1:3306/test1\"\n                ],\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ]\n          }\n        },\n        \"writer\": {\n          \"name\": \"mysqlwriter\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"connection\": [\n              {\n                \"jdbcUrl\": \"jdbc:mysql://127.0.0.1:3306/test2\",\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ],\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"writeMode\": \"insert\"\n          }\n        }\n      }\n    ],\n    \"setting\": {\n      \"speed\": {\n        \"channel\": 3.0,\n        \"byte\": -1.0\n      },\n      \"errorLimit\": {\n        \"record\": 0.0,\n        \"percentage\": 0.02\n      }\n    }\n  }\n}', NULL, 0, '2026-02-28 18:47:15', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.9<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:12000/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:12000/<br>code：200<br>msg：null', '2026-02-28 18:47:26', 500, 'java.lang.reflect.InvocationTargetException\n	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)\n	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n	at java.base/java.lang.reflect.Method.invoke(Method.java:569)\n	at com.xxl.job.core.handler.impl.MethodJobHandler.execute(MethodJobHandler.java:31)\n	at com.xxl.job.core.thread.JobThread.run(JobThread.java:176)\nCaused by: java.lang.RuntimeException: DataX任务执行失败\n	at com.cc.job.executor.handler.DataxHandler.runDataxHandler(DataxHandler.java:47)\n	... 6 more\nCaused by: java.lang.RuntimeException: DataX任务执行失败\n	at com.cc.job.executor.core.service.DataxTaskExecutor.execute(DataxTaskExecutor.java:78)\n	at com.cc.job.executor.handler.DataxHandler.runDataxHandler(DataxHandler.java:41)\n	... 6 more\nCaused by: java.lang.RuntimeException: DataX任务执行失败，退出码: 1\n	at com.cc.job.executor.core.service.DataxTaskExecutor.handleExecutionResult(DataxTaskExecutor.java:114)\n	at com.cc.job.executor.core.service.DataxTaskExecutor.execute(DataxTaskExecutor.java:73)\n	... 7 more\n', 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112491, 8, 25979, 'http://127.0.0.1:12000/', 'runDataxHandler', '{\n  \"job\": {\n    \"content\": [\n      {\n        \"reader\": {\n          \"name\": \"mysqlreader\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"where\": \"id > 3\",\n            \"connection\": [\n              {\n                \"jdbcUrl\": [\n                  \"jdbc:mysql://127.0.0.1:3306/test1\"\n                ],\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ]\n          }\n        },\n        \"writer\": {\n          \"name\": \"mysqlwriter\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"connection\": [\n              {\n                \"jdbcUrl\": \"jdbc:mysql://127.0.0.1:3306/test2\",\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ],\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"writeMode\": \"insert\"\n          }\n        }\n      }\n    ],\n    \"setting\": {\n      \"speed\": {\n        \"channel\": 3.0,\n        \"byte\": -1.0\n      },\n      \"errorLimit\": {\n        \"record\": 0.0,\n        \"percentage\": 0.02\n      }\n    }\n  }\n}', NULL, 0, '2026-02-28 18:47:45', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.9<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:12000/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:12000/<br>code：200<br>msg：null', '2026-02-28 18:47:57', 200, '', 0, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112492, 6, 24733, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '815811814519607296', NULL, 0, '2026-03-01 13:04:54', 500, '任务触发类型：手动触发<br>调度机器：192.168.1.9<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：500<br>msg：xxl-job remoting error(Connection refused), for url : http://127.0.0.1:15000/run', NULL, 0, NULL, 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112493, 6, 24733, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817013921218170880', NULL, 0, '2026-03-04 20:41:38', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.9<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-04 20:41:40', 500, '任务组执行失败: 获取任务组信息失败', 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112494, 6, 24733, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817019552838848512', NULL, 0, '2026-03-04 21:04:01', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.9<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-04 21:04:03', 200, '任务组执行完成', 0, '{\"12339\":{\"jobId\":24734,\"jobDesc\":\"删除JobEdge里面的数据\",\"status\":1},\"12351\":{\"jobId\":24746,\"jobDesc\":\"删除job_log里面的数据\",\"status\":1},\"12352\":{\"jobId\":24747,\"jobDesc\":\"删除job_log_report里面的数据\",\"status\":1},\"12350\":{\"jobId\":24745,\"jobDesc\":\"删除JobGroup里面的数据\",\"status\":1},\"12355\":{\"jobId\":24750,\"jobDesc\":\"删除job_logglue里面的数据\",\"status\":1},\"12353\":{\"jobId\":24748,\"jobDesc\":\"删除job_part里面的数据\",\"status\":1},\"12354\":{\"jobId\":24749,\"jobDesc\":\"删除job_node里面的数据\",\"status\":1},\"12348\":{\"jobId\":24743,\"jobDesc\":\"删除job_jdbc_datasource里面的数据\",\"status\":1},\"12349\":{\"jobId\":24744,\"jobDesc\":\"删除JobInfo里面的数据\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112495, 6, 24733, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817020215677292544', NULL, 0, '2026-03-04 21:06:39', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.9<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-04 21:06:41', 200, '任务组执行完成', 0, '{\"12339\":{\"jobId\":24734,\"jobDesc\":\"删除JobEdge里面的数据\",\"status\":1},\"12351\":{\"jobId\":24746,\"jobDesc\":\"删除job_log里面的数据\",\"status\":1},\"12352\":{\"jobId\":24747,\"jobDesc\":\"删除job_log_report里面的数据\",\"status\":1},\"12350\":{\"jobId\":24745,\"jobDesc\":\"删除JobGroup里面的数据\",\"status\":1},\"12355\":{\"jobId\":24750,\"jobDesc\":\"删除job_logglue里面的数据\",\"status\":1},\"12353\":{\"jobId\":24748,\"jobDesc\":\"删除job_part里面的数据\",\"status\":1},\"12354\":{\"jobId\":24749,\"jobDesc\":\"删除job_node里面的数据\",\"status\":1},\"12348\":{\"jobId\":24743,\"jobDesc\":\"删除job_jdbc_datasource里面的数据\",\"status\":1},\"12349\":{\"jobId\":24744,\"jobDesc\":\"删除JobInfo里面的数据\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112496, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817928057573740544', NULL, 0, '2026-03-07 09:14:05', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-07 09:14:15', 200, '任务组执行完成', 0, '{\"13368\":{\"jobId\":25789,\"jobDesc\":\"demoJobHandler3\",\"status\":0},\"13365\":{\"jobId\":25786,\"jobDesc\":\"demoJobHandler1\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112497, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817928336192966656', NULL, 0, '2026-03-07 09:15:12', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-07 09:15:23', 200, '任务组执行完成', 0, '{\"13367\":{\"jobId\":25788,\"jobDesc\":\"demoJobHandler2\",\"status\":1},\"13369\":{\"jobId\":25790,\"jobDesc\":\"demoJobHandler4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112498, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817933967360659456', NULL, 0, '2026-03-07 09:37:34', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-07 09:37:51', 200, '任务组执行完成', 0, '{\"13367\":{\"jobId\":25788,\"jobDesc\":\"demoJobHandler2\",\"status\":1},\"13365\":{\"jobId\":25786,\"jobDesc\":\"demoJobHandler1\",\"status\":1},\"13369\":{\"jobId\":25790,\"jobDesc\":\"demoJobHandler4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112499, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817934107999866880', NULL, 0, '2026-03-07 09:38:08', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-07 09:38:19', 200, '任务组执行完成', 0, '{\"13367\":{\"jobId\":25788,\"jobDesc\":\"demoJobHandler2\",\"status\":1},\"13369\":{\"jobId\":25790,\"jobDesc\":\"demoJobHandler4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112500, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817934276724133888', NULL, 0, '2026-03-07 09:38:48', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-07 09:38:59', 200, '任务组执行完成', 0, '{\"13367\":{\"jobId\":25788,\"jobDesc\":\"demoJobHandler2\",\"status\":1},\"13369\":{\"jobId\":25790,\"jobDesc\":\"demoJobHandler4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112501, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817939815902351360', NULL, 0, '2026-03-07 10:00:49', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-07 10:01:06', 200, '任务组执行完成', 0, '{\"13367\":{\"jobId\":25788,\"jobDesc\":\"demoJobHandler2\",\"status\":1},\"13365\":{\"jobId\":25786,\"jobDesc\":\"demoJobHandler1\",\"status\":1},\"13369\":{\"jobId\":25790,\"jobDesc\":\"demoJobHandler4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112502, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817939965496397824', NULL, 0, '2026-03-07 10:01:24', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-07 10:01:36', 200, '任务组执行完成', 0, '{\"13367\":{\"jobId\":25788,\"jobDesc\":\"demoJobHandler2\",\"status\":1},\"13369\":{\"jobId\":25790,\"jobDesc\":\"demoJobHandler4\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112503, 8, 25979, 'http://127.0.0.1:12000/', 'runDataxHandler', '{\n  \"job\": {\n    \"content\": [\n      {\n        \"reader\": {\n          \"name\": \"mysqlreader\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"where\": \"id \\u003e  \",\n            \"connection\": [\n              {\n                \"jdbcUrl\": [\n                  \"jdbc:mysql://127.0.0.1:3306/test1\"\n                ],\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ]\n          }\n        },\n        \"writer\": {\n          \"name\": \"mysqlwriter\",\n          \"parameter\": {\n            \"username\": \"root\",\n            \"password\": \"123456\",\n            \"connection\": [\n              {\n                \"jdbcUrl\": \"jdbc:mysql://127.0.0.1:3306/test2\",\n                \"table\": [\n                  \"student\"\n                ]\n              }\n            ],\n            \"column\": [\n              \"id\",\n              \"name\",\n              \"create_time\"\n            ],\n            \"writeMode\": \"insert\"\n          }\n        }\n      }\n    ],\n    \"setting\": {\n      \"speed\": {\n        \"channel\": 3.0,\n        \"byte\": -1.0\n      },\n      \"errorLimit\": {\n        \"record\": 0.0,\n        \"percentage\": 0.02\n      }\n    }\n  }\n}', NULL, 0, '2026-03-07 10:36:18', 500, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:12000/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:12000/<br>code：500<br>msg：xxl-job remoting error(Connection refused), for url : http://127.0.0.1:12000/run', NULL, 0, NULL, 2, NULL, 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112504, 6, 25785, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817953386262564864', NULL, 0, '2026-03-07 10:54:44', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-07 10:54:51', 200, '任务组执行完成', 0, '{\"13368\":{\"jobId\":25789,\"jobDesc\":\"demoJobHandler3\",\"status\":0},\"13365\":{\"jobId\":25786,\"jobDesc\":\"demoJobHandler1\",\"status\":1}}', 0);
INSERT INTO `job_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`, `node_status`, `is_deleted`) VALUES (112505, 6, 24733, 'http://127.0.0.1:15000', 'runJobGroupXxlJob', '817959511674458112', NULL, 0, '2026-03-07 11:19:05', 200, '任务触发类型：手动触发<br>调度机器：192.168.1.2<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://127.0.0.1:15000]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：300<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://127.0.0.1:15000<br>code：200<br>msg：null', '2026-03-07 11:19:07', 200, '任务组执行完成', 0, '{\"12339\":{\"jobId\":24734,\"jobDesc\":\"删除JobEdge里面的数据\",\"status\":1},\"12351\":{\"jobId\":24746,\"jobDesc\":\"删除job_log里面的数据\",\"status\":1},\"12352\":{\"jobId\":24747,\"jobDesc\":\"删除job_log_report里面的数据\",\"status\":1},\"12350\":{\"jobId\":24745,\"jobDesc\":\"删除JobGroup里面的数据\",\"status\":1},\"12355\":{\"jobId\":24750,\"jobDesc\":\"删除job_logglue里面的数据\",\"status\":1},\"12353\":{\"jobId\":24748,\"jobDesc\":\"删除job_part里面的数据\",\"status\":1},\"12354\":{\"jobId\":24749,\"jobDesc\":\"删除job_node里面的数据\",\"status\":1},\"12348\":{\"jobId\":24743,\"jobDesc\":\"删除job_jdbc_datasource里面的数据\",\"status\":1},\"12349\":{\"jobId\":24744,\"jobDesc\":\"删除JobInfo里面的数据\",\"status\":1}}', 0);
COMMIT;

-- ----------------------------
-- Table structure for job_log_report
-- ----------------------------
DROP TABLE IF EXISTS `job_log_report`;
CREATE TABLE `job_log_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trigger_day` datetime DEFAULT NULL COMMENT '调度-时间',
  `running_count` int NOT NULL DEFAULT '0' COMMENT '运行中-日志数量',
  `suc_count` int NOT NULL DEFAULT '0' COMMENT '执行成功-日志数量',
  `fail_count` int NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
  `update_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=347 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_log_report
-- ----------------------------
BEGIN;
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (286, '2025-12-21 00:00:00', 0, 0, 0, '2025-12-21 12:29:50', '2025-12-21 12:28:50', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (287, '2025-12-20 00:00:00', 0, 0, 0, '2025-12-21 12:29:50', '2025-12-21 12:28:50', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (288, '2025-12-19 00:00:00', 0, 0, 0, '2025-12-21 12:29:50', '2025-12-21 12:28:50', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (289, '2025-12-24 00:00:00', 0, 59, 7, '2025-12-26 23:59:59', '2025-12-24 12:36:02', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (290, '2025-12-23 00:00:00', 0, 0, 0, '2025-12-25 23:59:26', '2025-12-24 12:36:02', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (291, '2025-12-22 00:00:00', 0, 0, 0, '2025-12-24 23:29:31', '2025-12-24 12:36:02', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (292, '2025-12-25 00:00:00', 0, 0, 0, '2025-12-27 23:59:33', '2025-12-25 20:37:21', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (293, '2025-12-26 00:00:00', 0, 0, 0, '2025-12-28 23:59:05', '2025-12-26 00:00:26', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (294, '2025-12-27 00:00:00', 0, 0, 0, '2025-12-29 23:59:38', '2025-12-27 00:00:59', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (295, '2025-12-28 00:00:00', 0, 0, 0, '2025-12-30 23:59:10', '2025-12-28 00:00:33', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (296, '2025-12-29 00:00:00', 0, 0, 0, '2025-12-31 11:59:27', '2025-12-29 00:00:05', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (297, '2025-12-30 00:00:00', 0, 0, 0, '2026-01-01 23:59:48', '2025-12-30 00:00:38', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (298, '2025-12-31 00:00:00', 0, 0, 0, '2026-01-02 23:59:13', '2025-12-31 00:00:10', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (299, '2026-01-01 00:00:00', 0, 0, 0, '2026-01-03 15:10:28', '2026-01-01 17:03:33', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (300, '2026-01-02 00:00:00', 0, 13, 10, '2026-01-03 15:10:28', '2026-01-02 00:00:48', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (301, '2026-01-03 00:00:00', 0, 0, 0, '2026-01-03 15:10:28', '2026-01-03 00:00:13', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (302, '2026-01-06 00:00:00', 0, 8, 0, '2026-01-08 23:59:09', '2026-01-06 21:39:05', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (303, '2026-01-05 00:00:00', 0, 0, 0, '2026-01-07 23:59:44', '2026-01-06 21:39:05', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (304, '2026-01-04 00:00:00', 0, 0, 0, '2026-01-06 23:59:46', '2026-01-06 21:39:05', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (305, '2026-01-07 00:00:00', 0, 1, 0, '2026-01-09 23:59:25', '2026-01-07 00:00:46', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (306, '2026-01-08 00:00:00', 0, 0, 0, '2026-01-10 22:48:46', '2026-01-08 00:00:44', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (307, '2026-01-09 00:00:00', 0, 0, 0, '2026-01-11 20:12:53', '2026-01-09 00:00:09', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (308, '2026-01-10 00:00:00', 1, 21, 21, '2026-01-11 20:12:53', '2026-01-10 00:00:25', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (309, '2026-01-11 00:00:00', 0, 21, 3, '2026-01-13 23:59:05', '2026-01-11 08:24:57', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (310, '2026-01-13 00:00:00', 0, 0, 0, '2026-01-15 11:39:53', '2026-01-13 21:49:32', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (311, '2026-01-12 00:00:00', 0, 0, 0, '2026-01-14 23:59:38', '2026-01-13 21:49:32', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (312, '2026-01-14 00:00:00', 0, 0, 0, '2026-01-15 11:39:53', '2026-01-14 00:00:05', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (313, '2026-01-15 00:00:00', 0, 0, 0, '2026-01-15 11:39:53', '2026-01-15 00:00:38', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (314, '2026-01-18 00:00:00', 0, 0, 0, '2026-01-20 23:59:45', '2026-01-18 09:09:29', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (315, '2026-01-17 00:00:00', 0, 0, 0, '2026-01-19 23:59:16', '2026-01-18 09:09:29', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (316, '2026-01-16 00:00:00', 0, 0, 0, '2026-01-18 23:59:46', '2026-01-18 09:09:29', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (317, '2026-01-19 00:00:00', 0, 0, 0, '2026-01-21 23:59:15', '2026-01-19 00:00:46', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (318, '2026-01-20 00:00:00', 0, 0, 0, '2026-01-22 21:39:41', '2026-01-20 00:00:16', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (319, '2026-01-21 00:00:00', 0, 0, 0, '2026-01-22 21:39:41', '2026-01-21 00:00:45', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (320, '2026-01-22 00:00:00', 0, 0, 0, '2026-01-22 21:39:41', '2026-01-22 00:00:15', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (321, '2026-01-31 00:00:00', 0, 0, 0, '2026-01-31 10:51:18', '2026-01-31 10:08:56', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (322, '2026-01-30 00:00:00', 0, 0, 0, '2026-01-31 10:51:18', '2026-01-31 10:08:56', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (323, '2026-01-29 00:00:00', 0, 0, 0, '2026-01-31 10:51:18', '2026-01-31 10:08:56', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (324, '2026-02-03 00:00:00', 0, 0, 0, '2026-02-05 21:34:21', '2026-02-03 20:18:17', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (325, '2026-02-02 00:00:00', 0, 0, 0, '2026-02-04 18:10:29', '2026-02-03 20:18:17', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (326, '2026-02-01 00:00:00', 0, 0, 0, '2026-02-03 20:18:17', '2026-02-03 20:18:17', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (327, '2026-02-04 00:00:00', 0, 0, 10, '2026-02-05 21:34:21', '2026-02-04 18:10:29', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (328, '2026-02-05 00:00:00', 0, 0, 1, '2026-02-07 19:24:45', '2026-02-05 17:52:49', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (329, '2026-02-07 00:00:00', 1, 14, 5, '2026-02-08 12:54:44', '2026-02-07 09:07:09', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (330, '2026-02-06 00:00:00', 0, 0, 0, '2026-02-08 12:54:44', '2026-02-07 09:07:09', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (331, '2026-02-08 00:00:00', 0, 0, 0, '2026-02-10 21:20:04', '2026-02-08 09:49:36', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (332, '2026-02-10 00:00:00', 0, 1, 4, '2026-02-10 21:20:04', '2026-02-10 17:58:18', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (333, '2026-02-09 00:00:00', 0, 0, 0, '2026-02-10 21:20:04', '2026-02-10 17:58:18', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (334, '2026-02-19 00:00:00', 0, 0, 0, '2026-02-19 13:41:18', '2026-02-19 13:41:18', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (335, '2026-02-18 00:00:00', 0, 0, 0, '2026-02-19 13:41:18', '2026-02-19 13:41:18', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (336, '2026-02-17 00:00:00', 0, 0, 0, '2026-02-19 13:41:18', '2026-02-19 13:41:18', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (337, '2026-02-28 00:00:00', 0, 2, 1, '2026-03-01 16:33:46', '2026-02-28 18:42:47', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (338, '2026-02-27 00:00:00', 0, 0, 0, '2026-03-01 16:33:46', '2026-02-28 18:42:47', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (339, '2026-02-26 00:00:00', 0, 0, 0, '2026-02-28 20:10:49', '2026-02-28 18:42:47', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (340, '2026-03-01 00:00:00', 0, 0, 1, '2026-03-01 16:33:46', '2026-03-01 09:07:19', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (341, '2026-03-04 00:00:00', 0, 2, 1, '2026-03-06 19:26:08', '2026-03-04 20:40:40', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (342, '2026-03-03 00:00:00', 0, 0, 0, '2026-03-05 23:59:45', '2026-03-04 20:40:40', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (343, '2026-03-02 00:00:00', 0, 0, 0, '2026-03-04 23:59:15', '2026-03-04 20:40:40', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (344, '2026-03-05 00:00:00', 0, 0, 0, '2026-03-07 11:20:06', '2026-03-05 00:00:15', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (345, '2026-03-06 00:00:00', 0, 0, 0, '2026-03-07 11:20:06', '2026-03-06 00:00:45', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (346, '2026-03-07 00:00:00', 0, 9, 1, '2026-03-07 11:20:06', '2026-03-07 09:11:11', 0);
COMMIT;

-- ----------------------------
-- Table structure for job_logglue
-- ----------------------------
DROP TABLE IF EXISTS `job_logglue`;
CREATE TABLE `job_logglue` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_id` bigint NOT NULL COMMENT '任务，主键ID',
  `glue_type` varchar(50) DEFAULT NULL COMMENT 'GLUE类型',
  `glue_source` mediumtext COMMENT 'GLUE源代码',
  `glue_remark` varchar(128) NOT NULL COMMENT 'GLUE备注',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=78 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_logglue
-- ----------------------------
BEGIN;
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`, `is_deleted`) VALUES (77, 25839, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\"\n\necho \"脚本位置：$0\"\necho \"任务参数：$1\"\necho \"分片序号 = $2\"\necho \"分片总数 = $3\"\n\necho \"Good bye!\"\nexit 0\n\n', 'glue1', '2026-01-02 17:51:13', '2026-01-02 17:51:13', 0);
COMMIT;

-- ----------------------------
-- Table structure for job_node
-- ----------------------------
DROP TABLE IF EXISTS `job_node`;
CREATE TABLE `job_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `job_id` bigint DEFAULT NULL COMMENT '任务',
  `node_position_x` double DEFAULT NULL COMMENT '节点x',
  `node_position_y` double DEFAULT NULL COMMENT '节点',
  `node_width` double DEFAULT '0' COMMENT '节点宽度',
  `node_height` double DEFAULT '0' COMMENT '节点高度',
  `node_in_degree` bigint DEFAULT '0' COMMENT '入度',
  `node_out_degree` bigint DEFAULT '0' COMMENT '出度',
  `node_parent_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `background_color` varchar(100) DEFAULT NULL,
  `sort` int unsigned DEFAULT NULL COMMENT '排序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `job_parent_id` bigint DEFAULT NULL COMMENT '任务组id',
  `properties` varchar(1000) DEFAULT NULL COMMENT '属性',
  `condition_expression` text COMMENT '条件表达式（条件节点专用）',
  `expression_type` varchar(50) DEFAULT NULL COMMENT '表达式类型：SIMPLE-简单表达式，SCRIPT-脚本表达式',
  `children` varchar(500) DEFAULT NULL COMMENT '孩子节点',
  `node_type` varchar(100) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL COMMENT '节点备注',
  `tags` varchar(1000) DEFAULT NULL COMMENT '节点标签（JSON格式）',
  `is_deleted` int DEFAULT '0',
  `trigger_status` int DEFAULT NULL COMMENT '节点运行状态',
  PRIMARY KEY (`id`),
  KEY `idx_sort` (`sort`),
  KEY `idx_job_node_job_parent_id` (`job_parent_id`) USING BTREE,
  KEY `idx_job_node_job_id` (`job_id`) USING BTREE,
  KEY `idx_job_node_node_type` (`node_type`) USING BTREE,
  KEY `idx_job_node_parent_type` (`job_parent_id`,`node_type`)
) ENGINE=InnoDB AUTO_INCREMENT=13563 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务节点';

-- ----------------------------
-- Records of job_node
-- ----------------------------
BEGIN;

COMMIT;

-- ----------------------------
-- Table structure for job_node_result
-- ----------------------------
DROP TABLE IF EXISTS `job_node_result`;
CREATE TABLE `job_node_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_group_id` bigint NOT NULL COMMENT '任务组ID',
  `execution_batch_id` varchar(64) NOT NULL COMMENT '执行批次ID',
  `job_id` bigint NOT NULL COMMENT '节点任务ID',
  `job_name` varchar(255) DEFAULT NULL COMMENT '节点任务名称',
  `instance_key` varchar(255) DEFAULT NULL COMMENT '实例隔离键（executorServerAddress）',
  `result_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行结果数据（JSON格式，用于兼容小数据或作为后备）',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径（相对路径，相对于basePath）',
  `data_size` bigint DEFAULT NULL COMMENT '数据大小（字节）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_task_group_batch` (`task_group_id`,`execution_batch_id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_execution_batch_id` (`execution_batch_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_file_path` (`file_path`),
  KEY `idx_task_group_instance_batch` (`task_group_id`,`instance_key`,`execution_batch_id`),
  KEY `idx_task_group_instance_job_time` (`task_group_id`,`instance_key`,`job_id`,`create_time`),
  KEY `idx_task_group_instance_time` (`task_group_id`,`instance_key`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=269 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节点执行结果表';

-- ----------------------------
-- Records of job_node_result
-- ----------------------------
BEGIN;

COMMIT;

-- ----------------------------
-- Table structure for job_node_template
-- ----------------------------
DROP TABLE IF EXISTS `job_node_template`;
CREATE TABLE `job_node_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(255) NOT NULL COMMENT '模板名称',
  `template_type` varchar(50) DEFAULT NULL COMMENT '模板类型（Bean/API/SQL等）',
  `template_config` text COMMENT '模板配置（JSON格式）',
  `template_category` varchar(100) DEFAULT NULL COMMENT '模板分类',
  `description` varchar(500) DEFAULT NULL COMMENT '模板描述',
  `is_public` tinyint DEFAULT '0' COMMENT '是否公开：0-私有，1-公开',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_template_type` (`template_type`),
  KEY `idx_template_category` (`template_category`),
  KEY `idx_create_user_id` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节点模板表';

-- ----------------------------
-- Records of job_node_template
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for job_param
-- ----------------------------
DROP TABLE IF EXISTS `job_param`;
CREATE TABLE `job_param` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `param_key` varchar(128) NOT NULL COMMENT '参数名，引用时使用 ${param_key}',
  `param_value` text COMMENT '参数值',
  `comment` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_param_key` (`param_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_param
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for job_part
-- ----------------------------
DROP TABLE IF EXISTS `job_part`;
CREATE TABLE `job_part` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `job_part_name` varchar(100) DEFAULT NULL COMMENT '任务组分区描述',
  `sort` int unsigned DEFAULT NULL COMMENT '排序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=127 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务组分区';

-- ----------------------------
-- Records of job_part
-- ----------------------------
BEGIN;

COMMIT;

-- ----------------------------
-- Table structure for job_permission
-- ----------------------------
DROP TABLE IF EXISTS `job_permission`;
CREATE TABLE `job_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `permission_code` varchar(64) NOT NULL COMMENT '权限码，如 part:view',
  `resource_type` varchar(32) NOT NULL COMMENT '资源类型：PART/JOB_INFO/JOB_NODE',
  `action` varchar(32) NOT NULL COMMENT '操作：VIEW/EDIT/DELETE/EXECUTE',
  `name` varchar(64) DEFAULT NULL COMMENT '权限名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';

-- ----------------------------
-- Records of job_permission
-- ----------------------------
BEGIN;
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (1, 'part:view', 'PART', 'VIEW', '分区查看', '查看任务分区', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (2, 'part:edit', 'PART', 'EDIT', '分区编辑', '新增/修改任务分区', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (3, 'part:delete', 'PART', 'DELETE', '分区删除', '删除任务分区', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (4, 'job_info:view', 'JOB_INFO', 'VIEW', '任务查看', '查看任务组/画布', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (5, 'job_info:edit', 'JOB_INFO', 'EDIT', '任务编辑', '新增/修改/删除任务', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (6, 'job_info:delete', 'JOB_INFO', 'DELETE', '任务删除', '删除任务', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (7, 'job_info:execute', 'JOB_INFO', 'EXECUTE', '任务执行', '触发执行任务', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (8, 'job_node:view', 'JOB_NODE', 'VIEW', '节点查看', '查看任务节点', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (9, 'job_node:edit', 'JOB_NODE', 'EDIT', '节点编辑', '编辑任务节点', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (10, 'job_node:delete', 'JOB_NODE', 'DELETE', '节点删除', '删除任务节点', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (11, 'permission:manage', 'SYSTEM', 'MANAGE', '权限管理', '进入权限管理页面、管理用户与角色', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (12, 'datasource:view', 'DATASOURCE', 'VIEW', '数据源查看', '查看数据源列表与详情', '2026-03-01 09:20:56', '2026-03-01 09:20:56', 0);
INSERT INTO `job_permission` (`id`, `permission_code`, `resource_type`, `action`, `name`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (13, 'datasource:edit', 'DATASOURCE', 'EDIT', '数据源编辑', '新增/修改/删除数据源、测试连接', '2026-03-01 09:20:56', '2026-03-01 09:20:56', 0);
COMMIT;

-- ----------------------------
-- Table structure for job_registry
-- ----------------------------
DROP TABLE IF EXISTS `job_registry`;
CREATE TABLE `job_registry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `registry_group` varchar(50) NOT NULL,
  `registry_key` varchar(255) NOT NULL,
  `registry_value` varchar(255) NOT NULL,
  `update_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `i_g_k_v` (`registry_group`,`registry_key`,`registry_value`)
) ENGINE=InnoDB AUTO_INCREMENT=866 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_registry
-- ----------------------------
BEGIN;

COMMIT;

-- ----------------------------
-- Table structure for job_resource_permission
-- ----------------------------
DROP TABLE IF EXISTS `job_resource_permission`;
CREATE TABLE `job_resource_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resource_type` varchar(32) NOT NULL COMMENT '资源类型：PART/JOB_INFO/JOB_NODE',
  `resource_id` bigint NOT NULL COMMENT '资源ID',
  `user_id` bigint DEFAULT NULL COMMENT '被授权用户ID',
  `role_id` bigint DEFAULT NULL COMMENT '被授权角色ID',
  `permission_type` varchar(32) NOT NULL COMMENT '权限类型：VIEW/EDIT/DELETE/EXECUTE',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_resource_user` (`resource_type`,`resource_id`,`user_id`,`permission_type`),
  KEY `idx_user_type` (`user_id`,`resource_type`,`permission_type`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源级权限表';

-- ----------------------------
-- Records of job_resource_permission
-- ----------------------------
BEGIN;

COMMIT;

-- ----------------------------
-- Table structure for job_role
-- ----------------------------
DROP TABLE IF EXISTS `job_role`;
CREATE TABLE `job_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name` varchar(64) NOT NULL COMMENT '角色名称',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

-- ----------------------------
-- Records of job_role
-- ----------------------------
BEGIN;
INSERT INTO `job_role` (`id`, `role_name`, `role_code`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (1, '管理员', 'admin', '拥有全部权限', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
INSERT INTO `job_role` (`id`, `role_name`, `role_code`, `description`, `create_time`, `update_time`, `is_deleted`) VALUES (2, '操作员', 'operator', '查看与执行', '2026-02-28 20:09:34', '2026-02-28 20:09:34', 0);
COMMIT;

-- ----------------------------
-- Table structure for job_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `job_role_permission`;
CREATE TABLE `job_role_permission` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-权限关联表';

-- ----------------------------
-- Records of job_role_permission
-- ----------------------------
BEGIN;
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 1, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 2, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 3, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 4, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 5, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 6, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 7, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 8, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 9, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 10, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 11, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 12, '2026-03-01 09:20:56');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (1, 13, '2026-03-01 09:20:56');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (2, 1, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (2, 4, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (2, 7, '2026-02-28 20:09:34');
INSERT INTO `job_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES (2, 8, '2026-02-28 20:09:34');
COMMIT;

-- ----------------------------
-- Table structure for job_sse_broadcast
-- ----------------------------
DROP TABLE IF EXISTS `job_sse_broadcast`;
CREATE TABLE `job_sse_broadcast` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `connection_key` varchar(255) NOT NULL COMMENT 'SSE 连接键 parentJobId:randomId',
  `message_json` text NOT NULL COMMENT 'SSE 消息体 JSON',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=待处理 1=已处理',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_created` (`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SSE多实例广播队列表';

-- ----------------------------
-- Records of job_sse_broadcast
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for job_user
-- ----------------------------
DROP TABLE IF EXISTS `job_user`;
CREATE TABLE `job_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `ex1` varchar(255) DEFAULT NULL,
  `ex2` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_user
-- ----------------------------
BEGIN;
INSERT INTO `job_user` (`id`, `username`, `password`, `ex1`, `ex2`, `create_time`, `update_time`, `is_deleted`) VALUES (1, 'admin', '$2a$10$xiwlvTUZ2wP3remxQ8SKEew7jlHc7aGhUDOGlZ2iy.bIC0rsWSZNO', NULL, NULL, NULL, NULL, 0);

COMMIT;

-- ----------------------------
-- Table structure for job_user_role
-- ----------------------------
DROP TABLE IF EXISTS `job_user_role`;
CREATE TABLE `job_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-角色关联表';

-- ----------------------------
-- Records of job_user_role
-- ----------------------------
BEGIN;
INSERT INTO `job_user_role` (`user_id`, `role_id`, `create_time`) VALUES (1, 1, '2026-02-28 20:09:34');

COMMIT;

-- ----------------------------
-- Table structure for job_validation
-- ----------------------------
DROP TABLE IF EXISTS `job_validation`;
CREATE TABLE `job_validation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_id` bigint NOT NULL COMMENT '任务ID',
  `validation_sql` text NOT NULL COMMENT '校验SQL，应返回可转为数字的第一列（如 SELECT COUNT(*) FROM t）',
  `expected_min_rows` int NOT NULL DEFAULT '0' COMMENT '期望的最小行数',
  `jdbc_datasource_id` bigint NOT NULL COMMENT '执行校验SQL的数据源ID',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_job_validation_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_validation
-- ----------------------------
BEGIN;
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

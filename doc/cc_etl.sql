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

 Date: 21/12/2025 12:29:55
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务组';

-- ----------------------------
-- Records of job_compose
-- ----------------------------
BEGIN;
INSERT INTO `job_compose` (`id`, `app_name`, `executor_address`, `executor_server_address`, `create_time`, `is_deleted`) VALUES (6, 'cc-job-executor-compose', 'http://192.168.1.6:15000/', 'http://192.168.1.6:8500/', '2025-12-21 11:36:33', 0);
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
) ENGINE=InnoDB AUTO_INCREMENT=22366 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务边';

-- ----------------------------
-- Records of job_edge
-- ----------------------------
BEGIN;
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21116, 13003, 13004, '2025-11-16 14:10:27', 25403, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21117, 13004, 13005, '2025-11-16 14:10:27', 25403, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21118, 13006, 13007, '2025-11-16 14:10:27', 25403, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21119, 13002, 13003, '2025-11-16 14:10:27', 25403, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21120, 13004, 13006, '2025-11-16 14:10:27', 25403, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21313, 13016, 13017, '2025-11-16 14:12:34', 25416, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21314, 13017, 13018, '2025-11-16 14:12:34', 25416, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21315, 13019, 13020, '2025-11-16 14:12:34', 25416, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21316, 13015, 13016, '2025-11-16 14:12:34', 25416, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21317, 13017, 13019, '2025-11-16 14:12:34', 25416, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21840, 13055, 13057, '2025-11-16 14:25:38', 25455, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21841, 13056, 13057, '2025-11-16 14:25:38', 25455, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21842, 13054, 13055, '2025-11-16 14:25:38', 25455, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21843, 13054, 13056, '2025-11-16 14:25:38', 25455, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21844, 13056, 13058, '2025-11-16 14:25:38', 25455, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (21845, 13058, 13057, '2025-11-16 14:25:38', 25455, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22177, 13093, 13094, '2025-11-22 15:39:08', 25500, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22178, 13094, 13361, '2025-11-22 15:39:08', 25500, NULL, NULL, 'bottom', 'top', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22179, 13094, 13362, '2025-11-22 15:39:08', 25500, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22180, 13361, 13364, '2025-11-22 15:39:08', 25500, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22181, 13362, 13364, '2025-11-22 15:39:08', 25500, NULL, NULL, 'bottom', 'top', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22182, 13364, 13363, '2025-11-22 15:39:08', 25500, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22198, 12350, 12348, '2025-12-08 15:43:44', 24733, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22199, 12350, 12351, '2025-12-08 15:43:44', 24733, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22200, 12350, 12352, '2025-12-08 15:43:44', 24733, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22201, 12350, 12355, '2025-12-08 15:43:44', 24733, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22202, 12348, 12339, '2025-12-08 15:43:44', 24733, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22203, 12351, 12354, '2025-12-08 15:43:44', 24733, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22204, 12352, 12349, '2025-12-08 15:43:44', 24733, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22205, 12350, 12353, '2025-12-08 15:43:44', 24733, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22306, 13365, 13367, '2025-12-21 08:47:07', 25785, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22307, 13365, 13368, '2025-12-21 08:47:07', 25785, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22308, 13368, 13366, '2025-12-21 08:47:07', 25785, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22309, 13367, 13369, '2025-12-21 08:47:07', 25785, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22310, 13367, 13366, '2025-12-21 08:47:07', 25785, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22311, 13369, 13370, '2025-12-21 08:47:07', 25785, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22312, 13366, 13370, '2025-12-21 08:47:07', 25785, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22358, 13410, 13414, '2025-12-21 12:10:03', 25836, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22359, 13414, 13412, '2025-12-21 12:10:03', 25836, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22360, 13411, 13415, '2025-12-21 12:10:03', 25836, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22361, 13409, 13410, '2025-12-21 12:10:03', 25836, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22362, 13410, 13411, '2025-12-21 12:10:03', 25836, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22363, 13412, 13413, '2025-12-21 12:10:03', 25836, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22364, 13415, 13419, '2025-12-21 12:10:03', 25836, NULL, NULL, 'right', 'left', 0);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `job_parent_id`, `points_list`, `properties`, `start_point`, `end_point`, `is_deleted`) VALUES (22365, 13413, 13415, '2025-12-21 12:10:03', 25836, NULL, NULL, 'right', 'left', 0);
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
INSERT INTO `job_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`, `create_time`, `is_deleted`) VALUES (1, 'xxl-job-executor-sample', '示例执行器', 1, 'http://127.0.0.1:13000/', '2025-01-23 22:29:08', '2024-11-08 21:38:55', 0);
INSERT INTO `job_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`, `create_time`, `is_deleted`) VALUES (6, 'job-group-executor', '任务集执行器', 1, 'http://127.0.0.1:15000', '2025-11-30 17:01:11', '2024-11-16 09:49:27', 0);
INSERT INTO `job_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`, `create_time`, `is_deleted`) VALUES (8, 'cc-job-executor', 'cc-job-executor', 1, 'http://127.0.0.1:12000/', '2024-12-20 18:40:30', '2024-12-16 21:45:50', 0);
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
) ENGINE=InnoDB AUTO_INCREMENT=414 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务组快照表';

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
  `update_time` datetime DEFAULT NULL,
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
  `create_time` datetime DEFAULT NULL,
  `job_type` int DEFAULT '0' COMMENT '任务类型：0-普通任务，2-任务组',
  `parent_id` bigint DEFAULT '0' COMMENT '父任务ID',
  `req_type` varchar(100) DEFAULT NULL COMMENT '请求类型（API任务专用）',
  `req_header` varchar(1000) DEFAULT NULL COMMENT '请求头（API任务专用）',
  `req_body` varchar(3000) DEFAULT NULL COMMENT '请求体（API任务专用）',
  `req_url` varchar(1000) DEFAULT NULL COMMENT '请求URL（API任务专用）',
  `node_flag` varchar(10) DEFAULT NULL COMMENT '节点标识：Y-是节点，N-不是节点',
  `jdbc_datasource_id` bigint DEFAULT NULL COMMENT 'JDBC数据源ID',
  `increment_type` tinyint DEFAULT NULL COMMENT '增量类型：0-全量，1-增量',
  `increment_content` varchar(2000) DEFAULT NULL COMMENT '增量字段配置（JSON格式）',
  `pause_status` tinyint DEFAULT '0' COMMENT '暂停状态：0-运行，1-暂停',
  `job_part_id` int DEFAULT NULL COMMENT '任务分区ID',
  `is_deleted` int DEFAULT '0',
  `trigger_user_id` bigint DEFAULT NULL COMMENT '触发用户ID',
  PRIMARY KEY (`id`),
  KEY `idx_job_info_job_type` (`job_type`),
  KEY `idx_job_info_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=25849 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_info
-- ----------------------------
BEGIN;
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (34, 1, 'demoJobHandler2-1', '2024-11-16 09:39:05', 'xz', NULL, 'NONE', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'runJobJdbcXxlJob', '', 'SERIAL_EXECUTION', 0, 0, 0, 'SQL', NULL, NULL, '2025-05-09 21:51:51', NULL, 0, 0, 0, '2024-11-16 09:39:05', 0, 0, NULL, NULL, '', '', 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (35, 1, 'demoJobHandler3', '2024-11-16 09:39:13', 'xz', NULL, 'NONE', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler3', '', 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2025-05-09 22:23:12', NULL, 0, 0, 0, '2024-11-16 09:39:13', 0, 0, NULL, NULL, '', '', 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (36, 1, 'demoJobHandler4', '2024-11-16 09:39:20', 'xz', NULL, 'NONE', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler4', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2025-06-27 12:59:39', NULL, 0, 0, 0, '2024-11-16 09:39:20', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (37, 1, 'demoJobHandler5', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler5', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (38, 1, 'demoJobHandler6', '2024-11-16 09:38:55', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler6', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-12-21 22:54:20', NULL, 0, 0, 0, '2024-11-16 09:38:55', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (39, 1, 'demoJobHandler7', '2024-11-16 09:39:05', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler7', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:05', NULL, 0, 0, 0, '2024-11-16 09:39:05', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (40, 1, 'demoJobHandler8', '2024-11-16 09:39:13', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler8', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:13', NULL, 0, 0, 0, '2024-11-16 09:39:13', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (41, 1, 'demoJobHandler9', '2024-11-16 09:39:20', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler9', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:20', NULL, 0, 0, 0, '2024-11-16 09:39:20', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (42, 1, 'demoJobHandler10', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler10', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (43, 1, 'demoJobHandler11', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler11', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (44, 1, 'demoJobHandler12', '2024-11-16 09:38:55', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler12', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:38:55', NULL, 0, 0, 0, '2024-11-16 09:38:55', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (45, 1, 'demoJobHandler13', '2024-11-16 09:39:05', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler13', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:05', NULL, 0, 0, 0, '2024-11-16 09:39:05', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (46, 1, 'demoJobHandler14', '2024-11-16 09:39:13', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler14', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:13', NULL, 0, 0, 0, '2024-11-16 09:39:13', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (47, 1, 'demoJobHandler15', '2024-11-16 09:39:20', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler15', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:20', NULL, 0, 0, 0, '2024-11-16 09:39:20', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (48, 1, 'demoJobHandler16', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler16', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (49, 1, 'demoJobHandler17', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler17', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (50, 1, 'demoJobHandler18', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler18', NULL, 'SERIAL_EXECUTION', 0, 2, 0, 'BEAN', NULL, NULL, '2025-12-20 08:27:23', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (51, 1, 'demoJobHandler19', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler19', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (52, 1, 'demoJobHandler20', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler20', NULL, 'SERIAL_EXECUTION', 0, 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (24733, 6, '删除没有用的数据', '2025-08-23 13:42:09', 'xz', NULL, 'CRON', '0 0 * * * ? *', 'DO_NOTHING', 'FIRST', NULL, 'runJobGroupXxlJob', '24733', 'SERIAL_EXECUTION', 300, 0, 0, 'BEAN', NULL, NULL, '2025-12-08 15:43:44', NULL, 0, 1764864000000, 1764867600000, '2025-08-23 13:42:09', 2, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, 55, 0, 3);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (24734, 8, '删除JobEdge里面的数据', '2025-08-26 16:36:23', 'xz', NULL, 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'runJobJdbcXxlJob', 'delete from job_edge where is_deleted =1;', 'SERIAL_EXECUTION', 0, 0, 70, 'SQL', NULL, NULL, '2025-08-23 13:50:52', NULL, 0, 0, 0, '2025-08-23 13:43:56', 0, 24733, NULL, NULL, '', '', 'Y', 20, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (24743, 8, '删除job_jdbc_datasource里面的数据', '2025-08-26 16:36:23', 'xz', NULL, 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'runJobJdbcXxlJob', 'delete from job_jdbc_datasource where is_deleted =1;', 'SERIAL_EXECUTION', 0, 0, 71, 'SQL', NULL, NULL, '2025-08-23 13:52:24', NULL, 0, 0, 0, '2025-08-23 13:51:03', 0, 24733, NULL, NULL, '', '', 'Y', 20, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (24744, 8, '删除JobInfo里面的数据', '2025-08-26 16:36:23', 'xz', NULL, 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'runJobJdbcXxlJob', 'delete from job_info where is_deleted =1;', 'SERIAL_EXECUTION', 0, 0, 71, 'SQL', NULL, NULL, '2025-08-23 13:52:30', NULL, 0, 0, 0, '2025-08-23 13:50:59', 0, 24733, NULL, NULL, '', '', 'Y', 20, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (24745, 8, '删除JobGroup里面的数据', '2025-08-26 16:36:23', 'xz', NULL, 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'runJobJdbcXxlJob', 'delete from job_group where is_deleted =1;', 'SERIAL_EXECUTION', 0, 0, 81, 'SQL', NULL, NULL, '2025-08-23 13:52:35', NULL, 0, 0, 0, '2025-08-23 13:50:57', 0, 24733, NULL, NULL, '', '', 'Y', 20, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (24746, 8, '删除job_log里面的数据', '2025-08-26 16:36:23', 'xz', NULL, 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'runJobJdbcXxlJob', 'delete from job_log where is_deleted =1;', 'SERIAL_EXECUTION', 0, 0, 79, 'SQL', NULL, NULL, '2025-08-23 13:52:54', NULL, 0, 0, 0, '2025-08-23 13:51:01', 0, 24733, NULL, NULL, '', '', 'Y', 20, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (24747, 8, '删除job_log_report里面的数据', '2025-08-26 16:36:23', 'xz', NULL, 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'runJobJdbcXxlJob', 'delete from job_log_report where is_deleted =1;', 'SERIAL_EXECUTION', 0, 0, 71, 'SQL', NULL, NULL, '2025-08-23 13:53:09', NULL, 0, 0, 0, '2025-08-23 13:51:05', 0, 24733, NULL, NULL, '', '', 'Y', 20, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (24748, 8, '删除job_part里面的数据', '2025-08-26 16:36:23', 'xz', NULL, 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'runJobJdbcXxlJob', 'delete from job_part where is_deleted =1;', 'SERIAL_EXECUTION', 0, 0, 60, 'SQL', NULL, NULL, '2025-08-23 13:54:05', NULL, 0, 0, 0, '2025-08-23 13:53:56', 0, 24733, NULL, NULL, '', '', 'Y', 20, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (24749, 8, '删除job_node里面的数据', '2025-08-26 16:36:23', 'xz', NULL, 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'runJobJdbcXxlJob', 'delete from job_node where is_deleted =1;', 'SERIAL_EXECUTION', 0, 0, 66, 'SQL', NULL, NULL, '2025-08-23 13:54:09', NULL, 0, 0, 0, '2025-08-23 13:53:40', 0, 24733, NULL, NULL, '', '', 'Y', 20, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (24750, 8, '删除job_logglue里面的数据', '2025-08-26 16:36:23', 'xz', NULL, 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'runJobJdbcXxlJob', 'delete from job_logglue where is_deleted =1;', 'SERIAL_EXECUTION', 0, 0, 77, 'SQL', NULL, NULL, '2025-08-23 13:54:13', NULL, 0, 0, 0, '2025-08-23 13:53:12', 0, 24733, NULL, NULL, '', '', 'Y', 20, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25500, 6, 'qwer', '2025-11-21 22:39:12', 'xz', '', 'CRON', '0 0 0 * * ?', 'DO_NOTHING', 'FIRST', NULL, 'runJobGroupXxlJob', '25500', 'SERIAL_EXECUTION', 3, 0, 0, 'BEAN', NULL, NULL, '2025-11-22 15:41:30', '', 0, 0, 0, '2025-11-21 22:39:12', 1, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, 55, 0, 3);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25501, 1, 'demoJobHandler1', '2025-11-21 22:39:18', '11', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler1', '', 'SERIAL_EXECUTION', 300, 0, 5133, 'BEAN', NULL, NULL, '2025-11-22 11:55:25', NULL, 0, 0, 0, '2025-11-21 22:39:18', 0, 25500, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25502, 1, 'demoJobHandler2', '2025-11-21 22:39:19', '11', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler2', '', 'SERIAL_EXECUTION', 300, 0, 5081, 'BEAN', NULL, NULL, '2025-11-22 11:55:33', NULL, 0, 0, 0, '2025-11-21 22:39:19', 0, 25500, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25781, 1, 'demoJobHandler3', '2025-11-22 15:38:09', '11', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler3', '', 'SERIAL_EXECUTION', 300, 0, 5064, 'BEAN', NULL, NULL, '2025-11-22 15:39:21', NULL, 0, 0, 0, '2025-11-22 15:38:09', 0, 25500, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25782, 1, 'demoJobHandler5', '2025-11-22 15:38:12', '11', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler5', '', 'SERIAL_EXECUTION', 300, 0, 5096, 'BEAN', NULL, NULL, '2025-11-22 15:39:36', NULL, 0, 0, 0, '2025-11-22 15:38:12', 0, 25500, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25783, 1, 'demoJobHandler6', '2025-11-22 15:38:15', '11', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler6', '', 'SERIAL_EXECUTION', 300, 0, 5101, 'BEAN', NULL, NULL, '2025-11-22 15:39:44', NULL, 0, 0, 0, '2025-11-22 15:38:15', 0, 25500, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25784, 1, 'demoJobHandler4', '2025-11-22 15:38:17', '11', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', NULL, 'demoJobHandler4', '', 'SERIAL_EXECUTION', 300, 0, 5071, 'BEAN', NULL, NULL, '2025-11-22 15:39:31', NULL, 0, 0, 0, '2025-11-22 15:38:17', 0, 25500, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25785, 6, '测试任务组1', '2025-11-22 15:45:50', 'xz', '', 'CRON', '0 0 0 * * ?', 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'runJobGroupXxlJob', '25785', 'SERIAL_EXECUTION', 300, 0, 0, 'BEAN', NULL, NULL, '2025-12-21 08:47:07', '', 0, 1766246400000, 1766332800000, '2025-11-22 15:45:50', 2, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, 55, 0, 3);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25786, 1, 'demoJobHandler1', '2025-11-22 15:46:29', 'xz', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'DO_NOTHING', 'demoJobHandler1', '', 'SERIAL_EXECUTION', 300, 0, 5091, 'BEAN', NULL, NULL, '2025-12-20 09:25:05', NULL, 0, 0, 0, '2025-11-22 15:46:29', 0, 25785, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25787, 1, 'demoJobHandler5', '2025-11-22 15:46:32', 'xz', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'demoJobHandler5', '', 'SERIAL_EXECUTION', 300, 0, 5063, 'BEAN', NULL, NULL, '2025-12-20 08:51:39', NULL, 0, 0, 0, '2025-11-22 15:46:32', 0, 25785, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25788, 1, 'demoJobHandler2', '2025-11-22 15:46:33', 'xz', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'DO_NOTHING', 'demoJobHandler2', '', 'SERIAL_EXECUTION', 300, 3, 5076, 'BEAN', NULL, NULL, '2025-12-21 10:56:55', NULL, 0, 0, 0, '2025-11-22 15:46:33', 0, 25785, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25789, 1, 'demoJobHandler3', '2025-11-22 15:46:34', 'xz', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'demoJobHandler3', '', 'SERIAL_EXECUTION', 300, 0, 17606, 'BEAN', NULL, NULL, '2025-12-21 10:56:51', NULL, 0, 0, 0, '2025-11-22 15:46:34', 0, 25785, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25790, 1, 'demoJobHandler4', '2025-11-22 15:46:35', 'xz', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'demoJobHandler4', '', 'SERIAL_EXECUTION', 300, 0, 5109, 'BEAN', NULL, NULL, '2025-12-20 08:51:35', NULL, 0, 0, 0, '2025-11-22 15:46:35', 0, 25785, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25791, 1, 'demoJobHandler6', '2025-11-22 15:46:59', 'xz', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'demoJobHandler6', '', 'SERIAL_EXECUTION', 300, 0, 5045, 'BEAN', NULL, NULL, '2025-12-20 08:51:42', NULL, 0, 0, 0, '2025-11-22 15:46:59', 0, 25785, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25836, 6, '测试任务组2', '2025-12-21 09:08:28', '小赵', '', 'CRON', '0 0 0 * * ?', 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'runJobGroupXxlJob', '25836', 'SERIAL_EXECUTION', 800, 0, 0, 'BEAN', NULL, NULL, '2025-12-21 12:10:03', '', 0, 0, 1766332800000, '2025-12-21 09:08:28', 2, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, 64, 0, 3);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25837, 1, '任务节点1', '2025-12-21 09:09:22', '小赵', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'demoJobHandler1', '', 'SERIAL_EXECUTION', 300, 0, 0, 'BEAN', NULL, NULL, '2025-12-21 09:09:22', NULL, 0, 0, 0, '2025-12-21 09:09:22', 0, 25836, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25838, 8, '任务节点2', '2025-12-21 09:10:10', '小赵', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'runApiHandler', '{\"content-type\":\"*\"}', 'SERIAL_EXECUTION', 300, 0, 0, 'API', NULL, NULL, '2025-12-21 10:50:27', NULL, 0, 0, 0, '2025-12-21 09:10:10', 0, 25836, 'POST', NULL, '{\n    \"jobPartName\": \"测试4\",\n    \"sort\": 1\n}', 'http://localhost:8989/api/v1/jobParts/saveJobPart', 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25839, 1, '任务节点3', '2025-12-21 09:10:32', '小赵', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'demoJobHandler3', '', 'SERIAL_EXECUTION', 300, 0, 0, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\"\n\necho \"脚本位置：$0\"\necho \"任务参数：$1\"\necho \"分片序号 = $2\"\necho \"分片总数 = $3\"\n\necho \"Good bye!\"\nexit 0\n\n', 'glue1', '2025-12-21 09:13:35', NULL, 0, 0, 0, '2025-12-21 09:10:32', 0, 25836, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25840, 1, '任务节点5', '2025-12-21 09:10:34', '小赵', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'demoJobHandler5', '', 'SERIAL_EXECUTION', 300, 0, 0, 'GLUE_GROOVY', 'package com.xxl.job.service.handler;\n\nimport com.xxl.job.core.context.XxlJobHelper;\nimport com.xxl.job.core.handler.IJobHandler;\n\npublic class DemoGlueJobHandler extends IJobHandler {\n\n	@Override\n	public void execute() throws Exception {\n		XxlJobHelper.log(\"XXL-JOB, Hello World.\");\n	}\n}\n\n', 'java1', '2025-12-21 10:24:29', NULL, 0, 0, 0, '2025-12-21 09:10:34', 0, 25836, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25841, 1, '任务节点6', '2025-12-21 09:10:36', '小赵', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'demoJobHandler6', '', 'SERIAL_EXECUTION', 300, 0, 0, 'GLUE_PYTHON', '#!/usr/bin/python\n# -*- coding: UTF-8 -*-\nimport time\nimport sys\n\nprint (\"xxl-job: hello python\")\n\nprint (\"脚本位置：\", sys.argv[0])\nprint (\"任务参数：\", sys.argv[1])\nprint (\"分片序号：\", sys.argv[2])\nprint (\"分片总数：\", sys.argv[3])\n\nprint (\"Good bye!\")\nexit(0)\n\n', '测试python1', '2025-12-21 10:39:41', NULL, 0, 0, 0, '2025-12-21 09:10:36', 0, 25836, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25842, 8, '任务节点4', '2025-12-21 09:10:38', '小赵', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'runJobJdbcXxlJob', 'select * from job_part', 'SERIAL_EXECUTION', 300, 0, 0, 'SQL', NULL, NULL, '2025-12-21 10:22:54', NULL, 0, 0, 0, '2025-12-21 09:10:38', 0, 25836, NULL, NULL, NULL, NULL, 'Y', 20, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25843, 8, '任务节点7', '2025-12-21 09:10:51', '小赵', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'demoJobHandler7', '', 'SERIAL_EXECUTION', 300, 0, 0, 'GLUE_NODEJS', '#!/usr/bin/env node\nconsole.log(\"xxl-job: hello nodejs\")\n\nvar arguments = process.argv\n\nconsole.log(\"脚本位置: \" + arguments[1])\nconsole.log(\"任务参数: \" + arguments[2])\nconsole.log(\"分片序号: \" + arguments[3])\nconsole.log(\"分片总数: \" + arguments[4])\n\nconsole.log(\"Good bye!\")\nprocess.exit(0)\n\n', 'node1', '2025-12-21 10:46:20', NULL, 0, 0, 0, '2025-12-21 09:10:51', 0, 25836, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25845, 6, '单节点任务', '2025-12-21 09:17:49', 'xz', '', 'CRON', '0 0 0 * * ?', 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'runJobGroupXxlJob', '25845', 'SERIAL_EXECUTION', 300, 0, 0, 'BEAN', NULL, NULL, '2025-12-21 09:18:48', '', 0, 0, 0, '2025-12-21 09:17:49', 2, 0, NULL, NULL, NULL, NULL, 'N', NULL, NULL, NULL, 0, 65, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25846, 1, '11', '2025-12-21 09:18:33', 'xz', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', '11', '', 'SERIAL_EXECUTION', 300, 0, 0, 'BEAN', NULL, NULL, '2025-12-21 09:18:33', NULL, 0, 0, 0, '2025-12-21 09:18:33', 0, 25845, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `fail_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `run_time`, `glue_type`, `glue_source`, `glue_remark`, `glue_update_time`, `child_job_id`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `node_flag`, `jdbc_datasource_id`, `increment_type`, `increment_content`, `pause_status`, `job_part_id`, `is_deleted`, `trigger_user_id`) VALUES (25848, 8, '任务节点7_copy', '2025-12-21 11:36:17', '小赵', '', 'NONE', NULL, 'DO_NOTHING', 'FIRST', 'JOB_FAIL', 'demoJobHandler7', '', 'SERIAL_EXECUTION', 300, 0, 0, 'GLUE_CSHARP', 'using System;\n\nnamespace XxlJob.Service.Handler\n{\n    public class DemoGlueJobHandler\n    {\n        public void Execute()\n        {\n            // 使用 XxlJobHelper，方法与 Java 版本保持一致\n            XxlJobHelper.log(\"XXL-JOB, Hello C# World.\");\n            \n            // 获取任务参数\n            string jobParam = XxlJobHelper.getJobParam();\n            int shardIndex = XxlJobHelper.getShardIndex();\n            int shardTotal = XxlJobHelper.getShardTotal();\n            \n            XxlJobHelper.log(\"任务参数: {0}\", jobParam);\n            XxlJobHelper.log(\"分片序号: {0}, 分片总数: {1}\", shardIndex, shardTotal);\n            Console.WriteLine(\"XXL-JOB, Hello C# World.\");\n\n            // 在这里编写您的业务逻辑\n            // 例如：数据库操作、API 调用、文件处理等\n        }\n    }\n}\n\n', 'c#3', '2025-12-21 11:38:53', NULL, 0, 0, 0, '2025-12-21 11:36:17', 0, 25836, NULL, NULL, NULL, NULL, 'Y', NULL, NULL, NULL, 0, NULL, 0, NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='jdbc数据源配置';

-- ----------------------------
-- Records of job_jdbc_datasource
-- ----------------------------
BEGIN;
INSERT INTO `job_jdbc_datasource` (`id`, `datasource_name`, `datasource_group`, `jdbc_username`, `jdbc_password`, `jdbc_url`, `jdbc_driver_class`, `status`, `create_time`, `update_time`, `comments`, `datasource`, `database_name`, `schema_name`, `is_deleted`) VALUES (19, '测试数据源', 'Default', 'root', 'root', 'jdbc:mysql://localhost:3306/yanhuo-test', 'com.mysql.cj.jdbc.Driver', 1, '2025-05-26 19:47:51', '2025-05-26 19:47:51', '测试数据源', 'MYSQL', 'yanhuo-test', NULL, 0);
INSERT INTO `job_jdbc_datasource` (`id`, `datasource_name`, `datasource_group`, `jdbc_username`, `jdbc_password`, `jdbc_url`, `jdbc_driver_class`, `status`, `create_time`, `update_time`, `comments`, `datasource`, `database_name`, `schema_name`, `is_deleted`) VALUES (20, 'cc_etl', 'Default', 'root', '123456', 'jdbc:mysql://localhost:3306/cc_etl', 'com.mysql.cj.jdbc.Driver', 1, '2025-08-23 13:50:17', '2025-08-23 13:50:17', 'cc_etl', 'MYSQL', 'cc_etl', NULL, 0);
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
) ENGINE=InnoDB AUTO_INCREMENT=112288 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_log
-- ----------------------------
BEGIN;
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
) ENGINE=InnoDB AUTO_INCREMENT=289 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_log_report
-- ----------------------------
BEGIN;
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (286, '2025-12-21 00:00:00', 0, 0, 0, '2025-12-21 12:29:50', '2025-12-21 12:28:50', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (287, '2025-12-20 00:00:00', 0, 0, 0, '2025-12-21 12:29:50', '2025-12-21 12:28:50', 0);
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`, `is_deleted`) VALUES (288, '2025-12-19 00:00:00', 0, 0, 0, '2025-12-21 12:29:50', '2025-12-21 12:28:50', 0);
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
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_logglue
-- ----------------------------
BEGIN;
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
  `children` varchar(500) DEFAULT NULL COMMENT '孩子节点',
  `node_type` varchar(100) DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  `trigger_status` int DEFAULT NULL COMMENT '节点运行状态',
  PRIMARY KEY (`id`),
  KEY `idx_sort` (`sort`),
  KEY `idx_job_node_job_parent_id` (`job_parent_id`) USING BTREE,
  KEY `idx_job_node_job_id` (`job_id`) USING BTREE,
  KEY `idx_job_node_node_type` (`node_type`) USING BTREE,
  KEY `idx_job_node_parent_type` (`job_parent_id`,`node_type`)
) ENGINE=InnoDB AUTO_INCREMENT=13420 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务节点';

-- ----------------------------
-- Records of job_node
-- ----------------------------
BEGIN;
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (12339, 24734, 696.5, 150, 0, 0, 1, 0, NULL, NULL, NULL, '2025-08-23 13:43:56', '2025-08-26 16:36:23', 24733, '{\"jobId\":24734,\"width\":160,\"height\":90}', NULL, 'SQL', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (12348, 24743, 472.5, 80, 0, 0, 1, 1, NULL, NULL, NULL, '2025-08-23 13:55:00', '2025-08-26 16:36:23', 24733, '{\"jobId\":24743,\"width\":160,\"height\":90}', NULL, 'SQL', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (12349, 24744, 824.5, 371, 0, 0, 1, 0, NULL, NULL, NULL, '2025-08-23 13:55:00', '2025-08-26 16:36:23', 24733, '{\"jobId\":24744,\"width\":160,\"height\":90}', NULL, 'SQL', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (12350, 24745, 161.5, 124, 0, 0, 0, 5, NULL, NULL, NULL, '2025-08-23 13:55:00', '2025-08-26 16:36:23', 24733, '{\"jobId\":24745,\"width\":160,\"height\":90}', NULL, 'SQL', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (12351, 24746, 407.5, 182, 0, 0, 1, 1, NULL, NULL, NULL, '2025-08-23 13:55:00', '2025-08-26 16:36:23', 24733, '{\"jobId\":24746,\"width\":160,\"height\":90}', NULL, 'SQL', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (12352, 24747, 566.5, 393, 0, 0, 1, 1, NULL, NULL, NULL, '2025-08-23 13:55:00', '2025-08-26 16:36:23', 24733, '{\"jobId\":24747,\"width\":160,\"height\":90}', NULL, 'SQL', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (12353, 24748, 311.5, 430, 0, 0, 1, 0, NULL, NULL, NULL, '2025-08-23 13:55:00', '2025-08-26 16:36:23', 24733, '{\"jobId\":24748,\"width\":160,\"height\":90}', NULL, 'SQL', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (12354, 24749, 776.5, 260.5, 0, 0, 1, 0, NULL, NULL, NULL, '2025-08-23 13:55:00', '2025-08-26 16:36:23', 24733, '{\"jobId\":24749,\"width\":160,\"height\":90}', NULL, 'SQL', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (12355, 24750, 110, 300, 0, 0, 1, 0, NULL, NULL, NULL, '2025-08-23 13:55:00', '2025-08-26 16:36:23', 24733, '{\"jobId\":24750,\"width\":160,\"height\":90}', NULL, 'SQL', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13054, 25456, 1040.5, 360, 0, 0, 0, 0, NULL, NULL, NULL, '2025-11-16 14:25:37', '2025-11-16 20:45:43', 25455, '{\"jobId\":25456,\"width\":160,\"height\":90}', NULL, 'custom-bean', 0, -1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13055, 25457, 819.5, 549, 0, 0, 0, 0, NULL, NULL, NULL, '2025-11-16 14:25:37', '2025-11-16 20:45:43', 25455, '{\"jobId\":25457,\"width\":160,\"height\":90}', NULL, 'custom-api', 0, -1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13056, 25458, 208.5, 757.5, 0, 0, 0, 0, NULL, NULL, NULL, '2025-11-16 14:25:37', '2025-11-16 20:45:43', 25455, '{\"jobId\":25458,\"width\":160,\"height\":90}', NULL, 'custom-shell', 0, -1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13057, 25459, 140.5, 749.5, 0, 0, 0, 0, NULL, NULL, NULL, '2025-11-16 14:25:37', '2025-11-16 20:45:43', 25455, '{\"jobId\":25459,\"width\":160,\"height\":90}', NULL, 'custom-bean', 0, -1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13058, 25460, 127.5, 664.5, 0, 0, 0, 0, NULL, NULL, NULL, '2025-11-16 14:25:37', '2025-11-16 20:45:43', 25455, '{\"jobId\":25460,\"width\":160,\"height\":90}', NULL, 'custom-sql', 0, -1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13093, 25501, 87, 80.5, 0, 0, 0, 1, NULL, NULL, NULL, '2025-11-21 22:39:18', '2025-11-21 22:39:18', 25500, '{\"jobId\":25501,\"width\":160,\"height\":90}', NULL, 'Bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13094, 25502, 356.5, 85, 0, 0, 1, 2, NULL, NULL, NULL, '2025-11-21 22:39:19', '2025-11-21 22:39:19', 25500, '{\"jobId\":25502,\"width\":160,\"height\":90}', NULL, 'Bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13361, 25781, 298.5, 269, 0, 0, 1, 1, NULL, NULL, NULL, '2025-11-22 15:38:09', '2025-11-22 15:38:09', 25500, '{\"jobId\":25781,\"width\":160,\"height\":90}', NULL, 'custom-bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13362, 25782, 681.5, 80, 0, 0, 1, 1, NULL, NULL, NULL, '2025-11-22 15:38:12', '2025-11-22 15:38:12', 25500, '{\"jobId\":25782,\"width\":160,\"height\":90}', NULL, 'custom-bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13363, 25783, 922.5, 224, 0, 0, 1, 0, NULL, NULL, NULL, '2025-11-22 15:38:15', '2025-11-22 15:38:15', 25500, '{\"jobId\":25783,\"width\":160,\"height\":90}', NULL, 'custom-bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13364, 25784, 589.5, 298, 0, 0, 2, 1, NULL, NULL, NULL, '2025-11-22 15:38:17', '2025-11-22 15:38:17', 25500, '{\"jobId\":25784,\"width\":160,\"height\":90}', NULL, 'custom-bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13365, 25786, 138, 170.5, 0, 0, 0, 2, NULL, NULL, NULL, '2025-11-22 15:46:29', '2025-11-22 15:46:29', 25785, '{\"jobId\":25786,\"width\":160,\"height\":90}', NULL, 'Bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13366, 25787, 729.5, 292, 0, 0, 2, 1, NULL, NULL, NULL, '2025-11-22 15:46:32', '2025-11-22 15:46:32', 25785, '{\"jobId\":25787,\"width\":160,\"height\":90}', NULL, 'Bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13367, 25788, 407.5, 126, 0, 0, 1, 2, NULL, NULL, NULL, '2025-11-22 15:46:33', '2025-11-22 15:46:33', 25785, '{\"jobId\":25788,\"width\":160,\"height\":90}', NULL, 'custom-bean', 0, 0);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13368, 25789, 394.5, 292, 0, 0, 1, 1, NULL, NULL, NULL, '2025-11-22 15:46:34', '2025-11-22 15:46:34', 25785, '{\"jobId\":25789,\"width\":160,\"height\":90}', NULL, 'custom-bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13369, 25790, 683.5, 123, 0, 0, 1, 1, NULL, NULL, NULL, '2025-11-22 15:46:35', '2025-11-22 15:46:35', 25785, '{\"jobId\":25790,\"width\":160,\"height\":90}', NULL, 'Bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13370, 25791, 988.5, 182.5, 0, 0, 2, 0, NULL, NULL, NULL, '2025-11-22 15:46:59', '2025-11-22 15:46:59', 25785, '{\"jobId\":25791,\"width\":160,\"height\":90}', NULL, 'Bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13377, 25799, 0, 0, 0, 0, 0, 0, NULL, NULL, NULL, '2025-12-20 09:27:09', '2025-12-20 09:27:09', NULL, '{\"jobId\":25799,\"width\":160,\"height\":90}', NULL, 'custom-bean', 0, -1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13409, 25837, 326, 451, 0, 0, 0, 1, NULL, NULL, NULL, '2025-12-21 09:09:22', '2025-12-21 09:09:22', 25836, '{\"jobId\":25837,\"width\":160,\"height\":90}', NULL, 'Bean', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13410, 25838, 571, 451, 0, 0, 1, 2, NULL, NULL, NULL, '2025-12-21 09:10:10', '2025-12-21 09:10:10', 25836, '{\"jobId\":25838,\"width\":160,\"height\":90}', NULL, 'API', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13411, 25839, 847, 595.5, 0, 0, 1, 1, NULL, NULL, NULL, '2025-12-21 09:10:32', '2025-12-21 09:10:32', 25836, '{\"jobId\":25839,\"width\":160,\"height\":90}', NULL, 'Shell', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13412, 25840, 1039, 365.3333333333333, 0, 0, 1, 1, NULL, NULL, NULL, '2025-12-21 09:10:34', '2025-12-21 09:10:34', 25836, '{\"jobId\":25840,\"width\":160,\"height\":90}', NULL, 'Java', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13413, 25841, 1279, 365.3333333333333, 0, 0, 1, 1, NULL, NULL, NULL, '2025-12-21 09:10:36', '2025-12-21 09:10:36', 25836, '{\"jobId\":25841,\"width\":160,\"height\":90}', NULL, 'Python', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13414, 25842, 820, 365.3333333333333, 0, 0, 1, 1, NULL, NULL, NULL, '2025-12-21 09:10:38', '2025-12-21 09:10:38', 25836, '{\"jobId\":25842,\"width\":160,\"height\":90}', NULL, 'SQL', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13415, 25843, 1203, 595.5, 0, 0, 2, 1, NULL, NULL, NULL, '2025-12-21 09:10:51', '2025-12-21 09:10:51', 25836, '{\"jobId\":25843,\"width\":160,\"height\":90}', NULL, 'Node', 0, 1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13417, 25846, 366, 287, 0, 0, 0, 0, NULL, NULL, NULL, '2025-12-21 09:18:33', '2025-12-21 09:18:33', 25845, '{\"jobId\":25846,\"width\":160,\"height\":90}', NULL, 'Bean', 0, -1);
INSERT INTO `job_node` (`id`, `job_id`, `node_position_x`, `node_position_y`, `node_width`, `node_height`, `node_in_degree`, `node_out_degree`, `node_parent_id`, `background_color`, `sort`, `create_time`, `update_time`, `job_parent_id`, `properties`, `children`, `node_type`, `is_deleted`, `trigger_status`) VALUES (13419, 25848, 1525, 596, 0, 0, 1, 0, NULL, NULL, NULL, '2025-12-21 11:36:17', '2025-12-21 11:36:17', 25836, '{\"jobId\":25848,\"width\":160,\"height\":90}', NULL, 'C#', 0, 1);
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
  `result_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行结果数据（JSON格式，用于兼容小数据或作为后备）',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径（相对路径，相对于basePath）',
  `data_size` bigint DEFAULT NULL COMMENT '数据大小（字节）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_task_group_batch` (`task_group_id`, `execution_batch_id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_execution_batch_id` (`execution_batch_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_file_path` (`file_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节点执行结果表';

-- ----------------------------
-- Records of job_node_result
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
) ENGINE=InnoDB AUTO_INCREMENT=85 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务组分区';

-- ----------------------------
-- Records of job_part
-- ----------------------------
BEGIN;
INSERT INTO `job_part` (`id`, `job_part_name`, `sort`, `create_time`, `update_time`, `is_deleted`) VALUES (55, '任务分区1', 1, '2025-07-20 09:44:40', '2025-07-20 09:44:40', 0);
INSERT INTO `job_part` (`id`, `job_part_name`, `sort`, `create_time`, `update_time`, `is_deleted`) VALUES (64, '任务分区2', 0, '2025-12-21 07:43:05', '2025-12-21 07:43:05', 0);
INSERT INTO `job_part` (`id`, `job_part_name`, `sort`, `create_time`, `update_time`, `is_deleted`) VALUES (65, '任务分区3', 0, '2025-12-21 07:47:29', '2025-12-21 07:47:29', 0);
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
) ENGINE=InnoDB AUTO_INCREMENT=774 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_registry
-- ----------------------------
BEGIN;
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (147, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.233:10000/', '2024-11-23 18:30:52', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (193, 'EXECUTOR', 'datax-executor', 'http://192.168.31.119:12000/', '2024-12-17 22:58:20', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (275, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.119:10000/', '2024-12-29 14:01:05', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (303, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.26:10000/', '2025-01-12 14:45:39', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (349, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.247:13000/', '2025-02-06 18:14:41', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (440, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.86:13000/', '2025-02-28 21:29:42', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (444, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.186:13000/', '2025-03-15 09:56:43', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (449, 'EXECUTOR', 'cc-job-executor', 'http://192.168.31.186:12000/', '2025-03-15 09:56:43', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (575, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.236:13000/', '2025-03-30 20:19:33', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (577, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.197:13000/', '2025-05-08 00:43:59', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (581, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.50:13000/', '2025-05-22 18:55:29', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (583, 'EXECUTOR', 'cc-job-executor', 'http://192.168.31.18:12000/', '2025-06-07 20:03:03', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (584, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.18:13000/', '2025-06-21 10:22:14', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (585, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.200.63:13000/', '2025-06-27 11:42:28', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (586, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.200.63:13000/', '2025-06-27 11:42:28', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (587, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.0.102:13000/', '2025-06-27 19:19:36', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (595, 'EXECUTOR', 'cc-job-executor', 'http://192.168.200.63:12000/', '2025-07-02 08:25:10', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (596, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.1.14:13000/', '2025-07-10 17:50:21', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (597, 'EXECUTOR', 'cc-job-executor', 'http://198.18.0.1:12000/', '2025-07-12 17:10:08', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (617, 'EXECUTOR', 'xxl-job-executor-sample', 'http://198.18.0.1:13000/', '2025-07-14 11:09:16', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (641, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.63:13000/', '2025-07-21 16:48:42', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (644, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.157:13000/', '2025-08-20 09:17:41', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (647, 'EXECUTOR', 'cc-job-executor', 'http://192.168.31.66:12000/', '2025-08-30 11:10:52', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (648, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.66:13000/', '2025-08-30 11:10:52', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (652, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.1.3:13000/', '2025-11-01 22:27:18', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (659, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.1.8:13000/', '2025-11-15 16:11:56', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (669, 'EXECUTOR', 'cc-job-executor', 'http://192.168.1.7:12000/', '2025-11-22 10:29:26', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (672, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.1.7:13000/', '2025-11-23 09:06:14', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (693, 'EXECUTOR', 'cc-job-executor', 'http://192.168.1.13:12000/', '2025-11-30 12:55:07', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (694, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.1.13:13000/', '2025-11-30 12:55:14', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (703, 'EXECUTOR', 'cc-job-executor', 'http://172.20.10.2:12000/', '2025-12-04 23:13:28', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (704, 'EXECUTOR', 'xxl-job-executor-sample', 'http://172.20.10.2:13000/', '2025-12-08 20:19:18', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (732, 'EXECUTOR', 'cc-job-executor-compose', 'http://192.168.1.12:15000/', '2025-12-02 23:16:20', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (733, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.1.12:13000/', '2025-12-02 23:15:57', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (734, 'EXECUTOR', 'cc-job-executor-compose', 'http://172.20.10.2:15000/', '2025-12-04 23:42:29', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (746, 'EXECUTOR', 'cc-job-executor-compose', 'http://192.168.1.2:15000/', '2025-12-08 15:06:16', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (747, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.1.2:13000/', '2025-12-08 15:06:22', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (769, 'EXECUTOR', 'cc-job-executor-compose', 'http://192.168.1.6:15000/', '2025-12-21 12:29:44', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (772, 'EXECUTOR', 'cc-job-executor', 'http://192.168.1.6:12000/', '2025-12-21 12:29:32', NULL, 0);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`, `is_deleted`) VALUES (773, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.1.6:13000/', '2025-12-21 12:29:41', NULL, 0);
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
INSERT INTO `job_user` (`id`, `username`, `password`, `ex1`, `ex2`, `create_time`, `update_time`, `is_deleted`) VALUES (2, 'guest', '$2a$10$xiwlvTUZ2wP3remxQ8SKEew7jlHc7aGhUDOGlZ2iy.bIC0rsWSZNO', NULL, NULL, '2025-11-01 21:41:37', '2025-11-01 21:41:37', 0);
INSERT INTO `job_user` (`id`, `username`, `password`, `ex1`, `ex2`, `create_time`, `update_time`, `is_deleted`) VALUES (3, 'user01', '$2a$10$xiwlvTUZ2wP3remxQ8SKEew7jlHc7aGhUDOGlZ2iy.bIC0rsWSZNO', NULL, NULL, '2025-11-10 23:19:56', '2025-11-10 23:19:56', 0);
INSERT INTO `job_user` (`id`, `username`, `password`, `ex1`, `ex2`, `create_time`, `update_time`, `is_deleted`) VALUES (4, 'user02', '$2a$10$xiwlvTUZ2wP3remxQ8SKEew7jlHc7aGhUDOGlZ2iy.bIC0rsWSZNO', NULL, NULL, '2025-11-11 23:55:31', '2025-11-11 23:55:31', 0);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

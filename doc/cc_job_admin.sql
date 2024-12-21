/*
 Navicat Premium Data Transfer

 Source Server         : 本地mysql
 Source Server Type    : MySQL
 Source Server Version : 80300 (8.3.0)
 Source Host           : localhost:3307
 Source Schema         : cc_job_admin

 Target Server Type    : MySQL
 Target Server Version : 80300 (8.3.0)
 File Encoding         : 65001

 Date: 21/12/2024 09:36:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for job_edge
-- ----------------------------
DROP TABLE IF EXISTS `job_edge`;
CREATE TABLE `job_edge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `from_node_id` bigint DEFAULT NULL COMMENT 'from节点',
  `end_node_id` bigint DEFAULT NULL COMMENT 'end节点',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `task_parent_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=228 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务边';

-- ----------------------------
-- Records of job_edge
-- ----------------------------
BEGIN;
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `task_parent_id`) VALUES (224, 173, 174, '2024-12-20 20:12:02', 272);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `task_parent_id`) VALUES (225, 173, 175, '2024-12-20 20:12:02', 272);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `task_parent_id`) VALUES (226, 174, 176, '2024-12-20 20:12:02', 272);
INSERT INTO `job_edge` (`id`, `from_node_id`, `end_node_id`, `create_time`, `task_parent_id`) VALUES (227, 175, 176, '2024-12-20 20:12:02', 272);
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_group
-- ----------------------------
BEGIN;
INSERT INTO `job_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`, `create_time`) VALUES (1, 'xxl-job-executor-sample', '示例执行器', 1, 'http://127.0.0.1:10000/', '2024-12-20 18:39:46', '2024-11-08 21:38:55');
INSERT INTO `job_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`, `create_time`) VALUES (6, 'job-group-executor', '任务集执行器', 1, 'http://127.0.0.1:9999/', '2024-12-20 18:40:05', '2024-11-16 09:49:27');
INSERT INTO `job_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`, `create_time`) VALUES (8, 'cc-job-executor', 'cc-job-executor', 1, 'http://127.0.0.1:12000/', '2024-12-20 18:40:30', '2024-12-16 21:45:50');
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
  `executor_handler` varchar(255) DEFAULT NULL COMMENT '执行器任务handler',
  `executor_param` varchar(512) DEFAULT NULL COMMENT '执行器任务参数',
  `executor_block_strategy` varchar(50) DEFAULT NULL COMMENT '阻塞处理策略',
  `executor_timeout` int NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
  `executor_fail_retry_count` int NOT NULL DEFAULT '0' COMMENT '失败重试次数',
  `glue_type` varchar(50) NOT NULL COMMENT 'GLUE类型',
  `glue_source` mediumtext COMMENT 'GLUE源代码',
  `glue_remark` varchar(128) DEFAULT NULL COMMENT 'GLUE备注',
  `glue_updatetime` datetime DEFAULT NULL COMMENT 'GLUE更新时间',
  `child_jobid` varchar(255) DEFAULT NULL COMMENT '子任务ID，多个逗号分隔',
  `trigger_status` tinyint NOT NULL DEFAULT '0' COMMENT '调度状态：0-停止，1-运行',
  `trigger_last_time` bigint NOT NULL DEFAULT '0' COMMENT '上次调度时间',
  `trigger_next_time` bigint NOT NULL DEFAULT '0' COMMENT '下次调度时间',
  `create_time` datetime DEFAULT NULL,
  `job_type` int DEFAULT '0',
  `parent_id` bigint DEFAULT '0',
  `req_type` varchar(100) DEFAULT NULL COMMENT '请求类型',
  `req_header` varchar(1000) DEFAULT NULL COMMENT '请求头',
  `req_body` varchar(3000) DEFAULT NULL COMMENT '请求体',
  `req_url` varchar(1000) DEFAULT NULL COMMENT '请求地址',
  `is_node` varchar(10) DEFAULT NULL COMMENT '当前任务是否是节点',
  `rank_trigger_status` tinyint NOT NULL DEFAULT '0',
  `jdbc_datasource_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=278 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_info
-- ----------------------------
BEGIN;
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (33, 1, 'demoJobHandler1', '2024-11-16 09:38:55', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler1', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:38:55', NULL, 0, 0, 0, '2024-11-16 09:38:55', 0, 0, NULL, NULL, NULL, NULL, 'N', 1, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (34, 1, 'demoJobHandler2', '2024-11-16 09:39:05', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler2', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:05', NULL, 0, 0, 0, '2024-11-16 09:39:05', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (35, 1, 'demoJobHandler3', '2024-11-16 09:39:13', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler3', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:13', NULL, 0, 0, 0, '2024-11-16 09:39:13', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (36, 1, 'demoJobHandler4', '2024-11-16 09:39:20', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler4', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:20', NULL, 0, 0, 0, '2024-11-16 09:39:20', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (37, 1, 'demoJobHandler5', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler5', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (38, 1, 'demoJobHandler6', '2024-11-16 09:38:55', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler6', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:38:55', NULL, 0, 0, 0, '2024-11-16 09:38:55', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (39, 1, 'demoJobHandler7', '2024-11-16 09:39:05', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler7', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:05', NULL, 0, 0, 0, '2024-11-16 09:39:05', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (40, 1, 'demoJobHandler8', '2024-11-16 09:39:13', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler8', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:13', NULL, 0, 0, 0, '2024-11-16 09:39:13', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (41, 1, 'demoJobHandler9', '2024-11-16 09:39:20', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler9', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:20', NULL, 0, 0, 0, '2024-11-16 09:39:20', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (42, 1, 'demoJobHandler10', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler10', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (43, 1, 'demoJobHandler11', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler11', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (44, 1, 'demoJobHandler12', '2024-11-16 09:38:55', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler12', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:38:55', NULL, 0, 0, 0, '2024-11-16 09:38:55', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (45, 1, 'demoJobHandler13', '2024-11-16 09:39:05', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler13', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:05', NULL, 0, 0, 0, '2024-11-16 09:39:05', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (46, 1, 'demoJobHandler14', '2024-11-16 09:39:13', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler14', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:13', NULL, 0, 0, 0, '2024-11-16 09:39:13', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (47, 1, 'demoJobHandler15', '2024-11-16 09:39:20', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler15', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:20', NULL, 0, 0, 0, '2024-11-16 09:39:20', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (48, 1, 'demoJobHandler16', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler16', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (49, 1, 'demoJobHandler17', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler17', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (50, 1, 'demoJobHandler18', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler18', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (51, 1, 'demoJobHandler19', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler19', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (52, 1, 'demoJobHandler20', '2024-11-16 09:39:27', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler20', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:27', NULL, 0, 0, 0, '2024-11-16 09:39:27', 0, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (271, 8, '测试数据同步', '2024-12-20 20:06:45', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'runDataxHandler', '{\"job\":{\"content\":[{\"reader\":{\"name\":\"mysqlreader\",\"parameter\":{\"username\":\"root\",\"password\":\"root\",\"connection\":[{\"jdbcUrl\":[\"jdbc:mysql://localhost:3306/test1\"],\"table\":[\"stu\"]}],\"column\":[\"id\",\"name\"]}},\"writer\":{\"name\":\"mysqlwriter\",\"parameter\":{\"username\":\"root\",\"password\":\"root\",\"connection\":[{\"jdbcUrl\":\"jdbc:mysql://localhost:3306/test2\",\"table\":[\"stu\"]}],\"column\":[\"id\",\"name\"],\"writeMode\":\"update\"}}}],\"setting\":{\"speed\":{\"channel\":3,\"byte\":-1},\"errorLimit\":{\"record\":0,\"percentage\":0.02}}}}', 'SERIAL_EXECUTION', 0, 0, 'DATAX', NULL, NULL, '2024-12-20 20:10:36', NULL, 0, 0, 0, NULL, 0, 0, NULL, NULL, NULL, NULL, 'N', 1, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (272, 6, '测试任务组1', '2024-12-20 20:12:02', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'runJobGroupXxlJob', '272', 'SERIAL_EXECUTION', 60000, 0, 'BEAN', NULL, NULL, '2024-12-20 20:12:02', NULL, 0, 0, 0, '2024-12-20 20:12:02', 2, 0, NULL, NULL, NULL, NULL, 'N', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (273, 1, 'demoJobHandler1', '2024-11-16 09:38:55', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler1', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:38:55', NULL, 0, 0, 0, '2024-11-16 09:38:55', 0, 272, NULL, NULL, NULL, NULL, 'Y', 1, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (274, 1, 'demoJobHandler2', '2024-11-16 09:39:05', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler2', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:05', NULL, 0, 0, 0, '2024-11-16 09:39:05', 0, 272, NULL, NULL, NULL, NULL, 'Y', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (275, 1, 'demoJobHandler3', '2024-11-16 09:39:13', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler3', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:13', NULL, 0, 0, 0, '2024-11-16 09:39:13', 0, 272, NULL, NULL, NULL, NULL, 'Y', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (276, 1, 'demoJobHandler4', '2024-11-16 09:39:20', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler4', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-16 09:39:20', NULL, 0, 0, 0, '2024-11-16 09:39:20', 0, 272, NULL, NULL, NULL, NULL, 'Y', 0, NULL);
INSERT INTO `job_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`, `job_type`, `parent_id`, `req_type`, `req_header`, `req_body`, `req_url`, `is_node`, `rank_trigger_status`, `jdbc_datasource_id`) VALUES (277, 8, 'sql任务1', '2024-12-20 20:17:59', 'xz', NULL, 'CRON', '* * * * * ? *', 'DO_NOTHING', 'FIRST', 'runJobJdbcXxlJob', 'call testPro()', 'SERIAL_EXECUTION', 0, 0, 'SQL', NULL, NULL, '2024-12-20 20:17:59', NULL, 0, 0, 0, '2024-12-20 20:17:59', 0, 0, '', NULL, '', '', 'N', 1, 7);
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
  `datasource` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `database_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='jdbc数据源配置';

-- ----------------------------
-- Records of job_jdbc_datasource
-- ----------------------------
BEGIN;
INSERT INTO `job_jdbc_datasource` (`id`, `datasource_name`, `datasource_group`, `jdbc_username`, `jdbc_password`, `jdbc_url`, `jdbc_driver_class`, `status`, `create_time`, `update_time`, `comments`, `datasource`, `database_name`) VALUES (6, '同步stu1', 'Default', 'root', 'root', 'jdbc:mysql://localhost:3306/test1', 'com.mysql.cj.jdbc.Driver', 1, '2024-12-16 21:44:18', '2024-12-16 21:44:18', '同步stu1', 'MYSQL', 'test1');
INSERT INTO `job_jdbc_datasource` (`id`, `datasource_name`, `datasource_group`, `jdbc_username`, `jdbc_password`, `jdbc_url`, `jdbc_driver_class`, `status`, `create_time`, `update_time`, `comments`, `datasource`, `database_name`) VALUES (7, '同步stu2', 'Default', 'root', 'root', 'jdbc:mysql://localhost:3306/test2', 'com.mysql.cj.jdbc.Driver', 1, '2024-12-16 21:44:18', '2024-12-18 20:23:53', '同步stu1', 'MYSQL', 'test2');
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
  `executor_param` varchar(512) DEFAULT NULL COMMENT '执行器任务参数',
  `executor_sharding_param` varchar(20) DEFAULT NULL COMMENT '执行器任务分片参数，格式如 1/2',
  `executor_fail_retry_count` int NOT NULL DEFAULT '0' COMMENT '失败重试次数',
  `trigger_time` datetime DEFAULT NULL COMMENT '调度-时间',
  `trigger_code` int NOT NULL COMMENT '调度-结果',
  `trigger_msg` text COMMENT '调度-日志',
  `handle_time` datetime DEFAULT NULL COMMENT '执行-时间',
  `handle_code` int NOT NULL COMMENT '执行-状态',
  `handle_msg` text COMMENT '执行-日志',
  `alarm_status` tinyint NOT NULL DEFAULT '0' COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
  PRIMARY KEY (`id`),
  KEY `I_trigger_time` (`trigger_time`),
  KEY `I_handle_code` (`handle_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4398 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_log_report
-- ----------------------------
BEGIN;
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (1, '2024-11-03 00:00:00', 0, 8, 2, '2024-11-03 21:19:57', '2024-11-03 13:44:53');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (2, '2024-11-02 00:00:00', 0, 0, 0, '2024-11-03 21:19:57', '2024-11-03 13:44:53');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (3, '2024-11-01 00:00:00', 0, 0, 0, '2024-11-03 21:19:57', '2024-11-03 13:44:53');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (4, '2024-11-08 00:00:00', 0, 0, 0, '2024-11-10 23:58:25', '2024-11-08 20:08:14');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (5, '2024-11-07 00:00:00', 0, 0, 0, '2024-11-09 23:55:04', '2024-11-08 20:08:14');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (6, '2024-11-06 00:00:00', 0, 0, 0, '2024-11-08 23:47:42', '2024-11-08 20:08:14');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (7, '2024-11-09 00:00:00', 0, 6, 1, '2024-11-11 23:59:04', '2024-11-09 00:03:51');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (8, '2024-11-10 00:00:00', 0, 25, 403, '2024-11-12 23:47:15', '2024-11-10 00:11:04');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (9, '2024-11-11 00:00:00', 0, 18, 17, '2024-11-13 22:28:02', '2024-11-11 00:23:09');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (10, '2024-11-12 00:00:00', 0, 31, 18, '2024-11-14 22:31:51', '2024-11-12 00:00:04');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (11, '2024-11-13 00:00:00', 0, 0, 0, '2024-11-15 23:49:16', '2024-11-13 00:04:15');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (12, '2024-11-14 00:00:00', 0, 0, 0, '2024-11-16 22:22:17', '2024-11-14 19:15:06');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (13, '2024-11-15 00:00:00', 0, 0, 0, '2024-11-17 23:59:24', '2024-11-15 19:46:28');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (14, '2024-11-16 00:00:00', 0, 0, 0, '2024-11-18 23:59:37', '2024-11-16 00:05:41');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (15, '2024-11-17 00:00:00', 0, 47, 5, '2024-11-19 23:59:31', '2024-11-17 07:54:26');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (16, '2024-11-18 00:00:00', 0, 0, 0, '2024-11-20 23:59:52', '2024-11-18 00:00:24');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (17, '2024-11-19 00:00:00', 0, 33, 3, '2024-11-21 23:59:38', '2024-11-19 00:00:37');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (18, '2024-11-20 00:00:00', 0, 29, 9, '2024-11-22 23:47:02', '2024-11-20 00:00:31');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (19, '2024-11-21 00:00:00', 0, 0, 0, '2024-11-23 23:58:58', '2024-11-21 00:00:52');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (20, '2024-11-22 00:00:00', 0, 4, 1, '2024-11-24 12:26:57', '2024-11-22 00:00:38');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (21, '2024-11-23 00:00:00', 0, 249, 146, '2024-11-24 12:26:57', '2024-11-23 00:03:02');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (22, '2024-11-24 00:00:00', 0, 242, 260, '2024-11-24 12:26:57', '2024-11-24 00:00:27');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (23, '2024-11-30 00:00:00', 0, 122, 20, '2024-12-01 16:02:58', '2024-11-30 14:22:25');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (24, '2024-11-29 00:00:00', 0, 0, 0, '2024-12-01 16:02:58', '2024-11-30 14:22:25');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (25, '2024-11-28 00:00:00', 0, 0, 0, '2024-11-30 21:38:31', '2024-11-30 14:22:25');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (26, '2024-12-01 00:00:00', 0, 6, 0, '2024-12-03 23:59:37', '2024-12-01 14:58:04');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (27, '2024-12-03 00:00:00', 0, 26, 16, '2024-12-05 22:31:21', '2024-12-03 19:58:24');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (28, '2024-12-02 00:00:00', 0, 0, 0, '2024-12-04 23:00:21', '2024-12-03 19:58:24');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (29, '2024-12-04 00:00:00', 0, 45, 7, '2024-12-06 21:52:16', '2024-12-04 00:00:37');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (30, '2024-12-05 00:00:00', 0, 159, 46, '2024-12-06 21:52:16', '2024-12-05 18:38:41');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (31, '2024-12-06 00:00:00', 0, 58, 33, '2024-12-06 21:52:16', '2024-12-06 18:56:49');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (32, '2024-12-16 00:00:00', 0, 12, 5, '2024-12-18 23:23:02', '2024-12-16 21:18:29');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (33, '2024-12-15 00:00:00', 0, 0, 0, '2024-12-17 22:58:18', '2024-12-16 21:18:29');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (34, '2024-12-14 00:00:00', 0, 0, 0, '2024-12-16 23:59:30', '2024-12-16 21:18:29');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (35, '2024-12-17 00:00:00', 0, 7, 5, '2024-12-19 23:00:52', '2024-12-17 00:00:30');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (36, '2024-12-18 00:00:00', 0, 0, 0, '2024-12-20 23:59:53', '2024-12-18 20:00:46');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (37, '2024-12-19 00:00:00', 0, 0, 0, '2024-12-21 09:34:58', '2024-12-19 18:11:46');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (38, '2024-12-20 00:00:00', 0, 0, 0, '2024-12-21 09:34:58', '2024-12-20 18:34:36');
INSERT INTO `job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (39, '2024-12-21 00:00:00', 0, 0, 0, '2024-12-21 09:34:58', '2024-12-21 00:00:53');
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_logglue
-- ----------------------------
BEGIN;
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (1, 77, 'GLUE_GROOVY', 'package com.xxl.job.service.handler;\n\nimport com.xxl.job.core.context.XxlJobHelper;\nimport com.xxl.job.core.handler.IJobHandler;\nimport org.springframework.beans.factory.annotation.Autowired;\nimport com.xxl.job.executor.service.jobhandler.TestService;\npublic class DemoGlueJobHandler extends IJobHandler {\n  \n   @Autowired\n    private TestService testService; \n\n	@Override\n	public void execute() throws Exception {\n		XxlJobHelper.log(\"XXL-JOB, Hello World.\");\n      testService.test1();\n	}\n\n}\n', 'testGlue', NULL, NULL);
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (2, 77, 'GLUE_GROOVY', 'package com.xxl.job.service.handler;\n\nimport com.xxl.job.core.context.XxlJobHelper;\nimport com.xxl.job.core.handler.IJobHandler;\nimport org.springframework.beans.factory.annotation.Autowired;\nimport com.xxl.job.executor.service.jobhandler.TestService;\npublic class DemoGlueJobHandler extends IJobHandler {\n  \n   @Autowired\n    private TestService testService; \n\n	@Override\n	public void execute() throws Exception {\n		XxlJobHelper.log(\"XXL-JOB, Hello World.\");\n      testService.test1();\n	  testService.test2();\n	}\n\n}\n', '测试版本2', '2024-11-16 21:03:42', '2024-11-16 21:03:42');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (3, 77, 'GLUE_GROOVY', 'package com.xxl.job.service.handler;\n\nimport com.xxl.job.core.context.XxlJobHelper;\nimport com.xxl.job.core.handler.IJobHandler;\nimport org.springframework.beans.factory.annotation.Autowired;\nimport com.xxl.job.executor.service.jobhandler.TestService;\npublic class DemoGlueJobHandler extends IJobHandler {\n  \n   @Autowired\n    private TestService testService; \n\n	@Override\n	public void execute() throws Exception {\n		XxlJobHelper.log(\"XXL-JOB, Hello World.\");\n      testService.test1();\n	}\n\n}\n', '3333', '2024-11-16 21:08:13', '2024-11-16 21:08:13');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (4, 79, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\"\n\necho \"脚本位置：$0\"\necho \"任务参数：$1\"\necho \"分片序号 = $2\"\necho \"分片总数 = $3\"\n\necho \"Good bye!\"\nexit 0\n', '第一次shell', '2024-11-17 13:12:30', '2024-11-17 13:12:30');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (5, 79, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\"\n\necho \"脚本位置：$0\"\necho \"任务参数：$1\"\necho \"分片序号 = $2\"\necho \"分片总数 = $3\"\n\necho \"Good bye!\"\necho $(date \"+%Y-%m-%d %H:%M:%S\") \"hello, shell\" >> log.txt\nexit 0\n', '写入日志测试', '2024-11-17 13:22:50', '2024-11-17 13:22:50');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (6, 79, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\"\n\nsh ./test.sh\necho \"Good bye!\"\nexit 0\n', '333', '2024-11-17 13:36:29', '2024-11-17 13:36:29');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (7, 79, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\"\n\nsh test.sh\n\necho \"Good bye!\"\nexit 0\n', '444', '2024-11-17 13:37:08', '2024-11-17 13:37:08');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (8, 79, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\"\n\nsh /Users/zhaowenpeng/logs/gluesource/test.sh  \n\necho \"Good bye!\"\nexit 0\n', '555', '2024-11-17 13:39:11', '2024-11-17 13:39:11');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (9, 79, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\"\n\nsh /Users/zhaowenpeng/logs/gluesource/test.sh  >> log.txt\n\necho \"Good bye!\"\nexit 0\n', '666', '2024-11-17 13:40:21', '2024-11-17 13:40:21');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (10, 79, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\"\n\necho \"Good bye!\"\nexit 0\n', '888', '2024-11-17 13:41:10', '2024-11-17 13:41:10');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (11, 79, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\"\n\nsh /Users/zhaowenpeng/logs/gluesource/test.sh  >> log.txt\n\necho \"Good bye!\"\nexit 0\n', '000000', '2024-11-17 13:44:31', '2024-11-17 13:44:31');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (12, 79, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\" >> log.txt\n\necho \"Good bye!\"  >> log.txt\nexit 0\n', '2222', '2024-11-17 13:49:13', '2024-11-17 13:49:13');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (13, 79, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\" >> /Users/zhaowenpeng/logs/gluesource/log.txt\n\necho \"Good bye!\"  >> /Users/zhaowenpeng/logs/gluesource/log.txt\nexit 0\n', '3333', '2024-11-17 13:50:16', '2024-11-17 13:50:16');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (14, 80, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\" >> /Users/zhaowenpeng/logs/gluesource/log.txt\n\necho $(date \"+%Y-%m-%d %H:%M:%S\") \"hello, log\" >> /Users/zhaowenpeng/logs/gluesource/log.txt\n\necho \"Good bye!\"  >> /Users/zhaowenpeng/logs/gluesource/log.txt\nexit 0\n', '测试shell任务1', '2024-11-17 13:54:18', '2024-11-17 13:54:18');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (15, 80, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\" >> /Users/zhaowenpeng/logs/gluesource/log.txt\n\necho $(date \"+%Y-%m-%d %H:%M:%S\") \"hello, log\" >> /Users/zhaowenpeng/logs/gluesource/log.txt\n\nexit 0\n', '7777', '2024-11-17 14:03:45', '2024-11-17 14:03:45');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (16, 80, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\" >> /Users/zhaowenpeng/logs/gluesource/log.txt\n\necho $(date \"+%Y-%m-%d %H:%M:%S\") \"hello, log\" >> /Users/zhaowenpeng/logs/gluesource/log.txt\n\necho \"Good bye!\"  >> /Users/zhaowenpeng/logs/gluesource/log.txt\nexit 0\n', '99999', '2024-11-17 14:04:47', '2024-11-17 14:04:47');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (17, 80, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\" >> /Users/zhaowenpeng/logs/gluesource/log.txt\n\necho $(date \"+%Y-%m-%d %H:%M:%S\") \"hello, log\" >> /Users/zhaowenpeng/logs/gluesource/log.txt\n\necho \"Good bye!\"  >> /Users/zhaowenpeng/logs/gluesource/log.txt\nexit 0\n', '232', '2024-11-17 14:08:47', '2024-11-17 14:08:47');
INSERT INTO `job_logglue` (`id`, `job_id`, `glue_type`, `glue_source`, `glue_remark`, `create_time`, `update_time`) VALUES (18, 258, 'GLUE_SHELL', '#!/bin/bash\necho \"xxl-job: hello shell\"\ncurrent_time=$(date)\necho \"$current_time\" >> /Users/zhaowenpeng/Desktop/testShell.log\necho \"Good bye!\"\nexit 0\n', '测试shell', '2024-11-23 22:33:46', '2024-11-23 22:33:46');
COMMIT;

-- ----------------------------
-- Table structure for job_node
-- ----------------------------
DROP TABLE IF EXISTS `job_node`;
CREATE TABLE `job_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `task_id` bigint DEFAULT NULL COMMENT '任务',
  `node_position_x` double DEFAULT NULL COMMENT '节点x',
  `node_position_y` double DEFAULT NULL COMMENT '节点',
  `node_in_degree` bigint DEFAULT '0' COMMENT '入度',
  `node_out_degree` bigint DEFAULT '0' COMMENT '出度',
  `sort` int unsigned DEFAULT NULL COMMENT '排序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `task_parent_id` bigint DEFAULT NULL COMMENT '任务组id',
  PRIMARY KEY (`id`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB AUTO_INCREMENT=177 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务节点';

-- ----------------------------
-- Records of job_node
-- ----------------------------
BEGIN;
INSERT INTO `job_node` (`id`, `task_id`, `node_position_x`, `node_position_y`, `node_in_degree`, `node_out_degree`, `sort`, `create_time`, `update_time`, `task_parent_id`) VALUES (173, 273, 410, 104, 0, 2, NULL, '2024-12-20 20:12:02', '2024-12-20 20:12:02', 272);
INSERT INTO `job_node` (`id`, `task_id`, `node_position_x`, `node_position_y`, `node_in_degree`, `node_out_degree`, `sort`, `create_time`, `update_time`, `task_parent_id`) VALUES (174, 274, 233, 200, 1, 1, NULL, '2024-12-20 20:12:02', '2024-12-20 20:12:02', 272);
INSERT INTO `job_node` (`id`, `task_id`, `node_position_x`, `node_position_y`, `node_in_degree`, `node_out_degree`, `sort`, `create_time`, `update_time`, `task_parent_id`) VALUES (175, 275, 625, 206, 1, 1, NULL, '2024-12-20 20:12:02', '2024-12-20 20:12:02', 272);
INSERT INTO `job_node` (`id`, `task_id`, `node_position_x`, `node_position_y`, `node_in_degree`, `node_out_degree`, `sort`, `create_time`, `update_time`, `task_parent_id`) VALUES (176, 276, 412, 329, 2, 0, NULL, '2024-12-20 20:12:02', '2024-12-20 20:12:02', 272);
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
  PRIMARY KEY (`id`),
  KEY `i_g_k_v` (`registry_group`,`registry_key`,`registry_value`)
) ENGINE=InnoDB AUTO_INCREMENT=203 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of job_registry
-- ----------------------------
BEGIN;
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`) VALUES (147, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.233:10000/', '2024-11-23 18:30:52', NULL);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`) VALUES (193, 'EXECUTOR', 'datax-executor', 'http://192.168.31.119:12000/', '2024-12-17 22:58:20', NULL);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`) VALUES (198, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.119:10000/', '2024-12-21 09:03:06', NULL);
INSERT INTO `job_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`) VALUES (202, 'EXECUTOR', 'cc-job-executor', 'http://192.168.31.119:12000/', '2024-12-21 09:02:53', NULL);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

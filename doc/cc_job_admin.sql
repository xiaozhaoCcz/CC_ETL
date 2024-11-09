/*
 Navicat Premium Data Transfer

 Source Server         : 本地mysql
 Source Server Type    : MySQL
 Source Server Version : 80300 (8.3.0)
 Source Host           : localhost:3306
 Source Schema         : cc_job_admin

 Target Server Type    : MySQL
 Target Server Version : 80300 (8.3.0)
 File Encoding         : 65001

 Date: 09/11/2024 20:09:25
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for gen_config
-- ----------------------------
DROP TABLE IF EXISTS `gen_config`;
CREATE TABLE `gen_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表名',
  `module_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模块名',
  `package_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '包名',
  `business_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务名',
  `entity_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '实体类名',
  `author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作者',
  `parent_menu_id` bigint DEFAULT NULL COMMENT '上级菜单ID，对应sys_menu的id ',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tablename` (`table_name`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码生成基础配置表';

-- ----------------------------
-- Records of gen_config
-- ----------------------------
BEGIN;
INSERT INTO `gen_config` (`id`, `table_name`, `module_name`, `package_name`, `business_name`, `entity_name`, `author`, `parent_menu_id`, `create_time`, `update_time`, `is_deleted`) VALUES (1, 'task_registry', 'task', 'com.cc.job', '执行器', 'TaskRegistry', 'ccjob', NULL, '2024-11-03 08:19:07', '2024-11-03 08:19:07', b'0');
INSERT INTO `gen_config` (`id`, `table_name`, `module_name`, `package_name`, `business_name`, `entity_name`, `author`, `parent_menu_id`, `create_time`, `update_time`, `is_deleted`) VALUES (2, 'task_logglue', 'task', 'com.cc.job', 'task_logglue', 'TaskLogglue', 'ccjob', NULL, '2024-11-03 08:19:58', '2024-11-03 08:19:58', b'0');
INSERT INTO `gen_config` (`id`, `table_name`, `module_name`, `package_name`, `business_name`, `entity_name`, `author`, `parent_menu_id`, `create_time`, `update_time`, `is_deleted`) VALUES (3, 'task_log_report', 'task', 'com.cc.job', 'task_log_report', 'TaskLogReport', 'ccjob', NULL, '2024-11-03 08:20:26', '2024-11-03 08:20:26', b'0');
INSERT INTO `gen_config` (`id`, `table_name`, `module_name`, `package_name`, `business_name`, `entity_name`, `author`, `parent_menu_id`, `create_time`, `update_time`, `is_deleted`) VALUES (4, 'task_log', 'task', 'com.cc.job', 'task_log', 'TaskLog', 'ccjob', NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42', b'0');
INSERT INTO `gen_config` (`id`, `table_name`, `module_name`, `package_name`, `business_name`, `entity_name`, `author`, `parent_menu_id`, `create_time`, `update_time`, `is_deleted`) VALUES (5, 'task_lock', 'task', 'com.cc.job', 'task_lock', 'TaskLock', 'ccjob', NULL, '2024-11-03 08:21:01', '2024-11-03 08:21:01', b'0');
INSERT INTO `gen_config` (`id`, `table_name`, `module_name`, `package_name`, `business_name`, `entity_name`, `author`, `parent_menu_id`, `create_time`, `update_time`, `is_deleted`) VALUES (6, 'task_group', 'task', 'com.cc.job', 'task_group', 'TaskGroup', 'ccjob', NULL, '2024-11-03 08:21:19', '2024-11-03 08:21:19', b'0');
INSERT INTO `gen_config` (`id`, `table_name`, `module_name`, `package_name`, `business_name`, `entity_name`, `author`, `parent_menu_id`, `create_time`, `update_time`, `is_deleted`) VALUES (7, 'task_info', 'task', 'com.cc.job', 'task_info', 'TaskInfo', 'ccjob', NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36', b'0');
COMMIT;

-- ----------------------------
-- Table structure for gen_field_config
-- ----------------------------
DROP TABLE IF EXISTS `gen_field_config`;
CREATE TABLE `gen_field_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_id` bigint NOT NULL COMMENT '关联的配置ID',
  `column_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `column_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `column_length` int DEFAULT NULL,
  `field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字段名称',
  `field_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段类型',
  `field_sort` int DEFAULT NULL COMMENT '字段排序',
  `field_comment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段描述',
  `max_length` int DEFAULT NULL,
  `is_required` tinyint(1) DEFAULT NULL COMMENT '是否必填',
  `is_show_in_list` tinyint(1) DEFAULT '0' COMMENT '是否在列表显示',
  `is_show_in_form` tinyint(1) DEFAULT '0' COMMENT '是否在表单显示',
  `is_show_in_query` tinyint(1) DEFAULT '0' COMMENT '是否在查询条件显示',
  `query_type` tinyint DEFAULT NULL COMMENT '查询方式',
  `form_type` tinyint DEFAULT NULL COMMENT '表单类型',
  `dict_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字典类型',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `config_id` (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码生成字段配置表';

-- ----------------------------
-- Records of gen_field_config
-- ----------------------------
BEGIN;
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (1, 1, 'id', 'int', NULL, 'id', 'Integer', 1, '', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:19:07', '2024-11-03 08:19:07');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (2, 1, 'registry_group', 'varchar', NULL, 'registryGroup', 'String', 2, '', 50, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:19:07', '2024-11-03 08:19:07');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (3, 1, 'registry_key', 'varchar', NULL, 'registryKey', 'String', 3, '', 255, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:19:07', '2024-11-03 08:19:07');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (4, 1, 'registry_value', 'varchar', NULL, 'registryValue', 'String', 4, '', 255, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:19:07', '2024-11-03 08:19:07');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (5, 1, 'update_time', 'datetime', NULL, 'updateTime', 'LocalDateTime', 5, '', NULL, 1, 1, 1, 0, 1, 9, NULL, '2024-11-03 08:19:07', '2024-11-03 08:19:07');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (6, 2, 'id', 'int', NULL, 'id', 'Integer', 1, '', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:19:58', '2024-11-03 08:19:58');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (7, 2, 'job_id', 'int', NULL, 'jobId', 'Integer', 2, '任务，主键ID', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:19:58', '2024-11-03 08:19:58');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (8, 2, 'glue_type', 'varchar', NULL, 'glueType', 'String', 3, 'GLUE类型', 50, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:19:58', '2024-11-03 08:19:58');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (9, 2, 'glue_source', 'mediumtext', NULL, 'glueSource', NULL, 4, 'GLUE源代码', 16777215, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:19:58', '2024-11-03 08:19:58');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (10, 2, 'glue_remark', 'varchar', NULL, 'glueRemark', 'String', 5, 'GLUE备注', 128, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:19:58', '2024-11-03 08:19:58');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (11, 2, 'add_time', 'datetime', NULL, 'addTime', 'LocalDateTime', 6, '', NULL, 1, 1, 1, 0, 1, 9, NULL, '2024-11-03 08:19:58', '2024-11-03 08:19:58');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (12, 2, 'update_time', 'datetime', NULL, 'updateTime', 'LocalDateTime', 7, '', NULL, 1, 1, 1, 0, 1, 9, NULL, '2024-11-03 08:19:58', '2024-11-03 08:19:58');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (13, 3, 'id', 'int', NULL, 'id', 'Integer', 1, '', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:26', '2024-11-03 08:20:26');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (14, 3, 'trigger_day', 'datetime', NULL, 'triggerDay', 'LocalDateTime', 2, '调度-时间', NULL, 1, 1, 1, 0, 1, 9, NULL, '2024-11-03 08:20:26', '2024-11-03 08:20:26');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (15, 3, 'running_count', 'int', NULL, 'runningCount', 'Integer', 3, '运行中-日志数量', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:26', '2024-11-03 08:20:26');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (16, 3, 'suc_count', 'int', NULL, 'sucCount', 'Integer', 4, '执行成功-日志数量', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:26', '2024-11-03 08:20:26');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (17, 3, 'fail_count', 'int', NULL, 'failCount', 'Integer', 5, '执行失败-日志数量', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:26', '2024-11-03 08:20:26');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (18, 3, 'update_time', 'datetime', NULL, 'updateTime', 'LocalDateTime', 6, '', NULL, 1, 1, 1, 0, 1, 9, NULL, '2024-11-03 08:20:26', '2024-11-03 08:20:26');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (19, 4, 'id', 'bigint', NULL, 'id', 'Long', 1, '', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (20, 4, 'job_group', 'int', NULL, 'jobGroup', 'Integer', 2, '执行器主键ID', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (21, 4, 'job_id', 'int', NULL, 'jobId', 'Integer', 3, '任务，主键ID', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (22, 4, 'executor_address', 'varchar', NULL, 'executorAddress', 'String', 4, '执行器地址，本次执行的地址', 255, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (23, 4, 'executor_handler', 'varchar', NULL, 'executorHandler', 'String', 5, '执行器任务handler', 255, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (24, 4, 'executor_param', 'varchar', NULL, 'executorParam', 'String', 6, '执行器任务参数', 512, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (25, 4, 'executor_sharding_param', 'varchar', NULL, 'executorShardingParam', 'String', 7, '执行器任务分片参数，格式如 1/2', 20, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (26, 4, 'executor_fail_retry_count', 'int', NULL, 'executorFailRetryCount', 'Integer', 8, '失败重试次数', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (27, 4, 'trigger_time', 'datetime', NULL, 'triggerTime', 'LocalDateTime', 9, '调度-时间', NULL, 1, 1, 1, 0, 1, 9, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (28, 4, 'trigger_code', 'int', NULL, 'triggerCode', 'Integer', 10, '调度-结果', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (29, 4, 'trigger_msg', 'text', NULL, 'triggerMsg', 'String', 11, '调度-日志', 65535, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (30, 4, 'handle_time', 'datetime', NULL, 'handleTime', 'LocalDateTime', 12, '执行-时间', NULL, 1, 1, 1, 0, 1, 9, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (31, 4, 'handle_code', 'int', NULL, 'handleCode', 'Integer', 13, '执行-状态', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (32, 4, 'handle_msg', 'text', NULL, 'handleMsg', 'String', 14, '执行-日志', 65535, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (33, 4, 'alarm_status', 'tinyint', NULL, 'alarmStatus', 'Integer', 15, '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:20:42', '2024-11-03 08:20:42');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (34, 5, 'lock_name', 'varchar', NULL, 'lockName', 'String', 1, '锁名称', 50, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:01', '2024-11-03 08:21:01');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (35, 6, 'id', 'int', NULL, 'id', 'Integer', 1, '', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:19', '2024-11-03 08:21:19');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (36, 6, 'app_name', 'varchar', NULL, 'appName', 'String', 2, '执行器AppName', 64, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:19', '2024-11-03 08:21:19');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (37, 6, 'title', 'varchar', NULL, 'title', 'String', 3, '执行器名称', 12, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:19', '2024-11-03 08:21:19');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (38, 6, 'address_type', 'tinyint', NULL, 'addressType', 'Integer', 4, '执行器地址类型：0=自动注册、1=手动录入', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:19', '2024-11-03 08:21:19');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (39, 6, 'address_list', 'text', NULL, 'addressList', 'String', 5, '执行器地址列表，多地址逗号分隔', 65535, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:19', '2024-11-03 08:21:19');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (40, 6, 'update_time', 'datetime', NULL, 'updateTime', 'LocalDateTime', 6, '', NULL, 1, 1, 1, 0, 1, 9, NULL, '2024-11-03 08:21:19', '2024-11-03 08:21:19');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (41, 7, 'id', 'int', NULL, 'id', 'Integer', 1, '', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (42, 7, 'job_group', 'int', NULL, 'jobGroup', 'Integer', 2, '执行器主键ID', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (43, 7, 'job_desc', 'varchar', NULL, 'jobDesc', 'String', 3, '', 255, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (44, 7, 'add_time', 'datetime', NULL, 'addTime', 'LocalDateTime', 4, '', NULL, 1, 1, 1, 0, 1, 9, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (45, 7, 'update_time', 'datetime', NULL, 'updateTime', 'LocalDateTime', 5, '', NULL, 1, 1, 1, 0, 1, 9, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (46, 7, 'author', 'varchar', NULL, 'author', 'String', 6, '作者', 64, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (47, 7, 'alarm_email', 'varchar', NULL, 'alarmEmail', 'String', 7, '报警邮件', 255, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (48, 7, 'schedule_type', 'varchar', NULL, 'scheduleType', 'String', 8, '调度类型', 50, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (49, 7, 'schedule_conf', 'varchar', NULL, 'scheduleConf', 'String', 9, '调度配置，值含义取决于调度类型', 128, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (50, 7, 'misfire_strategy', 'varchar', NULL, 'misfireStrategy', 'String', 10, '调度过期策略', 50, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (51, 7, 'executor_route_strategy', 'varchar', NULL, 'executorRouteStrategy', 'String', 11, '执行器路由策略', 50, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (52, 7, 'executor_handler', 'varchar', NULL, 'executorHandler', 'String', 12, '执行器任务handler', 255, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (53, 7, 'executor_param', 'varchar', NULL, 'executorParam', 'String', 13, '执行器任务参数', 512, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (54, 7, 'executor_block_strategy', 'varchar', NULL, 'executorBlockStrategy', 'String', 14, '阻塞处理策略', 50, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (55, 7, 'executor_timeout', 'int', NULL, 'executorTimeout', 'Integer', 15, '任务执行超时时间，单位秒', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (56, 7, 'executor_fail_retry_count', 'int', NULL, 'executorFailRetryCount', 'Integer', 16, '失败重试次数', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (57, 7, 'glue_type', 'varchar', NULL, 'glueType', 'String', 17, 'GLUE类型', 50, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (58, 7, 'glue_source', 'mediumtext', NULL, 'glueSource', NULL, 18, 'GLUE源代码', 16777215, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (59, 7, 'glue_remark', 'varchar', NULL, 'glueRemark', 'String', 19, 'GLUE备注', 128, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (60, 7, 'glue_updatetime', 'datetime', NULL, 'glueUpdatetime', 'LocalDateTime', 20, 'GLUE更新时间', NULL, 1, 1, 1, 0, 1, 9, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (61, 7, 'child_jobid', 'varchar', NULL, 'childJobid', 'String', 21, '子任务ID，多个逗号分隔', 255, 1, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (62, 7, 'trigger_status', 'tinyint', NULL, 'triggerStatus', 'Integer', 22, '调度状态：0-停止，1-运行', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (63, 7, 'trigger_last_time', 'bigint', NULL, 'triggerLastTime', 'Long', 23, '上次调度时间', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
INSERT INTO `gen_field_config` (`id`, `config_id`, `column_name`, `column_type`, `column_length`, `field_name`, `field_type`, `field_sort`, `field_comment`, `max_length`, `is_required`, `is_show_in_list`, `is_show_in_form`, `is_show_in_query`, `query_type`, `form_type`, `dict_type`, `create_time`, `update_time`) VALUES (64, 7, 'trigger_next_time', 'bigint', NULL, 'triggerNextTime', 'Long', 24, '下次调度时间', NULL, 0, 1, 1, 0, 1, 1, NULL, '2024-11-03 08:21:36', '2024-11-03 08:21:36');
COMMIT;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_name` varchar(50) NOT NULL COMMENT '配置名称',
  `config_key` varchar(50) NOT NULL COMMENT '配置key',
  `config_value` varchar(100) NOT NULL COMMENT '配置值',
  `remark` varchar(200) DEFAULT NULL COMMENT '描述、备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` bigint NOT NULL COMMENT '创建人ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint(1) NOT NULL COMMENT '逻辑删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

-- ----------------------------
-- Records of sys_config
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '部门名称',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门编号',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父节点id',
  `tree_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '父节点id路径',
  `sort` smallint DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态(1-正常 0-禁用)',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标识(1-已删除 0-未删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_code` (`code`) USING BTREE COMMENT '部门编号唯一索引'
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='部门表';

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
BEGIN;
INSERT INTO `sys_dept` (`id`, `name`, `code`, `parent_id`, `tree_path`, `sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (1, '有来技术', 'YOULAI', 0, '0', 1, 1, 1, NULL, 1, '2024-06-24 23:48:59', 0);
INSERT INTO `sys_dept` (`id`, `name`, `code`, `parent_id`, `tree_path`, `sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (2, '研发部门', 'RD001', 1, '0,1', 1, 1, 2, NULL, 2, '2022-04-19 12:46:37', 0);
INSERT INTO `sys_dept` (`id`, `name`, `code`, `parent_id`, `tree_path`, `sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (3, '测试部门', 'QA001', 1, '0,1', 1, 1, 2, NULL, 2, '2022-04-19 12:46:37', 0);
COMMIT;

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ',
  `dict_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '类型编码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '类型名称',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态(0:正常;1:禁用)',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '是否删除(1-删除，0-未删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_code` (`dict_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='字典表';

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
BEGIN;
INSERT INTO `sys_dict` (`id`, `dict_code`, `name`, `status`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`, `is_deleted`) VALUES (1, 'gender', '性别', 1, NULL, '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1, 0);
INSERT INTO `sys_dict` (`id`, `dict_code`, `name`, `status`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`, `is_deleted`) VALUES (2, 'notice_type', '通知类型', 1, NULL, '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1, 0);
INSERT INTO `sys_dict` (`id`, `dict_code`, `name`, `status`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`, `is_deleted`) VALUES (3, 'notice_level', '通知级别', 1, NULL, '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1, 0);
COMMIT;

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联字典编码，与sys_dict表中的dict_code对应',
  `value` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典项值',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典项标签',
  `tag_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签类型，用于前端样式展示（如success、warning等）',
  `status` tinyint DEFAULT '0' COMMENT '状态（1-正常，0-禁用）',
  `sort` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='字典数据表';

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
BEGIN;
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (1, 'gender', '1', '男', 'primary', 1, 1, NULL, '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (2, 'gender', '2', '女', 'danger', 1, 2, NULL, '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (3, 'gender', '0', '保密', 'info', 1, 3, NULL, '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (4, 'notice_type', '1', '系统升级', 'success', 1, 1, '', '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (5, 'notice_type', '2', '系统维护', 'primary', 1, 2, '', '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (6, 'notice_type', '3', '安全警告', 'danger', 1, 3, '', '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (7, 'notice_type', '4', '假期通知', 'success', 1, 4, '', '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (8, 'notice_type', '5', '公司新闻', 'primary', 1, 5, '', '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (9, 'notice_type', '99', '其他', 'info', 1, 99, '', '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (10, 'notice_level', 'L', '低', 'info', 1, 1, '', '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (11, 'notice_level', 'M', '中', 'warning', 1, 2, '', '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
INSERT INTO `sys_dict_data` (`id`, `dict_code`, `value`, `label`, `tag_type`, `status`, `sort`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`) VALUES (12, 'notice_level', 'H', '高', 'danger', 1, 3, '', '2024-11-02 13:10:45', 1, '2024-11-02 13:10:45', 1);
COMMIT;

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '日志模块',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '日志内容',
  `request_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求路径',
  `ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IP地址',
  `province` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省份',
  `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市',
  `execution_time` bigint DEFAULT NULL COMMENT '执行时间(ms)',
  `browser` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '浏览器',
  `browser_version` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '浏览器版本',
  `os` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '终端系统',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标识(1-已删除 0-未删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=161 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='系统日志表';

-- ----------------------------
-- Records of sys_log
-- ----------------------------
BEGIN;
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (1, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '127.0.0.1', '0', '内网IP', 39, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-02 21:37:33', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (2, 'USER', '用户分页列表', '/api/v1/users/page', '127.0.0.1', '0', '内网IP', 24, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-02 21:37:36', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (3, 'ROLE', '角色分页列表', '/api/v1/roles/page', '127.0.0.1', '0', '内网IP', 17, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-02 21:37:37', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (4, 'USER', '用户分页列表', '/api/v1/users/page', '127.0.0.1', '0', '内网IP', 17, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-02 21:39:06', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (5, 'LOGIN', '登录', '/api/v1/auth/login', '192.168.31.157', '0', '内网IP', 212, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:24:36', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (6, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 44, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:24:47', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (7, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 27, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:27:26', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (8, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 31, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:27:29', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (9, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 24, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:28:04', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (10, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 24, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:28:27', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (11, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 23, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:28:42', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (12, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 27, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:31:55', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (13, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 21, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:31:57', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (14, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 30, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:33:09', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (15, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 26, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:33:23', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (16, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 20, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:34:21', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (17, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 50, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:35:56', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (18, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 20, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:37:01', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (19, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 25, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:37:35', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (20, 'LOGIN', '注销', '/api/v1/auth/logout', '192.168.31.157', '0', '内网IP', 25, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:38:11', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (21, 'LOGIN', '登录', '/api/v1/auth/login', '192.168.31.157', '0', '内网IP', 147, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:38:16', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (22, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 34, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:38:16', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (23, 'DEPT', '部门列表', '/api/v1/dept', '192.168.31.157', '0', '内网IP', 20, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:38:24', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (24, 'DICT', '字典分页列表', '/api/v1/dict/page', '192.168.31.157', '0', '内网IP', 20, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:38:24', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (25, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '192.168.31.157', '0', '内网IP', 26, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:38:38', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (26, 'USER', '用户分页列表', '/api/v1/users/page', '192.168.31.157', '0', '内网IP', 31, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:39:30', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (27, 'ROLE', '角色分页列表', '/api/v1/roles/page', '192.168.31.157', '0', '内网IP', 19, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:39:31', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (28, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 18, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:39:33', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (29, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 16, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:40:16', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (30, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 22, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:40:35', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (31, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 44, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:41:32', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (32, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 25, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:42:46', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (33, 'LOGIN', '注销', '/api/v1/auth/logout', '192.168.31.157', '0', '内网IP', 2, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:43:15', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (34, 'LOGIN', '登录', '/api/v1/auth/login', '192.168.31.157', '0', '内网IP', 140, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:43:17', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (35, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 34, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:43:17', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (36, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 34, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:44:28', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (37, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 23, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:45:35', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (38, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 30, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:50:36', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (39, 'LOGIN', '登录', '/api/v1/auth/login', '192.168.31.157', '0', '内网IP', 133, 'Chrome', '132.0.0.0', 'OSX', 2, '2024-11-03 07:52:05', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (40, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 15, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:53:36', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (41, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 30, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:54:18', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (42, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 23, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:54:21', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (43, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 37, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:54:37', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (44, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 27, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:54:39', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (45, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 25, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:55:47', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (46, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 52, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:55:54', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (47, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 25, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:56:17', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (48, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 9, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:57:53', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (49, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 28, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:58:01', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (50, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 38, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 07:59:27', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (51, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 40, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:01:59', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (52, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 36, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:02:06', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (53, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 26, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:02:21', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (54, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 29, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:03:23', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (55, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 53, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:03:52', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (56, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 26, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:05:56', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (57, 'DEPT', '部门列表', '/api/v1/dept', '192.168.31.157', '0', '内网IP', 24, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:06:01', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (58, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 24, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:06:02', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (59, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 54, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:07:08', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (60, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 19, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:08:10', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (61, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 25, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:08:36', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (62, 'USER', '用户分页列表', '/api/v1/users/page', '192.168.31.157', '0', '内网IP', 23, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:09:31', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (63, 'ROLE', '角色分页列表', '/api/v1/roles/page', '192.168.31.157', '0', '内网IP', 43, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:09:32', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (64, 'ROLE', '角色分页列表', '/api/v1/roles/page', '192.168.31.157', '0', '内网IP', 13, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:09:48', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (65, 'ROLE', '角色分页列表', '/api/v1/roles/page', '192.168.31.157', '0', '内网IP', 38, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:09:50', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (66, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 18, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:09:54', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (67, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 15, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:10:07', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (68, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 22, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:10:57', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (69, 'ROLE', '角色分页列表', '/api/v1/roles/page', '192.168.31.157', '0', '内网IP', 7, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:11:11', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (70, 'ROLE', '角色分页列表', '/api/v1/roles/page', '192.168.31.157', '0', '内网IP', 34, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:11:14', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (71, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '192.168.31.157', '0', '内网IP', 50, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:17:01', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (72, 'OTHER', '生成代码', '/api/v1/codegen/task_registry/config', '192.168.31.157', '0', '内网IP', 97, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:19:07', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (73, 'OTHER', '预览生成代码', '/api/v1/codegen/task_registry/preview', '192.168.31.157', '0', '内网IP', 219, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:19:07', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (74, 'OTHER', '下载代码', '/api/v1/codegen/task_registry/download', '192.168.31.157', '0', '内网IP', 88, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:19:20', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (75, 'OTHER', '生成代码', '/api/v1/codegen/task_logglue/config', '192.168.31.157', '0', '内网IP', 39, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:19:58', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (76, 'OTHER', '预览生成代码', '/api/v1/codegen/task_logglue/preview', '192.168.31.157', '0', '内网IP', 110, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:19:59', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (77, 'OTHER', '下载代码', '/api/v1/codegen/task_logglue/download', '192.168.31.157', '0', '内网IP', 81, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:20:00', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (78, 'OTHER', '生成代码', '/api/v1/codegen/task_log_report/config', '192.168.31.157', '0', '内网IP', 67, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:20:26', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (79, 'OTHER', '预览生成代码', '/api/v1/codegen/task_log_report/preview', '192.168.31.157', '0', '内网IP', 55, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:20:26', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (80, 'OTHER', '下载代码', '/api/v1/codegen/task_log_report/download', '192.168.31.157', '0', '内网IP', 84, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:20:26', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (81, 'OTHER', '生成代码', '/api/v1/codegen/task_log/config', '192.168.31.157', '0', '内网IP', 80, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:20:42', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (82, 'OTHER', '预览生成代码', '/api/v1/codegen/task_log/preview', '192.168.31.157', '0', '内网IP', 76, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:20:42', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (83, 'OTHER', '下载代码', '/api/v1/codegen/task_log/download', '192.168.31.157', '0', '内网IP', 84, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:20:43', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (84, 'OTHER', '生成代码', '/api/v1/codegen/task_lock/config', '192.168.31.157', '0', '内网IP', 17, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:21:01', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (85, 'OTHER', '预览生成代码', '/api/v1/codegen/task_lock/preview', '192.168.31.157', '0', '内网IP', 75, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:21:02', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (86, 'OTHER', '下载代码', '/api/v1/codegen/task_lock/download', '192.168.31.157', '0', '内网IP', 73, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:21:03', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (87, 'OTHER', '生成代码', '/api/v1/codegen/task_group/config', '192.168.31.157', '0', '内网IP', 33, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:21:19', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (88, 'OTHER', '预览生成代码', '/api/v1/codegen/task_group/preview', '192.168.31.157', '0', '内网IP', 95, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:21:19', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (89, 'OTHER', '下载代码', '/api/v1/codegen/task_group/download', '192.168.31.157', '0', '内网IP', 73, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:21:20', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (90, 'OTHER', '生成代码', '/api/v1/codegen/task_info/config', '192.168.31.157', '0', '内网IP', 42, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:21:36', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (91, 'OTHER', '预览生成代码', '/api/v1/codegen/task_info/preview', '192.168.31.157', '0', '内网IP', 94, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:21:37', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (92, 'OTHER', '下载代码', '/api/v1/codegen/task_info/download', '192.168.31.157', '0', '内网IP', 84, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:21:37', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (93, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '192.168.31.157', '0', '内网IP', 42, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:37:49', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (94, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '192.168.31.157', '0', '内网IP', 33, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:38:04', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (95, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '192.168.31.157', '0', '内网IP', 26, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:38:43', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (96, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '192.168.31.157', '0', '内网IP', 25, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:39:19', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (97, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '192.168.31.157', '0', '内网IP', 35, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:39:48', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (98, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '192.168.31.157', '0', '内网IP', 51, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:40:05', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (99, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '192.168.31.157', '0', '内网IP', 30, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:40:25', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (100, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '192.168.31.157', '0', '内网IP', 28, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:40:42', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (101, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 23, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:41:00', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (102, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 83, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:41:28', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (103, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 22, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:42:11', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (104, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 91, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:43:00', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (105, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 20, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:43:08', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (106, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 42, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:43:50', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (107, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 27, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:44:44', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (108, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 29, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:44:57', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (109, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 32, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:45:58', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (110, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 32, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:47:01', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (111, 'DEPT', '部门列表', '/api/v1/dept', '192.168.31.157', '0', '内网IP', 30, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:49:35', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (112, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 19, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:49:35', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (113, 'ROLE', '角色分页列表', '/api/v1/roles/page', '192.168.31.157', '0', '内网IP', 16, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:49:36', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (114, 'ROLE', '角色分页列表', '/api/v1/roles/page', '192.168.31.157', '0', '内网IP', 41, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:49:44', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (115, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 21, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:52:19', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (116, 'ROLE', '角色分页列表', '/api/v1/roles/page', '192.168.31.157', '0', '内网IP', 14, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:52:20', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (117, 'ROLE', '角色分页列表', '/api/v1/roles/page', '192.168.31.157', '0', '内网IP', 18, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:52:59', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (118, 'USER', '用户分页列表', '/api/v1/users/page', '192.168.31.157', '0', '内网IP', 26, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:53:23', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (119, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 30, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:53:28', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (120, 'DEPT', '部门列表', '/api/v1/dept', '192.168.31.157', '0', '内网IP', 15, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:53:47', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (121, 'DICT', '字典分页列表', '/api/v1/dict/page', '192.168.31.157', '0', '内网IP', 16, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:53:48', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (122, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 28, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:55:05', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (123, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '192.168.31.157', '0', '内网IP', 38, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 08:58:28', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (124, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 68, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 09:00:03', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (125, 'MENU', '菜单列表', '/api/v1/menus', '192.168.31.157', '0', '内网IP', 19, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 09:06:05', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (126, 'LOGIN', '登录', '/api/v1/auth/login', '192.168.31.157', '0', '内网IP', 217, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 09:44:28', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (127, 'LOGIN', '注销', '/api/v1/auth/logout', '192.168.31.157', '0', '内网IP', 48, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 10:03:30', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (128, 'LOGIN', '登录', '/api/v1/auth/login', '192.168.31.157', '0', '内网IP', 168, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 10:03:34', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (129, 'LOGIN', '注销', '/api/v1/auth/logout', '192.168.31.157', '0', '内网IP', 2, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 10:04:24', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (130, 'LOGIN', '登录', '/api/v1/auth/login', '192.168.31.157', '0', '内网IP', 160, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 10:04:27', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (131, 'LOGIN', '登录', '/api/v1/auth/login', '192.168.31.157', '0', '内网IP', 333, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 13:40:30', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (132, 'LOGIN', '注销', '/api/v1/auth/logout', '192.168.31.157', '0', '内网IP', 8, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 14:45:49', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (133, 'LOGIN', '登录', '/api/v1/auth/login', '192.168.31.157', '0', '内网IP', 333, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 15:44:58', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (134, 'LOGIN', '登录', '/api/v1/auth/login', '192.168.31.157', '0', '内网IP', 140, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 19:06:06', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (135, 'LOGIN', '登录', '/api/v1/auth/login', '192.168.31.157', '0', '内网IP', 267, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-03 21:11:01', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (136, 'LOGIN', '登录', '/api/v1/auth/login', '127.0.0.1', '0', '内网IP', 215, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-08 20:10:23', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (137, 'OTHER', '代码生成分页列表', '/api/v1/codegen/table/page', '127.0.0.1', '0', '内网IP', 32, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-08 20:10:26', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (138, 'LOGIN', '登录', '/api/v1/auth/login', '127.0.0.1', '0', '内网IP', 202, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-08 22:11:51', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (139, 'LOGIN', '登录', '/api/v1/auth/login', '127.0.0.1', '0', '内网IP', 204, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 07:22:49', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (140, 'LOGIN', '登录', '/api/v1/auth/login', '127.0.0.1', '0', '内网IP', 282, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 09:22:58', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (141, 'LOGIN', '登录', '/api/v1/auth/login', '127.0.0.1', '0', '内网IP', 213, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 11:24:02', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (142, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 21, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 13:17:32', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (143, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 21, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 13:17:47', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (144, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 20, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 13:17:53', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (145, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 19, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 13:22:20', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (146, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 20, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 13:22:30', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (147, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 16, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 13:22:36', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (148, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 16, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 13:22:42', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (149, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 17, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 13:22:52', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (150, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 16, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 13:22:54', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (151, 'LOGIN', '登录', '/api/v1/auth/login', '127.0.0.1', '0', '内网IP', 316, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 13:24:39', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (152, 'LOGIN', '登录', '/api/v1/auth/login', '127.0.0.1', '0', '内网IP', 368, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 14:43:14', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (153, 'LOGIN', '登录', '/api/v1/auth/login', '127.0.0.1', '0', '内网IP', 401, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 14:59:54', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (154, 'LOGIN', '登录', '/api/v1/auth/login', '127.0.0.1', '0', '内网IP', 426, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 15:00:45', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (155, 'LOGIN', '登录', '/api/v1/auth/login', '127.0.0.1', '0', '内网IP', 223, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 17:43:47', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (156, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 31, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 19:24:06', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (157, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 18, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 19:24:27', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (158, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 20, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 19:24:45', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (159, 'MENU', '菜单列表', '/api/v1/menus', '127.0.0.1', '0', '内网IP', 32, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 19:24:47', 0);
INSERT INTO `sys_log` (`id`, `module`, `content`, `request_uri`, `ip`, `province`, `city`, `execution_time`, `browser`, `browser_version`, `os`, `create_by`, `create_time`, `is_deleted`) VALUES (160, 'LOGIN', '登录', '/api/v1/auth/login', '127.0.0.1', '0', '内网IP', 215, 'MSEdge', '130.0.0.0', 'OSX', 2, '2024-11-09 19:58:11', 0);
COMMIT;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `parent_id` bigint NOT NULL COMMENT '父菜单ID',
  `tree_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父节点ID路径',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '菜单名称',
  `type` tinyint NOT NULL COMMENT '菜单类型（1-菜单 2-目录 3-外链 4-按钮）',
  `route_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由名称（Vue Router 中用于命名路由）',
  `route_path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '路由路径（Vue Router 中定义的 URL 路径）',
  `component` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件路径（组件页面完整路径，相对于 src/views/，缺省后缀 .vue）',
  `perm` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '【按钮】权限标识',
  `always_show` tinyint DEFAULT '0' COMMENT '【目录】只有一个子路由是否始终显示（1-是 0-否）',
  `keep_alive` tinyint DEFAULT '0' COMMENT '【菜单】是否开启页面缓存（1-是 0-否）',
  `visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '显示状态（1-显示 0-隐藏）',
  `sort` int DEFAULT '0' COMMENT '排序',
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '菜单图标',
  `redirect` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '跳转路径',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `params` json DEFAULT NULL COMMENT '路由参数',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=148 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='菜单管理';

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
BEGIN;
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (1, 0, '0', '系统管理', 2, '', '/system', 'Layout', NULL, NULL, NULL, 1, 1, 'system', '/system/user', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (2, 1, '0,1', '用户管理', 1, 'User', 'user', 'system/user/index', NULL, NULL, 1, 1, 1, 'el-icon-User', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (3, 1, '0,1', '角色管理', 1, 'Role', 'role', 'system/role/index', NULL, NULL, 1, 1, 2, 'role', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (4, 1, '0,1', '菜单管理', 1, 'Menu', 'menu', 'system/menu/index', NULL, NULL, 1, 1, 3, 'menu', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (5, 1, '0,1', '部门管理', 1, 'Dept', 'dept', 'system/dept/index', NULL, NULL, 1, 1, 4, 'tree', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (6, 1, '0,1', '字典管理', 1, 'Dict', 'dict', 'system/dict/index', NULL, NULL, 1, 1, 5, 'dict', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (20, 0, '0', '多级菜单', 2, NULL, '/multi-level', 'Layout', NULL, 1, NULL, 1, 9, 'cascader', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (21, 20, '0,20', '菜单一级', 1, NULL, 'multi-level1', 'demo/multi-level/level1', NULL, 1, NULL, 1, 1, '', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (22, 21, '0,20,21', '菜单二级', 1, NULL, 'multi-level2', 'demo/multi-level/children/level2', NULL, 0, NULL, 1, 1, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (23, 22, '0,20,21,22', '菜单三级-1', 1, NULL, 'multi-level3-1', 'demo/multi-level/children/children/level3-1', NULL, 0, 1, 1, 1, '', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (24, 22, '0,20,21,22', '菜单三级-2', 1, NULL, 'multi-level3-2', 'demo/multi-level/children/children/level3-2', NULL, 0, 1, 1, 2, '', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (26, 0, '0', '平台文档', 2, NULL, '/doc', 'Layout', NULL, NULL, NULL, 1, 8, 'document', 'https://juejin.cn/post/7228990409909108793', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30, 26, '0,26', '平台文档(外链)', 3, NULL, 'https://juejin.cn/post/7228990409909108793', '', NULL, NULL, NULL, 1, 2, 'link', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (31, 2, '0,1,2', '用户新增', 4, NULL, '', NULL, 'sys:user:add', NULL, NULL, 1, 1, '', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (32, 2, '0,1,2', '用户编辑', 4, NULL, '', NULL, 'sys:user:edit', NULL, NULL, 1, 2, '', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (33, 2, '0,1,2', '用户删除', 4, NULL, '', NULL, 'sys:user:delete', NULL, NULL, 1, 3, '', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (36, 0, '0', '组件封装', 2, NULL, '/component', 'Layout', NULL, NULL, NULL, 1, 10, 'menu', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (37, 36, '0,36', '富文本编辑器', 1, NULL, 'wang-editor', 'demo/wang-editor', NULL, NULL, 1, 1, 2, '', '', NULL, NULL, NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (38, 36, '0,36', '图片上传', 1, NULL, 'upload', 'demo/upload', NULL, NULL, 1, 1, 3, '', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (39, 36, '0,36', '图标选择器', 1, NULL, 'icon-selector', 'demo/icon-selector', NULL, NULL, 1, 1, 4, '', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (40, 0, '0', '接口文档', 2, NULL, '/api', 'Layout', NULL, 1, NULL, 1, 7, 'api', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (41, 40, '0,40', 'Apifox', 1, NULL, 'apifox', 'demo/api/apifox', NULL, NULL, 1, 1, 1, 'api', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (70, 3, '0,1,3', '角色新增', 4, NULL, '', NULL, 'sys:role:add', NULL, NULL, 1, 1, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (71, 3, '0,1,3', '角色编辑', 4, NULL, '', NULL, 'sys:role:edit', NULL, NULL, 1, 2, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (72, 3, '0,1,3', '角色删除', 4, NULL, '', NULL, 'sys:role:delete', NULL, NULL, 1, 3, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (73, 4, '0,1,4', '菜单新增', 4, NULL, '', NULL, 'sys:menu:add', NULL, NULL, 1, 1, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (74, 4, '0,1,4', '菜单编辑', 4, NULL, '', NULL, 'sys:menu:edit', NULL, NULL, 1, 3, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (75, 4, '0,1,4', '菜单删除', 4, NULL, '', NULL, 'sys:menu:delete', NULL, NULL, 1, 3, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (76, 5, '0,1,5', '部门新增', 4, NULL, '', NULL, 'sys:dept:add', NULL, NULL, 1, 1, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (77, 5, '0,1,5', '部门编辑', 4, NULL, '', NULL, 'sys:dept:edit', NULL, NULL, 1, 2, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (78, 5, '0,1,5', '部门删除', 4, NULL, '', NULL, 'sys:dept:delete', NULL, NULL, 1, 3, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (79, 6, '0,1,6', '字典新增', 4, NULL, '', NULL, 'sys:dict:add', NULL, NULL, 1, 1, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (81, 6, '0,1,6', '字典编辑', 4, NULL, '', NULL, 'sys:dict:edit', NULL, NULL, 1, 2, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (84, 6, '0,1,6', '字典删除', 4, NULL, '', NULL, 'sys:dict:delete', NULL, NULL, 1, 3, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (88, 2, '0,1,2', '重置密码', 4, NULL, '', NULL, 'sys:user:password:reset', NULL, NULL, 1, 4, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (89, 0, '0', '功能演示', 2, NULL, '/function', 'Layout', NULL, NULL, NULL, 1, 12, 'menu', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (90, 89, '0,89', 'Websocket', 1, NULL, '/function/websocket', 'demo/websocket', NULL, NULL, 1, 1, 3, '', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (91, 89, '0,89', '敬请期待...', 2, NULL, 'other/:id', 'demo/other', NULL, NULL, NULL, 1, 4, '', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (95, 36, '0,36', '字典组件', 1, NULL, 'dict-demo', 'demo/dict', NULL, NULL, 1, 1, 4, '', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (97, 89, '0,89', 'Icons', 1, NULL, 'icon-demo', 'demo/icons', NULL, NULL, 1, 1, 2, 'el-icon-Notification', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (102, 26, '0,26', '平台文档(内嵌)', 3, NULL, 'internal-doc', 'demo/internal-doc', NULL, NULL, NULL, 1, 1, 'document', '', '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (105, 2, '0,1,2', '用户查询', 4, NULL, '', NULL, 'sys:user:query', 0, 0, 1, 0, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (106, 2, '0,1,2', '用户导入', 4, NULL, '', NULL, 'sys:user:import', NULL, NULL, 1, 5, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (107, 2, '0,1,2', '用户导出', 4, NULL, '', NULL, 'sys:user:export', NULL, NULL, 1, 6, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (108, 36, '0,36', '增删改查', 1, NULL, 'curd', 'demo/curd/index', NULL, NULL, 1, 1, 0, '', '', NULL, NULL, NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (109, 36, '0,36', '列表选择器', 1, NULL, 'table-select', 'demo/table-select/index', NULL, NULL, 1, 1, 1, '', '', NULL, NULL, NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (110, 0, '0', '路由参数', 2, NULL, '/route-param', 'Layout', NULL, 1, 1, 1, 11, 'el-icon-ElementPlus', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (111, 110, '0,110', '参数(type=1)', 1, NULL, 'route-param-type1', 'demo/route-param', NULL, 0, 1, 1, 1, 'el-icon-Star', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', '{\"type\": \"1\"}');
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (112, 110, '0,110', '参数(type=2)', 1, NULL, 'route-param-type2', 'demo/route-param', NULL, 0, 1, 1, 2, 'el-icon-StarFilled', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', '{\"type\": \"2\"}');
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (117, 1, '0,1', '系统日志', 1, 'Log', 'log', 'system/log/index', NULL, 0, 1, 1, 6, 'document', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (118, 0, '0', '系统工具', 2, NULL, '/codegen', 'Layout', NULL, 0, 1, 1, 2, 'menu', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (119, 118, '0,118', '代码生成', 1, 'Codegen', 'codegen', 'codegen/index', NULL, 0, 1, 1, 1, 'code', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (120, 1, '0,1', '系统配置', 1, 'Config', 'config', 'system/config/index', NULL, 0, 1, 1, 7, 'setting', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (121, 120, '0,1,120', '查询系统配置', 4, NULL, '', NULL, 'sys:config:query', 0, 1, 1, 1, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (122, 120, '0,1,120', '新增系统配置', 4, NULL, '', NULL, 'sys:config:add', 0, 1, 1, 2, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (123, 120, '0,1,120', '修改系统配置', 4, NULL, '', NULL, 'sys:config:update', 0, 1, 1, 3, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (124, 120, '0,1,120', '删除系统配置', 4, NULL, '', NULL, 'sys:config:delete', 0, 1, 1, 4, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (125, 120, '0,1,120', '刷新系统配置', 4, NULL, '', NULL, 'sys:config:refresh', 0, 1, 1, 5, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (126, 1, '0,1', '通知公告', 1, 'Notice', 'notice', 'system/notice/index', NULL, NULL, NULL, 1, 9, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (127, 126, '0,1,126', '查询', 4, NULL, '', NULL, 'sys:notice:query', NULL, NULL, 1, 1, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (128, 126, '0,1,126', '新增', 4, NULL, '', NULL, 'sys:notice:add', NULL, NULL, 1, 2, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (129, 126, '0,1,126', '编辑', 4, NULL, '', NULL, 'sys:notice:edit', NULL, NULL, 1, 3, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (130, 126, '0,1,126', '删除', 4, NULL, '', NULL, 'sys:notice:delete', NULL, NULL, 1, 4, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (133, 126, '0,1,126', '发布', 4, NULL, '', NULL, 'sys:notice:publish', 0, 1, 1, 5, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (134, 126, '0,1,126', '撤回', 4, NULL, '', NULL, 'sys:notice:revoke', 0, 1, 1, 6, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (135, 1, '0,1', '字典数据', 1, 'DictData', 'dict-data', 'system/dict/data', NULL, 0, 1, 0, 6, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (136, 135, '0,1,135', '字典数据新增', 4, NULL, '', NULL, 'sys:dict-data:add', NULL, NULL, 1, 4, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (137, 135, '0,1,135', '字典数据编辑', 4, NULL, '', NULL, 'sys:dict-data:edit', NULL, NULL, 1, 5, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (138, 135, '0,1,135', '字典数据删除', 4, NULL, '', NULL, 'sys:dict-data:delete', NULL, NULL, 1, 6, '', NULL, '2024-11-02 13:10:45', '2024-11-02 13:10:45', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (139, 0, '0', '任务管理', 2, NULL, '/task', 'Layout', NULL, 0, 1, 1, 3, 'cascader', NULL, '2024-11-02 13:10:45', '2024-11-03 08:10:07', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (141, 139, '0,139', '任务管理', 1, 'Task-info', 'task-info', 'task/task-info/index', NULL, 0, 1, 1, 1, 'el-icon-Orange', NULL, '2024-11-03 08:10:57', '2024-11-09 19:24:27', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (142, 139, '0,139', '执行器管理', 1, 'Task-group', 'task-group', 'task/task-group/index', NULL, 0, 1, 1, 2, '', NULL, '2024-11-03 08:42:11', '2024-11-09 13:17:47', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (144, 139, '0,139', '任务日志', 1, 'Task-log', 'task-log', 'task/task-log/index', NULL, 0, 1, 1, 4, 'el-icon-Tickets', NULL, '2024-11-03 08:43:50', '2024-11-09 19:24:44', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '通知内容',
  `type` tinyint NOT NULL COMMENT '通知类型（关联字典编码：notice_type）',
  `level` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知等级（字典code：notice_level）',
  `target_type` tinyint NOT NULL COMMENT '目标类型（1: 全体, 2: 指定）',
  `target_user_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标人ID集合（多个使用英文逗号,分割）',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人ID',
  `publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '发布状态（0: 未发布, 1: 已发布, -1: 已撤回）',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `revoke_time` datetime DEFAULT NULL COMMENT '撤回时间',
  `create_by` bigint NOT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除（0: 未删除, 1: 已删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告表';

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '角色名称',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `status` tinyint(1) DEFAULT '1' COMMENT '角色状态(1-正常 0-停用)',
  `data_scope` tinyint DEFAULT NULL COMMENT '数据权限(0-所有数据 1-部门及子部门数据 2-本部门数据3-本人数据)',
  `create_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT NULL COMMENT '创建时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_name` (`name`) USING BTREE COMMENT '角色名称唯一索引',
  UNIQUE KEY `uk_code` (`code`) USING BTREE COMMENT '角色编码唯一索引'
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色表';

-- ----------------------------
-- Records of sys_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (1, '超级管理员', 'ROOT', 1, 1, 0, NULL, '2021-05-21 14:56:51', NULL, '2018-12-23 16:00:00', 0);
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (2, '系统管理员', 'ADMIN', 2, 1, 1, NULL, '2021-03-25 12:39:54', NULL, NULL, 0);
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (3, '访问游客', 'GUEST', 3, 1, 2, NULL, '2021-05-26 15:49:05', NULL, '2019-05-05 16:00:00', 0);
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (4, '系统管理员1', 'ADMIN1', 4, 1, 1, NULL, '2021-03-25 12:39:54', NULL, NULL, 0);
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (5, '系统管理员2', 'ADMIN2', 5, 1, 1, NULL, '2021-03-25 12:39:54', NULL, NULL, 0);
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (6, '系统管理员3', 'ADMIN3', 6, 1, 1, NULL, '2021-03-25 12:39:54', NULL, NULL, 0);
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (7, '系统管理员4', 'ADMIN4', 7, 1, 1, NULL, '2021-03-25 12:39:54', NULL, NULL, 0);
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (8, '系统管理员5', 'ADMIN5', 8, 1, 1, NULL, '2021-03-25 12:39:54', NULL, NULL, 0);
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (9, '系统管理员6', 'ADMIN6', 9, 1, 1, NULL, '2021-03-25 12:39:54', NULL, NULL, 0);
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (10, '系统管理员7', 'ADMIN7', 10, 1, 1, NULL, '2021-03-25 12:39:54', NULL, NULL, 0);
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (11, '系统管理员8', 'ADMIN8', 11, 1, 1, NULL, '2021-03-25 12:39:54', NULL, NULL, 0);
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES (12, '系统管理员9', 'ADMIN9', 12, 1, 1, NULL, '2021-03-25 12:39:54', NULL, NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  UNIQUE KEY `uk_roleid_menuid` (`role_id`,`menu_id`) USING BTREE COMMENT '角色菜单唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色和菜单关联表';

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
BEGIN;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 2);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 3);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 4);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 5);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 6);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 20);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 21);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 22);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 23);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 24);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 26);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 30);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 31);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 32);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 33);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 36);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 37);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 38);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 39);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 40);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 41);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 70);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 71);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 72);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 73);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 74);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 75);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 76);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 77);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 78);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 79);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 81);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 84);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 88);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 89);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 90);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 91);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 95);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 97);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 102);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 105);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 106);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 107);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 108);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 109);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 110);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 111);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 112);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 117);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 118);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 119);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 120);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 121);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 122);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 123);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 124);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 125);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 126);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 127);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 128);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 129);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 130);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 133);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 134);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 135);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 136);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 137);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 138);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 139);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 141);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 142);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 143);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 144);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 145);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 146);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 147);
COMMIT;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '昵称',
  `gender` tinyint(1) DEFAULT '1' COMMENT '性别((1-男 2-女 0-保密)',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码',
  `dept_id` int DEFAULT NULL COMMENT '部门ID',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '用户头像',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系方式',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态((1-正常 0-禁用)',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户邮箱',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人ID',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `login_name` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户信息表';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` (`id`, `username`, `nickname`, `gender`, `password`, `dept_id`, `avatar`, `mobile`, `status`, `email`, `create_time`, `create_by`, `update_time`, `update_by`, `is_deleted`) VALUES (1, 'root', '有来技术', 0, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', NULL, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18866668888', 1, 'youlaitech@163.com', NULL, NULL, NULL, NULL, 0);
INSERT INTO `sys_user` (`id`, `username`, `nickname`, `gender`, `password`, `dept_id`, `avatar`, `mobile`, `status`, `email`, `create_time`, `create_by`, `update_time`, `update_by`, `is_deleted`) VALUES (2, 'admin', '系统管理员', 1, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 1, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18866668887', 1, '', '2019-10-10 13:41:22', NULL, '2022-07-31 12:39:30', NULL, 0);
INSERT INTO `sys_user` (`id`, `username`, `nickname`, `gender`, `password`, `dept_id`, `avatar`, `mobile`, `status`, `email`, `create_time`, `create_by`, `update_time`, `update_by`, `is_deleted`) VALUES (3, 'websocket', '测试小用户', 1, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 3, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18866668886', 1, 'youlaitech@163.com', '2021-06-05 01:31:29', NULL, '2021-06-05 01:31:29', NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for sys_user_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_notice`;
CREATE TABLE `sys_user_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `notice_id` bigint NOT NULL COMMENT '公共通知id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `is_read` bigint NOT NULL DEFAULT '0' COMMENT '读取状态（0: 未读, 1: 已读）',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0: 未删除, 1: 已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户通知公告表';

-- ----------------------------
-- Records of sys_user_notice
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`) USING BTREE,
  UNIQUE KEY `uk_userid_roleid` (`user_id`,`role_id`) USING BTREE COMMENT '用户角色唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户和角色关联表';

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (2, 2);
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (3, 3);
COMMIT;

-- ----------------------------
-- Table structure for task_group
-- ----------------------------
DROP TABLE IF EXISTS `task_group`;
CREATE TABLE `task_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_name` varchar(64) NOT NULL COMMENT '执行器AppName',
  `title` varchar(12) NOT NULL COMMENT '执行器名称',
  `address_type` tinyint NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
  `address_list` text COMMENT '执行器地址列表，多地址逗号分隔',
  `update_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task_group
-- ----------------------------
BEGIN;
INSERT INTO `task_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`, `create_time`) VALUES (1, 'xxl-job-executor-sample', '示例执行器', 1, 'http://192.168.31.233:9999/', '2024-11-08 22:28:00', '2024-11-08 21:38:55');
INSERT INTO `task_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`, `create_time`) VALUES (5, '测试执行器2', '测试执行器2', 0, '222', '2024-11-09 20:09:21', '2024-11-08 22:44:24');
COMMIT;

-- ----------------------------
-- Table structure for task_info
-- ----------------------------
DROP TABLE IF EXISTS `task_info`;
CREATE TABLE `task_info` (
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task_info
-- ----------------------------
BEGIN;
INSERT INTO `task_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`) VALUES (1, 1, '测试任务1', '2018-11-03 22:21:31', 'XXL', '', 'CRON', '0 0 0 * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler', '', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化', '2018-11-03 22:21:31', '', 0, 0, 0, '2018-11-03 22:21:31');
INSERT INTO `task_info` (`id`, `job_group`, `job_desc`, `update_time`, `author`, `alarm_email`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`, `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`, `create_time`) VALUES (4, 1, '测试任务21', '2024-11-09 11:20:35', 'xiaozhao1', NULL, 'CRON', '0 * * * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler2', NULL, 'SERIAL_EXECUTION', 0, 0, 'BEAN', NULL, NULL, '2024-11-09 11:20:35', NULL, 0, 0, 0, '2024-11-09 11:20:35');
COMMIT;

-- ----------------------------
-- Table structure for task_lock
-- ----------------------------
DROP TABLE IF EXISTS `task_lock`;
CREATE TABLE `task_lock` (
  `lock_name` varchar(50) NOT NULL COMMENT '锁名称',
  PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task_lock
-- ----------------------------
BEGIN;
INSERT INTO `task_lock` (`lock_name`) VALUES ('schedule_lock');
COMMIT;

-- ----------------------------
-- Table structure for task_log
-- ----------------------------
DROP TABLE IF EXISTS `task_log`;
CREATE TABLE `task_log` (
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
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task_log
-- ----------------------------
BEGIN;
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (1, 1, 1, NULL, 'demoJobHandler', '', NULL, 0, '2024-11-03 14:00:24', 500, '任务触发类型：手动触发<br>调度机器：192.168.31.157<br>执行器-注册方式：自动注册<br>执行器-地址列表：null<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>调度失败：执行器地址为空<br><br>', NULL, 0, NULL, 2);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (2, 1, 1, NULL, 'demoJobHandler', '', NULL, 0, '2024-11-03 14:07:51', 500, '任务触发类型：手动触发<br>调度机器：192.168.31.157<br>执行器-注册方式：自动注册<br>执行器-地址列表：null<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>调度失败：执行器地址为空<br><br>', NULL, 0, NULL, 2);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (3, 1, 1, 'http://192.168.31.157:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-03 15:46:49', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.157<br>执行器-注册方式：自动注册<br>执行器-地址列表：[http://192.168.31.157:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.157:9999/<br>code：200<br>msg：null', '2024-11-03 15:46:59', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (4, 1, 1, 'http://192.168.31.157:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-03 19:32:24', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.157<br>执行器-注册方式：自动注册<br>执行器-地址列表：[http://192.168.31.157:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.157:9999/<br>code：200<br>msg：null', '2024-11-03 19:32:34', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (5, 1, 1, 'http://192.168.31.157:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-03 19:46:01', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.157<br>执行器-注册方式：自动注册<br>执行器-地址列表：[http://192.168.31.157:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.157:9999/<br>code：200<br>msg：null', '2024-11-03 19:46:11', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (6, 1, 1, 'http://192.168.31.157:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-03 19:46:05', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.157<br>执行器-注册方式：自动注册<br>执行器-地址列表：[http://192.168.31.157:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.157:9999/<br>code：200<br>msg：null', '2024-11-03 19:46:21', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (7, 1, 1, 'http://192.168.31.157:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-03 19:46:16', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.157<br>执行器-注册方式：自动注册<br>执行器-地址列表：[http://192.168.31.157:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.157:9999/<br>code：200<br>msg：null', '2024-11-03 19:46:31', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (8, 1, 1, 'http://192.168.31.157:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-03 19:49:31', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.157<br>执行器-注册方式：自动注册<br>执行器-地址列表：[http://192.168.31.157:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.157:9999/<br>code：200<br>msg：null', '2024-11-03 19:49:41', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (9, 1, 1, 'http://192.168.31.157:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-03 19:50:26', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.157<br>执行器-注册方式：自动注册<br>执行器-地址列表：[http://192.168.31.157:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.157:9999/<br>code：200<br>msg：null', '2024-11-03 19:50:36', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (10, 1, 1, 'http://192.168.31.157:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-03 19:51:12', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.157<br>执行器-注册方式：自动注册<br>执行器-地址列表：[http://192.168.31.157:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.157:9999/<br>code：200<br>msg：null', '2024-11-03 19:51:22', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (118, 1, 4, 'http://192.168.31.233:9999/', 'demoJobHandler2', '', NULL, 0, '2024-11-09 16:30:49', 0, '任务触发类型：手动触发<br>调度机器：192.168.31.233<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://192.168.31.233:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.233:9999/<br>code：200<br>msg：null', '2024-11-09 16:30:49', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (119, 1, 4, 'http://192.168.31.233:9999/', 'demoJobHandler2', '', NULL, 0, '2024-11-09 16:32:32', 0, '任务触发类型：手动触发<br>调度机器：192.168.31.233<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://192.168.31.233:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.233:9999/<br>code：200<br>msg：null', '2024-11-09 16:32:32', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (120, 1, 1, 'http://192.168.31.233:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-09 16:33:23', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.233<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://192.168.31.233:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.233:9999/<br>code：200<br>msg：null', '2024-11-09 16:33:33', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (121, 1, 1, 'http://192.168.31.233:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-09 18:29:03', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.233<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://192.168.31.233:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.233:9999/<br>code：200<br>msg：null', '2024-11-09 18:32:24', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (122, 1, 1, 'http://192.168.31.233:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-09 18:42:36', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.233<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://192.168.31.233:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.233:9999/<br>code：200<br>msg：null', '2024-11-09 18:45:56', 200, '', 0);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (123, 1, 1, 'http://192.168.31.233:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-09 18:46:32', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.233<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://192.168.31.233:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.233:9999/<br>code：200<br>msg：null', '2024-11-09 18:49:50', 500, 'web container destroy and kill the job. [job running, killed]', 2);
INSERT INTO `task_log` (`id`, `job_group`, `job_id`, `executor_address`, `executor_handler`, `executor_param`, `executor_sharding_param`, `executor_fail_retry_count`, `trigger_time`, `trigger_code`, `trigger_msg`, `handle_time`, `handle_code`, `handle_msg`, `alarm_status`) VALUES (124, 1, 1, 'http://192.168.31.233:9999/', 'demoJobHandler', '', NULL, 0, '2024-11-09 19:22:47', 200, '任务触发类型：手动触发<br>调度机器：192.168.31.233<br>执行器-注册方式：手动录入<br>执行器-地址列表：[http://192.168.31.233:9999/]<br>路由策略：第一个<br>阻塞处理策略：单机串行<br>任务超时时间：0<br>失败重试次数：0<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>触发调度：<br>address：http://192.168.31.233:9999/<br>code：200<br>msg：null', '2024-11-09 19:26:08', 200, '', 0);
COMMIT;

-- ----------------------------
-- Table structure for task_log_report
-- ----------------------------
DROP TABLE IF EXISTS `task_log_report`;
CREATE TABLE `task_log_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trigger_day` datetime DEFAULT NULL COMMENT '调度-时间',
  `running_count` int NOT NULL DEFAULT '0' COMMENT '运行中-日志数量',
  `suc_count` int NOT NULL DEFAULT '0' COMMENT '执行成功-日志数量',
  `fail_count` int NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
  `update_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task_log_report
-- ----------------------------
BEGIN;
INSERT INTO `task_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (1, '2024-11-03 00:00:00', 0, 8, 2, '2024-11-03 21:19:57', '2024-11-03 13:44:53');
INSERT INTO `task_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (2, '2024-11-02 00:00:00', 0, 0, 0, '2024-11-03 21:19:57', '2024-11-03 13:44:53');
INSERT INTO `task_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (3, '2024-11-01 00:00:00', 0, 0, 0, '2024-11-03 21:19:57', '2024-11-03 13:44:53');
INSERT INTO `task_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (4, '2024-11-08 00:00:00', 0, 0, 0, '2024-11-09 20:09:20', '2024-11-08 20:08:14');
INSERT INTO `task_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (5, '2024-11-07 00:00:00', 0, 0, 0, '2024-11-09 20:09:20', '2024-11-08 20:08:14');
INSERT INTO `task_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (6, '2024-11-06 00:00:00', 0, 0, 0, '2024-11-08 23:47:42', '2024-11-08 20:08:14');
INSERT INTO `task_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`, `create_time`) VALUES (7, '2024-11-09 00:00:00', 0, 6, 1, '2024-11-09 20:09:20', '2024-11-09 00:03:51');
COMMIT;

-- ----------------------------
-- Table structure for task_logglue
-- ----------------------------
DROP TABLE IF EXISTS `task_logglue`;
CREATE TABLE `task_logglue` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_id` bigint NOT NULL COMMENT '任务，主键ID',
  `glue_type` varchar(50) DEFAULT NULL COMMENT 'GLUE类型',
  `glue_source` mediumtext COMMENT 'GLUE源代码',
  `glue_remark` varchar(128) NOT NULL COMMENT 'GLUE备注',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task_logglue
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for task_registry
-- ----------------------------
DROP TABLE IF EXISTS `task_registry`;
CREATE TABLE `task_registry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `registry_group` varchar(50) NOT NULL,
  `registry_key` varchar(255) NOT NULL,
  `registry_value` varchar(255) NOT NULL,
  `update_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `i_g_k_v` (`registry_group`,`registry_key`,`registry_value`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task_registry
-- ----------------------------
BEGIN;
INSERT INTO `task_registry` (`id`, `registry_group`, `registry_key`, `registry_value`, `update_time`, `create_time`) VALUES (26, 'EXECUTOR', 'xxl-job-executor-sample', 'http://192.168.31.233:9999/', '2024-11-09 20:09:12', NULL);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

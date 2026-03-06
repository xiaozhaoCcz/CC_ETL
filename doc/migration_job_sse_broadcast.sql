-- SSE 多实例广播表：用于多 Admin 实例时跨实例推送节点状态到 PC 端
-- 执行前请确认数据库为 cc_etl

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
  KEY `idx_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SSE多实例广播队列表';

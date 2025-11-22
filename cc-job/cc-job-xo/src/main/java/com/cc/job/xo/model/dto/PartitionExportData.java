package com.cc.job.xo.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分区导出数据结构
 * 包含分区下的所有任务组、任务节点和边的关系
 */
@Data
public class PartitionExportData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分区信息
     */
    private PartitionInfo partition;

    /**
     * 任务组列表（jobType=2, isNode="N"）
     */
    private List<TaskGroupInfo> taskGroups;

    /**
     * 任务节点信息（包含节点位置）
     */
    @Data
    public static class PartitionInfo implements Serializable {
        private Long id;
        private String jobPartName;
        private Integer sort;
    }

    /**
     * 任务组信息
     */
    @Data
    public static class TaskGroupInfo implements Serializable {
        /**
         * 任务组基本信息（JobInfo）
         */
        private TaskInfoData taskGroupData;

        /**
         * 任务组下的节点列表（JobNode + JobInfo）
         */
        private List<NodeInfo> nodes;

        /**
         * 任务组下的边列表（JobEdge）
         */
        private List<EdgeInfo> edges;
    }

    /**
     * 任务信息数据（JobInfo的完整信息）
     */
    @Data
    public static class TaskInfoData implements Serializable {
        private Long id;
        private Long jobGroup;
        private String jobDesc;
        private String author;
        private String alarmEmail;
        private String scheduleType;
        private String scheduleConf;
        private String misfireStrategy;
        private String executorRouteStrategy;
        private String executorHandler;
        private String executorParam;
        private String executorBlockStrategy;
        private Integer executorTimeout;
        private Integer executorFailRetryCount;
        private String glueType;
        private String glueSource;
        private String glueRemark;
        private String childJobid;
        private Integer triggerStatus;
        private Long triggerLastTime;
        private Long triggerNextTime;
        private Integer jobType;
        private Long parentId;
        private String reqType;
        private String reqHeader;
        private String reqBody;
        private String reqUrl;
        private String isNode;
        private Integer rankTriggerStatus;
        private Long jdbcDatasourceId;
        private Integer incrType;
        private String incrContent;
        private Long runTime;
        private Integer isPause;
        private Integer jobPartId;
        private Integer triggerUserId;
    }

    /**
     * 节点信息（JobNode + 对应的JobInfo）
     */
    @Data
    public static class NodeInfo implements Serializable {
        /**
         * JobNode信息
         */
        private Long nodeId;
        private Long jobId;
        private Long jobParentId;
        private Double nodePositionX;
        private Double nodePositionY;
        private Long nodeInDegree;
        private Long nodeOutDegree;
        private Integer sort;
        private String children;
        private String properties;
        private String nodeType;
        private Integer triggerStatus;

        /**
         * 节点对应的JobInfo完整信息
         */
        private TaskInfoData taskInfo;
    }

    /**
     * 边信息（JobEdge）
     */
    @Data
    public static class EdgeInfo implements Serializable {
        private Long id;
        private Long jobParentId;
        private Long fromNodeId;
        private Long endNodeId;
        private String pointsList;
        private String properties;
        private String startPoint;
        private String endPoint;
    }
}


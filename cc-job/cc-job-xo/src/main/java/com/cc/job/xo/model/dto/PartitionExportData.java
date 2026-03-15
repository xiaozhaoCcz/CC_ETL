package com.cc.job.xo.model.dto;


import java.io.Serializable;
import java.util.List;

/**
 * 分区导出数据结构
 * 包含分区下的所有任务组、任务节点和边的关系
 */
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

    public PartitionInfo getPartition() {
        return partition;
    }

    public void setPartition(PartitionInfo partition) {
        this.partition = partition;
    }

    public List<TaskGroupInfo> getTaskGroups() {
        return taskGroups;
    }

    public void setTaskGroups(List<TaskGroupInfo> taskGroups) {
        this.taskGroups = taskGroups;
    }

    /**
     * 任务节点信息（包含节点位置）
     */
    public static class PartitionInfo implements Serializable {
        private Long id;
        private String jobPartName;
        private Integer sort;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getJobPartName() {
            return jobPartName;
        }

        public void setJobPartName(String jobPartName) {
            this.jobPartName = jobPartName;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }
    }

    /**
     * 任务组信息
     */
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

        public TaskInfoData getTaskGroupData() {
            return taskGroupData;
        }

        public void setTaskGroupData(TaskInfoData taskGroupData) {
            this.taskGroupData = taskGroupData;
        }

        public List<NodeInfo> getNodes() {
            return nodes;
        }

        public void setNodes(List<NodeInfo> nodes) {
            this.nodes = nodes;
        }

        public List<EdgeInfo> getEdges() {
            return edges;
        }

        public void setEdges(List<EdgeInfo> edges) {
            this.edges = edges;
        }
    }

    /**
     * 任务信息数据（JobInfo的完整信息）
     */
    public static class TaskInfoData implements Serializable {
        private Long id;
        private Long jobGroup;
        private String jobDesc;
        private String author;
        private String alarmEmail;
        private String scheduleType;
        private String scheduleConf;
        private String misfireStrategy;
        private String failStrategy;
        private String executorRouteStrategy;
        private String executorHandler;
        private String executorParam;
        private String executorBlockStrategy;
        private Integer executorTimeout;
        private Integer executorFailRetryCount;
        private String glueType;
        private String glueSource;
        private String glueRemark;
        private String childJobId;
        private Integer triggerStatus;
        private Long triggerLastTime;
        private Long triggerNextTime;
        private Integer jobType;
        private Long parentId;
        private String reqType;
        private String reqHeader;
        private String reqBody;
        private String reqUrl;
        private String nodeFlag;
        private Long jdbcDatasourceId;
        private Integer incrementType;
        private String incrementContent;
        private String incrementParamTemplate;
        private Long runTime;
        private Integer pauseStatus;
        private Integer jobPartId;
        private Integer triggerUserId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getJobGroup() {
            return jobGroup;
        }

        public void setJobGroup(Long jobGroup) {
            this.jobGroup = jobGroup;
        }

        public String getJobDesc() {
            return jobDesc;
        }

        public void setJobDesc(String jobDesc) {
            this.jobDesc = jobDesc;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getAlarmEmail() {
            return alarmEmail;
        }

        public void setAlarmEmail(String alarmEmail) {
            this.alarmEmail = alarmEmail;
        }

        public String getScheduleType() {
            return scheduleType;
        }

        public void setScheduleType(String scheduleType) {
            this.scheduleType = scheduleType;
        }

        public String getScheduleConf() {
            return scheduleConf;
        }

        public void setScheduleConf(String scheduleConf) {
            this.scheduleConf = scheduleConf;
        }

        public String getMisfireStrategy() {
            return misfireStrategy;
        }

        public void setMisfireStrategy(String misfireStrategy) {
            this.misfireStrategy = misfireStrategy;
        }

        public String getExecutorRouteStrategy() {
            return executorRouteStrategy;
        }

        public void setExecutorRouteStrategy(String executorRouteStrategy) {
            this.executorRouteStrategy = executorRouteStrategy;
        }

        public String getExecutorHandler() {
            return executorHandler;
        }

        public void setExecutorHandler(String executorHandler) {
            this.executorHandler = executorHandler;
        }

        public String getExecutorParam() {
            return executorParam;
        }

        public void setExecutorParam(String executorParam) {
            this.executorParam = executorParam;
        }

        public String getExecutorBlockStrategy() {
            return executorBlockStrategy;
        }

        public void setExecutorBlockStrategy(String executorBlockStrategy) {
            this.executorBlockStrategy = executorBlockStrategy;
        }

        public Integer getExecutorTimeout() {
            return executorTimeout;
        }

        public void setExecutorTimeout(Integer executorTimeout) {
            this.executorTimeout = executorTimeout;
        }

        public Integer getExecutorFailRetryCount() {
            return executorFailRetryCount;
        }

        public void setExecutorFailRetryCount(Integer executorFailRetryCount) {
            this.executorFailRetryCount = executorFailRetryCount;
        }

        public String getGlueType() {
            return glueType;
        }

        public void setGlueType(String glueType) {
            this.glueType = glueType;
        }

        public String getGlueSource() {
            return glueSource;
        }

        public void setGlueSource(String glueSource) {
            this.glueSource = glueSource;
        }

        public String getGlueRemark() {
            return glueRemark;
        }

        public void setGlueRemark(String glueRemark) {
            this.glueRemark = glueRemark;
        }

        public String getChildJobId() {
            return childJobId;
        }

        public void setChildJobId(String childJobId) {
            this.childJobId = childJobId;
        }

        public Integer getTriggerStatus() {
            return triggerStatus;
        }

        public void setTriggerStatus(Integer triggerStatus) {
            this.triggerStatus = triggerStatus;
        }

        public Long getTriggerLastTime() {
            return triggerLastTime;
        }

        public void setTriggerLastTime(Long triggerLastTime) {
            this.triggerLastTime = triggerLastTime;
        }

        public Long getTriggerNextTime() {
            return triggerNextTime;
        }

        public void setTriggerNextTime(Long triggerNextTime) {
            this.triggerNextTime = triggerNextTime;
        }

        public Integer getJobType() {
            return jobType;
        }

        public void setJobType(Integer jobType) {
            this.jobType = jobType;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        public String getReqType() {
            return reqType;
        }

        public void setReqType(String reqType) {
            this.reqType = reqType;
        }

        public String getReqHeader() {
            return reqHeader;
        }

        public void setReqHeader(String reqHeader) {
            this.reqHeader = reqHeader;
        }

        public String getReqBody() {
            return reqBody;
        }

        public void setReqBody(String reqBody) {
            this.reqBody = reqBody;
        }

        public String getReqUrl() {
            return reqUrl;
        }

        public void setReqUrl(String reqUrl) {
            this.reqUrl = reqUrl;
        }

        public String getNodeFlag() {
            return nodeFlag;
        }

        public void setNodeFlag(String nodeFlag) {
            this.nodeFlag = nodeFlag;
        }

        public Long getJdbcDatasourceId() {
            return jdbcDatasourceId;
        }

        public void setJdbcDatasourceId(Long jdbcDatasourceId) {
            this.jdbcDatasourceId = jdbcDatasourceId;
        }

        public Integer getIncrementType() {
            return incrementType;
        }

        public void setIncrementType(Integer incrementType) {
            this.incrementType = incrementType;
        }

        public String getIncrementContent() {
            return incrementContent;
        }

        public void setIncrementContent(String incrementContent) {
            this.incrementContent = incrementContent;
        }

        public String getIncrementParamTemplate() {
            return incrementParamTemplate;
        }

        public void setIncrementParamTemplate(String incrementParamTemplate) {
            this.incrementParamTemplate = incrementParamTemplate;
        }

        public Long getRunTime() {
            return runTime;
        }

        public void setRunTime(Long runTime) {
            this.runTime = runTime;
        }

        public Integer getPauseStatus() {
            return pauseStatus;
        }

        public void setPauseStatus(Integer pauseStatus) {
            this.pauseStatus = pauseStatus;
        }

        public Integer getJobPartId() {
            return jobPartId;
        }

        public void setJobPartId(Integer jobPartId) {
            this.jobPartId = jobPartId;
        }

        public Integer getTriggerUserId() {
            return triggerUserId;
        }

        public void setTriggerUserId(Integer triggerUserId) {
            this.triggerUserId = triggerUserId;
        }

        public String getFailStrategy() {
            return failStrategy;
        }

        public void setFailStrategy(String failStrategy) {
            this.failStrategy = failStrategy;
        }
    }

    /**
     * 节点信息（JobNode + 对应的JobInfo）
     */
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


        public Long getNodeId() {
            return nodeId;
        }

        public void setNodeId(Long nodeId) {
            this.nodeId = nodeId;
        }

        public Long getJobId() {
            return jobId;
        }

        public void setJobId(Long jobId) {
            this.jobId = jobId;
        }

        public Long getJobParentId() {
            return jobParentId;
        }

        public void setJobParentId(Long jobParentId) {
            this.jobParentId = jobParentId;
        }

        public Double getNodePositionX() {
            return nodePositionX;
        }

        public void setNodePositionX(Double nodePositionX) {
            this.nodePositionX = nodePositionX;
        }

        public Double getNodePositionY() {
            return nodePositionY;
        }

        public void setNodePositionY(Double nodePositionY) {
            this.nodePositionY = nodePositionY;
        }

        public Long getNodeInDegree() {
            return nodeInDegree;
        }

        public void setNodeInDegree(Long nodeInDegree) {
            this.nodeInDegree = nodeInDegree;
        }

        public Long getNodeOutDegree() {
            return nodeOutDegree;
        }

        public void setNodeOutDegree(Long nodeOutDegree) {
            this.nodeOutDegree = nodeOutDegree;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }

        public String getChildren() {
            return children;
        }

        public void setChildren(String children) {
            this.children = children;
        }

        public String getProperties() {
            return properties;
        }

        public void setProperties(String properties) {
            this.properties = properties;
        }

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }

        public Integer getTriggerStatus() {
            return triggerStatus;
        }

        public void setTriggerStatus(Integer triggerStatus) {
            this.triggerStatus = triggerStatus;
        }

        public TaskInfoData getTaskInfo() {
            return taskInfo;
        }

        public void setTaskInfo(TaskInfoData taskInfo) {
            this.taskInfo = taskInfo;
        }
    }

    /**
     * 边信息（JobEdge）
     */
    public static class EdgeInfo implements Serializable {
        private Long id;
        private Long jobParentId;
        private Long fromNodeId;
        private Long endNodeId;
        private String pointsList;
        private String properties;
        private String startPoint;
        private String endPoint;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getJobParentId() {
            return jobParentId;
        }

        public void setJobParentId(Long jobParentId) {
            this.jobParentId = jobParentId;
        }

        public Long getFromNodeId() {
            return fromNodeId;
        }

        public void setFromNodeId(Long fromNodeId) {
            this.fromNodeId = fromNodeId;
        }

        public Long getEndNodeId() {
            return endNodeId;
        }

        public void setEndNodeId(Long endNodeId) {
            this.endNodeId = endNodeId;
        }

        public String getPointsList() {
            return pointsList;
        }

        public void setPointsList(String pointsList) {
            this.pointsList = pointsList;
        }

        public String getProperties() {
            return properties;
        }

        public void setProperties(String properties) {
            this.properties = properties;
        }

        public String getStartPoint() {
            return startPoint;
        }

        public void setStartPoint(String startPoint) {
            this.startPoint = startPoint;
        }

        public String getEndPoint() {
            return endPoint;
        }

        public void setEndPoint(String endPoint) {
            this.endPoint = endPoint;
        }
    }
}


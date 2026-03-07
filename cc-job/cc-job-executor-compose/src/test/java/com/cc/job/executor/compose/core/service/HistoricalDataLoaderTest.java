package com.cc.job.executor.compose.core.service;

import cn.hutool.json.JSONUtil;
import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.context.DataContext;
import com.cc.job.executor.compose.core.model.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoricalDataLoaderTest {

    @Test
    void loadSpecificJobResultsShouldRestoreApiAliases() {
        AdminApiClient adminApiClient = mock(AdminApiClient.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        HistoricalDataLoader loader = new HistoricalDataLoader(adminApiClient, fileStorageService);

        Map<String, Object> apiResult = Map.of(
                "statusCode", 200,
                "body", Map.of("ok", true),
                "requestUrl", "http://demo/api",
                "requestMethod", "GET",
                "responseTime", 35L
        );
        Map<String, Object> nodeResult = Map.of(
                "code", 200,
                "message", "ok",
                "success", true,
                "data", Map.of("id", 1),
                "apiResult", apiResult
        );
        when(adminApiClient.getNodeResultsByBatch(1L, "full-batch", "http://127.0.0.1:8500/"))
                .thenReturn(List.of(Map.of(
                        "jobId", 100L,
                        "jobName", "api job",
                        "resultData", JSONUtil.toJsonStr(nodeResult)
                )));

        ExecutionContext context = new ExecutionContext();
        context.setDataContext(new DataContext("run-1"));
        context.setInstanceKey("http://127.0.0.1:8500/");

        int loaded = loader.loadSpecificJobResults(context, 1L, "full-batch", Set.of(100L));

        assertEquals(1, loaded);
        assertNotNull(context.getDataContext().get("api_job.response"));
        assertEquals(200, ((Number) context.getDataContext().get("api_job.response.statusCode")).intValue());
        assertEquals("http://demo/api", context.getDataContext().get("api_job.request.url"));
    }

    @Test
    void loadSpecificJobResultsShouldOnlyLoadRequestedJobs() {
        AdminApiClient adminApiClient = mock(AdminApiClient.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        HistoricalDataLoader loader = new HistoricalDataLoader(adminApiClient, fileStorageService);

        when(adminApiClient.getNodeResultsByBatch(1L, "full-batch", "http://127.0.0.1:8500/"))
                .thenReturn(List.of(
                        Map.of("jobId", 100L, "jobName", "jobA", "resultData", "A"),
                        Map.of("jobId", 101L, "jobName", "jobB", "resultData", "B")
                ));

        ExecutionContext context = new ExecutionContext();
        context.setDataContext(new DataContext("run-1"));
        context.setInstanceKey("http://127.0.0.1:8500/");

        int loaded = loader.loadSpecificJobResults(context, 1L, "full-batch", Set.of(101L));

        assertEquals(1, loaded);
        assertNull(context.getDataContext().get("jobA.result"));
        assertEquals("B", context.getDataContext().get("jobB.result"));
    }

    @Test
    void fillMissingAncestorsShouldQueryLatestNodeResultByInstanceKey() {
        AdminApiClient adminApiClient = mock(AdminApiClient.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        HistoricalDataLoader loader = new HistoricalDataLoader(adminApiClient, fileStorageService);

        when(adminApiClient.getLatestNodeResult(1L, 100L, "http://127.0.0.1:8500/"))
                .thenReturn(Map.of("jobId", 100L, "jobName", "jobA", "resultData", "A"));

        ExecutionContext context = new ExecutionContext();
        context.setTaskGroupId(1L);
        context.setDataContext(new DataContext("run-1"));
        context.setInstanceKey("http://127.0.0.1:8500/");

        int filled = loader.fillMissingAncestors(context, 1L, Map.of(100L, "jobA"));

        assertEquals(1, filled);
        assertEquals("A", context.getDataContext().get("jobA.result"));
        verify(adminApiClient).getLatestNodeResult(1L, 100L, "http://127.0.0.1:8500/");
    }
}

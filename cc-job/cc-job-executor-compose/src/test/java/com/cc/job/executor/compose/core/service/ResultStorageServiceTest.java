package com.cc.job.executor.compose.core.service;

import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.context.DataContext;
import com.cc.job.executor.compose.core.model.ExecutionContext;
import com.cc.job.xo.model.result.ApiResult;
import com.cc.job.xo.model.result.NodeResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResultStorageServiceTest {

    @Test
    void persistResultToDatabaseShouldPreferRootNodeResultOverValueAlias() {
        AdminApiClient adminApiClient = mock(AdminApiClient.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        ResultStorageService service = new ResultStorageService(adminApiClient, fileStorageService);

        when(fileStorageService.saveToFile(eq(1L), eq("batch-1"), eq(100L), any())).thenReturn(null);
        when(adminApiClient.saveNodeResults(eq(1L), eq("batch-1"), eq("http://127.0.0.1:8500/"), any())).thenReturn(true);

        NodeResult nodeResult = NodeResult.success("ok", 1L);
        ApiResult apiResult = new ApiResult();
        apiResult.setStatusCode(200);
        apiResult.setRequestUrl("http://demo/api");
        nodeResult.setApiResult(apiResult);

        DataContext dataContext = new DataContext("batch-1");
        dataContext.put("api_job", nodeResult, 100L);
        dataContext.put("api_job.value", "simple-value", 100L);

        ExecutionContext context = ExecutionContext.builder()
                .taskGroupId(1L)
                .executionBatchId("batch-1")
                .dataContext(dataContext)
                .jobNameMap(Map.of("api job", 100L))
                .instanceKey("http://127.0.0.1:8500/")
                .build();

        int persisted = service.persistResultToDatabase(context, 1L, "batch-1");

        assertEquals(1, persisted);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(adminApiClient).saveNodeResults(eq(1L), eq("batch-1"), eq("http://127.0.0.1:8500/"), captor.capture());

        List<Map<String, Object>> savedResults = captor.getValue();
        assertEquals(1, savedResults.size());
        String resultData = (String) savedResults.get(0).get("resultData");
        assertEquals("http://127.0.0.1:8500/", savedResults.get(0).get("instanceKey"));
        assertTrue(resultData.contains("apiResult"));
        assertTrue(resultData.contains("statusCode"));
    }
}

package com.cc.job.executor.compose.service;

import com.cc.job.executor.compose.client.AdminApiClient;
import com.cc.job.executor.compose.core.context.DataContext;
import com.cc.job.executor.compose.core.resolver.ParameterResolver;
import com.cc.job.xo.model.entity.JobInfo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class JobTriggerServiceTest {

    @Test
    void resolveTemplateStrictShouldResolveExistingUpstreamResult() {
        JobTriggerService service = new JobTriggerService(mock(AdminApiClient.class), new ParameterResolver());
        JobInfo jobInfo = new JobInfo();
        jobInfo.setId(10L);

        DataContext dataContext = new DataContext("batch-1");
        dataContext.put("upstream.value", "ready", 1L);

        String resolved = ReflectionTestUtils.invokeMethod(
                service,
                "resolveTemplateStrict",
                "run=#{upstream}.value",
                "executorParam",
                jobInfo,
                dataContext,
                Map.of("upstream", 1L)
        );

        assertEquals("run=ready", resolved);
    }

    @Test
    void resolveTemplateStrictShouldThrowWhenUpstreamResultMissing() {
        JobTriggerService service = new JobTriggerService(mock(AdminApiClient.class), new ParameterResolver());
        JobInfo jobInfo = new JobInfo();
        jobInfo.setId(10L);

        DataContext dataContext = new DataContext("batch-1");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "resolveTemplateStrict",
                        "run=#{upstream}.value",
                        "executorParam",
                        jobInfo,
                        dataContext,
                        Map.of("upstream", 1L)
                )
        );

        assertEquals("缺失上游结果: upstream.value", exception.getMessage());
    }
}

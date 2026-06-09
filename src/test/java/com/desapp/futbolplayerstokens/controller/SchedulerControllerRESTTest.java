package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.scheduler.DynamicMatchScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerControllerRESTTest {

    @Mock
    private DynamicMatchScheduler dynamicMatchScheduler;

    @InjectMocks
    private SchedulerControllerREST schedulerController;

    @Test
    void getSchedulerStatus_active() {
        when(dynamicMatchScheduler.getScheduledMatchCount()).thenReturn(3);

        ResponseEntity<Map<String, Object>> result = schedulerController.getSchedulerStatus();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = result.getBody();
        assertEquals(3, body.get("scheduledMatches"));
        assertEquals("ACTIVE", body.get("status"));
    }

    @Test
    void getSchedulerStatus_idle() {
        when(dynamicMatchScheduler.getScheduledMatchCount()).thenReturn(0);

        ResponseEntity<Map<String, Object>> result = schedulerController.getSchedulerStatus();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = result.getBody();
        assertEquals(0, body.get("scheduledMatches"));
        assertEquals("IDLE", body.get("status"));
    }

    @Test
    void getScheduledMatches_returnsList() {
        List<DynamicMatchScheduler.MatchScheduleInfo> matches = List.of(
                new DynamicMatchScheduler.MatchScheduleInfo(1L, 10L, 20L, null),
                new DynamicMatchScheduler.MatchScheduleInfo(2L, 30L, 40L, null)
        );
        when(dynamicMatchScheduler.getAllScheduledMatches()).thenReturn(matches);

        ResponseEntity<Map<String, Object>> result = schedulerController.getScheduledMatches();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = result.getBody();
        assertEquals(2, body.get("total"));
        assertNotNull(body.get("matches"));
    }

    @Test
    void getScheduledMatches_empty() {
        when(dynamicMatchScheduler.getAllScheduledMatches()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> result = schedulerController.getScheduledMatches();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = result.getBody();
        assertEquals(0, body.get("total"));
        assertTrue(((List<?>) body.get("matches")).isEmpty());
    }

    @Test
    void scheduleAllForTesting_success() {
        doNothing().when(dynamicMatchScheduler).scheduleAllMatchesForTesting();
        when(dynamicMatchScheduler.getScheduledMatchCount()).thenReturn(5);

        ResponseEntity<Map<String, Object>> result = schedulerController.scheduleAllForTesting();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = result.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals(5, body.get("scheduledMatches"));
    }

    @Test
    void scheduleAllForTesting_error_returnsBadRequest() {
        doThrow(new RuntimeException("Scheduler error")).when(dynamicMatchScheduler).scheduleAllMatchesForTesting();

        ResponseEntity<Map<String, Object>> result = schedulerController.scheduleAllForTesting();

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<String, Object> body = result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("Scheduler error", body.get("error"));
    }
}

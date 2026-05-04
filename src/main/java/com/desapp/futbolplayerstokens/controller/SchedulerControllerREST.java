package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.scheduler.DynamicMatchScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerControllerREST {

    private final DynamicMatchScheduler dynamicMatchScheduler;

    public SchedulerControllerREST(DynamicMatchScheduler dynamicMatchScheduler) {
        this.dynamicMatchScheduler = dynamicMatchScheduler;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> response = new HashMap<>();
        int count = dynamicMatchScheduler.getScheduledMatchCount();
        response.put("scheduledMatches", count);
        response.put("status", count > 0 ? "ACTIVE" : "IDLE");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/matches")
    public ResponseEntity<Map<String, Object>> getScheduledMatches() {
        Map<String, Object> response = new HashMap<>();
        List<DynamicMatchScheduler.MatchScheduleInfo> matches = dynamicMatchScheduler.getAllScheduledMatches();
        response.put("total", matches.size());
        response.put("matches", matches);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test/schedule-all")
    public ResponseEntity<Map<String, Object>> scheduleAllForTesting() {
        Map<String, Object> response = new HashMap<>();
        try {
            dynamicMatchScheduler.scheduleAllMatchesForTesting();
            int count = dynamicMatchScheduler.getScheduledMatchCount();
            response.put("success", true);
            response.put("scheduledMatches", count);
            response.put("message", count + " partidos programados para testing");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

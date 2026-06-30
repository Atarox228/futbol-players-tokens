package com.desapp.futbolplayerstokens.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class DebugController {

    @GetMapping("/debug/env")
    public ResponseEntity<Map<String, String>> debugEnv() {
        Map<String, String> footballVars = System.getenv().entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains("football")
                        || e.getKey().toLowerCase().contains("api_token")
                        || e.getKey().toLowerCase().contains("token"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() != null ? "set (len=" + e.getValue().length() + ")" : "null"
                ));

        if (footballVars.isEmpty()) {
            footballVars.put("_note", "No FOOTBALL/TOKEN env vars found. Total env vars: " + System.getenv().size());
            footballVars.put("_sample", System.getenv().keySet().stream().sorted().limit(30).collect(Collectors.joining(", ")));
        }

        return ResponseEntity.ok(footballVars);
    }
}

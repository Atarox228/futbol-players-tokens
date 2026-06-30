package com.desapp.futbolplayerstokens.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}

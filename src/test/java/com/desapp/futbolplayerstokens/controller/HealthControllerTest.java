package com.desapp.futbolplayerstokens.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    private final HealthController healthController = new HealthController();

    @Test
    void health_returnsOk() {
        var response = healthController.health();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}

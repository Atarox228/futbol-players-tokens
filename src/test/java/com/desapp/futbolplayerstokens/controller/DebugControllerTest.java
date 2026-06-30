package com.desapp.futbolplayerstokens.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DebugControllerTest {

    private final DebugController debugController = new DebugController();

    @Test
    void debugEnv_returnsOk() {
        var response = debugController.debugEnv();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void debugEnv_returnsMap() {
        var response = debugController.debugEnv();

        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isEmpty());
    }
}

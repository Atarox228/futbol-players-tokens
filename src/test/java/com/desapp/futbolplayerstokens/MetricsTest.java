package com.desapp.futbolplayerstokens;

import com.desapp.futbolplayerstokens.scheduler.MatchScraperScheduler;
import com.desapp.futbolplayerstokens.scheduler.PlayerScraperScheduler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockitoBean(types = PlayerScraperScheduler.class)
@MockitoBean(types = MatchScraperScheduler.class)
class MetricsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void actuatorHealthEndpoint_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorPrometheusEndpoint_returnsMetrics() throws Exception {
        String response = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertNotNull(response);
        assertTrue(response.contains("jvm_memory_used_bytes"));
        assertTrue(response.contains("http_server_requests_seconds"));
    }

    @Test
    void customMetricsAreRegistered() {
        assertNotNull(meterRegistry.find("orders.buy.total").counter());
        assertNotNull(meterRegistry.find("orders.sell.total").counter());
        assertNotNull(meterRegistry.find("orders.filled.total").counter());
        assertNotNull(meterRegistry.find("orders.matching.duration").timer());
        assertNotNull(meterRegistry.find("quotes.recalculate.duration").timer());
        assertNotNull(meterRegistry.find("users.registrations.total").counter());
    }

    @Test
    void prometheusEndpointContainsCustomMetrics() throws Exception {
        String response = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(response.contains("orders_buy_total"));
        assertTrue(response.contains("orders_sell_total"));
        assertTrue(response.contains("orders_filled_total"));
        assertTrue(response.contains("orders_matching_duration"));
        assertTrue(response.contains("quotes_recalculate_duration"));
        assertTrue(response.contains("users_registrations_total"));
    }
}

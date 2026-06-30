package com.desapp.futbolplayerstokens.aspect;

import com.desapp.futbolplayerstokens.repository.AuditLogRepository;
import com.desapp.futbolplayerstokens.scheduler.DynamicMatchScheduler;
import com.desapp.futbolplayerstokens.scheduler.MatchScraperScheduler;
import com.desapp.futbolplayerstokens.scheduler.PlayerScraperScheduler;
import com.desapp.futbolplayerstokens.scheduler.QuoteScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockitoBean(types = PlayerScraperScheduler.class)
@MockitoBean(types = MatchScraperScheduler.class)
@MockitoBean(types = DynamicMatchScheduler.class)
@MockitoBean(types = QuoteScheduler.class)
class AuditAspectTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @Test
    void healthEndpoint_triggersAuditAspect() throws Exception {
        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(auditLogRepository, times(1)).save(any());
    }
}

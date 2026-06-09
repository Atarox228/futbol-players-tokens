package com.desapp.futbolplayerstokens;

import com.desapp.futbolplayerstokens.scheduler.MatchScraperScheduler;
import com.desapp.futbolplayerstokens.scheduler.PlayerScraperScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@MockitoBean(types = PlayerScraperScheduler.class)
@MockitoBean(types = MatchScraperScheduler.class)
class FutbolPlayersTokensApplicationTests {

    @Test
    void contextLoads() {
    }

}

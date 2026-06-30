package com.desapp.futbolplayerstokens.config;

import com.desapp.futbolplayerstokens.controller.dto.PlayerRankingDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlayerRankingListSerializerTest {

    private final PlayerRankingListSerializer serializer = new PlayerRankingListSerializer();

    @Test
    void serializeAndDeserialize_roundTrip_preservesRankingData() {
        Player player = Player.builder()
                .id(7L)
                .name("Lionel Messi")
                .team("Inter Miami")
                .position("Forward")
                .league("MLS")
                .altPosition("Attacking Midfielder")
                .score(new BigDecimal("99.5"))
                .build();

        List<PlayerRankingDTO> ranking = List.of(PlayerRankingDTO.of(player, 1));

        byte[] bytes = serializer.serialize(ranking);
        List<PlayerRankingDTO> restored = serializer.deserialize(bytes);

        assertNotNull(bytes);
        assertNotNull(restored);
        assertEquals(ranking, restored);
    }
}
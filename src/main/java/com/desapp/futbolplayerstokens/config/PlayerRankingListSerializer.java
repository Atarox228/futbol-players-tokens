package com.desapp.futbolplayerstokens.config;

import com.desapp.futbolplayerstokens.controller.dto.PlayerRankingDTO;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.List;

public class PlayerRankingListSerializer implements RedisSerializer<List<PlayerRankingDTO>> {

    private final ObjectMapper objectMapper;
    private final JavaType type;

    public PlayerRankingListSerializer() {
        this.objectMapper = new ObjectMapper();
        this.type = TypeFactory.defaultInstance()
                .constructCollectionType(List.class, PlayerRankingDTO.class);
    }

    @Override
    public byte[] serialize(List<PlayerRankingDTO> value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("Error serializing PlayerRankingDTO list", e);
        }
    }

    @Override
    public List<PlayerRankingDTO> deserialize(@Nullable byte[] bytes) throws SerializationException {
        if (bytes == null) return null;
        try {
            return objectMapper.readValue(bytes, type);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing PlayerRankingDTO list", e);
        }
    }
}
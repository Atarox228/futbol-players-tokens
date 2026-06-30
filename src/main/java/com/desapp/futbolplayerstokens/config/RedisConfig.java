package com.desapp.futbolplayerstokens.config;

import com.desapp.futbolplayerstokens.controller.dto.PlayerRankingDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
@Profile("!test & !e2e")
public class RedisConfig {

    @Bean
    public RedisTemplate<String, List<PlayerRankingDTO>> rankingRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, List<PlayerRankingDTO>> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new PlayerRankingListSerializer());
        redisTemplate.setHashValueSerializer(new PlayerRankingListSerializer());
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }
}
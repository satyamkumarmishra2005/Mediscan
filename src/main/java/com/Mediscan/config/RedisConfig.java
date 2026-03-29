package com.Mediscan.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory){
        RedisCacheConfiguration pricesCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        GenericJacksonJsonRedisSerializer.create(builder -> {
                        })))
                .disableCachingNullValues();


        // Generics cache: 24 hour TTL (alternatives change rarely)
        RedisCacheConfiguration genericsCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))

                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                GenericJacksonJsonRedisSerializer.create(builder -> {
                })))
                .disableCachingNullValues();


        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration("medicinePrices", pricesCacheConfig)
                .withCacheConfiguration("genericAlternatives", genericsCacheConfig)
                .build();

    }
}

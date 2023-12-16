package com.springboot.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
@Configuration
@EnableRedisRepositories
public class RedisConfig {

    @Value("${redis.port}")
    private int redisPort;
    @Value("${redis.hostname}")
    private String redisHostName;
    @Value("${redis.cache.default.ttl}")
    private int defaultCacheTtl;
    @Value("${redis.cache.feed.ttl}")
    private int feedCacheTtlHrs;
    @Value("${redis.cache.posts.ttl}")
    private int postsCacheTtlHrs;
    @Value("${redis.cache.comment.ttl}")
    private int commentsCacheTtlHrs;

    @Bean
    public JedisConnectionFactory connectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisHostName);
        configuration.setPort(redisPort);
        return new JedisConnectionFactory(configuration);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        jsonSerializer.setObjectMapper(objectMapper);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(defaultCacheTtl))
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // Set custom TTL for posts, disallow caching null values, and add custom prefix
        cacheConfigurations.put("posts", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(postsCacheTtlHrs))
                .disableCachingNullValues()
        );
        cacheConfigurations.put("comment", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(commentsCacheTtlHrs))
                .disableCachingNullValues()
        );
        cacheConfigurations.put("userRecommendedPosts", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(feedCacheTtlHrs))
                .disableCachingNullValues()
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
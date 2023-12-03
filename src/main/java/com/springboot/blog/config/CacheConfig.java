package com.springboot.blog.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the cache manager for the application.
 * Uses a ConcurrentMapCacheManager with specific settings.
 */
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager =
                new ConcurrentMapCacheManager("posts", "comment");
        cacheManager.setAllowNullValues(false); // disallow null values in the cache
        cacheManager.setStoreByValue(true); // allowed to store elements by value
        return cacheManager;
    }
}
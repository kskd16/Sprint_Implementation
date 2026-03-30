package com.smartSure.policyService.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Distributed Caching Configuration for PolicyService
 * 
 * This configuration class sets up Redis-based distributed caching to improve
 * performance by caching frequently accessed data such as policy types,
 * policy information, premium calculations, and customer data across multiple
 * service instances.
 * 
 * Key Features:
 * - Redis as the distributed cache store
 * - JSON serialization for complex objects
 * - Configurable TTL for different cache regions
 * - Connection pooling for optimal performance
 * - Cache eviction strategies for data consistency
 * 
 * @author SmartSure Development Team
 * @version 1.0
 * @since 2024-03-25
 */
@Slf4j
@Configuration
@EnableCaching
public class PolicyCacheConfig {

    /**
     * Cache region names used throughout the PolicyService
     */
    public static final String POLICY_TYPES_CACHE = "policy-types";
    public static final String POLICIES_CACHE = "policies";
    public static final String PREMIUMS_CACHE = "premiums";
    public static final String PREMIUM_CALCULATIONS_CACHE = "premium-calculations";
    public static final String CUSTOMER_POLICIES_CACHE = "customer-policies";
    public static final String POLICY_SUMMARY_CACHE = "policy-summary";

    /**
     * Configures the Redis-based cache manager with custom serialization
     * and TTL settings for different cache regions.
     * 
     * @param redisConnectionFactory Redis connection factory
     * @return Configured RedisCacheManager instance
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        try {
            redisConnectionFactory.getConnection().ping();
            log.info("Redis available — initializing distributed cache manager for PolicyService");

            Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
            objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                                             ObjectMapper.DefaultTyping.NON_FINAL);
            jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jackson2JsonRedisSerializer))
                    .disableCachingNullValues();

            return RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withCacheConfiguration(POLICY_TYPES_CACHE,
                        defaultConfig.entryTtl(Duration.ofHours(4)))
                    .withCacheConfiguration(POLICIES_CACHE,
                        defaultConfig.entryTtl(Duration.ofMinutes(30)))
                    .withCacheConfiguration(PREMIUMS_CACHE,
                        defaultConfig.entryTtl(Duration.ofMinutes(15)))
                    .withCacheConfiguration(PREMIUM_CALCULATIONS_CACHE,
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                    .withCacheConfiguration(CUSTOMER_POLICIES_CACHE,
                        defaultConfig.entryTtl(Duration.ofMinutes(20)))
                    .withCacheConfiguration(POLICY_SUMMARY_CACHE,
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                    .build();

        } catch (Exception e) {
            log.warn("Redis unavailable ({}). Falling back to in-memory cache for PolicyService.",
                     e.getMessage());
            return new org.springframework.cache.concurrent.ConcurrentMapCacheManager(
                    POLICY_TYPES_CACHE, POLICIES_CACHE, PREMIUMS_CACHE,
                    PREMIUM_CALCULATIONS_CACHE, CUSTOMER_POLICIES_CACHE, POLICY_SUMMARY_CACHE);
        }
    }

    /**
     * Configures RedisTemplate for manual cache operations and complex queries.
     * 
     * @param redisConnectionFactory Redis connection factory
     * @return Configured RedisTemplate instance
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.debug("Configuring RedisTemplate for manual cache operations in PolicyService");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // Configure serializers
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, 
                                         ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        // Set serializers
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        
        log.debug("RedisTemplate configured successfully for PolicyService");
        return template;
    }
}
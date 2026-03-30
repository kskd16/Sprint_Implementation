package com.smartSure.claimService.config;

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
 * Distributed Caching Configuration for ClaimService
 * 
 * This configuration class sets up Redis-based distributed caching to improve
 * performance by caching frequently accessed data such as claim information,
 * policy details, document metadata, and claim statistics across multiple
 * service instances.
 * 
 * Key Features:
 * - Redis as the distributed cache store
 * - JSON serialization for complex objects
 * - Configurable TTL for different cache regions
 * - Connection pooling for optimal performance
 * - Cache eviction strategies for data consistency
 * - Document metadata caching for faster retrieval
 * 
 * @author SmartSure Development Team
 * @version 1.0
 * @since 2024-03-25
 */
@Slf4j
@Configuration
@EnableCaching
public class ClaimCacheConfig {

    /**
     * Cache region names used throughout the ClaimService
     */
    public static final String CLAIMS_CACHE = "claims";
    public static final String CLAIM_DOCUMENTS_CACHE = "claim-documents";
    public static final String POLICY_DETAILS_CACHE = "policy-details";
    public static final String CLAIM_STATISTICS_CACHE = "claim-statistics";
    public static final String UNDER_REVIEW_CLAIMS_CACHE = "under-review-claims";
    public static final String CUSTOMER_CLAIMS_CACHE = "customer-claims";

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
            log.info("Redis available — initializing distributed cache manager for ClaimService");

            Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
            ObjectMapper om = new ObjectMapper();
            om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
            om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
            serializer.setObjectMapper(om);

            RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(15))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                    .disableCachingNullValues();

            return RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(base)
                    .withCacheConfiguration(CLAIMS_CACHE,              base.entryTtl(Duration.ofMinutes(20)))
                    .withCacheConfiguration(CLAIM_DOCUMENTS_CACHE,     base.entryTtl(Duration.ofHours(2)))
                    .withCacheConfiguration(POLICY_DETAILS_CACHE,      base.entryTtl(Duration.ofMinutes(30)))
                    .withCacheConfiguration(CLAIM_STATISTICS_CACHE,    base.entryTtl(Duration.ofMinutes(5)))
                    .withCacheConfiguration(UNDER_REVIEW_CLAIMS_CACHE, base.entryTtl(Duration.ofMinutes(10)))
                    .withCacheConfiguration(CUSTOMER_CLAIMS_CACHE,     base.entryTtl(Duration.ofMinutes(15)))
                    .build();

        } catch (Exception e) {
            log.warn("Redis unavailable ({}). Falling back to in-memory cache for ClaimService.", e.getMessage());
            return new org.springframework.cache.concurrent.ConcurrentMapCacheManager(
                    CLAIMS_CACHE, CLAIM_DOCUMENTS_CACHE, POLICY_DETAILS_CACHE,
                    CLAIM_STATISTICS_CACHE, UNDER_REVIEW_CLAIMS_CACHE, CUSTOMER_CLAIMS_CACHE);
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
        log.debug("Configuring RedisTemplate for manual cache operations in ClaimService");
        
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
        
        log.debug("RedisTemplate configured successfully for ClaimService");
        return template;
    }
}
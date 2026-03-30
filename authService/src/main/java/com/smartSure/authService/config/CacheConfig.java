package com.smartSure.authService.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
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
 * Distributed Caching Configuration for AuthService.
 *
 * Uses Redis when available. If Redis is down at startup the application
 * falls back to an in-memory ConcurrentMapCacheManager so the service
 * still starts and handles requests — caching is simply skipped.
 *
 * Cache regions:
 *   users           — 2 hours TTL
 *   jwt-blacklist   — 1 hour  TTL
 *   login-attempts  — 15 min  TTL
 *   user-roles      — 45 min  TTL
 *
 * @author SmartSure Development Team
 * @version 2.1
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USER_CACHE           = "users";
    public static final String JWT_BLACKLIST_CACHE  = "jwt-blacklist";
    public static final String LOGIN_ATTEMPTS_CACHE = "login-attempts";
    public static final String USER_ROLES_CACHE     = "user-roles";

    /**
     * Builds a RedisCacheManager. Falls back to in-memory cache if Redis
     * is not reachable so the application starts cleanly in all environments.
     *
     * @param redisConnectionFactory Redis connection factory
     * @return CacheManager backed by Redis or in-memory fallback
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        try {
            // Verify Redis is reachable before wiring the full cache manager
            redisConnectionFactory.getConnection().ping();

            log.info("Redis is available — initializing distributed Redis cache manager");

            Jackson2JsonRedisSerializer<Object> serializer =
                    new Jackson2JsonRedisSerializer<>(Object.class);

            ObjectMapper om = new ObjectMapper();
            om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
            om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                                     ObjectMapper.DefaultTyping.NON_FINAL);
            serializer.setObjectMapper(om);

            RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(serializer))
                    .disableCachingNullValues();

            return RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(base)
                    .withCacheConfiguration(USER_CACHE,
                            base.entryTtl(Duration.ofHours(2)))
                    .withCacheConfiguration(JWT_BLACKLIST_CACHE,
                            base.entryTtl(Duration.ofHours(1)))
                    .withCacheConfiguration(LOGIN_ATTEMPTS_CACHE,
                            base.entryTtl(Duration.ofMinutes(15)))
                    .withCacheConfiguration(USER_ROLES_CACHE,
                            base.entryTtl(Duration.ofMinutes(45)))
                    .build();

        } catch (Exception e) {
            log.warn("Redis unavailable ({}). Falling back to in-memory cache — " +
                     "distributed caching disabled until Redis is reachable.", e.getMessage());
            return new ConcurrentMapCacheManager(
                    USER_CACHE, JWT_BLACKLIST_CACHE, LOGIN_ATTEMPTS_CACHE, USER_ROLES_CACHE);
        }
    }

    /**
     * RedisTemplate for manual key/value operations.
     * Safe to inject — operations will throw at runtime if Redis is down,
     * but the application context itself will not fail to start.
     *
     * @param redisConnectionFactory Redis connection factory
     * @return configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(Object.class);

        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                                 ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(om);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        log.debug("RedisTemplate configured for AuthService");
        return template;
    }
}

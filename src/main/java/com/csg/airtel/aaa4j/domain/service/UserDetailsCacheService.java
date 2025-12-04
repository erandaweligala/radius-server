package com.csg.airtel.aaa4j.domain.service;

import com.csg.airtel.aaa4j.domain.model.UserDetails;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * Redis-based caching service for user authentication details
 * Improves performance by reducing calls to external authentication service
 */
@ApplicationScoped
public class UserDetailsCacheService {

    private static final Logger logger = Logger.getLogger(UserDetailsCacheService.class);
    private static final String CACHE_KEY_PREFIX = "auth:user:";

    private final ReactiveValueCommands<String, UserDetails> cache;

    @ConfigProperty(name = "auth.cache.ttl.seconds", defaultValue = "300")
    long cacheTtlSeconds; // Default 5 minutes

    @ConfigProperty(name = "auth.cache.enabled", defaultValue = "true")
    boolean cacheEnabled;

    @Inject
    public UserDetailsCacheService(ReactiveRedisDataSource redisDataSource) {
        this.cache = redisDataSource.value(UserDetails.class);
    }

    /**
     * Get user details from cache
     * @param username The username
     * @return Uni with cached UserDetails or null if not found
     */
    public Uni<UserDetails> get(String username) {
        if (!cacheEnabled) {
            return Uni.createFrom().nullItem();
        }

        String cacheKey = CACHE_KEY_PREFIX + username;
        return cache.get(cacheKey)
                .onItem().invoke(userDetails -> {
                    if (userDetails != null) {
                        logger.infof("Cache HIT for user: %s", username);
                    } else {
                        logger.infof("Cache MISS for user: %s", username);
                    }
                })
                .onFailure().invoke(throwable ->
                    logger.warnf(throwable, "Failed to get user from cache: %s", username)
                )
                .onFailure().recoverWithNull(); // Return null on cache failure, don't fail auth
    }

    /**
     * Store user details in cache
     * @param username The username
     * @param userDetails The user details to cache
     * @return Uni that completes when cache operation finishes
     */
    public Uni<Void> set(String username, UserDetails userDetails) {
        if (!cacheEnabled) {
            return Uni.createFrom().voidItem();
        }

        // Only cache successful authentication results
        if (userDetails == null || !userDetails.getIsAuthorized()) {
            logger.debugf("Not caching unauthorized user: %s", username);
            return Uni.createFrom().voidItem();
        }

        String cacheKey = CACHE_KEY_PREFIX + username;
        Duration ttl = Duration.ofSeconds(cacheTtlSeconds);

        return cache.set(cacheKey, userDetails, ttl)
                .replaceWithVoid()
                .onItem().invoke(() ->
                    logger.infof("Cached user details for: %s (TTL: %d seconds)", username, cacheTtlSeconds)
                )
                .onFailure().invoke(throwable ->
                    logger.warnf(throwable, "Failed to cache user details for: %s", username)
                )
                .onFailure().recoverWithNull(); // Don't fail auth if caching fails
    }

    /**
     * Invalidate cached user details
     * @param username The username
     * @return Uni that completes when invalidation finishes
     */
    public Uni<Void> invalidate(String username) {
        if (!cacheEnabled) {
            return Uni.createFrom().voidItem();
        }

        String cacheKey = CACHE_KEY_PREFIX + username;
        return cache.delete(cacheKey)
                .replaceWithVoid()
                .onItem().invoke(() ->
                    logger.infof("Invalidated cache for user: %s", username)
                )
                .onFailure().invoke(throwable ->
                    logger.warnf(throwable, "Failed to invalidate cache for: %s", username)
                )
                .onFailure().recoverWithNull();
    }
}

package com.example.note.utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Limits sensitive actions (forgot-password, verify, reset) per key (email/IP/token).
 * Uses Redis when available.
 */
@Component
public class AttemptLimiter {

    private static final String PREFIX = "attempt:";

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${security.attempts.max:5}")
    private int maxAttempts;

    @Value("${security.attempts.window-minutes:15}")
    private int windowMinutes;

    /**
     * @return true if allowed; false if limit exceeded
     */
    public boolean tryConsume(String action, String key) {
        String redisKey = PREFIX + action + ":" + key;
        if (redisTemplate == null) {
            return true; // fail-open if Redis missing (dev only)
        }
        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(redisKey, windowMinutes, TimeUnit.MINUTES);
            }
            return count == null || count <= maxAttempts;
        } catch (Exception e) {
            return true;
        }
    }

    public void reset(String action, String key) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(PREFIX + action + ":" + key);
        } catch (Exception ignored) {
            // ignore
        }
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }
}

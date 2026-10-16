package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Value("${note.service.uri:http://localhost:8081}")
    private String noteServiceUri;

    /** Rate limit key = client IP (supports X-Forwarded-For). */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
//        exchange adalah ServerWebExchange, objek WebFlux yang menyediakan konteks request dan response HTTP.
        return exchange -> {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return Mono.just(forwarded.split(",")[0].trim());
            }
            if (exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }
            return Mono.just("unknown");
        };
    }

    /**
     * Redis token-bucket rate limiter.
     * replenishRate = tokens added per second
     * burstCapacity = max burst size
     */
    @Bean
    public RedisRateLimiter redisRateLimiter(
            @Value("${rate-limit.replenish-rate:10}") int replenishRate,
            @Value("${rate-limit.burst-capacity:20}") int burstCapacity
    ) {
        return new RedisRateLimiter(replenishRate, burstCapacity);
    }

    @Bean
    public RouteLocator customRouteLocator(
            RouteLocatorBuilder builder,
            RedisRateLimiter redisRateLimiter,
            KeyResolver ipKeyResolver
    ) {
        return builder.routes()
                // Note Application — business API
                .route("note-service", r -> r
                        .path("/api/notes/**", "/api/users/**", "/api/auth/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter)
                                        .setKeyResolver(ipKeyResolver)
                                )
                                .addResponseHeader("X-Gateway", "api-gateway")
                                .addResponseHeader("X-Target-Service", "note-service")
                        )
                        .uri(noteServiceUri)
                )
                // Swagger from note service
                .route("note-swagger", r -> r
                        .path("/swagger-ui.html", "/swagger-resources/**", "/v2/api-docs", "/webjars/**", "/note/**")
                        .uri(noteServiceUri)
                )
                .build();
    }
}

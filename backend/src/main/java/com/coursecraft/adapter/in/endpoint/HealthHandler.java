package com.coursecraft.adapter.in.endpoint;

import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

/** Public liveness endpoint — no auth. Handler for {@code GET /api/health}. */
public final class HealthHandler {

    public ServerResponse health(ServerRequest request) {
        return ServerResponse.ok().body(Map.of(
                "status", "UP",
                "service", "coursecraft-backend"));
    }
}

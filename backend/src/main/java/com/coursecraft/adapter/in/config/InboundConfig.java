package com.coursecraft.adapter.in.config;

import com.coursecraft.adapter.in.auth.RequestAuth;
import com.coursecraft.adapter.in.endpoint.HealthHandler;
import com.coursecraft.adapter.in.endpoint.MeHandler;
import com.coursecraft.domain.service.ProfileService;
import com.coursecraft.port.TokenVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * One of exactly three wiring classes. Builds inbound handlers and the functional routes by hand.
 * Routing is functional ({@link RouterFunction}) rather than annotated controllers, which keeps
 * the app free of {@code @Controller} stereotypes and sidesteps the Spring 6.2 rule that only
 * {@code @Controller} types are routed.
 */
@Configuration(proxyBeanMethods = false)
public class InboundConfig {

    @Bean
    HealthHandler healthHandler() {
        return new HealthHandler();
    }

    @Bean
    MeHandler meHandler(ProfileService profileService) {
        return new MeHandler(profileService);
    }

    @Bean
    RequestAuth requestAuth(TokenVerifier tokenVerifier) {
        return new RequestAuth(tokenVerifier);
    }

    /** Public routes — no auth. */
    @Bean
    RouterFunction<ServerResponse> publicRoutes(HealthHandler healthHandler) {
        return route(GET("/api/health"), healthHandler::health);
    }

    /** Authenticated routes — every route wrapped by the bearer-auth filter. */
    @Bean
    RouterFunction<ServerResponse> securedRoutes(MeHandler meHandler, RequestAuth requestAuth) {
        return route(GET("/api/me"), meHandler::me)
                .andRoute(POST("/api/creators"), meHandler::becomeCreator)
                .filter(requestAuth.required());
    }
}

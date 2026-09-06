package com.coursecraft.adapter.in.config;

import com.coursecraft.adapter.in.auth.RequestAuth;
import com.coursecraft.adapter.in.endpoint.CourseHandler;
import com.coursecraft.adapter.in.endpoint.HealthHandler;
import com.coursecraft.adapter.in.endpoint.MeHandler;
import com.coursecraft.adapter.in.error.ErrorMapping;
import com.coursecraft.domain.service.CourseService;
import com.coursecraft.domain.service.ProfileService;
import com.coursecraft.port.TokenVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.DELETE;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RequestPredicates.PUT;
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
    CourseHandler courseHandler(ProfileService profileService, CourseService courseService) {
        return new CourseHandler(profileService, courseService);
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

    /**
     * Authenticated routes. Inner filter maps domain exceptions to HTTP status; outer filter
     * enforces the bearer token. Specific paths ({@code /mine}, {@code /catalog}) are registered
     * before the {@code /{courseId}} pattern so they aren't swallowed by it.
     */
    @Bean
    RouterFunction<ServerResponse> securedRoutes(MeHandler meHandler, CourseHandler courseHandler,
                                                 RequestAuth requestAuth) {
        return route(GET("/api/me"), meHandler::me)
                .andRoute(POST("/api/creators"), meHandler::becomeCreator)
                .andRoute(POST("/api/courses"), courseHandler::createCourse)
                .andRoute(GET("/api/courses/mine"), courseHandler::myCourses)
                .andRoute(GET("/api/catalog"), courseHandler::catalog)
                .andRoute(POST("/api/courses/{courseId}/sections/{sectionId}/lectures"),
                        courseHandler::addLecture)
                .andRoute(POST("/api/courses/{courseId}/sections"), courseHandler::addSection)
                .andRoute(POST("/api/courses/{courseId}/publish"), courseHandler::publish)
                // editing / reordering (PUT, not PATCH — Java's HttpURLConnection can't do PATCH;
                // "order" routes are registered before the {id} routes so they aren't shadowed)
                .andRoute(PUT("/api/courses/{courseId}/sections/order"), courseHandler::reorderSections)
                .andRoute(PUT("/api/courses/{courseId}/sections/{sectionId}/lectures/order"),
                        courseHandler::reorderLectures)
                .andRoute(PUT("/api/courses/{courseId}/sections/{sectionId}/lectures/{lectureId}"),
                        courseHandler::updateLecture)
                .andRoute(DELETE("/api/courses/{courseId}/sections/{sectionId}/lectures/{lectureId}"),
                        courseHandler::deleteLecture)
                .andRoute(PUT("/api/courses/{courseId}/sections/{sectionId}"), courseHandler::renameSection)
                .andRoute(DELETE("/api/courses/{courseId}/sections/{sectionId}"), courseHandler::deleteSection)
                .andRoute(PUT("/api/courses/{courseId}"), courseHandler::renameCourse)
                .andRoute(DELETE("/api/courses/{courseId}"), courseHandler::deleteCourse)
                .andRoute(GET("/api/courses/{courseId}"), courseHandler::courseDetail)
                .filter(ErrorMapping.filter())
                .filter(requestAuth.required());
    }
}

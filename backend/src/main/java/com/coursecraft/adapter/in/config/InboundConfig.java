package com.coursecraft.adapter.in.config;

import com.coursecraft.adapter.in.auth.RequestAuth;
import com.coursecraft.adapter.in.endpoint.AnalyticsHandler;
import com.coursecraft.adapter.in.endpoint.AssessmentHandler;
import com.coursecraft.adapter.in.endpoint.CourseHandler;
import com.coursecraft.adapter.in.endpoint.HealthHandler;
import com.coursecraft.adapter.in.endpoint.LiveHandler;
import com.coursecraft.adapter.in.endpoint.MeHandler;
import com.coursecraft.adapter.in.endpoint.ProgressHandler;
import com.coursecraft.adapter.in.error.ErrorMapping;
import com.coursecraft.domain.service.AnalyticsService;
import com.coursecraft.domain.service.AssessmentService;
import com.coursecraft.domain.service.CourseService;
import com.coursecraft.domain.service.EnrollmentService;
import com.coursecraft.domain.service.LiveSessionService;
import com.coursecraft.domain.service.ProfileService;
import com.coursecraft.domain.service.ProgressService;
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
    CourseHandler courseHandler(ProfileService profileService, CourseService courseService,
                                EnrollmentService enrollmentService) {
        return new CourseHandler(profileService, courseService, enrollmentService);
    }

    @Bean
    AssessmentHandler assessmentHandler(ProfileService profileService, AssessmentService assessmentService) {
        return new AssessmentHandler(profileService, assessmentService);
    }

    @Bean
    ProgressHandler progressHandler(ProfileService profileService, ProgressService progressService) {
        return new ProgressHandler(profileService, progressService);
    }

    @Bean
    LiveHandler liveHandler(ProfileService profileService, LiveSessionService liveSessionService) {
        return new LiveHandler(profileService, liveSessionService);
    }

    @Bean
    AnalyticsHandler analyticsHandler(ProfileService profileService, AnalyticsService analyticsService) {
        return new AnalyticsHandler(profileService, analyticsService);
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
                                                 AssessmentHandler assessmentHandler,
                                                 ProgressHandler progressHandler,
                                                 LiveHandler liveHandler,
                                                 AnalyticsHandler analyticsHandler,
                                                 RequestAuth requestAuth) {
        return route(GET("/api/me"), meHandler::me)
                .andRoute(POST("/api/creators"), meHandler::becomeCreator)
                .andRoute(PUT("/api/me/display-name"), meHandler::updateDisplayName)
                .andRoute(POST("/api/courses"), courseHandler::createCourse)
                .andRoute(GET("/api/courses/mine"), courseHandler::myCourses)
                .andRoute(GET("/api/catalog"), courseHandler::catalog)
                .andRoute(POST("/api/courses/{courseId}/sections/{sectionId}/lectures"),
                        courseHandler::addLecture)
                .andRoute(POST("/api/courses/{courseId}/sections"), courseHandler::addSection)
                .andRoute(POST("/api/courses/{courseId}/publish"), courseHandler::publish)
                // enrollment (LJ-1)
                .andRoute(GET("/api/my/learning"), courseHandler::myLearning)
                .andRoute(POST("/api/courses/{courseId}/enroll"), courseHandler::enroll)
                .andRoute(DELETE("/api/courses/{courseId}/enroll"), courseHandler::unenroll)
                // assessments (CJ-4, LJ-4, LJ-5)
                .andRoute(POST("/api/courses/{courseId}/assessments"), assessmentHandler::create)
                .andRoute(GET("/api/courses/{courseId}/assessments"), assessmentHandler::listForCourse)
                .andRoute(POST("/api/assessments/{assessmentId}/questions"), assessmentHandler::addQuestion)
                .andRoute(GET("/api/assessments/{assessmentId}/take"), assessmentHandler::take)
                .andRoute(POST("/api/assessments/{assessmentId}/submit"), assessmentHandler::submit)
                .andRoute(GET("/api/assessments/{assessmentId}/leaderboard"), assessmentHandler::leaderboard)
                // progress (LJ-2, LJ-3)
                .andRoute(PUT("/api/progress"), progressHandler::save)
                .andRoute(GET("/api/my/continue"), progressHandler::continueLearning)
                .andRoute(GET("/api/lectures/{lectureId}/progress"), progressHandler::get)
                .andRoute(GET("/api/courses/{courseId}/progress"), progressHandler::courseProgress)
                // live lectures (CJ-7, LJ-7)
                .andRoute(POST("/api/courses/{courseId}/live"), liveHandler::schedule)
                .andRoute(GET("/api/courses/{courseId}/live"), liveHandler::listForCourse)
                .andRoute(POST("/api/live/{sessionId}/start"), liveHandler::goLive)
                .andRoute(POST("/api/live/{sessionId}/end"), liveHandler::end)
                .andRoute(POST("/api/live/{sessionId}/cancel"), liveHandler::cancel)
                // analytics (CJ-6)
                .andRoute(GET("/api/courses/{courseId}/analytics"), analyticsHandler::courseAnalytics)
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

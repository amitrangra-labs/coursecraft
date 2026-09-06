package com.coursecraft.adapter.in.endpoint;

import com.coursecraft.adapter.in.auth.RequestAuth;
import com.coursecraft.domain.object.CourseAnalytics;
import com.coursecraft.domain.object.User;
import com.coursecraft.domain.service.AnalyticsService;
import com.coursecraft.domain.service.ProfileService;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Creator analytics endpoint (journey CJ-6). */
public final class AnalyticsHandler {

    private final ProfileService profileService;
    private final AnalyticsService analyticsService;

    public AnalyticsHandler(ProfileService profileService, AnalyticsService analyticsService) {
        this.profileService = profileService;
        this.analyticsService = analyticsService;
    }

    public ServerResponse courseAnalytics(ServerRequest request) {
        User user = profileService.getOrProvision(RequestAuth.tokenOf(request));
        UUID courseId = UUID.fromString(request.pathVariable("courseId"));
        CourseAnalytics a = analyticsService.courseAnalytics(user, courseId);
        List<Map<String, Object>> assessments = a.assessments().stream()
                .map(s -> Map.<String, Object>of(
                        "title", s.title(),
                        "attempts", s.attempts(),
                        "averagePercent", s.averagePercent()))
                .toList();
        return ServerResponse.ok().body(Map.of(
                "enrollments", a.enrollments(),
                "assessments", assessments));
    }
}

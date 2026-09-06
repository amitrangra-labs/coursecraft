package com.coursecraft.domain.config;

import com.coursecraft.domain.service.AssessmentService;
import com.coursecraft.domain.service.CourseService;
import com.coursecraft.domain.service.EnrollmentService;
import com.coursecraft.domain.service.LiveSessionService;
import com.coursecraft.domain.service.ProfileService;
import com.coursecraft.domain.service.ProgressService;
import com.coursecraft.port.AssessmentStore;
import com.coursecraft.port.CourseStore;
import com.coursecraft.port.EnrollmentStore;
import com.coursecraft.port.LiveSessionStore;
import com.coursecraft.port.ProgressStore;
import com.coursecraft.port.UserStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One of exactly three wiring classes. Builds the domain services by hand, injecting the
 * outbound ports (implemented in {@code OutboundConfig}). No component scanning, no
 * {@code @Autowired}.
 */
@Configuration(proxyBeanMethods = false)
public class DomainConfig {

    @Bean
    ProfileService profileService(UserStore userStore) {
        return new ProfileService(userStore);
    }

    @Bean
    CourseService courseService(CourseStore courseStore) {
        return new CourseService(courseStore);
    }

    @Bean
    EnrollmentService enrollmentService(EnrollmentStore enrollmentStore, CourseStore courseStore) {
        return new EnrollmentService(enrollmentStore, courseStore);
    }

    @Bean
    AssessmentService assessmentService(AssessmentStore assessmentStore, CourseStore courseStore) {
        return new AssessmentService(assessmentStore, courseStore);
    }

    @Bean
    ProgressService progressService(ProgressStore progressStore, CourseStore courseStore) {
        return new ProgressService(progressStore, courseStore);
    }

    @Bean
    LiveSessionService liveSessionService(LiveSessionStore liveSessionStore, CourseStore courseStore) {
        return new LiveSessionService(liveSessionStore, courseStore);
    }
}

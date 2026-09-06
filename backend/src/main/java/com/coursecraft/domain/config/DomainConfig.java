package com.coursecraft.domain.config;

import com.coursecraft.domain.service.CourseService;
import com.coursecraft.domain.service.EnrollmentService;
import com.coursecraft.domain.service.ProfileService;
import com.coursecraft.port.CourseStore;
import com.coursecraft.port.EnrollmentStore;
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
}

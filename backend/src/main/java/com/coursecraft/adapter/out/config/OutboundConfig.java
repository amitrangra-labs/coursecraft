package com.coursecraft.adapter.out.config;

import com.coursecraft.adapter.out.client.JdbcAssessmentStore;
import com.coursecraft.adapter.out.client.JdbcCourseStore;
import com.coursecraft.adapter.out.client.JdbcEnrollmentStore;
import com.coursecraft.adapter.out.client.JdbcProgressStore;
import com.coursecraft.adapter.out.client.JdbcUserStore;
import com.coursecraft.adapter.out.client.NimbusTokenVerifier;
import com.coursecraft.port.AssessmentStore;
import com.coursecraft.port.CourseStore;
import com.coursecraft.port.EnrollmentStore;
import com.coursecraft.port.ProgressStore;
import com.coursecraft.port.TokenVerifier;
import com.coursecraft.port.UserStore;
import com.nimbusds.jose.JWSAlgorithm;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Arrays;
import java.util.List;

/**
 * One of exactly three wiring classes. Builds the outbound adapters by hand. Config values are
 * read from the {@link Environment} (not {@code @Value}). {@code JdbcClient} is auto-configured
 * by Spring Boot from {@code spring.datasource.*}.
 */
@Configuration(proxyBeanMethods = false)
public class OutboundConfig {

    @Bean
    UserStore userStore(JdbcClient jdbcClient) {
        return new JdbcUserStore(jdbcClient);
    }

    @Bean
    CourseStore courseStore(JdbcClient jdbcClient) {
        return new JdbcCourseStore(jdbcClient);
    }

    @Bean
    EnrollmentStore enrollmentStore(JdbcClient jdbcClient) {
        return new JdbcEnrollmentStore(jdbcClient);
    }

    @Bean
    AssessmentStore assessmentStore(JdbcClient jdbcClient) {
        return new JdbcAssessmentStore(jdbcClient);
    }

    @Bean
    ProgressStore progressStore(JdbcClient jdbcClient) {
        return new JdbcProgressStore(jdbcClient);
    }

    @Bean
    TokenVerifier tokenVerifier(Environment env) {
        String jwksUri = required(env, "coursecraft.auth.jwks-uri");
        String issuer = required(env, "coursecraft.auth.issuer");
        List<JWSAlgorithm> algorithms = parseAlgorithms(
                env.getProperty("coursecraft.auth.algorithms", "RS256,ES256"));
        return new NimbusTokenVerifier(jwksUri, issuer, algorithms);
    }

    private static String required(Environment env, String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config: " + key);
        }
        return value;
    }

    private static List<JWSAlgorithm> parseAlgorithms(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(JWSAlgorithm::parse)
                .toList();
    }
}

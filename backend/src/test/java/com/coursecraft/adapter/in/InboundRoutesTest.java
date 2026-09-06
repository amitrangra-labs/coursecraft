package com.coursecraft.adapter.in;

import com.coursecraft.adapter.in.config.InboundConfig;
import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.User;
import com.coursecraft.domain.object.VerifiedToken;
import com.coursecraft.domain.service.AssessmentService;
import com.coursecraft.domain.service.CourseService;
import com.coursecraft.domain.service.EnrollmentService;
import com.coursecraft.domain.service.LiveSessionService;
import com.coursecraft.domain.service.ProfileService;
import com.coursecraft.domain.service.ProgressService;
import com.coursecraft.port.CourseStore;
import com.coursecraft.port.TokenVerifier;
import com.coursecraft.port.UserStore;
import com.coursecraft.testsupport.InMemoryAssessmentStore;
import com.coursecraft.testsupport.InMemoryCourseStore;
import com.coursecraft.testsupport.InMemoryEnrollmentStore;
import com.coursecraft.testsupport.InMemoryLiveSessionStore;
import com.coursecraft.testsupport.InMemoryProgressStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the inbound slice end-to-end without any database or network: functional routing, the
 * bearer-auth filter, and the profile handlers. Real {@link InboundConfig} is imported; the
 * outbound ports are replaced by in-memory fakes.
 */
@WebMvcTest
@Import({InboundConfig.class, InboundRoutesTest.Fakes.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InboundRoutesTest {

    @Autowired
    MockMvc mvc;

    @Test
    void health_isPublic() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void me_withoutToken_is401() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withBadToken_is401() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", "Bearer nope"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_provisionsLearner() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", "Bearer good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LEARNER"))
                .andExpect(jsonPath("$.displayName").value("kofi"));
    }

    @Test
    void becomeCreator_upgradesRole() throws Exception {
        mvc.perform(post("/api/creators").header("Authorization", "Bearer good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CREATOR"));
    }

    @Test
    void catalog_authenticated_returnsList() throws Exception {
        mvc.perform(get("/api/catalog").header("Authorization", "Bearer good"))
                .andExpect(status().isOk());
    }

    @Test
    void createCourse_asLearner_is403() throws Exception {
        mvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer good")
                        .contentType("application/json")
                        .content("{\"title\":\"Algebra\"}"))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Fakes {
        @Bean
        ProfileService profileService(UserStore userStore) {
            return new ProfileService(userStore);
        }

        @Bean
        CourseStore courseStore() {
            return new InMemoryCourseStore();
        }

        @Bean
        CourseService courseService(CourseStore courseStore) {
            return new CourseService(courseStore);
        }

        @Bean
        EnrollmentService enrollmentService(CourseStore courseStore) {
            return new EnrollmentService(new InMemoryEnrollmentStore(courseStore), courseStore);
        }

        @Bean
        AssessmentService assessmentService(CourseStore courseStore) {
            return new AssessmentService(new InMemoryAssessmentStore(), courseStore);
        }

        @Bean
        ProgressService progressService(CourseStore courseStore) {
            return new ProgressService(new InMemoryProgressStore(), courseStore);
        }

        @Bean
        LiveSessionService liveSessionService(CourseStore courseStore) {
            return new LiveSessionService(new InMemoryLiveSessionStore(), courseStore);
        }

        @Bean
        UserStore userStore() {
            return new InMemoryUserStore();
        }

        @Bean
        TokenVerifier tokenVerifier() {
            return token -> {
                if ("good".equals(token)) {
                    return new VerifiedToken("sub-1", "kofi@example.org");
                }
                throw new TokenVerifier.InvalidTokenException("bad token");
            };
        }
    }

    private static final class InMemoryUserStore implements UserStore {
        private final Map<String, User> bySubject = new HashMap<>();
        private final Map<UUID, User> byId = new HashMap<>();

        @Override
        public Optional<User> findBySubject(String subject) {
            return Optional.ofNullable(bySubject.get(subject));
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public User upsertFromToken(VerifiedToken token) {
            User existing = bySubject.get(token.subject());
            if (existing != null) {
                return existing;
            }
            String name = token.email().substring(0, token.email().indexOf('@'));
            User created = new User(UUID.randomUUID(), token.subject(), token.email(),
                    name, Role.LEARNER, Instant.now());
            bySubject.put(created.subject(), created);
            byId.put(created.id(), created);
            return created;
        }

        @Override
        public User promoteToCreator(UUID id) {
            User u = byId.get(id);
            User promoted = new User(u.id(), u.subject(), u.email(), u.displayName(),
                    Role.CREATOR, u.createdAt());
            bySubject.put(promoted.subject(), promoted);
            byId.put(promoted.id(), promoted);
            return promoted;
        }
    }
}

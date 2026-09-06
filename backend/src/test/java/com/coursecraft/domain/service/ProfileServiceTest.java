package com.coursecraft.domain.service;

import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.User;
import com.coursecraft.domain.object.VerifiedToken;
import com.coursecraft.port.UserStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pure domain test — no Spring context, no database. Uses an in-memory fake of the
 * {@link UserStore} port to verify the {@link ProfileService} behaviour that drives journey CJ-1.
 */
class ProfileServiceTest {

    @Test
    void getOrProvision_provisionsLearnerOnFirstCall() {
        FakeUserStore store = new FakeUserStore();
        ProfileService service = new ProfileService(store);

        User user = service.getOrProvision(new VerifiedToken("sub-123", "ada@example.org"));

        assertEquals(Role.LEARNER, user.role());
        assertEquals("ada", user.displayName());
        assertEquals("ada@example.org", user.email());
    }

    @Test
    void becomeCreator_upgradesRole() {
        FakeUserStore store = new FakeUserStore();
        ProfileService service = new ProfileService(store);
        User learner = service.getOrProvision(new VerifiedToken("sub-123", "ada@example.org"));

        User creator = service.becomeCreator(learner.id());

        assertEquals(Role.CREATOR, creator.role());
        assertEquals(learner.id(), creator.id());
    }

    @Test
    void becomeCreator_unknownUser_throws() {
        ProfileService service = new ProfileService(new FakeUserStore());
        assertThrows(RuntimeException.class, () -> service.becomeCreator(UUID.randomUUID()));
    }

    /** Minimal in-memory UserStore honouring the upsert-by-subject contract. */
    private static final class FakeUserStore implements UserStore {
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
                User refreshed = new User(existing.id(), existing.subject(), token.email(),
                        existing.displayName(), existing.role(), existing.createdAt());
                save(refreshed);
                return refreshed;
            }
            String name = token.email().substring(0, token.email().indexOf('@'));
            User created = new User(UUID.randomUUID(), token.subject(), token.email(),
                    name, Role.LEARNER, Instant.now());
            save(created);
            return created;
        }

        @Override
        public User promoteToCreator(UUID id) {
            User u = byId.get(id);
            User promoted = new User(u.id(), u.subject(), u.email(), u.displayName(),
                    Role.CREATOR, u.createdAt());
            save(promoted);
            return promoted;
        }

        @Override
        public User updateDisplayName(UUID id, String displayName) {
            User u = byId.get(id);
            User renamed = new User(u.id(), u.subject(), u.email(), displayName, u.role(), u.createdAt());
            save(renamed);
            return renamed;
        }

        private void save(User u) {
            bySubject.put(u.subject(), u);
            byId.put(u.id(), u);
        }
    }
}

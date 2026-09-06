package com.coursecraft.adapter.out.client;

import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.User;
import com.coursecraft.domain.object.VerifiedToken;
import com.coursecraft.port.UserStore;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Optional;
import java.util.UUID;

/**
 * {@link UserStore} backed by the Supabase Postgres via {@link JdbcClient}. All SQL is visible
 * here — no repository proxies, no JPA. Schema lives in {@code schema.sql}.
 */
public final class JdbcUserStore implements UserStore {

    private final JdbcClient jdbc;

    public JdbcUserStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<User> findBySubject(String subject) {
        return jdbc.sql("""
                        SELECT id, subject, email, display_name, role, created_at
                        FROM app_user WHERE subject = :subject
                        """)
                .param("subject", subject)
                .query(JdbcUserStore::mapUser)
                .optional();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jdbc.sql("""
                        SELECT id, subject, email, display_name, role, created_at
                        FROM app_user WHERE id = :id
                        """)
                .param("id", id)
                .query(JdbcUserStore::mapUser)
                .optional();
    }

    @Override
    public User upsertFromToken(VerifiedToken token) {
        String defaultName = deriveDisplayName(token.email());
        jdbc.sql("""
                        INSERT INTO app_user (id, subject, email, display_name, role, created_at)
                        VALUES (gen_random_uuid(), :subject, :email, :displayName, 'LEARNER', now())
                        ON CONFLICT (subject)
                        DO UPDATE SET email = EXCLUDED.email
                        """)
                .param("subject", token.subject())
                .param("email", token.email())
                .param("displayName", defaultName)
                .update();
        return findBySubject(token.subject())
                .orElseThrow(() -> new IllegalStateException("upsert failed for " + token.subject()));
    }

    @Override
    public User promoteToCreator(UUID id) {
        jdbc.sql("UPDATE app_user SET role = 'CREATOR' WHERE id = :id")
                .param("id", id)
                .update();
        return findById(id)
                .orElseThrow(() -> new IllegalStateException("no user " + id));
    }

    private static String deriveDisplayName(String email) {
        if (email == null || email.isBlank()) {
            return "Learner";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private static User mapUser(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new User(
                rs.getObject("id", UUID.class),
                rs.getString("subject"),
                rs.getString("email"),
                rs.getString("display_name"),
                Role.valueOf(rs.getString("role")),
                rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant());
    }
}

package com.coursecraft.port;

import com.coursecraft.domain.object.User;
import com.coursecraft.domain.object.VerifiedToken;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting and reading user profiles. Implemented by an adapter over the
 * Supabase Postgres via {@code JdbcClient} — the domain never sees SQL.
 */
public interface UserStore {

    Optional<User> findBySubject(String subject);

    Optional<User> findById(UUID id);

    /**
     * Just-in-time provisioning: insert the profile on first sign-in, otherwise refresh the email.
     * Idempotent — safe to call on every authenticated request.
     */
    User upsertFromToken(VerifiedToken token);

    /** Upgrade a user to {@code CREATOR} (journey CJ-1). */
    User promoteToCreator(UUID id);
}

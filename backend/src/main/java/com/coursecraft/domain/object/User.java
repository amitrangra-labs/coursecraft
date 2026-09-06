package com.coursecraft.domain.object;

import java.time.Instant;
import java.util.UUID;

/**
 * A CourseCraft user profile — the app-owned record, distinct from the identity held by the
 * auth provider (Supabase Auth). {@code subject} is the provider's stable {@code sub} claim and
 * is the link between the two; we never store passwords.
 */
public record User(
        UUID id,
        String subject,
        String email,
        String displayName,
        Role role,
        Instant createdAt) {
}

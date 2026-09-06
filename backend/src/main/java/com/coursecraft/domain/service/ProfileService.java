package com.coursecraft.domain.service;

import com.coursecraft.domain.object.User;
import com.coursecraft.domain.object.VerifiedToken;
import com.coursecraft.port.UserStore;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Domain service for the current user's profile. Framework-free: no Spring/HTTP/SQL imports —
 * it depends only on the {@link UserStore} outbound port, which is injected by hand in
 * {@code DomainConfig}.
 */
public final class ProfileService {

    private final UserStore userStore;

    public ProfileService(UserStore userStore) {
        this.userStore = userStore;
    }

    /**
     * Return the caller's profile, provisioning it on first sign-in (journey CJ-1 / sign-in).
     */
    public User getOrProvision(VerifiedToken token) {
        return userStore.upsertFromToken(token);
    }

    /** Upgrade the caller to a creator so they can author courses (journey CJ-1). */
    public User becomeCreator(UUID userId) {
        User user = userStore.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("No user " + userId));
        return userStore.promoteToCreator(user.id());
    }
}

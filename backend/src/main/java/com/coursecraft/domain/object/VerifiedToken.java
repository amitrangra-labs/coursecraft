package com.coursecraft.domain.object;

/**
 * The trusted claims extracted from a verified Supabase JWT. Produced by the
 * {@link com.coursecraft.port.TokenVerifier} outbound port; the domain treats it as the identity
 * of the caller for a request.
 */
public record VerifiedToken(String subject, String email) {
}

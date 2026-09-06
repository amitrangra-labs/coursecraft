package com.coursecraft.adapter.out.client;

import com.coursecraft.domain.object.VerifiedToken;
import com.coursecraft.port.TokenVerifier;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Verifies Supabase-issued JWTs against the project's JWKS (asymmetric signing keys). Validates
 * signature, issuer, and expiry, then extracts {@code sub} and {@code email}. No Spring Security —
 * just Nimbus, wired by hand in {@code OutboundConfig}.
 */
public final class NimbusTokenVerifier implements TokenVerifier {

    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public NimbusTokenVerifier(String jwksUri, String issuer, List<JWSAlgorithm> algorithms) {
        this.processor = buildProcessor(jwksUri, issuer, algorithms);
    }

    private static ConfigurableJWTProcessor<SecurityContext> buildProcessor(
            String jwksUri, String issuer, List<JWSAlgorithm> algorithms) {
        try {
            JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(URI.create(jwksUri).toURL());
            DefaultJWTProcessor<SecurityContext> p = new DefaultJWTProcessor<>();
            p.setJWSKeySelector(new JWSVerificationKeySelector<>(Set.copyOf(algorithms), jwkSource));
            p.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                    new JWTClaimsSet.Builder().issuer(issuer).build(),
                    Set.of("sub", "exp")));
            return p;
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Bad JWKS URI: " + jwksUri, e);
        }
    }

    @Override
    public VerifiedToken verify(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new InvalidTokenException("Missing bearer token");
        }
        try {
            JWTClaimsSet claims = processor.process(bearerToken, null);
            String subject = claims.getSubject();
            String email = claims.getStringClaim("email");
            return new VerifiedToken(subject, email);
        } catch (Exception e) {
            throw new InvalidTokenException("Token verification failed", e);
        }
    }
}

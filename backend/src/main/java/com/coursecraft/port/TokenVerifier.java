package com.coursecraft.port;

import com.coursecraft.domain.object.VerifiedToken;

/**
 * Outbound port that verifies a bearer JWT and returns its trusted claims. Implemented by an
 * adapter that validates the signature against the auth provider's JWKS plus issuer/expiry.
 */
public interface TokenVerifier {

    /**
     * @param bearerToken the raw JWT (without the {@code "Bearer "} prefix)
     * @return the verified claims
     * @throws InvalidTokenException if the token is missing, malformed, expired, or not trusted
     */
    VerifiedToken verify(String bearerToken);

    /** Thrown when a token cannot be trusted; the inbound layer maps this to HTTP 401. */
    class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidTokenException(String message) {
            super(message);
        }
    }
}

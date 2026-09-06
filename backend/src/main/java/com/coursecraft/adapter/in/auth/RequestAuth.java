package com.coursecraft.adapter.in.auth;

import com.coursecraft.domain.object.VerifiedToken;
import com.coursecraft.port.TokenVerifier;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

/**
 * Bearer-token authentication as a functional filter (no Spring Security). Verifies the JWT via
 * the {@link TokenVerifier} port and stashes the {@link VerifiedToken} as a request attribute for
 * downstream handlers; rejects with 401 when absent or invalid.
 */
public final class RequestAuth {

    /** Request attribute key under which the verified token is stored. */
    public static final String TOKEN_ATTRIBUTE = "coursecraft.verifiedToken";

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenVerifier tokenVerifier;

    public RequestAuth(TokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    /** A filter that requires a valid bearer token on every wrapped route. */
    public HandlerFilterFunction<ServerResponse, ServerResponse> required() {
        return (request, next) -> {
            String header = request.headers().firstHeader("Authorization");
            if (header == null || !header.startsWith(BEARER_PREFIX)) {
                return unauthorized("Missing or malformed Authorization header");
            }
            try {
                VerifiedToken token = tokenVerifier.verify(header.substring(BEARER_PREFIX.length()));
                request.attributes().put(TOKEN_ATTRIBUTE, token);
                return next.handle(request);
            } catch (TokenVerifier.InvalidTokenException e) {
                return unauthorized("Invalid token");
            }
        };
    }

    /** Read the verified token a wrapped handler can rely on being present. */
    public static VerifiedToken tokenOf(ServerRequest request) {
        Object token = request.attributes().get(TOKEN_ATTRIBUTE);
        if (!(token instanceof VerifiedToken verified)) {
            throw new IllegalStateException("No verified token on request — route not behind auth filter");
        }
        return verified;
    }

    private static ServerResponse unauthorized(String message) {
        return ServerResponse.status(401).body(Map.of("error", "unauthorized", "message", message));
    }
}

package com.coursecraft.adapter.in.endpoint;

import com.coursecraft.adapter.in.auth.RequestAuth;
import com.coursecraft.domain.object.User;
import com.coursecraft.domain.object.VerifiedToken;
import com.coursecraft.domain.service.ProfileService;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Authenticated profile endpoints. Handlers for {@code GET /api/me} (provision-on-first-call)
 * and {@code POST /api/creators} (upgrade to creator, journey CJ-1). These run behind the bearer
 * auth filter, so a verified token is always present.
 */
public final class MeHandler {

    private final ProfileService profileService;

    public MeHandler(ProfileService profileService) {
        this.profileService = profileService;
    }

    public ServerResponse me(ServerRequest request) {
        VerifiedToken token = RequestAuth.tokenOf(request);
        User user = profileService.getOrProvision(token);
        return ServerResponse.ok().body(ProfileResponse.from(user));
    }

    public ServerResponse becomeCreator(ServerRequest request) {
        VerifiedToken token = RequestAuth.tokenOf(request);
        User user = profileService.getOrProvision(token);
        User creator = profileService.becomeCreator(user.id());
        return ServerResponse.ok().body(ProfileResponse.from(creator));
    }

    /** Outbound view — never leaks the internal subject beyond what the client needs. */
    public record ProfileResponse(String id, String email, String displayName, String role) {
        static ProfileResponse from(User user) {
            return new ProfileResponse(
                    user.id().toString(),
                    user.email(),
                    user.displayName(),
                    user.role().name());
        }
    }
}

package com.coursecraft.adapter.in.endpoint;

import com.coursecraft.adapter.in.auth.RequestAuth;
import com.coursecraft.domain.object.LiveSession;
import com.coursecraft.domain.object.User;
import com.coursecraft.domain.service.LiveSessionService;
import com.coursecraft.domain.service.ProfileService;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;
import java.util.UUID;

/** Live lecture endpoints (journeys CJ-7, LJ-7). */
public final class LiveHandler {

    private final ProfileService profileService;
    private final LiveSessionService liveService;

    public LiveHandler(ProfileService profileService, LiveSessionService liveService) {
        this.profileService = profileService;
        this.liveService = liveService;
    }

    public ServerResponse schedule(ServerRequest request) throws Exception {
        User user = currentUser(request);
        ScheduleRequest body = request.body(ScheduleRequest.class);
        Instant startsAt = body.startsAtEpochMs() == null ? Instant.now()
                : Instant.ofEpochMilli(body.startsAtEpochMs());
        LiveSession s = liveService.schedule(user, uuid(request, "courseId"),
                body.title(), body.youtubeVideoId(), startsAt);
        return ServerResponse.ok().body(LiveView.from(s));
    }

    public ServerResponse goLive(ServerRequest request) {
        return ServerResponse.ok().body(LiveView.from(
                liveService.goLive(currentUser(request), uuid(request, "sessionId"))));
    }

    public ServerResponse end(ServerRequest request) {
        return ServerResponse.ok().body(LiveView.from(
                liveService.end(currentUser(request), uuid(request, "sessionId"))));
    }

    public ServerResponse cancel(ServerRequest request) {
        return ServerResponse.ok().body(LiveView.from(
                liveService.cancel(currentUser(request), uuid(request, "sessionId"))));
    }

    public ServerResponse listForCourse(ServerRequest request) {
        User user = currentUser(request);
        return ServerResponse.ok().body(liveService.listForCourse(user, uuid(request, "courseId"))
                .stream().map(LiveView::from).toList());
    }

    private User currentUser(ServerRequest request) {
        return profileService.getOrProvision(RequestAuth.tokenOf(request));
    }

    private static UUID uuid(ServerRequest request, String name) {
        return UUID.fromString(request.pathVariable(name));
    }

    public record ScheduleRequest(String title, String youtubeVideoId, Long startsAtEpochMs) {
    }

    public record LiveView(String id, String title, String videoId, String status, long startsAtEpochMs) {
        static LiveView from(LiveSession s) {
            return new LiveView(s.id().toString(), s.title(), s.youtubeVideoId(),
                    s.status().name(), s.startsAt().toEpochMilli());
        }
    }
}

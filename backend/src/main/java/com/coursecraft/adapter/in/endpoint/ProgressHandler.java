package com.coursecraft.adapter.in.endpoint;

import com.coursecraft.adapter.in.auth.RequestAuth;
import com.coursecraft.domain.object.ContinueItem;
import com.coursecraft.domain.object.Progress;
import com.coursecraft.domain.object.User;
import com.coursecraft.domain.service.ProfileService;
import com.coursecraft.domain.service.ProgressService;
import com.coursecraft.domain.service.ProgressService.CourseProgress;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Playback progress endpoints (journeys LJ-2/LJ-3). */
public final class ProgressHandler {

    private final ProfileService profileService;
    private final ProgressService progressService;

    public ProgressHandler(ProfileService profileService, ProgressService progressService) {
        this.profileService = profileService;
        this.progressService = progressService;
    }

    public ServerResponse save(ServerRequest request) throws Exception {
        User user = currentUser(request);
        SaveProgressRequest body = request.body(SaveProgressRequest.class);
        progressService.save(user, UUID.fromString(body.lectureId()),
                body.positionSec() == null ? 0 : body.positionSec(),
                Boolean.TRUE.equals(body.completed()));
        return ServerResponse.ok().body(Map.of("status", "ok"));
    }

    public ServerResponse get(ServerRequest request) {
        User user = currentUser(request);
        Progress p = progressService.get(user, uuid(request, "lectureId"));
        return ServerResponse.ok().body(Map.of(
                "positionSec", p.positionSec(),
                "completed", p.completed()));
    }

    public ServerResponse courseProgress(ServerRequest request) {
        User user = currentUser(request);
        CourseProgress cp = progressService.courseProgress(user, uuid(request, "courseId"));
        return ServerResponse.ok().body(Map.of(
                "percent", cp.percent(),
                "completed", cp.completed(),
                "total", cp.total()));
    }

    public ServerResponse continueLearning(ServerRequest request) {
        User user = currentUser(request);
        Optional<ContinueItem> item = progressService.continueLearning(user);
        if (item.isEmpty()) {
            return ServerResponse.ok().body(Map.of("none", true));
        }
        ContinueItem c = item.get();
        return ServerResponse.ok().body(Map.of(
                "courseId", c.courseId().toString(),
                "courseTitle", c.courseTitle(),
                "lectureId", c.lectureId().toString(),
                "lectureTitle", c.lectureTitle(),
                "videoId", c.videoId() == null ? "" : c.videoId(),
                "positionSec", c.positionSec()));
    }

    private User currentUser(ServerRequest request) {
        return profileService.getOrProvision(RequestAuth.tokenOf(request));
    }

    private static UUID uuid(ServerRequest request, String name) {
        return UUID.fromString(request.pathVariable(name));
    }

    public record SaveProgressRequest(String lectureId, Integer positionSec, Boolean completed) {
    }
}

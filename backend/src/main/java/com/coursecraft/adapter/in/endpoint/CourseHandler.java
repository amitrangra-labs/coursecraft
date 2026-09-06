package com.coursecraft.adapter.in.endpoint;

import com.coursecraft.adapter.in.auth.RequestAuth;
import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.CourseDetail;
import com.coursecraft.domain.object.Lecture;
import com.coursecraft.domain.object.Section;
import com.coursecraft.domain.object.User;
import com.coursecraft.domain.service.CourseService;
import com.coursecraft.domain.service.ProfileService;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.UUID;

/**
 * Course authoring + catalog endpoints (journeys CJ-2/CJ-5, LJ-1). Runs behind the auth filter,
 * so the caller is resolved to a {@link User} via {@link ProfileService} on every request.
 */
public final class CourseHandler {

    private final ProfileService profileService;
    private final CourseService courseService;

    public CourseHandler(ProfileService profileService, CourseService courseService) {
        this.profileService = profileService;
        this.courseService = courseService;
    }

    // --- creator ---

    public ServerResponse createCourse(ServerRequest request) throws Exception {
        User user = currentUser(request);
        CreateCourseRequest body = request.body(CreateCourseRequest.class);
        Course course = courseService.createCourse(user, body.title(), body.subject(), body.level());
        return ServerResponse.ok().body(CourseSummary.from(course));
    }

    public ServerResponse addSection(ServerRequest request) throws Exception {
        User user = currentUser(request);
        UUID courseId = uuid(request, "courseId");
        TitleRequest body = request.body(TitleRequest.class);
        Section section = courseService.addSection(user, courseId, body.title());
        return ServerResponse.ok().body(SectionView.from(section, List.of()));
    }

    public ServerResponse addLecture(ServerRequest request) throws Exception {
        User user = currentUser(request);
        UUID courseId = uuid(request, "courseId");
        UUID sectionId = uuid(request, "sectionId");
        CreateLectureRequest body = request.body(CreateLectureRequest.class);
        Lecture lecture = courseService.addVideoLecture(
                user, courseId, sectionId, body.title(), body.videoProvider(), body.videoId());
        return ServerResponse.ok().body(LectureView.from(lecture));
    }

    public ServerResponse publish(ServerRequest request) {
        User user = currentUser(request);
        UUID courseId = uuid(request, "courseId");
        Course course = courseService.publish(user, courseId);
        return ServerResponse.ok().body(CourseSummary.from(course));
    }

    // --- creator editing / reordering ---

    public ServerResponse renameCourse(ServerRequest request) throws Exception {
        User user = currentUser(request);
        UUID courseId = uuid(request, "courseId");
        CreateCourseRequest body = request.body(CreateCourseRequest.class);
        Course course = courseService.renameCourse(user, courseId, body.title(), body.subject(), body.level());
        return ServerResponse.ok().body(CourseSummary.from(course));
    }

    public ServerResponse deleteCourse(ServerRequest request) {
        courseService.deleteCourse(currentUser(request), uuid(request, "courseId"));
        return ok();
    }

    public ServerResponse renameSection(ServerRequest request) throws Exception {
        User user = currentUser(request);
        TitleRequest body = request.body(TitleRequest.class);
        Section section = courseService.renameSection(user, uuid(request, "courseId"),
                uuid(request, "sectionId"), body.title());
        return ServerResponse.ok().body(SectionView.from(section, List.of()));
    }

    public ServerResponse deleteSection(ServerRequest request) {
        courseService.deleteSection(currentUser(request), uuid(request, "courseId"),
                uuid(request, "sectionId"));
        return ok();
    }

    public ServerResponse reorderSections(ServerRequest request) throws Exception {
        User user = currentUser(request);
        IdListRequest body = request.body(IdListRequest.class);
        courseService.reorderSections(user, uuid(request, "courseId"), toUuids(body));
        return ok();
    }

    public ServerResponse updateLecture(ServerRequest request) throws Exception {
        User user = currentUser(request);
        UpdateLectureRequest body = request.body(UpdateLectureRequest.class);
        Lecture lecture = courseService.updateLecture(user, uuid(request, "courseId"),
                uuid(request, "sectionId"), uuid(request, "lectureId"),
                body.title(), body.videoProvider(), body.videoId());
        return ServerResponse.ok().body(LectureView.from(lecture));
    }

    public ServerResponse deleteLecture(ServerRequest request) {
        courseService.deleteLecture(currentUser(request), uuid(request, "courseId"),
                uuid(request, "sectionId"), uuid(request, "lectureId"));
        return ok();
    }

    public ServerResponse reorderLectures(ServerRequest request) throws Exception {
        User user = currentUser(request);
        IdListRequest body = request.body(IdListRequest.class);
        courseService.reorderLectures(user, uuid(request, "courseId"), uuid(request, "sectionId"),
                toUuids(body));
        return ok();
    }

    public ServerResponse myCourses(ServerRequest request) {
        User user = currentUser(request);
        return ServerResponse.ok().body(courseService.listMyCourses(user).stream()
                .map(CourseSummary::from).toList());
    }

    // --- learner ---

    public ServerResponse catalog(ServerRequest request) {
        currentUser(request);
        return ServerResponse.ok().body(courseService.catalog().stream()
                .map(CourseSummary::from).toList());
    }

    public ServerResponse courseDetail(ServerRequest request) {
        User user = currentUser(request);
        UUID courseId = uuid(request, "courseId");
        return ServerResponse.ok().body(CourseDetailView.from(courseService.getDetail(user, courseId)));
    }

    // --- helpers ---

    private User currentUser(ServerRequest request) {
        return profileService.getOrProvision(RequestAuth.tokenOf(request));
    }

    private static UUID uuid(ServerRequest request, String name) {
        return UUID.fromString(request.pathVariable(name));
    }

    private static ServerResponse ok() {
        return ServerResponse.ok().body(java.util.Map.of("status", "ok"));
    }

    private static List<UUID> toUuids(IdListRequest body) {
        if (body == null || body.ids() == null) {
            return List.of();
        }
        return body.ids().stream().map(UUID::fromString).toList();
    }

    // --- request bodies ---

    public record CreateCourseRequest(String title, String subject, String level) {
    }

    public record TitleRequest(String title) {
    }

    public record CreateLectureRequest(String title, String videoProvider, String videoId) {
    }

    public record UpdateLectureRequest(String title, String videoProvider, String videoId) {
    }

    public record IdListRequest(List<String> ids) {
    }

    // --- responses ---

    public record CourseSummary(String id, String title, String subject, String level,
                                String status, String creatorId) {
        static CourseSummary from(Course c) {
            return new CourseSummary(c.id().toString(), c.title(), c.subject(), c.level(),
                    c.status().name(), c.creatorId().toString());
        }
    }

    public record LectureView(String id, String title, String videoProvider, String videoId,
                              int position) {
        static LectureView from(Lecture l) {
            return new LectureView(l.id().toString(), l.title(), l.videoProvider(), l.videoId(),
                    l.position());
        }
    }

    public record SectionView(String id, String title, int position, List<LectureView> lectures) {
        static SectionView from(Section s, List<Lecture> lectures) {
            return new SectionView(s.id().toString(), s.title(), s.position(),
                    lectures.stream().map(LectureView::from).toList());
        }
    }

    public record CourseDetailView(CourseSummary course, List<SectionView> sections) {
        static CourseDetailView from(CourseDetail detail) {
            List<SectionView> sections = detail.sections().stream()
                    .map(sw -> SectionView.from(sw.section(), sw.lectures()))
                    .toList();
            return new CourseDetailView(CourseSummary.from(detail.course()), sections);
        }
    }
}

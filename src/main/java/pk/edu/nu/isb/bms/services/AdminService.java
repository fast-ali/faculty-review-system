package pk.edu.nu.isb.bms.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.edu.nu.isb.bms.models.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final CourseRequestRepository courseRequestRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository,
                        FacultyRepository facultyRepository,
                        ReviewRepository reviewRepository,
                        CourseRepository courseRepository,
                        CourseRequestRepository courseRequestRepository,
                        DepartmentRepository departmentRepository,
                        AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.facultyRepository = facultyRepository;
        this.reviewRepository = reviewRepository;
        this.courseRepository = courseRepository;
        this.courseRequestRepository = courseRequestRepository;
        this.departmentRepository = departmentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<MyUser> listUsers() { return userRepository.findAll(); }

    public Optional<MyUser> findUser(Long id) { return userRepository.findById(id); }

    @Transactional
    public void setUserRole(Long userId, String role, Long adminUserId) {
        var actor = userRepository.findById(adminUserId).orElseThrow();
        var target = userRepository.findById(userId).orElseThrow();

        if (target.getId().equals(actor.getId()) && !"ROLE_ADMIN".equalsIgnoreCase(role)) {
            logAction(adminUserId, "SET_ROLE_BLOCKED", "Blocked self-demotion attempt for user " + userId + " to " + role);
            throw new IllegalArgumentException("You cannot demote your own admin role.");
        }

        target.setRole(role);
        userRepository.save(target);
        logAction(adminUserId, "SET_ROLE", "Set role of user " + userId + " to " + role);
    }

    @Transactional
    public void setUserEnabled(Long userId, boolean enabled, Long adminUserId) {
        var actor = userRepository.findById(adminUserId).orElseThrow();
        var target = userRepository.findById(userId).orElseThrow();

        if (target.getId().equals(actor.getId())) {
            logAction(adminUserId, "SUSPEND_USER_BLOCKED", "Blocked self-enable/disable for user " + userId);
            throw new IllegalArgumentException("You cannot change your own enabled status.");
        }

        if ("ROLE_ADMIN".equalsIgnoreCase(target.getRole())) {
            logAction(adminUserId, "SUSPEND_USER_BLOCKED", "Blocked enable/disable of admin user " + userId);
            throw new IllegalArgumentException("Admin accounts cannot be enabled/disabled from admin panel.");
        }

        target.setEnabled(enabled);
        userRepository.save(target);
        logAction(adminUserId, enabled ? "UNSUSPEND_USER" : "SUSPEND_USER", (enabled ? "Enabled" : "Disabled") + " user " + userId);
    }

    @Transactional
    public void deleteUser(Long userId, Long adminUserId) {
        var actor = userRepository.findById(adminUserId).orElseThrow();
        var target = userRepository.findById(userId).orElseThrow();

        if (target.getId().equals(actor.getId())) {
            logAction(adminUserId, "DELETE_USER_BLOCKED", "Blocked self-delete for user " + userId);
            throw new IllegalArgumentException("You cannot delete your own account.");
        }

        if ("ROLE_ADMIN".equalsIgnoreCase(target.getRole())) {
            logAction(adminUserId, "DELETE_USER_BLOCKED", "Blocked delete of admin user " + userId);
            throw new IllegalArgumentException("Admin accounts cannot be deleted from admin panel.");
        }

        userRepository.deleteById(userId);
        logAction(adminUserId, "DELETE_USER", "Deleted user " + userId);
    }

    public List<Review> listReportedReviews() {
        return reviewRepository.findAll().stream().filter(Review::isReported).toList();
    }

    public List<CourseRequest> listCourseRequests() {
        return courseRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ReviewRow> listAllReviewRows() {
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> new ReviewRow(
                        r.getId(),
                        "Anonymous",
                        r.getFaculty() != null ? r.getFaculty().getName() : "Unknown",
                        r.getCourse() != null ? (r.getCourse().getCode() + " - " + r.getCourse().getTitle()) : "N/A",
                        r.getCreatedAt(),
                        r.getRating()
                ))
                .toList();
    }

    @Transactional
    public void deleteReview(Long reviewId, Long adminUserId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow();
        String facultyName = review.getFaculty() != null ? review.getFaculty().getName() : "Unknown";
        String courseLabel = review.getCourse() != null ? review.getCourse().getCode() + "-" + review.getCourse().getTitle() : "N/A";
        String details = "Admin " + adminUserId + " deleted review " + reviewId +
                " (faculty=" + facultyName + ", course=" + courseLabel + ", rating=" + review.getRating() + ")";

        reviewRepository.delete(review);
        logAction(adminUserId, "DELETE_REVIEW", details);
    }

    public List<FacultyEntity> listFaculties() { return facultyRepository.findAll(); }

    @Transactional
    public void approveCourseRequest(Long requestId, Long adminUserId) {
        var actor = userRepository.findById(adminUserId).orElseThrow();
        CourseRequest request = courseRequestRepository.findByIdAndStatus(requestId, "PENDING").orElseThrow();

        String title = request.getRequestedCourseTitle().trim();
        String code = resolveCourseCode(request);

        Course course = courseRepository.findByCodeIgnoreCase(code).orElseGet(() -> {
            Course created = new Course();
            created.setCode(code);
            created.setTitle(title);
            return courseRepository.save(created);
        });

        courseRepository.linkCourseToFaculty(request.getFaculty().getId(), course.getId());

        request.setStatus("APPROVED");
        request.setProcessedByUserId(actor.getId());
        request.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
        request.setRejectionReason(null);
        courseRequestRepository.save(request);

        logAction(adminUserId, "APPROVE_COURSE_REQUEST", "Approved course request " + requestId + " -> " + code + " for faculty " + request.getFaculty().getId());
    }

    @Transactional
    public void rejectCourseRequest(Long requestId, String reason, Long adminUserId) {
        var actor = userRepository.findById(adminUserId).orElseThrow();
        CourseRequest request = courseRequestRepository.findByIdAndStatus(requestId, "PENDING").orElseThrow();
        String cleanedReason = reason == null ? "" : reason.trim();
        if (cleanedReason.isEmpty()) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }

        request.setStatus("REJECTED");
        request.setProcessedByUserId(actor.getId());
        request.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
        request.setRejectionReason(cleanedReason);
        courseRequestRepository.save(request);

        logAction(adminUserId, "REJECT_COURSE_REQUEST", "Rejected course request " + requestId + " (reason: " + cleanedReason + ")");
    }

    @Transactional
    public FacultyEntity addFaculty(FacultyEntity f, Long adminUserId) {
        FacultyEntity saved = facultyRepository.save(f);
        var log = new AuditLog();
        log.setActorUserId(adminUserId);
        log.setAction("ADD_FACULTY");
        log.setDetails("Added faculty " + saved.getId());
        auditLogRepository.save(log);
        return saved;
    }

    public List<Department> listDepartments() { return departmentRepository.findAll(); }

    @Transactional
    public Department addDepartment(String name, Long adminUserId) {
        Department existing = departmentRepository.findByNameIgnoreCase(name);
        if (existing != null) return existing;
        Department d = new Department();
        d.setName(name);
        Department saved = departmentRepository.save(d);
        var log = new AuditLog();
        log.setActorUserId(adminUserId);
        log.setAction("ADD_DEPARTMENT");
        log.setDetails("Added department " + name);
        auditLogRepository.save(log);
        return saved;
    }

    public List<AuditLog> listAuditLogs() {
        return auditLogRepository.findAll().stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public UserGrowthData getUserGrowthData(String interval) {
        String resolved = interval == null ? "30d" : interval;
        int days = switch (resolved) {
            case "7d" -> 7;
            case "year" -> 365;
            default -> 30;
        };

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(days - 1L);
        List<MyUser> users = userRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(from);

        Map<LocalDate, Integer> perDay = new LinkedHashMap<>();
        LocalDate startDay = from.toLocalDate();
        LocalDate today = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate();
        for (LocalDate d = startDay; !d.isAfter(today); d = d.plusDays(1)) {
            perDay.put(d, 0);
        }
        for (MyUser u : users) {
            if (u.getCreatedAt() == null) continue;
            LocalDate day = u.getCreatedAt().toLocalDate();
            perDay.computeIfPresent(day, (k, v) -> v + 1);
        }

        DateTimeFormatter fmt = days > 31
                ? DateTimeFormatter.ofPattern("MMM yy")
                : DateTimeFormatter.ofPattern("dd MMM");

        List<String> labels = new ArrayList<>();
        List<Integer> cumulative = new ArrayList<>();
        int running = 0;
        for (Map.Entry<LocalDate, Integer> e : perDay.entrySet()) {
            running += e.getValue();
            labels.add(e.getKey().format(fmt));
            cumulative.add(running);
        }

        return new UserGrowthData(labels, cumulative);
    }

    private void logAction(Long actorUserId, String action, String details) {
        var log = new AuditLog();
        log.setActorUserId(actorUserId);
        log.setAction(action);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    public record ReviewRow(Long id, String reviewer, String teacherName, String course, OffsetDateTime submittedAt, int averageRating) {}

    public record UserGrowthData(List<String> labels, List<Integer> values) {}

    private String resolveCourseCode(CourseRequest request) {
        String requestedCode = request.getRequestedCourseCode() == null ? "" : request.getRequestedCourseCode().trim();
        if (!requestedCode.isEmpty()) {
            return requestedCode.length() > 50 ? requestedCode.substring(0, 50) : requestedCode;
        }

        String title = request.getRequestedCourseTitle() == null ? "COURSE" : request.getRequestedCourseTitle().trim();
        String slug = title.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.isBlank()) {
            slug = "COURSE";
        }

        String code = "REQ-" + slug;
        if (request.getId() != null) {
            code = code + "-" + request.getId();
        }
        return code.length() > 50 ? code.substring(0, 50) : code;
    }
}

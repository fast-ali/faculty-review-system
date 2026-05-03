package pk.edu.nu.isb.bms.controllers;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pk.edu.nu.isb.bms.models.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ContentController {

    private final MyUserService userService;
    private final FacultyService facultyService;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseRequestRepository courseRequestRepository;

    public ContentController(MyUserService userService,
                             FacultyService facultyService,
                             ReviewRepository reviewRepository,
                             UserRepository userRepository,
                             CourseRepository courseRepository,
                             CourseRequestRepository courseRequestRepository) {
        this.userService = userService;
        this.facultyService = facultyService;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.courseRequestRepository = courseRequestRepository;
    }

    @GetMapping("/")
    public String home(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "dept", required = false, defaultValue = "All") String dept,
                       Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
        if (!authenticated) {
            return "redirect:/login";
        }

        List<Faculty> facultyList = facultyService.search(q, dept);
        Map<Long, Long> reviewCountByFacultyId = new HashMap<>();
        Map<Long, Double> averageRatingByFacultyId = new HashMap<>();

        for (Review review : reviewRepository.findAll()) {
            if (review.getFaculty() == null || review.getFaculty().getId() == null) {
                continue;
            }
            Long facultyId = review.getFaculty().getId();
            reviewCountByFacultyId.put(facultyId, reviewCountByFacultyId.getOrDefault(facultyId, 0L) + 1L);
            averageRatingByFacultyId.put(facultyId, averageRatingByFacultyId.getOrDefault(facultyId, 0.0) + review.getRating());
        }

        for (Faculty faculty : facultyList) {
            if (faculty.getId() == null) {
                continue;
            }
            Long count = reviewCountByFacultyId.getOrDefault(faculty.getId(), 0L);
            if (count > 0) {
                double sum = averageRatingByFacultyId.getOrDefault(faculty.getId(), 0.0);
                double avg = Math.round((sum / count) * 10.0) / 10.0;
                averageRatingByFacultyId.put(faculty.getId(), avg);
            }
        }

        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("dept", dept == null ? "All" : dept);
        model.addAttribute("faculty", facultyList);
        model.addAttribute("reviewCountByFacultyId", reviewCountByFacultyId);
        model.addAttribute("averageRatingByFacultyId", averageRatingByFacultyId);

        userRepository.findByUsername(auth.getName()).ifPresentOrElse(
                u -> model.addAttribute("currentUser", u),
                () -> model.addAttribute("currentUser", null)
        );

        return "home";
    }

    @GetMapping("/faculty/{id}")
    public String facultyDetail(@PathVariable Long id,
                                @RequestParam(value = "reviewSubmitted", required = false) String reviewSubmitted,
                                @RequestParam(value = "reviewReported", required = false) String reviewReported,
                                @RequestParam(value = "reviewCourseError", required = false) String reviewCourseError,
                                @RequestParam(value = "courseRequestSubmitted", required = false) String courseRequestSubmitted,
                                @RequestParam(value = "courseRequestError", required = false) String courseRequestError,
                                Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
        if (!authenticated) {
            return "redirect:/login";
        }

        var maybe = facultyService.findById(id);
        if (maybe.isEmpty()) {
            model.addAttribute("missingId", id);
            return "faculty-not-found";
        }
        Faculty f = maybe.get();
        List<Review> reviews = reviewRepository.findByFaculty_Id(id);

        model.addAttribute("faculty", f);
        model.addAttribute("reviews", reviews);
        model.addAttribute("courses", findCoursesForFaculty(id));
        model.addAttribute("reviewSubmitted", reviewSubmitted != null);
        model.addAttribute("reviewReported", reviewReported != null);
        model.addAttribute("reviewCourseError", reviewCourseError != null);
        model.addAttribute("courseRequestSubmitted", courseRequestSubmitted != null);
        model.addAttribute("courseRequestError", courseRequestError != null);

        model.addAttribute("criteriaLabels", List.of(
                "Subject Matter Knowledge",
                "Teaching Methods",
                "Student Engagement",
                "Collaboration & Teamwork",
                "Behavior Management",
                "Classroom Environment",
                "Professional Ethics",
                "Communication Skills"
        ));

        model.addAttribute("criteriaAverages", List.of(
                averageScore(reviews, "smk"),
                averageScore(reviews, "tm"),
                averageScore(reviews, "se"),
                averageScore(reviews, "ct"),
                averageScore(reviews, "bm"),
                averageScore(reviews, "ce"),
                averageScore(reviews, "pe"),
                averageScore(reviews, "cs")
        ));

        userRepository.findByUsername(auth.getName()).ifPresentOrElse(
                u -> model.addAttribute("currentUser", u),
                () -> model.addAttribute("currentUser", null)
        );
        return "faculty";
    }

    @PostMapping("/faculty/{id}/reviews")
    public String addReview(@PathVariable Long id,
                        @RequestParam(required = false) Integer rating,
                        @RequestParam String comment,
                        @RequestParam(required = false) Long courseId,
                        @RequestParam(required = false) Integer subjectMatterKnowledge,
                        @RequestParam(required = false) Integer teachingMethods,
                        @RequestParam(required = false) Integer studentEngagement,
                        @RequestParam(required = false) Integer collaborationTeamwork,
                        @RequestParam(required = false) Integer behaviorManagement,
                        @RequestParam(required = false) Integer classroomEnvironment,
                        @RequestParam(required = false) Integer professionalEthics,
                        @RequestParam(required = false) Integer communicationSkills) {
        if (!isCourseSelectableForFaculty(courseId, id)) {
            return "redirect:/faculty/" + id + "?reviewCourseError=1";
        }

        int baseRating = normalizeRating(rating == null ? 5 : rating);

        short smk = (short) normalizeRating(subjectMatterKnowledge == null ? baseRating : subjectMatterKnowledge);
        short tm = (short) normalizeRating(teachingMethods == null ? baseRating : teachingMethods);
        short se = (short) normalizeRating(studentEngagement == null ? baseRating : studentEngagement);
        short ct = (short) normalizeRating(collaborationTeamwork == null ? baseRating : collaborationTeamwork);
        short bm = (short) normalizeRating(behaviorManagement == null ? baseRating : behaviorManagement);
        short ce = (short) normalizeRating(classroomEnvironment == null ? baseRating : classroomEnvironment);
        short pe = (short) normalizeRating(professionalEthics == null ? baseRating : professionalEthics);
        short cs = (short) normalizeRating(communicationSkills == null ? baseRating : communicationSkills);

        Review r = new Review();
        FacultyEntity facultyRef = new FacultyEntity();
        facultyRef.setId(id);
        r.setFaculty(facultyRef);

        if (courseId != null) {
            courseRepository.findById(courseId).ifPresent(r::setCourse);
        }

        r.setSubjectMatterKnowledge(smk);
        r.setTeachingMethods(tm);
        r.setStudentEngagement(se);
        r.setCollaborationTeamwork(ct);
        r.setBehaviorManagement(bm);
        r.setClassroomEnvironment(ce);
        r.setProfessionalEthics(pe);
        r.setCommunicationSkills(cs);

        int avg = Math.round((smk + tm + se + ct + bm + ce + pe + cs) / 8.0f);
        r.setRating(normalizeRating(avg));
        r.setComment(comment == null ? "" : comment.trim());

        // Enforce anonymity: always store 'Anonymous'
        r.setStudentName("Anonymous");
        reviewRepository.save(r);
        return "redirect:/faculty/" + id + "?reviewSubmitted=1";
    }

    @PostMapping("/faculty/{id}/course-requests")
    public String requestCourseAddition(@PathVariable Long id,
                                        @RequestParam String requestedCourseTitle,
                                        @RequestParam(required = false) String requestedCourseCode) {
        String title = requestedCourseTitle == null ? "" : requestedCourseTitle.trim();
        String code = requestedCourseCode == null ? "" : requestedCourseCode.trim();

        if (title.isEmpty()) {
            return "redirect:/faculty/" + id + "?courseRequestError=1";
        }

        if (courseRequestRepository.existsByFaculty_IdAndRequestedCourseTitleIgnoreCaseAndStatus(id, title, "PENDING")) {
            return "redirect:/faculty/" + id + "?courseRequestError=1";
        }

        CourseRequest request = new CourseRequest();
        FacultyEntity facultyRef = new FacultyEntity();
        facultyRef.setId(id);
        request.setFaculty(facultyRef);
        request.setRequestedCourseTitle(title);
        request.setRequestedCourseCode(code.isEmpty() ? null : code);
        request.setStatus("PENDING");
        courseRequestRepository.save(request);
        return "redirect:/faculty/" + id + "?courseRequestSubmitted=1";
    }

    private int normalizeRating(int rating) {
        if (rating < 1) return 1;
        return Math.min(rating, 5);
    }

    private List<Course> findCoursesForFaculty(Long facultyId) {
        List<Course> assignedCourses = courseRepository.findByFacultyId(facultyId);
        if (!assignedCourses.isEmpty()) {
            return assignedCourses;
        }
        return courseRepository.findAllByOrderByCodeAsc();
    }

    private boolean isCourseSelectableForFaculty(Long courseId, Long facultyId) {
        if (courseId == null) {
            return false;
        }

        if (courseRepository.existsFacultyCourse(facultyId, courseId)) {
            return true;
        }

        // Allow global catalog selection only when no faculty-specific mappings exist yet.
        return !courseRepository.existsAnyFacultyCourse(facultyId) && courseRepository.existsById(courseId);
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "invalid", required = false) String invalid,
                        @RequestParam(value = "locked", required = false) String locked,
                        @RequestParam(value = "disabled", required = false) String disabled,
                        @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "registered", required = false) String registered,
                        Model model) {
        if (invalid != null) {
            model.addAttribute("loginError", "Invalid credentials. Please check your username and password.");
        } else if (locked != null) {
            model.addAttribute("loginError", "Your account is locked. Please contact support.");
        } else if (disabled != null) {
            model.addAttribute("loginError", "Your account is disabled. Please contact support.");
        } else if (error != null) {
            model.addAttribute("loginError", "Login failed. Please try again.");
        }

        if (logout != null) {
            model.addAttribute("loginSuccess", "You have been logged out successfully.");
        }
        if (registered != null) {
            model.addAttribute("loginSuccess", "Registration successful. Please log in.");
        }

        return "login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        if (!model.containsAttribute("registrationRequest")) {
            model.addAttribute("registrationRequest", new RegistrationRequest());
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String register(@Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("signupError", bindingResult.getFieldError() != null ? bindingResult.getFieldError().getDefaultMessage() : "Invalid signup data");
            return "signup";
        }

        userService.registerUser(request);
        // After successful registration redirect to login page so user can sign in
        return "redirect:/login?registered=true";
    }

    @PostMapping("/faculty/{facultyId}/reviews/{reviewId}/report")
    public String reportReview(@PathVariable Long facultyId,
                               @PathVariable Long reviewId) {
        reviewRepository.findByIdAndFaculty_Id(reviewId, facultyId).ifPresent(review -> {
            review.setReported(true);
            reviewRepository.save(review);
        });
        return "redirect:/faculty/" + facultyId + "?reviewReported=1";
    }

    private double averageScore(List<Review> reviews, String criterion) {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (Review r : reviews) {
            total += switch (criterion) {
                case "smk" -> scoreOrRating(r.getSubjectMatterKnowledge(), r.getRating());
                case "tm" -> scoreOrRating(r.getTeachingMethods(), r.getRating());
                case "se" -> scoreOrRating(r.getStudentEngagement(), r.getRating());
                case "ct" -> scoreOrRating(r.getCollaborationTeamwork(), r.getRating());
                case "bm" -> scoreOrRating(r.getBehaviorManagement(), r.getRating());
                case "ce" -> scoreOrRating(r.getClassroomEnvironment(), r.getRating());
                case "pe" -> scoreOrRating(r.getProfessionalEthics(), r.getRating());
                case "cs" -> scoreOrRating(r.getCommunicationSkills(), r.getRating());
                default -> r.getRating();
            };
        }

        double avg = total / reviews.size();
        return Math.round(avg * 10.0) / 10.0;
    }

    private int scoreOrRating(short criterionScore, int rating) {
        return criterionScore > 0 ? criterionScore : rating;
    }
}

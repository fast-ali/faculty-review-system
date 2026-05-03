package pk.edu.nu.isb.bms.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pk.edu.nu.isb.bms.models.*;
import pk.edu.nu.isb.bms.services.AdminService.ReviewRow;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseRequestRepository courseRequestRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminService adminService;

    private MyUser adminActor;
    private MyUser targetUser;

    @BeforeEach
    void setUp() {
        adminActor = new MyUser();
        adminActor.setId(1L);
        adminActor.setUsername("admin");
        adminActor.setRole("ROLE_ADMIN");

        targetUser = new MyUser();
        targetUser.setId(2L);
        targetUser.setUsername("student");
        targetUser.setRole("ROLE_STUDENT");
        targetUser.setEnabled(true);
    }

    @Test
    void listUsers() {
        when(userRepository.findAll()).thenReturn(List.of(adminActor, targetUser));
        List<MyUser> users = adminService.listUsers();
        assertEquals(2, users.size());
    }

    @Test
    void findUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        assertTrue(adminService.findUser(2L).isPresent());
    }

    // --- setUserRole Tests ---

    @Test
    void setUserRole_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminActor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        adminService.setUserRole(2L, "ROLE_FACULTY", 1L);

        verify(userRepository, times(1)).save(targetUser);
        assertEquals("ROLE_FACULTY", targetUser.getRole());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class)); // Verifies logAction was called
    }

    @Test
    void setUserRole_SelfDemotionBlocked() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminActor));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            // Admin tries to demote themselves to STUDENT
            adminService.setUserRole(1L, "ROLE_STUDENT", 1L);
        });
        assertEquals("You cannot demote your own admin role.", exception.getMessage());
    }

    // --- setUserEnabled Tests ---

    @Test
    void setUserEnabled_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminActor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        adminService.setUserEnabled(2L, false, 1L);

        assertFalse(targetUser.isEnabled());
        verify(userRepository, times(1)).save(targetUser);
    }

    @Test
    void setUserEnabled_SelfDisableBlocked() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminActor));

        assertThrows(IllegalArgumentException.class, () -> {
            adminService.setUserEnabled(1L, false, 1L);
        });
    }

    @Test
    void setUserEnabled_TargetIsAdminBlocked() {
        MyUser anotherAdmin = new MyUser();
        anotherAdmin.setId(3L);
        anotherAdmin.setRole("ROLE_ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminActor));
        when(userRepository.findById(3L)).thenReturn(Optional.of(anotherAdmin));

        assertThrows(IllegalArgumentException.class, () -> {
            adminService.setUserEnabled(3L, false, 1L);
        });
    }

    // --- deleteUser Tests ---

    @Test
    void deleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminActor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        adminService.deleteUser(2L, 1L);

        verify(userRepository, times(1)).deleteById(2L);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void deleteUser_SelfDeleteBlocked() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminActor));

        assertThrows(IllegalArgumentException.class, () -> {
            adminService.deleteUser(1L, 1L);
        });
    }

    // --- Review and Course Request Tests ---

    @Test
    void listReportedReviews() {
        Review cleanReview = new Review();
        cleanReview.setReported(false);

        Review reportedReview = new Review();
        reportedReview.setReported(true);

        when(reviewRepository.findAll()).thenReturn(List.of(cleanReview, reportedReview));

        List<Review> result = adminService.listReportedReviews();
        assertEquals(1, result.size());
        assertTrue(result.get(0).isReported());
    }

    @Test
    void listAllReviewRows() {
        Review review = new Review();
        review.setId(10L);
        review.setRating(5);
        review.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(reviewRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(review));

        List<ReviewRow> rows = adminService.listAllReviewRows();
        assertEquals(1, rows.size());
        assertEquals("Anonymous", rows.get(0).reviewer());
        assertEquals(5, rows.get(0).averageRating());
    }

    @Test
    void deleteReview() {
        Review review = new Review();
        review.setId(10L);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        adminService.deleteReview(10L, 1L);

        verify(reviewRepository, times(1)).delete(review);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // --- Course Request Approval/Rejection ---

    @Test
    void approveCourseRequest() {
        CourseRequest request = new CourseRequest();
        request.setId(5L);
        request.setRequestedCourseTitle("Software Engineering");
        request.setRequestedCourseCode("CS3009");
        FacultyEntity faculty = new FacultyEntity();
        faculty.setId(100L);
        request.setFaculty(faculty);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminActor));
        when(courseRequestRepository.findByIdAndStatus(5L, "PENDING")).thenReturn(Optional.of(request));

        // Mock that the course doesn't exist yet so it has to be created
        when(courseRepository.findByCodeIgnoreCase("CS3009")).thenReturn(Optional.empty());

        Course savedCourse = new Course();
        savedCourse.setId(50L);
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        adminService.approveCourseRequest(5L, 1L);

        assertEquals("APPROVED", request.getStatus());
        assertEquals(1L, request.getProcessedByUserId());
        verify(courseRepository, times(1)).linkCourseToFaculty(100L, 50L);
        verify(courseRequestRepository, times(1)).save(request);
    }

    @Test
    void rejectCourseRequest_Success() {
        CourseRequest request = new CourseRequest();
        request.setId(5L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminActor));
        when(courseRequestRepository.findByIdAndStatus(5L, "PENDING")).thenReturn(Optional.of(request));

        adminService.rejectCourseRequest(5L, "Duplicate course", 1L);

        assertEquals("REJECTED", request.getStatus());
        assertEquals("Duplicate course", request.getRejectionReason());
        verify(courseRequestRepository, times(1)).save(request);
    }

    @Test
    void rejectCourseRequest_MissingReason() {
        CourseRequest request = new CourseRequest();

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminActor));
        when(courseRequestRepository.findByIdAndStatus(5L, "PENDING")).thenReturn(Optional.of(request));

        assertThrows(IllegalArgumentException.class, () -> {
            adminService.rejectCourseRequest(5L, "   ", 1L); // Empty reason
        });
    }

    // --- Department and General Tests ---

    @Test
    void addDepartment_New() {
        when(departmentRepository.findByNameIgnoreCase("CS")).thenReturn(null);

        Department newDept = new Department();
        newDept.setName("CS");
        when(departmentRepository.save(any(Department.class))).thenReturn(newDept);

        Department result = adminService.addDepartment("CS", 1L);

        assertEquals("CS", result.getName());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void addDepartment_AlreadyExists() {
        Department existing = new Department();
        existing.setName("CS");
        when(departmentRepository.findByNameIgnoreCase("CS")).thenReturn(existing);

        Department result = adminService.addDepartment("CS", 1L);

        assertEquals("CS", result.getName());
        verify(departmentRepository, never()).save(any()); // Should not save a new one
    }

    @Test
    void getUserGrowthData() {
        // Arrange: Give it some users created recently
        MyUser user1 = new MyUser();
        user1.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));

        when(userRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(user1));

        // Act
        AdminService.UserGrowthData data = adminService.getUserGrowthData("7d");

        // Assert
        assertNotNull(data);
        assertEquals(7, data.labels().size());
        assertEquals(7, data.values().size());
    }
}
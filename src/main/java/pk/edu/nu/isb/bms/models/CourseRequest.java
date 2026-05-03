package pk.edu.nu.isb.bms.models;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "course_requests")
public class CourseRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private FacultyEntity faculty;

    @Column(name = "requested_course_title", nullable = false)
    private String requestedCourseTitle;

    @Column(name = "requested_course_code", length = 50)
    private String requestedCourseCode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "processed_by_user_id")
    private Long processedByUserId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @PrePersist
    void prePersist() {
        if (status == null) status = "PENDING";
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FacultyEntity getFaculty() { return faculty; }
    public void setFaculty(FacultyEntity faculty) { this.faculty = faculty; }
    public String getRequestedCourseTitle() { return requestedCourseTitle; }
    public void setRequestedCourseTitle(String requestedCourseTitle) { this.requestedCourseTitle = requestedCourseTitle; }
    public String getRequestedCourseCode() { return requestedCourseCode; }
    public void setRequestedCourseCode(String requestedCourseCode) { this.requestedCourseCode = requestedCourseCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Long getProcessedByUserId() { return processedByUserId; }
    public void setProcessedByUserId(Long processedByUserId) { this.processedByUserId = processedByUserId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}


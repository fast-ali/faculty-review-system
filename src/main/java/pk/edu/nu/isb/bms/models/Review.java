package pk.edu.nu.isb.bms.models;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private FacultyEntity faculty;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private int rating;

    @Column(name = "subject_matter_knowledge", nullable = false)
    private short subjectMatterKnowledge;

    @Column(name = "teaching_methods", nullable = false)
    private short teachingMethods;

    @Column(name = "student_engagement", nullable = false)
    private short studentEngagement;

    @Column(name = "collaboration_teamwork", nullable = false)
    private short collaborationTeamwork;

    @Column(name = "behavior_management", nullable = false)
    private short behaviorManagement;

    @Column(name = "classroom_environment", nullable = false)
    private short classroomEnvironment;

    @Column(name = "professional_ethics", nullable = false)
    private short professionalEthics;

    @Column(name = "communication_skills", nullable = false)
    private short communicationSkills;

    @Column(columnDefinition = "text")
    private String comment;

    private boolean reported;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FacultyEntity getFaculty() { return faculty; }
    public void setFaculty(FacultyEntity faculty) { this.faculty = faculty; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public short getSubjectMatterKnowledge() { return subjectMatterKnowledge; }
    public void setSubjectMatterKnowledge(short subjectMatterKnowledge) { this.subjectMatterKnowledge = subjectMatterKnowledge; }

    public short getTeachingMethods() { return teachingMethods; }
    public void setTeachingMethods(short teachingMethods) { this.teachingMethods = teachingMethods; }

    public short getStudentEngagement() { return studentEngagement; }
    public void setStudentEngagement(short studentEngagement) { this.studentEngagement = studentEngagement; }

    public short getCollaborationTeamwork() { return collaborationTeamwork; }
    public void setCollaborationTeamwork(short collaborationTeamwork) { this.collaborationTeamwork = collaborationTeamwork; }

    public short getBehaviorManagement() { return behaviorManagement; }
    public void setBehaviorManagement(short behaviorManagement) { this.behaviorManagement = behaviorManagement; }

    public short getClassroomEnvironment() { return classroomEnvironment; }
    public void setClassroomEnvironment(short classroomEnvironment) { this.classroomEnvironment = classroomEnvironment; }

    public short getProfessionalEthics() { return professionalEthics; }
    public void setProfessionalEthics(short professionalEthics) { this.professionalEthics = professionalEthics; }

    public short getCommunicationSkills() { return communicationSkills; }
    public void setCommunicationSkills(short communicationSkills) { this.communicationSkills = communicationSkills; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public boolean isReported() { return reported; }
    public void setReported(boolean reported) { this.reported = reported; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

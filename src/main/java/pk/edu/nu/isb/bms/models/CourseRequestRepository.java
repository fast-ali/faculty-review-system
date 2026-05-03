package pk.edu.nu.isb.bms.models;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRequestRepository extends JpaRepository<CourseRequest, Long> {

    List<CourseRequest> findAllByOrderByCreatedAtDesc();

    List<CourseRequest> findByStatusOrderByCreatedAtDesc(String status);

    Optional<CourseRequest> findByIdAndStatus(Long id, String status);

    boolean existsByFaculty_IdAndRequestedCourseTitleIgnoreCaseAndStatus(Long facultyId, String requestedCourseTitle, String status);
}


package pk.edu.nu.isb.bms.models;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCodeIgnoreCase(String code);

    List<Course> findAllByOrderByCodeAsc();

    @Query(value = "SELECT c.* FROM courses c INNER JOIN faculty_courses fc ON fc.course_id = c.id WHERE fc.faculty_id = :facultyId ORDER BY c.code", nativeQuery = true)
    List<Course> findByFacultyId(@Param("facultyId") Long facultyId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM faculty_courses fc WHERE fc.faculty_id = :facultyId AND fc.course_id = :courseId)", nativeQuery = true)
    boolean existsFacultyCourse(@Param("facultyId") Long facultyId, @Param("courseId") Long courseId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM faculty_courses fc WHERE fc.faculty_id = :facultyId)", nativeQuery = true)
    boolean existsAnyFacultyCourse(@Param("facultyId") Long facultyId);

    @Modifying
    @Query(value = "INSERT INTO faculty_courses (faculty_id, course_id) VALUES (:facultyId, :courseId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void linkCourseToFaculty(@Param("facultyId") Long facultyId, @Param("courseId") Long courseId);
}


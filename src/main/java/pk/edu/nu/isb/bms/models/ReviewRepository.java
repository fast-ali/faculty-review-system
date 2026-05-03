package pk.edu.nu.isb.bms.models;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByFaculty_Id(Long facultyId);

    List<Review> findAllByOrderByCreatedAtDesc();

    Optional<Review> findByIdAndFaculty_Id(Long reviewId, Long facultyId);
}

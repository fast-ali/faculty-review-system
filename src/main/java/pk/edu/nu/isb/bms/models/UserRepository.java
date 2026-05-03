package pk.edu.nu.isb.bms.models;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<MyUser, Long> {

    Optional<MyUser> findByUsername(String username);

    Optional<MyUser> findByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    long countByRoleAndEnabledTrueAndAccountLockedFalse(String role);

    boolean existsByIdAndRole(Long id, String role);

    List<MyUser> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(OffsetDateTime from);
}

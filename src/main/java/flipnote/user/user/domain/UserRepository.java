package flipnote.user.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByIdAndStatus(Long id, User.Status status);

    Optional<User> findByEmailAndStatus(String email, User.Status status);

    List<User> findByIdInAndStatus(List<Long> ids, User.Status status);
}

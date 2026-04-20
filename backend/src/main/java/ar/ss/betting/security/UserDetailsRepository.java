package ar.ss.betting.security;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDetailsRepository extends JpaRepository<User,Long> {

    Optional<User> findByUsername(String username);
}

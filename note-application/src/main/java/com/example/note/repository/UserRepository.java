package com.example.note.repository;

import com.example.note.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    Optional<User> findByVerificationToken(String verificationToken);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}

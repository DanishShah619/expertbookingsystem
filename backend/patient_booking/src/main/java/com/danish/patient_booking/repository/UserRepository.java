package com.danish.patient_booking.repository;

import org.springframework.data.repository.ListCrudRepository;
import com.danish.patient_booking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmail(String email);
}



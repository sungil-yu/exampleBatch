package com.example.examplebatch.part4;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Collection<User> findAllByUpdatedDate(LocalDate now);

}

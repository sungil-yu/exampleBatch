package com.example.examplebatch.part4;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Collection<User> findAllByUpdatedDate(LocalDate now);

    @Query("select min(u.id) from User u")
    long findMinId();

    @Query("select max(u.id) from User u")
    long findMaxId();

}

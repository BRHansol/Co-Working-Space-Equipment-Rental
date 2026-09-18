package com.example.roombooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.roombooking.domain.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}

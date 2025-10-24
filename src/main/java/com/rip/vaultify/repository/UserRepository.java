package com.rip.vaultify.repository;

import com.rip.vaultify.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

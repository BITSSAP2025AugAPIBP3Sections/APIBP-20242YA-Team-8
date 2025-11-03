package com.rip.vaultify.repository;

import com.rip.vaultify.model.File;
import com.rip.vaultify.model.Permission;
import com.rip.vaultify.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByFileAndUser(File file, User user);

    List<Permission> findByFile(File file);
    
    List<Permission> findByUser(User user);
    
    @Query("SELECT p FROM Permission p WHERE p.user = :user AND p.access != :access")
    List<Permission> findByUserExcludingAccess(@Param("user") User user, @Param("access") Permission.Access access);
}

package com.rip.vaultify.repository;

import com.rip.vaultify.model.FileEntity;
import com.rip.vaultify.model.Permission;
import com.rip.vaultify.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByFileAndUser(FileEntity file, User user);

    List<Permission> findByFile(FileEntity file);
}

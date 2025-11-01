package com.rip.vaultify.repository;

import com.rip.vaultify.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByFolderId(Long folderId);
    List<File> findByOriginalNameContainingIgnoreCase(String name);
}
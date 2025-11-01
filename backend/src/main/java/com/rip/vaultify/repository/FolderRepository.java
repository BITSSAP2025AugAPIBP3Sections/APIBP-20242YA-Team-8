package com.rip.vaultify.repository;

import com.rip.vaultify.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByParentId(Long parentId);
    List<Folder> findByUserId(Long userId);
    List<Folder> findByUserIdAndParentIdIsNull(Long userId);
    List<Folder> findByUserIdAndParentId(Long userId, Long parentId);
}

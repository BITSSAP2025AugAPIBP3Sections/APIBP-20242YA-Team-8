package com.rip.vaultify.service;

import com.rip.vaultify.model.File;
import com.rip.vaultify.model.Permission;
import com.rip.vaultify.model.User;
import com.rip.vaultify.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    /**
     * Share a file with a user. Only the owner can share files.
     * Non-owners cannot share files further.
     * 
     * @param file The file to share
     * @param targetUser The user to share with
     * @param access The access level (READ or WRITE, not OWNER)
     * @param owner The user attempting to share (must be the owner)
     * @return The created or updated permission
     * @throws RuntimeException if the requester is not the owner or trying to grant OWNER access
     */
    @Transactional
    public Permission share(File file, User targetUser, Permission.Access access, User owner) {
        // Only owner can share files
        if (!isOwner(file, owner)) {
            throw new RuntimeException("Only the file owner can share this file");
        }
        
        // Cannot grant OWNER access through sharing (only file creator gets OWNER)
        if (access == Permission.Access.OWNER) {
            throw new RuntimeException("Cannot grant OWNER access. Only the file creator is the owner.");
        }
        
        // Cannot share with the owner (they already have OWNER access)
        if (targetUser.getId().equals(owner.getId())) {
            throw new RuntimeException("Cannot share file with the owner");
        }
        
        Optional<Permission> maybe = permissionRepository.findByFileAndUser(file, targetUser);
        Permission p;
        if (maybe.isPresent()) {
            p = maybe.get();
        } else {
            p = new Permission();
            p.setFile(file);
            p.setUser(targetUser);
        }
        p.setAccess(access);
        return permissionRepository.save(p);
    }

    /**
     * Create an OWNER permission for the file creator
     */
    @Transactional
    public Permission createOwnerPermission(File file, User owner) {
        Optional<Permission> existing = permissionRepository.findByFileAndUser(file, owner);
        if (existing.isPresent()) {
            // Update if exists
            Permission p = existing.get();
            p.setAccess(Permission.Access.OWNER);
            return permissionRepository.save(p);
        } else {
            Permission p = new Permission();
            p.setFile(file);
            p.setUser(owner);
            p.setAccess(Permission.Access.OWNER);
            return permissionRepository.save(p);
        }
    }

    /**
     * Check if a user is the owner of a file
     */
    public boolean isOwner(File file, User user) {
        Optional<Permission> permission = permissionRepository.findByFileAndUser(file, user);
        return permission.isPresent() && permission.get().getAccess() == Permission.Access.OWNER;
    }

    /**
     * Check if a user has read or write permission on a file
     */
    public boolean hasReadPermission(File file, User user) {
        // Owner always has read permission
        if (isOwner(file, user)) {
            return true;
        }
        
        Optional<Permission> permission = permissionRepository.findByFileAndUser(file, user);
        return permission.isPresent() && 
               (permission.get().getAccess() == Permission.Access.READ || 
                permission.get().getAccess() == Permission.Access.WRITE);
    }

    /**
     * Check if a user has write permission on a file
     */
    public boolean hasWritePermission(File file, User user) {
        // Owner always has write permission
        if (isOwner(file, user)) {
            return true;
        }
        
        Optional<Permission> permission = permissionRepository.findByFileAndUser(file, user);
        return permission.isPresent() && permission.get().getAccess() == Permission.Access.WRITE;
    }

    public List<Permission> getFilePermissions(File file) {
        return permissionRepository.findByFile(file);
    }

    public Optional<Permission> getUserPermission(File file, User user) {
        return permissionRepository.findByFileAndUser(file, user);
    }

    /**
     * Get all shared files for a user (excluding OWNER permissions and viewed ones)
     */
    public List<Permission> getSharedFilesForUser(User user) {
        List<Permission> allShared = permissionRepository.findByUserExcludingAccess(user, Permission.Access.OWNER);
        // Filter out already viewed notifications
        return allShared.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getViewed()))
                .collect(Collectors.toList());
    }
    
    /**
     * Mark a permission as viewed/accepted
     */
    @Transactional
    public void markPermissionAsViewed(Long permissionId, User user) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found"));
        
        if (!permission.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Permission does not belong to user");
        }
        
        permission.setViewed(true);
        permissionRepository.save(permission);
    }

    /**
     * Get file permissions with user info
     */
    public List<Map<String, Object>> getFilePermissionsWithUserInfo(File file) {
        List<Permission> permissions = permissionRepository.findByFile(file);
        return permissions.stream()
                .map(p -> {
                    Map<String, Object> permInfo = new HashMap<>();
                    permInfo.put("id", p.getId());
                    permInfo.put("username", p.getUser().getUsername());
                    permInfo.put("userId", p.getUser().getId());
                    permInfo.put("access", p.getAccess().name());
                    return permInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get owner of a file
     */
    public Optional<User> getFileOwner(File file) {
        return permissionRepository.findByFile(file).stream()
                .filter(p -> p.getAccess() == Permission.Access.OWNER)
                .map(Permission::getUser)
                .findFirst();
    }
    
    /**
     * Get all accepted shared files for a user (viewed = true, excluding OWNER)
     */
    public List<Permission> getAcceptedSharedFilesForUser(User user) {
        List<Permission> allShared = permissionRepository.findByUserExcludingAccess(user, Permission.Access.OWNER);
        // Return only viewed (accepted) files
        return allShared.stream()
                .filter(p -> Boolean.TRUE.equals(p.getViewed()))
                .collect(Collectors.toList());
    }
    
    /**
     * Update permission access level (only owner can do this)
     */
    @Transactional
    public Permission updatePermission(Long permissionId, Permission.Access newAccess, User owner) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found"));
        
        File file = permission.getFile();
        
        // Only owner can update permissions
        if (!isOwner(file, owner)) {
            throw new RuntimeException("Only the file owner can update permissions");
        }
        
        // Cannot update to OWNER
        if (newAccess == Permission.Access.OWNER) {
            throw new RuntimeException("Cannot grant OWNER access");
        }
        
        // Mark as unviewed so user gets notification
        permission.setViewed(false);
        permission.setAccess(newAccess);
        return permissionRepository.save(permission);
    }
    
    /**
     * Revoke permission (only owner can do this)
     */
    @Transactional
    public void revokePermission(Long permissionId, User owner) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found"));
        
        File file = permission.getFile();
        
        // Only owner can revoke permissions
        if (!isOwner(file, owner)) {
            throw new RuntimeException("Only the file owner can revoke permissions");
        }
        
        // Cannot revoke OWNER permission
        if (permission.getAccess() == Permission.Access.OWNER) {
            throw new RuntimeException("Cannot revoke OWNER permission");
        }
        
        permissionRepository.delete(permission);
    }
}

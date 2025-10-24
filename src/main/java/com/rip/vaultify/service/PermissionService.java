package com.rip.vaultify.service;

import com.rip.vaultify.model.FileEntity;
import com.rip.vaultify.model.Permission;
import com.rip.vaultify.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface PermissionService {
    public Permission share(FileEntity file, User user, Permission.Access access);
    public List<Permission> getFilePermissions(FileEntity file);
    public Optional<Permission> getUserPermission(FileEntity file, User user);
}

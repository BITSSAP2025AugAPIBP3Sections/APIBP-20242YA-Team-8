package com.rip.vaultify.service.Implementation;

import com.rip.vaultify.model.FileEntity;
import com.rip.vaultify.model.Permission;
import com.rip.vaultify.model.User;
import com.rip.vaultify.repository.PermissionRepository;
import com.rip.vaultify.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public class PermissionServiceImplementation implements PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public Permission share(FileEntity file, User user, Permission.Access access){
        Optional<Permission> maybe = permissionRepository.findByFileAndUser(file, user);
        if(maybe.isPresent()){
            Permission p = maybe.get();
            p.setAccess(access);
            return permissionRepository.save(p);
        }else{
            Permission p = new Permission();
            p.setFile(file);
            p.setAccess(access);
            return permissionRepository.save(p);
        }
    }

    @Override
    public List<Permission> getFilePermissions(FileEntity file){
        return permissionRepository.findByFile(file);
    }

    @Override
    public Optional<Permission> getUserPermission(FileEntity file, User user){
        return permissionRepository.findByFileAndUser(file, user);
    }
}

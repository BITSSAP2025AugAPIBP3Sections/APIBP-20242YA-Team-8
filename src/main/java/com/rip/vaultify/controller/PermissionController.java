package com.rip.vaultify.controller;

import com.rip.vaultify.model.FileEntity;
import com.rip.vaultify.model.Permission;
import com.rip.vaultify.model.User;
import com.rip.vaultify.repository.FileRepository;
import com.rip.vaultify.repository.UserRepository;
import com.rip.vaultify.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/share")
    public ResponseEntity<?> share(@RequestBody Map<String, String> body){
        Long fileId = Long.valueOf(body.get("fileId"));
        String username = body.get("username");
        String access = body.get("access");

        FileEntity file = fileRepository.findById(fileId).orElseThrow();
        // User user = userRepository.findByUsername(username).orElseThrow();
        Permission.Access acc = Permission.Access.valueOf(access.toUpperCase());
        // Permission p = permissionService.share(file, user, acc);
        // return ResponseEntity.ok(Map.of("id", p.getId(),"access", p.getAccess().name()));
        return null;
    }
}

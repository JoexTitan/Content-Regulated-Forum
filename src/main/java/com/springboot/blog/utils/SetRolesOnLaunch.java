package com.springboot.blog.utils;

import com.springboot.blog.entity.RoleEntity;
import com.springboot.blog.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SetRolesOnLaunch {

    private final RoleRepository roleRepository;
    @PostConstruct
    public void init() {
        createRoleIfNotFound("ROLE_ADMIN");
        createRoleIfNotFound("ROLE_USER");
    }
    private void createRoleIfNotFound(String name) {
        RoleEntity role = roleRepository.findByName(name)
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity();
                    newRole.setName(name);
                    return roleRepository.save(newRole);
                });
    }
}

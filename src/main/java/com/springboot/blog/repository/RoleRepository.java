package com.springboot.blog.repository;

import com.springboot.blog.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity,Long> {
    // BASIC CRUD OPERATIONS INHERITED FROM JPA REPOSITORY
    Optional<RoleEntity> findByName(String name);
}
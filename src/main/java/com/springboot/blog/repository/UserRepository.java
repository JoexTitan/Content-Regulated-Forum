package com.springboot.blog.repository;

import com.springboot.blog.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<UserEntity,Long> {
    Boolean existsByEmail(String email);

    Boolean existsByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsernameOrEmail(String username, String email);

    @Query("SELECT u FROM UserEntity u WHERE u.username = :username")
    Optional<UserEntity> findByUsername(@Param("username") String username);

    @Query(value = "SELECT * FROM users u INNER JOIN user_followers uf ON u.id = uf.follower_id WHERE uf.user_id = :userId", nativeQuery = true)
    Set<UserEntity> findFollowersByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM users u INNER JOIN user_followers uf ON u.id = uf.user_id WHERE uf.follower_id = :userId", nativeQuery = true)
    Set<UserEntity> findFollowingByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM users ORDER BY users.id", nativeQuery = true)
    Set<UserEntity> findAllUsers();

}

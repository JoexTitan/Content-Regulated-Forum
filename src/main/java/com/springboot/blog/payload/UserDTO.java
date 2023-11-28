package com.springboot.blog.payload;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.springboot.blog.entity.Post;
import com.springboot.blog.entity.RoleEntity;
import com.springboot.blog.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class UserDTO implements Serializable{
    private Long id;
    private String name;
    private String username;
    private String email;

    @ElementCollection
    private List<String> favBlogGenres;

    private Set<RoleEntity> roles;

    private Set<PostDto> posts = new HashSet<>();

    private Set<FollowerDto> followers = new HashSet<>();

    private Set<FollowingDto> following = new HashSet<>();
}
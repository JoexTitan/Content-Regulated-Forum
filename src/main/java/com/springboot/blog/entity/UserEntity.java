package com.springboot.blog.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Set<RoleEntity> roles;

    @OneToMany(mappedBy = "publisherID", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Post> posts = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "user_followers",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "follower_id", referencedColumnName = "id")
    )
    private Set<UserEntity> followers = new HashSet<>();

    @ManyToMany(mappedBy = "followers")
    private Set<UserEntity> following = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "reported_posts",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "post_id", referencedColumnName = "id")
    )
    private Set<Post> reportedPosts = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "liked_posts",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "post_id", referencedColumnName = "id")
    )
    private Set<Post> likedPosts = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "shared_posts",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "post_id", referencedColumnName = "id")
    )
    private Set<Post> sharedPosts = new HashSet<>();

    private List<String> favBlogGenres;
}

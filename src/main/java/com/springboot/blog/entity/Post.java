package com.springboot.blog.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "posts", uniqueConstraints = {@UniqueConstraint(columnNames = {"title"})})
public class Post implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "title", nullable = false)
    @NotBlank(message = "Title cannot be blank")
    @Size(max = 20, message = "Title must be at most 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Title must contain only letters, numbers, and spaces")
    private String title;

    @Column(name = "description", nullable = false)
    @NotBlank(message = "Description cannot be blank")
    @Size(max = 250, message = "Description must be at most 250 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Description must contain only letters, numbers, and spaces")
    private String description;

    @Column(name = "content", nullable = false)
    @NotBlank(message = "Content cannot be blank")
    @Size(min = 10, message = "Content must be at least 10 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Content must contain only letters, numbers, and spaces")
    private String content;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comment> comments = new HashSet<>();
}

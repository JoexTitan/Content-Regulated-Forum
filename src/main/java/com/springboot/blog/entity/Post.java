package com.springboot.blog.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
    private String title;

    @Column(name = "description", nullable = false)
    @NotBlank(message = "Description cannot be blank")
    @Size(max = 100000, message = "Description must be at most 100000 characters")
    private String description;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Content cannot be blank")
    @Size(min = 100, message = "Content must be at least 100 characters")
    private String content;

    @Column(name = "tags")
    @NotEmpty(message = "Tags must not be empty")
    private List<String> tags;

    @Column(name = "publishDate")
    private Date publishDate;

    @Column(name = "likesCount")
    private Long likesCount;

    @Column(name = "shareCount")
    private Long shareCount;

    @Column(name = "commentCount")
    private Long commentCount;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comment> comments = new HashSet<>();

    @Column(name = "postSentiment")
    private String postSentiment;

    @Column(name = "profanityStatus")
    private String profanityStatus;

    @Column(name = "numOfReports")
    private Long numOfReports;

    @ManyToOne
    @JoinColumn(name = "publisher_id", nullable = false)
    private UserEntity publisherID;
}

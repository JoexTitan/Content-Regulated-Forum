package com.springboot.blog.payload;

import com.springboot.blog.entity.UserEntity;
import jakarta.persistence.Column;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PostDto implements Serializable {
    private Long id;
    private Long publisherID;
    private String title;
    private String description;
    private String content;
    private List<String> tags;
    private Date publishDate;
    private Set<CommentDto> comments;
    private long likesCount;
    private long shareCount;
    private long commentCount;
    private String postSentiment;
    private String profanityStatus;
    private Long numOfReports;
}

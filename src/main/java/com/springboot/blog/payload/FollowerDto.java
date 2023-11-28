package com.springboot.blog.payload;

import lombok.Data;

@Data
public class FollowerDto {
    private Long id;
    private String name;
    private String username;
}

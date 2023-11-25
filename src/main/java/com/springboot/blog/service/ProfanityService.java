package com.springboot.blog.service;

import com.springboot.blog.entity.Post;
import com.springboot.blog.payload.PostDto;

import java.util.List;

public interface ProfanityService {
    Post profanityMarker(Post postDto);
    List<PostDto> filterPostProfanity(List<PostDto> postDtoList);
}

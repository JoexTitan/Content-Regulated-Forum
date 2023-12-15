package com.springboot.blog.service;

import com.springboot.blog.entity.Post;
import com.springboot.blog.payload.PostDto;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface ProfanityService {
    Post profanityMarker(Post postDto) throws ExecutionException, InterruptedException;
    List<PostDto> filterPostProfanity(List<PostDto> postDtoList) throws ExecutionException, InterruptedException;
}

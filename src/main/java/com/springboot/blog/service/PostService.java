package com.springboot.blog.service;

import com.springboot.blog.payload.PostDto;
import com.springboot.blog.payload.PostResponse;

import java.util.Set;

public interface PostService {
    PostDto createPost(PostDto postDto);

    PostResponse getAllPosts(int pageNo, int pageSize, String sortBy, String sortDir);

    PostDto getPostById(long id);

    PostDto updatePost(PostDto postDto, long id);

    void reportPost(Long postId, String username);

    void deletePostById(long id);

    void incrementLikes(Long postId);

    void incrementShares(Long postId);

    Set<PostDto> getPostByPublisherId(long publisherId);
}

package com.springboot.blog.service;

import com.springboot.blog.payload.PostDto;
import com.springboot.blog.payload.PostResponse;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface PostService {
    PostDto createPost(PostDto postDto) throws ExecutionException, InterruptedException;

    PostResponse getAllPosts(int pageNo, int pageSize, String sortBy, String sortDir) throws ExecutionException, InterruptedException;

    PostDto getPostById(long id);

    PostDto updatePost(PostDto postDto, long id) throws ExecutionException, InterruptedException;

    void reportPost(Long postId, String username);

    void deletePostById(long id, String username);

    void incrementLikes(Long postId, String username);

    void incrementShares(Long postId, String username);

    List<PostDto> getPostByPublisherId(long publisherId);
}

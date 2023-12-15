package com.springboot.blog.service;

import com.springboot.blog.payload.PostDto;
import com.springboot.blog.payload.UserDTO;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface UserService {
    Set<PostDto> getRecommendedPosts(long userId) throws ExecutionException, InterruptedException;

    void addFavGenres(long userId, UserDTO userDTO);

    void clearAllFavGenres(long userId);

    void follow(Long currentUser, Long targetUser);

    void unfollow(Long currentUser, Long targetUser);

    Set<UserDTO> getUserFollowers(Long user);

    Set<UserDTO> getUserFollowing(Long user);

    Set<UserDTO> getAllUsers();

    UserDTO findUserByUsername(String username);
}

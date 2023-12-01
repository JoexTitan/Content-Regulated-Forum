package com.springboot.blog.service;

import com.springboot.blog.entity.UserEntity;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.payload.UserDTO;

import java.util.List;
import java.util.Set;

public interface UserService {
    Set<PostDto> getRecommendedPosts(long userId);

    void addFavGenres(long userId, String genre);

    void follow(UserEntity currentUser, UserEntity targetUser);

    void unfollow(UserEntity currentUser, UserEntity targetUser);

    Set<UserDTO> getUserFollowers(Long user);

    Set<UserDTO> getUserFollowing(Long user);

    Set<UserDTO> getAllUsers();

    UserDTO findUserByUsername(String username);
}

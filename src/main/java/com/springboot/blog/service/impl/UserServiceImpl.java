package com.springboot.blog.service.impl;

import com.springboot.blog.entity.UserEntity;
import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.payload.*;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.service.NowTrendingService;
import com.springboot.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final NowTrendingService nowTrendingService;
    @Override
    public Set<PostDto> getRecommendedPosts(long userId) {

        // 1) fetch "Favourite Publishers" for the user with the provided ID
        Set<UserDTO> userFavPublishers = getUserFollowing(userId);

        // extract only the IDs from the userDTO objects to avoid nested loops later on
        Set<Long> userFavPublisherIDs = userFavPublishers
                .stream().map(user -> user.getId()).collect(Collectors.toSet());

        System.out.println("userFavPublisherIDs: " + userFavPublisherIDs);

        // 2) fetch "Favourite Genres" for the user with the provided ID
        UserEntity foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new BlogAPIException("Was not able to find user with ID: " + userId));

        List<String> favGenres = foundUser.getFavBlogGenres();

        // 3) Fetch top 20 weekly posts using nowTrendingService
        List<PostDto> trendyPosts = nowTrendingService.getWeeklyTrending(20);

        // 4) Compare data of trending posts with user preferences (publishers, genres, likes, popularity)
        Set<PostDto> postsFromDesiredGenres = new HashSet<>();
        Set<PostDto> postsFromDesiredPublishers = new HashSet<>();
        for (PostDto post: trendyPosts) {
            if (userFavPublisherIDs.contains(post.getPublisherID())) {
                postsFromDesiredPublishers.add(post);
                System.out.println("Post added to user feed: ID-" + post.getId());
            }
            for (String tag: post.getTags()) {
                if (favGenres.contains(tag)) {
                    postsFromDesiredGenres.add(post);
                }
            }
        }
        // return union of FavouritePublishers & FavouriteGenres
        Set<PostDto> result = new HashSet<>(postsFromDesiredGenres);
        result.addAll(postsFromDesiredPublishers);

        return result;
    }

    @Override
    public void addFavGenres(long userId, String genre) {
        UserEntity foundUser = userRepository.findById(userId).orElseThrow(
                () -> new BlogAPIException("Was not able to find the user!"));

        List<String> userPreferences = foundUser.getFavBlogGenres();

        // Check if userPreferences is null and initialize it if needed
        if (userPreferences == null) {
            userPreferences = new ArrayList<>();
            foundUser.setFavBlogGenres(userPreferences);
        }

        if (genre != null) {
            userPreferences.add(genre);
            userRepository.save(foundUser);
        }
    }

    @Override
    public void follow(UserEntity currentUser, UserEntity targetUser) {
        currentUser.getFollowing().add(targetUser);
        targetUser.getFollowers().add(currentUser);
        userRepository.save(currentUser);
        userRepository.save(targetUser);
    }

    @Override
    public void unfollow(UserEntity currentUser, UserEntity targetUser) {
        currentUser.getFollowing().remove(targetUser);
        targetUser.getFollowers().remove(currentUser);
        userRepository.save(currentUser);
        userRepository.save(targetUser);
    }

    @Override
    public Set<UserDTO> getUserFollowers(Long userID) {
        Set<UserEntity> users = userRepository.findFollowersByUserId(userID);
        Set<UserDTO> userDTOs = new HashSet<>();

        for (UserEntity user : users) {
            UserDTO userDTO = mapUserToDTO(user);
            userDTOs.add(userDTO);
        }

        return userDTOs;
    }

    @Override
    public Set<UserDTO> getUserFollowing(Long userID) {
        Set<UserEntity> users = userRepository.findFollowingByUserId(userID);
        Set<UserDTO> userDTOs = new HashSet<>();

        for (UserEntity user : users) {
            UserDTO userDTO = mapUserToDTO(user);
            userDTOs.add(userDTO);
        }

        return userDTOs;
    }

    @Override
    public Set<UserDTO> getAllUsers() {
        Set<UserEntity> users = userRepository.findAllUsers();
        Set<UserDTO> userDTOs = new HashSet<>();

        for (UserEntity user : users) {
            UserDTO userDTO = mapUserToDTO(user);
            userDTOs.add(userDTO);
        }

        return userDTOs;
    }

    @Override
    public UserDTO findUserByUsername(String username) {
        UserEntity foundUser = userRepository.findByUsername(username).orElseThrow(
                () -> new BlogAPIException("did not find the username: " + username));
        UserDTO userDTO = mapUserToDTO(foundUser);
        return userDTO;
    }

    private UserDTO mapUserToDTO(UserEntity user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        userDTO.setFavBlogGenres(user.getFavBlogGenres());
        userDTO.setRoles(user.getRoles());
        // closes off circular references errors caused by bidirectional mappings
        userDTO.setPosts(user.getPosts().stream()
                .map(post -> post.getId())
                .collect(Collectors.toSet()));

        userDTO.setFollowers(user.getFollowers().stream()
                .map(follower -> modelMapper.map(follower, FollowerDto.class))
                .collect(Collectors.toSet()));

        userDTO.setFollowing(user.getFollowing().stream()
                .map(target -> modelMapper.map(target, FollowingDto.class))
                .collect(Collectors.toSet()));

        userDTO.setReportedPosts(user.getReportedPosts().stream()
                .map(post -> post.getId())
                .collect(Collectors.toSet()));

        userDTO.setLikedPosts(user.getLikedPosts().stream()
                .map(post -> post.getId())
                .collect(Collectors.toSet()));

        userDTO.setSharedPosts(user.getSharedPosts().stream()
                .map(post -> post.getId())
                .collect(Collectors.toSet()));

        return userDTO;
    }
}


























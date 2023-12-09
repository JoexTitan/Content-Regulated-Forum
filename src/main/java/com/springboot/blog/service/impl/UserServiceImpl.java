package com.springboot.blog.service.impl;

import com.springboot.blog.entity.UserEntity;
import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.exception.ResourceNotFoundException;
import com.springboot.blog.payload.*;
import com.springboot.blog.repository.PostRepository;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.service.NowTrendingService;
import com.springboot.blog.service.ReputationService;
import com.springboot.blog.service.UserService;
import com.springboot.blog.utils.AppEnums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final ReputationService reputationService;
    private final NowTrendingService nowTrendingService;
    @Override
    // 1) will need user reputation to sort distinguished publishers & break any ties
    // 2) will need to check posts for followers' followers (friends of friends posts & interests)
    public Set<PostDto> getRecommendedPosts(long userId) {
        Set<PostDto> postsFromDesiredGenres = new HashSet<>();
        Set<PostDto> postsFromDesiredPublishers = new HashSet<>();
        // fetching "Favourite Publishers" for the user
        Set<UserDTO> userFavPublishers = getUserFollowing(userId);

        // extracting IDs from userDTO to avoid nested loops later on
        Set<Long> userFavPublisherIDs = userFavPublishers
                .stream().map(user -> user.getId()).collect(Collectors.toSet());
        System.out.println("userFavPublisherIDs: " + userFavPublisherIDs);

        // fetching "Favourite Genres" for the user with the provided ID
        UserEntity foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST,
                        "Was not able to find user with ID: " + userId, ErrorCode.USER_NOT_FOUND));

        Set<String> favGenres = foundUser.getFavBlogGenres();
        // fetching top 20 weekly posts using nowTrendingService
        List<PostDto> trendyPosts = nowTrendingService.getWeeklyTrending(20);
        // comparing data of trending posts with user preferences (genres & post tags)

        for (PostDto post : trendyPosts) {
            if (userFavPublisherIDs.contains(post.getPublisherID())) {
                System.out.println("\nPublisherID# " + post.getPublisherID() +
                        " | RANK: " + reputationService.overallReputationScore(post.getPublisherID()));

                postsFromDesiredPublishers.add(post);
                System.out.println("Added to feed from fav publisher: Post-" + post.getId());
            }
            for (String tag : post.getTags()) {
                if (favGenres.contains(tag)) {
                    postsFromDesiredGenres.add(post);
                    System.out.println("Added to feed from fav genre: Post-" + post.getId());
                    break; // no need to continue checking other genres
                }
            }
        }
        // return union of FavouritePublishers & FavouriteGenres
        Set<PostDto> result = new HashSet<>(postsFromDesiredGenres);
        result.addAll(postsFromDesiredPublishers);
        return result; // (Union) + (HashSet) won't see duplicates
    }

    @Override
    public void addFavGenres(long userId, UserDTO userDTO) {
        UserEntity foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST,
                        "Was not able to find user with ID: " + userId, ErrorCode.USER_NOT_FOUND));
        foundUser.getFavBlogGenres().addAll(userDTO.getFavBlogGenres());
        userRepository.save(foundUser);
    }

    @Override
    public void clearAllFavGenres(long userId) {
        UserEntity foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST,
                        "Was not able to find user with ID: " + userId, ErrorCode.USER_NOT_FOUND));
        foundUser.setFavBlogGenres(new HashSet<>());
        userRepository.save(foundUser);
    }

    @Override
    public void follow(Long currentUserID, Long targetUserID) {
        UserEntity currUserObj =  userRepository.findById(currentUserID).orElseThrow(
                () -> new ResourceNotFoundException("currentUserID", "ID", currentUserID));
        UserEntity targetUserObj =  userRepository.findById(targetUserID).orElseThrow(
                () -> new ResourceNotFoundException("targetUserID", "ID", targetUserID));
        currUserObj.getFollowing().add(targetUserObj);
        targetUserObj.getFollowers().add(currUserObj);
        userRepository.save(currUserObj);
        userRepository.save(targetUserObj);
    }

    @Override
    public void unfollow(Long currentUserID, Long targetUserID) {
        UserEntity currUserObj =  userRepository.findById(currentUserID).orElseThrow(
                () -> new ResourceNotFoundException("currentUserID", "ID", currentUserID));
        UserEntity targetUserObj =  userRepository.findById(targetUserID).orElseThrow(
                () -> new ResourceNotFoundException("targetUserID", "ID", targetUserID));
        currUserObj.getFollowing().remove(targetUserObj);
        targetUserObj.getFollowers().remove(currUserObj);
        userRepository.save(currUserObj);
        userRepository.save(targetUserObj);
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
        UserEntity foundUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST,
                        "Was not able to find username: " + username, ErrorCode.USER_NOT_FOUND));
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
        // below we close off circular reference errors caused by bidirectional mapping
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


























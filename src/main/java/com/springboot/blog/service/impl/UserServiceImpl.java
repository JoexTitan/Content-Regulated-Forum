package com.springboot.blog.service.impl;

import com.springboot.blog.aspect.GetExecutionTime;
import com.springboot.blog.entity.UserEntity;
import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.exception.ResourceNotFoundException;
import com.springboot.blog.payload.*;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.service.NowTrendingService;
import com.springboot.blog.service.ReputationService;
import com.springboot.blog.service.UserService;
import com.springboot.blog.utils.AppEnums.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final ReputationService reputationService;
    private final NowTrendingService nowTrendingService;

    @Value("${distinguished_publisher_threshold}")
    private double DISTINGUISHED_PUBLISHER_THRESHOLD;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserService.class);
    /**
     * For optimization purposes userRecommendedPosts are cached.
     * The cache collection is set to refresh every 3 hours.
     *
     * @param userId The unique identifier of the user.
     * @return A collection Set<PostDto> fetched from
     * trending publishers, favourite authors, and preferred genres.
     */
    @Override
    @GetExecutionTime
    @Cacheable(value = "userRecommendedPosts", key = "#userId")
    public Set<PostDto> getRecommendedPosts(long userId) throws ExecutionException, InterruptedException {
        Set<PostDto> userFeedCollection = new HashSet<>();
        // will store Map<publisherID, publisherRank>
        Map<Long, Double> publisherReputationMap = new HashMap<>();
        // fetching "Favourite Publishers" for the user
        Set<UserDTO> userFavPublishers = getUserFollowing(userId);

        // extracting IDs from userDTO to avoid nested loops
        Set<Long> userFavPublisherIDs = userFavPublishers
                .stream().map(user -> user.getId()).collect(Collectors.toSet());
        System.out.println("userFavPublisherIDs: " + userFavPublisherIDs);

        // fetching "Favourite Genres" for the user with the provided ID
        UserEntity foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST,
                        "Was not able to find user with ID: " + userId, ErrorCode.USER_NOT_FOUND));

        Set<String> favGenres = foundUser.getFavBlogGenres();
        // fetching top 25 weekly posts using nowTrendingService
        List<PostDto> trendyPosts = nowTrendingService.getWeeklyTrending(25);
        for (PostDto post : trendyPosts) {
            // check if trending posts contain user's fav publishers
            if (userFavPublisherIDs.contains(post.getPublisherID())) {
                userFeedCollection.add(post);
                System.out.println("Added post to feed from favourite publisher: Post-" + post.getId());
            }
            // we will store Map<publisherID, publisherRank> in publisherReputationMap
            if (publisherReputationMap.containsKey(post.getPublisherID())) {
                if (publisherReputationMap.get(post.getPublisherID()) > DISTINGUISHED_PUBLISHER_THRESHOLD) {
                    userFeedCollection.add(post);
                    System.out.println("Added post to feed from distinguished publisher: Post-" + post.getId());
                }
            } else {
                // if the publisher has not been added to publisherReputationMap we will add him there
                double publisherRank = reputationService.overallReputationScore(post.getPublisherID()).get();
                publisherReputationMap.put(post.getPublisherID(), publisherRank);
                System.out.println("PublisherID# " + post.getPublisherID() + " | RANK: " + publisherRank);
                // if publisher is of high rank we will be updating postsFromDistinguishedPublishers collection
                if (publisherRank >= DISTINGUISHED_PUBLISHER_THRESHOLD) {;
                    userFeedCollection.add(post);
                    System.out.println("Added post to feed from distinguished publisher: Post-" + post.getId());
                }
            }
            // check tags for the publishment & cross validate with user preferences and favourite genres
            for (String tag : post.getTags()) {
                if (favGenres.contains(tag)) {
                    userFeedCollection.add(post);
                    System.out.println("Added post to feed from favourite genre: Post-" + post.getId());
                    break; // no need to continue checking other genres
                }
            }
        }
        return userFeedCollection;
    }

    @Override
    @Transactional
    @GetExecutionTime
    @Async("asyncTaskExecutor")
    public void follow(Long currentUserID, Long targetUserID) {
        // Asynchronously fetch both users within the same transaction
        CompletableFuture<UserEntity> currUserFuture = fetchUserAsync(currentUserID);
        CompletableFuture<UserEntity> targetUserFuture = fetchUserAsync(targetUserID);

        CompletableFuture<Void> result = currUserFuture.thenCombine(targetUserFuture, (currUserObj, targetUserObj) -> {
            // Modify entities and save to the database
            currUserObj.getFollowing().add(targetUserObj);
            targetUserObj.getFollowers().add(currUserObj);

            userRepository.save(currUserObj);
            userRepository.save(targetUserObj);

            return null;
        });
        // wait for the completion of the CompletableFuture
        result.join();
    }

    @Override
    @Transactional
    @GetExecutionTime
    @Async("asyncTaskExecutor")
    public void unfollow(Long currentUserID, Long targetUserID) {
        // Asynchronously fetch both users within the same transaction
        CompletableFuture<UserEntity> currUserFuture = fetchUserAsync(currentUserID);
        CompletableFuture<UserEntity> targetUserFuture = fetchUserAsync(targetUserID);

        CompletableFuture<Void> result = currUserFuture.thenCombine(targetUserFuture, (currUserObj, targetUserObj) -> {
            // Modify entities and save to the database
            currUserObj.getFollowing().remove(targetUserObj);
            targetUserObj.getFollowers().remove(currUserObj);

            userRepository.save(currUserObj);
            userRepository.save(targetUserObj);

            return null;
        });
        // wait for the completion of the CompletableFuture
        result.join();
    }

    @Transactional
    @Async("asyncTaskExecutor")
    public CompletableFuture<UserEntity> fetchUserAsync(Long userID) {
        UserEntity user = userRepository.findById(userID)
                .orElseThrow(() -> new ResourceNotFoundException("UserID", "ID", userID));
        return CompletableFuture.completedFuture(user);
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
package com.springboot.blog.controller;

import com.springboot.blog.entity.UserEntity;
import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.jwt.JwtTokenProvider;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.payload.UserDTO;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.service.UserService;
import com.springboot.blog.utils.AppEnums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserController.class);

    @GetMapping("/{userId}/feed")
    public ResponseEntity<Set<PostDto>> getRecommendedPosts(@PathVariable Long userId, HttpServletRequest request) throws ExecutionException, InterruptedException {
        LOGGER.info("UserController.getRecommendedPosts currentUserId: {}", userId);
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from token: {}", username);
        UserDTO authenticUser = userService.findUserByUsername(username);
        // check if authenticUserID == currentUserId
        if (!authenticUser.getId().equals(userId)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "You cannot request post feed that is not your own", ErrorCode.CANNOT_BE_DIFF_USER);
        }
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "feed requested")
                .body(userService.getRecommendedPosts(userId));
    }

    @PostMapping("/{currentUserId}/follow/{targetUserId}")
    public ResponseEntity<String> follow(@PathVariable Long currentUserId,
                                         @PathVariable Long targetUserId, HttpServletRequest request) {
        LOGGER.info("UserController.follow currentUserId: {}, targetUserId: {}", currentUserId, targetUserId);
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from token: {}", username);

        UserDTO authenticUser = userService.findUserByUsername(username);
        // check if authenticUserID == currentUserId
        if (!authenticUser.getId().equals(currentUserId)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "You cannot follow on behalf of someone else", ErrorCode.CANNOT_BE_DIFF_USER);
        }
        // check if the currUser follows targetUser
        boolean alreadyFollowing = authenticUser.getFollowing().stream()
                .anyMatch(followDto -> followDto.getId().equals(targetUserId));

        if (alreadyFollowing) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "You cannot follow the same person twice");
        }

        userService.follow(currentUserId, targetUserId);
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "followed")
                .body("Followed successfully");
    }

    @PostMapping("/{currentUserId}/unfollow/{targetUserId}")
    public ResponseEntity<String> unfollow(@PathVariable Long currentUserId,
                                           @PathVariable Long targetUserId, HttpServletRequest request) {
        LOGGER.info("UserController.unfollow currentUserId: {}, targetUserId: {}", currentUserId, targetUserId);
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from token: {}", username);

        UserDTO authenticUser = userService.findUserByUsername(username);
        // check if authenticUserID == currentUserId
        if (!authenticUser.getId().equals(currentUserId)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "You cannot unfollow on behalf of someone else", ErrorCode.CANNOT_BE_DIFF_USER);
        }
        // check if the currUser follows targetUser
        boolean alreadyFollowing = authenticUser.getFollowing().stream()
                .anyMatch(followDto -> followDto.getId().equals(targetUserId));

        if (!alreadyFollowing) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "You cannot unfollow a person you didn't subscribe to");
        }

        userService.unfollow(currentUserId, targetUserId);
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "unfollowed")
                .body("Unfollowed successfully");
    }

    @PostMapping("/{userId}/preferences")
    public ResponseEntity<String> addFavGenres(@PathVariable Long userId,
                                               @RequestBody UserDTO userDTO, HttpServletRequest request) {
        LOGGER.info("UserController.addFavGenres currentUserId: {}", userId);
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from token: {}", username);

        UserDTO authenticUser = userService.findUserByUsername(username);
        // check if authenticUserID == currentUserId
        if (!authenticUser.getId().equals(userId)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "You cannot add blog preferences on behalf of someone else",
                    ErrorCode.CANNOT_BE_DIFF_USER);
        }
        // check if the currUser follows targetUser
        boolean genreAlreadyPresent = userDTO.getFavBlogGenres().stream()
                .anyMatch(newGenre -> authenticUser.getFavBlogGenres().contains(newGenre));
        if (genreAlreadyPresent) {
            throw new BlogAPIException("You already added this genre to your blog preferences");
        }

        userService.addFavGenres(userId, userDTO);
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "added preferences")
                .body(("Added preferences successfully for user #ID-" + userId));
    }

    @DeleteMapping("/{userId}/preferences")
    public ResponseEntity<String> clearAllFavGenres(@PathVariable Long userId,
                                               @RequestBody UserDTO userDTO, HttpServletRequest request) {
        LOGGER.info("UserController.clearAllFavGenres currentUserId: {}", userId);
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from token: {}", username);

        UserDTO authenticUser = userService.findUserByUsername(username);
        // check if authenticUserID == currentUserId
        if (!authenticUser.getId().equals(userId)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "You cannot remove preferences on behalf of someone else",
                    ErrorCode.CANNOT_BE_DIFF_USER);
        }
        userService.clearAllFavGenres(userId);
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "removed preferences")
                .body(("removed blog preferences successfully for user #ID-" + userId));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Set<UserDTO>> getAllFollowers(@PathVariable Long userId, HttpServletRequest request) {
        LOGGER.info("UserController.getAllFollowers currentUserId: {}", userId);
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        } // public information for any user to see following & followers of members
        return ResponseEntity.ok(userService.getUserFollowers(userId));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Set<UserDTO>> getAllFollowing(@PathVariable Long userId, HttpServletRequest request) {
        LOGGER.info("UserController.getAllFollowing currentUserId: {}", userId);
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        } // public information for any user to see following & followers of members
        return ResponseEntity.ok(userService.getUserFollowing(userId));
    }

    @GetMapping("/members")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Set<UserDTO>> getAllUsers(HttpServletRequest request) {
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        } // private information only for admin users to see & interact with
        return ResponseEntity.ok(userService.getAllUsers());
    }
}

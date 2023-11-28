package com.springboot.blog.controller;

import com.springboot.blog.entity.UserEntity;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.payload.UserDTO;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/{currentUserId}/follow/{targetUserId}")
    public ResponseEntity<String> follow(@PathVariable Long currentUserId, @PathVariable Long targetUserId) {
        userService.follow(userRepository.findById(currentUserId).get(), userRepository.findById(targetUserId).get());
        return ResponseEntity.ok("Followed successfully");
    }

    @PostMapping("/{currentUserId}/unfollow/{targetUserId}")
    public ResponseEntity<String> unfollow(@PathVariable Long currentUserId, @PathVariable Long targetUserId) {
        userService.unfollow(userRepository.findById(currentUserId).get(), userRepository.findById(targetUserId).get());
        return ResponseEntity.ok("Unfollowed successfully");
    }

    @PostMapping("/{userId}/preferences")
    public ResponseEntity<String> addFavGenres(@PathVariable Long userId, @RequestBody String genre) {
        userService.addFavGenres(userId, genre);
        return ResponseEntity.ok("Added genres successfully to user #ID-" + userId);
    }


    @GetMapping("/{userId}/followers")
    public ResponseEntity<Set<UserDTO>> getAllFollowers(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserFollowers(userId));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Set<UserDTO>> getAllFollowing(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserFollowing(userId));
    }

    @GetMapping("/members")
    public ResponseEntity<Set<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{userId}/feed")
    public ResponseEntity<Set<PostDto>> getRecommendedPosts(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getRecommendedPosts(userId));
    }


}

package com.springboot.blog.controller;

import com.springboot.blog.aspect.GetExecutionTime;
import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.jwt.JwtTokenProvider;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.payload.PostResponse;
import com.springboot.blog.payload.UserDTO;
import com.springboot.blog.service.PostService;
import com.springboot.blog.service.UserService;
import com.springboot.blog.utils.AppEnums.AppConstants;
import com.springboot.blog.utils.AppEnums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private static final Logger LOGGER = LoggerFactory.getLogger(PostController.class);

    @PostMapping("/report/{postId}")
    public ResponseEntity<String> reportPost(@PathVariable(name = "postId") long postId, HttpServletRequest request) {
        LOGGER.info("PostController.reportPost id: {}", postId);
        // extracting token form the headers
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from the token: {}", username);
        // If authorization fails we will throw an exception
        if (!authorizedToInteract(username, postId)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "You cannot report your own post", ErrorCode.CANNOT_INVOKE_ON_OWN);
        }
        // initialize the reportedPosts set to handle NullPointers
        Set<Long> reportedPosts = userService.findUserByUsername(username).getReportedPosts();
        if (reportedPosts.isEmpty()) {
            reportedPosts = new HashSet<>();
        }
        boolean targetPostAlreadyReported = reportedPosts.stream().anyMatch(postID -> postID == postId);
        if (targetPostAlreadyReported) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "You cannot report the same post twice");
        }
        LOGGER.info("the following user: {}, added a complaint!", username);
        postService.reportPost(postId, username);
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "report post")
                .body("Thank you, the post has been reported.");
    }

    @PostMapping("/like/{postId}")
    public ResponseEntity<String> likePost(@PathVariable(name = "postId") long postId, HttpServletRequest request) {
        LOGGER.info("PostController.likePost postId: {}", postId);
        // extracting token form the headers
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from the token: {}", username);
        // If authorization fails we will throw an exception
        if (!authorizedToInteract(username, postId)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "You cannot like your own post", ErrorCode.CANNOT_INVOKE_ON_OWN);
        }
        // initialize the likePost set to handle NullPointers
        Set<Long> likedPosts = userService.findUserByUsername(username).getLikedPosts();
        if (likedPosts.isEmpty()) {
            likedPosts = new HashSet<>();
        }
        // if currUser already liked the post disable their ability to like again
        boolean targetPostAlreadyLiked = likedPosts.stream().anyMatch(postID -> postID == postId);
        if (targetPostAlreadyLiked) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "You cannot like the same post twice");
        }
        postService.incrementLikes(postId, username);
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "liked post")
                .body(username + " liked the post: ID-" + postId);
    }

    @PostMapping("/share/{postId}")
    public ResponseEntity<String> sharePost(@PathVariable(name = "postId") long postId, HttpServletRequest request) {
        LOGGER.info("PostController.sharePost postId: {}", postId);
        // extracting token form the headers
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from the token: {}", username);
        // If authorization fails we will throw an exception
        if (!authorizedToInteract(username, postId)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "You cannot promote/share your own post", ErrorCode.CANNOT_INVOKE_ON_OWN);
        }
        // currUser is able to share the same post multiple times
        postService.incrementShares(postId, username);
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "shared post")
                .body(username + " shared the post: ID-" + postId);
    }

    @PostMapping
    @GetExecutionTime
    public ResponseEntity<PostDto> createPost(@Valid @RequestBody PostDto postDto, HttpServletRequest request) throws ExecutionException, InterruptedException {
        if (!contentTypeValidator(request)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "please provide json body with the request");
        }
        return new ResponseEntity<>(postService.createPost(postDto), HttpStatus.CREATED);
    }

    @GetExecutionTime
    @PutMapping("/{id}")
    public ResponseEntity<PostDto> updatePost(@Valid @RequestBody PostDto postDto,
                                              @PathVariable(name = "id") long id, HttpServletRequest request) throws ExecutionException, InterruptedException {
        LOGGER.info("PostController.updatePost postId: {}", id);
        if (!contentTypeValidator(request)) { // validate delivered content/payload
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "Unsupported media type", ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
        // extracting token form the headers
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from token: {}", username);
        // verify that currUser is the owner of the target post to update
        Long currUserId = userService.findUserByUsername(username).getId();
        Long actualPostPublisherID = postService.getPostById(id).getPublisherID();
        if (!currUserId.equals(actualPostPublisherID)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "You cannot edit posts that are not your own", ErrorCode.CANNOT_BE_DIFF_USER);
        }
        // otherwise allow the currUser to make modifications to their post
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "post update")
                .body(postService.updatePost(postDto, id));
    }

    @GetExecutionTime
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePost(@PathVariable(name = "id") long id, HttpServletRequest request){
        LOGGER.info("PostController.deletePost postId: {}", id);
        // extracting token form the headers
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from token: {}", username);
        // verify that currUser is the owner of the target post to delete
        Long currUserId = userService.findUserByUsername(username).getId();
        Long actualPostPublisherID = postService.getPostById(id).getPublisherID();
        if (!currUserId.equals(actualPostPublisherID)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "You cannot delete posts that are not your own", ErrorCode.CANNOT_BE_DIFF_USER);
        }
        // otherwise allow the currUser to proceed & delete their post
        postService.deletePostById(id, username);
        return new ResponseEntity<>("Post entity deleted successfully.", HttpStatus.OK);
    }

    @GetMapping
    @GetExecutionTime
    public PostResponse getAllPosts( // time taken to execute : 24 ms
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir) throws ExecutionException, InterruptedException {
        return postService.getAllPosts(pageNo, pageSize, sortBy, sortDir);
    }

    @GetExecutionTime
    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getPostById(@PathVariable(name = "id") long id){
        return ResponseEntity.ok(postService.getPostById(id)); // time taken to execute : 6 ms
    }

    @GetExecutionTime
    @GetMapping("/publisher/{publisherId}")
    public ResponseEntity<List<PostDto>> getPostByPublisherId(@PathVariable(name = "publisherId") long publisherId){
        return ResponseEntity.ok(postService.getPostByPublisherId(publisherId)); // time taken to execute : 19 ms
    }

    private boolean contentTypeValidator(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("application/json")) {
            return true;
        }
        return false;
    }

    private boolean authorizedToInteract(String username, long postId) {
        UserDTO currUser = userService.findUserByUsername(username);
        boolean selfInteraction = currUser.getPosts()
                .stream().anyMatch(postID -> postID == postId);

        return !selfInteraction;
    }
}
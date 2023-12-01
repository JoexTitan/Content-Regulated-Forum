package com.springboot.blog.controller;
import com.springboot.blog.aspect.GetExecutionTime;
import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.jwt.JwtTokenProvider;
import com.springboot.blog.payload.CommentDto;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.payload.UserDTO;
import com.springboot.blog.service.CommentService;
import com.springboot.blog.service.PostService;
import com.springboot.blog.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class CommentController {

    private final PostService postService;
    private final UserService userService;
    private final CommentService commentService;
    private final JwtTokenProvider jwtTokenProvider;
    private static final Logger LOGGER = LoggerFactory.getLogger(CommentController.class);

    @GetExecutionTime
    @PostMapping("/posts/{postId}/comments")
    // @Api(tags = "Comment Controller", description = "Operations related to comments") # add swagger docs #################
    public ResponseEntity<CommentDto> createComment(@PathVariable(value = "postId") long postId,
                                                    @Valid @RequestBody CommentDto commentDto, HttpServletRequest request) {
        LOGGER.info("CommentController.createComment id: {}", postId);
        if (!contentTypeValidator(request)) { // validate delivered content/payload
            throw new BlogAPIException("Unsupported media type");
        }
        // extracting token form the headers
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException("The provided jwt token is not valid");
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from token: {}", username);
        // If authorization fails we will throw an exception
        if (!authorizedToComment(username, postId)) {
            throw new BlogAPIException("You cannot comment on your own post");
        }
        boolean sameEmailOnFile = userService.findUserByUsername(username).getEmail().equals(commentDto.getEmail());
        if (!sameEmailOnFile) {
            throw new BlogAPIException("You cannot comment on behalf of someone else - choose your own email");
        }
        // attached comment body is validated by CommentValidationFilter before transacting
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("state", "comment added")
                .body(commentService.createComment(postId, commentDto));
    }

    @PutMapping("/posts/{postId}/comments/{id}")
    public ResponseEntity<CommentDto> updateComment(@PathVariable(value = "postId") Long postId,
                                                    @PathVariable(value = "id") Long commentId,
                                                    @Valid @RequestBody CommentDto commentDto, HttpServletRequest request){
        LOGGER.info("CommentController.updateComment postId: {}, commentId: {}", postId, commentId);
        if (!contentTypeValidator(request)) { // validate delivered content/payload
            throw new BlogAPIException("Unsupported media type");
        }
        // extracting token form the headers
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException("The provided jwt token is not valid");
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from token: {}", username);
        boolean sameEmailOnFile = userService
                .findUserByUsername(username).getEmail().equals(commentDto.getEmail());
        if (!sameEmailOnFile) {
            throw new BlogAPIException("You cannot comment on behalf of someone else - choose your own email");
        }
        if (!authorizedToEdit(username, postId, commentId)) {
            throw new BlogAPIException("You cannot edit comments that are not your own");
        }
        // attached comment body is validated by CommentValidationFilter before transacting
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "comment update")
                .body(commentService.updateComment(postId, commentId, commentDto));
    }

    @DeleteMapping("/posts/{postId}/comments/{id}")
    public ResponseEntity<String> deleteComment(@PathVariable(value = "postId") Long postId,
                                                @PathVariable(value = "id") Long commentId, HttpServletRequest request){
        LOGGER.info("CommentController.deleteComment postId: {}, commentId: {}", postId, commentId);
        if (!contentTypeValidator(request)) { // validate delivered content/payload
            throw new BlogAPIException("Unsupported media type");
        }
        // extracting token form the headers
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException("The provided jwt token is not valid");
        }
        // extract username from token to check authorizations
        String username = jwtTokenProvider.extractUsername(token);
        LOGGER.info("extracted username from token: {}", username);
        // If authorization fails we will throw an exception
        if (!authorizedToEdit(username, postId, commentId)) {
            throw new BlogAPIException("You cannot delete comments that are not your own");
        }
        commentService.deleteComment(postId, commentId);
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "comment deleted").build();
    }
    @GetExecutionTime
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentDto>> getCommentsByPostId(
            @PathVariable(value = "postId") Long postId, HttpServletRequest request){
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "comments fetched")
                .body(commentService.getCommentsByPostId(postId));
    }
    @GetExecutionTime
    @GetMapping("/posts/{postId}/comments/{id}")
    public ResponseEntity<CommentDto> getCommentById(@PathVariable(value = "postId") Long postId,
                                                     @PathVariable(value = "id") Long commentId){
        return ResponseEntity.status(HttpStatus.OK)
                .header("state", "comment retrieved")
                .body(commentService.getCommentById(postId, commentId)); // 39 ms
    }

    private boolean authorizedToComment(String username, long postId) {
        UserDTO commentPublisher = userService.findUserByUsername(username);
        boolean sameUser = commentPublisher.getPosts()
                .stream().anyMatch(postDto -> postDto.getId() == postId);

        if (sameUser == false) {
            return true;
        }
        return false;
    }

    private boolean authorizedToEdit(String username, long postId, long commentId) {
        String publisherEmail = userService.findUserByUsername(username).getEmail();
        PostDto targetPost = postService.getPostById(postId);

        // check if the commentId is linked to the publisher
        boolean currUserCanEdit = targetPost.getComments()
                .stream().anyMatch(commentDto ->
                        commentDto.getId() == commentId && commentDto.getEmail().equals(publisherEmail));

        return currUserCanEdit;
    }

    private boolean contentTypeValidator(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("application/json")) {
            return true;
        }
        return false;
    }
}

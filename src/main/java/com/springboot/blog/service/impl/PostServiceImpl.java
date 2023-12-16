package com.springboot.blog.service.impl;

import com.springboot.blog.entity.Post;
import com.springboot.blog.entity.UserEntity;
import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.exception.ResourceNotFoundException;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.payload.PostResponse;
import com.springboot.blog.repository.PostRepository;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.service.PostService;
import com.springboot.blog.service.ProfanityService;
import com.springboot.blog.utils.AppEnums.ErrorCode;
import com.springboot.blog.utils.AppEnums.ProfanityStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    @PersistenceContext
    private EntityManager entityManager;
    private final ModelMapper mapper;
    private final CacheManager cacheManager;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ProfanityService profanityService;
    private final SentimentAnalysisService sentimentAnalysisService;

    private UserEntity getCurrentUser() {
        String Username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(Username)
                .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST,
                        "the user was not found with username: " + Username, ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public PostDto createPost(PostDto postDto) throws ExecutionException, InterruptedException {
        String postSentiment = sentimentAnalysisService.analyzeSentiment(postDto.getContent());
        Post post = mapToEntity(postDto);
        post.setPublishDate(new Date());
        post.setNumOfReports((long) 0);
        post.setPostSentiment(postSentiment);
        UserEntity currentUser = getCurrentUser();
        post.setPublisherID(currentUser);
        // Detach the user entity
        // entityManager.detach(currentUser);

        // Add the post to the user's set of posts
        currentUser.getPosts().add(post);
        // Perform profanity check
        post = profanityService.profanityMarker(post);
        // Merge the post to reattach it
        // post = entityManager.merge(post);

        Post newPost = postRepository.save(post);
        PostDto postResponse = mapToDTO(newPost);
        return postResponse;
    }

    @Override
    public PostResponse getAllPosts(int pageNo, int pageSize, String sortBy, String sortDir) throws ExecutionException, InterruptedException {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Post> posts = postRepository.findAll(pageable);
        // get content for page/pagination object
        List<Post> listOfPosts = posts.getContent();
        List<PostDto> listOfPostDto = listOfPosts.stream().map(post -> mapToDTO(post)).collect(Collectors.toList());
        List<PostDto> profanityFreePosts = profanityService.filterPostProfanity(listOfPostDto);

        PostResponse postResponse = new PostResponse();
        postResponse.setContent(profanityFreePosts);
        postResponse.setPageNo(posts.getNumber());
        postResponse.setPageSize(posts.getSize());
        postResponse.setTotalElements(posts.getTotalElements());
        postResponse.setTotalPages(posts.getTotalPages());
        postResponse.setLast(posts.isLast());

        return postResponse;
    }

    @Override
    @Cacheable(cacheNames = "posts", key = "#id")
    public PostDto getPostById(long id) {
        Post post = postRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Post", "id", id));
        if (!Objects.equals(post.getProfanityStatus(), ProfanityStatus.ACTIVE)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The post has been blocked due to its inappropriate content.", ErrorCode.POST_BLOCKED);
        }
        return mapToDTO(post);
    }

    @Override
    @CachePut(cacheNames = "posts", key = "#id")
    public PostDto updatePost(PostDto postDto, long id) throws ExecutionException, InterruptedException {
        Post post = postRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Post", "id", id));
        post.setTitle(postDto.getTitle());
        post.setDescription(postDto.getDescription());
        post.setContent(postDto.getContent());
        Post updatedPost = postRepository.save(profanityService.profanityMarker(post));
        return mapToDTO(updatedPost);
    }

    @Override
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public void deletePostById(long postId, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST,
                        "the user was not found with username: " + username, ErrorCode.USER_NOT_FOUND));
        // Update bidirectional relationship
        user.getPosts().remove(post);
        user.getLikedPosts().remove(post);
        user.getSharedPosts().removeIf(userPostId -> userPostId.equals(postId));
        user.getReportedPosts().remove(post);
        // Save the user to update the relationship
        userRepository.save(user);
        // Delete the post & persist to the database
        postRepository.delete(post);
        // evictPostCache(postId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public void incrementLikes(Long postId, String username) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post", "id", postId));
        UserEntity currUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST,
                        "the user was not found with username: " + username, ErrorCode.USER_NOT_FOUND));
        currUser.getLikedPosts().add(post);
        post.setLikesCount(post.getLikesCount() + 1);
        userRepository.save(currUser);
        postRepository.save(post);
        // evictPostCache(postId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public void incrementShares(Long postId, String username) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post", "id", postId));
        UserEntity currUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST,
                        "the user was not found with username: " + username, ErrorCode.USER_NOT_FOUND));
        currUser.getSharedPosts().add(post);
        post.setShareCount(post.getShareCount() + 1);
        userRepository.save(currUser);
        postRepository.save(post);
        // evictPostCache(postId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public void reportPost(Long postId, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        UserEntity currUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST,
                        "the user was not found with username: " + username, ErrorCode.USER_NOT_FOUND));
        currUser.getReportedPosts().add(post); // add postId to the getReportedPosts set
        post.setNumOfReports(post.getNumOfReports() + 1); // increment # of reports for post
        userRepository.save(currUser);
        postRepository.save(post);
        // evictPostCache(postId);
    }

    @Override
    public List<PostDto> getPostByPublisherId(long publisherId) {
        List<Post> posts = postRepository.findAllPostsByPublisher(publisherId);
        List<PostDto> postDTOs = posts.stream()
                .map(post -> mapToDTO(post))
                .collect(Collectors.toList());
        return postDTOs;
    }

    private PostDto mapToDTO(Post post){
        PostDto postDto = mapper.map(post, PostDto.class);
        return postDto;
    }

    private Post mapToEntity(PostDto postDto){
        Post post = mapper.map(postDto, Post.class);
        return post;
    }
    private void evictPostCache(Long postId) {
        // Evict the cache for the associated post
        cacheManager.getCache("posts").evict(postId);
    }
}

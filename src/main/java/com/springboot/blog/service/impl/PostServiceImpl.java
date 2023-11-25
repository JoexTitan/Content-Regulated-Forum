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
import com.springboot.blog.utils.ProfanityStatus;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final ModelMapper mapper;
    private final CacheManager cacheManager;
    private final PostRepository postRepository;
    private final ProfanityService profanityService;
    private final SentimentAnalysisService sentimentAnalysisService;

    @Override
    public PostDto createPost(PostDto postDto) {

        String postSentiment = sentimentAnalysisService.analyzeSentiment(postDto.getContent());

        // convert DTO to entity
        Post post = mapToEntity(postDto);
        post.setPublishDate(new Date());
        post.setPostSentiment(postSentiment);
        // post.setProfanityStatus(ProfanityStatus.ACTIVE);
        Post newPost = postRepository.save(profanityService.profanityMarker(post));

        // convert entity to DTO
        PostDto postResponse = mapToDTO(newPost);
        return postResponse;
    }

    @Override
    public PostResponse getAllPosts(int pageNo, int pageSize, String sortBy, String sortDir) {

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
        if (Objects.equals(post.getProfanityStatus(), ProfanityStatus.ACTIVE)) {
            return mapToDTO(post);
        } else {
            throw new BlogAPIException("The current post has been blocked due to its inappropriate content.");
        }
    }

    @Override
    @CachePut(cacheNames = "posts", key = "#id")
    public PostDto updatePost(PostDto postDto, long id) {
        Post post = postRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Post", "id", id));

        post.setTitle(postDto.getTitle());
        post.setDescription(postDto.getDescription());
        post.setContent(postDto.getContent());

        Post updatedPost = postRepository.save(profanityService.profanityMarker(post));
        return mapToDTO(updatedPost);
    }
    @Override
    @CacheEvict(cacheNames = "posts", allEntries = true)
    public void incrementLikes(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post", "id", postId));
        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);
        evictPostCache(postId);
    }
    @Override
    public void incrementShares(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post", "id", postId));
        post.setShareCount(post.getShareCount() + 1);
        postRepository.save(post);
        evictPostCache(postId);
    }

    @Override
    public void deletePostById(long postId) {
        // get post by id from the database
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post", "id", postId));
        postRepository.delete(post);
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

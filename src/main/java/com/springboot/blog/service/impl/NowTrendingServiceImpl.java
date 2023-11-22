package com.springboot.blog.service.impl;

import com.springboot.blog.entity.Post;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.repository.PostRepository;
import com.springboot.blog.service.NowTrendingService;
import com.springboot.blog.utils.ObjectMapperUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class NowTrendingServiceImpl implements NowTrendingService {

    private final PostRepository postRepository;
    @Override
    public List<PostDto> getDailyTrending(int numOfPosts) {
        List<Post> listOfDailyPosts = postRepository.findDailyTrendingPosts(numOfPosts);
        return ObjectMapperUtils.mapAll(listOfDailyPosts, PostDto.class);
    }
    @Override
    public List<PostDto> getWeeklyTrending(int numOfPosts) {
        List<Post> listOfWeeklyPosts = postRepository.findWeeklyTrendingPosts(numOfPosts);
        return ObjectMapperUtils.mapAll(listOfWeeklyPosts, PostDto.class);
    }
    @Override
    public List<PostDto> getMonthlyTrending(int numOfPosts) {
        List<Post> listOfMonthlyPosts = postRepository.findMonthlyTrendingPosts(numOfPosts);
        return ObjectMapperUtils.mapAll(listOfMonthlyPosts, PostDto.class);
    }
}

package com.springboot.blog.service.impl;

import com.springboot.blog.entity.Post;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.repository.PostRepository;
import com.springboot.blog.service.NowTrendingService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class NowTrendingServiceImpl implements NowTrendingService {

    private final ModelMapper modelMapper;
    private final PostRepository postRepository;

    private <D, T> D map(final T entity, Class<D> dtoClass) {
        return modelMapper.map(entity, dtoClass);
    }
    private <D, T> List<D> mapAll(final List<T> entityList, Class<D> dtoClass) {
        return entityList.stream().map(entity -> map(entity, dtoClass)).collect(Collectors.toList());
    }

    @Override
    public List<PostDto> getDailyTrending(int numOfPosts) {
        List<Post> listOfDailyPosts = postRepository.findDailyTrendingPosts(numOfPosts);
        return mapAll(listOfDailyPosts, PostDto.class);
    }
    @Override
    public List<PostDto> getWeeklyTrending(int numOfPosts) {
        List<Post> listOfWeeklyPosts = postRepository.findWeeklyTrendingPosts(numOfPosts);
        return mapAll(listOfWeeklyPosts, PostDto.class);
    }
    @Override
    public List<PostDto> getMonthlyTrending(int numOfPosts) {
        List<Post> listOfMonthlyPosts = postRepository.findMonthlyTrendingPosts(numOfPosts);
        return mapAll(listOfMonthlyPosts, PostDto.class);
    }
}

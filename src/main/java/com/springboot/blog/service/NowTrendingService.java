package com.springboot.blog.service;
import java.util.List;
import com.springboot.blog.payload.PostDto;
public interface NowTrendingService {
    List<PostDto> getDailyTrending(int numOfPosts);
    List<PostDto> getWeeklyTrending(int numOfPosts);
    List<PostDto> getMonthlyTrending(int numOfPosts);
}

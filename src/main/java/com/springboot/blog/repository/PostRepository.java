package com.springboot.blog.repository;

import com.springboot.blog.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query(value = "SELECT * FROM posts WHERE publish_date >= CURRENT_DATE - INTERVAL 1 DAY ORDER BY likes_count DESC, share_count DESC, comment_count DESC LIMIT :numOfPosts", nativeQuery = true)
    List<Post> findDailyTrendingPosts(@Param("numOfPosts") int numOfPosts);
    @Query(value = "SELECT * FROM posts WHERE publish_date >= CURRENT_DATE - INTERVAL 7 DAY ORDER BY likes_count DESC, share_count DESC, comment_count DESC LIMIT :numOfPosts", nativeQuery = true)
    List<Post> findWeeklyTrendingPosts(@Param("numOfPosts") int numOfPosts);
    @Query(value = "SELECT * FROM posts WHERE publish_date >= CURRENT_DATE - INTERVAL 30 DAY ORDER BY likes_count DESC, share_count DESC, comment_count DESC LIMIT :numOfPosts", nativeQuery = true)
    List<Post> findMonthlyTrendingPosts(@Param("numOfPosts") int numOfPosts);
}

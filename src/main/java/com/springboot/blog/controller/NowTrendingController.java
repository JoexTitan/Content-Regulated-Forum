package com.springboot.blog.controller;

import com.springboot.blog.aspect.GetExecutionTime;
import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.jwt.JwtTokenProvider;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.service.NowTrendingService;
import com.springboot.blog.service.impl.NowTrendingServiceImpl;
import com.springboot.blog.utils.AppEnums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trending")
@RequiredArgsConstructor
public class NowTrendingController {

    private final JwtTokenProvider jwtTokenProvider;
    private final NowTrendingService nowTrendingService;

    @GetExecutionTime
    @GetMapping("/daily/{NumOfPosts}")
    public ResponseEntity<List<PostDto>> getDailyTrending(@PathVariable int NumOfPosts, HttpServletRequest request){
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        } return ResponseEntity.ok(nowTrendingService.getDailyTrending(NumOfPosts));
    }
    @GetExecutionTime
    @GetMapping("/weekly/{NumOfPosts}")
    public ResponseEntity<List<PostDto>> getWeeklyTrending(@PathVariable int NumOfPosts, HttpServletRequest request){
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        } return ResponseEntity.ok(nowTrendingService.getWeeklyTrending(NumOfPosts));
    }
    @GetExecutionTime
    @GetMapping("/monthly/{NumOfPosts}")
    public ResponseEntity<List<PostDto>> getMonthlyTrending(@PathVariable int NumOfPosts, HttpServletRequest request){
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        } return ResponseEntity.ok(nowTrendingService.getMonthlyTrending(NumOfPosts));
    }
}

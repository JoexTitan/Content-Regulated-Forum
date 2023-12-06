package com.springboot.blog.controller;
import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.jwt.JwtTokenProvider;
import com.springboot.blog.service.impl.SentimentAnalysisService;
import com.springboot.blog.utils.AppEnums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sentiment")
public class SentimentAnalysisController {

    private final JwtTokenProvider jwtTokenProvider;
    private final SentimentAnalysisService sentimentAnalysisService;
    private static final Logger LOGGER = LoggerFactory.getLogger(SentimentAnalysisController.class);
    @Autowired
    public SentimentAnalysisController(SentimentAnalysisService sentimentAnalysisService,
                                       JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sentimentAnalysisService = sentimentAnalysisService;
    }

    @PostMapping("/analyze") // proof of concept for the profanity filter class
    public ResponseEntity<String> analyzeSentiment(@RequestBody String text, HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("application/json")) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "Unsupported media type", ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
        LOGGER.info("Running SentimentAnalysisController.analyzeSentiment");
        String token = jwtTokenProvider.getTokenFromHeader(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "The provided jwt token is not valid", ErrorCode.INVALID_JWT_TOKEN);
        } String sentiment = sentimentAnalysisService.analyzeSentiment(text);
        return ResponseEntity.ok("Text Sentiment Classification: " + sentiment);
    }
}

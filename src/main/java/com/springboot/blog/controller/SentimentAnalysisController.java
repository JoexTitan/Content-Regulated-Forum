package com.springboot.blog.controller;
import com.springboot.blog.service.impl.SentimentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SentimentAnalysisController {

    private final SentimentAnalysisService sentimentAnalysisService;
    @Autowired
    public SentimentAnalysisController(SentimentAnalysisService sentimentAnalysisService) {
        this.sentimentAnalysisService = sentimentAnalysisService;
    }

    @PostMapping("/analyze-sentiment") // POC for the profanity filter classification
    public ResponseEntity<String> analyzeSentiment(@RequestBody String text) {
        String sentiment = sentimentAnalysisService.analyzeSentiment(text);
        return ResponseEntity.ok("Sentiment: " + sentiment);
    }
}

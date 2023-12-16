package com.springboot.blog.service.impl;

import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.service.ProfanityService;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
public class SentimentAnalysisService {

    private final StanfordCoreNLP pipeline;
    private final Logger logger = LoggerFactory.getLogger(SentimentAnalysisService.class);

    public SentimentAnalysisService() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        this.pipeline = new StanfordCoreNLP(props);
    }
    @CircuitBreaker(name = "defaultCircuit", fallbackMethod = "fallbackSentiment")
    public String analyzeSentiment(String text) {
        try {
            Annotation annotation = new Annotation(text);
            pipeline.annotate(annotation);
            for (edu.stanford.nlp.util.CoreMap sentence : annotation.get(
                    edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation.class)) {
                String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
                return mapSentiment(sentiment);
            }
            return "undetermined"; // return "undetermined" when the model is uncertain

        } catch (Exception e) {
            logger.error("Error during sentiment analysis for text: {}", text, e);
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "failed to analyzeSentiment");
        }
    }

    private String mapSentiment(String originalSentiment) {
        if (originalSentiment == null) {
            return "cannot classify a blank response";
        }
        switch (originalSentiment.toLowerCase()) {
            case "very negative":
                return "Very Negative";
            case "negative":
                return "Negative";
            case "neutral":
                return "Neutral";
            case "positive":
                return "Positive";
            case "very positive":
                return "Very Positive";
            default:
                return "undetermined";
        }
    }

    public String fallbackSentiment(Throwable throwable) {
        // return when the circuit opens
        return "Neutral";
    }
}
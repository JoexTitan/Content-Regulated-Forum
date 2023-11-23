package com.springboot.blog.service.impl;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class SentimentAnalysisService {

    private final StanfordCoreNLP pipeline;
    public SentimentAnalysisService() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        this.pipeline = new StanfordCoreNLP(props);
    }
    public String analyzeSentiment(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);

        for (edu.stanford.nlp.util.CoreMap sentence : annotation.get(
                edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation.class)) {
            String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
            return mapSentiment(sentiment);
        }
        return "undetermined";
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
}
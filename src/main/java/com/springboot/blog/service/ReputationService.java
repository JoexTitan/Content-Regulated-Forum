package com.springboot.blog.service;

import java.util.concurrent.CompletableFuture;

public interface ReputationService {
    public CompletableFuture<Double> overallReputationScore(long publisherID);
}

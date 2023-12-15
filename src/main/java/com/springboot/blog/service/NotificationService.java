package com.springboot.blog.service;

import java.util.concurrent.ExecutionException;

public interface NotificationService {
    public void sendRecommendedPostNotifications() throws ExecutionException, InterruptedException;
}

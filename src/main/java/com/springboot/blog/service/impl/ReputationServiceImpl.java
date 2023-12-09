package com.springboot.blog.service.impl;

import com.springboot.blog.entity.Post;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.repository.PostRepository;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.service.PostService;
import com.springboot.blog.service.ReputationService;
import com.springboot.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * The ReputationService class calculates the overall reputation score for a user,
 * considering various metrics such as engagement, posting frequency, profanity usage,
 * sentiment of posts, and the number of followers.
 *
 * Reputation plays a crucial role for the way we:
 *   i) recommend blogs to the user via user feed
 *   ii) and calculate blog profanity threshold
 */

@Service
@RequiredArgsConstructor
public class ReputationServiceImpl implements ReputationService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * Calculates the overall reputation score for a user.
     *
     * For optimization purposes, the method is annotated with @Cacheable,
     * caching the result under the "userReputationScore" cache name based on the publisherID.
     * The cache policy is set to refresh every 24 hours.
     *
     * @param publisherID The unique identifier of the user.
     * @return userReputationRating The calculated overall reputation score as a double.
     */
    // @Cacheable(value = "userReputationScore", key = "#publisherID")
    public double overallReputationScore(long publisherID) {
        List<Post> posts = postRepository.findAllPostsByPublisher(publisherID);
        // If the user has no posts, assign the lowest score possible
        if (posts == null || posts.isEmpty()) {
            return 0.0;
        } // below are individual scores for each metric that build publisher reputation
        // total achievable reputation score is 40 points since profanity is a deductible
        double postEngagementScore = Math.min(averagePostEngagement(posts), 25); // likes | shares | comments
        double postFrequencyScore = Math.min(averagePublishFrequency(posts), 2.5); // blog post frequency ratio
        double postProfanityScore = Math.min(averagePostProfanityScore(posts), 7.5); // profanity marker score
        double postSentimentScore = Math.min(averagePostSentiment(posts), 2.5); // sentiment marker for publisher
        double followerScore = Math.min((double) userRepository.findFollowersByUserId(publisherID).size() / 100, 2.5);
        System.out.println("\n\npostEngagementScore: " + postEngagementScore + "\npostFrequencyScore: " + postFrequencyScore +
        "\npostProfanityScore: " + postProfanityScore + "\npostSentimentScore: " + postSentimentScore + "\nfollowerScore: " + followerScore);
        // Combine the individual scores to obtain the overall reputation rank
        return postEngagementScore + postFrequencyScore + postSentimentScore + followerScore - postProfanityScore;
    }

    public static double averagePublishFrequency(List<Post> posts) {
        double normalizationFactor = 120;
        double totalFrequencyScore = 0.0;
        for (int i = 1; i < posts.size(); i++) {
            Date previousPostTime = posts.get(i - 1).getPublishDate();
            Date currentPostTime = posts.get(i).getPublishDate();
            double hoursBetweenPosts = calculateHoursBetweenPosts(previousPostTime, currentPostTime);
            double frequencyScore = calculateFrequencyScore(hoursBetweenPosts);
            totalFrequencyScore += frequencyScore;
        }
        // Calculate the average publish frequency score
        return (totalFrequencyScore / (posts.size() - 1)) / normalizationFactor;
    }

    private static double calculateHoursBetweenPosts(Date previousPostTime, Date currentPostTime) {
        // Calculate the time difference in hours using Date objects
        long millisecondsBetweenPosts = currentPostTime.getTime() - previousPostTime.getTime();
        return millisecondsBetweenPosts / (1000.0 * 60.0 * 60.0); // Convert milliseconds to hours
    }

    private static double calculateFrequencyScore(double hoursBetweenPosts) {
        double epsilon = 0.0001; // add epsilon to avoid division by zero
        return 1.0 / (hoursBetweenPosts + epsilon);
    }

    public static double averagePostEngagement(List<Post> posts) {
        double normalizationFactor = 18;
        List<Double> likesScores = new ArrayList<>();
        List<Double> commentsScores = new ArrayList<>();
        List<Double> sharesScores = new ArrayList<>();

        for (Post post : posts) {
            likesScores.add((double) post.getLikesCount());
            commentsScores.add((double) post.getCommentCount());
            sharesScores.add((double) post.getShareCount());
        }
        // Calculate weighted median scores for each metric
        double weightedMedianLikes = calculateWeightedMedian(likesScores, 0.45);
        double weightedMedianShares = calculateWeightedMedian(sharesScores, 0.45);
        double weightedMedianComments = calculateWeightedMedian(commentsScores, 0.10);
        // Calculate overall reputation based on weighted median scores
        return ((weightedMedianLikes + weightedMedianShares + weightedMedianComments) / 3) * normalizationFactor;
    }

    private static double calculateWeightedMedian(List<Double> list, double weight) {
        Collections.sort(list);
        int size = list.size();
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (int i = 0; i < size; i++) {
            // Adjusting weight for even-sized lists
            double medianWeight = (size % 2 == 0) ? 0.5 : 1.0;
            double currentWeight = medianWeight * weight;
            weightedSum += list.get(i) * currentWeight;
            totalWeight += currentWeight;
        }
        return weightedSum / totalWeight;
    }

    public static double averagePostSentiment(List<Post> posts) {
        int totalSentimentScore = 0;

        for (Post post : posts) {
            int sentimentScore = mapSentimentToScore(post.getPostSentiment());
            totalSentimentScore += sentimentScore;
        }
        double averageSentimentScore = (double) totalSentimentScore / posts.size();
        double normalizationFactor = 8.0;
        return averageSentimentScore * normalizationFactor;
    }

    private static int mapSentimentToScore(String sentiment) {
        // Assuming a simple mapping where "negative" is -1, "neutral" is 0, and "positive" is 1
        switch (sentiment.toLowerCase()) {
            case "negative":
                return -1;
            case "neutral":
                return 0;
            case "positive":
                return 1;
            default:
                return 0; // default to neutral if sentiment is not recognized
        }
    }

    public static double averagePostProfanityScore(List<Post> posts) {
        double totalProfanityScore = 0.0;

        for (Post post : posts) {
            double profanityScore = analyzeProfanity(post.getProfanityStatus());
            totalProfanityScore += profanityScore;
        }

        double averageProfanityScore = totalProfanityScore / posts.size();
        double normalizationFactor = 6.0;

        return averageProfanityScore * normalizationFactor;
    }

    private static double analyzeProfanity(String profanityStatus) {
        // "Blocked" means high profanity (score 1), and anything else means low profanity (score 0)
        return "Blocked".equals(profanityStatus) ? 1.0 : 0.0;
    }
}
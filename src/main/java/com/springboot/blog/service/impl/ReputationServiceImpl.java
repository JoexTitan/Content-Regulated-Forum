package com.springboot.blog.service.impl;

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
 * The ReputationCalculator class calculates the overall reputation score for a user,
 * considering various metrics such as engagement, posting frequency, profanity usage,
 * sentiment of posts, and the number of followers.
 *
 * Reputation plays a crucial role for the:
 *   i) Recommendation algorithm in the user feed
 *   ii) Blog profanity threshold allocation
 */

@Service
@RequiredArgsConstructor
public class ReputationServiceImpl implements ReputationService {

    private final PostService postService;
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
        List<PostDto> posts = postService.getPostByPublisherId(publisherID);
        // If the user has no posts, assign the lowest score possible
        if (posts == null || posts.isEmpty()) {
            return 0.0;
        } // calculate individual scores for each metric that builds reputation
        double postEngagementScore = averagePostEngagement(posts);
        double postFrequencyScore = averagePublishFrequency(posts);
        double postProfanityScore = averagePostProfanityScore(posts);
        double postSentimentScore = averagePostSentiment(posts);
        double followerScore = (double) userRepository.findFollowersByUserId(publisherID).size() / 10;
        // Combine the individual scores to obtain the overall reputation score
        System.out.println(
                "\n\npostEngagementScore: " + postEngagementScore +
                "\npostFrequencyScore: " + postFrequencyScore +
                "\npostProfanityScore: " + postProfanityScore +
                "\npostSentimentScore: " + postSentimentScore +
                "\nfollowerScore: " + followerScore
                );

        return postEngagementScore + postFrequencyScore + postSentimentScore + followerScore - postProfanityScore;
    }



    public static double averagePublishFrequency(List<PostDto> posts) {
        if (posts.isEmpty()) {
            return 0.0; // No posts, lowest frequency score assigned
        }

        // Sort posts by date
        Collections.sort(posts, Comparator.comparing(PostDto::getPublishDate));

        double totalFrequencyScore = 0.0;

        for (int i = 1; i < posts.size(); i++) {
            Date previousPostTime = posts.get(i - 1).getPublishDate();
            Date currentPostTime = posts.get(i).getPublishDate();
            double hoursBetweenPosts = calculateHoursBetweenPosts(previousPostTime, currentPostTime);
            double frequencyScore = calculateFrequencyScore(hoursBetweenPosts);
            totalFrequencyScore += frequencyScore;
        }

        // Calculate the average publish frequency score
        double averageFrequencyScore = totalFrequencyScore / (posts.size() - 1);
        return averageFrequencyScore / 380;
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


    public static double averagePostEngagement(List<PostDto> posts) {

        List<Double> likesScores = new ArrayList<>();
        List<Double> commentsScores = new ArrayList<>();
        List<Double> sharesScores = new ArrayList<>();
        List<Double> reportsScores = new ArrayList<>();

        // Define weights for each metric
        double weightLikes = 0.4;
        double weightShares = 0.3;
        double weightReports = 0.2;
        double weightComments = 0.1;

        for (PostDto post : posts) {
            likesScores.add((double) post.getLikesCount());
            commentsScores.add((double) post.getCommentCount());
            sharesScores.add((double) post.getShareCount());
            reportsScores.add((double) post.getNumOfReports());
        }
        // Calculate weighted median scores for each metric
        double weightedMedianLikes = calculateWeightedMedian(likesScores, weightLikes);
        double weightedMedianShares = calculateWeightedMedian(sharesScores, weightShares);
        double weightedMedianComments = calculateWeightedMedian(commentsScores, weightComments);
        double weightedMedianReports = calculateWeightedMedian(reportsScores, weightReports);
        // Calculate overall reputation based on weighted median scores
        double overallReputation = (weightedMedianLikes + weightedMedianShares + weightedMedianComments - weightedMedianReports) / 4;
        return overallReputation * 18;
    }

    private static double calculateWeightedMedian(List<Double> list, double weight) {
        Collections.sort(list);
        int size = list.size();
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (int i = 0; i < size; i++) {
            // Adjust weight for even-sized lists
            double medianWeight = (size % 2 == 0) ? 0.5 : 1.0;
            double currentWeight = medianWeight * weight;
            weightedSum += list.get(i) * currentWeight;
            totalWeight += currentWeight;
        }

        return weightedSum / totalWeight;
    }

    public static double averagePostSentiment(List<PostDto> posts) {
        int totalSentimentScore = 0;

        for (PostDto post : posts) {
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


    public static double averagePostProfanityScore(List<PostDto> posts) {
        double totalProfanityScore = 0.0;

        for (PostDto post : posts) {
            double profanityScore = analyzeProfanity(post.getProfanityStatus());
            totalProfanityScore += profanityScore;
        }

        double averageProfanityScore = totalProfanityScore / posts.size();
        double normalizationFactor = 6.0; // Adjust this value based on your desired range

        return averageProfanityScore * normalizationFactor;
    }
    private static double analyzeProfanity(String profanityStatus) {
        // "Blocked" means high profanity (score 1), and anything else means low profanity (score 0)
        return "Blocked".equals(profanityStatus) ? 1.0 : 0.0;
    }
}
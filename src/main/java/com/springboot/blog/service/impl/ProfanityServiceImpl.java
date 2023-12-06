package com.springboot.blog.service.impl;

import com.springboot.blog.entity.Post;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.service.ProfanityService;
import com.springboot.blog.utils.ProfanityManagerUtil;
import com.springboot.blog.utils.AppEnums.ProfanityStatus;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfanityServiceImpl implements ProfanityService {

    private final ModelMapper modelMapper;

    // English profanity dictionary from wikipedia
    // https://en.wikipedia.org/wiki/Category:English_profanity

    // dynamic threshold of 2 - 4% for all text per blog post
    private static final double NEGATIVE_PROFANITY_THRESHOLD = 0.02;
    private static final double NEUTRAL_PROFANITY_THRESHOLD  = 0.03;
    private static final double POSITIVE_PROFANITY_THRESHOLD = 0.04;
    @Override
    public List<PostDto> filterPostProfanity(List<PostDto> postDtoList) {
        List<PostDto> filteredList = new ArrayList<>();

        for (PostDto postDto : postDtoList) {
            Post postEntity = modelMapper.map(postDto, Post.class);
            Post profanityFreePost = profanityMarker(postEntity);
            if (profanityFreePost.getProfanityStatus().equals(ProfanityStatus.ACTIVE)) {
                filteredList.add(modelMapper.map(profanityFreePost, PostDto.class));
            }
        }
        return filteredList;
    }

    @Override
    public Post profanityMarker(Post post) {
        String[] words = post.getContent().split("\\s+");
        int totalWords = words.length;
        double profanityWordCount = 0;

        StringBuilder filteredText = new StringBuilder();

        for (String word : words) {
            // Separate word and punctuation
            String[] parts = word.split("([.,!])", 2);
            // System.out.println("parts: " + parts.toString());
            String wordPart = parts[0]; // The word part
            String punctuationPart = parts.length > 1 ? parts[1] : ""; // The punctuation part
            // System.out.println("punctuationPart: " + punctuationPart);
            if (ProfanityManagerUtil.PROFANITY_WORDS.containsKey(wordPart.toLowerCase())) {
                profanityWordCount += 1;
                // Append punctuation, hide profane word, and spacing
                filteredText.append(punctuationPart).append("****").append(" ");
                // System.out.println("filteredText: " + filteredText);
            } else {
                filteredText.append(word).append(" "); // Append word and spacing
            }
        }

        if (filteredText.toString().endsWith(" ")) {
            // Remove the trailing space at the end of the string
            filteredText.deleteCharAt(filteredText.length() - 1);
        }

        // System.out.println(filteredText);
        post.setContent(filteredText.toString());
        double profanityRatio = (profanityWordCount / totalWords);

        System.out.println("profanityRatio: " + profanityRatio + " #### Thresholds: P(0.04) | N(0.03) | N(0.02)");

        // If the blog has too much profanity || has bad reputation we will block it
        if (!post.getPostSentiment().isEmpty()) { // to avoid NullPointerException
            if ("Negative".equals(post.getPostSentiment())
                    && profanityRatio >= NEGATIVE_PROFANITY_THRESHOLD) {
                // blocked the post regardless of reputation / popularity
                post.setProfanityStatus(ProfanityStatus.BLOCKED);

            } else if ("Neutral".equals(post.getPostSentiment())
                    && profanityRatio >= NEUTRAL_PROFANITY_THRESHOLD) {
                post.setProfanityStatus(ProfanityStatus.BLOCKED);

            } else if ("Positive".equals(post.getPostSentiment())
                    && profanityRatio >= POSITIVE_PROFANITY_THRESHOLD) {
                post.setProfanityStatus(ProfanityStatus.BLOCKED);

            } else {
                post.setProfanityStatus(ProfanityStatus.ACTIVE);
            }
        }
        // return post to invoked instance
        return post;
    }
}
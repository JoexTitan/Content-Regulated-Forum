package com.springboot.blog.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfanityManagerUtil {

    public static Map<String, Integer> PROFANITY_WORDS = new HashMap<>();

    static {
        try {
            loadProfanityWordsFromJson("src/main/resources/profanity_word_bank.json");
            System.out.println("Loaded -> src/main/resources/profanity_word_bank.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void loadProfanityWordsFromJson(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String[] words = objectMapper.readValue(new File(filePath), String[].class);

        for (String word : words) {
            PROFANITY_WORDS.put(word.toLowerCase(), 0);
        }
    }
    public static boolean isProfanity(String word) {
        return PROFANITY_WORDS.containsKey(word.toLowerCase());
    }
}
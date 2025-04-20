package com.cv_jd_matching.HR.parser;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

/**
 * @author Miruna
 */
public class JobDescriptionParser {

    private static final String PROMPT_JD =
            """
            
            """;

    private static final String GEMINI_MODEL = "gemini-2.0-flash-001";
    private static final String GEMINI_API_KEY = System.getenv("GOOGLE_API_KEY");

    public static Map<String, Object> parseJobDescription(String content) {
        return Map.of();
    }

}
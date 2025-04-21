package com.cv_jd_matching.HR.parser;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JobDescriptionParser {
    //prompt realizat de Muresan Davide
    private static final String PROMPT = """ 
    <task>
    <name>Job Description Analysis</name>
    <description>
    Transform the job description into atomic elements with these rules:
    1. Split ALL entries containing commas/conjunctions into individual items
    2. Group related tasks from the same original statement into task_groups
    3. Preserve OR relationships in qualifications and skills
    4. Maintain parallel structure with CV format
    </description>
    <output-structure>
    {{
        "job_title": "string",
        "company_overview": "string",
        "key_responsibilities": {{
            "task_groups": [
                {{
                    "original_statement": "string",
                    "sub_tasks": [{{"task": "string"}}]
                }}
            ]
        }},
        "required_qualifications": [
            {{
                "group": [{{"requirement": "string"}}],
                "group_type": "OR"
            }} | {{"requirement": "string"}}
        ],
        "preferred_skills": [
            {{
                "group": [{{"skill": "string"}}],
                "group_type": "OR"
            }} | {{"skill": "string"}}
        ],
        "benefits": [{{"benefit": "string"}}]
    }}
    </output-structure>
    <examples>
    <!-- Responsabilități grupate -->
    Input: "design, development, and implementation of SAP applications and solutions"
    Output: {{
        "original_statement": "Assist in the design, development, and implementation of SAP applications and solutions",
        "sub_tasks": [
            {{"task": "Design SAP applications"}},
            {{"task": "Design SAP solutions"}},
            {{"task": "Develop SAP applications"}},
            {{"task": "Develop SAP solutions"}},
            {{"task": "Implement SAP applications"}},
            {{"task": "Implement SAP solutions"}}
        ]
    }}

    <!-- Calificări cu OR -->
    Input: "Bachelor’s degree in Computer Science, Information Technology, or related field"
    Output: {{
        "group": [
            {{"requirement": "Bachelor’s degree in Computer Science"}},
            {{"requirement": "Bachelor’s degree in Information Technology"}},
            {{"requirement": "Bachelor’s degree in related field"}}
        ],
        "group_type": "OR"
    }}

    <!-- Skill-uri cu OR -->
    Input: "SAP Fiori or SAP S/4HANA"
    Output: {{
        "group": [
            {{"skill": "SAP Fiori"}},
            {{"skill": "SAP S/4HANA"}}
        ],
        "group_type": "OR"
    }}
    </examples>
    <content>
    {content}
    </content>
    </task>
    """;

    private static final String GEMINI_MODEL = "gemini-2.0-flash-001";
    private static final String GEMINI_API_KEY = System.getenv("GOOGLE_API_KEY");

    public static Map<String, Object> parseJd(String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("job_title", "");
        data.put("company_overview", "");
        data.put("key_responsibilities", new ArrayList<>());
        data.put("required_qualifications", new ArrayList<>());
        data.put("preferred_skills", new ArrayList<>());
        data.put("benefits", new ArrayList<>());

        if (GEMINI_API_KEY == null || GEMINI_API_KEY.isEmpty()) {
            System.err.println("Gemini API key not found.");
            return basicParse(content);
        }

        try (Client client = Client.builder().apiKey(GEMINI_API_KEY).build()) {

            String encodedContent = SymbolsManipulation.encodeDiacritics(content);

            GenerateContentResponse response =
                    client.models.generateContent(GEMINI_MODEL, PROMPT + "\n\nJob Description:\n" + encodedContent, null);

            String apiResponse = response.text().trim();
            if (apiResponse.startsWith("```json")) {
                apiResponse = apiResponse.substring(7).trim();
            }
            if (apiResponse.endsWith("```")) {
                apiResponse = apiResponse.substring(0, apiResponse.length() - 3).trim();
            }

            apiResponse = SymbolsManipulation.decodeDiacritics(apiResponse);
            Gson gson = new Gson();
            Map parsedData = gson.fromJson(apiResponse, Map.class);

            if (parsedData != null) {
                data.put("job_title", parsedData.getOrDefault("job_title", ""));
                data.put("company_overview", parsedData.getOrDefault("company_overview", ""));
                data.put("key_responsibilities", convertToList(parsedData.get("key_responsibilities")));
                data.put("required_qualifications", convertToList(parsedData.get("required_qualifications")));
                data.put("preferred_skills", convertToList(parsedData.get("preferred_skills")));
                data.put("benefits", convertToList(parsedData.get("benefits")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return basicParse(content);
        }

        return data;
    }

    private static Map<String, Object> basicParse(String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("job_title", extractField(content, "Job Title"));
        data.put("company_overview", extractField(content, "Company Overview"));
        data.put("key_responsibilities", extractList(content, "Key Responsibilities"));
        data.put("required_qualifications", extractList(content, "Required Qualifications"));
        data.put("preferred_skills", extractList(content, "Preferred Skills"));
        data.put("benefits", extractList(content, "Benefits"));
        return data;
    }

    private static String extractField(String content, String title) {
        int start = content.indexOf(title);
        if (start == -1) return "";
        int end = content.indexOf("\n\n", start);
        if (end == -1) end = content.length();
        return content.substring(start + title.length(), end).trim();
    }

    private static List<String> extractList(String content, String title) {
        int start = content.indexOf(title);
        if (start == -1) return new ArrayList<>();
        int end = content.indexOf("\n\n", start);
        if (end == -1) end = content.length();
        String section = content.substring(start + title.length(), end).trim();
        return Arrays.stream(section.split("\n|-"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    private static List<Object> convertToList(Object obj) {
        if (obj == null) return new ArrayList<>();
        if (obj instanceof List) return (List<Object>) obj;
        if (obj instanceof LinkedTreeMap) return List.of(obj);
        if (obj instanceof String) return List.of(obj);
        return new ArrayList<>();
    }

}

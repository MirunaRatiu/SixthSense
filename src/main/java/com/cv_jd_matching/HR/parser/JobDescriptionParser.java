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

    private static final String PROMPT = """
            <task>
                          <name>Advanced and Structured Job Description Parsing (for CV-JD Matching)</name>
                          <description>
                            The goal is to extract structured data from job descriptions to enable intuitive and robust **matching with parsed CVs**.
                        
                            Each section must be **cleanly separated**, each item **clearly listed**, and **free from unnecessary line breaks (`\\n`)** that may interrupt context. If line breaks appear in the middle of a sentence or paragraph, treat them as spaces.
                        
                            If sections contain **bullet points (-, *, •)** or are **numbered**, treat each point as an individual list item. If sections are written in paragraph form, **segment the ideas logically into a list of items**.
                        
                            Preserve the integrity of the original text while adapting it into a structure that aligns with CV parsing for similarity scoring.
                        
                            The following sections will be parsed (if present):
                            - `job_title`: A concise string with the exact job title
                            - `company_overview`: A string (one paragraph)
                            - `key_responsibilities`: A list of strings (each describing one responsibility)
                            - `required_qualifications`: A list of strings (each describing one requirement)
                            - `preferred_skills`: A list of strings (each describing one skill or nice-to-have item)
                            - `benefits`: A list of strings (each describing one benefit)
                            - `others`: A dictionary with any remaining information, grouped by title
                        
                            **Synonyms** for each section should also be considered, for example:
                            - Responsibilities = Duties, Tasks, Expectations
                            - Required Qualifications = Must-haves, Requirements, You Should Have
                            - Preferred Skills = Nice-to-Haves, Preferred Qualifications
                            - Benefits = Perks, Compensation, What We Offer
                        
                            Each list item should:
                            - Be **logically separated** (by bullet, dash, number, or sentence)
                            - Be **cleaned** of line breaks (`\\n`) unless part of markdown formatting
                            - Be **ready for similarity comparison** with parsed CV fields
                        
                            ⚠️ DO NOT combine multiple responsibilities or skills into one long list entry. Instead, split by logical separators like commas, semicolons, or bullet points.
                        
                            ✅ If a list appears with separators but no bullets (e.g., "Java, Python, SQL"), break it into distinct items.
                        
                            Return only valid JSON, matching this example structure:
                        
                            
                            {
                              "job_title": "Junior Tech Lead",
                              "company_overview": "[Company overview as paragraph]",
                              "key_responsibilities": ["Responsibility 1", "Responsibility 2", ...],
                              "required_qualifications": ["Qualification 1", "Qualification 2", ...],
                              "preferred_skills": ["Skill 1", "Skill 2", ...],
                              "benefits": ["Benefit 1", "Benefit 2", ...],
                              
                            }
                            
                        
                            ❗Omit any section if it does not appear explicitly. If a section is present but empty or cannot be parsed properly, return it as an empty list `[]`.
                        
                            The output must be clean, consistent, and **ready to be matched** against CVs parsed with detailed technical skills, education, experience, etc.
                          </description>
                        
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

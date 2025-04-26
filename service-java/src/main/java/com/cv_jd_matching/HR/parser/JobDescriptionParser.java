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
  <name>Atomic Job Description Decomposition for Semantic CV Matching</name>
  <description>
    Analyze the job description enclosed in <content> and decompose it into a fully structured JSON with **maximal atomicity and semantic fidelity**. Focus on **Key Responsibilities**, **Required Qualifications**, and **Preferred Skills**.

    === Atomic Decomposition Logic ===

    🔍 Apply these rules to extract logically complete, atomic statements:

    1. **Nested AND/OR logic**:
       - Support **group nesting**: 
         - `group_type: "AND"` can contain multiple `OR` groups
         - `group_type: "OR"` can contain multiple `AND` groups
       - Nesting MUST reflect the semantic logic of the original sentence.

    2. **Enumerations with "and"/"or" inside complex phrases**:
       → e.g., "using MySQL, MongoDB, or PostgreSQL":
       - Split into **all possible logical combinations** by distributing verbs/actions.
       - Preserve semantic units, e.g.:
         - Design using MySQL
         - Design using MongoDB
         - Implement using PostgreSQL
         etc.

    3. **"and" splits**:
       - Split **EVERY** instance where "and" joins distinct actions or items.
       ✅ Remove "and"
       ✅ Resulting items are grouped with `group_type: "AND"`

    4. **"or" splits**:
       - Split **EVERY** instance where "or" presents alternatives.
       ✅ Remove "or"
       ✅ Resulting items are grouped with `group_type: "OR"`

    5. **Comma-based enumerations**:
       - Treat all comma-separated lists (e.g., "X, Y, and Z") as **AND**, unless context clearly implies OR.
       ✅ Remove commas and conjunctions.
       ✅ group_type: "AND"
       ✅ Same treatment as "and" rules.

    6. **Combination Expansion**:
       - When both AND and OR exist in the same statement, **expand all possible combinations**, reflecting logical structure.
       - Ex: "Design and implement using X, Y, or Z" → generate:
         - Design using X
         - Design using Y
         - Implement using Z
         etc.
       - Then group logically using nested AND/OR.

    === Domain Deduction ===
    Analyze the `company_overview` to deduce the company's main domain or industry (e.g., "FinTech", "E-commerce", "Healthcare SaaS", "Enterprise Software Solutions"). Use keywords and context to infer the primary focus of the company's activities. If the domain is not specific to a vertical industry, use a general term like "Enterprise Software Solutions" or "Technology Services".Store the deduced domain in the "message" field as a string, e.g., "Enterprise Software Solutions industry".

    === Output Structure Requirements ===

    * Each atomic `task`, `requirement`, or `skill` must be:
      - A standalone, grammatically correct phrase
      - Free of "and"/"or" if they caused a split
      - Fully self-contained (contextually reconstructed if needed)
    * Maintain the `original_statement` as provided.
    * No atomic item should end with a dangling period or conjunction.
    * Include the deduced domain in the `domain` field.

    === Output Format ===

    Output **only** the JSON object matching this structure:

    {
      "job_title": "string",
      "company_overview": "string",
      "message": "string",
      "key_responsibilities": [
        {
          "original_statement": "string",
          "group": [
            {
              "group": [
                {"task": "string"},
                ...
              ],
              "group_type": "AND" | "OR"
            },
            ...
          ],
          "group_type": "AND" | "OR"
        }
        |
        {
          "original_statement": "string",
          "task": "string"
        }
      ],
      "required_qualifications": [
        {
          "original_statement": "string",
          "group": [
            {
              "group": [
                {"requirement": "string"},
                ...
              ],
              "group_type": "AND" | "OR"
            },
            ...
          ],
          "group_type": "AND" | "OR"
        }
        |
        {
          "original_statement": "string",
          "requirement": "string"
        }
      ],
      "preferred_skills": [
        {
          "original_statement": "string",
          "group": [
            {
              "group": [
                {"skill": "string"},
                ...
              ],
              "group_type": "AND" | "OR"
            },
            ...
          ],
          "group_type": "AND" | "OR"
        }
        |
        {
          "original_statement": "string",
          "skill": "string"
        }
      ],
      "benefits": [
        {
          "original_statement": "string",
          "benefit": "string"
        }
      ]
    }

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
            System.out.println(apiResponse);
            Gson gson = new Gson();
            Map parsedData = gson.fromJson(apiResponse, Map.class);

            if (parsedData != null) {
                data.put("job_title", parsedData.getOrDefault("job_title", ""));
                data.put("company_overview", parsedData.getOrDefault("company_overview", ""));
                data.put("key_responsibilities", convertToList(parsedData.get("key_responsibilities")));
                data.put("required_qualifications", convertToList(parsedData.get("required_qualifications")));
                data.put("preferred_skills", convertToList(parsedData.get("preferred_skills")));
                data.put("benefits", convertToList(parsedData.get("benefits")));
                data.put("message", parsedData.getOrDefault("message",""));
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
        data.put("message", extractField(content,"message"));
        return data;
    }

    private static String extractField(String content, String title) {
        int start = content.indexOf(title);
        if (start == -1) {
            return "";
        }
        int end = content.indexOf("\n\n", start);
        if (end == -1) {
            end = content.length();
        }
        return content.substring(start + title.length(), end).trim();
    }

    private static List<String> extractList(String content, String title) {
        int start = content.indexOf(title);
        if (start == -1) {
            return new ArrayList<>();
        }
        int end = content.indexOf("\n\n", start);
        if (end == -1) {
            end = content.length();
        }
        String section = content.substring(start + title.length(), end).trim();
        return Arrays.stream(section.split("\n|-"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    private static List<Object> convertToList(Object obj) {
        if (obj == null) {
            return new ArrayList<>();
        }
        if (obj instanceof List) {
            return (List<Object>) obj;
        }
        if (obj instanceof LinkedTreeMap) {
            return List.of(obj);
        }
        if (obj instanceof String) {
            return List.of(obj);
        }
        return new ArrayList<>();
    }

}
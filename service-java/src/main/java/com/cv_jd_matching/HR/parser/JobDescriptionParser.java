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
  <name>Strict CV Parser for SQL-Compatible Text (Zero Hallucination)</name>
  <description>
    Extract ONLY explicitly mentioned information from the following CV content. DO NOT assume or infer anything that is not clearly written. The extracted fields will be used in a SQL table with TEXT columns. Leave fields blank if not present. DO NOT fill in missing values. DO NOT invent job titles, skill levels, technologies, dates, or names.

    Your task is to match the following output schema with exact CV content:

    - name: full name of the candidate
    - technical_skills: list of skills (e.g., Java - Intermediate). Include level ONLY if explicitly stated.
    - foreign_languages: list as "Language - Level" or just "Language"
    - education: each degree including institution, field of study, period (start-end), duration if mentioned
    - certifications: each certification with name, issuer, date, and technologies/tools mentioned (ONLY if clearly stated)
    - project_experience: project title, description, technologies used (if mentioned)
    - work_experience: job, internship, competition, or volunteering experience, with role/title, organization, period, duration, description (bullet-style), and technologies if listed
    - others: everything else grouped logically under original titles (e.g., "Contact Information", "Interests", "Traits")

    === CRITICAL RULES ===

    ⚠️ DO NOT INVENT or add ANY information.
    ⚠️ DO NOT infer job titles, skill levels, or project names.
    ⚠️ If a field is missing in the CV, leave it empty.
    ⚠️ DO NOT summarize, guess, or rewrite. Extract as-is.
    ⚠️ DO NOT use examples or completions from previous templates.

    === Section Detection (Synonyms Allowed) ===

    - Skills: "Technical Skills", "Skills", "Technologies", "Programming Languages", etc.
    - Education: "Education", "Studies", "Training", etc.
    - Certifications: "Certificates", "Courses", "Trainings", etc.
    - Work Experience: "Experience", "Work History", "Volunteering", "Hackathons", etc.
    - Projects: "Projects", "Portfolio", "Achievements"
    - Languages: "Languages", "Foreign Languages", etc.
    - Others: "Contact Info", "Interests", "Traits", etc.

    === Extraction Behavior ===

    - technical_skills: split skills by commas, slashes or line breaks. Include levels only if stated.
    - education: include institution, degree/field, start/end years, and duration if written.
    - certifications: include name, issuer, date, and technologies ONLY if written in the CV.
    - work_experience: extract only what exists. Do not add duration if end date is missing. Keep tech/tools mentioned.
    - project_experience: title, brief description, technologies/tools if mentioned
    - foreign_languages: extract only those written
    - others: keep original section titles like "Contact", "Interests", etc. Normalize emails/phones/social links.

    === Output Format ===

    Return the result in PLAIN TEXT (no JSON). Format example:

    name: Jane Doe
    technical_skills: Python, Java - Intermediate, SQL
    foreign_languages: English - Fluent, German
    education: 
      Bachelor - Computer Science - University of Bucharest - 2019-2022 - Duration: 3 years
    certifications: 
      Oracle Certified Associate - Oracle - 2022 - Technologies: Java, SQL
    work_experience: 
      Backend Developer at Softvision (2023-01 to Present)
      Description: Built REST APIs, maintained microservices
      Technologies: Java, Spring Boot, PostgreSQL
    project_experience:
      Smart Notes App - Mobile app for note-taking - Technologies: Kotlin, Firebase
    others:
      Contact Information:
        Email: jane.doe@email.com, Phone: +40712345678
      Interests:
        Hiking, UI Design, Puzzles

    ⚠ Again: DO NOT INVENT ANYTHING.
    ⚠ Replace all `\\n` with space inside each value.
    ⚠ Return ALL FIELDS even if empty.
    ⚠ Preserve Romanian diacritics exactly as written in the CV.
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
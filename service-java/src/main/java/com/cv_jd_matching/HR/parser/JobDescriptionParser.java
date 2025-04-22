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
            <name>Advanced and Structured Parsing of Job Description</name>
            <description>
            Analyze the provided job description content and extract structured information as intuitively and detailed as possible, returning a JSON object with the defined sections.
            The following sections will be extracted as separate sections and are considered mandatory if present in the job description:
            - Job Title
            - Company Overview
            - Key Responsibilities
            - Required Qualifications
            - Preferred Skills
            - Benefits

            The JSON will include ONLY those fields and sections explicitly mentioned in the job description. If a specific information or section is not present, the corresponding field in the JSON will be omitted or its value will be `null`.
            Sections will be identified using standard titles AND their synonyms.
            Any other section, INCLUDING additional contact information, will be grouped under "others" with the section title as the key and the content as the value.
            The primary goal is to facilitate further processing with NLP techniques for matching with resumes.
            The following sections will be identified and structured to adapt to various formats and styles of job descriptions:
            </description>
            <steps>
                <step>
                    <name>Extracting Job Title</name>
                    <details>
                    Search and extract the job title from the job description. If found, it will be returned as the value for the "job_title" key at the top level of the JSON. If not found, the value of the "job_title" key will be `null` or the key will be omitted.
                    </details>
                </step>
                <step>
                    <name>Extracting Company Overview</name>
                    <details>
                    Identify sections relevant to company information using titles such as "Company Overview", "About Us", "Company Description", etc. Extract the content and return it under the "company_overview" key. If no relevant section is found, the key will be omitted.
                    </details>
                </step>
                <step>
                    <name>Extracting Key Responsibilities</name>
                    <details>
                    Identify sections relevant to responsibilities using titles such as "Key Responsibilities", "Responsibilities", "Duties", etc. Extract each responsibility as a list item and return under the "key_responsibilities" key. If no relevant section is found, the key will be omitted.
                    </details>
                </step>
                <step>
                    <name>Extracting Required Qualifications</name>
                    <details>
                    Identify sections relevant to qualifications using titles such as "Required Qualifications", "Requirements", "Must-Have Skills", etc. Extract each qualification as a list item and return under the "required_qualifications" key. If no relevant section is found, the key will be omitted.
                    </details>
                </step>
                <step>
                    <name>Extracting Preferred Skills</name>
                    <details>
                    Identify sections relevant to preferred skills using titles such as "Preferred Skills", "Nice-to-Have Skills", "Preferred Qualifications", etc. Extract each skill as a list item and return under the "preferred_skills" key. If no relevant section is found, the key will be omitted.
                    </details>
                </step>
                <step>
                    <name>Extracting Benefits</name>
                    <details>
                    Identify sections relevant to benefits using titles such as "Benefits", "Perks", "Compensation", etc. Extract each benefit as a list item and return under the "benefits" key. If no relevant section is found, the key will be omitted.
                    </details>
                </step>
                <step>
                    <name>Handling Other Sections</name>
                    <details>
                    Any other section identified that does not match the main categories (and their synonyms), including additional contact information, will be included under the "others" key. Extract the section title and content ONLY if such sections exist. If no such sections exist, the "others" key will be omitted.
                    </details>
                </step>
            </steps>
            <output-structure>
                <name>Output Format</name>
                <details>
                Return a single JSON object that includes ONLY the keys for the information and sections present in the job description (identified by standard titles OR synonyms). The following sections will be at the top level (if found):
                - "job_title": string (present only if found, mandatory)
                - "company_overview": string (present only if found, mandatory)
                - "key_responsibilities": list of strings (present only if found, mandatory)
                - "required_qualifications": list of strings (present only if found, mandatory)
                - "preferred_skills": list of strings (present only if found, mandatory)
                - "benefits": list of strings (present only if found, mandatory)
                - "others": object (present only if there are other sections or additional contact information)
                </details>
            </output-structure>
            <considerations>
                - Extract ONLY information explicitly present in the job description.
                - If an entire section (identified by standard title OR synonym) is missing, omit the corresponding key in the JSON.
                - If an individual field is missing (e.g., a specific benefit), do not include that field.
                - Include the job title, company overview, key responsibilities, required qualifications, preferred skills, and benefits at the top level of the JSON (if found).
                - Include any other sections or additional contact information under the "others" key.
                - Prioritize accuracy and exact reflection of the job description's content.
                - If a section in the job description contains information relevant to multiple mandatory sections, distribute the content contextually into the appropriate sections based on associated keywords.
                - If, after distribution, a section cannot extract any information, it will be included in the JSON with an empty list `[]` to signal the absence of content but the logical presence of the section.
            </considerations>
            <content>
            {job_description_content}
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
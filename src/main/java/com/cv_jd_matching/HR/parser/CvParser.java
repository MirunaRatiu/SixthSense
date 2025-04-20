package com.cv_jd_matching.HR.parser;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;


public class CvParser {

    private static final String PROMPT =
    """
    <global_instructions>
                    - When extracting any text from the CV, **replace all occurrences of the newline character (`\\n`) with a space.**
                    - When extracting text that should represent proper nouns, titles, or the beginning of words, **prioritize interpreting the simple vertical character as the uppercase letter "I"**, if the linguistic context suggests this.
                    - When the simple vertical character appears between elements that seem to be from a list or in places where a separator would be logical, **interpret it as the vertical bar "|"**.
                    - **Ensure all Romanian diacritics (ă, â, î, ș, ț) are preserved exactly as they appear in the text**. This is crucial, especially for names and Romanian-specific content.
                  </global_instructions>
    <task>
                  <name>Advanced and Structured CV Parsing (Exclusive Extraction with Synonyms - Mandatory Main Sections with Scores, Separate Certifications, and Dual Parsing)</name>
                  <description>
                    **The primary goal is to analyze the text content of a Curriculum Vitae and extract structured information in a detailed and intuitive JSON format.**
                    The following sections will be extracted as separate sections and are considered mandatory if present in the CV:
                    - Full Name
                    - Technical Skills ("Technical Skills" or synonyms) - the score will be included only if explicitly mentioned.
                    - Education ("Education" or synonyms) - only information related to degrees (e.g., Bachelor, Master) will be included.
                    - Certifications ("Certifications", "Certificări", "Certificate", "Qualifications" or synonyms) - only information related to certificates will be included.
                    - Work Experience ("Work Experience" or synonyms) - competitions will also be included.
                    - Projects Experience ("Projects Experience" or synonyms)
                    ONLY those fields and sections that are explicitly mentioned in the CV will be included in the JSON. If certain information or a section is not present, the corresponding field in the JSON will be omitted or its value will be null.
                    Sections will be identified using standard titles AND their synonyms. **Maximum importance will be given to the COMPLETE and EXACT preservation of all special characters and diacritics specific to the Romanian language (ă, ț, ș, â, î) in the extracted text and in the returned JSON structure.**
                    The "Languages" section (and synonyms) will be separated. Any other section, INCLUDING the rest of the contact information (email, phone, social media), will be grouped under "others" with the section title as the key and the content as the value.
                    If a section in the CV contains information relevant to two parsing sections (e.g., "Education & Certifications", "Qualifications"), the content will be distributed accordingly in the "education" and "certifications" sections of the JSON.
                    The main objective is to facilitate further processing with NLP techniques for matching with job descriptions.
                    Sections will be identified using standard titles AND their synonyms. **Maximum importance will be given to the COMPLETE and EXACT preservation of all special characters and diacritics specific to the Romanian language (ă, ț, ș, â, î) in the extracted text and in the returned JSON structure. Every diacritic in the CV must be reflected IDENTICALLY in the JSON.**
                    IMPORTANT: The analysis will be performed on a CV containing text in both English and Romanian. **Give MAXIMUM importance to the correct preservation of all special characters and diacritics specific to the Romanian language (ă, ț, ș, â, î) in ALL sections, ESPECIALLY in the name and any Romanian content.**
                    - Do NOT add a period (`.`) at the end of any string (unless it's part of a sentence extracted from the CV).
                    **Additional instructions for text interpretation:**
                                - Pay close attention to simple vertical characters. If such a character appears in the context of a proper noun, a title, or the beginning of a word, it is much more likely to be the uppercase letter "I" and not the vertical bar "|".
                                - The vertical bar "|" is often used as a separator in lists or tables. If the vertical character appears between list items or in places where a separator would be logical, it is more likely to be "|".
                    The following sections will be identified and structured, adapting to various CV formats and styles:
                    **Do not split ideas based solely on line breaks.**
                  </description>
                  <steps>
                    <step>
                      <name>Identifying and Extracting the Name</name>
                      <details>
                        Search for and extract the full name of the person from the CV, **ignoring titles (e.g., Mr., Ms., Eng.) or initials that appear to be abbreviations of other words.** Identify the person's first and last name. If found, it will be returned as the value for the "name" key at the top level of the JSON. If not found, the value of the "name" key will be null or the key will be omitted. **Ensure that you extract only the person's name, without including abbreviations or initials that are not part of the actual name (except when the initial is an integral part of the first name, as in "M. Eminescu"). Pay attention to the context to distinguish between an initial of a first name and an abbreviation of a title or other word. Be mindful of compound names (e.g., "Jean-Paul"). **EXTREME ATTENTION SHOULD BE PAID TO PRESERVING ROMANIAN DIACRITICS (ă, ț, ș, â, î) EXACTLY AS THEY APPEAR IN THE NAME.**
                      </details>
                    </step>
                    <step>
                      <name>Extracting Technical Skills with Score</name>
                      <details>
                        Identify the relevant sections for technical skills using titles such as "Technical Skills", "Skills", "Technical", "Proggramming Languages". Analyze the content of these sections to extract each individual technical skill, taking into account the following rules:
            
                        **Rules for Separating Multiple Skills:**
                        If a line or a list item contains multiple skills separated by a comma (,), **treat each comma-delimited segment as a distinct skill.** Remove any unnecessary whitespace from the beginning and end of each resulting skill.
            
                         **General Normalization Rule for "language":**
                                        - After extracting a skill, **replace all occurrences of the word " language" (case-insensitive, surrounded by word boundaries or at the beginning/end of the string) with an empty string.**
                                        - After the replacement, **trim any leading or trailing whitespace** that might have been introduced.
                                        - For other skills that do not contain " language", save them as extracted.
                        **Rules for Associating Expertise Level:**
                        When extracting a skill, also look for an indicator of the expertise level associated with it, respecting the following order of priority:
            
                        1.  **Immediate Specific Level:** If a level (score or proficiency) is mentioned **directly after** a single skill or after a skill in a comma-separated list (e.g., "Java - Expert", "Python (Advanced)", "C++, Score: 8/10"), associate that level with the respective skill.
            
                        2.  **Unique Level for Multiple Skills (Contextual):** If a level is mentioned **only once after a list of comma-separated skills** (e.g., "JavaScript, HTML, CSS - Intermediate"), **apply that level to each skill in the list**, **ONLY IF** the context clearly indicates that the level is general for all. Pay attention to formulations that suggest a collective level.
            
                        3.  **Specific Level within the List:** If the level is specified individually for each skill in a list (e.g., "Java (Expert), Spring Boot (Advanced), SQL (Medium)"), extract and associate the corresponding level with each skill.
            
                        4.  **Ambiguity or Lack of Clarity:** If the level association is ambiguous, uncertain, or the level is mentioned before the list of skills without a clear connection, **include only the skills without assigning a level** for those cases.
            
                        **Format of the "level" Field:**
                        - If a *score* is identified (e.g., "7", "90%"), include the `"level"` field with the value `"[Value]"`.
                        - If a *proficiency level* is identified (e.g., "Expert", "Advanced", "Intermediate", "Beginner", "Fluent", "Professional"), include the `"level"` field with the proficiency value.
                        - If both (score and proficiency) are present for a skill, include the `"level"` field with both pieces of information (e.g., `"Score: 8/10, Advanced"`).
                        - If no level is identified for a skill, include only the `"skill"` field with the skill name.
            
                        **Desired Output Structure:**
                        "technical_skills": [
                          { "skill": "Java(OOP)", "level": "Expert" },
                          { "skill": "Spring Boot", "level": "Expert" },
                          { "skill": "Python", "level": "Advanced" },
                          { "skill": "Django" },
                          { "skill": "SQL", "level": "Medium" },
                          { "skill": "PostgreSQL", "level": "Medium" },
                          { "skill": "AWS" },
                          { "skill": "Docker" },
                          { "skill": "Node.js" },
                          { "skill": "REST APIs" }
                          // ... other skills with or without level
                        ]
                        Return this structure under the "technical_skills" key ONLY if technical skills are found. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**
                      </details>
                    </step>
                    <step>
                      <name>Extracting Education (Degrees Only) and Certifications with Dual Parsing</name>
                      <details>
                        Identify the relevant sections for education and certifications using titles such as "Education", "Educație", "Studii", "Formare", "Certifications", "Certificări", "Certificate", "Qualifications", "Education and Qualifications".
            
                        *For Education (Degrees):*
                        Extract details ONLY for information related to degrees (e.g., Bachelor, Master, information from universities, colleges, high schools, or equivalents). If no degree information is found, the "education" key will be omitted.
                        <instruction>In the "Education" section, for high schools, only the name of the high school will be used, without including "Colegiul Național" unless it is an integral part of the high school's name as it appears explicitly in the CV.</instruction>
                        For each education element:
                          - **When extracting the institution name, if it is enclosed in double quotes ("..."), remove the quotes and any adjacent leading or trailing whitespace.**                          - Extract "institution", "degree", "field_of_study", and "notes" if present.
                          - **Extract the "period". If the period includes a start and end date, save them in "start_date" and "end_date" within the "period" sub-object (format "YYYY-MM").**
                          - **Search the text associated with the education element for explicit mentions of program duration using terms like "program duration", "durata studiilor", "length of program" or equivalents. If you find a duration expressed in years and/or months (e.g., "4 ani", "4 years", "4 luni", "4 months", "4 ani și 3 luni", "4 years and 3 months"), save this duration exactly as mentioned in the `"duration"` field within the "period" sub-object.**
                           **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**
                          - If the duration is 3 or 4 years and no degree is explicitly mentioned, default the "degree" field to "Bachelor".
            
                        *For Certifications:*
                        Extract details ONLY for information related to certifications, specific modules, specialization courses (e.g., those offered by academies, companies, organizations). Ensure to clearly distinguish these from academic degrees. If no certificate information is found, the "certifications" key will be omitted. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**
            
                        If a section combines both types of information (e.g., "Education and Qualifications"), analyze each individual element and distribute it to the corresponding section ("education" or "certifications") based on the type of institution/program mentioned.
                        **Look for and extract any mentioned technologies, tools, or methodologies associated with the certification and include them in a "technologies" list .**
                        *Education Structure:*
                        "education": [
                          {
                            "institution": "Institution Name (University, College, High School)",
                            "degree": "Degree Title",
                            "field_of_study": "Field of Study",
                            "period": {
                              "start_date": "YYYY-MM" ,
                              "end_date": "YYYY-MM" ,
                              "duration": "Explicitly mentioned duration (e.g., 4 years, 4 months, 4 years and 3 months)"
                            },
                            "notes": "Special Mentions"
                          },
                          { ... }
                        ]
            
                        
                        *Certifications Structure:*
                        "certifications": [
                          { "name": "Certification/Module Name", "institution": "Issuing Institution (Academy, Company)", "date": "YYYY-MM","technologies": ["Technology A", "Technology B", ...] },
                          { ... }
                        ]
            
                        Return the education structure under the "education" key ONLY if degree information is found.
                        Return the certifications structure under the "certifications" key ONLY if certifications are found.
                      </details>
                    </step>
                    <step>
                      <name>Extracting Work Experience (Including Competitions) with Duration</name>
                      <details>
                        Identify the relevant sections for work experience using titles such as "Work Experience", "Experience", "Professional Experience", "Istoric Profesional", "Hackathons", "Contests". Extract the details for each job AND for the mentioned competitions. If no relevant section is found, the "work_experience" key will be omitted.
                        For the "period" field:
                        - **Extract the "period". If the period includes a start and end date, save them in "start_date" and "end_date" within the "period" sub-object (format "YYYY-MM").**
                        - **If a duration is explicitly mentioned (e.g., "2 ani și 3 luni", "5 months", "1 year"), save this duration exactly as mentioned in the `"duration"` field within the "period" sub-object.**
                        - If both "start_date" and "end_date" (including the year) are present, calculate the duration in years, months, and days and include a `"duration_calculated"` field with this value. Keep the "start_date" and "end_date" fields as well.
                        - If either date is missing or the duration cannot be calculated, include only the present "start_date" and/or "end_date" fields. For competitions, use the "date" field if present. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**
                        **For job entries, look for and extract any mentioned technologies, tools, or methodologies used in that role and include them in a "technologies" list.**
                        Structure:
                        "work_experience": [
                          {
                            "type": "job",
                            "title": "Job Title",
                            "company": "Company Name",
                            "period": {
                              "start_date": "YYYY-MM" ,
                              "end_date": "YYYY-MM" ,
                              "duration": "Explicitly mentioned duration (e.g., 2 ani și 3 luni, 5 months, 1 year)" ,
                              "duration_calculated": "X years, Y months, Z days" // calculated if both start and end dates are available
                            },
                            "description": ["Point 1", "Point 2", "..."],
                            "technologies": ["Technology A", "Technology B", ...]
                          },
                          {
                            "type": "competition",
                            "name": "Competition Name",
                            "organization": "Organizer" ,
                            "period": { "date": "YYYY-MM" } ,
                            "description": ["Competition Details"]
                          },
                          { ... }
                        ]
                        Return this structure under the "work_experience" key ONLY if work experience or competition information is found.
                      </details>
                    </step>
                    <step>
                      <name>Extracting Project Experience</name>
                      <details>
                        Identify the relevant sections for project experience using titles such as "Projects Experience", "Projects", "Proiecte", "Experiență Proiecte". Extract the details for each project ONLY if mentioned. If no relevant section is found, the "projects_experience" key will be omitted. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**
                       **Instructions for description:** When extracting the content for the "description" field, identify and capture each complete idea as a single string. **Do not split ideas based solely on line breaks.** If an idea spans multiple lines in the CV, join those lines together into one continuous string. Ensure that any newline characters (`\n`) present in the original multi-line text are **not included** in the final string. You may need to replace or remove these newline characters during the joining process.
                       **For job entries,,ook for and extract any mentioned technologies, tools, or methodologies used in that role and include them in a "technologies" list.**
                        Structure:
                        "project_experience": [
                          {
                            "title": "Project Title",
                            "description": "Short Description" (optional),
                            "technologies": ["Technology 1", "Technology 2", "..."]
                          },
                          { ... }
                        ]
                        Return this structure under the "project_experience" key ONLY if project experience information is found. **Ensure that 'technologies' is returned as a list of strings.**
                      </details>
                    </step>
                    <step>
                      <name>Extracting Foreign Languages</name>
                      <details>
                        Identify the relevant sections for foreign languages using titles such as "Languages", "Limbi Străine", "Foreign Languages", "Limbi". Extract the languages and levels ONLY if mentioned. If no relevant section is found, the "languages" key will be omitted. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**
                        Structure:
                        "foreign_languages": [
                          { "language": "Foreign Language", "proficiency": "Level" } (level is optional),
                          { ... }
                        ]
                        Return this structure under the "foreign_languages" key ONLY if foreign languages are found.
                      </details>
                    </step>
                    <step>
                      <details>
                        Any other identified section that does not fit the main categories (and their synonyms), INCLUDING the rest of the contact information, will be included under the "others" key. Extract the title of each sub-section and the relevant content associated with it to facilitate matching with job descriptions. If no other sections exist, the "others" key will be omitted.
            
                        **Detailed and Prioritized Instructions for Parsing the "others" Section:**
            
                        For each section identified under "others":
            
                        1.  **Identify Explicit Titles:** Look for lines or phrases that are formatted as titles (e.g., bold, underlined, with a larger font size, separated by blank spaces). These indicate the beginning of a new sub-section (e.g., "Contact", "Interests", "Volunteer experience").
            
                        2.  **Extract Content Associated with Titles:** All text that follows an explicit title and up to the next explicit title (or the end of the "others" section) belongs to that sub-section.
            
                        3.  **Structure Content by Titles:** Return a JSON object where each **key is the explicit title identified**, and the value is a list of strings representing each relevant line of content from that sub-section. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THESE SUB-SECTIONS PRESERVES DIACRITICS.**
            
                        **Handling Information Without Explicit Titles (Logical Grouping):**
            
                        If you find information that is not preceded by an explicit title, try to group it logically based on keywords and format:
            
                        - **Contact Information:** Lines containing keywords such as "Email", "Website", "LinkedIn", "Phone Number", "Address" should be grouped under the title "Contact Information".**ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SUB-SECTION PRESERVES DIACRITICS.**
                        - **Interests:** Lines containing keywords such as "Interests", "Pasiuni" should be grouped under the title "Interests" as a list of strings. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SUB-SECTION PRESERVES DIACRITICS.**
                        - **Personal Characteristics/Soft Skills:** Lines with adjectives or short descriptive phrases should be grouped under titles such as "Characteristics" or "Soft Skills" as a list of strings. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SUB-SECTION PRESERVES DIACRITICS.**
                        - **Volunteer experience:** Lines containing "Volunteer", "Voluntariat", "Asociație" should be grouped under "Volunteer experience" as a list of strings. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SUB-SECTION PRESERVES DIACRITICS.**
                        - **Check if the extracted key is "Phone no" or "Phone No" (case-insensitive). If it is, use "Phone number" as the key in the "others" object.**                        **Desired Output Structure (Under the "others" Key):**
            
                        ```json
                        "others": {
                          "Contact Information": [
                            { "Email": "mirunaratiu03@gmail.com" },
                                                    { "Address": "Cluj-Napoca" },
                                                    { "Website": "[https://github.com/MirunaRatiu](https://github.com/MirunaRatiu)" },
                                                    { "LinkedIn": "[www.linkedin.com/in/miruna-ratiu](https://www.linkedin.com/in/miruna-ratiu)" },
                                                    { "Phone number": "+40735506666" }
                                                  ],
                                                  "Interests": [
                                                    "AI",
                                                    "Robotics",
                                                    "Computer Science",
                                                    "Basketball",
                                                    "Swimming",
                                                    "Traveling",
                                                    "Photography",
                                                    "Strategic games"
                                                  ],
                                                  "Characteristics": [
                                                    "Curious",
                                                    "Adaptable",
                                                    "Collaborative",
                                                    "Open-minded",
                                                    "Determined",
                                                    "Solution-Oriented"
                                                  ],
                                                  "Volunteer experience": [
                                                    "Member in Technical University Students' Organization (OSUT)"
                                                  ]
                                                  // ... other identified sections
                                                }
                                                ```
                                              </details>
                                            </step>
                                          </steps>
                                          <output-structure>
                                            <name>Output Format</name>
                                            <details>
                                              Return a single JSON object that will include ONLY the keys for the information and sections present in the CV (identified by standard titles OR synonyms). The following sections will be at the top level (if found):
                                              - "name": string (present only if found, mandatory)
                                              - "technical_skills": list of objects (present only if technical skills or synonyms exist, mandatory)
                                              - "education": list of objects (present only if degree information or synonyms exist, mandatory)
                                              - "certifications": list of objects (present only if certifications or synonyms exist, mandatory)
                                              - "work_experience": list of objects (present only if work experience or competitions exist, mandatory)
                                              - "project_experience": list of objects (present only if project experience or synonyms exist, mandatory)
                                              - "foreign_languages": list of objects (present only if foreign languages or synonyms exist)
                                              - "others": list of objects (present only if other sections OR additional contact information exist)
                                              - **Parse the provided text, paying SPECIAL attention to the correctness of diacritics. Ensure that the text is processed correctly, preserving ALL original diacritics in the final text, especially for Romanian words and the name.**
                                              - Return only valid and complete JSON. All lists (arrays) must be closed correctly and use double quotes consistently.
                                            </details>
                                          </output-structure>
                                        </task>
"""
    ;

    private static final String GEMINI_MODEL = "gemini-2.0-flash-001";
    private static final String GEMINI_API_KEY = System.getenv("GOOGLE_API_KEY");


    // Method to parse CV and integrate Gemini API call
    public static Map<String, Object> parseCv(String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "");
        data.put("technical_skills", new ArrayList<String>());
        data.put("foreign_languages", new ArrayList<String>());
        data.put("education", new ArrayList<String>());
        data.put("certifications", new ArrayList<String>());
        data.put("project_experience", new ArrayList<String>());
        data.put("work_experience", new ArrayList<String>());
        List<Map<String, Object>> others = new ArrayList<>();
        data.put("others", others);

        if (GEMINI_API_KEY == null || GEMINI_API_KEY.isEmpty()) {
            System.err.println("Gemini API key not found in environment variables.");
            return basicParse(content);
        }

        try(Client client = Client.builder().apiKey(GEMINI_API_KEY).build()) {

            String encodedContent = encodeDiacritics(replaceLongDashWithShortDash(content));

            GenerateContentResponse response =
                    client.models.generateContent(GEMINI_MODEL, PROMPT + "\n\nCV Content:\n" + encodedContent, null);

            System.out.println("Response:" + response);
            String apiResponse = response.text();

            if (apiResponse.startsWith("```json")) {
                apiResponse = apiResponse.substring(7).trim();
            }
            if (apiResponse.endsWith("```")) {
                apiResponse = apiResponse.substring(0, apiResponse.length() - 3).trim();
            }
            System.out.println("API Response: " + apiResponse);

            apiResponse = decodeDiacritics(apiResponse);

            Gson gson = new Gson();
            Map parsedData = null;

            if (apiResponse.contains("\\u")) {
                parsedData = gson.fromJson(apiResponse, Map.class);
                if (parsedData == null) {
                    System.err.println("Eroare la reparsarea JSON cu suport Unicode.");
                    System.err.println("Răspuns brut: " + apiResponse);
                    return basicParse(content);
                }
            } else {
                parsedData = gson.fromJson(apiResponse, Map.class);
                if (parsedData == null) {
                    System.err.println("Eroare la parsarea JSON.");
                    System.err.println("Răspuns brut: " + apiResponse);
                    return basicParse(content);
                }
            }

            if (parsedData != null) {
                data.put("name", parsedData.get("name"));
                data.put("technical_skills", convertToList(parsedData.get("technical_skills")));
                data.put("foreign_languages", convertToList(parsedData.get("foreign_languages")));
                data.put("education", convertToList(parsedData.get("education")));
                data.put("certifications", convertToList(parsedData.get("certifications")));
                data.put("project_experience", convertToList(parsedData.get("project_experience")));
                data.put("work_experience", convertToList(parsedData.get("work_experience")));
                data.put("others", convertToList(parsedData.get("others")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return basicParse(content);
        }

        return data;
    }


    public static String encodeDiacritics(String text) {
        if (text == null) return null;

        text = text.replace("ă", "\\u0103");
        text = text.replace("î", "\\u00EE");
        text = text.replace("ț", "\\u021B");
        text = text.replace("ș", "\\u0219");
        text = text.replace("â", "\\u00E2");

        text = text.replace("Ă", "\\u0102");
        text = text.replace("Î", "\\u00CE");
        text = text.replace("Ț", "\\u021A");
        text = text.replace("Ș", "\\u0218");
        text = text.replace("Â", "\\u00C2");

        text = text.replace("\"", "\\u0022");

//        text = text.replace("\\", "\\u005C");
//        text = text.replace("|", "\\u007C");
//        text = text.replace("-", "\\u002D");

        return text;
    }

    public static String decodeDiacritics(String text) {
        if (text == null) return null;

        text = text.replace("\\u0103", "ă");
        text = text.replace("\\u00EE", "î");
        text = text.replace("\\u021B", "ț");
        text = text.replace("\\u0219", "ș");
        text = text.replace("\\u00E2", "â");


        text = text.replace("\\u0102", "Ă");
        text = text.replace("\\u00CE", "Î");
        text = text.replace("\\u021A", "Ț");
        text = text.replace("\\u0218", "Ș");
        text = text.replace("\\u00C2", "Â");

        text = text.replace("\\u0022", "\"");
//        text = text.replace("\\u005C", "\\");
//        text = text.replace("\\u007C", "|");
//        text = text.replace("\\u002D", "-");

        return text;
    }

    public static String replaceLongDashWithShortDash(String text) {
        return text.replace("–", "-");
    }

    public static List<Object> convertToList(Object obj) {
    if (obj == null) {
        return new ArrayList<>();
    }

    if (obj instanceof List) {
        return (List<Object>) obj;
    } else if (obj instanceof LinkedTreeMap) {
        List<Object> list = new ArrayList<>();
        list.add(obj);
        return list;
    } else if (obj instanceof String) {
        List<Object> list = new ArrayList<>();
        list.add(obj);
        return list;
    }

    return new ArrayList<>();
}


    private static Map<String, Object> basicParse(String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", extractName(content));
        data.put("technical_skills", extractSkills(content, "Technical Skills"));
        data.put("foreign_languages", extractLanguages(content, "Foreign Languages"));
        data.put("education", extractEducation(content, "Education"));
        data.put("certifications", extractCertifications(content, "Certifications"));
        data.put("project_experience", extractProjects(content, "Project Experience"));
        data.put("work_experience", extractWorkExperience(content, "Work Experience"));
        data.put("others", extractOthers(content, new String[]{"Technical Skills", "Foreign Languages", "Education", "Certifications", "Project Experience"}));
        return data;
    }

    private static String extractName(String content) {
        Pattern namePattern = Pattern.compile("([A-ZĂÂÎȘȚ][a-zăâîșț]+\\s){1,2}[A-ZĂÂÎȘȚ][a-zăâîșț]+");
        Matcher nameMatcher = namePattern.matcher(content.substring(0, Math.min(500, content.length())));
        if (nameMatcher.find()) {
            return nameMatcher.group(1).trim();
        }
        return "";
    }

    private static List<String> extractSkills(String content, String sectionTitle) {
        return extractList(content, sectionTitle);
    }

    private static List<String> extractLanguages(String content, String sectionTitle) {
        return extractList(content, sectionTitle);
    }

    private static List<String> extractEducation(String content, String sectionTitle) {
        return extractList(content, sectionTitle);
    }

    private static List<String> extractCertifications(String content, String sectionTitle) {
        return extractList(content, sectionTitle);
    }

    private static List<String> extractProjects(String content, String sectionTitle) {
        List<String> projects = new ArrayList<>();
        int startIndex = content.indexOf(sectionTitle);
        if (startIndex != -1) {
            int endIndex = content.indexOf("\n\n", startIndex + sectionTitle.length());
            if (endIndex == -1) endIndex = content.length();
            String sectionContent = content.substring(startIndex + sectionTitle.length(), endIndex).trim();
            projects.addAll(Arrays.asList(sectionContent.split("\n")));
        }
        return projects.stream().map(String::trim).collect(Collectors.toList());
    }

    private static List<String> extractWorkExperience(String content, String sectionTitle) {
        List<String> workExperience = new ArrayList<>();
        int startIndex = content.indexOf(sectionTitle);
        if (startIndex != -1) {
            int endIndex = content.indexOf("\n\n", startIndex + sectionTitle.length());
            if (endIndex == -1) endIndex = content.length();
            String sectionContent = content.substring(startIndex + sectionTitle.length(), endIndex).trim();
            workExperience.addAll(Arrays.asList(sectionContent.split("\n")));
        }
        return workExperience.stream().map(String::trim).collect(Collectors.toList());
    }

    private static List<Map<String, Object>> extractOthers(String content, String[] excludedSections) {
        List<Map<String, Object>> others = new ArrayList<>();
        List<String> excluded = Arrays.asList(excludedSections);
        String[] lines = content.split("\n");
        String currentKey = null;
        StringBuilder currentValue = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            boolean isSectionTitle = false;
            for (String section : excluded) {
                if (line.startsWith(section)) {
                    isSectionTitle = true;
                    if (currentKey != null) {
                        others.add(Map.of(currentKey, currentValue.toString().trim()));
                        currentKey = null;
                        currentValue.setLength(0);
                    }
                    break;
                }
            }

            if (!isSectionTitle) {
                Pattern keyValuePair = Pattern.compile("^([^:]+):\\s*(.+)$");
                Matcher matcher = keyValuePair.matcher(line);
                if (matcher.matches()) {
                    if (currentKey != null) {
                        others.add(Map.of(currentKey, currentValue.toString().trim()));
                        currentValue.setLength(0);
                    }
                    currentKey = matcher.group(1).trim();
                    currentValue.append(matcher.group(2).trim());
                } else if (currentKey != null) {
                                        currentValue.append("\\n").append(line);
                    } else {
                    // If no key is identified and it's not a section title, we consider it as a potential key
                    // or append to the last 'others' entry if it's a continuation
                    Pattern potentialKey = Pattern.compile("^([A-ZĂÂÎȘȚ][a-zăâîșț]\\s]+)$");
                    Matcher keyMatcher = potentialKey.matcher(line);
                    if (keyMatcher.matches() && others.isEmpty()) {
                        currentKey = line;
                    } else if (!others.isEmpty()) {
                        Map<String, Object> lastEntry = others.get(others.size() - 1);
                        String existingKey = lastEntry.keySet().iterator().next();
                        lastEntry.put(existingKey, lastEntry.get(existingKey) + "\n" + line);
                        } else {
                        others.add(Map.of("unknown", line));
                        }
                    }
            }
        }
        if (currentKey != null) {
            others.add(Map.of(currentKey, currentValue.toString().trim()));
        }
        return others;
    }

    private static List<String> extractList(String content, String sectionTitle) {
        List<String> items = new ArrayList<>();
        int startIndex = content.indexOf(sectionTitle);
        if (startIndex != -1) {
            int endIndex = content.indexOf("\n\n", startIndex + sectionTitle.length());
            if (endIndex == -1) endIndex = content.length();
            String sectionContent = content.substring(startIndex + sectionTitle.length(), endIndex).trim();
            items.addAll(Arrays.asList(sectionContent.split(",")));
            if (items.isEmpty()) {
                items.addAll(Arrays.asList(sectionContent.split("\n")));
            }
        }
        return items.stream().map(String::trim).collect(Collectors.toList());
    }





}

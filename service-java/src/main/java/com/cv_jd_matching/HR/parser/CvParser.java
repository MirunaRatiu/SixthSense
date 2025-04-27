package com.cv_jd_matching.HR.parser;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang3.StringEscapeUtils;


public class CvParser {

    private static final String PROMPT =
            """
            <global_instructions>
                            # --- General Text Processing ---
                            - When extracting any text from the CV, **replace all occurrences of the newline character (`\\n`) with a space,** except when joining multi-line descriptions (see step details).
                            - When extracting text that should represent proper nouns, titles, or the beginning of words, **prioritize interpreting the simple vertical character as the uppercase letter "I"**, if the linguistic context suggests this (e.g., "I have experience...").
                            - When the simple vertical character appears between elements that seem to be from a list or in places where a separator would be logical, **interpret it as the vertical bar "|"** (e.g., "Java | Python").
                            - **Ensure all Romanian diacritics (ă, â, î, ș, ț) are preserved exactly as they appear in the text**. This is crucial, especially for names and Romanian-specific content. This is a NON-NEGOTIABLE requirement.
                            - Everything you extract must be semantically meaningful and contextually valid. Do not extract fragments or phrases that do not make sense on their own or are clearly cut off mid-thought.
                            - Make sure every extracted piece of information makes sense in context, is complete, and linguistically coherent.
                            - Use common sense and contextual understanding to ensure that all data extracted is relevant, properly structured, and logically belongs in the identified section.

                            # --- OCR Correction & Normalization ---
                            - Apply light correction to terms that clearly appear to be common OCR mistakes and contextually resemble standard keywords (e.g., "Cornputer Sciance" → "Computer Science", "G201"(in date context) → "2011") **only if the correction is obvious based on context**. Never invent or hallucinate words.
                            - Normalize visually similar characters based on context (e.g., interpret "I" vs. "l", "B" vs. "8", "O" vs "0") using linguistic clues from surrounding content.
                            - Normalize common OCR issues in descriptive text (e.g., "| bring" → "I bring", "Wy goal ls 1o" → "My goal is to", "enhancing" vs "enhencng", "experiance" -> "experience").
                            - For text originating from scanned images, normalize by reducing multiple spaces to single spaces, and remove non-ASCII characters that are not Romanian diacritics or standard punctuation marks.
                            - Correct common OCR typos for known academic fields and technology terms (e.g., "Softwere Enginering" -> "Software Engineering") if they match standard terminology and the correction is contextually obvious.

                            # === START DATE MODIFICATION (Global Rule) ===
                            - **Strict Date Normalization (Apply Before Structuring Periods):**
                                *   **Identify:** Locate date ranges or single dates associated with education, work, certifications in the text.
                                *   **Extract Components:** Identify year and (if available) month for each date point (start, end, or single date).
                                *   **Recognize Input Formats:** Accept common formats like "MM/YYYY", "MM.YYYY", "MM-YYYY", "YYYY-MM", "YYYY", "Month YYYY" (e.g., "Ianuarie 2020", "Mar 2021"). Handle mixed separators (`/`, `.`, `-`). Recognize Romanian month names (e.g., Ianuarie, Februarie, Martie, Aprilie, Mai, Iunie, Iulie, August, Septembrie, Octombrie, Noiembrie, Decembrie) and common English abbreviations (Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec).
                                *   **Target Output Format:** Convert the extracted components STRICTLY into `YYYY-MM` format if the month is identified, or `YYYY` format if only the year is identified. Month numbers must have leading zeros (e.g., `01`, `05`, `11`).
                                *   **Keywords for Ongoing:** If keywords like "Present", "Ongoing", "În curs", "Currently", "Până în prezent", "Actual", or similar contextual clues indicate an ongoing entry, use the exact string `"Present"` as the value for the `end_date`.
                                *   **Accuracy & Error Prevention:** Ensure extracted dates correspond accurately to the correct CV entry. The final date strings in the JSON MUST be clean, valid, and contain ONLY digits and hyphens (for `YYYY-MM` format), OR just digits (for `YYYY` format), OR the exact word `"Present"`. Avoid including any residual characters, separators, or formatting issues from the original text in the final JSON date value.
                                *   **Examples (Input Text Fragment -> JSON Output Fields):**
                                    *   "01/2011 - 01/2013" → `start_date: "2011-01"`, `end_date: "2013-01"`
                                    *   "01.2011 - 01.2013" → `start_date: "2011-01"`, `end_date: "2013-01"`
                                    *   "2011 - 2013" → `start_date: "2011"`, `end_date: "2013"`
                                    *   "2007 - Present" → `start_date: "2007"`, `end_date: "Present"`
                                    *   "Martie 2020 - August 2022" -> `start_date: "2020-03"`, `end_date: "2022-08"`
                                    *   "Sep 2019 - Ongoing" -> `start_date: "2019-09"`, `end_date: "Present"`
                                    *   "Completed: 2021" -> `date: "2021"` (for certifications/competitions)
                                    *   "Issued: 05/2022" -> `date: "2022-05"` (for certifications/competitions)
                                *   **Inference for Single Dates:** If only one date is present without an "ongoing" keyword (e.g., just "2010" or "03/2015"), attempt to infer if it's a start or end date based on CV layout/context if reasonably possible. If inference is uncertain or impossible, include only the specific date field that was found (e.g., just `start_date` or just `end_date` or just `date` for certifications/competitions).
                                *   **Whitespace & Empty Values:** Trim any leading/trailing whitespace from date components before formatting. Ignore date fields completely if the information is missing or nonsensical after attempting OCR correction.
                            # === END DATE MODIFICATION (Global Rule) ===

                            # --- Structuring Rules ---
                            - Before placing any section under "others", verify if the section's title and content have already been assigned to one of the main structured fields (name, technical_skills, education, certifications, work_experience, project_experience, foreign_languages) based on title or content analysis. If so, **DO NOT include them again under "others".** This is a CRITICAL exclusion rule.
                            - When extracting paragraphs (e.g., from "Summary", "About Me", descriptions), split them into individual sentences. Retain only sentences that are linguistically valid and contextually meaningful. Remove any text that contains broken English, garbled words, or unrecognizable phrases. Ensure correct handling of Romanian diacritics.

                          </global_instructions>
            <task>
                          <name>Advanced and Structured CV Parsing (Exclusive Extraction with Synonyms - Mandatory Main Sections with Scores, Separate Certifications, and Dual Parsing)</name>
                          <description>
                            **The primary goal is to analyze the text content of a Curriculum Vitae and extract structured information in a detailed and intuitive JSON format.**
                            The following sections will be extracted as separate sections and are considered mandatory if present in the CV:
                            - Full Name
                            - Technical Skills ("Technical Skills" or synonyms) - the score will be included only if explicitly mentioned.
                            - Education ("Education" or synonyms) - only information related to degrees (e.g., Bachelor, Master) will be included.
                            - Certifications ("Certifications", "Certificate", "Qualifications" or synonyms) - only information related to certificates will be included.
                            - Work Experience ("Work Experience" or synonyms) - competitions **and volunteering** will also be included.
                            - Projects Experience ("Projects Experience" or synonyms)
                            ONLY those fields and sections that are explicitly mentioned in the CV will be included in the JSON. If certain information or a section is not present, the corresponding field in the JSON will be omitted or its value will be null.
                            Sections will be identified using standard titles AND their synonyms. **Maximum importance will be given to the COMPLETE and EXACT preservation of all special characters and diacritics specific to the Romanian language (ă, ț, ș, â, î) in the extracted text and in the returned JSON structure.**
                            The "Languages" section (and synonyms) will be separated. Any other section, INCLUDING the rest of the contact information (email, phone, social media), will be grouped under "others" with the section title as the key and the content as the value.
                            **If a section in the CV contains information relevant to two parsing sections (e.g., "Education & Certifications", "Experience and Projects"), the content will be distributed accordingly into the appropriate JSON sections ("education" and "certifications", or "work_experience" and "project_experience") based on detailed analysis of each item within that section.**
                            The main objective is to facilitate further processing with NLP techniques for matching with job descriptions.
                            Sections will be identified using standard titles AND their synonyms. **Maximum importance will be given to the COMPLETE and EXACT preservation of all special characters and diacritics specific to the Romanian language (ă, ț, ș, â, î) in the extracted text and in the returned JSON structure. Every diacritic in the CV must be reflected IDENTICALLY in the JSON.**
                            IMPORTANT: The analysis will be performed on a CV containing text in both English and Romanian. **Give MAXIMUM importance to the correct preservation of all special characters and diacritics specific to the Romanian language (ă, ț, ș, â, î) in ALL sections, ESPECIALLY in the name and any Romanian content.**
                            - Do NOT add a period (`.`) at the end of any string (unless it's part of a sentence extracted from the CV).
                            **Additional instructions for text interpretation:**
                                        - Pay close attention to simple vertical characters. If such a character appears in the context of a proper noun, a title, or the beginning of a word, it is much more likely to be the uppercase letter "I" and not the vertical bar "|".
                            The following sections will be identified and structured, adapting to various CV formats and styles:
                            **Do not split ideas based solely on line breaks.** Ensure all extracted information is semantically coherent and complete.
                          </description>
                          <steps>
                            <step>
                              <name>Identifying and Extracting the Name</name>
                              <details>
                                **CRITICAL STEP:** Search for and extract the **true and complete full name** of the person, typically found at the top of the CV and often with more prominent formatting (e.g., larger font size, bold).
                                - Identify the person's First Name, Middle Name(s) (if present), and Last Name.
                                - **Strict Exclusion:** You MUST ignore and discard any surrounding text that is NOT part of the actual name, such as:
                                    - Job titles (e.g., "Software Engineer", "Information Technology Specialist")
                                    - Contact information (email, phone numbers, LinkedIn URLs)
                                    - Location details (City, Country)
                                    - Generic titles (Mr., Ms., Dr., Eng.)
                                    - Initials that appear to be abbreviations of other words (unless clearly part of a compound name structure).
                                    - Random characters or OCR noise (e.g., "? ENE ??").
                                - **Formatting Rules:**
                                    - Ensure each distinct name part (First, Middle, Last) starts with a capital letter.
                                    - Handle hyphenated names correctly (e.g., "Jean-Paul", "Maria-Elena"). Preserve the hyphen.
                                - **Semantic Check:** The final extracted string MUST represent a plausible human name.
                                - **Diacritics:** EXTREME ATTENTION MUST BE PAID TO PRESERVING ROMANIAN DIACRITICS (ă, ț, ș, â, î) EXACTLY AS THEY APPEAR IN THE NAME.
                                - **Output:** Return the cleaned, full name as the value for the `"name"` key at the top level of the JSON. If no plausible name can be confidently identified, the value of the `"name"` key should be null or the key omitted entirely.
                              </details>
                            </step>
                            <step>
                                          <name>Extracting Technical Skills (Skills Only Output)</name>
                                                      <details>
                                                        Identify the relevant sections for technical skills using titles such as "Technical Skills", "Skills", "Technical", "Programming Languages", "Professional Skills", "Key Skills", "Tech Stack", "Technology Proficiency", "Technical Expertise", "Core Technologies", "Development Tools & Technologies", "Technical Competencies", "IT Skills", "What I Work With", "Tools & Technologies", "Things I Know", "Digital Toolkit", "My Toolbox", "Tech Arsenal", "Tehnologii", "Competențe Tehnice". Analyze the content of these sections to extract each individual technical skill, taking into account the following rules:
                    
                                                        - Extract each individual technical skill.
                                                        - Extract the full, exact name of each skill as it appears, including modifiers like 'Big', 'Advanced', 'Basic' etc., unless modified by normalization rules below.
                    
                                                        **Mandatory Comma Separation Rule:**
                                                        - **General Case:** If a line, list item, or text segment contains multiple distinct skills separated by a comma (,), and **NO single level indicator applies to the group at the end** (see Level Extraction rules below), you MUST treat each comma-delimited segment as a separate, individual skill.
                                                        - Remove any leading or trailing whitespace from each skill after splitting.
                                                        - **Note:** This rule is applied *after* checking for shared levels (see below).
                    
                                                        **General Normalization Rule for "programming":**
                                                        - After extracting a skill, **replace all occurrences of the word " programming"** (case-insensitive, surrounded by word boundaries or at the beginning/end of the string) **with an empty string.**
                                                        - After the replacement, **trim any leading or trailing whitespace** that might have been introduced.
                                                        - For other skills that do not contain " programming", save them as extracted.
                    
                                                        **General Normalization Rule for "language":**
                                                        - After extracting a skill, **replace all occurrences of the word " language"** (case-insensitive, surrounded by word boundaries or at the beginning/end of the string) **with an empty string.**
                                                        - After the replacement, **trim any leading or trailing whitespace** that might have been introduced.
                                                        - For other skills that do not contain " language", save them as extracted.
                    
                                                        **Extracting and Associating Expertise Levels:**
                                                        - When extracting skills, **also identify any explicitly mentioned expertise levels or scores** associated with those skills. Levels often appear at the end of a line or segment, sometimes after a colon (e.g., `: 4`, `: Advanced`).
                                                        - Consider these as level indicators: "Expert", "Advanced", "Intermediate", "Beginner", "Proficient", "Familiar", numerical scores (e.g., "1/5", "7/10", "Score: 9", simple numbers like "4"), and Romanian words like "Avansat", "Incepator".
                    
                                                        - **Priority Rule for Shared Levels (CRITICAL FOR YOUR REQUIREMENT):**
                                                            - **IF a line or text segment clearly contains multiple skills listed together (often separated by commas or similar delimiters) FOLLOWED BY a single, shared level indicator** (e.g., `Python, TensorFlow: 4` or `JavaScript, ReactJS: 3` or `AWS SageMaker, Docker - Advanced`), THEN:
                                                                1. **Identify and extract the single shared `level`** (e.g., "4", "3", "Advanced").
                                                                2. **Isolate the text containing the list of skills** preceding the level indicator (e.g., `Python, TensorFlow` or `JavaScript, ReactJS` or `AWS SageMaker, Docker`).
                                                                3. **Split this skill text into individual skills** using the comma (or other relevant delimiter like '/') as a separator. Trim whitespace from each resulting skill. (e.g., ["Python", "TensorFlow"] or ["JavaScript", "ReactJS"]).
                                                                4. **Assign the extracted shared `level` to EACH of these individual skills.**
                                                                5. Process each resulting skill-level pair (e.g., Python with level 4, TensorFlow with level 4).
                                                            - **This rule takes precedence.** Do NOT assign the level only to the last skill in such cases.
                    
                                                        - **Rule for Individual Levels:**
                                                            - If levels are provided individually for each skill within a segment (e.g., `Java: Expert, Python: Intermediate`), extract each skill and its specifically associated level.
                    
                                                        - **Rule for No Level:**
                                                            - If, after applying the above rules, a skill is identified without any associated level indicator (either shared or individual), extract the skill without a level. This applies to skills listed alone without levels, or skills extracted using the basic Comma Separation Rule when no group level was present.
                    
                                                        - **Final Processing:** Apply the "programming" and "language" normalization rules to the skill name *after* association with its level (if any).
                    
                                                        **Final Output Structure (Skills with Levels):**
                                                        - The final JSON output for this section MUST be a list of objects, where each object contains:
                                                            - The `skill` key with the extracted and normalized skill name as its string value.
                                                            - An optional `level` key with the extracted expertise level as its string value (if available). **If no level is found for a skill, the `level` key MUST be omitted for that skill object.**
                    
                                                        **Desired JSON Output Example (Reflecting Corrected Logic):**
                                                        ```json
                                                        "technical_skills": [
                                                          {"skill": "Python", "level": "4"},       // Corrected based on "Python, TensorFlow: 4"
                                                          {"skill": "TensorFlow", "level": "4"},   // Corrected based on "Python, TensorFlow: 4"
                                                          {"skill": "JavaScript", "level": "3"},   // Corrected based on "JavaScript, ReactJS: 3"
                                                          {"skill": "ReactJS", "level": "3"},      // Corrected based on "JavaScript, ReactJS: 3"
                                                          {"skill": "AWS SageMaker", "level": "2"}, // Corrected based on "AWS SageMaker, Docker: 2"
                                                          {"skill": "Docker", "level": "2"},       // Corrected based on "AWS SageMaker, Docker: 2"
                                                          {"skill": "SQL", "level": "3"},          // Corrected based on "SQL, PostgreSQL: 3"
                                                          {"skill": "PostgreSQL", "level": "3"},   // Corrected based on "SQL, PostgreSQL: 3"
                                                          {"skill": "Figma", "level": "2"},        // Corrected based on "Figma, Adobe XD: 2"
                                                          {"skill": "Adobe XD", "level": "2"},     // Corrected based on "Figma, Adobe XD: 2"
                                                          {"skill": "Java", "level": "Expert"},    // Example: Individual level
                                                          {"skill": "Spring Boot", "level": "Advanced"}, // Example: Individual level
                                                          {"skill": "C/C++"},                      // Example: No level
                                                          {"skill": "C"},                      // Example: No level
                                                          {"skill": "C++"},                      // Example: No level
                                                          {"skill": "CSS"},                      // Example: No level
                                                          {"skill": "HTML"},                      // Example: No level
                                                          {"skill": "Cybersecurity"},               // Example: No level
                                                          {"skill": "Big Data Analytics"},         // Example: No level
                                                          {"skill": "Git"},                         // Example: No level
                                                          {"skill": "MySQL"}                        // Example: No level
                                                          // ... any other skills extracted following the rules
                                                        ]
                                                                                                ```
                                                                                                Return this structure under the `"technical_skills"` key ONLY if technical skills are found. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**
                                            </details>
                                        </step>
                           <step>
                              <name>Extracting Education (Degrees Only) and Certifications with Dual Parsing</name>
                              - **Do NOT treat personal achievements, awards, or job-based recognitions (e.g., "Cost Saving Initiative", "Employee of the Month", "System Uptima Excellence Award") as certifications.** These should be placed under the "others" section using the original title from the CV, such as "Achievements" or "Distinctions".
                              <details>
                                Identify the relevant sections for education using titles such as "Education", "Studies", "Formare", "Educație", "Education and Qualifications", "Educational Background", "Academic Background", "Academic History", "Academic Qualifications", "Educational Qualifications", "Formal Education", "Academic Training", "Scholarly Background", "Learning Journey".
                                Identify the relevant sections for certifications using titles such as "Certifications", "Certificate", "Qualifications", "Certificări", "Certificates", "Professional Certifications", "Accreditations", "Licenses & Certifications", "Training & Certifications", "Technical Certifications", "Relevant Certifications", "Achievements & Certifications", "Certifications & Courses", "Completed Certifications", "Industry Certifications".

                                **CRITICAL: Dual Parsing for Combined Sections:**
                                - **If a single section in the CV uses a combined title (e.g., "Education and Qualifications", "Academic Background & Certifications", "Studii și Calificări") OR if a section contains a mix of academic degrees and certifications without a clear combined title, you MUST analyze each individual entry listed within that section.**
                                - **Based on the *content* of each entry (e.g., mentions of "University", "Faculty", "Bachelor", "Master", "Licență" vs. "Certificate", "Course", "Academy", training provider names, specific certification names like "AWS Certified"), decide whether it belongs in the `"education"` list or the `"certifications"` list in the final JSON.**
                                - **Do not simply put all content under one key based on a combined title. Each item must be routed correctly.**

                                *For Education (Academic Degrees):*
                                Extract details ONLY for information related to academic degrees (e.g., Bachelor, Master, PhD, information from universities, colleges, high schools, or equivalents). If no degree information is found after analyzing all relevant sections, the "education" key will be omitted.
                                <instruction>In the "Education" section, for high schools, only the name of the high school will be used, without including "Colegiul Național" unless it is an integral part of the high school's name as it appears explicitly in the CV.</instruction>
                                For each identified education element:
                                  - **If the institution name is enclosed in double quotes ("..."), extract only the content inside the quotes and discard anything before or after unless it's part of the official school name.**
                                  - Extract "institution", "degree", "field_of_study", and "notes" if present.
                                  - Always distinguish between the **degree** and the **field of study**:
                                            - The "degree" refers to the **type of diploma obtained**, such as "Bachelor", "Master", "Doctorate", "Licență" or equivalent.
                                            - The "field_of_study" refers to **what was studied**, such as "Computer Science", "Law", "Mechanical Engineering", "Philology", "Informatică".
                                            - Example: A correct entry is `"degree": "Bachelor", "field_of_study": "Computer Science"`, not the other way around.
                                            - Never put a subject of study (e.g., "Computer Science") in the "degree" field.
                                            - If the degree is expressed in an extended form like "Master of Science in X" or "Bachelor of Arts in Y", split the information:
                                                            - Set `"degree"` to `"Master"` or `"Bachelor"`
                                                            - Set `"field_of_study"` to `"Science in X"` or `"Arts in Y"`
                                                            - Example: "Master of Science in Information Technology" → `"degree": "Master"`, `"field_of_study": "Science in Information Technology"`

                                  # === START DATE MODIFICATION (Education Period - Instructions) ===
                                  - Extract the "period" for each education entry by applying the **Strict Date Normalization** rule (defined in `<global_instructions>`) to identify and format `start_date` and `end_date` values into the required `YYYY-MM` / `YYYY` / `Present` format.
                                  # === END DATE MODIFICATION (Education Period - Instructions) ===

                                  # (Keeping original duration instructions below, they work with the normalized dates)
                                  - If the duration is explicitly mentioned in the text (e.g., "4 years", "4 ani", "2 years and 6 months"), include it in the `"duration"` field:
                                                      - `"duration": "4 years"`
                                  - If both `"start_date"` and `"end_date"` (with year information, not "Present") are successfully extracted based on the **Strict Date Normalization** rule, automatically calculate the time difference and include a `"duration_calculated"` field in the format:
                                                      - `"duration_calculated": "X years, Y months"` (e.g., `"duration_calculated": "3 years, 2 months"`)
                                  - **Search the text associated with the education element for explicit mentions of program duration using terms like "program duration", "length of program", "durata program" or equivalents. If you find a duration expressed in years and/or months (e.g., "4 ani", "4 years", "4 luni", "4 months", "4 ani și 3 luni", "4 years and 3 months"), save this duration exactly as mentioned in the `"duration"` field within the "period" sub-object.** (Note: This slightly overlaps previous duration instruction, prioritizing the explicitly found text if available).

                                   # === START TECHNOLOGIES MODIFICATION (Education) ===
                                   - **Mandatory Field:** Look for and extract any mentioned technologies, tools, or methodologies associated with the education entry. Include them in a `"technologies"` list. Apply the general normalization rule for "programming"/"language" to each extracted technology. **If no technologies are mentioned for this entry, include the key `"technologies"` with an empty list `[]` as its value.**
                                   # === END TECHNOLOGIES MODIFICATION (Education) ===

                                   **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**

                                  # === START DEFAULT DEGREE RULE MODIFICATION ===
                                  - **Default Degree Rule (Conditional):** If the extracted `institution` name contains keywords like "University", "Universitatea", "Faculty", "Facultatea" (or obvious synonyms indicating higher education) AND **no explicit degree** (like "Bachelor", "Master", "Licență", "Doctorat") was found within the text for that specific entry, then **default the `"degree"` field to `"Bachelor"`.** For all other types of institutions (e.g., High School, College without university context) where no degree is mentioned, the `"degree"` field should be omitted or set to null.
                                  # === END DEFAULT DEGREE RULE MODIFICATION ===

                                *For Certifications (Courses, Trainings, Professional Certs):*
                                Extract details ONLY for information related to certifications, specific modules, specialization courses (e.g., those offered by academies, companies, online platforms, organizations). Ensure to clearly distinguish these from academic degrees based on content analysis. If no certificate information is found after analyzing all relevant sections, the "certifications" key will be omitted. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**

                                # === START CERTIFICATIONS FIELDS MODIFICATION ===
                                For each identified certification element:
                                 - Extract the certification `name`.
                                 - Extract the issuing `institution` (e.g., Coursera, Udemy, Google, Microsoft, specific Academy name). Ensure this is the *issuing body* and not a technology or topic.
                                 - **(Optional) Extract any descriptive text** associated with the certification. If it's a paragraph, process it into coherent sentences using the global sentence processing rule. If it's bullet points, extract them as a list of strings. Place this into the `"description"` field. Omit if no description exists.
                                  - **Mandatory Field (`technologies`):**
                                                 - **Scope:** Carefully scan ONLY the extracted `name` and the `description` (if present) of THIS specific certification entry.
                                                 - **Action:** Identify and extract keywords representing specific technologies, software, tools, programming languages, platforms, frameworks, or methodologies mentioned *within* this scope (e.g., Python, Java, SQL, AWS, Azure, Docker, TensorFlow, Git, Agile, React, Machine Learning).
                                                 - **Normalization:** Apply the general normalization rule for "programming"/"language" to each extracted term (e.g., "Python programming" -> "Python language").
                                                 - **Output:** Collect these identified terms into a list under the `"technologies"` key.
                                                 - **Empty List Condition:** If, after thoroughly searching the `name` and `description` for this specific entry, NO relevant technology keywords are found, you MUST include the key `"technologies"` with an empty list `[]` as its value. **Do not include technologies mentioned elsewhere in the document unless they are explicitly part of THIS certification's name or description.**
                                 - Apply the **Strict Date Normalization** rule (defined in `<global_instructions>`) to identify and format the completion date into the required `YYYY-MM` / `YYYY` format. Place this value in the `"date"` field. Omit if no date is found.
                                # === END CERTIFICATIONS FIELDS MODIFICATION ===

                                 *Education Structure:*
                                "education": [
                                  {
                                    "institution": "Institution Name (University, College, High School)",
                                    "degree": "Degree Title", // Can be null/omitted for non-Uni/Faculty if not specified, or defaulted to Bachelor
                                    "field_of_study": "Field of Study",
                                    "period": {
                                      "start_date": "YYYY-MM or YYYY", // Strict Format
                                      "end_date": "YYYY-MM or YYYY or Present", // Strict Format
                                      "duration": "Explicitly mentioned duration (e.g., 4 years, 4 months, 4 years and 3 months)", // Optional
                                      "duration_calculated": "X years, Y months" // Optional, Calculated
                                    },
                                    "technologies": ["Tech A", "Tech B"], // Mandatory, empty list [] if none found
                                    "notes": "Special Mentions" // Optional
                                  },
                                  { ... }
                                ]


                                *Certifications Structure:*
                                "certifications": [
                                  {
                                    "name": "Certification/Module/Course Name",
                                    "institution": "Issuing Institution (Academy, Company, Platform)",
                                    "date": "YYYY-MM or YYYY", // Strict Format, Optional
                                    "description": "Descriptive text or list of points related to the certification", // Optional
                                    "technologies": ["Technology A", "Technology B", ...] // Mandatory, empty list [] if none found
                                  },
                                  { ... }
                                ]

                                Return the education structure under the "education" key ONLY if degree information is found.
                                Return the certifications structure under the "certifications" key ONLY if certifications are found.
                              </details>
                            </step>
                            <step>
                              <name>Extracting Work Experience (Including Competitions and Volunteering) with Duration</name>
                              <details>
                                Identify the relevant sections for work experience using titles such as "Work Experience", "Experience", "Professional Experience", "Experiență Profesională", "Istoric Profesional", "Employment History", "Relevant Experience", "Career History", "Job Experience", "Work History", "Industry Experience", "Practical Experience", "Professional Background", "Employment Experience", "Volunteering", "Volunteer Experience", "Voluntariat", "Hackathons", "Contests", "Competitions". Extract the details for each job, volunteer position, AND mentioned competition. If no relevant section is found, the "work_experience" key will be omitted.

                                **Categorization:**
                                - Standard job entries should have `"type": "job"`.
                                - Volunteering entries should have `"type": "volunteer"`.
                                - Competition entries should have `"type": "competition"`.

                                **Handling Combined Sections:** If a section titled "Experience" contains both jobs and projects, analyze each entry. Job details go here (`work_experience`), project details go to `project_experience`.

                                # === START DATE MODIFICATION (Work Experience Period - Instructions) ===
                                For the "period" field:
                                - Apply the **Strict Date Normalization** rule (defined in `<global_instructions>`) to extract `"start_date"` and `"end_date"` values (for jobs/volunteering) or a single `"date"` (typically for competitions) into the required `YYYY-MM` / `YYYY` / `Present` format.
                                # === END DATE MODIFICATION (Work Experience Period - Instructions) ===

                                # (Keeping original duration instructions below, they work with the normalized dates)
                                - **If a duration is explicitly mentioned (e.g., "2 ani și 3 luni", "5 months", "1 year"), save this duration exactly as mentioned in the `"duration"` field within the "period" sub-object.**
                                - If both "start_date" and "end_date" (including the year information, not "Present") are successfully extracted based on the **Strict Date Normalization** rule for a job/volunteering entry, calculate the duration in years, months, and days and include a `"duration_calculated"` field with this value. Keep the "start_date" and "end_date" fields as well.
                                - If date components are missing or the duration cannot be calculated, include only the present and correctly formatted `start_date`, `end_date`, or `date` fields obtained via the normalization rule.

                                **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**

                                # === START TECHNOLOGIES MODIFICATION (Work Experience) ===
                                - **Mandatory Field:** For job and volunteer entries (and competitions if applicable and mentioned), look for and extract any mentioned technologies, tools, or methodologies used. Include them in a `"technologies"` list. Apply the general normalization rule for "programming"/"language" to each extracted technology. **If no technologies are mentioned for this entry, include the key `"technologies"` with an empty list `[]` as its value.**
                                # === END TECHNOLOGIES MODIFICATION (Work Experience) ===

                                 Structure:
                                "work_experience": [
                                  {
                                    "type": "job", // or "volunteer"
                                    "title": "Job Title / Volunteer Role",
                                    "company": "Company Name / Organization",
                                    "period": {
                                      "start_date": "YYYY-MM or YYYY" , // Strict Format
                                      "end_date": "YYYY-MM or YYYY or Present" , // Strict Format
                                      "duration": "Explicitly mentioned duration (e.g., 2 ani și 3 luni, 5 months, 1 year)" , // Optional
                                      "duration_calculated": "X years, Y months, Z days" // Optional, calculated if both start/end dates available
                                    },
                                    "description": ["Point 1", "Point 2", "..."], // Process multi-line descriptions into single strings per point/sentence.
                                    "technologies": ["Technology A", "Technology B", ...] // Mandatory, empty list [] if none found
                                  },
                                  {
                                    "type": "competition",
                                    "name": "Competition Name",
                                    "organization": "Organizer" ,
                                    "period": {
                                      "date": "YYYY-MM or YYYY" // Strict Format, or use start/end date if range
                                    },
                                    "description": ["Competition Details"],
                                    "technologies": ["Tech Used (if mentioned)", ...] // Mandatory [], include if mentioned
                                  },
                                  { ... }
                                ]
                                Return this structure under the "work_experience" key ONLY if work experience, competition, or volunteering information is found.
                              </details>
                            </step>
                            <step>
                              <name>Extracting Project Experience</name>
                              <details>
                                Identify the relevant sections for project experience using titles such as "Projects Experience", "Projects", "Proiecte", "Personal Projects", "Technical Projects", "Software Projects", "Development Projects", "Portfolio Projects", "Academic Projects", "Relevant Projects", "IT Projects", "Hands-on Projects", "Practical Experience". Extract the details for each project ONLY if mentioned. If no relevant section is found, the "projects_experience" key will be omitted. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**
                               **Instructions for description:** When extracting the content for the "description" field, identify and capture each complete idea or bullet point as a single string. **Do not split ideas based solely on line breaks.** If an idea spans multiple lines in the CV, join those lines together into one continuous string, replacing internal newlines with spaces. Ensure that any newline characters (`\n`) present in the original multi-line text are **not included** in the final string unless they represent distinct bullet points/ideas.
                               **Handling Combined Sections:** If a section titled "Experience" contains both jobs and projects, analyze each entry. Project details go here (`project_experience`), job details go to `work_experience`.

                                # === START TECHNOLOGIES MODIFICATION (Project Experience) ===
                                - **Mandatory Field:** For project entries, look for and extract any mentioned technologies, tools, or methodologies used. Include them in a `"technologies"` list. Apply the general normalization rule for "programming"/"language" to each extracted technology. Ensure this is returned as a list of strings. **If no technologies are mentioned for this entry, include the key `"technologies"` with an empty list `[]` as its value.**
                                # === END TECHNOLOGIES MODIFICATION (Project Experience) ===

                                Structure:
                                "project_experience": [
                                  {
                                    "title": "Project Title",
                                    "description": "Short Description (joined multi-line text if needed)", // Optional
                                    "technologies": ["Technology 1", "Technology 2", "..."] // Mandatory list of strings, empty list [] if none found
                                  },
                                  { ... }
                                ]
                                Return this structure under the "project_experience" key ONLY if project experience information is found.
                              </details>
                            </step>
                            <step>
                              <name>Extracting Foreign Languages</name>
                              <details>
                                Identify the relevant sections for foreign languages using titles such as "Languages", "Foreign Languages", "Limbi Străine", "Language Skills", "Spoken Languages", "Linguistic Abilities", "Language Proficiency", "Language Competencies", "Multilingual Skills", "Communication in Foreign Languages", "What Languages I Speak", "Languages I Know", "Polyglot Profile". Extract the languages and levels ONLY if mentioned. If no relevant section is found, the "foreign_languages" key will be omitted. **ENSURE THAT ANY ROMANIAN TEXT WITHIN THIS SECTION PRESERVES DIACRITICS.**
                                Structure:
                                "foreign_languages": [
                                  { "language": "Foreign Language", "proficiency": "Level" } // level is optional
                                  ,
                                  { ... }
                                ]
                                Return this structure under the "foreign_languages" key ONLY if foreign languages are found.
                              </details>
                            </step>
                            <step>
                              <name>Extracting Other Information</name>
                                - **CRITICAL Exclusion:** Do NOT include sub-sections in the "others" key if their title OR content clearly matches a known category or synonym used in main sections like education, certifications, work_experience (including volunteering/competitions), technical_skills, project_experience, or foreign_languages. Verify against the synonym lists and content types defined in previous steps.
                                            - If a sub-section titled "Summary", "Profile", "Professional Summary", "About Me", "Despre Mine", "Obiectiv Profesional" etc. is found, parse its sentences individually and include only grammatically coherent, complete, and meaningful sentences in the final JSON. Remove any OCR-corrupted, broken, or nonsensical sentences. Apply global newline and OCR normalization rules.
                                            - Normalize common OCR issues in this section (e.g., "| bring" → "I bring", "Wy goal ls 1o" → "My goal is to", "enhancing" vs "enhencng", etc.).

                              <details>
                                Any other identified section that does not fit the main categories (after careful verification based on title and content) will be included under the "others" key. This INCLUDES contact information (email, phone, social media links, address), interests, soft skills/characteristics, and potentially non-certification achievements/awards. Extract the title of each sub-section and the relevant content associated with it. If no other sections exist, the "others" key will be omitted.
                                - Do NOT include professional achievements, workplace awards, or internal recognitions (e.g., "Cost Reduction Leader", "Employee Training Initiative Success", "System Uptime Excellence Award") under the "certifications" section.
                                - Instead, extract them under the "others" key, using the subsection title "Achievements", "Distinctions", "Premii", or the title as found in the CV. Present each achievement as a string in a list.

                                **Detailed and Prioritized Instructions for Parsing the "others" Section:**

                                For each section identified under "others":

                                1.  **Identify Explicit Titles:** Look for lines or phrases that are formatted as titles (e.g., bold, underlined, all caps, larger font size, separated by blank spaces). These indicate the beginning of a new sub-section (e.g., "Contact", "Interests", "Summary", "Hobby-uri", "Competențe transversale").

                                2.  **Extract Content Associated with Titles:** All text that follows an explicit title and up to the next explicit title (or the end of the CV section being parsed) belongs to that sub-section.

                                3.  **Structure Content by Titles:** The value associated with each identified title (key) within the `"others"` object MUST be structured as follows:
                                    *   If the key is `"Contact Information"` (or clear synonyms like "Contact", "Date de Contact"), the value MUST be a **list of objects**, where each object has a type key (e.g., "Email", "Phone number", "LinkedIn", "Website", "Address") and the corresponding value string.
                                    *   For ALL OTHER keys (e.g., "Summary", "Interests", "Achievements", "Characteristics", "Soft Skills", "Hobby-uri"), the value MUST be a **list of strings**. Each string should represent a distinct point, sentence, or item from the original CV section. Apply global newline replacement and sentence processing rules where applicable (especially for summary/paragraph sections).
                                    **ENSURE THAT ANY ROMANIAN TEXT WITHIN THESE SUB-SECTIONS PRESERVES DIACRITICS.**

                                **Handling Information Without Explicit Titles (Logical Grouping):**

                                If you find information that clearly belongs in "others" but is not preceded by an explicit title, try to group it logically based on keywords and format:

                                - **Contact Information:** Lines containing keywords such as "Email", "Website", "LinkedIn", "Phone Number", "Address", "Telefon", "Adresă" should be grouped under the title `"Contact Information"` and formatted as a list of objects. **ENSURE ROMANIAN DIACRITICS ARE PRESERVED.**
                                - **Interests:** Lines containing keywords such as "Interests", "Pasiuni", "Hobbies", "Hobby-uri" should be grouped under the title `"Interests"` as a list of strings. **ENSURE ROMANIAN DIACRITICS ARE PRESERVED.**
                                - **Personal Characteristics/Soft Skills:** Lines with adjectives or short descriptive phrases, often listed together, should be grouped under titles such as `"Characteristics"`, `"Soft Skills"`, or `"Competențe transversale"` as a list of strings. **ENSURE ROMANIAN DIACRITICS ARE PRESERVED.**
                                - **Phone Number Key Normalization:** Check if a contact key extracted is "Phone no", "Phone No", "Tel." (case-insensitive). If it is, use `"Phone number"` as the key within the corresponding object in the `"Contact Information"` list.

                                **Desired Output Structure (Under the "others" Key):**
                                ```json
                                "others": {
                                  "Contact Information": [ // List of OBJECTS
                                    { "Email": "mirunaratiu03@gmail.com" },
                                    { "Address": "Cluj-Napoca" },
                                    { "Website": "https://github.com/MirunaRatiu" },
                                    { "LinkedIn": "www.linkedin.com/in/miruna-ratiu" },
                                    { "Phone number": "+40735506666" }
                                  ],
                                  "Interests": [ // List of STRINGS
                                    "AI",
                                    "Robotics",
                                    "Computer Science",
                                    "Basketball",
                                    "Swimming",
                                    "Traveling",
                                    "Photography",
                                    "Strategic games"
                                  ],
                                   "Hobby-uri": [ // Example with Romanian title
                                     "Citit",
                                     "Drumeții"
                                   ],
                                  "Characteristics": [ // List of STRINGS
                                    "Curious",
                                    "Adaptable",
                                    "Collaborative",
                                    "Open-minded",
                                    "Determined",
                                    "Solution-Oriented"
                                  ],
                                   "Achievements": [ // List of STRINGS
                                     "Achievement description 1",
                                     "Achievement description 2"
                                  ],
                                   "Summary": [ // List of STRINGS (processed sentences)
                                     "Sentence 1.",
                                     "Sentence 2."
                                   ]
                                  // ... other sections like "Obiectiv Profesional": [list of strings]
                                }
                                ```
                              </details>
                            </step>
                          </steps>
                          <output-structure>
                            <name>Output Format</name>
                            <details>
                              Return a single JSON object that will include ONLY the keys for the information and sections present in the CV (identified by standard titles OR synonyms, and correctly distributed if titles were combined). The following sections will be at the top level (if found):
                              - "name": string (present only if found, mandatory)
                              - "technical_skills": list of objects (present only if technical skills or synonyms exist, mandatory)
                              - "education": list of objects (present only if degree information or synonyms exist, mandatory, includes mandatory `technologies` field)
                              - "certifications": list of objects (present only if certifications or synonyms exist, mandatory, includes optional description and mandatory `technologies` field)
                              - "work_experience": list of objects (present only if work experience, volunteering, or competitions exist, mandatory, includes mandatory `technologies` field for jobs/volunteering)
                              - "project_experience": list of objects (present only if project experience or synonyms exist, mandatory, includes mandatory `technologies` field)
                              - "foreign_languages": list of objects (present only if foreign languages or synonyms exist)
                              - "others": object (present only if other sections OR additional contact information exist after main category exclusion; the value is an object containing keys whose values are lists - typically lists of strings, except for Contact Information which is a list of objects)
                              - **Parse the provided text, paying SPECIAL attention to the correctness of diacritics. Ensure that the text is processed correctly, preserving ALL original diacritics in the final text, especially for Romanian words and the name.**
                              - Return only valid and complete JSON. All lists (arrays `[]`) and objects (curly braces `{}`) must be closed correctly and use double quotes consistently for keys and string values. Ensure extracted text makes semantic sense.
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
            System.out.println("Content "+content);
            String encodedContent = SymbolsManipulation.encodeDiacritics(replaceLongDashWithShortDash(content));

            GenerateContentResponse response =
                    client.models.generateContent(GEMINI_MODEL, PROMPT + "\n\nCV Content:\n" + encodedContent, null);


            System.out.println("Response:" + response);
            String apiResponse = response.text();

            apiResponse = SymbolsManipulation.decodeDiacritics(apiResponse);

            if (apiResponse.startsWith("```json")) {
                apiResponse = apiResponse.substring(7).trim();
            }
            if (apiResponse.endsWith("```")) {
                apiResponse = apiResponse.substring(0, apiResponse.length() - 3).trim();
            }
            System.out.println("API Response: " + apiResponse);



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
                String name= (String) parsedData.get("name");
                data.put("name",name.replaceFirst("^[A-Z]\\s+", ""));
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
        data.put("name", extractName(content).replaceFirst("^[A-Z]\\s+", ""));
        data.put("technical_skills", extractSkills(content));
        data.put("foreign_languages", extractLanguages(content));
        data.put("education", extractEducation(content));
        data.put("certifications", extractCertifications(content));
        data.put("project_experience", extractProjects(content));
        data.put("work_experience", extractWorkExperience(content));
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

    private static List<String> extractSkills(String content) {
        return extractList(content, "Technical Skills");
    }

    private static List<String> extractLanguages(String content) {
        return extractList(content, "Foreign Languages");
    }

    private static List<String> extractEducation(String content) {
        return extractList(content, "Education");
    }

    private static List<String> extractCertifications(String content) {
        return extractList(content, "Certifications");
    }

    private static List<String> extractProjects(String content) {
        List<String> projects = new ArrayList<>();
        int startIndex = content.indexOf("Project Experience");
        if (startIndex != -1) {
            int endIndex = content.indexOf("\n\n", startIndex + "Project Experience".length());
            if (endIndex == -1) {
                endIndex = content.length();
            }
            String sectionContent = content.substring(startIndex + "Project Experience".length(), endIndex).trim();
            projects.addAll(Arrays.asList(sectionContent.split("\n")));
        }
        return projects.stream().map(String::trim).collect(Collectors.toList());
    }

    private static List<String> extractWorkExperience(String content) {
        List<String> workExperience = new ArrayList<>();
        int startIndex = content.indexOf("Work Experience");
        if (startIndex != -1) {
            int endIndex = content.indexOf("\n\n", startIndex + "Work Experience".length());
            if (endIndex == -1) {
                endIndex = content.length();
            }
            String sectionContent = content.substring(startIndex + "Work Experience".length(), endIndex).trim();
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
            if (line.isEmpty()) {
                continue;
            }

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
            if (endIndex == -1) {
                endIndex = content.length();
            }
            String sectionContent = content.substring(startIndex + sectionTitle.length(), endIndex).trim();
            items.addAll(Arrays.asList(sectionContent.split(",")));
            if (items.isEmpty()) {
                items.addAll(Arrays.asList(sectionContent.split("\n")));
            }
        }
        return items.stream().map(String::trim).collect(Collectors.toList());
    }





}

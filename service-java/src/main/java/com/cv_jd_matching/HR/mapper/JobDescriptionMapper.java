package com.cv_jd_matching.HR.mapper;

import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionViewDTO;
import com.cv_jd_matching.HR.entity.JobDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JobDescriptionMapper {
    public static JobDescriptionDTO mapEntityToDTO(JobDescription jobDescription){
        return JobDescriptionDTO.builder()
                .jobTitle(jobDescription.getJobTitle())
                .companyOverview(jobDescription.getCompanyOverview())
                .keyResponsibilities(jobDescription.getKeyResponsibilities())
                .preferredSkills(jobDescription.getPreferredSkills())
                .requiredQualifications(jobDescription.getRequiredQualifications())
                .id(jobDescription.getId())
                .message(jobDescription.getMessage())
                .build();
    }

    public static JobDescriptionViewDTO mapEntityToViewDTO(JobDescription jobDescription){
        return JobDescriptionViewDTO.builder()
                .jobTitle(jobDescription.getJobTitle())
                .id(jobDescription.getId())
                .accessLink(jobDescription.getPathName())
                .jobDepartment(jobDescription.getMessage())
                .companyOverview(jobDescription.getCompanyOverview())
                .requiredQualifications(parseOriginalStatements(jobDescription.getRequiredQualifications()))

                .keyResponsabilities(parseOriginalStatements(jobDescription.getKeyResponsibilities()))
                .preferredSkills(parseOriginalStatementsToList(jobDescription.getPreferredSkills()))

                .build();
    }

    private static String parseOriginalStatements(String input) {
        StringBuilder result = new StringBuilder();
        int index = 0;

        while (index < input.length()) {
            int start = input.indexOf("original_statement=", index);
            if (start == -1) {
                break; // No more original_statement fields
            }
            start += "original_statement=".length();

            // Read until the first '.' that ends a sentence
            int end = start;
            boolean foundPeriod = false;
            while (end < input.length()) {
                char c = input.charAt(end);
                if (c == '.') {
                    foundPeriod = true;
                    end++; // include the period
                    break;
                }
                end++;
            }

            if (foundPeriod) {
                String statement = input.substring(start, end).trim();
                result.append(statement).append(" ");
                index = end;
            } else {
                // If no period is found, stop parsing
                break;
            }
        }

        return result.toString().trim();
    }


    private static List<String> parseOriginalStatementsToList(String input) {
        List<String> statements = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return statements;
        }

        // Regex to find 'original_statement=' and capture its value (non-greedily)
        // until the next comma followed by a space (', ') or a closing brace ('}').
        // This pattern is designed for the specific input format you provided.
        Pattern pattern = Pattern.compile("original_statement=(.*?)(?:, |})");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String statement = matcher.group(1); // Capture group 1 contains the value

            // Clean up leading/trailing whitespace from the extracted statement
            statement = statement.trim();

            // Optional: Add more robust cleaning if the regex might capture unwanted characters
            // For instance, if the input format varies slightly.

            if (!statement.isEmpty()) {
                statements.add(statement);
            }
        }

        return statements;
    }


}
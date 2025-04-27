package com.cv_jd_matching.HR.mapper;

import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionViewDTO;
import com.cv_jd_matching.HR.entity.JobDescription;


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
}
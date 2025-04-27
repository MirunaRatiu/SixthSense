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
                .build();
    }

    public static JobDescriptionViewDTO mapEntityToViewDTO(JobDescription jobDescription){
        return JobDescriptionViewDTO.builder()
                .jobTitle(jobDescription.getJobTitle())
                .id(jobDescription.getId())
                .build();
    }
}
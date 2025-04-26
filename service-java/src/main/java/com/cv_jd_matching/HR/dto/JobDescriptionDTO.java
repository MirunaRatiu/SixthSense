package com.cv_jd_matching.HR.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class JobDescriptionDTO {
    private String jobTitle;
    private String companyOverview;
    private String keyResponsibilities;
    private String requiredQualifications;
    private String preferredSkills;
    private Integer id;
}
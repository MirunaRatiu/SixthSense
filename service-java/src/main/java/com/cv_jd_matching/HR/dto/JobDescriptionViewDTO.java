package com.cv_jd_matching.HR.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDescriptionViewDTO {
    private String jobTitle;
    private Integer id;
    private String accessLink;
    private String companyOverview;
    private String requiredQualifications;
    private String jobDepartment;
    private String keyResponsabilities;
    private List<String> preferredSkills;
}

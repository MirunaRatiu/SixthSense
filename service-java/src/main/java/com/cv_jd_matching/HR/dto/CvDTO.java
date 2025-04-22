package com.cv_jd_matching.HR.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class CvDTO {
    private String technicalSkills;
    private String foreignLanguages;
    private String education;
    private String certifications;
    private String projectExperience;
    private String workExperience;
    private String others;
    private Integer id;
}
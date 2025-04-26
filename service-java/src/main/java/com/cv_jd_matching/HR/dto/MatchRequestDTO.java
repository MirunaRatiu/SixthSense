package com.cv_jd_matching.HR.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchRequestDTO {
    private CvDTO cv;
    private JobDescriptionDTO jd;
    private Map<String, Integer> jobSkills;
    private List<String> industryKeywords;
}

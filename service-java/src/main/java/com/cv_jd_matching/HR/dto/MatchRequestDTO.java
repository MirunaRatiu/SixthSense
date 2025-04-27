package com.cv_jd_matching.HR.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchRequestDTO {
    @JsonProperty("cv")
    private Integer cv;

    @JsonProperty("jd")
    private JobDescriptionDTO jd;

    @JsonProperty("job_skills")
    private Map<String, Integer> jobSkills;
}

package com.cv_jd_matching.HR.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class JobMatchRequestDTO {

    @JsonProperty("jd")
    private JobDescriptionDTO jobDescription;

    @JsonProperty("job_skills")
    private Map<String, Integer> jobSkills;
}

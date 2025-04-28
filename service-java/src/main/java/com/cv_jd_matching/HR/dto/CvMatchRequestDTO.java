package com.cv_jd_matching.HR.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class CvMatchRequestDTO {

    @JsonProperty("cv")
    private Integer cvId;

    @JsonProperty("jd")
    private List<JobDescriptionDTO> jobDescriptions;
}

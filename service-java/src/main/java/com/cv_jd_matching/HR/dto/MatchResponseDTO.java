package com.cv_jd_matching.HR.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchResponseDTO {
    private float score;
    private Map<String, String> explanation;
}

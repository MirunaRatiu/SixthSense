package com.cv_jd_matching.HR.dto;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Data
public class JobViewMatchDTO {
    private Integer jdId;
    private Map<String, Integer> additionalSkills;
}
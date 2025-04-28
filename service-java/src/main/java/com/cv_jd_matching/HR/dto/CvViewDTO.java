package com.cv_jd_matching.HR.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvViewDTO {
    private String name;
    private Integer id;
    private List<String> skills;
    private List<String> education;
    private List<String> languages;
    private String accessLink;
}

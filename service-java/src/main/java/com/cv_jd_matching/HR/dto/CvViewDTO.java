package com.cv_jd_matching.HR.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvViewDTO {
    private String name;
    private Integer id;
    private String skills;
}

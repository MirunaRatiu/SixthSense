package com.cv_jd_matching.HR.mapper;

import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.entity.Cv;

public class CvMapper {
    public static CvDTO mapEntityToDTO(Cv cv){
        return CvDTO.builder()
                .projectExperience(String.valueOf(cv.getProjectExperience()))
                .certifications(String.valueOf(cv.getCertifications()))
                .education(String.valueOf(cv.getEducation()))
                .foreignLanguages(String.valueOf(cv.getForeignLanguages()))
                .workExperience(String.valueOf(cv.getWorkExperience()))
                .others(String.valueOf(cv.getOthers()))
                .technicalSkills(String.valueOf(cv.getTechnicalSkills()))
                .id(cv.getId())
                .build();
    }
}
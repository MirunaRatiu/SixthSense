package com.cv_jd_matching.HR.mapper;

import com.cv_jd_matching.HR.dto.*;
import com.cv_jd_matching.HR.error.InputException;
import com.cv_jd_matching.HR.service.CvService;
import com.cv_jd_matching.HR.service.JobDescriptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchResponseMapper {
    public static CvMatchResponseDTO mapMatchToCVDTO(MatchResponseDTO matchResponseDTO, CvViewDTO cvViewDTO) {
        return CvMatchResponseDTO.builder()
                .explanation(matchResponseDTO.getExplanation())
                .score(matchResponseDTO.getScore())
                .cvViewDTO(cvViewDTO)
                .build();
    }

    public static JobMatchResponseDTO mapMatchToJobDTO(MatchResponseDTO matchResponseDTO, JobDescriptionViewDTO jobDescriptionViewDTO) {
        return JobMatchResponseDTO.builder()
                .explanation(matchResponseDTO.getExplanation())
                .score(matchResponseDTO.getScore())
                .jobDescriptionViewDTO(jobDescriptionViewDTO)
                .build();
    }
}
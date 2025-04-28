package com.cv_jd_matching.HR.mapper;

import com.cv_jd_matching.HR.dto.CvMatchResponseDTO;
import com.cv_jd_matching.HR.dto.JobMatchResponseDTO;
import com.cv_jd_matching.HR.dto.MatchResponseDTO;
import com.cv_jd_matching.HR.error.InputException;
import com.cv_jd_matching.HR.service.CvService;
import com.cv_jd_matching.HR.service.JobDescriptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MatchResponseMapper {
    private final CvService cvService;
    private final JobDescriptionService jobDescriptionService;

    public CvMatchResponseDTO mapMatchToCVDTO(MatchResponseDTO matchResponseDTO) throws InputException {
        return CvMatchResponseDTO.builder()
                .explanation(matchResponseDTO.getExplanation())
                .score(matchResponseDTO.getScore())
                .cvViewDTO(cvService.getCvById(matchResponseDTO.getId()))
                .build();
    }

    public JobMatchResponseDTO mapMatchToJobDTO(MatchResponseDTO matchResponseDTO) throws InputException {
        return JobMatchResponseDTO.builder()
                .explanation(matchResponseDTO.getExplanation())
                .score(matchResponseDTO.getScore())
                .jobDescriptionViewDTO(jobDescriptionService.getJobDescriptionById(matchResponseDTO.getId()))
                .build();
    }
}

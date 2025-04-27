package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.dto.MatchResponseDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface MatchingClient {
    Mono<MatchResponseDTO> match(Integer cvId, Integer jobDescriptionId);

    Mono<String> embedJobDescription(JobDescriptionDTO jobDescriptionDTO);

    Mono<String> embedCv(CvDTO cvDTO);

    Mono<List<MatchResponseDTO>> matchCv(Integer cvId);

    Mono<List<MatchResponseDTO>> matchJobDescription(Integer jdId);
}
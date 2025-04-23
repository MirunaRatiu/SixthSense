package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import reactor.core.publisher.Mono;

public interface MatchingClient {
    Mono<Integer> match(Integer cvId, Integer jobDescriptionId);

    Mono<String> embedJobDescription(JobDescriptionDTO jobDescriptionDTO);

    Mono<String> embedCv(CvDTO cvDTO);
}
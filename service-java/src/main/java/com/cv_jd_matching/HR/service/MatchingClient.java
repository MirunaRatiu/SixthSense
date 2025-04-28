package com.cv_jd_matching.HR.service;


import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.dto.MatchResponseDTO;

import com.cv_jd_matching.HR.dto.*;
import com.cv_jd_matching.HR.error.InputException;

import com.cv_jd_matching.HR.error.WrongWeightsException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface MatchingClient {

    Mono<MatchResponseDTO> match(Integer cvId, Integer jobDescriptionId) throws InputException;


    Mono<String> embedJobDescription(JobDescriptionDTO jobDescriptionDTO);

    Mono<String> embedCv(CvDTO cvDTO);


    List<JobMatchResponseDTO> matchCv(Integer cvId);

    List<CvMatchResponseDTO> matchJobDescription(Integer jdId, Map<String, Integer> additionalSkills) throws WrongWeightsException, InputException;


    Mono<String> deleteCv(Integer cvId);

    Mono<String> deleteJobDescription(Integer jdId);
}
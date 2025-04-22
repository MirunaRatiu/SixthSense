package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.config.WebClientConfig;
import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.entity.Cv;
import com.cv_jd_matching.HR.entity.JobDescription;
import com.cv_jd_matching.HR.mapper.CvMapper;
import com.cv_jd_matching.HR.mapper.JobDescriptionMapper;
import com.cv_jd_matching.HR.repository.ICvRepository;
import com.cv_jd_matching.HR.repository.IJobDescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchingClientImpl implements MatchingClient{
    private final WebClient webClient;
    private final ICvRepository cvRepository;
    private final IJobDescriptionRepository jobDescriptionRepository;

    public Mono<Integer> match(Integer cvId, Integer jobDescriptionId){
        Optional<Cv> cv = cvRepository.findById(cvId);
        Optional<JobDescription> jobDescription = jobDescriptionRepository.findById(jobDescriptionId);
        if(cv.isEmpty() || jobDescription.isEmpty()){
            throw new RuntimeException("BAD MATCHING INPUT!");
        }
        CvDTO cvDTO = CvMapper.mapEntityToDTO(cv.get());
        JobDescriptionDTO jobDescriptionDTO = JobDescriptionMapper.mapEntityToDTO(jobDescription.get());
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("cvDTO", cvDTO);
        builder.part("jobDescriptionDTO", jobDescriptionDTO);
        return webClient.post()
                .uri("/match")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Integer.class);
    }

}

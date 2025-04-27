package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.config.WebClientConfig;
import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.dto.MatchRequestDTO;
import com.cv_jd_matching.HR.dto.MatchResponseDTO;
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

import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchingClientImpl implements MatchingClient{
    private final WebClient webClient;
    private final ICvRepository cvRepository;
    private final IJobDescriptionRepository jobDescriptionRepository;

    public Mono<MatchResponseDTO> match(Integer cvId, Integer jobDescriptionId){
        Optional<Cv> cv = cvRepository.findById(cvId);
        Optional<JobDescription> jobDescription = jobDescriptionRepository.findById(jobDescriptionId);
        if(cv.isEmpty() || jobDescription.isEmpty()){
            throw new RuntimeException("BAD MATCHING INPUT!");
        }
        CvDTO cvDTO = CvMapper.mapEntityToDTO(cv.get());
        JobDescriptionDTO jobDescriptionDTO = JobDescriptionMapper.mapEntityToDTO(jobDescription.get());

        Map<String, Integer> jobSkills = new HashMap<>();
        jobSkills.put("Python", 30);
        jobSkills.put("TensorFlow", 20);
        jobSkills.put("PyTorch", 20);
        jobSkills.put("Scikit-learn", 10);
        jobSkills.put("AWS", 10);
        jobSkills.put("Git", 10);

        MatchRequestDTO requestDTO = new MatchRequestDTO(cvDTO, jobDescriptionDTO, jobSkills);

        return webClient.post()
                .uri("/match/aux")
                .bodyValue(requestDTO)
                .retrieve()
                .bodyToMono(MatchResponseDTO.class);
    }

    public Mono<String> embedJobDescription(JobDescriptionDTO jobDescriptionDTO){
        return webClient.post()
                .uri("/embed/jd")
                .bodyValue(jobDescriptionDTO)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> embedCv(CvDTO cvDTO){
        return webClient.post()
                .uri("/embed/cv")
                .bodyValue(cvDTO)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<List<MatchResponseDTO>> matchCv(Integer cvId) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/delete/cv/{item_id}")
                        .build(cvId))
                .retrieve()
                .bodyToFlux(MatchResponseDTO.class)
                .collectList();
    }

    public Mono<List<MatchResponseDTO>> matchJobDescription(Integer jdId) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/delete/jd/{item_id}")
                        .build(jdId))
                .retrieve()
                .bodyToFlux(MatchResponseDTO.class)
                .collectList();
    }

}

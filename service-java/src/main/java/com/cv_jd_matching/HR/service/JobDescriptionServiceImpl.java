package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionViewDTO;
import com.cv_jd_matching.HR.entity.JobDescription;
import com.cv_jd_matching.HR.mapper.JobDescriptionMapper;
import com.cv_jd_matching.HR.repository.IJobDescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class JobDescriptionServiceImpl implements JobDescriptionService {
    private final IJobDescriptionRepository jobDescriptionRepository;
    private final WebClient webClient;

    public void deleteFiles(List<Integer> jobIDs){
        List<JobDescription> jobDescriptions = jobIDs.stream().map(id -> jobDescriptionRepository.findById(id).get()).toList();
        jobDescriptionRepository.deleteAll(jobDescriptions);
        for(Integer id: jobIDs){
            webClient.method(HttpMethod.DELETE)
                    .uri("/delete/jd")
                    .bodyValue(id)
                    .retrieve()
                    .bodyToMono(String.class).subscribe();
        }
    }

    public List<JobDescriptionViewDTO> getJobDescriptions(){
        Iterable<JobDescription> jobs = jobDescriptionRepository.findAll();
        List<JobDescription> jobList = StreamSupport.stream(jobs.spliterator(), false).toList();
        return jobList.stream().map(JobDescriptionMapper::mapEntityToViewDTO).toList();
    }

    public JobDescriptionViewDTO getJobDescriptionById(Integer id){
        Optional<JobDescription> job = jobDescriptionRepository.findById(id);
        if(job.isEmpty()){
            throw new RuntimeException("Wrong id");
        }
        return JobDescriptionMapper.mapEntityToViewDTO(job.get());
    }

    public JobDescriptionDTO getJobDescriptionByPath(String path){
        Optional<JobDescription> job = jobDescriptionRepository.findJobDescriptionByPathName(path);
        if(job.isEmpty()){
            throw new RuntimeException("Wrong url");
        }
        return JobDescriptionMapper.mapEntityToDTO(job.get());
    }
}

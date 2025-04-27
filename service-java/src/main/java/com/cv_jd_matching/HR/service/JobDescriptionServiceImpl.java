package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionViewDTO;
import com.cv_jd_matching.HR.entity.JobDescription;
import com.cv_jd_matching.HR.error.InputException;
import com.cv_jd_matching.HR.error.PathException;
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
    private final MatchingClient matchingClient;

    public void deleteFiles(List<Integer> jobIDs){
        List<JobDescription> jobDescriptions = jobIDs.stream()
                .map(jobDescriptionRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        jobDescriptionRepository.deleteAll(jobDescriptions);
        for(Integer id: jobIDs){
            matchingClient.deleteJobDescription(id).subscribe();
        }
    }

    public List<JobDescriptionViewDTO> getJobDescriptions(){
        Iterable<JobDescription> jobs = jobDescriptionRepository.findAll();
        List<JobDescription> jobList = StreamSupport.stream(jobs.spliterator(), false).toList();
        return jobList.stream().map(JobDescriptionMapper::mapEntityToViewDTO).toList();
    }

    public JobDescriptionViewDTO getJobDescriptionById(Integer id) throws InputException {
        Optional<JobDescription> job = jobDescriptionRepository.findById(id);
        if(job.isEmpty()){
            throw new InputException("Wrong id");
        }
        return JobDescriptionMapper.mapEntityToViewDTO(job.get());
    }

    public JobDescriptionDTO getJobDescriptionByPath(String path) throws PathException {
        Optional<JobDescription> job = jobDescriptionRepository.findJobDescriptionByPathName(path);
        if(job.isEmpty()){
            throw new PathException("Wrong url");
        }
        return JobDescriptionMapper.mapEntityToDTO(job.get());
    }
}

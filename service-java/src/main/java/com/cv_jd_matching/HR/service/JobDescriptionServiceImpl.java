package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.dto.JobDescriptionViewDTO;
import com.cv_jd_matching.HR.entity.JobDescription;
import com.cv_jd_matching.HR.mapper.JobDescriptionMapper;
import com.cv_jd_matching.HR.repository.IJobDescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class JobDescriptionServiceImpl implements JobDescriptionService {
    private final IJobDescriptionRepository jobDescriptionRepository;

    public void deleteFiles(List<String> jobNames){
        List<JobDescription> jobDescriptions = jobNames.stream().map(name -> jobDescriptionRepository.findJobDescriptionByJobTitle(name).get()).toList();
        jobDescriptionRepository.deleteAll(jobDescriptions);
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
}

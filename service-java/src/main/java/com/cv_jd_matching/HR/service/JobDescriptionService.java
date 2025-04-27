package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.dto.JobDescriptionDTO;
import com.cv_jd_matching.HR.dto.JobDescriptionViewDTO;
import com.cv_jd_matching.HR.entity.JobDescription;
import com.cv_jd_matching.HR.error.InputException;
import com.cv_jd_matching.HR.error.PathException;
import com.cv_jd_matching.HR.mapper.JobDescriptionMapper;

import java.util.List;
import java.util.stream.StreamSupport;

public interface JobDescriptionService {
    void deleteFiles(List<Integer> ids);

    List<JobDescriptionViewDTO> getJobDescriptions();

    JobDescriptionViewDTO getJobDescriptionById(Integer id) throws InputException;

    JobDescriptionDTO getJobDescriptionByPath(String path) throws PathException;
}

package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.CvViewDTO;
import com.cv_jd_matching.HR.entity.Cv;

import java.util.List;

public interface CvService {
    List<CvViewDTO> getCvs();

    void deleteFiles(List<Integer> ids);

    CvDTO getCvByPath(String path);
}

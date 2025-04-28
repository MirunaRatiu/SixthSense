package com.cv_jd_matching.HR.service;

import com.cv_jd_matching.HR.dto.CvDTO;
import com.cv_jd_matching.HR.dto.CvViewDTO;
import com.cv_jd_matching.HR.entity.Cv;
import com.cv_jd_matching.HR.error.InputException;
import com.cv_jd_matching.HR.error.PathException;
import com.cv_jd_matching.HR.mapper.CvMapper;
import com.cv_jd_matching.HR.repository.ICvRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class CvServiceImpl implements CvService{

    private final ICvRepository cvRepository;
    private final MatchingClient matchingClient;
    public List<CvViewDTO> getCvs(){
        Iterable<Cv> cvs = cvRepository.findAll();
        List<Cv> parsed = StreamSupport.stream(cvs.spliterator(), false).toList();
        return parsed.stream().map(CvMapper::mapEntityToViewDTO).toList();
    }

    public void deleteFiles(List<Integer> ids){
        List<Cv> cvs = ids.stream()
                .map(cvRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        cvRepository.deleteAll(cvs);
        for(Integer id: ids){
            matchingClient.deleteCv(id).subscribe();
        }
    }

    public CvDTO getCvByPath(String path) throws PathException {
        Optional<Cv> cv = cvRepository.findCvByPathName(path);
        if(cv.isEmpty()){
            throw new PathException("Wrong path");
        }
        return CvMapper.mapEntityToDTO(cv.get());
    }

    @Override
    public CvViewDTO getCvById(Integer id) throws InputException {
        Optional<Cv> cv = cvRepository.findById(id);
        if(cv.isEmpty()){
            throw new InputException("The cv with that id is not saved in the database");
        }
        return CvMapper.mapEntityToViewDTO(cv.get());
    }
}

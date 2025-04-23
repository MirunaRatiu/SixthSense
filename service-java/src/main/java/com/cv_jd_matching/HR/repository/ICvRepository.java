package com.cv_jd_matching.HR.repository;

import com.cv_jd_matching.HR.entity.Cv;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ICvRepository extends CrudRepository<Cv, Integer> {
    default int countAll() {
        return Math.toIntExact(count());
    }

    Optional<Cv> findCvByPathName(String path);

    Optional<Cv> findCvByName(String name);

}

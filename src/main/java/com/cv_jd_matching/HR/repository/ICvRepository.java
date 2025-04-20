package com.cv_jd_matching.HR.repository;

import com.cv_jd_matching.HR.entity.Cv;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICvRepository extends CrudRepository<Cv, Integer> {
    default int countAll() {
        return Math.toIntExact(count());
    }
}

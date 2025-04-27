package com.cv_jd_matching.HR.repository;

import com.cv_jd_matching.HR.entity.Cv;
import com.cv_jd_matching.HR.entity.JobDescription;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IJobDescriptionRepository extends CrudRepository<JobDescription, Integer> {
    default int countAll() {
        return Math.toIntExact(count());
    }

    @Query(value = "SELECT * FROM job_description " +
            "ORDER BY CAST(REGEXP_SUBSTR(file_name, '[0-9]+') AS UNSIGNED) DESC " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<JobDescription> findTopByOrderByFileNameDesc();

}

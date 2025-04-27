package com.cv_jd_matching.HR.repository;

import com.cv_jd_matching.HR.entity.Cv;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

@Repository
public interface ICvRepository extends CrudRepository<Cv, Integer> {
    default int countAll() {
        return Math.toIntExact(count());
    }
    @Query(value = "SELECT * FROM cv WHERE file_name REGEXP :pattern LIMIT 1", nativeQuery = true)
    Cv findByFileNameRegex(@Param("pattern") String pattern);

    @Query("SELECT c FROM Cv c WHERE c.fileName LIKE CONCAT('%', :normalizedName, '.%')")
    Optional<Cv> findFirstByFileNameContaining(@Param("normalizedName") String normalizedName);

    @Query(value = "SELECT * FROM cv " +
            "ORDER BY CAST(REGEXP_SUBSTR(file_name, '[0-9]+') AS UNSIGNED) DESC " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<Cv> findTopByOrderByFileNameDesc();

    Optional<Cv> findCvByPathName(String path);

    Optional<Cv> findCvByName(String name);

    @Modifying
    @Transactional
    void deleteAllByCreatedAtBefore(Date date);

}

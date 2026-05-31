package com.example.demo.repository;

import com.example.demo.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findAllByOrderByCreatedAtDesc();
    List<Application> findByStatusOrderByCreatedAtDesc(String status);
    Optional<Application> findByEmail(String email);
    List<Application> findByStatusAndCareerTrackOrderByCreatedAtDesc(String status, String careerTrack);

    @Query("SELECT a FROM Application a WHERE LOWER(a.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.careerTrack) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.department) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Application> searchByKeyword(@Param("query") String keyword);

    @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
    List<Object[]> countApplicationsByStatus();

    @Query("SELECT a FROM Application a WHERE a.qrCodeExpiresAt IS NOT NULL AND a.qrCodeExpiresAt < :now")
    List<Application> findExpiredQrCodes(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT a.careerTrack FROM Application a WHERE a.careerTrack IS NOT NULL AND a.careerTrack != '' ORDER BY a.careerTrack")
    List<String> findDistinctCareerTracks();

    @Query("SELECT DISTINCT a.department FROM Application a WHERE a.department IS NOT NULL AND a.department != '' ORDER BY a.department")
    List<String> findDistinctDepartments();
}

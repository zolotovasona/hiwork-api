package com.example.demo.repository;

import com.example.demo.entity.CareerTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CareerTrackRepository extends JpaRepository<CareerTrack, Long> {
    Optional<CareerTrack> findByName(String name);
}

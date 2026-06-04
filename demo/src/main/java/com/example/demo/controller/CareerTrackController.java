package com.example.demo.controller;

import com.example.demo.entity.CareerTrack;
import com.example.demo.entity.User;
import com.example.demo.repository.CareerTrackRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/career-tracks")
@CrossOrigin(origins = "*")
public class CareerTrackController {

    @Autowired
    private CareerTrackRepository careerTrackRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAllTracks() {
        try {
            List<CareerTrack> tracks = careerTrackRepository.findAll();
            
            // Считаем количество сотрудников по каждому треку
            List<User> employees = userRepository.findAll().stream()
                    .filter(u -> "EMPLOYEE".equalsIgnoreCase(u.getRole()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> result = new ArrayList<>();
            
            for (CareerTrack track : tracks) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", track.getId());
                map.put("name", track.getName());
                map.put("levels", track.getLevels());
                map.put("branches", track.getBranches());
                map.put("branchNamesJson", track.getBranchNamesJson());
                map.put("levelNamesJson", track.getLevelNamesJson());
                
                long count = employees.stream()
                        .filter(e -> track.getName().equals(e.getCareerTrack()))
                        .count();
                map.put("employeesCount", count);
                
                result.add(map);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createTrack(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            int levels = (int) body.get("levels");
            int branches = (int) body.get("branches");
            String branchNamesJson = (String) body.get("branchNamesJson");
            String levelNamesJson = (String) body.get("levelNamesJson");

            if (careerTrackRepository.findByName(name).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Трек с таким названием уже существует"));
            }

            CareerTrack track = new CareerTrack();
            track.setName(name);
            track.setLevels(levels);
            track.setBranches(branches);
            track.setBranchNamesJson(branchNamesJson);
            track.setLevelNamesJson(levelNamesJson);

            careerTrackRepository.save(track);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "id", track.getId(),
                    "name", track.getName()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTrack(@PathVariable Long id) {
        try {
            careerTrackRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    // ✅ Удалить трек
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTrack(@PathVariable Long id) {
        try {
            CareerTrack track = careerTrackRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Трек не найден"));
        
            careerTrackRepository.delete(track);
            System.out.println("✅ Трек удалён: " + track.getName());
        
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Трек удалён"
            ));
        
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

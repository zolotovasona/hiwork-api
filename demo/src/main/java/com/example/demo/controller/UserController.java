package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // Получение списка всех сотрудников
    @GetMapping("/employees")
    public ResponseEntity<?> getAllEmployees() {
        try {
            List<User> employees = userRepository.findAll().stream()
                    .filter(user -> "EMPLOYEE".equalsIgnoreCase(user.getRole()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> result = new java.util.ArrayList<>();

            for (User emp : employees) {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", emp.getId());
                map.put("fullName", emp.getFullName() != null ? emp.getFullName() : "");
                map.put("email", emp.getEmail());
                map.put("careerTrack", emp.getCareerTrack() != null ? emp.getCareerTrack() : "");
                map.put("department", emp.getDepartment() != null ? emp.getDepartment() : "");
                map.put("level", emp.getLevel() != null ? emp.getLevel() : "Junior");
                map.put("aboutMe", emp.getAboutMe() != null ? emp.getAboutMe() : "");
                map.put("resumeUrl", emp.getResumeUrl() != null ? emp.getResumeUrl() : "");
                map.put("portfolioUrl", emp.getPortfolioUrl() != null ? emp.getPortfolioUrl() : "");
                map.put("profilePhotoUrl", emp.getProfilePhotoUrl() != null ? emp.getProfilePhotoUrl() : "");
                map.put("qrCode", emp.getQrCode() != null ? emp.getQrCode() : "");
                map.put("createdAt", emp.getCreatedAt() != null ? emp.getCreatedAt().toString() : "");
                result.add(map);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    // ✅ Удалить сотрудника
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));

            if (!"EMPLOYEE".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Можно удалять только сотрудников"));
            }

            userRepository.delete(user);
            System.out.println("✅ Сотрудник удалён: " + user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Сотрудник уволен"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    // ✅ Обновить данные сотрудника
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));

            if (!"EMPLOYEE".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Можно обновлять только сотрудников"));
            }

            // Обновляем только переданные поля
            if (body.containsKey("careerTrack")) {
                user.setCareerTrack((String) body.get("careerTrack"));
            }
            if (body.containsKey("department")) {
                user.setDepartment((String) body.get("department"));
            }

            userRepository.save(user);
            System.out.println("✅ Сотрудник обновлён: " + user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Данные сотрудника обновлены",
                    "employeeId", user.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

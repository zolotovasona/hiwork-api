package com.example.demo.controller;

import com.example.demo.entity.Application;
import com.example.demo.entity.User;
import com.example.demo.repository.ApplicationRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@CrossOrigin(origins = "*")
@Profile("dev") // ✅ Работает ТОЛЬКО в профиле dev
public class TestDataController {

    @Autowired private UserRepository userRepository;
    @Autowired private ApplicationRepository appRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    // === 👤 СОЗДАНИЕ ТЕСТОВОГО HR ===
    @GetMapping("/create-test-hr")
    public Map<String, Object> createTestHr() {
        // Удаляем старого, если есть
        userRepository.findByEmail("hr@test.com").ifPresent(userRepository::delete);

        User hr = new User();
        hr.setEmail("hr@test.com");
        hr.setPasswordHash(passwordEncoder.encode("password123"));
        hr.setRole("HR");
        hr.setFullName("Тестовый HR");
        hr.setQrCode("04062026");
        hr.setCreatedAt(LocalDateTime.now());
        userRepository.save(hr);

        // ✅ Возвращаем Map (JSON), а не String
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "✅ HR создан");
        result.put("credentials", Map.of(
                "email", "hr@test.com",
                "password", "password123",
                "hrCode", "04062026"
        ));
        return result;
    }

    // === 👤 СОЗДАНИЕ ТЕСТОВОГО СОТРУДНИКА ===
    @GetMapping("/create-test-employee")
    public Map<String, Object> createTestEmployee() {
        userRepository.findByEmail("emp@test.com").ifPresent(userRepository::delete);

        User emp = new User();
        emp.setEmail("emp@test.com");
        emp.setPasswordHash(passwordEncoder.encode("password123"));
        emp.setRole("EMPLOYEE");
        emp.setFullName("Тестовый Сотрудник");
        emp.setQrCode("1234567");
        emp.setQrCodeExpiresAt(LocalDateTime.now().plusMinutes(2));
        emp.setCreatedAt(LocalDateTime.now());
        userRepository.save(emp);

        // ✅ Возвращаем Map (JSON), а не String
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "✅ Сотрудник создан");
        result.put("credentials", Map.of(
                "email", "emp@test.com",
                "password", "password123",
                "qrCode", "1234567"
        ));
        return result;
    }

    // ===  СОЗДАНИЕ ТЕСТОВЫХ ЗАЯВОК ===
    @GetMapping("/create-test-applications")
    public Map<String, Object> createTestApplications() {
        appRepo.deleteAll();

        String[] names = {"Анна Иванова", "Дмитрий Петров", "Елена Сидорова"};
        String[] tracks = {"Java Developer", "QA Engineer", "UI/UX Designer"};
        String[] depts = {"IT", "QA", "Design"};

        for (int i = 0; i < names.length; i++) {
            Application app = new Application();
            app.setFullName(names[i]);
            app.setEmail("candidate" + (i+1) + "@test.com");
            app.setCareerTrack(tracks[i]);
            app.setDepartment(depts[i]);
            app.setExperience(String.valueOf((i+1) * 2));
            app.setAboutMe("Тестовая заявка #" + (i+1));
            app.setStatus(i == 0 ? "new" : i == 1 ? "interviewing" : "hired");
            app.setCreatedAt(LocalDateTime.now().minusDays(i));
            appRepo.save(app);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "✅ Создано 3 тестовых заявки");
        result.put("count", 3);
        return result;
    }

    // === 🧹 ОЧИСТКА ДАННЫХ ===
    @GetMapping("/clear-test-data")
    public Map<String, Object> clearTestData() {
        userRepository.deleteAll();
        appRepo.deleteAll();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "🗑️ Все данные удалены");
        return result;
    }
}
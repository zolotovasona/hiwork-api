package com.example.demo.controller;

import com.example.demo.entity.Application;
import com.example.demo.entity.CareerTrack;
import com.example.demo.entity.User;
import com.example.demo.repository.ApplicationRepository;
import com.example.demo.repository.CareerTrackRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"https://hiwork-hiring-site.onrender.com", "http://localhost:5500", "*"})
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CareerTrackRepository careerTrackRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private FileService fileService;

    // === СОЗДАНИЕ НОВОЙ ЗАЯВКИ ===
    @PostMapping
    public ResponseEntity<?> createApplication(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("careerTrack") String careerTrack,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "aboutMe", required = false) String aboutMe,
            @RequestParam(value = "resumeTextContent", required = false) String resumeTextContent,
            @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile,
            @RequestParam(value = "portfolio", required = false) MultipartFile portfolioFile,
            @RequestParam(value = "photo", required = false) MultipartFile photoFile) {

        try {
            System.out.println("📩 Новая заявка от: " + fullName + " (" + email + ")");

            Application app = new Application();
            app.setFullName(fullName);
            app.setEmail(email);
            app.setCareerTrack(careerTrack);
            app.setDepartment(department != null ? department : "");
            app.setExperience(experience != null ? experience : "0");
            app.setAboutMe(aboutMe != null ? aboutMe : "");
            app.setResumeTextContent(resumeTextContent != null ? resumeTextContent : "");
            app.setStatus("new");

            // Загружаем файлы
            if (resumeFile != null && !resumeFile.isEmpty()) {
                String url = fileService.saveFile(resumeFile, "resume");
                app.setResumeUrl(url);
            }
            if (portfolioFile != null && !portfolioFile.isEmpty()) {
                String url = fileService.saveFile(portfolioFile, "portfolio");
                app.setPortfolioUrl(url);
            }
            if (photoFile != null && !photoFile.isEmpty()) {
                String url = fileService.saveFile(photoFile, "photo");
                app.setProfilePhotoUrl(url);
            }

            applicationRepository.save(app);
            System.out.println("✅ Заявка сохранена с id=" + app.getId());

            // ✅ Уведомляем HR о новой заявке
            try {
                emailService.notifyHrAboutNewApplicant(fullName, email, careerTrack);
            } catch (Exception e) {
                System.err.println("⚠️ Ошибка отправки email (не критично): " + e.getMessage());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("id", app.getId());
            result.put("message", "Заявка успешно отправлена");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ Ошибка создания заявки: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // === ПОЛУЧЕНИЕ ВСЕХ ЗАЯВОК ===
    @GetMapping
    public ResponseEntity<?> getAllApplications() {
        try {
            List<Application> apps = applicationRepository.findAllByOrderByCreatedAtDesc();
            List<Map<String, Object>> result = new ArrayList<>();

            for (Application app : apps) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", app.getId());
                map.put("fullName", app.getFullName() != null ? app.getFullName() : "");
                map.put("email", app.getEmail() != null ? app.getEmail() : "");
                map.put("careerTrack", app.getCareerTrack() != null ? app.getCareerTrack() : "");
                map.put("department", app.getDepartment() != null ? app.getDepartment() : "");
                map.put("experience", app.getExperience() != null ? app.getExperience() : "0");
                map.put("aboutMe", app.getAboutMe() != null ? app.getAboutMe() : "");
                map.put("resumeTextContent", app.getResumeTextContent() != null ? app.getResumeTextContent() : "");
                map.put("resumeUrl", app.getResumeUrl() != null ? app.getResumeUrl() : "");
                map.put("portfolioUrl", app.getPortfolioUrl() != null ? app.getPortfolioUrl() : "");
                map.put("profilePhotoUrl", app.getProfilePhotoUrl() != null ? app.getProfilePhotoUrl() : "");
                map.put("status", app.getStatus() != null ? app.getStatus() : "new");
                map.put("qrCode", app.getQrCode() != null ? app.getQrCode() : "");
                map.put("qrCodeExpiresAt", app.getQrCodeExpiresAt() != null ? app.getQrCodeExpiresAt().toString() : "");
                map.put("createdAt", app.getCreatedAt() != null ? app.getCreatedAt().toString() : "");
                result.add(map);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ Ошибка получения заявок: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // === ПОЛУЧЕНИЕ ОПЦИЙ (треки и отделы) ===
    @GetMapping("/options")
    public ResponseEntity<?> getOptions() {
        try {
            List<Application> apps = applicationRepository.findAll();
            
            List<String> careerTracks = apps.stream()
                    .map(Application::getCareerTrack)
                    .filter(t -> t != null && !t.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            List<String> departments = apps.stream()
                    .map(Application::getDepartment)
                    .filter(d -> d != null && !d.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("careerTracks", careerTracks);
            result.put("departments", departments);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // === ПЕРЕВОД В СОБЕСЕДОВАНИЕ ===
    @PostMapping("/{id}/interview")
    public ResponseEntity<?> moveToInterview(@PathVariable Long id) {
        try {
            Application app = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            app.setStatus("interviewing");
            applicationRepository.save(app);

            System.out.println("✅ Заявка " + id + " переведена в собеседование");

            // ✅ Уведомляем кандидата
            try {
                emailService.sendInterviewInvite(app.getEmail(), app.getFullName());
            } catch (Exception e) {
                System.err.println("⚠️ Ошибка отправки email: " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Заявка переведена в собеседование"
            ));

        } catch (Exception e) {
            System.err.println("❌ Ошибка: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // === НАЙМ СОТРУДНИКА ===
    @PostMapping("/{id}/hire")
    public ResponseEntity<?> hireApplicant(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            Application app = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            // Обновляем трек и отдел, если переданы
            if (body != null) {
                if (body.containsKey("careerTrack")) {
                    app.setCareerTrack(body.get("careerTrack"));
                }
                if (body.containsKey("department")) {
                    app.setDepartment(body.get("department"));
                }
            }

            // Генерируем QR-код
            app.generateQrCode();
            app.setStatus("hired");
            applicationRepository.save(app);

            System.out.println("✅ Сотрудник принят: " + app.getEmail() + ", QR: " + app.getQrCode());

            // ✅ Уведомляем сотрудника о найме
            try {
                emailService.sendHireConfirmation(
                        app.getEmail(),
                        app.getFullName(),
                        app.getCareerTrack(),
                        app.getQrCode()
                );
            } catch (Exception e) {
                System.err.println("⚠️ Ошибка отправки email: " + e.getMessage());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Сотрудник принят");
            result.put("qrCode", app.getQrCode());
            result.put("qrCodeExpiresAt", app.getQrCodeExpiresAt().toString());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ Ошибка найма: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // === РЕГЕНЕРАЦИЯ QR-КОДА ===
    @PostMapping("/{id}/regenerate-qr")
    public ResponseEntity<?> regenerateQr(@PathVariable Long id) {
        try {
            Application app = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            app.generateQrCode();
            applicationRepository.save(app);

            System.out.println("🔄 QR перегенерирован для заявки " + id);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("qrCode", app.getQrCode());
            result.put("qrCodeExpiresAt", app.getQrCodeExpiresAt().toString());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // === ЗАВЕРШЕНИЕ ЗАЯВКИ (ПЕРЕНОС В СОТРУДНИКИ) ===
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeApplication(@PathVariable Long id) {
        try {
            Application app = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена с id=" + id));

            System.out.println("📋 Обрабатываем заявку id=" + id + ", email=" + app.getEmail());

            if (!"hired".equals(app.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Можно перенести только принятых сотрудников (статус: " + app.getStatus() + ")"));
            }

            // Проверяем, нет ли уже пользователя с таким email
            Optional<User> existingUser = userRepository.findByEmail(app.getEmail());
            if (existingUser.isPresent()) {
                System.out.println("⚠️ Пользователь с email " + app.getEmail() + " уже существует. Обновляем данные.");
                User emp = existingUser.get();
                emp.setFullName(app.getFullName());
                emp.setCareerTrack(app.getCareerTrack());
                emp.setDepartment(app.getDepartment());
                emp.setAboutMe(app.getAboutMe());
                emp.setResumeUrl(app.getResumeUrl());
                emp.setPortfolioUrl(app.getPortfolioUrl());
                emp.setProfilePhotoUrl(app.getProfilePhotoUrl());
                emp.setRole("EMPLOYEE");
                if (emp.getLevel() == null) emp.setLevel("Junior");
                userRepository.save(emp);

                applicationRepository.delete(app);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Сотрудник обновлён (уже существовал)",
                        "employeeId", emp.getId()
                ));
            }

            // Создаём нового сотрудника
            User employee = new User();
            employee.setEmail(app.getEmail());
            employee.setFullName(app.getFullName() != null ? app.getFullName() : "Неизвестно");
            employee.setCareerTrack(app.getCareerTrack() != null ? app.getCareerTrack() : "");
            employee.setDepartment(app.getDepartment() != null ? app.getDepartment() : "");
            employee.setAboutMe(app.getAboutMe());
            employee.setResumeUrl(app.getResumeUrl());
            employee.setPortfolioUrl(app.getPortfolioUrl());
            employee.setProfilePhotoUrl(app.getProfilePhotoUrl());
            employee.setRole("EMPLOYEE");
            employee.setLevel("Junior");

            employee.generateQrCodeWithTimeout();

            String tempPassword = "TempPass_" + System.currentTimeMillis();
            employee.setPasswordHash(tempPassword);

            userRepository.save(employee);
            System.out.println("✅ Сотрудник сохранён с id=" + employee.getId());

            applicationRepository.delete(app);
            System.out.println("✅ Заявка " + id + " удалена");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Сотрудник успешно добавлен",
                    "employeeId", employee.getId(),
                    "email", employee.getEmail()
            ));

        } catch (Exception e) {
            System.err.println("❌ Ошибка при переносе: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // === ОТКЛОНЕНИЕ ЗАЯВКИ ===
    @DeleteMapping("/{id}")
    public ResponseEntity<?> rejectApplication(@PathVariable Long id) {
        try {
            Application app = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            String email = app.getEmail();
            String name = app.getFullName();

            applicationRepository.delete(app);
            System.out.println("❌ Заявка " + id + " отклонена");

            try {
                emailService.notifyRejected(email, name);
            } catch (Exception e) {
                System.err.println("⚠️ Ошибка отправки email: " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Заявка отклонена"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

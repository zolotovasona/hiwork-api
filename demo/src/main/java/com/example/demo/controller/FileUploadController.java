package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {
    
    // ✅ Тот же абсолютный путь!
    private static final String UPLOAD_DIR = "/tmp/uploads/";

    private static final List<String> ALLOWED_TYPES = List.of("resume", "portfolio", "photo");
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".pdf", ".doc", ".docx", ".txt", ".jpg", ".jpeg", ".png", ".gif", ".zip", ".rar");

    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("type") String type) {

        if (type == null || !ALLOWED_TYPES.contains(type.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Недопустимый тип: " + type));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Файл пуст"));
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") ?
                originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase() : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Недопустимое расширение: " + extension));
        }

        try {
            String filename = UUID.randomUUID().toString() + extension;
            Path categoryPath = Paths.get(UPLOAD_DIR, type);
            Files.createDirectories(categoryPath);
            Path filePath = categoryPath.resolve(filename);
            Files.write(filePath, file.getBytes());

            System.out.println("✅ Файл сохранён: " + filePath.toAbsolutePath());

            String fileUrl = "/api/files/" + type + "/" + filename;
            return ResponseEntity.ok(Map.of(
                    "success", true, "url", fileUrl,
                    "filename", originalFilename, "type", type, "size", file.getSize()
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка загрузки: " + e.getMessage()));
        }
    }
}

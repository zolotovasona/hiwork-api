package com.example.demo.controller;

import lombok.extern.java.Log;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CrossOrigin(origins = {"https://hiwork-hiring-site.onrender.com", "http://localhost:5500", "*"})
@RestController
@RequestMapping("/api/files")
@Log
public class FileController {

    private final String uploadDir = "uploads/";

    @GetMapping("/{category}/{filename}")
    public ResponseEntity<?> downloadFile(
            @PathVariable String category,
            @PathVariable String filename) {
        try {
            if (!category.matches("^(resume|portfolio|photo)$")) {
                return ResponseEntity.badRequest().body("Неверная категория");
            }
            if (filename.contains("..") || filename.contains("/")) {
                return ResponseEntity.badRequest().body("Неверное имя файла");
            }

            Path filePath = Paths.get(System.getProperty("user.dir"), "uploads", category, filename).normalize();
            log.info("Trying to serve: " + filePath.toString());

            if (!Files.exists(filePath)) {
                log.warning("File not found: " + filePath);
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new FileSystemResource(filePath));

        } catch (IOException e) {
            log.severe("Error serving file: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Ошибка сервера");
        }
    }
}

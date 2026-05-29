package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDirectories() {
        return args -> {
            String baseDir = "uploads";
            String[] subDirs = {"resume", "portfolio", "photo"};
            try {
                Path basePath = Paths.get(baseDir);
                Files.createDirectories(basePath);
                for (String sub : subDirs) {
                    Files.createDirectories(basePath.resolve(sub));
                }
                System.out.println("✅ Папки для файлов созданы");
            } catch (Exception e) {
                System.err.println("⚠️ Не удалось создать папки: " + e.getMessage());
            }
        };
    }
}


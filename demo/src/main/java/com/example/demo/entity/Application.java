package com.example.demo.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;  // ✅ JAKARTA вместо JAVAX!
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String careerTrack;

    @Column(columnDefinition = "TEXT")
    private String aboutMe;

    private String department;
    private String experience;

    @Column(columnDefinition = "TEXT")
    private String resumeTextContent;

    @Column(columnDefinition = "TEXT")
    private String resumeUrl;
    
    @Column(columnDefinition = "TEXT")
    private String portfolioUrl;
    
    @Column(columnDefinition = "TEXT")
    private String profilePhotoUrl;

    private String status;
    private String qrCode;
    private LocalDateTime qrCodeExpiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

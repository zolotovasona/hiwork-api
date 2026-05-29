package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Random;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String careerTrack;
    private String department;
    private String experience;
    private String aboutMe;

    // Файлы
    private String resumeUrl;
    private String portfolioUrl;
    private String profilePhotoUrl;

    // ✅ Текст резюме (добавлено!)
    private String resumeTextContent;

    @Column(nullable = false)
    private String status = "new";

    @Column(length = 7)
    private String qrCode;

    @Column(name = "qr_code_expires_at")
    private LocalDateTime qrCodeExpiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ==========================================
    //  ГЕТТЕРЫ И СЕТТЕРЫ (ВСЕ ВРУЧНУЮ)
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCareerTrack() { return careerTrack; }
    public void setCareerTrack(String careerTrack) { this.careerTrack = careerTrack; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getAboutMe() { return aboutMe; }
    public void setAboutMe(String aboutMe) { this.aboutMe = aboutMe; }

    public String getResumeUrl() { return resumeUrl; }
    public void setResumeUrl(String resumeUrl) { this.resumeUrl = resumeUrl; }

    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    // ✅ Сеттер для текста резюме
    public String getResumeTextContent() { return resumeTextContent; }
    public void setResumeTextContent(String resumeTextContent) { this.resumeTextContent = resumeTextContent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public LocalDateTime getQrCodeExpiresAt() { return qrCodeExpiresAt; }
    public void setQrCodeExpiresAt(LocalDateTime qrCodeExpiresAt) { this.qrCodeExpiresAt = qrCodeExpiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ==========================================
    //  ЛОГИКА QR-КОДА
    // ==========================================
    public boolean isQrCodeValid() {
        return qrCode != null && qrCodeExpiresAt != null && LocalDateTime.now().isBefore(qrCodeExpiresAt);
    }

    public void generateQrCodeWithTimeout() {
        this.qrCode = String.format("%07d", new Random().nextInt(10_000_000));
        this.qrCodeExpiresAt = LocalDateTime.now().plusMinutes(2);
    }

    public long getQrCodeSecondsRemaining() {
        if (!isQrCodeValid()) return 0;
        return Duration.between(LocalDateTime.now(), qrCodeExpiresAt).getSeconds();
    }
}
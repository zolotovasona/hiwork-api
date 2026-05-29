package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Random;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role; // "HR" или "EMPLOYEE"

    private String fullName;
    private String careerTrack;
    private String department;
    private String level;

    @Column(columnDefinition = "TEXT")
    private String aboutMe;

    @Column(length = 7)
    private String qrCode;

    @Column(name = "qr_code_expires_at")
    private LocalDateTime qrCodeExpiresAt;

    private String resumeUrl;
    private String portfolioUrl;
    private String profilePhotoUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ==========================================
    //  ГЕТТЕРЫ И СЕТТЕРЫ (ВРУЧНУЮ, БЕЗ LOMBOK)
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCareerTrack() { return careerTrack; }
    public void setCareerTrack(String careerTrack) { this.careerTrack = careerTrack; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getAboutMe() { return aboutMe; }
    public void setAboutMe(String aboutMe) { this.aboutMe = aboutMe; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public LocalDateTime getQrCodeExpiresAt() { return qrCodeExpiresAt; }
    public void setQrCodeExpiresAt(LocalDateTime qrCodeExpiresAt) { this.qrCodeExpiresAt = qrCodeExpiresAt; }

    public String getResumeUrl() { return resumeUrl; }
    public void setResumeUrl(String resumeUrl) { this.resumeUrl = resumeUrl; }

    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ==========================================
    //  ЛОГИКА
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

    public boolean isHr() {
        return "HR".equalsIgnoreCase(this.role);
    }

    public boolean isEmployee() {
        return "EMPLOYEE".equalsIgnoreCase(this.role);
    }
}
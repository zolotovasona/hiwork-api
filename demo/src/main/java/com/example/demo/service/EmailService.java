package com.example.demo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EmailService {
    @Autowired private JavaMailSender mailSender;
    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // HTML режим
            mailSender.send(message);
            LOG.info("✅ Письмо отправлено на " + to);
        } catch (MessagingException e) {
            LOG.log(Level.SEVERE, "❌ Ошибка отправки письма на " + to, e);
        }
    }

    public void sendInterviewInvite(String to, String fullName) { /* HTML-шаблон приглашения */ }

    public void sendHireConfirmation(String to, String fullName, String qrCode, String qrImageUrl) {
        String html = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;border:1px solid #eee;border-radius:8px;text-align:center;'>" +
                "<h2 style='color:#28a745;'>🎉 Поздравляем! Вы приняты в HiWork</h2>" +
                "<p>Здравствуйте, <b>" + fullName + "</b>!</p>" +
                "<p>Для активации аккаунта используйте код:</p>" +
                "<div style='background:#f8f9fa;padding:15px;border-radius:6px;margin:20px 0;'>" +
                "<span style='font-size:32px;font-weight:bold;letter-spacing:4px;color:#0d6efd;'>" + qrCode + "</span>" +
                "</div>" +
                "<p>📲 Отсканируйте QR-код или введите код вручную:</p>" +
                "<img src='" + qrImageUrl + "' alt='QR Code' style='max-width:200px;height:auto;margin:15px 0;'/>" +
                "<p style='color:#dc3545;font-weight:bold;'>⏰ Код действителен 2 минуты</p>" +
                "<hr/><p style='color:#666;font-size:12px;'>© 2026 HiWork HR Platform</p>" +
                "</div>";
        sendHtmlEmail(to, "🎉 Вы приняты в HiWork! Код доступа внутри", html);
    }

    public void sendQrRegenerated(String to, String fullName, String newQrCode, String newQrImageUrl) { /* аналогично */ }
}
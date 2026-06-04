package com.example.demo.service;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    @Value("${resend.api.key:}")
    private String apiKey;

    @Value("${resend.from.email:onboarding@resend.dev}")
    private String fromEmail;

    @Value("${app.hr.email:test@mail.ru}")
    private String hrEmail;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private final OkHttpClient client = new OkHttpClient();

    // === БАЗОВЫЙ МЕТОД ОТПРАВКИ ===
    public boolean sendEmail(String to, String subject, String htmlBody) {
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("⚠️ Resend API key не настроен. Email не отправлен на " + to);
            return false;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("from", fromEmail);
            json.put("to", to);
            json.put("subject", subject);
            json.put("html", htmlBody);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(RESEND_API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    System.out.println("✅ Email отправлен на " + to);
                    return true;
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Нет тела";
                    System.err.println("❌ Ошибка отправки email: " + response.code() + " - " + errorBody);
                    return false;
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Exception при отправке email: " + e.getMessage());
            return false;
        }
    }

    // === УВЕДОМЛЕНИЕ HR О НОВОЙ ЗАЯВКЕ ===
    public void notifyHrAboutNewApplicant(String applicantName, String applicantEmail, String careerTrack) {
        if (hrEmail == null || hrEmail.isEmpty()) {
            System.out.println("⚠️ HR email не настроен. Уведомление не отправлено.");
            return;
        }

        String subject = "🆕 Новая заявка: " + applicantName;
        String html = "<div style='font-family: sans-serif; padding: 20px;'>" +
                "<h2 style='color: #667eea;'>Поступила новая заявка!</h2>" +
                "<p><b>ФИО:</b> " + applicantName + "</p>" +
                "<p><b>Email:</b> " + applicantEmail + "</p>" +
                "<p><b>Карьерный трек:</b> " + careerTrack + "</p>" +
                "<hr>" +
                "<p style='color: #888;'>Откройте приложение HIWork для обработки.</p>" +
                "</div>";

        sendEmail(hrEmail, subject, html);
    }

    // === ПРИГЛАШЕНИЕ НА СОБЕСЕДОВАНИЕ ===
    public void sendInterviewInvite(String email, String applicantName) {
        String subject = "📅 Приглашение на собеседование в HIWork";
        String html = "<div style='font-family: sans-serif; padding: 20px;'>" +
                "<h2 style='color: #667eea;'>Здравствуйте, " + applicantName + "!</h2>" +
                "<p>Мы рассмотрили вашу заявку и хотели бы пригласить вас на собеседование.</p>" +
                "<p><b>Детали собеседования:</b></p>" +
                "<ul>" +
                "<li>Формат: Онлайн (Zoom/Teams)</li>" +
                "<li>Длительность: 30-45 минут</li>" +
                "<li>Что взять с собой: Ноутбук, вопросы к нам</li>" +
                "</ul>" +
                "<p>Мы свяжемся с вами в ближайшее время для согласования времени.</p>" +
                "<p>С уважением,<br>Команда HIWork 💙</p>" +
                "</div>";

        sendEmail(email, subject, html);
    }

    // === ПОДТВЕРЖДЕНИЕ НАЙМА ===
    public void sendHireConfirmation(String email, String applicantName, String careerTrack, String qrCode) {
        String subject = "🎉 Поздравляем! Вы приняты в HIWork!";
        String html = "<div style='font-family: sans-serif; padding: 20px;'>" +
                "<h2 style='color: #667eea;'>Поздравляем, " + applicantName + "!</h2>" +
                "<p>Мы рады сообщить, что ваша заявка одобрена! Вы приняты в команду HIWork!</p>" +
                "<p><b>Ваша позиция:</b> " + careerTrack + "</p>" +
                "<p><b>Ваш QR-код для доступа:</b></p>" +
                "<div style='background: #f0f0f0; padding: 20px; text-align: center; margin: 20px 0;'>" +
                "<span style='font-size: 32px; color: #667eea; font-weight: bold;'>" + qrCode + "</span>" +
                "</div>" +
                "<p>Сохраните этот код — он понадобится при первом входе в систему.</p>" +
                "<p>Добро пожаловать в команду! 🚀</p>" +
                "<p>С уважением,<br>Команда HIWork 💙</p>" +
                "</div>";

        sendEmail(email, subject, html);
    }

    // === УВЕДОМЛЕНИЕ ОБ ОТКАЗЕ ===
    public void notifyRejected(String candidateEmail, String candidateName) {
        String subject = "Решение по вашей заявке в HIWork";
        String html = "<div style='font-family: sans-serif; padding: 20px;'>" +
                "<h2>Здравствуйте, " + candidateName + "!</h2>" +
                "<p>Благодарим за интерес к HIWork.</p>" +
                "<p>К сожалению, на данный момент мы не готовы предложить вам позицию.</p>" +
                "<p>Мы сохраним ваше резюме и свяжемся, если появятся подходящие вакансии.</p>" +
                "<p>Удачи в поиске! 💙</p>" +
                "</div>";

        sendEmail(candidateEmail, subject, html);
    }

    public void notifyNewApplication(String hrEmailParam, String applicantName, String applicantEmail, String careerTrack) {
        notifyHrAboutNewApplicant(applicantName, applicantEmail, careerTrack);
    }
}

package com.example.demo.service;

import okhttp3.*;
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

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private final OkHttpClient client = new OkHttpClient();

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

    public void notifyNewApplication(String hrEmail, String applicantName, String applicantEmail, String careerTrack) {
        String subject = "🆕 Новая заявка: " + applicantName;
        String html = "<h2>Поступила новая заявка!</h2>" +
                "<p><b>ФИО:</b> " + applicantName + "</p>" +
                "<p><b>Email:</b> " + applicantEmail + "</p>" +
                "<p><b>Карьерный трек:</b> " + careerTrack + "</p>" +
                "<p>Откройте приложение HIWork для обработки заявки.</p>";
        sendEmail(hrEmail, subject, html);
    }

    public void notifyHired(String candidateEmail, String candidateName, String qrCode) {
        String subject = "🎉 Поздравляем! Вы приняты в HIWork!";
        String html = "<h2>Поздравляем, " + candidateName + "!</h2>" +
                "<p>Ваша заявка одобрена. Вы приняты в команду HIWork!</p>" +
                "<p><b>Ваш QR-код для доступа:</b> <span style='font-size:24px; color:#667eea;'>" + qrCode + "</span></p>" +
                "<p>Сохраните этот код — он понадобится при входе.</p>" +
                "<p>Добро пожаловать в команду! 🚀</p>";
        sendEmail(candidateEmail, subject, html);
    }

    public void notifyRejected(String candidateEmail, String candidateName) {
        String subject = "Решение по вашей заявке в HIWork";
        String html = "<h2>Здравствуйте, " + candidateName + "!</h2>" +
                "<p>Благодарим за интерес к HIWork.</p>" +
                "<p>К сожалению, на данный момент мы не готовы предложить вам позицию.</p>" +
                "<p>Мы сохраним ваше резюме и свяжемся, если появятся подходящие вакансии.</p>" +
                "<p>Удачи в поиске! 💙</p>";
        sendEmail(candidateEmail, subject, html);
    }
}

package com.example.demo.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@CrossOrigin(origins = {"https://hiwork-hiring-site.onrender.com", "http://localhost:5500", "*"})
@RestController
@RequestMapping("/api/qr")
public class QrController {

    @GetMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] generateQr(
            @RequestParam String code,
            @RequestParam(defaultValue = "250") int size) {
        try {
            if (code == null || code.isEmpty()) {
                throw new IllegalArgumentException("Код не может быть пустым");
            }

            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix matrix = qrWriter.encode(code, BarcodeFormat.QR_CODE, size, size);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            System.err.println("❌ QR generation error: " + e.getMessage());
            return new byte[0];
        }
    }
}

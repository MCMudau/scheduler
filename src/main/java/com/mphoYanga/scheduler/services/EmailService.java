package com.mphoYanga.scheduler.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Send a verification PIN email to the specified address.
     *
     * @param toEmail     recipient email address
     * @param adminName   admin's first name (for personalisation)
     * @param pin         the 5-digit verification PIN
     */
    public void sendVerificationPin(String toEmail, String adminName, String pin) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Mpho Yanga Construction — Your Verification PIN");
            helper.setFrom("noreply@mphoyanga.co.zw");

            String html = buildPinEmailHtml(adminName, pin);
            helper.setText(html, true); // true = HTML content

            new Thread(()->{
                mailSender.send(message);
            }).start();


        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email to " + toEmail, e);
        }
    }

    /**
     * Build the branded HTML email body.
     */
    private String buildPinEmailHtml(String name, String pin) {
        // Split PIN into individual digits for the styled display
        String[] digits = pin.split("");

        StringBuilder digitBoxes = new StringBuilder();
        for (String digit : digits) {
            digitBoxes.append(
                "<span style='" +
                "display:inline-block;" +
                "width:48px;height:56px;line-height:56px;" +
                "border:2px solid #e8762a;" +
                "border-radius:10px;" +
                "font-size:28px;font-weight:700;" +
                "font-family:monospace;" +
                "color:#1a1a1a;" +
                "text-align:center;" +
                "margin:0 4px;" +
                "background:#fff8f3;" +
                "'>" + digit + "</span>"
            );
        }

        return "<!DOCTYPE html>" +
            "<html><head><meta charset='UTF-8'></head><body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,sans-serif;'>" +
            "<div style='max-width:560px;margin:40px auto;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 40px rgba(0,0,0,0.1);'>" +

            // Header
            "<div style='background:linear-gradient(135deg,#0d1b2a,#1a2e47);padding:32px 40px;text-align:center;'>" +
            "<div style='display:inline-flex;gap:8px;margin-bottom:12px;'>" +
            "<span style='background:#1a5fad;width:32px;height:32px;border-radius:7px;display:inline-block;line-height:32px;font-size:16px;'>🔨</span>" +
            "<span style='background:#e8762a;width:32px;height:32px;border-radius:7px;display:inline-block;line-height:32px;font-size:16px;'>🧱</span>" +
            "<span style='background:#3cb54a;width:32px;height:32px;border-radius:7px;display:inline-block;line-height:32px;font-size:16px;'>🖌️</span>" +
            "</div>" +
            "<h1 style='color:#ffffff;font-size:22px;margin:0;letter-spacing:1px;'>MPHO YANGA CONSTRUCTION</h1>" +
            "<p style='color:rgba(255,255,255,0.5);font-size:11px;letter-spacing:3px;margin:4px 0 0;text-transform:uppercase;'>Admin Portal</p>" +
            "</div>" +

            // Body
            "<div style='padding:40px;'>" +
            "<p style='font-size:16px;color:#333;'>Hi <strong>" + name + "</strong>,</p>" +
            "<p style='font-size:15px;color:#555;line-height:1.7;'>Your admin account has been successfully created. Use the verification PIN below to activate your account:</p>" +

            // PIN display
            "<div style='text-align:center;margin:36px 0;'>" +
            "<p style='font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#888;margin-bottom:16px;'>Your Verification PIN</p>" +
            digitBoxes +
            "</div>" +

            "<div style='background:#fff8f3;border:1px solid #fde0c5;border-radius:10px;padding:16px 20px;margin-bottom:24px;'>" +
            "<p style='margin:0;font-size:13px;color:#c45e18;'>⚠️ This PIN is valid for <strong>24 hours</strong>. Do not share it with anyone.</p>" +
            "</div>" +

            "<p style='font-size:14px;color:#555;line-height:1.7;'>If you did not create this account, please contact us immediately at <a href='mailto:admin@mphoyanga.co.zw' style='color:#1a5fad;'>admin@mphoyanga.co.zw</a>.</p>" +
            "</div>" +

            // Footer
            "<div style='background:#f8f8f8;border-top:1px solid #eee;padding:20px 40px;text-align:center;'>" +
            "<p style='font-size:12px;color:#aaa;margin:0;'>© 2025 Mpho Yanga Construction · BP No. 0200269091 · Zimbabwe</p>" +
            "</div>" +

            "</div></body></html>";
    }

    /**
     * Send a verification PIN email to a newly registered client.
     */
    public void sendClientVerificationPin(String toEmail, String clientName, String pin) {
        sendMail(toEmail, "Mpho Yanga Construction — Verify Your Account",
                buildPinEmailHtml(clientName, pin, "Client Portal", "#3cb54a"));
    }

    // ── Shared mail sender ────────────────────────────────────────────

    private void sendMail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setFrom("noreply@mphoyanga.co.zw");
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + toEmail, e);
        }
    }

    // ── Email HTML builder ────────────────────────────────────────────

    /**
     * Build a branded HTML email with individual PIN digit boxes.
     *
     * @param name        recipient first name
     * @param pin         5-digit PIN string
     * @param portalLabel e.g. "Admin Portal" or "Client Portal"
     * @param accentColor hex color for the accent (e.g. "#3cb54a")
     */
    private String buildPinEmailHtml(String name, String pin, String portalLabel, String accentColor) {
        StringBuilder digitBoxes = new StringBuilder();
        for (char digit : pin.toCharArray()) {
            digitBoxes.append(
                    "<span style='" +
                            "display:inline-block;" +
                            "width:48px;height:56px;line-height:56px;" +
                            "border:2px solid " + accentColor + ";" +
                            "border-radius:10px;" +
                            "font-size:28px;font-weight:700;" +
                            "font-family:monospace;" +
                            "color:#1a1a1a;" +
                            "text-align:center;" +
                            "margin:0 4px;" +
                            "background:#f6fff8;" +
                            "'>" + digit + "</span>"
            );
        }

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,sans-serif;'>" +
                "<div style='max-width:560px;margin:40px auto;background:#ffffff;border-radius:16px;" +
                "overflow:hidden;box-shadow:0 8px 40px rgba(0,0,0,0.1);'>" +

                // Header
                "<div style='background:linear-gradient(135deg,#0d1b2a,#1a2e47);padding:32px 40px;text-align:center;'>" +
                "<div style='display:inline-flex;gap:8px;margin-bottom:12px;'>" +
                "<span style='background:#1a5fad;width:32px;height:32px;border-radius:7px;display:inline-block;line-height:32px;font-size:16px;'>🔨</span>" +
                "<span style='background:#e8762a;width:32px;height:32px;border-radius:7px;display:inline-block;line-height:32px;font-size:16px;'>🧱</span>" +
                "<span style='background:#3cb54a;width:32px;height:32px;border-radius:7px;display:inline-block;line-height:32px;font-size:16px;'>🖌️</span>" +
                "</div>" +
                "<h1 style='color:#ffffff;font-size:22px;margin:0;letter-spacing:1px;'>MPHO YANGA CONSTRUCTION</h1>" +
                "<p style='color:rgba(255,255,255,0.5);font-size:11px;letter-spacing:3px;margin:4px 0 0;text-transform:uppercase;'>" + portalLabel + "</p>" +
                "</div>" +

                // Body
                "<div style='padding:40px;'>" +
                "<p style='font-size:16px;color:#333;'>Hi <strong>" + name + "</strong>,</p>" +
                "<p style='font-size:15px;color:#555;line-height:1.7;'>" +
                "Your account has been created. Enter the verification PIN below to activate it:</p>" +

                // PIN display
                "<div style='text-align:center;margin:36px 0;'>" +
                "<p style='font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#888;margin-bottom:16px;'>Your Verification PIN</p>" +
                digitBoxes +
                "</div>" +

                "<div style='background:#f0fff4;border:1px solid #c6f6d5;border-radius:10px;padding:16px 20px;margin-bottom:24px;'>" +
                "<p style='margin:0;font-size:13px;color:#276749;'>⚠️ This PIN is valid for <strong>24 hours</strong>. Do not share it with anyone.</p>" +
                "</div>" +

                "<p style='font-size:14px;color:#555;line-height:1.7;'>If you did not create this account, please contact us immediately at " +
                "<a href='mailto:admin@mphoyanga.co.zw' style='color:#1a5fad;'>admin@mphoyanga.co.zw</a>.</p>" +
                "</div>" +

                // Footer
                "<div style='background:#f8f8f8;border-top:1px solid #eee;padding:20px 40px;text-align:center;'>" +
                "<p style='font-size:12px;color:#aaa;margin:0;'>© 2025 Mpho Yanga Construction · BP No. 0200269091 · Zimbabwe</p>" +
                "</div>" +

                "</div></body></html>";
    }
}

package com.mphoYanga.scheduler.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Dedicated email service for the forgot-password / PIN reset flow.
 * Kept separate from EmailService so concerns stay isolated and
 * the template can evolve independently.
 */
@Service
public class PasswordResetEmailService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private static final String FROM_EMAIL    = "mudaumuthusi@gmail.com";
    private static final String FROM_NAME     = "Mpho Yanga Construction";
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Internal HTTP sender ──────────────────────────────────────────────────

    private void sendMail(String toEmail, String toName, String subject, String htmlBody) {
        try {
            log.info("=== PASSWORD RESET EMAIL → {}", toEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", brevoApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String body = String.format("""
                {
                    "sender": {"name": "%s", "email": "%s"},
                    "to": [{"email": "%s", "name": "%s"}],
                    "subject": "%s",
                    "htmlContent": %s
                }
                """,
                    FROM_NAME, FROM_EMAIL,
                    toEmail, toName,
                    subject,
                    objectMapper.writeValueAsString(htmlBody)
            );

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);

            log.info("=== PASSWORD RESET EMAIL SENT → {} | Status: {}", toEmail, response.getStatusCode());

        } catch (Exception e) {
            log.error("=== PASSWORD RESET EMAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Password reset mail failed: " + e.getMessage(), e);
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Send a branded password-reset PIN email.
     *
     * @param toEmail     recipient email address
     * @param name        user's first name
     * @param pin         5-digit reset PIN
     * @param portalLabel e.g. "Admin Portal" or "Client Portal"
     * @param accentColor hex colour for portal accent band, e.g. "#e8762a"
     */
    public void sendPasswordResetPin(String toEmail, String name,
                                     String pin, String portalLabel, String accentColor) {
        sendMail(
                toEmail,
                name,
                "Mpho Yanga Construction — Password Reset PIN",
                buildResetEmailHtml(name, pin, portalLabel, accentColor)
        );
    }

    // ── HTML template builder ─────────────────────────────────────────────────

    private String buildResetEmailHtml(String name, String pin,
                                        String portalLabel, String accentColor) {

        // Build individual digit boxes
        StringBuilder digitBoxes = new StringBuilder();
        for (char digit : pin.toCharArray()) {
            digitBoxes.append(
                    "<span style='" +
                    "display:inline-block;" +
                    "width:44px;height:52px;line-height:52px;" +
                    "font-size:28px;font-weight:700;" +
                    "font-family:monospace;" +
                    "color:#1a1a1a;" +
                    "text-align:center;" +
                    "margin:0 4px;" +
                    "background:#fff8f0;" +
                    "border:2px solid " + accentColor + ";" +
                    "border-radius:8px;" +
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
                "<span style='background:#1a5fad;width:32px;height:32px;border-radius:7px;" +
                "display:inline-block;line-height:32px;font-size:16px;'>🔨</span>" +
                "<span style='background:#e8762a;width:32px;height:32px;border-radius:7px;" +
                "display:inline-block;line-height:32px;font-size:16px;'>🧱</span>" +
                "<span style='background:#3cb54a;width:32px;height:32px;border-radius:7px;" +
                "display:inline-block;line-height:32px;font-size:16px;'>🖌️</span>" +
                "</div>" +
                "<h1 style='color:#ffffff;font-size:22px;margin:0;letter-spacing:1px;'>MPHO YANGA CONSTRUCTION</h1>" +
                "<p style='color:rgba(255,255,255,0.5);font-size:11px;letter-spacing:3px;" +
                "margin:4px 0 0;text-transform:uppercase;'>" + portalLabel + "</p>" +
                "</div>" +

                // Warning band
                "<div style='background:#fff3cd;border-bottom:3px solid " + accentColor + ";padding:16px 40px;" +
                "display:flex;align-items:center;gap:12px;'>" +
                "<span style='font-size:24px;'>🔐</span>" +
                "<div>" +
                "<div style='font-size:15px;font-weight:700;color:#7c4a00;'>Password Reset Request</div>" +
                "<div style='font-size:12px;color:#9a5f00;margin-top:2px;'>" +
                "A password reset was requested for your account.</div>" +
                "</div>" +
                "</div>" +

                // Body
                "<div style='padding:40px;'>" +
                "<p style='font-size:16px;color:#333;'>Hi <strong>" + name + "</strong>,</p>" +
                "<p style='font-size:15px;color:#555;line-height:1.7;'>" +
                "We received a request to reset the password for your Mpho Yanga Construction account. " +
                "Use the PIN below to proceed. This PIN expires in <strong>15 minutes</strong>.</p>" +

                // PIN display
                "<div style='text-align:center;margin:36px 0;'>" +
                "<p style='font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#888;" +
                "margin-bottom:16px;'>Your Password Reset PIN</p>" +
                digitBoxes +
                "</div>" +

                // Warning box
                "<div style='background:#fff8f0;border:1px solid " + accentColor + ";border-radius:10px;" +
                "padding:16px 20px;margin-bottom:24px;'>" +
                "<p style='margin:0;font-size:13px;color:#7c4a00;'>" +
                "⚠️ This PIN is valid for <strong>15 minutes only</strong>. " +
                "If you did not request a password reset, you can safely ignore this email — " +
                "your account remains secure.</p>" +
                "</div>" +

                "<p style='font-size:14px;color:#555;line-height:1.7;'>" +
                "If you need help, contact us at " +
                "<a href='mailto:admin@mphoyanga.co.zw' style='color:#1a5fad;'>admin@mphoyanga.co.zw</a>.</p>" +
                "</div>" +

                // Footer
                "<div style='background:#f8f8f8;border-top:1px solid #eee;padding:20px 40px;text-align:center;'>" +
                "<p style='font-size:12px;color:#aaa;margin:0;'>" +
                "© 2025 Mpho Yanga Construction · BP No. 0200269091 · Zimbabwe</p>" +
                "</div>" +

                "</div></body></html>";
    }
}

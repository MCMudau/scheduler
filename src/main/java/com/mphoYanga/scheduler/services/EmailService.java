package com.mphoYanga.scheduler.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String FROM_EMAIL = "mudaumuthusi@gmail.com";
    private static final String FROM_NAME  = "Mpho Yanga Construction";
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Shared HTTP API sender ────────────────────────────────────────

    private void sendMail(String toEmail, String toName, String subject, String htmlBody) {
        try {
            log.info("=== ATTEMPTING TO SEND EMAIL TO: {}", toEmail);

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
                    FROM_NAME,
                    FROM_EMAIL,
                    toEmail,
                    toName,
                    subject,
                    objectMapper.writeValueAsString(htmlBody)
            );

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    BREVO_API_URL,
                    request,
                    String.class
            );

            log.info("=== EMAIL SENT SUCCESSFULLY TO: {} | Status: {}", toEmail, response.getStatusCode());

        } catch (Exception e) {
            log.error("=== MAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Mail failed: " + e.getMessage(), e);
        }
    }

    // ── Public sending methods ────────────────────────────────────────

    /**
     * Send a verification PIN email to a newly registered admin.
     *
     * @param toEmail     recipient email address
     * @param adminName   admin's first name (for personalisation)
     * @param pin         the 5-digit verification PIN
     */
    public void sendVerificationPin(String toEmail, String adminName, String pin) {
        sendMail(
                toEmail,
                adminName,
                "Mpho Yanga Construction — Your Verification PIN",
                buildPinEmailHtml(adminName, pin, "Admin Portal", "#e8762a")
        );
    }

    /**
     * Send a verification PIN email to a newly registered client.
     *
     * @param toEmail      recipient email address
     * @param clientName   client's first name (for personalisation)
     * @param pin          the 5-digit verification PIN
     */
    public void sendClientVerificationPin(String toEmail, String clientName, String pin) {
        sendMail(
                toEmail,
                clientName,
                "Mpho Yanga Construction — Verify Your Account",
                buildPinEmailHtml(clientName, pin, "Client Portal", "#3cb54a")
        );
    }

    /**
     * Send a quotation confirmation email with the PDF attached.
     *
     * @param toEmail         recipient email address
     * @param clientName      client's first name
     * @param quotationNumber quotation reference number
     * @param projectTitle    project title
     * @param totalAmount     formatted total amount string
     * @param pdfBytes        PDF file bytes to attach
     * @param pdfFileName     filename for the attachment
     */
    public void sendQuotationConfirmed(String toEmail, String clientName,
                                       String quotationNumber, String projectTitle,
                                       String totalAmount,
                                       byte[] pdfBytes, String pdfFileName) {
        try {
            log.info("=== ATTEMPTING TO SEND QUOTATION EMAIL TO: {}", toEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", brevoApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Base64 encode the PDF attachment
            String base64Pdf = java.util.Base64.getEncoder().encodeToString(pdfBytes);
            String htmlContent = buildQuotationConfirmedHtml(clientName, quotationNumber,
                    projectTitle, totalAmount);

            String body = String.format("""
                {
                    "sender": {"name": "%s", "email": "%s"},
                    "to": [{"email": "%s", "name": "%s"}],
                    "subject": "Mpho Yanga Construction — Your Quotation Has Been Confirmed",
                    "htmlContent": %s,
                    "attachment": [
                        {
                            "content": "%s",
                            "name": "%s"
                        }
                    ]
                }
                """,
                    FROM_NAME,
                    FROM_EMAIL,
                    toEmail,
                    clientName,
                    objectMapper.writeValueAsString(htmlContent),
                    base64Pdf,
                    pdfFileName
            );

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    BREVO_API_URL,
                    request,
                    String.class
            );

            log.info("=== QUOTATION EMAIL SENT SUCCESSFULLY TO: {} | Status: {}", toEmail, response.getStatusCode());

        } catch (Exception e) {
            log.error("=== QUOTATION MAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Quotation mail failed: " + e.getMessage(), e);
        }
    }

    public void sendQuotationRejected(String toEmail, String clientName,
                                      String quotationNumber, String projectTitle) {
        try {
            log.info("=== ATTEMPTING TO SEND REJECTION EMAIL TO: {}", toEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", brevoApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String htmlContent = buildQuotationRejectedHtml(clientName, quotationNumber, projectTitle);

            String body = String.format("""
                {
                    "sender": {"name": "%s", "email": "%s"},
                    "to": [{"email": "%s", "name": "%s"}],
                    "subject": "Mpho Yanga Construction — Update on Your Quotation",
                    "htmlContent": %s
                }
                """,
                    FROM_NAME, FROM_EMAIL, toEmail, clientName,
                    objectMapper.writeValueAsString(htmlContent)
            );

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);
            log.info("=== REJECTION EMAIL SENT TO: {} | Status: {}", toEmail, response.getStatusCode());

        } catch (Exception e) {
            log.error("=== REJECTION MAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Rejection mail failed: " + e.getMessage(), e);
        }
    }

    // ── Email HTML builders ───────────────────────────────────────────

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

    /**
     * Build the branded HTML email body for quotation confirmation.
     *
     * @param clientName      client's first name
     * @param quotationNumber quotation reference number
     * @param projectTitle    project title
     * @param totalAmount     formatted total amount string
     */
    private String buildQuotationConfirmedHtml(String clientName, String quotationNumber,
                                               String projectTitle, String totalAmount) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,sans-serif;'>" +
                "<div style='max-width:580px;margin:40px auto;background:#ffffff;border-radius:16px;" +
                "overflow:hidden;box-shadow:0 8px 40px rgba(0,0,0,0.1);'>" +

                // Header band
                "<div style='background:linear-gradient(135deg,#0d1b2a,#1a2e47);padding:32px 40px;text-align:center;'>" +
                "<div style='display:inline-flex;gap:8px;margin-bottom:12px;'>" +
                "<span style='background:#1a5fad;width:32px;height:32px;border-radius:7px;" +
                "display:inline-block;line-height:32px;font-size:16px;'>🔨</span>" +
                "<span style='background:#e8762a;width:32px;height:32px;border-radius:7px;" +
                "display:inline-block;line-height:32px;font-size:16px;'>🧱</span>" +
                "<span style='background:#3cb54a;width:32px;height:32px;border-radius:7px;" +
                "display:inline-block;line-height:32px;font-size:16px;'>🖌️</span>" +
                "</div>" +
                "<h1 style='color:#ffffff;font-size:22px;margin:0;letter-spacing:1px;'>" +
                "MPHO YANGA CONSTRUCTION</h1>" +
                "<p style='color:rgba(255,255,255,0.5);font-size:11px;letter-spacing:3px;" +
                "margin:4px 0 0;text-transform:uppercase;'>Client Portal</p>" +
                "</div>" +

                // Green confirmed banner
                "<div style='background:#ecfdf5;border-bottom:3px solid #3cb54a;padding:20px 40px;" +
                "display:flex;align-items:center;gap:14px;'>" +
                "<span style='font-size:28px;'>✅</span>" +
                "<div>" +
                "<div style='font-size:16px;font-weight:700;color:#065f46;'>Quotation Confirmed!</div>" +
                "<div style='font-size:13px;color:#047857;margin-top:2px;'>" +
                "Your quotation has been reviewed and confirmed by our team.</div>" +
                "</div>" +
                "</div>" +

                // Body
                "<div style='padding:36px 40px;'>" +
                "<p style='font-size:16px;color:#333;margin-bottom:20px;'>" +
                "Hi <strong>" + clientName + "</strong>,</p>" +
                "<p style='font-size:14px;color:#555;line-height:1.7;margin-bottom:24px;'>" +
                "We're pleased to confirm that your quotation has been reviewed and finalised. " +
                "Please find your confirmed quotation PDF attached to this email. " +
                "You can also log into your client portal to view and download it at any time." +
                "</p>" +

                // Quotation summary box
                "<div style='background:#f8fafd;border:1px solid #dde3f0;border-radius:12px;" +
                "padding:20px 24px;margin-bottom:24px;'>" +
                "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;" +
                "color:#7c8ba1;font-weight:700;margin-bottom:14px;'>Quotation Summary</div>" +

                "<div style='display:flex;justify-content:space-between;margin-bottom:10px;'>" +
                "<span style='font-size:13px;color:#7c8ba1;'>Quotation Number</span>" +
                "<span style='font-size:13px;font-weight:700;color:#1a2233;font-family:monospace;'>" +
                quotationNumber + "</span>" +
                "</div>" +

                "<div style='display:flex;justify-content:space-between;margin-bottom:10px;" +
                "padding-top:10px;border-top:1px solid #dde3f0;'>" +
                "<span style='font-size:13px;color:#7c8ba1;'>Project</span>" +
                "<span style='font-size:13px;font-weight:700;color:#1a2233;'>" + projectTitle + "</span>" +
                "</div>" +

                "<div style='display:flex;justify-content:space-between;padding-top:10px;" +
                "border-top:1px solid #dde3f0;'>" +
                "<span style='font-size:13px;color:#7c8ba1;'>Total Amount</span>" +
                "<span style='font-size:18px;font-weight:700;color:#1a5fad;'>" + totalAmount + "</span>" +
                "</div>" +
                "</div>" +

                // Next steps
                "<div style='background:#fffbeb;border:1px solid #fde68a;border-radius:10px;" +
                "padding:16px 20px;margin-bottom:24px;'>" +
                "<div style='font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:1px;" +
                "color:#92400e;margin-bottom:8px;'>Next Steps</div>" +
                "<ol style='margin:0;padding-left:18px;color:#78350f;font-size:13px;line-height:1.8;'>" +
                "<li>Review your quotation PDF (attached to this email)</li>" +
                "<li>A 50% deposit is required to commence work</li>" +
                "<li>Contact us to confirm your acceptance and arrange payment</li>" +
                "</ol>" +
                "</div>" +

                "<p style='font-size:14px;color:#555;line-height:1.7;'>" +
                "To accept this quotation or if you have any questions, please contact us at " +
                "<a href='mailto:admin@mphoyanga.co.zw' style='color:#1a5fad;font-weight:600;'>" +
                "admin@mphoyanga.co.zw</a> or reach us on WhatsApp." +
                "</p>" +
                "</div>" +

                // Footer
                "<div style='background:#f8f8f8;border-top:1px solid #eee;padding:20px 40px;" +
                "text-align:center;'>" +
                "<p style='font-size:12px;color:#aaa;margin:0;'>" +
                "© 2025 Mpho Yanga Construction · BP No. 0200269091 · Zimbabwe</p>" +
                "</div>" +

                "</div></body></html>";
    }

    private String buildQuotationRejectedHtml(String clientName, String quotationNumber,
                                               String projectTitle) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,sans-serif;'>" +
                "<div style='max-width:580px;margin:40px auto;background:#ffffff;border-radius:16px;" +
                "overflow:hidden;box-shadow:0 8px 40px rgba(0,0,0,0.1);'>" +

                "<div style='background:linear-gradient(135deg,#0d1b2a,#1a2e47);padding:32px 40px;text-align:center;'>" +
                "<div style='display:inline-flex;gap:8px;margin-bottom:12px;'>" +
                "<span style='background:#1a5fad;width:32px;height:32px;border-radius:7px;display:inline-block;line-height:32px;font-size:16px;'>🔨</span>" +
                "<span style='background:#e8762a;width:32px;height:32px;border-radius:7px;display:inline-block;line-height:32px;font-size:16px;'>🧱</span>" +
                "<span style='background:#3cb54a;width:32px;height:32px;border-radius:7px;display:inline-block;line-height:32px;font-size:16px;'>🖌️</span>" +
                "</div>" +
                "<h1 style='color:#ffffff;font-size:22px;margin:0;letter-spacing:1px;'>MPHO YANGA CONSTRUCTION</h1>" +
                "<p style='color:rgba(255,255,255,0.5);font-size:11px;letter-spacing:3px;margin:4px 0 0;text-transform:uppercase;'>Client Portal</p>" +
                "</div>" +

                "<div style='background:#fff5f5;border-bottom:3px solid #e53e3e;padding:20px 40px;display:flex;align-items:center;gap:14px;'>" +
                "<span style='font-size:28px;'>📋</span>" +
                "<div>" +
                "<div style='font-size:16px;font-weight:700;color:#742a2a;'>Quotation Not Approved</div>" +
                "<div style='font-size:13px;color:#c53030;margin-top:2px;'>We were unable to proceed with this quotation at this time.</div>" +
                "</div>" +
                "</div>" +

                "<div style='padding:36px 40px;'>" +
                "<p style='font-size:16px;color:#333;margin-bottom:20px;'>Hi <strong>" + clientName + "</strong>,</p>" +
                "<p style='font-size:14px;color:#555;line-height:1.7;margin-bottom:24px;'>" +
                "Thank you for submitting your quotation request. After careful review, we are unable to " +
                "proceed with quotation <strong>" + quotationNumber + "</strong> for <strong>" + projectTitle + "</strong> at this time.</p>" +

                "<div style='background:#fff5f5;border:1px solid #fed7d7;border-radius:10px;padding:18px 20px;margin-bottom:24px;'>" +
                "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#c53030;font-weight:700;margin-bottom:8px;'>Quotation Details</div>" +
                "<div style='font-size:13px;color:#4a5568;'><strong>Reference:</strong> " + quotationNumber + "</div>" +
                "<div style='font-size:13px;color:#4a5568;margin-top:4px;'><strong>Project:</strong> " + projectTitle + "</div>" +
                "</div>" +

                "<p style='font-size:14px;color:#555;line-height:1.7;margin-bottom:24px;'>" +
                "We encourage you to reach out to our team if you would like to discuss your requirements " +
                "further or submit a revised quotation. We value your interest and hope to work with you in the future.</p>" +

                "<div style='background:#f0f6ff;border:1px solid #bee3f8;border-radius:10px;padding:18px 20px;margin-bottom:24px;'>" +
                "<div style='font-size:13px;font-weight:700;color:#2c5282;margin-bottom:8px;'>Contact Us</div>" +
                "<div style='font-size:13px;color:#4a5568;'>📧 <a href='mailto:mudaumuthusi@gmail.com' style='color:#1a5fad;'>mudaumuthusi@gmail.com</a></div>" +
                "</div>" +
                "</div>" +

                "<div style='background:#f8f8f8;border-top:1px solid #eee;padding:20px 40px;text-align:center;'>" +
                "<p style='font-size:12px;color:#aaa;margin:0;'>© 2025 Mpho Yanga Construction · BP No. 0200269091 · Zimbabwe</p>" +
                "</div>" +
                "</div></body></html>";
    }
}
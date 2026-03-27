package com.mphoYanga.scheduler.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;


@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final String FROM_EMAIL = "mudaumuthusi@gmail.com";
    private static final String FROM_NAME  = "Mpho Yanga Construction";

    // ── Shared mail sender ────────────────────────────────────────────

    private void sendMail(String toEmail, String subject, String htmlBody) {
        try {
            log.info("=== ATTEMPTING TO SEND EMAIL TO: {}", toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setFrom(FROM_EMAIL, FROM_NAME);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("=== EMAIL SENT SUCCESSFULLY TO: {}", toEmail);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("=== MAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email to " + toEmail, e);
        } catch (MailException e) {
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
                "Mpho Yanga Construction — Your Verification PIN",
                buildPinEmailHtml(adminName, pin, "Admin Portal", "#e8762a")
        );
    }

    /**
     * Send a verification PIN email to a newly registered client.
     *
     * @param toEmail     recipient email address
     * @param clientName  client's first name (for personalisation)
     * @param pin         the 5-digit verification PIN
     */
    public void sendClientVerificationPin(String toEmail, String clientName, String pin) {
        sendMail(
                toEmail,
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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Mpho Yanga Construction — Your Quotation Has Been Confirmed");
            helper.setFrom(FROM_EMAIL, FROM_NAME);
            helper.setText(buildQuotationConfirmedHtml(clientName, quotationNumber,
                    projectTitle, totalAmount), true);
            helper.addAttachment(pdfFileName,
                    new org.springframework.core.io.ByteArrayResource(pdfBytes),
                    "application/pdf");
            mailSender.send(message);
            log.info("=== QUOTATION EMAIL SENT SUCCESSFULLY TO: {}", toEmail);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("=== QUOTATION MAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send quotation email to " + toEmail, e);
        } catch (MailException e) {
            log.error("=== QUOTATION MAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Quotation mail failed: " + e.getMessage(), e);
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
}
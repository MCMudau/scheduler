package com.mphoYanga.scheduler.services;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String FROM_NAME = "Mpho Yanga Construction";

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final JavaMailSender mailSender;

    // Cached once on first use — loading from classpath each email is wasteful
    private String logoBase64 = null;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ── Logo ──────────────────────────────────────────────────────────

    private String logoImgTag() {
        if (logoBase64 == null) {
            try {
                ClassPathResource res = new ClassPathResource("static/Screenshot 2026-06-03 182310.png");
                byte[] bytes = res.getInputStream().readAllBytes();
                logoBase64 = Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                log.warn("Could not load logo image: {}", e.getMessage());
                logoBase64 = "";
            }
        }
        if (!logoBase64.isEmpty()) {
            return "<img src=\"data:image/png;base64," + logoBase64 + "\" " +
                   "alt=\"Mpho Yanga Construction\" style=\"height:72px;display:block;border:0;\">";
        }
        // Fallback: coloured boxes if image is missing
        return "<table cellpadding=\"0\" cellspacing=\"6\" border=\"0\"><tr>" +
               "<td style=\"background:#1a5fad;width:58px;height:58px;border-radius:10px;" +
               "text-align:center;vertical-align:middle;font-size:26px;\">&#128296;</td>" +
               "<td style=\"background:#e8762a;width:58px;height:58px;border-radius:10px;" +
               "text-align:center;vertical-align:middle;font-size:26px;\">&#129521;</td>" +
               "<td style=\"background:#3cb54a;width:58px;height:58px;border-radius:10px;" +
               "text-align:center;vertical-align:middle;font-size:26px;\">&#128396;</td>" +
               "</tr></table>";
    }

    // ── Shared header block (table-safe, works in all email clients) ──

    private String emailHeader(String rightTitle) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>" +
               "<body style=\"margin:0;padding:20px;background:#f0f2f5;font-family:Arial,sans-serif;\">" +
               "<table width=\"640\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\" " +
               "style=\"max-width:640px;background:#ffffff;border:1px solid #dde3f0;\">" +

               // ── Logo row ──────────────────────────────────────────────────
               "<tr><td style=\"padding:20px 24px 10px;\">" +
               "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>" +
               "<td style=\"vertical-align:bottom;\">" + logoImgTag() + "</td>" +
               "<td style=\"vertical-align:bottom;text-align:right;font-family:Arial,sans-serif;" +
               "font-size:26px;font-weight:300;color:#666;letter-spacing:3px;\">" + rightTitle + "</td>" +
               "</tr></table></td></tr>" +

               // ── Company name row ──────────────────────────────────────────
               "<tr><td style=\"padding:8px 24px 0;\">" +
               "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>" +
               "<td>" +
               "<span style=\"font-size:26px;font-weight:900;color:#1a1a1a;font-family:Arial,sans-serif;\">MPHO </span>" +
               "<span style=\"font-size:26px;font-weight:400;font-style:italic;color:#1a1a1a;\">YAN</span>" +
               "<span style=\"font-size:26px;font-weight:900;text-decoration:underline;color:#1a1a1a;\">GA</span><br>" +
               "<span style=\"font-size:13px;font-weight:700;letter-spacing:4px;color:#1a1a1a;" +
               "font-family:Arial,sans-serif;\">CONSTRUCTION</span>" +
               "</td>" +
               "<td style=\"text-align:right;vertical-align:top;font-size:12px;color:#555;" +
               "font-family:Arial,sans-serif;\"><b>BP No.</b>0200269091</td>" +
               "</tr></table></td></tr>" +

               // ── Blue divider ──────────────────────────────────────────────
               "<tr><td style=\"padding:10px 0 0;\">" +
               "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">" +
               "<tr><td style=\"height:3px;background:#1a5fad;font-size:0;line-height:0;\">&nbsp;</td></tr>" +
               "</table></td></tr>";
    }

    // ── Shared payment + thank-you footer ────────────────────────────

    private String paymentFooter() {
        return
               // ── Contact line ──────────────────────────────────────────────
               "<tr><td style=\"padding:14px 24px 0;font-size:12px;color:#555;font-family:Arial,sans-serif;" +
               "line-height:1.7;\">" +
               "If you have any questions, contact <b>Mpho Mudau</b><br>" +
               "&#128222; <b>0712332083 / 0771527368</b> &nbsp;|&nbsp; " +
               "&#9993; <a href=\"mailto:" + fromEmail + "\" style=\"color:#1a5fad;\">" + fromEmail + "</a>" +
               "</td></tr>" +

               // ── Payment methods ───────────────────────────────────────────
               "<tr><td style=\"padding:14px 24px;background:#f5f5f5;border-top:1px solid #e0e0e0;" +
               "font-size:12px;color:#444;font-family:Arial,sans-serif;line-height:1.8;\">" +
               "<b>Payment Methods</b><br>" +
               "We Accept Visa, Master Card, EcoCash.<br><br>" +
               "<b>Banking Details</b><br>" +
               "Mpho Yanga Investments<br>" +
               "Acc / ZB 4555470019200 ZiG<br>" +
               "4555470019405 US$" +
               "</td></tr>" +

               // ── Thank you ─────────────────────────────────────────────────
               "<tr><td style=\"background:#1a2233;padding:14px 24px;text-align:center;\">" +
               "<span style=\"color:#ffffff;font-size:13px;font-weight:700;letter-spacing:3px;" +
               "font-family:Arial,sans-serif;\">THANK YOU FOR YOUR BUSINESS!</span>" +
               "</td></tr>" +

               "</table></body></html>";
    }

    // ── SMTP senders ──────────────────────────────────────────────────

    private void sendMail(String toEmail, String subject, String htmlBody)
            throws Exception {
        log.info("=== SENDING EMAIL TO: {}", toEmail);
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
        helper.setFrom(fromEmail, FROM_NAME);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(msg);
        log.info("=== EMAIL SENT TO: {}", toEmail);
    }

    private void sendMailWithAttachment(String toEmail, String subject,
                                        String htmlBody, byte[] attachmentBytes,
                                        String attachmentFilename) throws Exception {
        log.info("=== SENDING EMAIL WITH ATTACHMENT TO: {}", toEmail);
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromEmail, FROM_NAME);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentBytes));
        mailSender.send(msg);
        log.info("=== EMAIL WITH ATTACHMENT SENT TO: {}", toEmail);
    }

    // ── Public API ────────────────────────────────────────────────────

    public void sendVerificationPin(String toEmail, String adminName, String pin) {
        try {
            sendMail(toEmail,
                    "Mpho Yanga Construction — Your Verification PIN",
                    buildPinEmailHtml(adminName, pin, "Admin Portal", "#e8762a"));
        } catch (Exception e) {
            log.error("=== VERIFICATION PIN MAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Mail failed: " + e.getMessage(), e);
        }
    }

    public void sendClientVerificationPin(String toEmail, String clientName, String pin) {
        try {
            sendMail(toEmail,
                    "Mpho Yanga Construction — Verify Your Account",
                    buildPinEmailHtml(clientName, pin, "Client Portal", "#3cb54a"));
        } catch (Exception e) {
            log.error("=== CLIENT VERIFICATION MAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Mail failed: " + e.getMessage(), e);
        }
    }

    public void sendQuotationConfirmed(String toEmail, String clientName,
                                       String quotationNumber, String projectTitle,
                                       String totalAmount,
                                       byte[] pdfBytes, String pdfFileName) {
        try {
            sendMailWithAttachment(toEmail,
                    "Mpho Yanga Construction — Your Quotation Has Been Confirmed",
                    buildQuotationConfirmedHtml(clientName, quotationNumber, projectTitle, totalAmount),
                    pdfBytes, pdfFileName);
        } catch (Exception e) {
            log.error("=== QUOTATION MAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Quotation mail failed: " + e.getMessage(), e);
        }
    }

    public void sendQuotationRejected(String toEmail, String clientName,
                                      String quotationNumber, String projectTitle) {
        try {
            sendMail(toEmail,
                    "Mpho Yanga Construction — Update on Your Quotation",
                    buildQuotationRejectedHtml(clientName, quotationNumber, projectTitle));
        } catch (Exception e) {
            log.error("=== REJECTION MAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Rejection mail failed: " + e.getMessage(), e);
        }
    }

    // ── HTML builders ─────────────────────────────────────────────────

    private String buildPinEmailHtml(String name, String pin, String portalLabel, String accentColor) {

        // Individual PIN digit boxes
        StringBuilder digits = new StringBuilder();
        for (char ch : pin.toCharArray()) {
            digits.append(
                "<td style=\"width:48px;height:56px;line-height:56px;border:2px solid ")
                .append(accentColor)
                .append(";border-radius:10px;font-size:28px;font-weight:700;font-family:monospace;")
                .append("color:#1a1a1a;text-align:center;background:#f6fff8;padding:0 4px;\">")
                .append(ch)
                .append("</td><td width=\"8\"></td>");
        }

        return emailHeader(portalLabel) +

               // ── Greeting ──────────────────────────────────────────────────
               "<tr><td style=\"padding:28px 24px 0;font-family:Arial,sans-serif;\">" +
               "<p style=\"font-size:15px;color:#333;margin:0 0 10px;\">Hi <b>" + name + "</b>,</p>" +
               "<p style=\"font-size:14px;color:#555;line-height:1.7;margin:0;\">" +
               "Your account has been created. Enter the verification PIN below to activate it:</p>" +
               "</td></tr>" +

               // ── PIN digits ────────────────────────────────────────────────
               "<tr><td style=\"padding:28px 24px;text-align:center;\">" +
               "<p style=\"font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#888;margin:0 0 16px;\">" +
               "Your Verification PIN</p>" +
               "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\"><tr>" +
               digits +
               "</tr></table>" +
               "</td></tr>" +

               // ── Warning ───────────────────────────────────────────────────
               "<tr><td style=\"padding:0 24px 20px;\">" +
               "<table width=\"100%\" cellpadding=\"14\" cellspacing=\"0\" border=\"0\" " +
               "style=\"background:#f0fff4;border:1px solid #c6f6d5;border-radius:10px;\">" +
               "<tr><td style=\"font-size:13px;color:#276749;font-family:Arial,sans-serif;\">&#9888;&#65039; " +
               "This PIN is valid for <b>24 hours</b>. Do not share it with anyone.</td></tr>" +
               "</table></td></tr>" +

               "<tr><td style=\"padding:0 24px 20px;font-size:13px;color:#555;" +
               "font-family:Arial,sans-serif;line-height:1.7;\">If you did not create this account, " +
               "contact us at <a href=\"mailto:" + fromEmail + "\" style=\"color:#1a5fad;\">" + fromEmail + "</a>." +
               "</td></tr>" +

               paymentFooter();
    }

    private String buildQuotationConfirmedHtml(String clientName, String quotationNumber,
                                               String projectTitle, String totalAmount) {
        String today = new java.text.SimpleDateFormat("MMMM d, yyyy").format(new java.util.Date());

        return emailHeader("Quotation") +

               // ── Confirmed banner ──────────────────────────────────────────
               "<tr><td style=\"padding:0;\">" +
               "<table width=\"100%\" cellpadding=\"14\" cellspacing=\"0\" border=\"0\" " +
               "style=\"background:#ecfdf5;border-bottom:3px solid #3cb54a;\"><tr>" +
               "<td width=\"36\" style=\"font-size:24px;vertical-align:middle;\">&#9989;</td>" +
               "<td style=\"font-family:Arial,sans-serif;vertical-align:middle;\">" +
               "<b style=\"font-size:15px;color:#065f46;\">Quotation Confirmed!</b><br>" +
               "<span style=\"font-size:12px;color:#047857;\">Your quotation PDF is attached to this email.</span>" +
               "</td>" +
               "</tr></table></td></tr>" +

               // ── Bill To | Quote details ────────────────────────────────────
               "<tr><td style=\"padding:18px 24px 0;\">" +
               "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>" +

               // Left — Bill To
               "<td style=\"width:50%;vertical-align:top;font-family:Arial,sans-serif;\">" +
               "<div style=\"font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:1px;" +
               "color:#888;margin-bottom:6px;\">Bill To:</div>" +
               "<div style=\"font-size:14px;font-weight:700;color:#1a1a1a;\">" + clientName + "</div>" +
               "</td>" +

               // Right — Quotation meta
               "<td style=\"width:50%;vertical-align:top;text-align:right;font-family:Arial,sans-serif;" +
               "font-size:12px;color:#555;line-height:1.9;\">" +
               "<b>Quotation&nbsp;#:</b>&nbsp;<span style=\"font-family:monospace;color:#1a1a1a;\">" + quotationNumber + "</span><br>" +
               "<b>Date:</b>&nbsp;" + today + "<br>" +
               "<b>Project:</b>&nbsp;" + projectTitle +
               "</td></tr></table></td></tr>" +

               // ── Items table ───────────────────────────────────────────────
               "<tr><td style=\"padding:18px 0 0;\">" +
               "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">" +

               // Table header
               "<tr style=\"background:#1a5fad;\">" +
               "<th style=\"padding:10px 24px;text-align:left;color:#fff;font-size:11px;" +
               "letter-spacing:1px;text-transform:uppercase;font-family:Arial,sans-serif;" +
               "font-weight:700;\">Description</th>" +
               "<th style=\"padding:10px 24px;text-align:right;color:#fff;font-size:11px;" +
               "letter-spacing:1px;text-transform:uppercase;font-family:Arial,sans-serif;" +
               "font-weight:700;\">Amount</th>" +
               "</tr>" +

               // Item row
               "<tr style=\"background:#f8fafd;\">" +
               "<td style=\"padding:14px 24px;font-size:13px;color:#333;font-family:Arial,sans-serif;" +
               "border-bottom:1px solid #e8ecf0;\">" + projectTitle + "</td>" +
               "<td style=\"padding:14px 24px;font-size:13px;color:#333;font-family:Arial,sans-serif;" +
               "text-align:right;border-bottom:1px solid #e8ecf0;\">Incl.</td>" +
               "</tr>" +

               // Total row
               "<tr style=\"background:#eef3fc;\">" +
               "<td style=\"padding:12px 24px;text-align:right;font-weight:700;font-size:13px;" +
               "color:#1a5fad;font-family:Arial,sans-serif;border-top:2px solid #1a5fad;" +
               "letter-spacing:1px;\">TOTAL</td>" +
               "<td style=\"padding:12px 24px;font-weight:700;font-size:20px;color:#1a5fad;" +
               "font-family:Arial,sans-serif;text-align:right;border-top:2px solid #1a5fad;\">" +
               totalAmount + "</td>" +
               "</tr></table></td></tr>" +

               // ── Next steps ────────────────────────────────────────────────
               "<tr><td style=\"padding:18px 24px 0;\">" +
               "<table width=\"100%\" cellpadding=\"14\" cellspacing=\"0\" border=\"0\" " +
               "style=\"background:#fffbeb;border:1px solid #fde68a;border-radius:8px;\"><tr>" +
               "<td style=\"font-family:Arial,sans-serif;\">" +
               "<b style=\"font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#92400e;\">Next Steps</b><br><br>" +
               "<span style=\"font-size:13px;color:#78350f;line-height:1.9;\">" +
               "1. Review your quotation PDF (attached to this email)<br>" +
               "2. A 50% deposit is required to commence work<br>" +
               "3. Contact us to confirm acceptance and arrange payment" +
               "</span></td>" +
               "</tr></table></td></tr>" +

               "<tr><td height=\"10\"></td></tr>" +

               paymentFooter();
    }

    private String buildQuotationRejectedHtml(String clientName, String quotationNumber,
                                               String projectTitle) {
        return emailHeader("Quotation") +

               // ── Not approved banner ───────────────────────────────────────
               "<tr><td style=\"padding:0;\">" +
               "<table width=\"100%\" cellpadding=\"14\" cellspacing=\"0\" border=\"0\" " +
               "style=\"background:#fff5f5;border-bottom:3px solid #e53e3e;\"><tr>" +
               "<td width=\"36\" style=\"font-size:24px;vertical-align:middle;\">&#128203;</td>" +
               "<td style=\"font-family:Arial,sans-serif;vertical-align:middle;\">" +
               "<b style=\"font-size:15px;color:#742a2a;\">Quotation Not Approved</b><br>" +
               "<span style=\"font-size:12px;color:#c53030;\">We were unable to proceed with this quotation at this time.</span>" +
               "</td></tr></table></td></tr>" +

               // ── Body ──────────────────────────────────────────────────────
               "<tr><td style=\"padding:22px 24px 0;font-family:Arial,sans-serif;\">" +
               "<p style=\"font-size:15px;color:#333;margin:0 0 12px;\">Hi <b>" + clientName + "</b>,</p>" +
               "<p style=\"font-size:13px;color:#555;line-height:1.7;margin:0 0 18px;\">" +
               "Thank you for submitting your quotation request. After careful review, we are unable to " +
               "proceed with quotation <b>" + quotationNumber + "</b> for <b>" + projectTitle + "</b> at this time.</p>" +
               "</td></tr>" +

               // ── Quotation details box ─────────────────────────────────────
               "<tr><td style=\"padding:0 24px 18px;\">" +
               "<table width=\"100%\" cellpadding=\"14\" cellspacing=\"0\" border=\"0\" " +
               "style=\"background:#fff5f5;border:1px solid #fed7d7;border-radius:8px;\"><tr>" +
               "<td style=\"font-family:Arial,sans-serif;font-size:13px;color:#4a5568;line-height:1.8;\">" +
               "<b style=\"font-size:10px;text-transform:uppercase;letter-spacing:1px;color:#c53030;\">Quotation Details</b><br><br>" +
               "<b>Reference:</b> " + quotationNumber + "<br>" +
               "<b>Project:</b> " + projectTitle +
               "</td></tr></table></td></tr>" +

               "<tr><td style=\"padding:0 24px 18px;font-size:13px;color:#555;" +
               "font-family:Arial,sans-serif;line-height:1.7;\">" +
               "We encourage you to reach out if you'd like to discuss your requirements " +
               "further or submit a revised quotation. We value your interest and hope to work " +
               "with you in the future." +
               "</td></tr>" +

               paymentFooter();
    }
}

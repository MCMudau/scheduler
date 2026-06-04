package com.mphoYanga.scheduler.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class PasswordResetEmailService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailService.class);
    private static final String FROM_NAME = "Mpho Yanga Construction";

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final JavaMailSender mailSender;

    public PasswordResetEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private void sendMail(String toEmail, String toName, String subject, String htmlBody) {
        try {
            log.info("=== PASSWORD RESET EMAIL → {}", toEmail);
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(fromEmail, FROM_NAME);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
            log.info("=== PASSWORD RESET EMAIL SENT → {}", toEmail);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            log.error("=== PASSWORD RESET EMAIL FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Password reset mail failed: " + e.getMessage(), e);
        }
    }

    public void sendPasswordResetPin(String toEmail, String name,
                                     String pin, String portalLabel, String accentColor) {
        sendMail(
                toEmail,
                name,
                "Mpho Yanga Construction — Password Reset PIN",
                buildResetEmailHtml(name, pin, portalLabel, accentColor)
        );
    }

    private String buildResetEmailHtml(String name, String pin,
                                        String portalLabel, String accentColor) {
        StringBuilder digitBoxes = new StringBuilder();
        for (char digit : pin.toCharArray()) {
            digitBoxes
                .append("<td style=\"width:44px;height:52px;line-height:52px;border:2px solid ")
                .append(accentColor)
                .append(";border-radius:8px;font-size:28px;font-weight:700;font-family:monospace;")
                .append("color:#1a1a1a;text-align:center;background:#fff8f0;padding:0;\">")
                .append(digit)
                .append("</td><td width=\"8\"></td>");
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>" +
               "<body style=\"margin:0;padding:20px;background:#f0f2f5;font-family:Arial,sans-serif;\">" +
               "<table width=\"560\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\" " +
               "style=\"max-width:560px;background:#ffffff;border:1px solid #dde3f0;\">" +

               // Header
               "<tr><td style=\"background:linear-gradient(135deg,#0d1b2a,#1a2e47);padding:28px 32px;text-align:center;\">" +
               "<p style=\"color:#ffffff;font-size:20px;font-weight:900;letter-spacing:1px;margin:0;\">MPHO YANGA CONSTRUCTION</p>" +
               "<p style=\"color:rgba(255,255,255,0.5);font-size:11px;letter-spacing:3px;text-transform:uppercase;margin:4px 0 0;\">" +
               portalLabel + "</p></td></tr>" +

               // Warning band
               "<tr><td style=\"background:#fff3cd;border-bottom:3px solid " + accentColor + ";padding:14px 32px;\">" +
               "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>" +
               "<td style=\"font-size:22px;vertical-align:middle;padding-right:12px;\">&#128272;</td>" +
               "<td style=\"font-family:Arial,sans-serif;\">" +
               "<div style=\"font-size:14px;font-weight:700;color:#7c4a00;\">Password Reset Request</div>" +
               "<div style=\"font-size:12px;color:#9a5f00;margin-top:2px;\">A password reset was requested for your account.</div>" +
               "</td></tr></table></td></tr>" +

               // Body
               "<tr><td style=\"padding:32px;font-family:Arial,sans-serif;\">" +
               "<p style=\"font-size:15px;color:#333;margin:0 0 12px;\">Hi <b>" + name + "</b>,</p>" +
               "<p style=\"font-size:14px;color:#555;line-height:1.7;margin:0 0 24px;\">" +
               "Use the PIN below to reset your password. This PIN expires in <b>15 minutes</b>.</p>" +

               // PIN
               "<p style=\"font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#888;text-align:center;margin:0 0 14px;\">Your Password Reset PIN</p>" +
               "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\" style=\"margin-bottom:28px;\"><tr>" +
               digitBoxes +
               "</tr></table>" +

               // Warning box
               "<table width=\"100%\" cellpadding=\"14\" cellspacing=\"0\" border=\"0\" " +
               "style=\"background:#fff8f0;border:1px solid " + accentColor + ";border-radius:10px;margin-bottom:20px;\"><tr>" +
               "<td style=\"font-size:13px;color:#7c4a00;font-family:Arial,sans-serif;\">" +
               "&#9888;&#65039; This PIN is valid for <b>15 minutes only</b>. " +
               "If you did not request a reset, you can safely ignore this email.</td></tr></table>" +

               "<p style=\"font-size:13px;color:#555;line-height:1.7;margin:0;\">If you need help, contact us at " +
               "<a href=\"mailto:" + fromEmail + "\" style=\"color:#1a5fad;\">" + fromEmail + "</a>.</p>" +
               "</td></tr>" +

               // Footer
               "<tr><td style=\"background:#1a2233;padding:14px 24px;text-align:center;\">" +
               "<span style=\"color:#ffffff;font-size:12px;font-weight:700;letter-spacing:3px;font-family:Arial,sans-serif;\">THANK YOU FOR YOUR BUSINESS!</span>" +
               "</td></tr>" +

               "</table></body></html>";
    }
}

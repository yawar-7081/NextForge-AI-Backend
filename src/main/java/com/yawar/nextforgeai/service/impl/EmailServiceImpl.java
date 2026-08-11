package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.service.EmailService;
import com.yawar.nextforgeai.util.EmailTemplateUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${frontend.url}")
    private String FRONTEND_URL;

    private void sendHtmlEmail(String to, String subject, String htmlBody)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // true = HTML

        try{
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed To Send Mail - {}",subject);
            throw new RuntimeException(e);
        }
    }

    @Async
    @Override
    public void sendOtpEmail(String email, String otp) throws MessagingException {

        log.info("========== OTP Email Process Started ==========");
        log.info("Preparing OTP email for: {}", email);
        log.info("Generated OTP: {}", otp);

        String html = EmailTemplateUtil.loadTemplate("otp.html");
        log.debug("Email template loaded successfully.");

        html = html.replace("{{OTP}}", otp);

        log.debug("Placeholders replaced successfully.");
        log.debug("Name placeholder exists after replacement: {}", html.contains("{{name}}"));
        log.debug("OTP placeholder exists after replacement: {}", html.contains("{{otp}}"));

        log.info("Sending OTP email to: {}", email);

        sendHtmlEmail(email, "✨ NextForge AI - OTP Verification", html);

        log.info("OTP email sent successfully to {}", email);
        log.info("========== OTP Email Process Completed ==========");
    }

    @Async
    @Override
    public void sendRegisterSuccessfulEmail(String email) throws MessagingException {

        log.info("========== Registration Success Email Process Started ==========");
        log.info("Preparing registration success email for: {}", email);

        String html = EmailTemplateUtil.loadTemplate("registrationSuccessful.html");
        log.debug("Registration success email template loaded successfully.");

        html = html.replace("{{EMAIL}}", email);

        log.debug("Placeholders replaced successfully.");
        log.debug("Email placeholder exists after replacement: {}", html.contains("{{email}}"));

        log.info("Sending registration success email to: {}", email);

        sendHtmlEmail(email, "✨ NextForge AI - 🎉 Registration Successful", html);

        log.info("Registration success email sent successfully to {}", email);
        log.info("========== Registration Success Email Process Completed ==========");
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String email, String resetLink) throws MessagingException{
        log.info("========== Reset Password Email Process Started ==========");
        log.info("Preparing registration success email for: {}", email);
        log.info("Reset Link: {}",resetLink);

        String html = EmailTemplateUtil.loadTemplate("resetPassword.html");
        log.debug("Registration success email template loaded successfully.");

        html = html.replace("{{email}}", email);
        html = html.replace("{{RESET_URL}}",resetLink);
        log.debug("Placeholders replaced successfully.");
        log.debug("Email placeholder exists after replacement: {}", html.contains("{{email}}"));

        log.info("Sending Password Reset email to: {}", email);

        sendHtmlEmail(email, "✨ NextForge AI - Password Reset Link", html);

        log.info("Reset Password email sent successfully to {}", email);
        log.info("========== Reset Password Email Process Completed ==========");
    }

    @Async
    @Override
    public void sendPasswordResetSuccessfulEmail(String email) throws MessagingException {

        log.info("========== Password Reset Success Email Process Started ==========");
        log.info("Preparing password reset success email for: {}", email);

        String html = EmailTemplateUtil.loadTemplate("registrationSuccessful.html");
        log.debug("Password Reset success email template loaded successfully.");

        html = html.replace("{{LOGIN_URL}}", FRONTEND_URL+"/login");

        log.debug("Placeholders replaced successfully.");

        log.info("Sending password reset success email to: {}", email);

        sendHtmlEmail(email, "✨ NextForge AI - 🎉 Password Reset Successful", html);

        log.info("Password Reset success email sent successfully to {}", email);
        log.info("========== Registration Success Email Process Completed ==========");
    }

    @Override
    @Async
    public void sendMailToOwner(
            String eventTitle,
            String eventType,
            String eventMessage,
            String name,
            String username,
            String email,
            String userId,
            String details
    ) {

        try {

            log.info(
                    "Sending admin notification. eventType={}, userId={}",
                    eventType,
                    userId
            );

            String html = EmailTemplateUtil.loadTemplate(
                    "adminNotificationTemplate.html"
            );

            html = html
                    .replace("${EVENT_TITLE}", safe(eventTitle))
                    .replace("${EVENT_TYPE}", safe(eventType))
                    .replace("${EVENT_MESSAGE}", safe(eventMessage))
                    .replace("${NAME}", safe(name))
                    .replace("${USERNAME}", safe(username))
                    .replace("${EMAIL}", safe(email))
                    .replace("${USER_ID}", safe(userId))
                    .replace("${DETAILS}", safe(details))
                    .replace("${TIMESTAMP}", Instant.now().toString());

            sendHtmlEmail(
                    "mdyawarrkt@gmail.com",
                    "NextForge AI - " + eventTitle,
                    html
            );

            log.info(
                    "Admin notification sent successfully. eventType={}, userId={}",
                    eventType,
                    userId
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to send admin notification. eventType={}, userId={}",
                    eventType,
                    userId,
                    ex
            );
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}

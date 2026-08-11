package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.service.EmailService;
import com.yawar.nextforgeai.util.EmailTemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Value("${frontend.url}")
    private String FRONTEND_URL;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.sender-email}")
    private String brevoSenderEmail;

    @Value("${brevo.sender-name}")
    private String brevoSenderName;

    private final RestClient brevoClient = RestClient.builder()
            .baseUrl("https://api.brevo.com")
            .build();

    private void sendHtmlEmail(
            String to,
            String subject,
            String htmlBody
    ) {

        Map<String, Object> sender = new HashMap<>();
        sender.put("name", brevoSenderName);
        sender.put("email", brevoSenderEmail);

        Map<String, Object> recipient = new HashMap<>();
        recipient.put("email", to);

        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("sender", sender);
        requestBody.put("to", List.of(recipient));
        requestBody.put("subject", subject);
        requestBody.put("htmlContent", htmlBody);

        try {

            String response = brevoClient
                    .post()
                    .uri("/v3/smtp/email")
                    .header("api-key", brevoApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.info(
                    "Email sent successfully via Brevo. to={}, subject={}, response={}",
                    to,
                    subject,
                    response
            );

        } catch (Exception e) {

            log.error(
                    "Failed to send email via Brevo. to={}, subject={}",
                    to,
                    subject,
                    e
            );

            throw new RuntimeException(
                    "Failed to send email via Brevo",
                    e
            );
        }
    }

    @Async
    @Override
    public void sendOtpEmail(String email, String otp)  {

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
    public void sendRegisterSuccessfulEmail(String email)  {

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
    public void sendPasswordResetEmail(String email, String resetLink) {
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
    public void sendPasswordResetSuccessfulEmail(String email)  {

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
                    brevoSenderEmail,
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

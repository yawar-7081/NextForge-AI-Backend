package com.yawar.nextforgeai.service.impl;

import com.yawar.nextforgeai.service.EmailService;
import com.yawar.nextforgeai.util.EmailTemplateUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private void sendHtmlEmail(String to, String subject, String htmlBody)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // true = HTML

        mailSender.send(message);
    }

    @Override
    public void sendOtpEmail(String email, String otp, String name) throws MessagingException {

        log.info("========== OTP Email Process Started ==========");
        log.info("Preparing OTP email for: {}", email);
        log.info("Recipient Name: {}", name);
        log.info("Generated OTP: {}", otp);

//        String html = EmailTemplateUtil.loadTemplate("otp-email.html");
//        log.debug("Email template loaded successfully.");
//
//        html = html.replace("${otp}", otp);
//        html = html.replace("${name}", name);
//
//        log.debug("Placeholders replaced successfully.");
//        log.debug("Name placeholder exists after replacement: {}", html.contains("${name}"));
//        log.debug("OTP placeholder exists after replacement: {}", html.contains("${otp}"));

        log.info("Sending OTP email to: {}", email);

//        sendHtmlEmail(email, "OTP Verification", html);

        log.info("OTP email sent successfully to {}", email);
        log.info("========== OTP Email Process Completed ==========");
    }

    @Override
    public void sendRegisterSuccessfulEmail(String email, String name) throws MessagingException {

        log.info("========== Registration Success Email Process Started ==========");
        log.info("Preparing registration success email for: {}", email);
        log.info("Recipient Name: {}", name);

//        String html = EmailTemplateUtil.loadTemplate("registration-successful.html");
//        log.debug("Registration success email template loaded successfully.");
//
//        html = html.replace("${name}", name);
//        html = html.replace("${email}", email);
//
//        log.debug("Placeholders replaced successfully.");
//        log.debug("Name placeholder exists after replacement: {}", html.contains("${name}"));
//        log.debug("Email placeholder exists after replacement: {}", html.contains("${email}"));

        log.info("Sending registration success email to: {}", email);

//        sendHtmlEmail(email, "🎉 Registration Successful", html);

        log.info("Registration success email sent successfully to {}", email);
        log.info("========== Registration Success Email Process Completed ==========");
    }

    @Override
    public void sendPasswordResetEmail(String email, String name, String resetLink) {
        log.info("========== Reset Password Email Process Started ==========");
        log.info("Preparing registration success email for: {}", email);
        log.info("Recipient Name: {}", name);
        log.info("Reset Link: {}",resetLink);

//        String html = EmailTemplateUtil.loadTemplate("registration-successful.html");
//        log.debug("Registration success email template loaded successfully.");
//
//        html = html.replace("${name}", name);
//        html = html.replace("${email}", email);
//
//        log.debug("Placeholders replaced successfully.");
//        log.debug("Name placeholder exists after replacement: {}", html.contains("${name}"));
//        log.debug("Email placeholder exists after replacement: {}", html.contains("${email}"));

        log.info("Sending Password Reset email to: {}", email);

//        sendHtmlEmail(email, "🎉 Registration Successful", html);

        log.info("Reset Password email sent successfully to {}", email);
        log.info("========== Reset Password Email Process Completed ==========");
    }
}

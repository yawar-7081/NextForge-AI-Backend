package com.yawar.nextforgeai.service;


import org.springframework.messaging.MessagingException;

public interface EmailService {
    void sendOtpEmail(String email, String otp) throws MessagingException;
    void sendRegisterSuccessfulEmail(String email) throws MessagingException;
    void sendPasswordResetEmail(String email, String resetLink) throws MessagingException;
    void sendPasswordResetSuccessfulEmail(String email) throws MessagingException;

    void sendMailToOwner(
            String eventTitle,
            String eventType,
            String eventMessage,
            String name,
            String username,
            String email,
            String userId,
            String details
    );

}

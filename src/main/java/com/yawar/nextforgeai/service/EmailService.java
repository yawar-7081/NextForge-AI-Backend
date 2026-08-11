package com.yawar.nextforgeai.service;

import jakarta.mail.MessagingException;

public interface EmailService {
    void sendOtpEmail(String email, String otp) throws MessagingException;
    void sendRegisterSuccessfulEmail(String email) throws MessagingException;
    void sendPasswordResetEmail(String email, String resetLink) throws MessagingException;
    void sendPasswordResetSuccessfulEmail(String email) throws MessagingException;

    void sendMailToOwner(String email, String username, String name, String id);
}

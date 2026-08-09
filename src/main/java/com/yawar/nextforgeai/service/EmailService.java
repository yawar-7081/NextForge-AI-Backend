package com.yawar.nextforgeai.service;

import jakarta.mail.MessagingException;

public interface EmailService {
    void sendOtpEmail(String email, String otp, String name) throws MessagingException;
    void sendRegisterSuccessfulEmail(String email, String name) throws MessagingException;
    void sendPasswordResetEmail(String email,String name, String resetLink);
}

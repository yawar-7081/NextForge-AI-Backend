package com.yawar.nextforgeai.service;


public interface EmailService {
    void sendOtpEmail(String email, String otp) ;
    void sendRegisterSuccessfulEmail(String email) ;
    void sendPasswordResetEmail(String email, String resetLink) ;
    void sendPasswordResetSuccessfulEmail(String email) ;

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

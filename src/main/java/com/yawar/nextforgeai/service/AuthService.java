package com.yawar.nextforgeai.service;


import com.yawar.nextforgeai.dto.*;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

public interface AuthService {
    com.yawar.nextforgeai.dto.RegisterResponse register(@Valid com.yawar.nextforgeai.dto.RegisterRequest request) ;

    com.yawar.nextforgeai.dto.AuthResponse verifyOtpAndFilnalizeRegister(String userId, @Valid com.yawar.nextforgeai.dto.OtpRequest request, HttpServletRequest httpServletRequest) ;

    com.yawar.nextforgeai.dto.AuthResponse login(@Valid com.yawar.nextforgeai.dto.LoginRequest request, HttpServletRequest httpServletRequest);

    void forgotPassword(@Valid com.yawar.nextforgeai.dto.ForgotPasswordRequest request) ;

    void resetPassword(@Valid com.yawar.nextforgeai.dto.ResetPasswordRequest request) ;

    com.yawar.nextforgeai.dto.AuthResponse refreshToken(String refreshToken);

    void logout(String request);
}

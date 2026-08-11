package com.yawar.nextforgeai.controller;



import com.yawar.nextforgeai.service.AuthService;
import com.yawar.nextforgeai.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class AuthController {

    AuthService authService;
    SessionService sessionService;

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<com.yawar.nextforgeai.dto.RegisterResponse> register(@Valid @RequestBody(required = true) com.yawar.nextforgeai.dto.RegisterRequest request) throws MessagingException {
        com.yawar.nextforgeai.dto.RegisterResponse response=authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/verify-otp/{userId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<com.yawar.nextforgeai.dto.AuthResponse> verifyOtpAndFilnalizeRegister(
            @PathVariable(value = "userId") String userId,
            @Valid @RequestBody(required = true) com.yawar.nextforgeai.dto.OtpRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws MessagingException {
        com.yawar.nextforgeai.dto.AuthResponse response = authService.verifyOtpAndFilnalizeRegister(userId,request,httpServletRequest);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(false)          // true in production (HTTPS)
                .sameSite("Lax")        // Strict or Lax
                .path("/nextforgeai/api/v1/auth")
                .maxAge(Duration.ofDays(30))
                .build();

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Don't expose refresh token in response body
        response.setRefreshToken(null);

        return ResponseEntity.ok(response);
    }
    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<com.yawar.nextforgeai.dto.AuthResponse> login(
            @Valid @RequestBody com.yawar.nextforgeai.dto.LoginRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {

        com.yawar.nextforgeai.dto.AuthResponse response = authService.login(request, httpServletRequest);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(false)          // true in production (HTTPS)
                .sameSite("Lax")        // Strict or Lax
                .path("/nextforgeai/api/v1/auth")
                .maxAge(Duration.ofDays(30))
                .build();

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Don't expose refresh token in response body
        response.setRefreshToken(null);

        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/forgot-password",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody(required = true) com.yawar.nextforgeai.dto.ForgotPasswordRequest request) throws MessagingException {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message","Password Reset Link Send Successfully"));
    }


    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody com.yawar.nextforgeai.dto.ResetPasswordRequest request) throws MessagingException {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset. You can now login."));
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<com.yawar.nextforgeai.dto.AuthResponse> refreshToken(
            @CookieValue(name = "refreshToken") String refreshToken,
            HttpServletResponse httpServletResponse
    ) {

        com.yawar.nextforgeai.dto.AuthResponse response = authService.refreshToken(refreshToken);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(false)          // true in production
                .sameSite("Lax")
                .path("/nextforgeai/api/v1/auth")
                .maxAge(Duration.ofDays(30))
                .build();

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Don't expose refresh token in JSON
        response.setRefreshToken(null);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken") String refreshToken,
            HttpServletResponse response
    ) {

        authService.logout(refreshToken);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false) // true in production (HTTPS)
                .sameSite("Lax")
                .path("/nextforgeai/api/v1/auth")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.noContent().build();
    }
}
